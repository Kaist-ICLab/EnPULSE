package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import kaist.iclab.tracker.sensor.microema.WatchOption

/**
 * Two large tap buttons for ≤2 options (Yes/No, binary choices).
 */
@Composable
internal fun TapButtonsInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onSelect: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Button(
                    onClick = { onSelect(option.display) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSelected) AccentBlue else DarkSurface
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = option.display,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Scrollable number picker (0–10 range).
 */
@Composable
internal fun NumberPickerInput(
    numberOptions: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onSelect: (String) -> Unit
) {
    val pickerState = rememberPickerState(
        initialNumberOfOptions = numberOptions.size,
        initiallySelectedOption = selectedIndex
    )

    LaunchedEffect(selectedIndex) {
        if (pickerState.selectedOption != selectedIndex) {
            pickerState.scrollToOption(selectedIndex)
        }
    }

    // Sync picker scroll → onValueChange
    LaunchedEffect(pickerState.selectedOption) {
        onIndexChanged(pickerState.selectedOption)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Picker(
            state = pickerState,
            modifier = Modifier.size(width = 80.dp, height = 60.dp),
            separation = 4.dp,
            contentDescription = ""
        ) { index ->
            Text(
                text = numberOptions[index],
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = numberOptions.first(), fontSize = 10.sp, color = MutedGrey)
            Text(text = numberOptions.last(), fontSize = 10.sp, color = MutedGrey)
        }
    }
}
