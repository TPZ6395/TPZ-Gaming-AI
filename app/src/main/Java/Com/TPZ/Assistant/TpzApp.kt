package com.tpz.assistant

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class TpzApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            TpzAutoGamer.stopLoop()
            System.gc()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    "TPZ Anti-Lag Shield recovered: ${throwable.localizedMessage ?: "Error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            TpzAutoGamer.stopLoop()
            System.gc()
        }
    }
}
