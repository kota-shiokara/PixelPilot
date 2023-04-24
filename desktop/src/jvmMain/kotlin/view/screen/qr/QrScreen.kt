package view.screen.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
fun QrScreen(
    qrScreenStateHolder: QrScreenStateHolder,
    modifier: Modifier = Modifier,
) {
    val state by qrScreenStateHolder.state.collectAsState()

    Box(
        modifier = modifier
    ) {
        state.qrcode?.let { qrcode ->
            Image(
                qrcode,
                contentDescription = "",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}