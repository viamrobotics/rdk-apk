package com.viam.rdk.fgservice

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private const val TAG = "TabLayout"

@Composable
fun TabRow(selected: MutableState<String?>, tabNames: List<String>) {
    if (selected.value == null) selected.value = tabNames[0]
    val selectedBorder = BorderStroke(1.dp, Color.Black)
    Row {
        tabNames.map { name ->
            TextButton(onClick = { selected.value = name }, border = if (selected.value == name) selectedBorder else null) {
                Text(name, textDecoration = if (selected.value == name) TextDecoration.Underline else TextDecoration.None)
            }
        }
    }
    Divider()
}

// TabLayout places contents in a tab view
@Composable
fun TabLayout(tabNames: List<String>, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var curIndex by rememberSaveable() {
        mutableIntStateOf(0)
    }
    TabRow(selectedTab, tabNames)
    Layout(modifier = modifier, content = content) {measurables, constraints ->
        if (tabNames.size != measurables.size) {
            Log.w(TAG, "tabNames.size != measurables.size -- (${tabNames.size}, ${measurables.size})")
            layout(constraints.maxWidth, 0) {}
        }  else if (tabNames.isEmpty()) {
            Log.w(TAG, "tabNames is empty")
            layout(constraints.maxWidth, 0) {}
        } else {
            Log.i(TAG, "tab layout ${measurables.size}, $constraints")
            val selIndex = tabNames.indexOf(selectedTab.value ?: tabNames[0]) // yup can be -1 and explode
            val placeable = measurables[selIndex].measure(constraints)
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
    }
}
