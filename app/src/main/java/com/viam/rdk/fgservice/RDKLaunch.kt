package com.viam.rdk.fgservice

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Timer
import java.util.TimerTask
import com.jakewharton.processphoenix.ProcessPhoenix
import java.net.URLDecoder
import java.nio.charset.Charset


private const val TAG = "RDKLaunch"
val defaultConfPath = Environment.getExternalStorageDirectory().toPath().resolve("Download/viam.json").toString()
val selectedTab = mutableStateOf<String?>(null)
val jsonComments = mapOf(
    "default" to "from default path in downloads",
    "id-secret" to "from ID + secret",
    "pasted" to "from pasted JSON",
)
val jsonComment = mutableStateOf(jsonComments["default"])

fun serviceRunning(ctx: Context): Boolean {
    val manager = ctx.getSystemService(ComponentActivity.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (RDKForegroundService::class.java.name == service.service.className) {
            return true
        }
    }
    return false
}

fun maybeStart(ctx: Context) {
    if (!serviceRunning(ctx)) {
        ctx.startService(Intent(ctx, RDKForegroundService::class.java))
        Log.i(TAG, "started RDK service")
    } else {
        Log.i(TAG, "not starting service, already running")
    }
}

fun maybeStop(ctx: Context) {
    if (singleton != null) {
        singleton?.stopAndDestroy()
    } else {
        Log.i(TAG, "not stopping service, already down")
    }
}

// todo: disable lint-baseline.xml entries related to API 28 + fix
class RDKLaunch : ComponentActivity(){
    // todo: persist this please
    val confPath = mutableStateOf(defaultConfPath)
    val perms = mutableStateOf(mapOf<String, Boolean>())
    private lateinit var timer: Timer;
    val bgState = mutableStateOf(RDKStatus.STOPPED)

    inner class CheckService() : TimerTask() {
        override fun run() {
            val newVal = singleton?.thread?.status ?: RDKStatus.STOPPED
            if (newVal != bgState.value) {
                Log.i(TAG, "bgState transition ${bgState.value} -> $newVal")
            }
            bgState.value = newVal
        }
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.i(TAG, "ONCREATE!!!")
    }

    override fun onStart() {
        super.onStart()
        confPath.value = PreferenceManager.getDefaultSharedPreferences(this).getString("confPath", defaultConfPath) ?: defaultConfPath
        refreshPermissions()
        maybeStart(this)
        timer = Timer()
        timer.schedule(CheckService(), 1000, 1000)
        setContent {
            MyScaffold(this)
        }
    }

    // send open_document intent which we catch in onActivityResult + write to a json config
    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, 2)
        // see onActivityResult for the continuation of this
    }

    // write passed value to a json config
    fun setPastedConfig(value: String) {
        val path = filesDir.resolve("pasted.viam.json")
        writeString(value, path)
        savePref(path.toString())
        jsonComment.value = jsonComments["pasted"]
        // todo: signal bg service
        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
    }

    // destroy the process and start it again afterwards. killProcess removes Activity + Service.
    // necessary because we have no other way to clean up the RDK's in-memory state
    // (and we don't want to use a subprocess bc android)
    fun hardRestart() {
        ProcessPhoenix.triggerRebirth(this);
    }

    fun setIdKeyConfig(id: String, key: String) {
        val fullJson = """{"cloud":{"app_address":"https://app.viam.com:443","id":"$id","secret":"$key"}}"""
        val path = filesDir.resolve("id-secret.viam.json")
        writeString(fullJson, path)
        savePref(path.toString())
        jsonComment.value = jsonComments["id-secret"]
        // todo: signal bg service
        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()

    }

    // save viam.json path to shared preferences. used for persistence + to communicate w/ service
    fun savePref(value: String) {
        confPath.value = value
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putString("confPath", value)
        editor.apply()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "activity result $requestCode $resultCode $data")
        data?.data?.also { uri ->
            Log.i(TAG, "uri is $uri")
            if (DocumentsContract.isDocumentUri(this, uri)) {
                Log.i(TAG,"opening URI $uri")
                val fd = contentResolver.openFileDescriptor(uri, "r")
                try {
                    if (fd == null) {
                        Toast.makeText(this, "fd is null", Toast.LENGTH_SHORT).show()
                    } else {
                        val stream = FileInputStream(fd.fileDescriptor)
                        val path = filesDir.resolve("loaded.viam.json")
                        copyStream(stream, path)
                        savePref(path.toString())
                        // todo: signal bg service
                        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    fd?.close()
                }
                jsonComment.value = "loaded from ${formatUri(uri)}"
            } else {
                Toast.makeText(this, "not a document URI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // reload observable permission state
    fun refreshPermissions() {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val rperms = info.requestedPermissions
        val map = mutableMapOf<String, Boolean>()
        for (perm in rperms) {
            val pInfo = packageManager.getPermissionInfo(perm, 0)
            if (pInfo.protection == PermissionInfo.PROTECTION_DANGEROUS) {
                map[perm] = packageManager.checkPermission(perm, packageName) != PackageManager.PERMISSION_DENIED
            }
        }
        perms.value = map
    }
}

// copy opened input stream to target path
fun copyStream(stream: FileInputStream, dest: File) {
    val buf = ByteArray(5000)
    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(dest)
        var nbytes = stream.read(buf)
        while (nbytes > -1) {
            output.write(buf, 0, nbytes)
            nbytes = stream.read(buf)
        }
        output.flush()
    } finally {
        output?.close()
    }
}

fun writeString(value: String, path: File) {
    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(path)
        output.write(value.encodeToByteArray())
        output.flush()
    } finally {
        output?.close()
    }
}

fun formatUri(uri: Uri): String? {
    return uri.path?.split(":")?.last()
}
