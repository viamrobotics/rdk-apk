package com.viam.rdk.fgservice

import android.app.Activity
import android.content.Intent
import android.util.Log

private const val TAG = "RDKLaunch"

// todo: disable lint-baseline.xml entries related to API 28 + fix
class RDKLaunch : Activity(){
    override fun onStart() {
        super.onStart()
        // todo: prompt for necessary permissions first
        startForegroundService(Intent(this, RDKForegroundService::class.java))
        Log.i(TAG, "started RDK service")
        finishAndRemoveTask()
    }
}
