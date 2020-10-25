package com.crearo.openbot.controller

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlin.math.roundToInt

data class ControlEventForCar(var left: Float, var right: Float) {

    private var prevDpadState = DpadState(0f, 0f, 0f, 0f)
    private val controllerLooper = ControllerLooper(this, Looper.myLooper()!!)

    companion object {
        private const val GAS_STEP = 0.05f
    }

    /**
     * Receives updates from the controller. The controller merely tells us there is a change in
     * state. We should this discard the current looper we're in which is on a trajectory to
     * increment / decrement our throttle.
     **/
    fun onDpadEvent(dpadState: DpadState) {
        controllerLooper.removeMessages(ControllerLooper.UPDATE_STATE)
        controllerLooper.sendMessage(controllerLooper.updateStateMessage(dpadState, prevDpadState))
        prevDpadState = dpadState
    }

    private fun onLooperEvent(dpadState: DpadState, prevDpadState: DpadState) {
        if (dpadState.gas == 1f) {
            left = mapToFirmware(left + GAS_STEP)
            right = mapToFirmware(right + GAS_STEP)
        } else if (dpadState.gas == 0f) {
            left = mapToFirmware(left - GAS_STEP)
            right = mapToFirmware(right - GAS_STEP)
        }

        // todo this is fragile af.
        if (dpadState.xAxis > 0f) {
            left *= (1 - 0.8f * dpadState.xAxis)
        } else if (dpadState.xAxis < 0f) {
            right *= (1 + 0.8f * dpadState.xAxis)
        }
    }

    private fun mapToFirmware(value: Float): Float {
        return (value * -240f).coerceIn(-255f, 255f)
    }

    override fun toString(): String {
        return "ControlEventForCar(left=${left.roundToInt()}, right=${right.roundToInt()})"
    }

    private class ControllerLooper(val control: ControlEventForCar, looper: Looper) : Handler(looper) {
        companion object {
            const val UPDATE_STATE = 1
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UPDATE_STATE -> {
                    val dpadState = msg.data.getSerializable("dpadState") as DpadState
                    val prevDpadState = msg.data.getSerializable("prevDpadState") as DpadState
                    control.onLooperEvent(dpadState, prevDpadState)
                    sendMessageDelayed(updateStateMessage(dpadState, prevDpadState), 500)
                }
            }
        }

        fun updateStateMessage(dpadState: DpadState, prevDpadState: DpadState): Message {
            val bundle = Bundle()
            bundle.putSerializable("dpadState", dpadState)
            bundle.putSerializable("prevDpadState", prevDpadState)
            val message = Message()
            message.what = UPDATE_STATE
            message.data = bundle
            return message
        }
    }

}