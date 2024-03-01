package com.viam.rdk.fgservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import droid.Droid.mainEntry
import java.io.File
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.exists


private const val TAG = "RDKForegroundService"
private const val FOREGROUND_NOTIF_ID = 1

/** returns list of missing perms. empty means all granted */
fun missingPerms(context: Context, perms: Array<String>): Array<String> {
    return perms.filter(fun (perm: String): Boolean {
        return context.packageManager.checkPermission(perm, context.packageName) == PackageManager.PERMISSION_DENIED
    }).toTypedArray()
}

class RDKThread() : Thread() {
    lateinit var filesDir: java.io.File
    lateinit var context: Context
    lateinit var confPath: String

    /** wait for necessary permissions to be granted */
    fun permissionLoop() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions
        var counter = 0
        while (true) {
            val missing = missingPerms(context, perms)
            if (missing.isEmpty()) {
                Log.i(TAG, "All permissions granted, starting")
                break
            }
            if (counter % 10 == 0) {
                Log.i(TAG, "Some permissions are missing, waiting to start: ${missing.joinToString()}")
            }
            counter += 1
            sleep(1000)
        }
    }

    override fun run() {
        super.run()
        permissionLoop()
        val path = File(confPath)
        val dirPath = path.parentFile?.toPath()
        if (dirPath == null) {
            Log.i(TAG, "confPath $confPath parentFile is null")
            return
        }
        val watcher = dirPath.fileSystem.newWatchService()
        while (!path.exists()) {
            Log.i(TAG, "waiting for viam.json at $path")
            dirPath.register(watcher, arrayOf(StandardWatchEventKinds.ENTRY_CREATE))
            watcher.take()
        }
        watcher.close()
        Log.i(TAG, "found $path")
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
        val notif = Notification.Builder(this, chan.id).setContentText("The RDK is running in the background").setSmallIcon(R.mipmap.ic_launcher).build()
        this.startForeground(FOREGROUND_NOTIF_ID, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread.filesDir = cacheDir
        thread.context = applicationContext
        thread.confPath = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("confPath", defaultConfPath) ?: defaultConfPath
        Log.i(TAG, "got confPath ${thread.confPath}")
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // todo: figure out how to stop thread -- need to send exit command to RDK via exported API
        super.onDestroy()
    }
}
