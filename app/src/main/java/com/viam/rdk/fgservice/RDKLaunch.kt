package com.viam.rdk.fgservice

import android.app.Activity
import android.content.Intent
import android.util.Log

private const val TAG = "RDKLaunch"

class RDKLaunch : Activity(){
    override fun onStart() {
        super.onStart()
        startForegroundService(Intent(this, RDKForegroundService::class.java))
        Log.i(TAG, "started RDK service")
    }
}
