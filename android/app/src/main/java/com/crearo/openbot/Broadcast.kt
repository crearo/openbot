package com.crearo.openbot

import android.content.Intent

class Broadcast {

    companion object {
        val CONNECTED: Intent = Intent("connection").putExtra("action", "connected")
        val DISCONNECTED: Intent = Intent("connection").putExtra("action", "disconnected")
        val CONNECTION_FAILED: Intent = Intent("connection").putExtra("action", "connection_failed")
        fun DATA(data: String): Intent {
            return Intent("data").putExtra("data", data)
        }
    }

}