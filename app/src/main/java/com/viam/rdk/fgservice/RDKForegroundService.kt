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
import androidx.compose.runtime.mutableStateOf
import droid.Droid.mainEntry
import droid.Droid.droidStopHook
import java.io.File
import java.nio.file.StandardWatchEventKinds
import java.util.Timer
import java.util.TimerTask
import kotlin.io.path.exists


private const val TAG = "RDKForegroundService"
private const val FOREGROUND_NOTIF_ID = 1

/** returns list of missing perms. empty means all granted */
fun missingPerms(context: Context, perms: Array<String>): Array<String> {
    return perms.filter(fun (perm: String): Boolean {
        return context.packageManager.checkPermission(perm, context.packageName) == PackageManager.PERMISSION_DENIED
    }).toTypedArray()
}

enum class RDKStatus {
    STOPPED, WAIT_CONFIG, WAIT_PERMISSION, RUNNING, STOPPING, UNSET;

    fun restartable(): Boolean {
        return this == STOPPING || this == STOPPED
    }

    fun stoppable(): Boolean {
        return this == RUNNING || this == WAIT_CONFIG || this == WAIT_PERMISSION
    }
}

class RDKThread() : Thread() {
    lateinit var filesDir: java.io.File
    lateinit var context: Context
    lateinit var confPath: String
    var waitPerms: Boolean = true
    var status: RDKStatus = RDKStatus.STOPPED

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
        status = RDKStatus.WAIT_PERMISSION
        if (waitPerms) {
            permissionLoop()
        } else {
            Log.i(TAG, "waitPerms = false, skipping permissionLoop")
        }
        status = RDKStatus.WAIT_CONFIG
        val path = File(confPath)
        val dirPath = path.parentFile?.toPath()
        if (dirPath == null) {
            Log.i(TAG, "confPath $confPath parentFile is null")
            return
        }
        dirPath.fileSystem.newWatchService().use {
            while (!path.exists()) {
                Log.i(TAG, "waiting for viam.json at $path")
                dirPath.register(it, arrayOf(StandardWatchEventKinds.ENTRY_CREATE))
                it.take()
            }
        }
        Log.i(TAG, "found $path")
        if (!path.canRead()) {
            Log.e(TAG, "can't read path at $path, bailing")
            // todo: communicate this in UX as state
            return
        }
        // todo: I think we crash the entire process if the viam.json config fails to parse; be more graceful
        try {
            status = RDKStatus.RUNNING
            mainEntry(path.toString(), filesDir.toString())
        } catch (e: Exception) {
            Log.e(TAG, "viam thread caught error $e")
        } finally {
            Log.i(TAG, "finished viam thread")
        }
        status = RDKStatus.STOPPED
    }
}

class RDKBinder : Binder() {}

var singleton: RDKForegroundService? = null

class RDKForegroundService : Service() {
    final val thread = RDKThread()
    var timer: Timer? = null

    override fun onBind(intent: Intent): IBinder {
        return RDKBinder()
    }

    override fun onCreate() {
        super.onCreate()
        singleton = this
        val chan = NotificationChannel("background", "background", NotificationManager.IMPORTANCE_HIGH)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        val notif = Notification.Builder(this, chan.id).setContentText("The RDK is running in the background").setSmallIcon(R.mipmap.ic_launcher).build()
        this.startForeground(FOREGROUND_NOTIF_ID, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread.filesDir = cacheDir
        thread.context = applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        // todo: can just set these values directly, don't need to do through prefs
        thread.confPath = prefs.getString("confPath", defaultConfPath) ?: defaultConfPath
        thread.waitPerms = prefs.getBoolean("waitPerms", true)
        Log.i(TAG, "got confPath ${thread.confPath}")
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    // trigger RDK stop, destroy this service when done
    fun stopAndDestroy() {
        thread.status = RDKStatus.STOPPING
        droidStopHook()
        if (timer == null) {
            timer = Timer()
            timer?.schedule(StopTimer(), 0, 250)
        }
    }

    inner class StopTimer : TimerTask() {
        override fun run() {
            if (thread.status == RDKStatus.STOPPED) {
                Log.i(TAG, "StopTimer found STOPPED")
                cancel()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        singleton = null
        Log.i(TAG, "service destroyed")
    }
}
