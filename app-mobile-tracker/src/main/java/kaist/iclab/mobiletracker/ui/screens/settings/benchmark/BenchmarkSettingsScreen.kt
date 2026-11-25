package kaist.iclab.mobiletracker.ui.screens.settings.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.toggle.Toggle
import kaist.iclab.mobiletracker.ui.screens.settings.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.ui.theme.appTextFieldColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.tracker.benchmark.BenchmarkRecorder
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Benchmark settings (pradipta/testing only). Saved values apply to the next run; they can't be
 * changed while a run is active. In independent mode the run is started and stopped here.
 */
@Composable
fun BenchmarkSettingsScreen(
    navController: NavController,
    backgroundController: BackgroundController = koinInject()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val config by BenchmarkRecorder.config.collectAsState()
    val activeRun by BenchmarkRecorder.activeRun.collectAsState()
    val controllerState by backgroundController.controllerStateFlow.collectAsState()
    val collecting = controllerState.flag == ControllerState.FLAG.RUNNING ||
            controllerState.flag == ControllerState.FLAG.PAUSED

    var mode by remember(config) { mutableStateOf(config.mode) }
    var scenario by remember(config) { mutableStateOf(config.scenario) }
    var intervalText by remember(config) { mutableStateOf(config.intervalSec.toString()) }
    var durationText by remember(config) { mutableStateOf(config.durationMin.toString()) }
    var holdWakeLock by remember(config) { mutableStateOf(config.holdWakeLock) }

    val interval = intervalText.toIntOrNull()
    val duration = durationText.toIntOrNull()
    val intervalValid = interval != null &&
            interval in BenchmarkRecorder.MIN_INTERVAL_SEC..BenchmarkRecorder.MAX_INTERVAL_SEC
    val durationValid = duration != null && duration in 0..BenchmarkRecorder.MAX_DURATION_MIN
    val scenarioValid = scenario.isNotBlank()
    val locked = activeRun != null
    val formValid = intervalValid && durationValid && scenarioValid
    fun saveForm() = BenchmarkRecorder.updateConfig(
        BenchmarkRecorder.Config(
            mode = mode,
            intervalSec = interval!!,
            durationMin = duration!!,
            scenario = scenario,
            holdWakeLock = holdWakeLock,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Styles.HEADER_HEIGHT)
                    .padding(horizontal = Styles.HEADER_HORIZONTAL_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_desc)
                    )
                }
                Text(
                    text = stringResource(R.string.menu_benchmark),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.benchmark_description,
                        BenchmarkRecorder.runsLocation
                    ),
                    color = AppColors.TextSecondary,
                    fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                            end = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                            bottom = Styles.SCREEN_DESCRIPTION_BOTTOM_PADDING
                        )
                )

                StatusCard(activeRun = activeRun, mode = config.mode, collecting = collecting)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Styles.CARD_CONTAINER_HORIZONTAL_PADDING,
                            end = Styles.CARD_CONTAINER_HORIZONTAL_PADDING,
                            bottom = 12.dp
                        ),
                    colors = CardDefaults.cardColors(containerColor = AppColors.White),
                    shape = Styles.CARD_SHAPE
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.benchmark_mode_title),
                            fontSize = Styles.TEXT_FONT_SIZE,
                            color = AppColors.TextPrimary
                        )
                        ModeOption(
                            title = stringResource(R.string.benchmark_mode_follow),
                            description = stringResource(R.string.benchmark_mode_follow_desc),
                            selected = mode == BenchmarkRecorder.Mode.FOLLOW_COLLECTION,
                            onSelect = { mode = BenchmarkRecorder.Mode.FOLLOW_COLLECTION },
                            enabled = !locked
                        )
                        ModeOption(
                            title = stringResource(R.string.benchmark_mode_independent),
                            description = stringResource(R.string.benchmark_mode_independent_desc),
                            selected = mode == BenchmarkRecorder.Mode.INDEPENDENT,
                            onSelect = { mode = BenchmarkRecorder.Mode.INDEPENDENT },
                            enabled = !locked
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BenchmarkToggle(
                            title = stringResource(R.string.benchmark_wake_lock_title),
                            description = stringResource(R.string.benchmark_wake_lock_desc),
                            checked = holdWakeLock,
                            onCheckedChange = { holdWakeLock = it },
                            enabled = !locked
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        BenchmarkField(
                            value = scenario,
                            onValueChange = { scenario = it },
                            label = stringResource(R.string.benchmark_scenario_label),
                            supporting = stringResource(R.string.benchmark_scenario_help),
                            isError = !scenarioValid,
                            enabled = !locked,
                            numeric = false
                        )
                        BenchmarkField(
                            value = intervalText,
                            onValueChange = { intervalText = it.filter(Char::isDigit) },
                            label = stringResource(R.string.benchmark_interval_label),
                            supporting = stringResource(
                                R.string.benchmark_interval_help,
                                BenchmarkRecorder.MIN_INTERVAL_SEC,
                                BenchmarkRecorder.MAX_INTERVAL_SEC
                            ),
                            isError = !intervalValid,
                            enabled = !locked,
                            numeric = true
                        )
                        BenchmarkField(
                            value = durationText,
                            onValueChange = { durationText = it.filter(Char::isDigit) },
                            label = stringResource(R.string.benchmark_duration_label),
                            supporting = stringResource(
                                if (mode == BenchmarkRecorder.Mode.INDEPENDENT) {
                                    R.string.benchmark_duration_help_independent
                                } else {
                                    R.string.benchmark_duration_help
                                }
                            ),
                            isError = !durationValid,
                            enabled = !locked,
                            numeric = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        if (activeRun?.independent == true) {
                            BenchmarkButton(
                                text = stringResource(R.string.benchmark_stop),
                                onClick = { BenchmarkRecorder.stopIndependent() },
                                enabled = true
                            )
                        } else {
                            BenchmarkButton(
                                text = stringResource(R.string.benchmark_save),
                                onClick = {
                                    saveForm()
                                    AppToast.show(
                                        context,
                                        resources.getString(R.string.benchmark_saved)
                                    )
                                },
                                enabled = !locked && formValid,
                                outlined = mode == BenchmarkRecorder.Mode.INDEPENDENT
                            )
                            if (mode == BenchmarkRecorder.Mode.INDEPENDENT) {
                                Spacer(modifier = Modifier.height(8.dp))
                                BenchmarkButton(
                                    text = stringResource(R.string.benchmark_start),
                                    onClick = {
                                        saveForm()
                                        BenchmarkRecorder.startIndependent()
                                    },
                                    enabled = !locked && formValid
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    activeRun: BenchmarkRecorder.Run?,
    mode: BenchmarkRecorder.Mode,
    collecting: Boolean
) {
    val time = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val lines = when {
        activeRun != null -> listOfNotNull(
            stringResource(R.string.benchmark_status_recording, activeRun.name),
            stringResource(R.string.benchmark_status_started, time.format(Date(activeRun.startedAt))),
            activeRun.endsAt?.let {
                stringResource(
                    if (activeRun.independent) {
                        R.string.benchmark_status_recording_stops_at
                    } else {
                        R.string.benchmark_status_stops_at
                    },
                    time.format(Date(it))
                )
            } ?: stringResource(R.string.benchmark_status_no_limit),
        )

        mode == BenchmarkRecorder.Mode.INDEPENDENT ->
            listOf(stringResource(R.string.benchmark_status_idle_independent))
        collecting -> listOf(stringResource(R.string.benchmark_status_off_at_start))
        else -> listOf(stringResource(R.string.benchmark_status_idle))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Styles.CARD_CONTAINER_HORIZONTAL_PADDING,
                end = Styles.CARD_CONTAINER_HORIZONTAL_PADDING,
                bottom = 12.dp
            ),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        shape = Styles.CARD_SHAPE
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.benchmark_status),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            lines.forEach {
                Text(text = it, fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = AppColors.PrimaryColor)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = title, fontSize = Styles.TEXT_FONT_SIZE, color = AppColors.TextPrimary)
            Text(
                text = description,
                fontSize = Styles.DESCRIPTION_FONT_SIZE,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun BenchmarkButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    outlined: Boolean = false,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Text(text = text, fontSize = 14.sp, color = AppColors.PrimaryColor)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.PrimaryColor,
                disabledContainerColor = AppColors.BorderLight
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Text(text = text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BenchmarkToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = Styles.TEXT_FONT_SIZE, color = AppColors.TextPrimary)
            Text(
                text = description,
                fontSize = Styles.DESCRIPTION_FONT_SIZE,
                color = AppColors.TextSecondary
            )
        }
        Toggle(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun BenchmarkField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String,
    isError: Boolean,
    enabled: Boolean,
    numeric: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        supportingText = { Text(supporting, fontSize = 11.sp) },
        isError = isError,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        textStyle = TextStyle(fontSize = 14.sp),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
        colors = appTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )
}
