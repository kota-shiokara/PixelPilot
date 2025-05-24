package jp.ikanoshiokara.pixelpilot

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import jp.ikanoshiokara.pixelpilot.bluetooth.BluetoothHidMouseButton
import jp.ikanoshiokara.pixelpilot.bluetooth.BluetoothHidMouseManager
import jp.ikanoshiokara.pixelpilot.ui.theme.PixelPilotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothHidMouseManager: BluetoothHidMouseManager

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                permissionRequest()
                bluetoothHidMouseManager = BluetoothHidMouseManager(context)
                bluetoothHidMouseManager.setupBluetooth()
            }

            PixelPilotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        bluetoothHidMouseManager.sendMouseClick(
                                            BluetoothHidMouseButton.MIDDLE
                                        )
                                    }
                                }
                            ) {
                                Text("Middle Click")
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(color = Color.DarkGray)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        coroutineScope.launch {
                                            if (it.x < size.width * 2 / 3) {
                                                bluetoothHidMouseManager.sendMouseClick(
                                                    BluetoothHidMouseButton.LEFT
                                                )
                                            } else {
                                                bluetoothHidMouseManager.sendMouseClick(
                                                    BluetoothHidMouseButton.RIGHT
                                                )
                                            }
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        val (dx, dy) = dragAmount
                                        coroutineScope.launch {
                                            bluetoothHidMouseManager
                                                .sendMouseReport(
                                                    dx.toInt().coerceIn(-127, 127),
                                                    dy.toInt().coerceIn(-127, 127)
                                                )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ここをスワイプでマウス移動")
                        }
                    }
                }
            }
        }
    }

    private fun permissionRequest() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val androidSPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            requestPermissions(
                androidSPermissions,
                1
            )
        } else {
            val androidPermissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            requestPermissions(
                androidPermissions,
                1
            )
        }
    }
}
