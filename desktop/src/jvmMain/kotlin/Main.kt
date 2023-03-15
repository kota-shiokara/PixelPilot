import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.ikanoshiokara.common.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket


private const val SERVER_PORT = 49152
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PixelPilot"
    ) {
        println(InetAddress.getLocalHost().hostAddress)
        Surface(modifier = Modifier.fillMaxSize()) {
            val messages = mutableStateListOf<String>()
            val composableScope = rememberCoroutineScope()

            composableScope.launch(Dispatchers.IO) {
                try {
                    val serverSocket = ServerSocket(SERVER_PORT)
                    while (true) {
                        val socket = serverSocket.accept()
                        val inputStream = DataInputStream(socket.getInputStream())
                        val message = inputStream.readUTF()
                        println("receive: $message")
                        messages.add(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Message")

                LazyColumn {
                    items(messages) { message ->
                        Text(message)
                    }
                }
            }
        }
    }
}
