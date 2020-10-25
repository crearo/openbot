package com.crearo.openbot

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED
import android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.crearo.openbot.controller.ControlEventForCar
import com.crearo.openbot.controller.Dpad
import com.crearo.openbot.controller.DpadState
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val dpad = Dpad()
    private lateinit var tvInfo: TextView
    private lateinit var tvUsbConnected: TextView
    private lateinit var tvControllerConnected: TextView
    private val dpadState = DpadState(0f, 0f, 0f, 0f)
    private val controlEventForCar = ControlEventForCar(0f, 0f)
    private lateinit var usbConnection: UsbConnection
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
            tvControllerConnected.text = "Controller Status: $device"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvInfo = findViewById(R.id.tv_info)
        tvUsbConnected = findViewById(R.id.tv_usb_connected)
        tvControllerConnected = findViewById(R.id.tv_controller_connected)
        usbConnection = UsbConnection(applicationContext)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val intentFilter = IntentFilter()
        intentFilter.addAction("connection")
        intentFilter.addAction("data")
        localBroadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runOnUiThread {
                    when (intent?.action) {
                        "connection" -> tvUsbConnected.text = intent.getStringExtra("action")
                    }
                }
            }
        }, intentFilter)

        findViewById<Button>(R.id.btn_pair).setOnClickListener { bondController() }
    }

    override fun onResume() {
        super.onResume()
        val bluetoothConnectionIntentFilter = IntentFilter()
        bluetoothConnectionIntentFilter.addAction(ACTION_ACL_CONNECTED)
        bluetoothConnectionIntentFilter.addAction(ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothBroadcastReceiver, bluetoothConnectionIntentFilter)
        usbConnection.startListener()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothBroadcastReceiver)
        usbConnection.stopListener()
        usbConnection.stopUsbConnection()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (!dpad.isDpadDevice(event)) {
            return super.onGenericMotionEvent(event)
        }
        val xAxis: Float = event!!.getAxisValue(MotionEvent.AXIS_X)
        dpadState.xAxis = xAxis
        onDpadEvent(dpadState)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!dpad.isDpadDevice(event) || event!!.repeatCount != 0) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_C -> dpadState.updateThrottle(0f, 1f, 0f)
            KeyEvent.KEYCODE_BUTTON_X -> dpadState.updateThrottle(1f, 0f, 0f)
            KeyEvent.KEYCODE_BUTTON_A -> dpadState.updateThrottle(0f, 0f, 1f)
        }
        onDpadEvent(dpadState)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!dpad.isDpadDevice(event) || event!!.repeatCount != 0) {
            return super.onKeyUp(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_C -> dpadState.gas = 0f
            KeyEvent.KEYCODE_BUTTON_X -> dpadState.reverse = 0f
            KeyEvent.KEYCODE_BUTTON_A -> dpadState.brake = 0f
        }
        onDpadEvent(dpadState)
        return true
    }

    private fun onDpadEvent(dpadState: DpadState) {
        controlEventForCar.onDpadEvent(dpadState)
        tvInfo.text = "$controlEventForCar"
        if (!usbConnection.isBusy()) {
            usbConnection.send("${controlEventForCar.left},${controlEventForCar.right}\n")
        }
    }

    private fun bondController() {
        val bluetoothDevice = getPairedProController().also { tvControllerConnected.text = "Found Device: ${it.isPresent}" }
        if (bluetoothDevice.isPresent) {
            connectBluetoothDevice(bluetoothDevice.get())
        }
    }

    private fun getPairedProController(): Optional<BluetoothDevice> {
        // 59:C7:BE:FB:00:A2
        return Optional.ofNullable(bluetoothAdapter.getRemoteDevice("59:C7:BE:FB:00:A2"))
//        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
//        return pairedDevices.stream().filter { device: BluetoothDevice -> device.name.contains("Pro Controller") }.findFirst()
    }

    private fun connectBluetoothDevice(bluetoothDevice: BluetoothDevice) {
        /*        val gatt = bluetoothDevice.connectGatt(this, true, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.d(TAG, "ConnectionState: $status $newState")
            }
        })

        gatt.connect()*/

        val socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.randomUUID())
        if (bluetoothAdapter.isDiscovering) {
            tvControllerConnected.text = "Discovering right now, try again later"
            return
        }
        if (!socket.isConnected) {
            tvControllerConnected.text = "Attempting connection"
            try {
                socket.connect()
            } catch (e: IOException) {
                tvControllerConnected.text = "Failed to connect ${e.message}"
            }
        }
    }
}
