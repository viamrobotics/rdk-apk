package com.viam.rdk.fgservice

import android.preference.PreferenceManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsCard(activity: RDKLaunch) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    var checked by rememberSaveable() {
        mutableStateOf(prefs.getBoolean("waitPerms", true))
    }

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

            Row(Modifier.clickable(role= Role.Checkbox, onClick = {
                prefs.edit().putBoolean("waitPerms", !checked).apply(); checked = !checked
            }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { newChecked -> prefs.edit().putBoolean("waitPerms", newChecked).apply(); checked = newChecked })
                Text("Wait for all permissions before starting")
            }
        }
    }
}
