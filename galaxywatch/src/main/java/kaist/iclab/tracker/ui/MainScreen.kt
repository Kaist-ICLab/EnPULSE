package kaist.iclab.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import kaist.iclab.tracker.ui.theme.WearAppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    viewModel: AbstractMainViewModel = koinViewModel()
) {
    val isRunning = viewModel.isRunningState.collectAsState().value
    val collectorConfig = viewModel.collectorConfigState.collectAsState().value
    val listState = rememberScalingLazyListState() // for Scaling Lazy column

    WearAppTheme {
        Scaffold(
            timeText = {
                TimeText(modifier = Modifier.scrollAway(listState))
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(listOf("controller") + collectorConfig.keys.toList()) { name ->
                        when (name) {
                            "controller" -> Controller(isRunning = isRunning)
                            else -> BasicToggleChip(
                                modifier = Modifier.fillMaxWidth(),
                                text = name,
                                checked = collectorConfig[name] ?: false,
                                enabled = true,
                                onEnable = { viewModel.enable(name) },
                                onDisable = { viewModel.disable(name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Controller(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicIconButton(
            onClick = { /* TODO */ },
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Data",
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            iconModifier = Modifier.size(ButtonDefaults.SmallIconSize)
        )
        BasicIconButton(
            onClick = { /* TODO */ },
            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause recording of data",
            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize),
            iconModifier = Modifier.size(ButtonDefaults.LargeIconSize)
        )
        BasicIconButton(
            onClick = { /* TODO */ },
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync data",
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            iconModifier = Modifier.size(ButtonDefaults.SmallIconSize)
        )
    }
}


//@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun MainScreenPreview() {
    MainScreen(viewModel = FakeMainViewModelImpl())
}

