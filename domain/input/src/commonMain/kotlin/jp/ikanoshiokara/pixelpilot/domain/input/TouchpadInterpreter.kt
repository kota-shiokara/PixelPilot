package jp.ikanoshiokara.pixelpilot.domain.input

import jp.ikanoshiokara.pixelpilot.model.input.Button
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.model.settings.ScrollDirection
import kotlin.math.abs
import kotlin.math.hypot

/**
 * タッチパッドのジェスチャーの判定に使う値。単位は dp とミリ秒。
 *
 * @property touchSlop これより動いたら、タップではなく移動とみなす。
 * @property tapMaxDurationMillis これより長く触れていたら、タップとみなさない。
 * @property doubleTapTimeoutMillis タップの後、この時間内にもう一度触れると「タップ直後に押したまま」とみなす。
 * @property doubleTapSlop 2 回目に触れた位置が、1 回目からこれ以上離れていたら別の操作とみなす。
 * @property pointerGain 感度 1.0 のときの、1dp あたりのカーソルの移動量(ホストのピクセル)。
 * @property scrollDpPerStep ホイール 1 段ぶんになる、2 本指の移動量。
 * @property longPressMillis 1 本指で動かさずにこれだけ触れていたら、操作ダイアルを開く。
 * @property dialSelectDistance ダイアルの中心からこれ以上ずらしたら項目を選ぶ。これより内側で離すとキャンセル。
 */
data class TouchpadConfig(
    val touchSlop: Float = 8f,
    val tapMaxDurationMillis: Long = 250,
    val doubleTapTimeoutMillis: Long = 300,
    val doubleTapSlop: Float = 48f,
    val pointerGain: Float = 1.6f,
    val scrollDpPerStep: Float = 16f,
    val longPressMillis: Long = 500,
    val dialSelectDistance: Float = 32f,
)

/**
 * タッチ点の列を [InputEvent] に変換する(docs/design/v2.0-standard.md 4.1)。純 Kotlin で、UI から切り離して単体テストする。
 *
 * | 操作 | 動作 |
 * |---|---|
 * | 1 本指でなぞる | カーソル移動 |
 * | 1 本指でタップ | 左クリック(続けて 2 回でダブルクリック) |
 * | 2 本指でタップ | 右クリック |
 * | 3 本指でタップ | 中クリック |
 * | 2 本指でなぞる | スクロール(縦・横) |
 * | タップ直後に押したままなぞる | ドラッグ(左ボタンを押したまま移動) |
 * | 1 本指で長押し | 操作ダイアルを開く。ずらして離すと 戻る(左)・進む(右)・中クリック(上)・右クリック(下) |
 *
 * タイマーは使わない。タップは指を離した時点で、ドラッグは 2 回目に触れて動いた時点で決まる。
 * そのため、タップ直後のドラッグでは、1 回目のタップのクリックが先に送られる(macOS のタップでドラッグと同じ)。
 *
 * 長押しは「動かないまま時間がたった」ことで決まるため、画面側が指の触れている間に [onTimePassed] で時刻を知らせる。
 * 知らせが来なくても、次のタッチの出来事の時刻で判定する。開いているダイアルは [dial] で読む。
 *
 * スレッドセーフではない。UI のスレッドから呼ぶ。
 */
class TouchpadInterpreter(
    var settings: MouseSettings = MouseSettings(),
    private val config: TouchpadConfig = TouchpadConfig(),
) {
    private class Pointer(val startX: Float, val startY: Float, var x: Float, var y: Float)

    private val pointers = mutableMapOf<Long, Pointer>()

    /** 1 回のジェスチャー(最初の指が触れてから、全部の指が離れるまで)の状態。 */
    private var gestureStartMillis = 0L
    private var maxPointers = 0
    private var moved = false
    private var dragCandidate = false
    private var dragging = false
    private var scrolling = false

    /** ダイアルを 2 本目の指で取り消した後など、全部の指が離れるまで何も送らない。 */
    private var ignoringGesture = false

    /** 開いている操作ダイアル。閉じていれば null。 */
    var dial: DialState? = null
        private set

    /** 直前のタップ(1 本指)が終わった時刻と位置。タップ直後のドラッグの判定に使う。 */
    private var lastTapUpMillis: Long? = null
    private var lastTapX = 0f
    private var lastTapY = 0f

    /** タッチの出来事を受け取り、送るべき入力イベントを順に返す。 */
    fun onTouch(event: TouchEvent): List<InputEvent> = when (event) {
        is TouchEvent.Down -> onDown(event)
        is TouchEvent.Move -> onMove(event)
        is TouchEvent.Up -> onUp(event)
        is TouchEvent.Cancel -> cancel()
    }

    /**
     * 指が触れたまま時間がたったことを知らせる。長押しなら操作ダイアルを開く。
     *
     * @return ダイアルをこの呼び出しで開いたら true(画面側が振動などで知らせる)。
     */
    fun onTimePassed(timeMillis: Long): Boolean = openDialIfLongPressed(timeMillis)

    private fun onDown(event: TouchEvent.Down): List<InputEvent> {
        if (pointers.isEmpty()) {
            gestureStartMillis = event.timeMillis
            maxPointers = 0
            moved = false
            scrolling = false
            val lastTap = lastTapUpMillis
            dragCandidate = lastTap != null &&
                event.timeMillis - lastTap <= config.doubleTapTimeoutMillis &&
                distance(lastTapX, lastTapY, event.x, event.y) <= config.doubleTapSlop
        }
        pointers[event.pointerId] = Pointer(event.x, event.y, event.x, event.y)
        maxPointers = maxOf(maxPointers, pointers.size)
        if (ignoringGesture) return emptyList()
        if (pointers.size > 1 && dial != null) {
            // ダイアルを開いている間に 2 本目の指が触れたら取り消す
            dial = null
            ignoringGesture = true
            return emptyList()
        }
        if (pointers.size > 1) {
            // 2 本目の指が触れたら、タップ直後のドラッグではない
            dragCandidate = false
            // 1 本指で動かし始めた後に 2 本目が触れた場合も、そこからスクロールにする
            if (moved && !dragging) scrolling = true
        }
        return emptyList()
    }

    private fun onMove(event: TouchEvent.Move): List<InputEvent> {
        val pointer = pointers[event.pointerId] ?: return emptyList()
        val dx = event.x - pointer.x
        val dy = event.y - pointer.y
        pointer.x = event.x
        pointer.y = event.y
        if (ignoringGesture) return emptyList()

        openDialIfLongPressed(event.timeMillis)
        dial?.let { current ->
            // ダイアルを開いている間は、カーソルを動かさずに項目を選ぶ
            dial = current.copy(
                fingerX = event.x,
                fingerY = event.y,
                selected = dialItemAt(event.x - current.centerX, event.y - current.centerY),
            )
            return emptyList()
        }

        val output = mutableListOf<InputEvent>()
        if (!moved && distance(pointer.startX, pointer.startY, pointer.x, pointer.y) > config.touchSlop) {
            moved = true
            when {
                pointers.size >= 2 -> scrolling = true
                dragCandidate -> {
                    dragging = true
                    output += InputEvent.MouseButton(Button.LEFT, pressed = true)
                }
            }
        }
        if (!moved) return output

        when {
            scrolling && pointers.size >= 2 -> scroll(dx / pointers.size, dy / pointers.size)?.let { output += it }
            // 2 本指の操作の後に 1 本だけ残った指の動きは、カーソルの飛びを防ぐため無視する
            maxPointers == 1 -> {
                val gain = config.pointerGain * settings.sensitivity
                output += InputEvent.PointerMove(dx * gain, dy * gain)
            }
        }
        return output
    }

    private fun onUp(event: TouchEvent.Up): List<InputEvent> {
        val pointer = pointers.remove(event.pointerId) ?: return emptyList()
        if (pointers.isNotEmpty()) return emptyList()

        if (ignoringGesture) {
            lastTapUpMillis = null
            resetGesture()
            return emptyList()
        }
        dial?.let { current ->
            // 選んだ項目のボタンを押して離す。中央付近で離したらキャンセル
            val button = current.selected?.button
            lastTapUpMillis = null
            resetGesture()
            return if (button == null) {
                emptyList()
            } else {
                listOf(InputEvent.MouseButton(button, pressed = true), InputEvent.MouseButton(button, pressed = false))
            }
        }

        val output = mutableListOf<InputEvent>()
        val isTap = !moved && event.timeMillis - gestureStartMillis <= config.tapMaxDurationMillis
        when {
            dragging -> {
                output += InputEvent.MouseButton(Button.LEFT, pressed = false)
                lastTapUpMillis = null
            }
            isTap -> {
                val button = when (maxPointers) {
                    1 -> Button.LEFT
                    2 -> Button.RIGHT
                    else -> Button.MIDDLE
                }
                output += InputEvent.MouseButton(button, pressed = true)
                output += InputEvent.MouseButton(button, pressed = false)
                if (maxPointers == 1) {
                    lastTapUpMillis = event.timeMillis
                    lastTapX = pointer.startX
                    lastTapY = pointer.startY
                } else {
                    lastTapUpMillis = null
                }
            }
            else -> lastTapUpMillis = null
        }
        resetGesture()
        return output
    }

    /** 進行中のジェスチャーを打ち切る。ドラッグ中なら左ボタンを離し、ダイアルは閉じる。 */
    fun cancel(): List<InputEvent> {
        val output = if (dragging) listOf(InputEvent.MouseButton(Button.LEFT, pressed = false)) else emptyList()
        pointers.clear()
        lastTapUpMillis = null
        resetGesture()
        return output
    }

    private fun resetGesture() {
        moved = false
        dragCandidate = false
        dragging = false
        scrolling = false
        maxPointers = 0
        ignoringGesture = false
        dial = null
    }

    /**
     * 1 本指で、動かさずに、タップ直後でもなく [TouchpadConfig.longPressMillis] 触れていたらダイアルを開く。
     * タップ直後のもう一度のタッチは、ドラッグを優先して開かない。
     */
    private fun openDialIfLongPressed(timeMillis: Long): Boolean {
        if (dial != null || ignoringGesture || moved || dragCandidate) return false
        if (pointers.size != 1 || maxPointers != 1) return false
        if (timeMillis - gestureStartMillis < config.longPressMillis) return false
        val pointer = pointers.values.single()
        dial = DialState(
            centerX = pointer.startX,
            centerY = pointer.startY,
            fingerX = pointer.x,
            fingerY = pointer.y,
            selected = dialItemAt(pointer.x - pointer.startX, pointer.y - pointer.startY),
        )
        return true
    }

    /** ダイアルの中心からのずれ(dp)で項目を選ぶ。横と縦で大きいほうの向き。画面の y は下が正。 */
    private fun dialItemAt(dx: Float, dy: Float): DialItem? = when {
        hypot(dx, dy) < config.dialSelectDistance -> null
        abs(dx) >= abs(dy) -> if (dx < 0) DialItem.BACK else DialItem.FORWARD
        dy < 0 -> DialItem.MIDDLE
        else -> DialItem.RIGHT
    }

    /**
     * 2 本指の移動量(dp)をスクロール量(ホイールの段数)にする。
     * HID のホイールは上へのスクロールが正、横スクロールは右が正(InputEvent.Scroll)。
     */
    private fun scroll(dx: Float, dy: Float): InputEvent.Scroll? {
        if (dx == 0f && dy == 0f) return null
        val step = config.scrollDpPerStep
        val (vertical, horizontal) = when (settings.scrollDirection) {
            // 指を下へ → 内容が下へ動く → 上へスクロール。指を右へ → 内容が右へ動く → 左へスクロール
            ScrollDirection.NATURAL -> dy / step to -dx / step
            // 指を下へ → 下へスクロール。指を右へ → 右へスクロール
            ScrollDirection.TRADITIONAL -> -dy / step to dx / step
        }
        return InputEvent.Scroll(vertical = vertical.withoutNegativeZero(), horizontal = horizontal.withoutNegativeZero())
    }

    /** 符号を反転したときにできる -0.0 を 0.0 にそろえる(data class の比較で別の値として扱われるため)。 */
    private fun Float.withoutNegativeZero(): Float = this + 0f

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x2 - x1, y2 - y1)
}
