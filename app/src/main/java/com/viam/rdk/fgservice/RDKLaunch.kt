package com.viam.rdk.fgservice

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
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
        maybeStart(this)
        setContent {
            MyScaffold(this::openFile, this::setPastedConfig, confPath, this)
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
        var output: FileOutputStream? = null
        try {
            val path = filesDir.resolve("pasted.viam.json")
            output = FileOutputStream(path)
            output.write(value.encodeToByteArray())
            output.flush()
            confPath.value = path.toString()
            // todo: signal bg service
            Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
        } finally {
            output?.close()
        }
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
                var output: FileOutputStream? = null
                try {
                    if (fd == null) {
                        Toast.makeText(this, "fd is null", Toast.LENGTH_SHORT).show()
                    } else {
                        val stream = FileInputStream(fd.fileDescriptor)
                        // note: we'll OOM if this file is huge
                        val buf = ByteArray(5000)
                        val path = filesDir.resolve("loaded.viam.json")
                        output = FileOutputStream(path)
                        var nbytes = stream.read(buf)
                        while (nbytes > -1) {
                            output.write(buf, 0, nbytes)
                            nbytes = stream.read(buf)
                        }
                        output.flush()
                        confPath.value = path.toString()
                        // todo: signal bg service
                        Toast.makeText(this, "copied to $path", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    fd?.close()
                    output?.close()
                }
            } else {
                Toast.makeText(this, "not a document URI", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
