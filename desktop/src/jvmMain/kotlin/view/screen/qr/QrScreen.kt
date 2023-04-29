package view.screen.qr

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun QrScreen(
    qrScreenStateHolder: QrScreenStateHolder,
    modifier: Modifier = Modifier,
) {
    val state by qrScreenStateHolder.state.collectAsState()

    qrScreenStateHolder.waitClient()

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

@Composable
fun QrContent(
    qrcode: Painter? = null,
    stateMessage: String = ""
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        qrcode?.let { qrcode ->
            Image(
                modifier = Modifier.weight(8F).fillMaxHeight(),
                painter = qrcode,
                contentDescription = "",
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = stateMessage,
            modifier = Modifier.weight(2F).fillMaxHeight()
        )
    }
}