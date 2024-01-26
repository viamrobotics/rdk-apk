package com.viam.rdk.fgservice

import android.app.Activity
import android.content.Intent




class RDKLaunch : Activity(){
    override fun onStart() {
        super.onStart()
        startService(Intent(this, RDKForegroundService::class.java))
    }
}