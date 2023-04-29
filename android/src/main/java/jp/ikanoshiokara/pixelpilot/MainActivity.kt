package jp.ikanoshiokara.pixelpilot

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.ikanoshiokara.pixelpilot.MainActivity.Companion.SERVER_PORT
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val cameraState = remember { mutableStateOf(false) }
                        val serverIp = remember { mutableStateOf("") }

                        if (cameraState.value) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PreviewView(
                                    modifier = Modifier.size(300.dp),
                                    onSuccess = {
                                        serverIp.value = it[0]
                                        Log.d("MainActivity", it[0])
                                        cameraState.value = false
                                    }
                                )
                            }
                        } else {
                            MainContent(
                                serverIp = serverIp.value,
                                cameraButtonOnClick = {
                                    cameraState.value = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    serverIp: String,
    cameraButtonOnClick: () -> Unit = {},
) {
    val composableScope = rememberCoroutineScope()

    val sessionState = remember { mutableStateOf<SessionState>(SessionState.WAIT) }

    Text("Connect Status: ${sessionState.value.stateMessage}")

    when(sessionState.value) {
        is SessionState.WAIT -> {
            Button(
                onClick = cameraButtonOnClick
            ) {
                Text("Get Server IP")
            }

            if (serverIp.isNotBlank()) {
                Button(
                    onClick = {
                        Log.d("Message_Info", serverIp)
                        composableScope.launch(Dispatchers.IO) {
                            Log.d("Message_Info", "coroutine start")
                            val socket = Socket(serverIp, SERVER_PORT)
                            val outputStream = DataOutputStream(socket.getOutputStream())
                            outputStream.writeUTF("ping")

                            sessionState.value = SessionState.CONNECT
                        }
                    },
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Start Connect")
                }
            }
        }
        is SessionState.CONNECT -> {
            val sendCommand = { code: Int ->
                composableScope.launch(Dispatchers.IO) {
                    val socket = Socket(serverIp, SERVER_PORT)
                    val outputStream = DataOutputStream(socket.getOutputStream())
                    outputStream.writeUTF("$code")
                }
            }

            Button(
                onClick = {
                    composableScope.launch(Dispatchers.IO) {
                        val socket = Socket(serverIp, SERVER_PORT)
                        val outputStream = DataOutputStream(socket.getOutputStream())
                        outputStream.writeUTF("exit")

                        sessionState.value = SessionState.WAIT
                    }
                }
            ) {
                Text("Exit")
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        sendCommand(38)
                    }
                ) {
                    Text("↑")
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            sendCommand(37)
                        }
                    ) {
                        Text("←")
                    }
                    Button(
                        onClick = {
                            sendCommand(39)
                        }
                    ) {
                        Text("→")
                    }
                }
                Button(
                    onClick = {
                        sendCommand(40)
                    }
                ) {
                    Text("↓")
                }
            }
        }
    }
}