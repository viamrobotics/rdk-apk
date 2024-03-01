package com.viam.rdk.fgservice

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val TAG = "RDKLaunch"

// todo: disable lint-baseline.xml entries related to API 28 + fix
class RDKLaunch : ComponentActivity(){
    override fun onStart() {
        super.onStart()
        // donotcommit -- restore startService
//        startService(Intent(this, RDKForegroundService::class.java))
        Log.i(TAG, "started RDK service")
        setContent {
            MyScaffold()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScaffold() {
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
        Button(onClick = { /*TODO*/ }) {
            Text("Load viam.json")
        }
        Text("Paste config", style=MaterialTheme.typography.titleMedium)
        BasicTextField(
            value=value,
            onValueChange={value = it},
            textStyle = TextStyle.Default.copy(fontFamily = FontFamily.Monospace),
            minLines = 3,
            modifier = Modifier.border(1.dp, Color.Black).fillMaxWidth(),
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
