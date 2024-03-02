package com.viam.rdk.fgservice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsCard(activity: RDKLaunch) {
    // todo: onClick navigate to app permissions
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Permissions", style=MaterialTheme.typography.titleMedium)
                IconButton(onClick = activity::refreshPermissions) {
                    Icon(Icons.Outlined.Refresh, "Refresh")
                }
            }
            activity.perms.value.forEach {
                entry -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (entry.value) Icons.Outlined.Check else Icons.Outlined.Clear, "check")
                    Text(entry.key.split("\\.".toRegex()).last())
                }
            }
        }
    }
}
