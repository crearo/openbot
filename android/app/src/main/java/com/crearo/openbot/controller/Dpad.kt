package com.crearo.openbot.controller

import android.view.InputDevice
import android.view.InputEvent

class Dpad {

    fun isDpadDevice(event: InputEvent?): Boolean {
        return event != null && event.source and InputDevice.SOURCE_DPAD != InputDevice.SOURCE_DPAD
    }
}