package com.crearo.openbot

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.crearo.openbot.controller.ControlEventForCar
import com.crearo.openbot.controller.Dpad
import com.crearo.openbot.controller.DpadState

class MainActivity : AppCompatActivity() {

    private val dpad = Dpad()
    private lateinit var tvInfo: TextView
    private lateinit var tvConnected: TextView
    private val dpadState = DpadState(0f, 0f, 0f, 0f)
    private val controlEventForCar = ControlEventForCar(0f, 0f)
    private val usbConnection = UsbConnection(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvInfo = findViewById(R.id.tv_info)
        tvConnected = findViewById(R.id.tv_connected)
    }

    override fun onResume() {
        super.onResume()
        usbConnection.startUsbConnection()
    }

    override fun onPause() {
        super.onPause()
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
        // textView.text = "$dpadState\n${textView.text}"
        controlEventForCar.onDpadEvent(dpadState)
        tvInfo.text = "$controlEventForCar"
    }
}
