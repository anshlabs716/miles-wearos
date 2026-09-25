package com.example.miles.wear.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Real BLE sensor support: external heart-rate straps (0x180D) and cadence
 * sensors (0x1816). Scans, connects, enables notifications and feeds the live
 * values into SensorTracker. Local-first — raw sensor data, no cloud.
 */
class BleSensorManager(private val context: Context) {

    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data object Scanning : ConnectionState
        data class Connecting(val name: String?) : ConnectionState
        data class Connected(
            val name: String,
            val hrBpm: Int?,
            val cadenceRpm: Int?,
            val batteryPct: Int?
        ) : ConnectionState
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _discovered = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discovered: StateFlow<List<BluetoothDevice>> = _discovered.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var scanner = adapter?.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    val isSupported: Boolean = adapter != null && scanner != null

    fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun toggleScan() {
        val current = _state.value
        if (current is ConnectionState.Scanning) {
            stopScan()
        } else {
            startScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!hasPermissions()) return
        val bleScanner = scanner ?: return
        _discovered.value = emptyList()
        _state.value = ConnectionState.Scanning
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            bleScanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.w("BleSensorManager", "Scan failed", e)
            _state.value = ConnectionState.Idle
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        if (_state.value !is ConnectionState.Connected) {
            _state.value = ConnectionState.Idle
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            mainHandler.post {
                val updated = _discovered.value.toMutableList()
                if (updated.none { it.address == device.address }) {
                    updated.add(device)
                    _discovered.value = updated
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = ConnectionState.Idle
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (!hasPermissions()) return
        stopScan()
        _state.value = ConnectionState.Connecting(device.name ?: device.address)
        gatt?.disconnect()
        gatt?.close()
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
        gatt = null
        _state.value = ConnectionState.Idle
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                g.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt?.close()
                gatt = null
                _state.value = ConnectionState.Idle
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val hr = g.getService(UUID.fromString(BleSensorCodec.HR_SERVICE))
            val csc = g.getService(UUID.fromString(BleSensorCodec.CSC_SERVICE))
            val battery = g.getService(UUID.fromString(BleSensorCodec.BATTERY_SERVICE))

            hr?.getCharacteristic(UUID.fromString(BleSensorCodec.HR_MEASUREMENT))?.let { enableNotify(g, it) }
            csc?.getCharacteristic(UUID.fromString(BleSensorCodec.CSC_MEASUREMENT))?.let { enableNotify(g, it) }
            battery?.getCharacteristic(UUID.fromString(BleSensorCodec.BATTERY_LEVEL))?.let {
                g.readCharacteristic(it)
            }
            _state.value = ConnectionState.Connected(
                name = g.device.name ?: g.device.address,
                hrBpm = null,
                cadenceRpm = null,
                batteryPct = null
            )
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val current = _state.value as? ConnectionState.Connected ?: return
            var hr = current.hrBpm
            var cadence = current.cadenceRpm
            when (characteristic.uuid.toString().lowercase()) {
                BleSensorCodec.HR_MEASUREMENT -> BleSensorCodec.parseHeartRate(value)?.let { hr = it }
                BleSensorCodec.CSC_MEASUREMENT -> BleSensorCodec.parseCadence(value)?.let { cadence = it }
            }
            _state.value = current.copy(hrBpm = hr, cadenceRpm = cadence)
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                characteristic.uuid.toString().lowercase() == BleSensorCodec.BATTERY_LEVEL
            ) {
                val current = _state.value as? ConnectionState.Connected ?: return
                val battery = BleSensorCodec.parseBattery(characteristic.value ?: ByteArray(0))
                _state.value = current.copy(batteryPct = battery)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotify(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            g.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(UUID.fromString(BleSensorCodec.CCCD))
                ?: return
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        } catch (e: Exception) {
            Log.w("BleSensorManager", "enableNotify failed", e)
        }
    }
}