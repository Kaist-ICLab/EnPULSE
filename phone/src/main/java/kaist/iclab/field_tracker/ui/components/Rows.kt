package kaist.iclab.field_tracker.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.SensorUIModel
import kaist.iclab.field_tracker.ui.components.userInputs.DurationInput
import kaist.iclab.field_tracker.ui.components.userInputs.RadioButtonGroup
import kaist.iclab.field_tracker.ui.theme.MainTheme
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.core.SensorState

@Composable
fun BaseRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    tail: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (tail != null) Arrangement.SpaceBetween else Arrangement.Start
    ) {
        Column(
            verticalArrangement = if (subtitle != null) Arrangement.spacedBy(2.dp) else Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDivider) {
                VerticalDivider(
                    modifier = Modifier
                        .height(14.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            tail?.let {
                it()
            }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    switchStatus: SwitchStatus,
    onClick: (() -> Unit)? = null,
) {
    BaseRow(title, subtitle, onClick) {
        BasicSwitch(switchStatus)
    }
}

@Composable
private fun BaseActionableModalRow(
    title: String,
    subtitle: String? = null,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    child: @Composable () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    BaseRow(title, subtitle, showDivider = true) {
        IconButton(
            modifier = Modifier.size(48.dp),
            enabled = enabled,
            onClick = { showDialog = true }
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else .5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
    BaseAlertDialog(
        showDialog,
        title,
        onDismiss = { showDialog = false },
        onConfirm = {
            showDialog = false
            onConfirm()
        }
    ) { child() }
}

@Composable
fun SelectOptionModalRow(
    title: String,
    currOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var changedOption by remember { mutableStateOf(currOption) }
    BaseActionableModalRow(
        title = title,
        subtitle = currOption,
        onConfirm = { onOptionSelected(changedOption) },
        enabled = enabled
    ) {
        RadioButtonGroup(
            options = options,
            selectedOption = changedOption,
            onOptionSelected = {
                changedOption = it
            }
        )
    }
}

@Composable
fun DurationInputModalRow(
    title: String,
    curValue: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true
) {

    var changedValue by remember { mutableStateOf(curValue) }
    BaseActionableModalRow(
        title = title,
        subtitle = curValue,
        onConfirm = { onValueChanged(changedValue) },
        enabled = enabled
    ) {
        DurationInput(
            value = changedValue,
            onValueChanged = { changedValue = it }
        )
    }
}

@Composable
fun NavigationRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    BaseRow(title, subtitle, showDivider = true, onClick = onClick) {
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onClick
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun SensorSwitchRow(
    sensor: SensorUIModel,
    onClick: (() -> Unit) = {}
//    title: String,
//    sensorState: SensorState,
//    enable: () -> Unit,
//    disable: () -> Unit,
//    onClick: () -> Unit = {}
) {
    SwitchRow(
        title,
        subtitle = when (sensorState.flag) {
            SensorState.FLAG.UNAVAILABLE -> sensorState.message
            SensorState.FLAG.ENABLED -> sensorState.message
            SensorState.FLAG.DISABLED -> null
            SensorState.FLAG.RUNNING -> "Tracker is running. Please turn off the tracker to change configuration."
            else -> null
        },
        switchStatus = SwitchStatus(
            isChecked = sensorState.flag in setOf(
                SensorState.FLAG.ENABLED,
                SensorState.FLAG.RUNNING
            ),
            onCheckedChange = {
                if (it) enable() else disable()
            },
            disabled = sensorState.flag in setOf(
                SensorState.FLAG.UNAVAILABLE,
                SensorState.FLAG.RUNNING,
            )
        ),
        onClick = onClick
    )
}
//                        val warning = Toast.makeText(LocalContext.current, "Please grant all permissions", Toast.LENGTH_SHORT)
//title = "Status",
//sensor = s
///*TODO: Should we remove this simple logic too? -> YES*/
//enable = {
//    Log.d("DataConfigScreen", "Enable")
//    val isGranted = permissionMap.filter { it.key in collector.permissions}
//        .all{ it.value == PermissionState.GRANTED }
//    if(isGranted){
//        sensor.enable()
//    }else{
//        warning.show()
//    }
//},
//disable = collector.disable

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun SettingRowPreview() {
    val switchStatus = SwitchStatus(
        isChecked = true,
        onCheckedChange = { },
        disabled = false
    )
    MainTheme {
        Column {
            SwitchRow("Location", "ready", switchStatus)
            SelectOptionModalRow(
                "Experiment Group",
                "Group A",
                listOf("None", "Group A", "Group B", "Group C"),
                onOptionSelected = { }
            )
            NavigationRow("Location", "go", {})
        }
    }
}