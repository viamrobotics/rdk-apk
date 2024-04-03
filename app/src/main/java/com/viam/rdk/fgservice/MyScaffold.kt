package com.viam.rdk.fgservice

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp

@Composable
fun StateViewer(status: RDKStatus) {
    val widget = when (status) {
        RDKStatus.STOPPING -> CircularProgressIndicator()
        RDKStatus.RUNNING -> Icon(Icons.Default.ArrowForward, "Running")
        RDKStatus.WAITING_TO_START -> Icon(Icons.Default.KeyboardArrowUp, "Starting")
        RDKStatus.STOPPED -> Icon(Icons.Default.KeyboardArrowDown, "Stopped")
    }
    Row {
        widget
        Text("bgState ${status.name}")
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
                    OutlinedButton(onClick = { activity.hardRestart() }, enabled = bgState == RDKStatus.STOPPED || bgState == RDKStatus.STOPPING) { Text("Restart") }
                    Spacer(Modifier.width(20.dp))
                    OutlinedButton(onClick = { singleton?.stopAndDestroy() }, enabled = bgState == RDKStatus.RUNNING || bgState == RDKStatus.WAITING_TO_START) { Text("Stop") }
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = activity::openFile) {
                Text("Load viam.json")
            }
            Button(onClick = { activity.savePref(defaultConfPath) }){
                Text("Default viam.json")
            }
        }

        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(20.dp))

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
