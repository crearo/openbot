package com.crearo.openbot.controller

data class ControlEventForCar(var left: Float, var right: Float) {

    fun onDpadEvent(dpadState: DpadState) {
        if (dpadState.gas == 1f) {
            left = mapToFirmware(1f)
            right = mapToFirmware(1f)
        } else if (dpadState.gas == 0f) {
            left = mapToFirmware(0f)
            right = mapToFirmware(0f)
        }

        // todo this is fragile af.
        if (dpadState.xAxis > 0f) {
            right *= 0.8f
        } else if (dpadState.xAxis < 0f) {
            left *= 0.8f
        }
    }

    private fun mapToFirmware(value: Float): Float {
        return value * 120f
    }

}