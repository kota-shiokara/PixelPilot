package jp.ikanoshiokara.pixelpilot.view.mouse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ikanoshiokara.pixelpilot.domain.input.TouchEvent
import jp.ikanoshiokara.pixelpilot.domain.input.TouchpadInterpreter
import jp.ikanoshiokara.pixelpilot.domain.transport.InputTransport
import jp.ikanoshiokara.pixelpilot.model.input.Button
import jp.ikanoshiokara.pixelpilot.model.input.InputEvent
import jp.ikanoshiokara.pixelpilot.model.settings.MouseSettings
import jp.ikanoshiokara.pixelpilot.view.common.ConnectionStatusBadge
import jp.ikanoshiokara.pixelpilot.view.common.PixelPilotColors
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.Res
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.button_left
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.button_middle
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.button_right
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.hint_one_finger
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.hint_three_fingers
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.hint_two_fingers
import jp.ikanoshiokara.pixelpilot.view.mouse.resources.mouse_surface_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * マウス画面(画面デザインの仮案「メイン: マウス」、docs/design/v2.0-standard.md 4.)。
 *
 * - 操作面はできるだけ広く取る。上に帯は置かず、接続状態は操作面の左上に重ねる
 * - 操作面の左右に余白を残す(ジェスチャーナビゲーションの「戻る」と取り合わないため。幅は実機で調整する)
 * - 下部の左・中・右のボタンは、押している間ボタンを押したままにする。設定で非表示にできる
 *
 * タッチ点は [TouchpadInterpreter] に渡し、できた入力イベントを [InputTransport] で送る。画面はそれ以外を判断しない。
 */
@Composable
fun MouseScreen(
    settings: MouseSettings,
    modifier: Modifier = Modifier,
    transport: InputTransport = koinInject(),
) {
    val connection by transport.state.collectAsState()
    val interpreter = remember { TouchpadInterpreter(settings) }
    LaunchedEffect(settings) { interpreter.settings = settings }

    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 8.dp)
                .background(PixelPilotColors.Surface, RoundedCornerShape(24.dp))
                .border(1.dp, PixelPilotColors.Outline, RoundedCornerShape(24.dp))
                .touchpad(interpreter, transport),
        ) {
            val description = stringResource(Res.string.mouse_surface_description)
            Box(modifier = Modifier.fillMaxSize().semantics { contentDescription = description })
            ConnectionStatusBadge(connection, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            Hints(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
        }

        if (settings.showButtons) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HoldButton(Res.string.button_left, Button.LEFT, transport, Modifier.weight(1f))
                HoldButton(Res.string.button_middle, Button.MIDDLE, transport, Modifier.width(64.dp))
                HoldButton(Res.string.button_right, Button.RIGHT, transport, Modifier.weight(1f))
            }
        }
    }
}

/** 操作面の左右の余白。画面端からの「戻る」のジェスチャーと取り合わないようにする。 */
private val SIDE_MARGIN = 20.dp

/**
 * タッチを [TouchpadInterpreter] に渡す。座標は dp にしてから渡す。
 * 指が画面の外へ出たり、システムにタッチを取られたりして中断されたら、進行中のジェスチャーを打ち切る。
 */
private fun Modifier.touchpad(interpreter: TouchpadInterpreter, transport: InputTransport): Modifier =
    pointerInput(interpreter, transport) {
        awaitPointerEventScope {
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        val x = change.position.x / density
                        val y = change.position.y / density
                        val id = change.id.value
                        val time = change.uptimeMillis
                        val touch = when {
                            !change.previousPressed && change.pressed -> TouchEvent.Down(id, x, y, time)
                            change.previousPressed && !change.pressed -> TouchEvent.Up(id, time)
                            // 複数の指のうち動いていない指の通知は渡さない
                            change.pressed && change.positionChanged() -> TouchEvent.Move(id, x, y, time)
                            else -> null
                        }
                        if (touch != null) interpreter.onTouch(touch).forEach(transport::send)
                        change.consume()
                    }
                }
            } finally {
                interpreter.cancel().forEach(transport::send)
            }
        }
    }

@Composable
private fun Hints(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Res.string.hint_one_finger, Res.string.hint_two_fingers, Res.string.hint_three_fingers).forEach {
            Text(text = stringResource(it), fontSize = 12.sp, color = PixelPilotColors.OnSurfaceMuted)
        }
    }
}

/** 押している間、マウスのボタンを押したままにする。 */
@Composable
private fun HoldButton(label: StringResource, button: Button, transport: InputTransport, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(PixelPilotColors.SurfaceRaised, RoundedCornerShape(14.dp))
            .pointerInput(button, transport) {
                detectTapGestures(
                    onPress = {
                        transport.send(InputEvent.MouseButton(button, pressed = true))
                        tryAwaitRelease()
                        transport.send(InputEvent.MouseButton(button, pressed = false))
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(label), fontSize = 14.sp, color = PixelPilotColors.OnSurface)
    }
}
