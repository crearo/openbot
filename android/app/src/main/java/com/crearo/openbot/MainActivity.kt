package com.crearo.openbot

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.crearo.openbot.controller.ControllerState
import com.crearo.openbot.controller.Dpad

class MainActivity : AppCompatActivity() {

    private val dpad = Dpad()
    private lateinit var textView: TextView
    private val controllerState = ControllerState(0f, 0f, 0f, 0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textview)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (!dpad.isDpadDevice(event)) {
            return super.onGenericMotionEvent(event)
        }
        val xAxis: Float = event!!.getAxisValue(MotionEvent.AXIS_X)
        val yAxis: Float = event.getAxisValue(MotionEvent.AXIS_Y)
        // addLog("x: ${round(xAxis * 10) / 10f}, y: ${round(yAxis * 10) / 10f}")
        controllerState.xAxis = xAxis
        addLog(controllerState.toString())
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!dpad.isDpadDevice(event) || event!!.repeatCount != 0) {
            return super.onKeyDown(keyCode, event)
        }
        // addLog("Down: ${KeyEvent.keyCodeToString(keyCode)}")
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_C -> controllerState.updateThrottle(0f, 1f, 0f)
            KeyEvent.KEYCODE_BUTTON_X -> controllerState.updateThrottle(1f, 0f, 0f)
            KeyEvent.KEYCODE_BUTTON_A -> controllerState.updateThrottle(0f, 0f, 1f)
        }
        addLog(controllerState.toString())
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!dpad.isDpadDevice(event) || event!!.repeatCount != 0) {
            return super.onKeyUp(keyCode, event)
        }
        // addLog("Up: ${KeyEvent.keyCodeToString(keyCode)}")
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_C -> controllerState.gas = 0f
            KeyEvent.KEYCODE_BUTTON_X -> controllerState.reverse = 0f
            KeyEvent.KEYCODE_BUTTON_A -> controllerState.brake = 0f
        }
        addLog(controllerState.toString())
        return true
    }

    private fun addLog(log: String) {
        textView.text = "${log}\n${textView.text}"
    }
}
