package com.crearo.openbot

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.AsyncTask
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets

class UsbConnection(val context: Context) {

    private val TAG = "UsbConnection"
    private val ACTION_USB_PERMISSION = "UsbConnection.USB_PERMISSION"

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)
    private val usbPermissionIntent = PendingIntent.getBroadcast(this.context, 0, Intent(ACTION_USB_PERMISSION), 0)
    private var connection: UsbDeviceConnection? = null
    private var serialDevice: UsbSerialDevice? = null
    private var busy = false
    private val baudRate = 115200
    private var buffer: String = ""

    private val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    startSerialConnection(usbDevice)
                } else {
                    Log.w(TAG, "Permission denied for device $usbDevice")
                    Toast.makeText(context, "USB Host permission is required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val usbDetachedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (device != null) {
                    Log.i(TAG, "USB device detached")
                    stopUsbConnection()
                }
            }
        }
    }

    private val callback = UsbReadCallback { data ->
        try {
            val dataUtf8 = String(data, StandardCharsets.UTF_8)
            buffer += dataUtf8
            var index: Int
            while (buffer.indexOf('\n').also { index = it } != -1) {
                val dataStr: String = buffer.substring(0, index).trim { it <= ' ' }
                buffer = if (buffer.length == index) "" else buffer.substring(index + 1)
                AsyncTask.execute { onSerialDataReceived(dataStr) }
            }
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Error receiving USB data", e)
        }
    }


    fun startUsbConnection(): Boolean {
        val usbPermissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        localBroadcastManager.registerReceiver(usbPermissionReceiver, usbPermissionFilter)
        // Detach events are sent as a system-wide broadcast
        val usbDetachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        localBroadcastManager.registerReceiver(usbDetachedReceiver, usbDetachedFilter)

        val connectedDevices: Map<String, UsbDevice> = usbManager.deviceList
        if (connectedDevices.isNotEmpty()) {
            for (usbDevice in connectedDevices.values) {
                Log.i(TAG, "Device found: " + usbDevice.deviceName)
                return if (usbManager.hasPermission(usbDevice)) {
                    startSerialConnection(usbDevice)
                } else {
                    usbManager.requestPermission(usbDevice, usbPermissionIntent)
                    Toast.makeText(context, "Please allow USB Host connection", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
        Log.w(TAG, "Could not start USB connection - No devices found")
        return false
    }

    private fun startSerialConnection(device: UsbDevice): Boolean {
        Log.i(TAG, "Ready to open USB device connection")
        connection = usbManager.openDevice(device)
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection)
        if (serialDevice == null) {
            Log.w(TAG, "Could not create Usb Serial Device")
            return false
        }
        return if (serialDevice!!.open()) {
            serialDevice!!.setBaudRate(baudRate)
            serialDevice!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
            serialDevice!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
            serialDevice!!.setParity(UsbSerialInterface.PARITY_NONE)
            serialDevice!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
            serialDevice!!.read(callback)
            Log.i(TAG, "Serial connection opened")
            localBroadcastManager.sendBroadcast(Broadcast.CONNECTED)
            true
        } else {
            Log.w(TAG, "Cannot open serial connection")
            localBroadcastManager.sendBroadcast(Broadcast.CONNECTION_FAILED)
            false
        }
    }

    private fun onSerialDataReceived(data: String) {
        Log.i(TAG, "Serial data received: $data")
        localBroadcastManager.sendBroadcast(Broadcast.DATA(data))
    }

    fun stopUsbConnection() {
        try {
            serialDevice?.close()
            connection?.close()
        } finally {
            serialDevice = null
            connection = null
        }
        localBroadcastManager.unregisterReceiver(usbPermissionReceiver)
        localBroadcastManager.unregisterReceiver(usbDetachedReceiver)
    }

    fun send(msg: String) {
        busy = true
        serialDevice?.write(msg.toByteArray(StandardCharsets.UTF_8))
        busy = false
    }


    fun isOpen(): Boolean {
        return connection != null
    }

    fun isBusy(): Boolean {
        return busy
    }

}