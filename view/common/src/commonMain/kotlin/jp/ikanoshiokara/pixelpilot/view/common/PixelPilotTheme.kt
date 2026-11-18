package jp.ikanoshiokara.pixelpilot.view.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PixelPilot の色。画面デザインの仮案(暗い背景 + アクセント 1 色)のとおり。
 *
 * 暗い部屋や手元を見ずに使う場面を想定し、v2.0 はダークテーマのみとする。
 */
object PixelPilotColors {
    val Background = Color(0xFF111418)
    val Surface = Color(0xFF1B2027)
    val SurfaceRaised = Color(0xFF252B34)
    val Outline = Color(0xFF3A424E)
    val OnSurface = Color(0xFFE8ECF1)

    /** 補足の文言。背景に対してコントラスト比 4.5:1 以上。 */
    val OnSurfaceMuted = Color(0xFFA4AEBB)

    val Accent = Color(0xFF5AB0FF)
    val OnAccent = Color(0xFF0B1A2A)

    /** 接続状態: 接続中(緑)。色だけに頼らず、必ず文言を添える。 */
    val StatusConnected = Color(0xFF4CC38A)

    /** 接続状態: ユーザーの操作で直る(黄)。権限・Bluetooth オフなど。 */
    val StatusActionNeeded = Color(0xFFF2B54C)

    /** 接続状態: 自動では直らない・使えない(赤)。 */
    val StatusError = Color(0xFFF26B6B)
}

private val colorScheme = darkColorScheme(
    primary = PixelPilotColors.Accent,
    onPrimary = PixelPilotColors.OnAccent,
    background = PixelPilotColors.Background,
    onBackground = PixelPilotColors.OnSurface,
    surface = PixelPilotColors.Background,
    onSurface = PixelPilotColors.OnSurface,
    surfaceVariant = PixelPilotColors.Surface,
    onSurfaceVariant = PixelPilotColors.OnSurfaceMuted,
    surfaceContainer = PixelPilotColors.Surface,
    surfaceContainerHigh = PixelPilotColors.SurfaceRaised,
    outline = PixelPilotColors.Outline,
    error = PixelPilotColors.StatusError,
)

/** 全画面で使うテーマ。各アプリの画面のいちばん外側で使う。 */
@Composable
fun PixelPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
