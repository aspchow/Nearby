package com.avinash.nearby.receiver.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.avinash.nearby.permission.SystemStateReceiver
import com.avinash.nearby.receiver.model.BLEAdvertisementMeta
import com.avinash.nearby.utils.BLEConsts
import com.avinash.nearby.utils.printLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 03/07/25.
 */
@AndroidEntryPoint
class BLEAdvertiseService : Service() {

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    @Inject
    lateinit var serviceRepository: AdvertisingServiceRepository

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    @Inject
    lateinit var systemStateReceiver: SystemStateReceiver

    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var currentIntent: Intent? = null

    private val serviceScope = CoroutineScope(SupervisorJob())



    private val advertiseCallback = object : AdvertiseCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            serviceRepository.updateServiceMeta(
                meta = BLEAdvertisementMeta.Advertising(
                    advertisingName = currentIntent.getBLEName(),
                )
            )
            printLog("BLE advertising started successfully.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            printLog("BLE advertising failed with error code: $errorCode")
            serviceRepository.updateServiceMeta(
                meta = BLEAdvertisementMeta.AdvertisementFailed(
                    advertisingName = currentIntent.getBLEName()
                )
            )
            stopSelf()
        }
    }

    companion object {
        const val ACTION_START = "com.phonepe.ble.START_ADVERTISING"
        const val ACTION_STOP = "com.example.ble.STOP_ADVERTISING"
        const val BLE_NAME = "ADVERTISING_DEVICE_NAME"
        private const val NOTIFICATION_CHANNEL_ID = "BleAdvertisingChannel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        collectTheBleNameChangeEvent()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentIntent = intent
        when (intent?.action) {
            ACTION_START -> {
                printLog("Received start command")
                goForeground()
                val name = intent.getBLEName()
                startBleAdvertising(name)
            }

            ACTION_STOP -> {
                printLog("Received stop command")
                stopBleAdvertising()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        printLog("Service is being destroyed")
        serviceRepository.updateServiceMeta(meta = BLEAdvertisementMeta.Stopped)
        stopBleAdvertising()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun goForeground() {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) and higher
            // For Android 14 (API 34) and higher, specify the foreground service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE // Or LOCATION, etc.
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startBleAdvertising(name: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            printLog("BLUETOOTH_ADVERTISE permission not granted. Cannot start advertising.")
            stopSelf()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            printLog("Bluetooth is not enabled. Cannot start advertising.")
            stopSelf()
            return
        }

        bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            print("Failed to create a BluetoothLeAdvertiser.")
            stopSelf()
            return
        }

        val settings = buildAdvertiseSettings()
        val data = buildAdvertiseData()
        val setName = bluetoothAdapter.setName(name)
        printLog("The set name $setName")
        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopBleAdvertising() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            printLog(
                "BLUETOOTH_ADVERTISE permission not granted. Cannot guarantee advertiser stop."
            )
            return
        }
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        bleAdvertiser = null
        printLog("BLE advertising stopped.")
        serviceRepository.updateServiceMeta(meta = BLEAdvertisementMeta.Stopped)
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setDiscoverable(true)
                }
            }
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
    }

    private fun buildAdvertiseData(): AdvertiseData {
        val puId = ParcelUuid(UUID.fromString(BLEConsts.SERVICE_UUID))
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .apply {
                if (BLEConsts.ENABLE_SERVICE_FILTER) {
                    addServiceUuid(puId)
                }
            }
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "BLE Advertising Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for the BLE advertising foreground service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val snoozeIntent: Intent =
            Intent(this@BLEAdvertiseService, BLEAdvertiseService::class.java).apply {
                action = ACTION_STOP
            }
        val pendingIntent = PendingIntent.getService(
            this, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE Advertiser Active")
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Advertising",
                pendingIntent
            )
            .setContentText("This device is advertising its name via Bluetooth.")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun collectTheBleNameChangeEvent() {
        serviceScope.launch {
            systemStateReceiver.systemStateUpdate.collect {
                when (it) {
                    BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED -> {
                        printLog("Bluetooth local name changed, restarting advertising")
                        if (ActivityCompat.checkSelfPermission(
                                this@BLEAdvertiseService,
                                Manifest.permission.BLUETOOTH_ADVERTISE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return@collect
                        }

                        bleAdvertiser?.stopAdvertising(advertiseCallback)
                        val settings = buildAdvertiseSettings()
                        val data = buildAdvertiseData()
                        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
                    }
                }
            }
        }
    }

    fun Intent?.getBLEName(): String {
        return this?.getStringExtra(BLE_NAME) ?: "Default BLE Device"
    }
}