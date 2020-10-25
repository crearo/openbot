package com.crearo.openbot.controller

import java.io.Serializable
import kotlin.math.round

class DpadState(xAxis: Float, var reverse: Float, var gas: Float, var brake: Float) : Serializable {

    var xAxis: Float = xAxis
        set(value) {
            field = round(value * 10) / 10f
        }

    fun updateThrottle(reverse: Float, gas: Float, brake: Float) {
        this.reverse = reverse
        this.gas = gas
        this.brake = brake
    }

    override fun toString(): String {
        return """
            gas: $gas, reverse: $reverse, brake: $brake
            xAxis: $xAxis
        """.trimIndent()
    }

}