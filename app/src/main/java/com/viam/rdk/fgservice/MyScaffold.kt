package com.viam.rdk.fgservice

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScaffold(activity: RDKLaunch) {
    var value by rememberSaveable() {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viam RDK") },
                actions = {
                    // todo: confirmation dialog for restart button
                    IconButton(onClick = { maybeStart(activity) }) {
                        Icon(Icons.Outlined.PlayArrow, "Start")
                    }
                    IconButton(onClick = { maybeStop(activity) }) {
                        Icon(Icons.Outlined.Clear, "Stop")
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
        Text(activity.confPath.value)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = activity::openFile) {
                Text("Load viam.json")
            }
            Button(onClick = { activity.savePref(defaultConfPath) }){
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
        Button(onClick = { activity.setPastedConfig(value) }) {
            Text("Apply config")
        }
    }
    }
}
