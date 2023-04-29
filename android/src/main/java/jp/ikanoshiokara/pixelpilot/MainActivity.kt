package jp.ikanoshiokara.pixelpilot

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.DataInputStream
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

                    val sessionState = remember { mutableStateOf<SessionState>(SessionState.WAIT) }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val scanLauncher = rememberLauncherForActivityResult(
                            contract = ScanContract(),
                            onResult = { result ->
                                serverIp.value = result.contents
                            }
                        )
                        Button(
                            onClick = {
                                scanLauncher.launch(ScanOptions())
                            }
                        ) {
                            Text("Start Connect")
                        }

                        Text("Connect Status: ${sessionState.value.stateMessage}")

                        when(sessionState.value) {
                            is SessionState.WAIT -> {
                                Text("Enter message")
                                TextField(
                                    value = message.value,
                                    onValueChange = {
                                        message.value = it
                                    }
                                )
                                Button(
                                    onClick = {
                                        composableScope.launch(Dispatchers.IO) {
                                            val socket = Socket(serverIp.value, SERVER_PORT)
                                            val outputStream = DataOutputStream(socket.getOutputStream())
                                            val inputStream = DataInputStream(socket.getInputStream())
                                            outputStream.writeUTF(message.value)
                                            val receiveMessage = inputStream.readUTF()
                                            if (receiveMessage == "pong") {
                                                sessionState.value = SessionState.CONNECT
                                            }

                                            Log.d("Message_Info", "Send: ${message.value}")
                                            message.value = ""
                                        }
                                    },
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text("Send")
                                }
                            }
                            is SessionState.CONNECT -> {

                            }
                        }
                    }
                }
            }
        }
    }
}