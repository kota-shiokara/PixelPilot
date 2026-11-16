package jp.ikanoshiokara.pixelpilot.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * KMP 構成(androidApp → shared/ui-components)のつながりを確かめるための仮画面。
 * ホスト選択(#44)やマウス画面(#40)ができたら削除する。
 */
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PixelPilot v2(開発中)",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
