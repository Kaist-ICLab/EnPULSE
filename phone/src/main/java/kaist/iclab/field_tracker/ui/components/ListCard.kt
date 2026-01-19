package kaist.iclab.field_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.ui.theme.MainTheme

@Composable
fun ListCard(
    title: String? = null,
    rows: List<@Composable () -> Unit>,
    disabled: Boolean = false
) {
    Box {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (title != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 18.dp)
            ) {
                rows.forEachIndexed { index, rowContent ->
                    rowContent()
                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
        if (disabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = Color.White.copy(alpha = .6F))
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ListCardPreview() {
    val switchStatus = SwitchStatus(
        isChecked = true,
        onCheckedChange = { },
        disabled = false
    )
    val rows = listOf<@Composable () -> Unit>(
        {
            BaseRow(
                title = "Activity Recognition",
            ) { BasicSwitch(switchStatus) }
        }, {
            BaseRow(
                title = "Location",
            ) { BasicSwitch(switchStatus) }
        }
    )
    MainTheme {
        Column(
            modifier = Modifier.padding(top = 72.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            ListCard(title = "Collectors", rows = rows)
            ListCard(rows = rows)
        }
    }
}
