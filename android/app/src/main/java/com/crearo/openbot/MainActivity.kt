package com.crearo.openbot

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.crearo.openbot.controller.Dpad
import kotlin.math.round

class MainActivity : AppCompatActivity() {

    private val dpad = Dpad()
    private lateinit var textView: TextView

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
        addLog("x: ${round(xAxis * 10)}, y: ${round(yAxis * 10)}")
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!dpad.isDpadDevice(event)) {
            return super.onKeyDown(keyCode, event)
        }
        addLog(KeyEvent.keyCodeToString(keyCode))
        return true
    }

    private fun addLog(log: String) {
        textView.text = "${log}\n${textView.text}"
    }
}
