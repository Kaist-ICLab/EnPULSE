package kaist.iclab.benchmark.wearable

import android.Manifest
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import kotlinx.coroutines.launch

/**
 * Wear OS benchmark UI using Jetpack Compose for Wear.
 * Provides a scrollable interface to configure and run benchmarks.
 */
class BenchmarkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            WatchBenchmarkScreen(
                onStart = { scenario, intervalSec, durationMin ->
                    startBenchmark(
                        scenario,
                        intervalSec,
                        durationMin
                    )
                },
                onStop = { stopBenchmark() },
                onPause = { pauseBenchmark() }
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }


    private fun startBenchmark(scenarioName: String, intervalSeconds: Long, durationMinutes: Long) {
        if (scenarioName.isBlank()) {
            Toast.makeText(this, "Enter scenario name", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, BenchmarkService::class.java).apply {
            putExtra(BenchmarkService.EXTRA_INTERVAL_MS, intervalSeconds * 1000L)
            putExtra(BenchmarkService.EXTRA_SCENARIO_NAME, scenarioName)
            putExtra(BenchmarkService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopBenchmark() {
        val intent = Intent(this, BenchmarkService::class.java).apply {
            action = BenchmarkService.ACTION_STOP
        }
        startService(intent)
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun pauseBenchmark() {
        val intent = Intent(this, BenchmarkService::class.java).apply {
            action =
                if (BenchmarkService.isPaused) BenchmarkService.ACTION_RESUME else BenchmarkService.ACTION_PAUSE
        }
        startService(intent)
    }
}

@Composable
fun WatchBenchmarkScreen(
    onStart: (String, Long, Long) -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
) {
    var scenarioName by remember { mutableStateOf("") }
    var intervalText by remember { mutableStateOf("60") }
    var durationText by remember { mutableStateOf("0") }
    var isRunning by remember { mutableStateOf(BenchmarkService.isRunning) }
    var isPaused by remember { mutableStateOf(BenchmarkService.isPaused) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var batteryLevel by remember { mutableStateOf("—") }
    var showInstructions by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasData by remember { mutableStateOf(BenchmarkService.hasStoredData(context)) }

    // Periodic status refresh
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val wasRunning = isRunning
                isRunning = BenchmarkService.isRunning
                isPaused = BenchmarkService.isPaused
                if (isRunning) {
                    val elapsed =
                        (System.currentTimeMillis() - BenchmarkService.startTimeMs - BenchmarkService.totalPausedMs) / 1000
                    elapsedSeconds = elapsed
                    val initial = BenchmarkService.initialBatteryLevel
                    val current = BenchmarkService.latestBatteryLevel
                    batteryLevel = if (initial != -1 && current != -1) {
                        "$current% (Dropped: ${initial - current}%)"
                    } else if (current != -1) {
                        "$current%"
                    } else "—"
                } else if (wasRunning) {
                    // Update hasData only when transitioning from running to stopped
                    hasData = BenchmarkService.hasStoredData(context)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
        onDispose { handler.removeCallbacks(runnable) }
    }

    MaterialTheme {
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            item {
                Text(
                    text = stringResource(id = R.string.activity_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isRunning) {
                // --- Configuration inputs ---
                item {
                    LabeledInput(
                        label = "Scenario",
                        value = scenarioName,
                        onValueChange = { scenarioName = it },
                        placeholder = "e.g. Baseline",
                        keyboardType = KeyboardType.Text
                    )
                }

                item {
                    LabeledInput(
                        label = "Sampling Interval (seconds)",
                        value = intervalText,
                        onValueChange = { intervalText = it },
                        placeholder = "60",
                        keyboardType = KeyboardType.Number
                    )
                }

                item {
                    LabeledInput(
                        label = "Duration (minutes)",
                        value = durationText,
                        onValueChange = { durationText = it },
                        placeholder = "0 = ∞",
                        keyboardType = KeyboardType.Number
                    )
                }

                // Start button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val interval = intervalText.toLongOrNull() ?: 60L
                            val duration = durationText.toLongOrNull() ?: 0L
                            onStart(scenarioName, interval, duration)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Guidelines Toggle Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Chip(
                        onClick = { showInstructions = !showInstructions },
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF222222)),
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = if (showInstructions) "▲ Hide Guidelines" else "ℹ View Guidelines",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    )
                }

                // Manage Data Buttons
                if (hasData) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Stored Data",
                            color = Color(0xFF999999),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Chip(
                            onClick = {
                                Toast.makeText(context, "Sending All Data...", Toast.LENGTH_SHORT)
                                    .show()
                                scope.launch {
                                    DataTransferManager.sendAllDataToPhone(context)
                                }
                            },
                            colors = ChipDefaults.primaryChipColors(
                                backgroundColor = Color(
                                    0xFF1E88E5
                                )
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Send All to Phone",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Chip(
                            onClick = {
                                BenchmarkService.deleteAllData(context)
                                hasData = BenchmarkService.hasStoredData(context)
                                Toast.makeText(context, "All Data Deleted", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Delete All Data",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                if (showInstructions) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(
                                    Color(0xFF1A1A1A),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Instructions:",
                                color = Color.Yellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "• Scenario: Name prefix of output folders (e.g. watch-Benchmark_Baseline).\n" +
                                        "• Interval: Metrics sampling interval in seconds (recommended 60s for standard, 15–30s for fast tests).\n" +
                                        "• Duration: Auto-stop duration in minutes (set to 0 for infinite run).\n" +
                                        "• Tap Start (▶) to run. The logger runs in the background.\n" +
                                        "• Tap Stop (■) to save CSV outputs to local external storage.\n" +
                                        "• Extract files using:\n" +
                                        "  adb pull /sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/",
                                color = Color(0xFFCCCCCC),
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }


            } else {
                // --- Running status ---
                item {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    val statusLabel = if (isPaused) "⏸ Paused" else "● Running"
                    val folderStr =
                        BenchmarkService.currentFolderPath?.substringAfterLast("/") ?: "—"

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .background(
                                Color(0xFF1E1E1E),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = if (isPaused) Color(0xFFFFB300) else Color(0xFF43A047),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Folder: $folderStr",
                            color = Color(0xFF999999),
                            fontSize = 10.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Elapsed: ${hours}h ${minutes}m ${seconds}s",
                            color = Color(0xFFDDDDDD),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "Battery: $batteryLevel",
                            color = Color(0xFFDDDDDD),
                            fontSize = 12.sp,
                        )
                    }
                }

                // Pause / Resume + Stop buttons
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Pause / Resume
                        Button(
                            onClick = {
                                onPause()
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isPaused) Color(0xFF2196F3) else Color(
                                    0xFFFFA000
                                )
                            ),
                            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.Refresh else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Stop
                        Button(
                            onClick = { onStop() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336)),
                            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = RemoteInput.getResultsFromIntent(result.data)
            val text = results?.getCharSequence("input_result")?.toString()
            if (text != null) {
                val sanitized = if (keyboardType == KeyboardType.Number) {
                    text.filter { it.isDigit() }
                } else {
                    text
                }
                onValueChange(sanitized)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable {
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs = listOf(
                    RemoteInput.Builder("input_result")
                        .setLabel(label)
                        .build()
                )
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                launcher.launch(intent)
            }
    ) {
        Text(
            text = label,
            color = Color(0xFFB0B0B0),
            fontSize = 11.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF333333),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF777777),
                    fontSize = 14.sp,
                )
            } else {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
