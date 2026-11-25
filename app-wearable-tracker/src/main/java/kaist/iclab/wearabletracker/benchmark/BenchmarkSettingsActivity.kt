package kaist.iclab.wearabletracker.benchmark

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.input.RemoteInputIntentHelper
import kaist.iclab.tracker.benchmark.BenchmarkRecorder
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Benchmark settings on the watch (pradipta/testing only). Each change is saved at once and
 * applies to the next run; nothing can be changed while a run is active. In independent mode the
 * run is started and stopped here.
 */
class BenchmarkSettingsActivity : ComponentActivity() {
    private val backgroundController: BackgroundController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearableTrackerTheme {
                BenchmarkSettingsScreen(backgroundController)
            }
        }
    }
}

private const val INPUT_KEY = "input_result"

@Composable
private fun BenchmarkSettingsScreen(backgroundController: BackgroundController) {
    val config by BenchmarkRecorder.config.collectAsState()
    val activeRun by BenchmarkRecorder.activeRun.collectAsState()
    val controllerState by backgroundController.controllerStateFlow.collectAsState()
    val collecting = controllerState.flag == ControllerState.FLAG.RUNNING ||
            controllerState.flag == ControllerState.FLAG.PAUSED
    val locked = activeRun != null
    val independent = config.mode == BenchmarkRecorder.Mode.INDEPENDENT

    fun save(change: (BenchmarkRecorder.Config) -> BenchmarkRecorder.Config) =
        BenchmarkRecorder.updateConfig(change(BenchmarkRecorder.config.value))

    val scenarioLauncher = textInputLauncher { text ->
        if (text.isNotBlank()) save { it.copy(scenario = text) }
    }
    val intervalLauncher = textInputLauncher { text ->
        text.filter(Char::isDigit).toIntOrNull()?.let { value -> save { it.copy(intervalSec = value) } }
    }
    val durationLauncher = textInputLauncher { text ->
        text.filter(Char::isDigit).toIntOrNull()?.let { value -> save { it.copy(durationMin = value) } }
    }

    // Read here: the click handlers below can't call stringResource.
    val scenarioPrompt = stringResource(R.string.benchmark_scenario_input)
    val intervalPrompt = stringResource(R.string.benchmark_interval_input)
    val durationPrompt = stringResource(R.string.benchmark_duration_input)

    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
    val run = activeRun
    val status = when {
        run != null -> stringResource(R.string.benchmark_status_recording, run.scenario) + "\n" +
                (run.endsAt?.let { stringResource(R.string.benchmark_status_stops_at, time.format(Date(it))) }
                    ?: stringResource(R.string.benchmark_no_limit))

        independent -> stringResource(R.string.benchmark_status_idle_independent)
        collecting -> stringResource(R.string.benchmark_status_off_at_start)
        else -> stringResource(R.string.benchmark_status_idle)
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 36.dp),
        ) {
            Text(
                text = stringResource(R.string.benchmark_title),
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = status,
                fontSize = 11.sp,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )

            if (run?.independent == true) {
                Chip(
                    label = { Text(stringResource(R.string.benchmark_stop)) },
                    onClick = { BenchmarkRecorder.stopIndependent() },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            } else if (independent) {
                Chip(
                    label = { Text(stringResource(R.string.benchmark_start)) },
                    onClick = { BenchmarkRecorder.startIndependent() },
                    enabled = !locked,
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }
            // Tapping switches between the two modes.
            SettingChip(
                label = stringResource(R.string.benchmark_mode),
                value = stringResource(
                    if (independent) R.string.benchmark_mode_independent else R.string.benchmark_mode_follow
                ),
                enabled = !locked,
                onClick = {
                    save {
                        it.copy(
                            mode = if (independent) {
                                BenchmarkRecorder.Mode.FOLLOW_COLLECTION
                            } else {
                                BenchmarkRecorder.Mode.INDEPENDENT
                            }
                        )
                    }
                }
            )
            ToggleChip(
                checked = config.holdWakeLock,
                onCheckedChange = { checked -> save { it.copy(holdWakeLock = checked) } },
                enabled = !locked,
                label = { Text(stringResource(R.string.benchmark_wake_lock)) },
                secondaryLabel = {
                    Text(
                        stringResource(
                            if (config.holdWakeLock) R.string.benchmark_wake_lock_on else R.string.benchmark_off
                        )
                    )
                },
                toggleControl = {
                    androidx.wear.compose.material.Icon(
                        imageVector = ToggleChipDefaults.switchIcon(config.holdWakeLock),
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            SettingChip(
                label = stringResource(R.string.benchmark_scenario),
                value = config.scenario,
                enabled = !locked,
                onClick = { scenarioLauncher.launch(textInputIntent(scenarioPrompt)) }
            )
            SettingChip(
                label = stringResource(R.string.benchmark_interval),
                value = stringResource(R.string.benchmark_interval_value, config.intervalSec),
                enabled = !locked,
                onClick = { intervalLauncher.launch(textInputIntent(intervalPrompt)) }
            )
            SettingChip(
                label = stringResource(R.string.benchmark_duration),
                value = if (config.durationMin == 0) {
                    stringResource(R.string.benchmark_no_limit)
                } else {
                    stringResource(R.string.benchmark_duration_value, config.durationMin)
                },
                enabled = !locked,
                onClick = { durationLauncher.launch(textInputIntent(durationPrompt)) }
            )
            Text(
                text = stringResource(
                    if (independent) R.string.benchmark_duration_note_independent else R.string.benchmark_duration_note
                ),
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SettingChip(label: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Chip(
        label = { Text(label) },
        secondaryLabel = { Text(value) },
        onClick = onClick,
        enabled = enabled,
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )
}

@Composable
private fun textInputLauncher(onText: (String) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            RemoteInput.getResultsFromIntent(result.data)
                ?.getCharSequence(INPUT_KEY)
                ?.toString()
                ?.let(onText)
        }
    }

private fun textInputIntent(label: String): Intent {
    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
    RemoteInputIntentHelper.putRemoteInputsExtra(
        intent,
        listOf(RemoteInput.Builder(INPUT_KEY).setLabel(label).build())
    )
    return intent
}
