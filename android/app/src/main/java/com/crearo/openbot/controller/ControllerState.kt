package com.crearo.openbot.controller

import kotlin.math.round

data class ControllerState(var xAxis: Float, var reverse: Float, var gas: Float, var brake: Float) {

    fun updateThrottle(reverse: Float, gas: Float, brake: Float) {
        this.reverse = reverse
        this.gas = gas
        this.brake = brake
    }

    override fun toString(): String {
        return """
            xAxis: ${round(xAxis * 10) / 10f},
            gas: $gas, reverse: $reverse, brake: $brake
        """.trimIndent()
    }

}