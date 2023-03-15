package jp.ikanoshiokara.pixelpilot

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.Socket

class MainActivity : AppCompatActivity() {

    companion object {
        const val SERVER_PORT = 49152
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val message = remember { mutableStateOf("") }
                    val serverIp = remember { mutableStateOf("") }
                    val composableScope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Enter IPAddress")
                        TextField(
                            value = serverIp.value,
                            onValueChange = {
                                serverIp.value = it
                            }
                        )
                        Text("Enter message")
                        TextField(
                            value = message.value,
                            onValueChange = {
                                message.value = it
                            }
                        )
                        Button(
                            onClick = {
                                Log.d("Message_Info", "Send: ${message.value}")
                                composableScope.launch(Dispatchers.IO) {
                                    val socket = Socket(serverIp.value, SERVER_PORT)
                                    val outputStream = DataOutputStream(socket.getOutputStream())

                                    outputStream.writeUTF(message.value)
                                    outputStream.close()
                                    socket.close()

                                    message.value = ""
                                }
                            },
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}