package com.viam.rdk.fgservice

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
fun StateViewer(status: RDKStatus) {
    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier
        .fillMaxWidth()
        .height(30.dp)) {
        when (status) {
            RDKStatus.STOPPING -> CircularProgressIndicator(modifier = Modifier
                .height(20.dp)
                .width(20.dp))
            RDKStatus.RUNNING -> Text("\uD83C\uDD99")
            RDKStatus.WAIT_PERMISSION -> Text("⏳\uD83D\uDD12")
            RDKStatus.WAIT_CONFIG -> Text("⏳⚙\uFE0F")
            RDKStatus.STOPPED -> Text("\uD83D\uDED1")
            RDKStatus.UNSET -> Text("❓")
        }
        Spacer(Modifier.width(10.dp))
        Text("service state ${status.name}", modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScaffold(activity: RDKLaunch) {
    var fullJson by rememberSaveable() {
        mutableStateOf("")
    }
    var idValue by rememberSaveable() {
        mutableStateOf("")
    }
    var secretValue by rememberSaveable() {
        mutableStateOf("")
    }
    val bgState by rememberSaveable {
        activity.bgState
    }
    val mono = TextStyle.Default.copy(fontFamily = FontFamily.Monospace)
    val textMod = Modifier
        .border(1.dp, Color.Black)
        .fillMaxWidth()
        .padding(5.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viam RDK") },
                actions = {
                    OutlinedButton(onClick = { activity.hardRestart() }, enabled = bgState.restartable()) { Text("Restart") }
                    Spacer(Modifier.width(20.dp))
                    OutlinedButton(onClick = { singleton?.stopAndDestroy() }, enabled = bgState.stoppable()) { Text("Stop") }
                },
            )
        }
    ) {
            padding -> Column(modifier = Modifier
        .padding(padding)
        .padding(PaddingValues(horizontal = 10.dp))
        .verticalScroll(rememberScrollState())
            ) {
                StateViewer(bgState)
        PermissionsCard(activity)

        Spacer(Modifier.height(20.dp))

        Text("viam.json path", style=MaterialTheme.typography.titleMedium)
        Text(activity.confPath.value)
        Text("Using config ${jsonComment.value}", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        TabLayout(listOf("Load json", "ID + secret", "Paste json")) {
            Column {
                Button(onClick = { activity.savePref(defaultConfPath) }){
                    Text("Use default /sdcard/Download/viam.json")
                }
                Button(onClick = activity::openFile) {
                    Text("Load viam.json")
                }
            }

            Column {
                Text("ID and secret", style=MaterialTheme.typography.titleMedium)
                Text("ID", style=MaterialTheme.typography.titleSmall)
                BasicTextField(value = idValue, onValueChange = {idValue = it}, textStyle=mono, modifier = textMod)
                Text("secret", style=MaterialTheme.typography.titleSmall)
                BasicTextField(value = secretValue, onValueChange = {secretValue=it}, textStyle=mono, modifier = textMod)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { activity.setIdKeyConfig(idValue, secretValue) }) {
                        Text("Apply key + secret")
                    }
                    Button(onClick = { idValue = ""; secretValue = "" }) {
                        Text("Clear")
                    }
                }
            }

            Column {
                Text("Paste full json", style=MaterialTheme.typography.titleMedium)
                BasicTextField(
                    value=fullJson,
                    onValueChange={fullJson = it},
                    textStyle = mono,
                    minLines = 3,
                    modifier = textMod,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { activity.setPastedConfig(fullJson) }) {
                        Text("Apply pasted config")
                    }
                    Button(onClick = { fullJson = "" }) {
                        Text("Clear")
                    }
                }
            }
        }
    }
    }
}
