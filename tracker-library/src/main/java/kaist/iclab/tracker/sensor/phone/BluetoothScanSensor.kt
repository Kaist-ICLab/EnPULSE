package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class BluetoothScanSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<BluetoothScanSensor.Config, BluetoothScanSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    data class Config(
        val doScan: Boolean,
        val interval: Long,
        val scanDuration: Long,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val name: String,
        val alias: String,
        val address: String,
        val bondState: Int,
        val connectionType: Int,
        val classType: Int,
        val rssi: Int,
        val isLE: Boolean
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.ACCESS_FINE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,

        ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull<Int>().toTypedArray()

    private fun handleDeviceFound(
        device: BluetoothDevice,
        timestamp: Long,
        rssi: Int,
        isLE: Boolean
    ) {
        try {
            val deviceName = device.name
            val deviceAlias =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else null

            // Only include devices that have both name and alias
            if (deviceName == null || deviceAlias == null) return

            listeners.forEach { item ->
                item.invoke(
                    Entity(
                        System.currentTimeMillis(),
                        timestamp,
                        deviceName,
                        deviceAlias,
                        device.address,
                        device.bondState,
                        device.type,
                        device.bluetoothClass.deviceClass,
                        rssi,
                        isLE
                    )
                )
            }
        } catch (e: SecurityException) {
            throw e
        }
    }

    private val broadcastListener: BroadcastListener = BroadcastListener(
        context,
        arrayOf(BluetoothDevice.ACTION_FOUND)
    )

    private val alarmListener = AlarmListener(
        context, "BT_SCAN_REQUEST", 0x00, configStateFlow.value.interval,
    )

    private val broadcastCallback = BroadcastListener@{ intent: Intent? ->
        val extras = intent?.extras ?: return@BroadcastListener
        val rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI, 0).toInt()
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            // Only option until TIRAMISU
            extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
        } ?: return@BroadcastListener
        handleDeviceFound(device, System.currentTimeMillis(), rssi, false)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private val alarmCallback = { _: Intent? ->
        val adapter = bluetoothManager.adapter
        if (adapter.isEnabled) {
            try {
                adapter.startDiscovery()
                adapter.bluetoothLeScanner.startScan(scanCallback)

                CoroutineScope(Dispatchers.IO).launch {
                    delay(configStateFlow.value.scanDuration) // 5 seconds delay
                    adapter.bluetoothLeScanner.stopScan(scanCallback)
                    adapter.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                throw e
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                handleDeviceFound(it.device, System.currentTimeMillis(), it.rssi, true)
            }
        }
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    override fun init() {
        super.init()

        // TODO: Bluetooth status can change any time?
        if (!bluetoothManager.adapter.isEnabled) {
            SensorState(SensorState.FLAG.UNAVAILABLE, "Bluetooth is disabled")
        }
    }

    override fun onStart() {
        broadcastListener.addListener(broadcastCallback)
        if (configStateFlow.value.doScan) {
            alarmListener.addListener(alarmCallback)
        }
    }

    override fun onStop() {
        broadcastListener.removeListener(broadcastCallback)
        if (configStateFlow.value.doScan) {
            alarmListener.removeListener(alarmCallback)
        }
    }
}