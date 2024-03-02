package com.viam.rdk.fgservice

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "RDKLaunch"
val defaultConfPath = Environment.getExternalStorageDirectory().toPath().resolve("Download/viam.json").toString()

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
    if (serviceRunning(ctx)) {
        ctx.stopService(Intent(ctx, RDKForegroundService::class.java))
    } else {
        Log.i(TAG, "not stopping service, already down")
    }
}

// todo: disable lint-baseline.xml entries related to API 28 + fix
class RDKLaunch : ComponentActivity(){
    // todo: persist this please
    val confPath = mutableStateOf(defaultConfPath)

    override fun onStart() {
        super.onStart()
        confPath.value = PreferenceManager.getDefaultSharedPreferences(this).getString("confPath", defaultConfPath) ?: defaultConfPath
        maybeStart(this)
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
    }

    // write passed value to a json config
    fun setPastedConfig(value: String) {
        val path = filesDir.resolve("pasted.viam.json")
        writeString(value, path)
        savePref(path.toString())
        // todo: signal bg service
        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
    }

    fun setIdKeyConfig(id: String, key: String) {
        val fullJson = """{"cloud":{"app_address":"https://app.viam.com:443","id":"$id","secret":"$key"}}"""
        val path = filesDir.resolve("id-secret.viam.json")
        writeString(fullJson, path)
        savePref(path.toString())
        // todo: signal bg service
        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
    }

    // save path to shared preferences. used for persistence + to communicate w/ service
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
        data?.data?.also {
            uri -> Log.i(TAG, "uri is $uri")
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
            } else {
                Toast.makeText(this, "not a document URI", Toast.LENGTH_SHORT).show()
            }
        }
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