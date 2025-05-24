package jp.ikanoshiokara.pixelpilot.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors

class BluetoothHidMouseManager(private val context: Context) {
    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    private val reportId = 0

    fun setupBluetooth(onConnected: (() -> Unit)? = null) {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager.adapter
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    bluetoothHidDevice = proxy as BluetoothHidDevice
                    registerHidApp()
                    val pairedDevices = adapter.bondedDevices
                    // ここら辺を保存するなどでよしなにしてハードコーディングを避けたい
                    if (pairedDevices.isNotEmpty() && pairedDevices.any { it.name.contains("kota") }) {
                        hostDevice = pairedDevices.first { it.name.contains("kota") }
                        bluetoothHidDevice?.connect(hostDevice)
                        onConnected?.invoke()
                    }
                }
            }
            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HID_DEVICE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun registerHidApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "PixelPilot", "Mouse", "kota-shiokara",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            MOUSE_REPORT_DESCRIPTOR
        )
        bluetoothHidDevice?.registerApp(
            sdp,
            null, null,
            Executors.newSingleThreadExecutor(),
            object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    hostDevice = pluggedDevice
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendMouseReport(dx: Int, dy: Int) {
        val report = byteArrayOf(0x00, dx.toByte(), dy.toByte())
        hostDevice?.let {
            bluetoothHidDevice?.sendReport(it, reportId, report)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendMouseClick(button: BluetoothHidMouseButton) {
        val report = byteArrayOf(button.value, 0x00, 0x00)
        val clearReport = byteArrayOf(0x00, 0x00, 0x00)
        hostDevice?.let {
            bluetoothHidDevice?.sendReport(it, reportId, report)
            // これをやらないとClickイベントがDownのままっぽい
            bluetoothHidDevice?.sendReport(it, reportId, clearReport)
        }
    }

    companion object {
        private const val BYTE_A1 = 0xA1.toByte()
        private const val BYTE_95 = 0x95.toByte()
        private const val BYTE_81 = 0x81.toByte()
        private const val BYTE_C0 = 0xC0.toByte()
        val MOUSE_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
            0x05, 0x01, 0x09, 0x02, BYTE_A1, 0x01,
            0x09, 0x01, BYTE_A1, 0x00,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x03,
            0x15, 0x00, 0x25, 0x01,
            BYTE_95, 0x03, 0x75, 0x01, BYTE_81, 0x02,
            BYTE_95, 0x01, 0x75, 0x05, BYTE_81, 0x03,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31,
            0x15, BYTE_81, 0x25, 0x7F,
            0x75, 0x08, BYTE_95, 0x02, BYTE_81, 0x06,
            BYTE_C0, BYTE_C0
        )
    }
}

enum class BluetoothHidMouseButton(val value: Byte) {
    LEFT(0x01),
    RIGHT(0x02),
    MIDDLE(0x04),
    BACK(0x08),
    FORWARD(0x10);

    fun toByte(): Byte = value
}

