package com.viam.rdk.fgservice

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import droid.Droid.mainEntry
import kotlin.io.path.exists
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.Path

private const val TAG = "RDKForegroundService"
// todo: use app's filesDir instead of this
private val CONFIG_DIR = Environment.getExternalStorageDirectory().toPath().resolve("Download")

class RDKThread : Thread() {
    // lateinit var filesDir: java.io.File
    override fun run() {
        super.run()
        // val dirPath = filesDir.toPath()
        val dirPath = CONFIG_DIR
        val path = dirPath.resolve("viam.json")
        while (!path.exists()) {
            Log.i(TAG, "waiting for viam.json at $path")
            val watcher = dirPath.fileSystem.newWatchService()
            dirPath.register(watcher, arrayOf(StandardWatchEventKinds.ENTRY_CREATE))
            watcher.take()
        }
        Log.i(TAG, "found $path, starting")
        mainEntry()
    }
}

class RDKForegroundService : Service() {
    private final val thread = RDKThread()
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // thread.filesDir = filesDir
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // todo: figure out how to stop thread -- need to send exit command to RDK via exported API
        super.onDestroy()
    }
}
