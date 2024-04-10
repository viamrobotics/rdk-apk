package com.viam.rdk.fgservice

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.jakewharton.processphoenix.ProcessPhoenix
import dalvik.system.PathClassLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Timer
import java.util.TimerTask
import java.util.function.Supplier
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


private const val TAG = "RDKLaunch"
val defaultConfPath = Environment.getExternalStorageDirectory().toPath().resolve("Download/viam.json").toString()
val selectedTab = mutableStateOf<String?>(null)
val jsonComments = mapOf(
    "default" to "from default path in downloads",
    "id-secret" to "from ID + secret",
    "pasted" to "from pasted JSON",
)
val jsonComment = mutableStateOf(jsonComments["default"])
var moduleSecret: String? = null

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

class ModuleStartReceiver(var applicationContext: Context) : BroadcastReceiver() {

    val threads: MutableMap<String, Thread> = mutableMapOf()

    companion object {
        val START_ACTION = "com.viam.rdk.fgservice.START_MODULE"
        val STOP_ACTION = "com.viam.rdk.fgservice.STOP_MODULE"
    }

    override fun onReceive(contxt: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            ModuleStartReceiver.START_ACTION, ModuleStartReceiver.STOP_ACTION -> {
                if (intent.extras == null || !intent.extras!!.containsKey("secret")) {
                    Log.e(TAG, "${intent.action} called without secret")
                    return
                }
                val intentSecret = intent.getStringExtra("secret");
                if (!intentSecret.equals(moduleSecret)) {
                    Log.e(TAG, "${intent.action} called with incorrect secret")
                    return
                }
            }
            else -> {
                Log.e(TAG, "${intent.action} unsupported")
                return
            }
        }

        val entryPointClass = intent.getStringExtra("java_entry_point_class")!!;

        synchronized(threads) {
            if (intent.action.equals(STOP_ACTION)) {
                if (!threads.containsKey(entryPointClass)) {
                    return
                }
                threads[entryPointClass]!!.interrupt()
                threads.remove(entryPointClass)
                return
            }

            if (threads.containsKey(entryPointClass)) {
                // we didn't stop the last one somehow
                threads[entryPointClass]!!.interrupt()
                threads.remove(entryPointClass)
                return
            }

            val thread = Thread {
                // this is the loader for the module's jar and its associated libraries
                // Note: Shared libraries have not been tested yet; they may interfere with
                // this process' own loaded libraries.
                // The intent is broadcast from the module's executed shell script and contains:
                // secret - moduleSecret that this process creates
                // proc_file - the file to write the results (exit code) of the main entry point to
                // java_class_path - the class path to search for all classes related to the entry point
                // java_library_path - the library path for any libraries we may need (untested)
                // java_entry_point_class - the class containing the main function
                // java_entry_point_args - the args to invoke with

                // todo: we need to test on newer android versions what happens when
                //  a shared libraries is loaded, then this module is unloaded and reloaded again.
                //  The unloading of shared libraries happens at GC but we don't control that. We may
                //  need to try using a finalizer on the ClassLoaders such that we block
                //  future attempts to re-load the same module until GC has happened. This may
                //  require encouraging GC to happen.
                val loader =
                    PathClassLoader(
                        intent.getStringExtra("java_class_path"),
                        intent.getStringExtra("java_library_path"),
                        ClassLoader.getSystemClassLoader()
                    );
                val fakeCtxCls =
                    Class.forName("com.viam.sdk.android.module.fake.FakeContext", true, loader)
                val fakeCtxAccessibleField = fakeCtxCls.getDeclaredField("accessible")
                fakeCtxAccessibleField.isAccessible = true
                fakeCtxAccessibleField.set(fakeCtxCls, false)
                val modCls = Class.forName("com.viam.sdk.android.module.Module", true, loader)
                val parentContextField = modCls.getDeclaredField("parentContext")
                parentContextField.isAccessible = true
                parentContextField.set(modCls, Supplier {
                    applicationContext
                })
                val mainCls =
                    Class.forName(entryPointClass, true, loader)
                val mainMethod = mainCls.getDeclaredMethod("main", Array<String>::class.java)
                var exitCode = 0
                try {
                    // on the shell script side, we combine the args with IFS=\n
                    mainMethod.invoke(
                        null,
                        intent.getStringExtra("java_entry_point_args")!!.split("\n").toTypedArray()
                    )
                } catch (t: Throwable) {
                    if (t !is InterruptedException) {
                        Log.e(
                            TAG,
                            "error invoking main for " + intent.getStringExtra("java_entry_point_class"),
                            t
                        )
                        exitCode = 1
                    }
                } finally {
                    File(intent.getStringExtra("proc_file")).writeText(exitCode.toString())
                }
            }

            threads[entryPointClass] = thread
            thread.start()
        }
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @OptIn(ExperimentalEncodingApi::class)
    override fun onStart() {
        super.onStart()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        confPath.value = prefs.getString("confPath", defaultConfPath) ?: defaultConfPath
        jsonComment.value = prefs.getString("jsonComment", jsonComment.value)
        moduleSecret = prefs.getString("moduleSecret", moduleSecret)
        if (moduleSecret == null) {
            val secureRandom = SecureRandom()
            val bytes = ByteArray(64)
            secureRandom.nextBytes(bytes)
            Base64.encode(bytes)
            val secretKey = Base64.encode(bytes)
            prefs.edit().putString("moduleSecret", secretKey).apply()
            moduleSecret = secretKey
        }
        refreshPermissions()

        // todo: make it configurable from UI or json config
        val intentFilter = IntentFilter()
        intentFilter.addAction(ModuleStartReceiver.START_ACTION)
        intentFilter.addAction(ModuleStartReceiver.STOP_ACTION)
        applicationContext.registerReceiver(
            ModuleStartReceiver(applicationContext),
            intentFilter,
        )

        maybeStart(this)
        timer = Timer()
        timer.schedule(CheckService(), 1000, 1000)
        setContent {
            MyScaffold(this)
        }
    }

    fun setJsonComment(value: String) {
        jsonComment.value = value
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.edit().putString("jsonComment", value).apply() // todo: is this blocking?
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
        setJsonComment(jsonComments["pasted"]!!)
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
        setJsonComment(jsonComments["id-secret"]!!)
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
                setJsonComment("loaded from URI: ${formatUri(uri)}")
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

fun formatUri(uri: Uri): String {
    return "${uri.authority}${uri.path}"
}
