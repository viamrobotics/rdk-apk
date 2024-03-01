package com.viam.rdk.fgservice

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


private const val TAG = "RDKLaunch"
const val defaultConfPath = "/sdcard/Download/viam.json"

// todo: disable lint-baseline.xml entries related to API 28 + fix
class RDKLaunch : ComponentActivity(){
    val confPath = mutableStateOf(defaultConfPath)
    override fun onStart() {
        super.onStart()
        // donotcommit -- restore startService
//        startService(Intent(this, RDKForegroundService::class.java))
        Log.i(TAG, "started RDK service")
        setContent {
            MyScaffold(this::openFile, confPath)
        }
    }

    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, 2)
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
                        val path = this.filesDir.resolve("loaded.viam.json")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScaffold(openFile: ()->Unit, confPath: MutableState<String>) {
    var value by rememberSaveable() {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viam RDK") },
                actions = {
                    // todo: confirmation dialog for restart button
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Refresh, "Restart")
                    }
                },
            )
        }
    ) {
        padding -> Column(modifier = Modifier
        .padding(padding)
        .padding(PaddingValues(horizontal = 10.dp))) {
            PermissionsCard()
        Text("viam.json path", style=MaterialTheme.typography.titleMedium)
        Text(confPath.value)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = openFile) {
                Text("Load viam.json")
            }
            Button(onClick = { confPath.value = defaultConfPath }){
                Text("Default viam.json")
            }
        }

        Text("Paste config", style=MaterialTheme.typography.titleMedium)
        BasicTextField(
            value=value,
            onValueChange={value = it},
            textStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
            minLines = 3,
            modifier = Modifier
                .border(1.dp, Color.Black)
                .fillMaxWidth(),
        )
        Button(onClick = { /*TODO*/ }) {
            Text("Apply config")
        }
    }
    }
}

@Composable
fun PermissionsCard() {
    // todo: onClick navigate to app permissions
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text("Permissions", style=MaterialTheme.typography.titleMedium)
            // todo: make permissions dynamic
            Row {
                Icon(Icons.Outlined.Check, "check")
                Text("perm1")
            }
            Row {
                Icon(Icons.Outlined.Check, "check")
                Text("perm2")
            }
            Row {
                Icon(Icons.Outlined.Check, "check")
                Text("perm3")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Android")
}
