package jp.ikanoshiokara.pixelpilot.view.mouse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ikanoshiokara.pixelpilot.domain.input.DialItem
import jp.ikanoshiokara.pixelpilot.domain.input.DialState
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.Res
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_back
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_choose
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_description
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_forward
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_middle
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_release
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.dial_right
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.cos
import kotlin.math.sin

/** リングの外側・内側の半径。中央の穴がキャンセルの範囲(実機で調整する、#48)。 */
private val OUTER_RADIUS = 104.dp
private val INNER_RADIUS = 44.dp

/** 弧と弧の間の隙間(度)。 */
private const val GAP_DEGREES = 3f

/** 項目の向き(度。右が 0 で時計回り、Compose の drawArc と同じ)と文言。 */
private val ITEMS: List<Triple<DialItem, Float, StringResource>> = listOf(
    Triple(DialItem.FORWARD, 0f, Res.string.dial_forward),
    Triple(DialItem.RIGHT, 90f, Res.string.dial_right),
    Triple(DialItem.BACK, 180f, Res.string.dial_back),
    Triple(DialItem.MIDDLE, 270f, Res.string.dial_middle),
)

/**
 * 長押しで開く操作ダイアル(#98、画面デザインの仮案「2-b」)。リングを 4 つの弧に分け、選んでいる弧を強調する。
 *
 * 操作面と同じ大きさに重ねる。座標は [DialState] の dp(操作面の左上が原点)。
 * 画面の端で開いたときは、リングがはみ出さないよう中心を内側へずらして描く(選ぶ向きは指のずれで決まるので変わらない)。
 */
@Composable
internal fun TouchpadDialOverlay(state: DialState, modifier: Modifier = Modifier) {
    val labels = ITEMS.associate { (item, _, label) -> item to stringResource(label) }
    val description = stringResource(Res.string.dial_description)
    val status = state.selected?.let { stringResource(Res.string.dial_release, labels.getValue(it)) }
        ?: stringResource(Res.string.dial_choose)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .semantics { contentDescription = description },
    ) {
        val centerX = state.centerX.dp.clampInto(maxWidth)
        val centerY = state.centerY.dp.clampInto(maxHeight)
        val fingerX = centerX + (state.fingerX - state.centerX).dp
        val fingerY = centerY + (state.fingerY - state.centerY).dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            // ダイアルの外の操作面を暗くする
            drawRect(PixelPilotColors.Background.copy(alpha = 0.55f))

            val center = Offset(centerX.toPx(), centerY.toPx())
            val ringRadius = ((OUTER_RADIUS + INNER_RADIUS) / 2).toPx()
            val ringWidth = (OUTER_RADIUS - INNER_RADIUS).toPx()
            val edgeRadius = (OUTER_RADIUS - 3.dp).toPx()
            ITEMS.forEach { (item, angle, _) ->
                val selected = item == state.selected
                val start = angle - 45f + GAP_DEGREES / 2
                val sweep = 90f - GAP_DEGREES
                drawArc(
                    color = if (selected) PixelPilotColors.Accent.copy(alpha = 0.28f) else PixelPilotColors.SurfaceRaised,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = center - Offset(ringRadius, ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = ringWidth),
                )
                if (selected) {
                    // 選んでいる弧の外周にアクセント色の線を引く
                    drawArc(
                        color = PixelPilotColors.Accent,
                        startAngle = angle - 38f,
                        sweepAngle = 76f,
                        useCenter = false,
                        topLeft = center - Offset(edgeRadius, edgeRadius),
                        size = Size(edgeRadius * 2, edgeRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // 中央の破線の円: ここで離すとキャンセル
            drawCircle(
                color = PixelPilotColors.Outline,
                radius = 28.dp.toPx(),
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                ),
            )
            val finger = Offset(fingerX.toPx(), fingerY.toPx())
            if (state.selected != null) {
                drawLine(
                    color = PixelPilotColors.Accent.copy(alpha = 0.9f),
                    start = center,
                    end = finger,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(color = PixelPilotColors.OnSurfaceMuted, radius = 4.dp.toPx(), center = center)
            drawCircle(color = PixelPilotColors.OnSurface.copy(alpha = 0.18f), radius = 14.dp.toPx(), center = finger)
            drawCircle(
                color = PixelPilotColors.OnSurface,
                radius = 14.dp.toPx(),
                center = finger,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        // 弧の中の文言
        val labelRadius = (OUTER_RADIUS + INNER_RADIUS) / 2
        ITEMS.forEach { (item, angle, _) ->
            val radians = angle / 180f * kotlin.math.PI.toFloat()
            Box(
                modifier = Modifier
                    .offset(
                        x = centerX + labelRadius * cos(radians) - LABEL_SIZE / 2,
                        y = centerY + labelRadius * sin(radians) - LABEL_SIZE / 2,
                    )
                    .size(LABEL_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labels.getValue(item),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item == state.selected) PixelPilotColors.Accent else PixelPilotColors.OnSurface,
                )
            }
        }

        // 離したときの操作。ダイアルの位置によらず、操作面の上部に出す
        Box(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
        ) {
            Text(
                text = status,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.selected != null) PixelPilotColors.Accent else PixelPilotColors.OnSurfaceMuted,
                modifier = Modifier
                    .background(PixelPilotColors.Background.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** 文言を置く枠の大きさ。 */
private val LABEL_SIZE = 72.dp

/** リングが [limit] の中に収まるように、中心の位置を内側へ寄せる。収まらない幅なら真ん中に置く。 */
private fun Dp.clampInto(limit: Dp): Dp =
    if (limit < OUTER_RADIUS * 2) limit / 2 else coerceIn(OUTER_RADIUS, limit - OUTER_RADIUS)
