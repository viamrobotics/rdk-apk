package com.viam.rdk.fgservice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
