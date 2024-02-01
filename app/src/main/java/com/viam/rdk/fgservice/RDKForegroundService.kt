package com.viam.rdk.fgservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import droid.Droid.mainEntry
import kotlin.io.path.exists
import java.nio.file.StandardWatchEventKinds
import android.net.ConnectivityManager
import android.system.Os

private const val TAG = "RDKForegroundService"
// todo: use app's filesDir instead of this
private val CONFIG_DIR = Environment.getExternalStorageDirectory().toPath().resolve("Download")
private const val FOREGROUND_NOTIF_ID = 1

class RDKThread : Thread() {
    lateinit var filesDir: java.io.File
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
        try {
            mainEntry(path.toString(), filesDir.toString())
        } catch (e: Exception) {
            Log.e(TAG, "viam thread caught error $e")
        } finally {
            Log.i(TAG, "finished viam thread")
        }
    }
}

class RDKBinder : Binder() {}

class RDKForegroundService : Service() {
    private final val thread = RDKThread()
    override fun onBind(intent: Intent): IBinder {
        return RDKBinder()
    }

    override fun onCreate() {
        super.onCreate()
        val chan = NotificationChannel("background", "background", NotificationManager.IMPORTANCE_HIGH)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        val notif = Notification.Builder(this, chan.id).setContentTitle("Viam RDK").setContentText("The RDK is running in the background").setSmallIcon(R.mipmap.ic_launcher).build()
        this.startForeground(FOREGROUND_NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread.filesDir = filesDir
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // todo: figure out how to stop thread -- need to send exit command to RDK via exported API
        super.onDestroy()
    }
}
