package jp.ikanoshiokara.pixelpilot.transport.bluetooth.hid

import jp.ikanoshiokara.pixelpilot.domain.transport.InputTransport
import jp.ikanoshiokara.pixelpilot.model.connection.ConnectionState
import jp.ikanoshiokara.pixelpilot.model.connection.TransportKind
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.input.KeyboardLeds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Bluetooth HID 経路の [InputTransport]。送信キュー(docs/design/v2.0-standard.md 3.3)。
 *
 * - [send] は呼び出し元(UI スレッドなど)をブロックせず、[BluetoothHidController.executor] の 1 本のスレッドに積む。
 *   HID のコールバックと同じスレッドなので、接続・切断の通知とレポートの送信が前後しない。
 * - 移動・スクロールは溜めておき、[motionFlushIntervalMillis] ごとにまとめて 1 レポートで送る。
 *   ボタン・キーはすぐに送り、その前に溜めている移動を吐き出す([InputBatcher])。
 * - 未接続のイベントは捨てる。接続が切れたら押下状態を初期化し、再接続時に押したままにならないようにする。
 */
class BluetoothHidTransport(private val controller: BluetoothHidController) : InputTransport {
    override val kind: TransportKind = TransportKind.BLUETOOTH_HID
    override val state: StateFlow<ConnectionState> = controller.state
    override val keyboardLeds: StateFlow<KeyboardLeds> = controller.keyboardLeds

    /**
     * 移動・スクロールをまとめて送る間隔。短いほど滑らかだが送信回数が増える。
     * 遅延の PoC で実機に合わせて調整できるよう、定数ではなく変更できる値にしている(#48)。
     */
    @Volatile
    var motionFlushIntervalMillis: Long = DEFAULT_MOTION_FLUSH_INTERVAL_MILLIS

    private val executor = controller.executor
    private val batcher = InputBatcher()

    /** 溜めた移動を吐き出す予約が入っているか。送信キューのスレッドからだけ読み書きする。 */
    private var flushScheduled = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            controller.state.collect { state ->
                if (state !is ConnectionState.Connected) {
                    // 切断済みのため離したレポートは送れない。押下状態と溜めた移動だけ捨てる
                    executor.execute { batcher.reset() }
                }
            }
        }
    }

    override fun send(event: InputEvent) {
        executor.execute {
            if (controller.state.value !is ConnectionState.Connected) return@execute
            batcher.add(event).forEach(controller::sendReport)
            scheduleFlushIfNeeded()
        }
    }

    private fun scheduleFlushIfNeeded() {
        if (!batcher.hasPendingMotion || flushScheduled) return
        flushScheduled = true
        executor.schedule(
            Runnable {
                flushScheduled = false
                if (controller.state.value is ConnectionState.Connected) {
                    batcher.flush().forEach(controller::sendReport)
                }
            },
            motionFlushIntervalMillis,
            TimeUnit.MILLISECONDS,
        )
    }

    private companion object {
        /** 約 120Hz。設計書 3.3 の「約 8ms」。 */
        const val DEFAULT_MOTION_FLUSH_INTERVAL_MILLIS = 8L
    }
}
