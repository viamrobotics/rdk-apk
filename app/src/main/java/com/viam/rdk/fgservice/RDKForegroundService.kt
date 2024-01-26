package com.viam.rdk.fgservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import droid.Droid.mainEntry

class RDKThread : Thread() {
    override fun run() {
        super.run()
        mainEntry()
    }
}

class RDKForegroundService : Service() {
     private final val thread = RDKThread()
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // todo: figure out how to stop thread
        super.onDestroy()
    }
}