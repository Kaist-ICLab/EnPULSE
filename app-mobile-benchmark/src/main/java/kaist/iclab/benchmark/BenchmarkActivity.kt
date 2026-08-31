package kaist.iclab.benchmark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kaist.iclab.benchmark.mobile.R

/**
 * Simple UI for the benchmark logger.
 * - Interval selector (spinner): 15s, 30s, 60s, 120s
 * - Scenario name text field
 * - Start / Stop buttons
 * - Status display (filename, elapsed time, battery level)
 */
class BenchmarkActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001

        private val INTERVAL_OPTIONS = arrayOf(
            "15 seconds" to 15_000L,
            "30 seconds" to 30_000L,
            "60 seconds (default)" to 60_000L,
            "120 seconds" to 120_000L,
        )
    }

    private lateinit var editInterval: EditText
    private lateinit var editDuration: EditText
    private lateinit var editScenarioName: EditText
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusRunnable = object : Runnable {
        override fun run() {
            updateStatusDisplay()
            if (BenchmarkService.isRunning) {
                statusHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_title)
        buildUi()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
        if (BenchmarkService.isRunning) {
            statusHandler.post(statusRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        statusHandler.removeCallbacks(statusRunnable)
    }

    private fun buildUi() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Instructions section
        layout.addView(TextView(this).apply {
            text = "📋 Instructions"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layout.addView(TextView(this).apply {
            text = "1. Set the sampling interval and scenario name.\n" +
                    "2. Press '▶ Start' to begin background logging.\n" +
                    "3. Start EnPULSE data collection and run your test.\n" +
                    "4. Press '■ Stop' when done. Output is saved to:\n" +
                    "   Downloads/EnPULSE/Benchmark_<Scenario>_<Date>/"
            textSize = 12.5f
            setTextColor(0xFF555555.toInt())
            setPadding(
                0,
                (4 * resources.displayMetrics.density).toInt(),
                0,
                (14 * resources.displayMetrics.density).toInt()
            )
        })

        // Divider
        layout.addView(View(this).apply {
            setBackgroundColor(0xFFE0E0E0.toInt())
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * resources.displayMetrics.density).toInt()
            ).apply {
                bottomMargin = (14 * resources.displayMetrics.density).toInt()
            }
        })

        // Interval selector
        layout.addView(TextView(this).apply {
            text = "Sampling Interval (seconds):"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layout.addView(TextView(this).apply {
            text =
                "How often battery, CPU, and RAM metrics are sampled. Recommended: 60s for standard tests, 15–30s for fast micro-benchmarks."
            textSize = 11.5f
            setTextColor(0xFF666666.toInt())
            setPadding(
                0,
                (2 * resources.displayMetrics.density).toInt(),
                0,
                (4 * resources.displayMetrics.density).toInt()
            )
        })

        editInterval = EditText(this).apply {
            setText("60")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        layout.addView(editInterval)

        // Spacer
        layout.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (12 * resources.displayMetrics.density).toInt()
            )
        })

        // Duration selector
        layout.addView(TextView(this).apply {
            text = "Duration (minutes):"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layout.addView(TextView(this).apply {
            text = "Auto-stop after this many minutes. Leave as 0 to run indefinitely."
            textSize = 11.5f
            setTextColor(0xFF666666.toInt())
            setPadding(
                0,
                (2 * resources.displayMetrics.density).toInt(),
                0,
                (4 * resources.displayMetrics.density).toInt()
            )
        })

        editDuration = EditText(this).apply {
            setText("0")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        layout.addView(editDuration)

        // Spacer
        layout.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (12 * resources.displayMetrics.density).toInt()
            )
        })

        // Scenario name
        layout.addView(TextView(this).apply {
            text = "Scenario Name:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layout.addView(TextView(this).apply {
            text =
                "Name of the benchmarking scenario (e.g., A_Baseline, B_PhoneOnly, C_BioTracking, E2_Accelerometer)."
            textSize = 11.5f
            setTextColor(0xFF666666.toInt())
            setPadding(
                0,
                (2 * resources.displayMetrics.density).toInt(),
                0,
                (4 * resources.displayMetrics.density).toInt()
            )
        })

        editScenarioName = EditText(this).apply {
            hint = "e.g., A_Baseline"
            setSingleLine(true)
        }
        layout.addView(editScenarioName)

        // Spacer
        layout.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (16 * resources.displayMetrics.density).toInt()
            )
        })

        // Buttons
        val btnLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        btnStart = Button(this).apply {
            text = "▶ Start"
            setOnClickListener { startBenchmark() }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
        }
        btnLayout.addView(btnStart)

        btnPause = Button(this).apply {
            text = "⏸ Pause"
            isEnabled = false
            setOnClickListener { pauseBenchmark() }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
        }
        btnLayout.addView(btnPause)

        btnStop = Button(this).apply {
            text = "■ Stop"
            isEnabled = false
            setOnClickListener { stopBenchmark() }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        btnLayout.addView(btnStop)
        layout.addView(btnLayout)

        // Spacer
        layout.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (16 * resources.displayMetrics.density).toInt()
            )
        })

        // Status display
        tvStatus = TextView(this).apply {
            text = "Status: Idle"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.NORMAL)
            setBackgroundColor(0xFFF5F5F5.toInt())
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        layout.addView(tvStatus)

        // Battery Optimization Button
        val btnBattery = Button(this).apply {
            text = "🔋 Request Unrestricted Battery"
            setOnClickListener { requestIgnoreBatteryOptimizations() }
        }
        layout.addView(btnBattery)

        // Spacer
        layout.addView(View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (12 * resources.displayMetrics.density).toInt()
            )
        })

        val scrollView = android.widget.ScrollView(this)
        scrollView.addView(layout)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val systemBars =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setContentView(scrollView)
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent =
                    Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                startActivity(intent)
            } catch (e: Exception) {
                val intent =
                    Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        } else {
            Toast.makeText(
                this,
                "Battery optimization is already disabled for Benchmark!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startBenchmark() {
        val scenarioName = editScenarioName.text.toString().trim()
        if (scenarioName.isEmpty()) {
            Toast.makeText(this, "Please enter a scenario name", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalSeconds = editInterval.text.toString().toLongOrNull() ?: 60L
        val intervalMs = intervalSeconds * 1000L

        val durationMinutes = editDuration.text.toString().toLongOrNull() ?: 0L

        val intent = Intent(this, BenchmarkService::class.java).apply {
            putExtra(BenchmarkService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(BenchmarkService.EXTRA_SCENARIO_NAME, scenarioName)
            putExtra(BenchmarkService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(this, intent)

        Toast.makeText(this, "Benchmark started", Toast.LENGTH_SHORT).show()

        // Small delay to let the service start
        statusHandler.postDelayed({
            updateUiState()
            statusHandler.post(statusRunnable)
        }, 500)
    }

    private fun pauseBenchmark() {
        val intent = Intent(this, BenchmarkService::class.java).apply {
            action =
                if (BenchmarkService.isPaused) BenchmarkService.ACTION_RESUME else BenchmarkService.ACTION_PAUSE
        }
        startService(intent)

        statusHandler.postDelayed({
            updateUiState()
            updateStatusDisplay()
        }, 300)
    }

    private fun stopBenchmark() {
        val intent = Intent(this, BenchmarkService::class.java).apply {
            action = BenchmarkService.ACTION_STOP
        }
        startService(intent)

        Toast.makeText(this, "Benchmark stopped. Saved to Downloads/EnPULSE/", Toast.LENGTH_LONG)
            .show()

        statusHandler.postDelayed({
            updateUiState()
        }, 500)
    }

    private fun updateUiState() {
        val running = BenchmarkService.isRunning
        val paused = BenchmarkService.isPaused

        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        btnPause.isEnabled = running
        btnPause.text = if (paused) "▶ Resume" else "⏸ Pause"

        editInterval.isEnabled = !running
        editDuration.isEnabled = !running
        editScenarioName.isEnabled = !running

        if (!running) {
            val folderPath = BenchmarkService.currentFolderPath
            tvStatus.text = if (folderPath != null) {
                "Status: Stopped\nSaved to: $folderPath"
            } else {
                "Status: Idle"
            }
        } else {
            updateStatusDisplay()
        }
    }

    private fun updateStatusDisplay() {
        if (!BenchmarkService.isRunning) return

        val elapsed =
            (System.currentTimeMillis() - BenchmarkService.startTimeMs - BenchmarkService.totalPausedMs) / 1000
        val hours = elapsed / 3600
        val minutes = (elapsed % 3600) / 60
        val seconds = elapsed % 60

        val initialBattery = BenchmarkService.initialBatteryLevel
        val currentBattery = BenchmarkService.latestBatteryLevel

        val batteryDiffStr = if (initialBattery != -1 && currentBattery != -1) {
            val dropped = initialBattery - currentBattery
            "$currentBattery% (Dropped: $dropped%)"
        } else if (currentBattery != -1) {
            "$currentBattery%"
        } else {
            "—"
        }

        val statusLabel = if (BenchmarkService.isPaused) "⏸ Paused" else "● Running"

        tvStatus.text = buildString {
            appendLine("Status: $statusLabel")
            appendLine("Folder: ${BenchmarkService.currentFolderPath ?: "—"}")
            appendLine("Elapsed: ${hours}h ${minutes}m ${seconds}s")
            appendLine("Battery: $batteryDiffStr")
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
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
}
