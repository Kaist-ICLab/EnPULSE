package kaist.iclab.tracker.benchmark

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import kaist.iclab.tracker.sensor.controller.ControllerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Benchmark recorder built into EnPULSE (pradipta/testing only). It replaces the separate
 * benchmark apps for battery, and adds EnPULSE's own memory, which only EnPULSE's process can read.
 *
 * Two modes ([Config.mode]); only one applies at a time:
 *  - [Mode.FOLLOW_COLLECTION]: a run starts when data collection starts and ends when it stops. If
 *    [Config.durationMin] is set, collection itself is stopped when that time has passed (through
 *    the callback given to [init]), which then ends the run.
 *  - [Mode.INDEPENDENT]: a run starts with [startIndependent] and ends with [stopIndependent] or when
 *    [Config.durationMin] has passed, whether or not collection is running. Collection is never
 *    started or stopped by it. [BenchmarkService] keeps the process alive meanwhile, since nothing
 *    else does while collection is stopped.
 *
 * While a run is active, one row goes to `metrics.csv` every [Config.intervalSec] seconds, plus one
 * at every collection state change and one at the end; `summary.txt` is written when it ends. Each
 * row has battery, EnPULSE's memory, its CPU, threads, files, GC, storage and process start time,
 * and the device's state (screen, Doze, power saver, network, phone or watch connected).
 *
 * Files go where the benchmark apps put theirs, one folder per run named
 * `<role>-<model>_<scenario>_<time>`:
 *  - phone: `/sdcard/Download/EnPULSE/`, written through MediaStore (no storage permission needed);
 *  - watch: `/sdcard/Android/data/<package>/files/EnPULSE-Benchmark/`, because MediaStore is not
 *    reliable on Wear OS (the watch benchmark app avoids it for the same reason).
 * adb can pull both. The columns up to `battery_charge_uah` match the benchmark apps, so
 * `scripts/tools/generate_report.py` reads both; `collection_state` tells the two apart.
 *
 * Wake lock ([Config.holdWakeLock], on by default): a partial wake lock keeps the CPU awake for the
 * whole run, like the benchmark apps, so samples are on time. It is held by EnPULSE's process, so
 * batterystats charges it to EnPULSE: its estimated mAh includes the awake CPU, and
 * `scripts/tools/enpulse_stats.py` reports this lock's time apart from EnPULSE's own wake locks
 * (tag [WAKE_LOCK_TAG]). Doze ignores it unless EnPULSE is exempt from battery optimisation; the
 * summary says which. Without it, samples pause while the CPU sleeps and the timestamps show the
 * gaps. Either way the duration limit uses an alarm, so the run ends on time.
 *
 * The active run is saved in SharedPreferences. After a process restart, a follow-collection run
 * continues if collection is still RUNNING and is closed otherwise; an independent run continues
 * once [BenchmarkService] is running again.
 */
object BenchmarkRecorder {
    private const val TAG = "BenchmarkRecorder"
    /** Watch runs: under the app's external files folder. */
    const val DIRECTORY = "EnPULSE-Benchmark"
    /** Phone runs: under Download/, the same folder the phone benchmark app uses. */
    const val DOWNLOADS_DIRECTORY = "EnPULSE"
    private const val PREFS = "enpulse_benchmark"
    private const val ACTION_DURATION_REACHED = "kaist.iclab.tracker.benchmark.DURATION_REACHED"
    private const val ALARM_REQUEST_CODE = 0x42454e43 // "BENC"

    const val MIN_INTERVAL_SEC = 1
    const val MAX_INTERVAL_SEC = 3600
    const val MAX_DURATION_MIN = 7 * 24 * 60
    const val WAKE_LOCK_TAG = "EnPULSE:BenchmarkRecorder"

    private val HEADER =
        "timestamp_iso,timestamp_ms,battery_level_pct,battery_voltage_mv,battery_temp_c," +
                "battery_status,battery_charge_uah,collection_state," +
                "total_pss_mb,java_heap_mb,native_heap_mb,code_mb,stack_mb,graphics_mb," +
                "private_other_mb,system_mb,native_heap_allocated_bytes,java_heap_used_bytes," +
                SystemSampler.COLUMNS.joinToString(",") + "\n"

    // Debug.MemoryInfo summary keys, in the same order as the CSV columns after total_pss_mb.
    private val SUMMARY_KEYS = listOf(
        "summary.java-heap",
        "summary.native-heap",
        "summary.code",
        "summary.stack",
        "summary.graphics",
        "summary.private-other",
        "summary.system",
    )

    enum class Mode { FOLLOW_COLLECTION, INDEPENDENT }

    /**
     * @property durationMin end the run after this many minutes (in follow-collection mode, by
     *   stopping collection); 0 means no limit.
     * @property holdWakeLock keep the CPU awake for the whole run.
     */
    data class Config(
        val mode: Mode = Mode.FOLLOW_COLLECTION,
        val intervalSec: Int = 60,
        val durationMin: Int = 0,
        val scenario: String = "default",
        val holdWakeLock: Boolean = true,
    )

    /**
     * @property name the run's folder name.
     * @property ref where metrics.csv is: a MediaStore content URI (Downloads) or a file path.
     * @property endsAt wall-clock time the run ends, or null for no limit.
     * @property independent started by [startIndependent], not by collection.
     */
    data class Run(
        val name: String,
        val ref: String,
        val scenario: String,
        val startedAt: Long,
        val endsAt: Long?,
        val intervalMs: Long,
        val holdWakeLock: Boolean,
        val independent: Boolean,
    )

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config.asStateFlow()

    private val _activeRun = MutableStateFlow<Run?>(null)
    val activeRun: StateFlow<Run?> = _activeRun.asStateFlow()

    // Every file write and state change runs on this one thread, in the order it was requested.
    private val scope = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var deviceRole = "device"
    private var saveToDownloads = false

    /** Where runs are saved, for display. */
    var runsLocation = ""
        private set
    private var onDurationReached: () -> Unit = {}
    private var appProbe: BenchmarkAppProbe = object : BenchmarkAppProbe {}
    private lateinit var systemSampler: SystemSampler

    private var samplingJob: Job? = null
    private var recordingInThisProcess = false
    private var currentFlag: ControllerState.FLAG? = null
    private var lastCollectionFlag: ControllerState.FLAG? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val alarmManager by lazy { appContext.getSystemService(AlarmManager::class.java) }
    private val deadlineIntent by lazy {
        PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            Intent(ACTION_DURATION_REACHED).setPackage(appContext.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Call once from Application.onCreate, before feeding it controller states.
     *
     * @param deviceRole "phone" or "watch"; starts the folder name.
     * @param saveToDownloads true: Download/EnPULSE/ via MediaStore; false: the app's own folder.
     * @param appProbe run details only the app knows.
     * @param onDurationReached stops data collection. Runs on the main thread.
     */
    fun init(
        context: Context,
        deviceRole: String,
        saveToDownloads: Boolean,
        appProbe: BenchmarkAppProbe,
        onDurationReached: () -> Unit
    ) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        this.deviceRole = deviceRole
        this.saveToDownloads = saveToDownloads
        runsLocation = if (saveToDownloads) {
            "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_DIRECTORY/"
        } else {
            "Android/data/${appContext.packageName}/files/$DIRECTORY/"
        }
        this.onDurationReached = onDurationReached
        this.appProbe = appProbe
        systemSampler = SystemSampler(appContext)
        _config.value = loadConfig()
        _activeRun.value = loadRun()

        ContextCompat.registerReceiver(
            appContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    scope.launch { _activeRun.value?.let { checkDeadline(it) } }
                }
            },
            IntentFilter(ACTION_DURATION_REACHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // An independent run outlives a process restart only if its service runs again.
        _activeRun.value?.takeIf { it.independent }?.let { run ->
            launchSafely("resume ${run.name}") {
                try {
                    startService()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not restart the benchmark service; closing ${run.name}", e)
                    finishRun(run, endedBy = "app restarted and could not resume")
                }
            }
        }
    }

    /** Saved values apply from the next run; the active run keeps the ones it started with. */
    fun updateConfig(config: Config) {
        val clean = config.copy(
            intervalSec = config.intervalSec.coerceIn(MIN_INTERVAL_SEC, MAX_INTERVAL_SEC),
            durationMin = config.durationMin.coerceIn(0, MAX_DURATION_MIN),
            scenario = sanitize(config.scenario).ifEmpty { "default" },
        )
        prefs.edit()
            .putString("mode", clean.mode.name)
            .putInt("interval_sec", clean.intervalSec)
            .putInt("duration_min", clean.durationMin)
            .putString("scenario", clean.scenario)
            .putBoolean("wake_lock", clean.holdWakeLock)
            .apply()
        _config.value = clean
    }

    /** Feed every controller state change here (RUNNING, PAUSED, READY, DISABLED). */
    fun onCollectionState(flag: ControllerState.FLAG) =
        launchSafely("handle collection state $flag") { handleState(flag) }

    /** Independent mode: starts a run now, whether or not collection is running. */
    fun startIndependent() = launchSafely("start an independent run") {
        if (_activeRun.value != null || _config.value.mode != Mode.INDEPENDENT) return@launchSafely
        val run = createRun(independent = true)
        record(run, lastCollectionFlag)
        startService()
    }

    /** Ends the active independent run. Does nothing to a follow-collection run. */
    fun stopIndependent() = launchSafely("stop the independent run") {
        _activeRun.value?.takeIf { it.independent }?.let { finishRun(it, endedBy = "stopped manually") }
    }

    /** Called by [BenchmarkService] each time it starts, including after a process restart. */
    internal fun onServiceStarted() = launchSafely("resume after the service started") {
        val run = _activeRun.value
        if (run == null || !run.independent) {
            stopService()
            return@launchSafely
        }
        if (isPastDeadline(run)) fireDurationReached(run) else record(run, lastCollectionFlag)
    }

    private fun launchSafely(what: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to $what", e)
            }
        }
    }

    private fun handleState(flag: ControllerState.FLAG) {
        lastCollectionFlag = flag
        val run = _activeRun.value

        if (run != null && run.independent) {
            // Collection doesn't start or end an independent run; just mark the change.
            if (recordingInThisProcess && currentFlag != flag) appendSample(run, flag.name)
            currentFlag = flag
            return
        }

        when (flag) {
            ControllerState.FLAG.RUNNING, ControllerState.FLAG.PAUSED -> {
                val current = run
                    ?: if (_config.value.mode == Mode.FOLLOW_COLLECTION) createRun(independent = false)
                    else return
                if (isPastDeadline(current)) {
                    // Restarted after the deadline: stop collection, which then closes the run.
                    fireDurationReached(current)
                    return
                }
                record(current, flag)
            }

            ControllerState.FLAG.READY, ControllerState.FLAG.DISABLED ->
                run?.let { finishRun(it, endedBy = "collection stopped") }
        }
    }

    /** Starts or continues recording [run] in this process; [flag] is the collection state now. */
    private fun record(run: Run, flag: ControllerState.FLAG?) {
        val firstInProcess = !recordingInThisProcess
        if (firstInProcess || currentFlag != flag) appendSample(run, flag?.name ?: "UNKNOWN")
        currentFlag = flag
        recordingInThisProcess = true
        if (firstInProcess) scheduleDeadline(run)
        if (run.holdWakeLock) renewWakeLock(run)
        if (samplingJob?.isActive != true) samplingJob = scope.launch { sampleLoop(run) }
    }

    private fun finishRun(run: Run, endedBy: String) {
        samplingJob?.cancel()
        samplingJob = null
        // After a restart nothing was recording here, so a sample now would be taken after the
        // run had already ended.
        if (recordingInThisProcess) appendSample(run, "STOPPED")
        writeSummary(run, endedBy)
        releaseWakeLock()
        cancelDeadline()
        clearRun()
        recordingInThisProcess = false
        currentFlag = null
        if (run.independent) stopService()
        Log.i(TAG, "Run finished: ${run.name} ($endedBy)")
    }

    private fun startService() = ContextCompat.startForegroundService(
        appContext, Intent(appContext, BenchmarkService::class.java)
    )

    private fun stopService() = appContext.stopService(Intent(appContext, BenchmarkService::class.java))

    private suspend fun sampleLoop(run: Run) {
        while (currentCoroutineContext().isActive) {
            delay(run.intervalMs)
            if (isPastDeadline(run)) {
                fireDurationReached(run)
                return
            }
            try {
                if (run.holdWakeLock) renewWakeLock(run)
                appendSample(run, currentFlag?.name ?: "UNKNOWN")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write sample", e)
            }
        }
    }

    // ---- run lifecycle ----

    private fun createRun(independent: Boolean): Run {
        val cfg = _config.value
        val now = System.currentTimeMillis()
        val time = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(now))
        val name = "$deviceRole-${sanitize(Build.MODEL)}_${cfg.scenario}_$time"
        systemSampler.reset()

        val run = Run(
            name = name,
            ref = createRunFile(name, "metrics.csv", HEADER),
            scenario = cfg.scenario,
            startedAt = now,
            endsAt = if (cfg.durationMin > 0) now + cfg.durationMin * 60_000L else null,
            intervalMs = cfg.intervalSec * 1000L,
            holdWakeLock = cfg.holdWakeLock,
            independent = independent,
        )
        prefs.edit()
            .putString("run_name", run.name)
            .putString("run_ref", run.ref)
            .putString("run_info", runInfo(cfg).joinToString("\n") { (k, v) -> "$k\t$v" })
            .putString("run_scenario", run.scenario)
            .putLong("run_started_at", run.startedAt)
            .putLong("run_ends_at", run.endsAt ?: -1L)
            .putLong("run_interval_ms", run.intervalMs)
            .putBoolean("run_wake_lock", run.holdWakeLock)
            .putBoolean("run_independent", run.independent)
            .putBoolean("run_duration_reached", false)
            .apply()
        _activeRun.value = run
        Log.i(TAG, "Run started: $runsLocation$name")
        return run
    }

    private fun clearRun() {
        prefs.edit()
            .remove("run_name").remove("run_ref").remove("run_info")
            .remove("run_scenario").remove("run_started_at")
            .remove("run_ends_at").remove("run_interval_ms").remove("run_wake_lock")
            .remove("run_independent").remove("run_duration_reached")
            .apply()
        _activeRun.value = null
    }

    private fun loadConfig() = Config(
        mode = prefs.getString("mode", null)
            ?.let { saved -> Mode.entries.firstOrNull { it.name == saved } }
            ?: Mode.FOLLOW_COLLECTION,
        intervalSec = prefs.getInt("interval_sec", 60),
        durationMin = prefs.getInt("duration_min", 0),
        scenario = prefs.getString("scenario", null) ?: "default",
        holdWakeLock = prefs.getBoolean("wake_lock", true),
    )

    private fun loadRun(): Run? {
        val name = prefs.getString("run_name", null)
        val ref = prefs.getString("run_ref", null)
        if (name == null || ref == null || !fileExists(ref)) {
            clearRun()
            return null
        }
        return Run(
            name = name,
            ref = ref,
            scenario = prefs.getString("run_scenario", null) ?: "default",
            startedAt = prefs.getLong("run_started_at", 0L),
            endsAt = prefs.getLong("run_ends_at", -1L).takeIf { it > 0 },
            intervalMs = prefs.getLong("run_interval_ms", 60_000L),
            holdWakeLock = prefs.getBoolean("run_wake_lock", true),
            independent = prefs.getBoolean("run_independent", false),
        )
    }

    // ---- duration limit ----

    private fun isPastDeadline(run: Run) =
        run.endsAt != null && System.currentTimeMillis() >= run.endsAt - 1000

    private fun checkDeadline(run: Run) {
        if (isPastDeadline(run)) fireDurationReached(run)
    }

    private fun fireDurationReached(run: Run) {
        if (prefs.getBoolean("run_duration_reached", false)) return
        prefs.edit().putBoolean("run_duration_reached", true).apply()
        if (run.independent) {
            finishRun(run, endedBy = "duration reached")
            return
        }
        Log.i(TAG, "Duration reached for ${run.name}; stopping collection")
        scope.launch(Dispatchers.Main) {
            try {
                onDurationReached()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop collection at the end of the duration", e)
            }
        }
    }

    @SuppressLint("MissingPermission") // canScheduleExactAlarms() is checked first
    private fun scheduleDeadline(run: Run) {
        val endsAt = run.endsAt ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, deadlineIntent)
        } else {
            // Can be several minutes late in Doze; the summary shows the real end time.
            Log.w(TAG, "Exact alarms not allowed; the run may end a few minutes late")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, deadlineIntent)
        }
    }

    private fun cancelDeadline() = alarmManager.cancel(deadlineIntent)

    // ---- wake lock ----

    /**
     * Acquires the lock, or extends it, for a few sampling intervals. It is renewed at every
     * sample, so it lasts as long as the run but expires on its own if the process stops sampling.
     */
    private fun renewWakeLock(run: Run) {
        val lock = wakeLock ?: appContext.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(maxOf(run.intervalMs * 3, 5 * 60_000L))
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun isBatteryOptimizationExempt() =
        appContext.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(appContext.packageName)

    // ---- files ----
    // Each run's ref says where it lives, so a run keeps its storage even if the setting changes.

    private fun isContentUri(ref: String) = ref.startsWith("content://")

    /** Creates `<name>/<fileName>` with its header. Returns its ref. */
    private fun createRunFile(name: String, fileName: String, header: String): String {
        if (saveToDownloads) {
            val uri = insertDownload(name, fileName, "text/csv")
            appContext.contentResolver.openOutputStream(uri, "w")!!.use { it.write(header.toByteArray()) }
            return uri.toString()
        }
        val base = appContext.getExternalFilesDir(DIRECTORY) ?: File(appContext.filesDir, DIRECTORY)
        val file = File(File(base, name).apply { mkdirs() }, fileName)
        file.writeText(header)
        return file.absolutePath
    }

    private fun insertDownload(name: String, fileName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_DIRECTORY/$name/"
            )
        }
        return appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create $fileName in Downloads")
    }

    private fun fileExists(ref: String): Boolean {
        if (!isContentUri(ref)) return File(ref).exists()
        return try {
            appContext.contentResolver.openFileDescriptor(Uri.parse(ref), "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** [header] re-creates a plain file that was deleted; a deleted Downloads file just fails. */
    private fun appendTo(ref: String, header: String, text: String) {
        if (isContentUri(ref)) {
            appContext.contentResolver.openOutputStream(Uri.parse(ref), "wa")!!
                .use { it.write(text.toByteArray()) }
            return
        }
        val file = File(ref)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(header)
        }
        file.appendText(text)
    }

    private fun readLines(ref: String?): List<String> = try {
        when {
            ref == null -> emptyList()
            isContentUri(ref) -> appContext.contentResolver.openInputStream(Uri.parse(ref))
                ?.bufferedReader()?.use { it.readLines() } ?: emptyList()

            else -> File(ref).takeIf { it.exists() }?.readLines() ?: emptyList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read $ref", e)
        emptyList()
    }

    private fun writeSummaryFile(run: Run, text: String) {
        if (isContentUri(run.ref)) {
            val uri = insertDownload(run.name, "summary.txt", "text/plain")
            appContext.contentResolver.openOutputStream(uri, "w")!!.use { it.write(text.toByteArray()) }
        } else {
            File(File(run.ref).parentFile, "summary.txt").writeText(text)
        }
    }

    // ---- sampling ----

    private fun appendSample(run: Run, state: String) {
        val now = System.currentTimeMillis()
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(now))
        appendTo(run.ref, HEADER, sampleRow(now, iso, state) + "\n")
    }

    /** One metrics.csv row up to and including the system columns, without a newline. */
    private fun sampleRow(now: Long, iso: String, state: String): String {
        val battery =
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = appContext.getSystemService(BatteryManager::class.java)

        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPct = if (level >= 0 && scale > 0) (level * 100 / scale).toString() else ""
        val voltage = battery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
        val tempRaw = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = tempRaw?.takeIf { it != Int.MIN_VALUE }
            ?.let { String.format(Locale.US, "%.1f", it / 10.0) }
        val status = when (battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        // Long.MIN_VALUE (or Int.MIN_VALUE) when the fuel gauge doesn't support it. The energy
        // counter isn't recorded: Samsung phones and watches never support it.
        val chargeUah = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it != Long.MIN_VALUE && it != Int.MIN_VALUE.toLong() }

        val memory = Debug.MemoryInfo()
        Debug.getMemoryInfo(memory)
        val runtime = Runtime.getRuntime()
        fun mb(kb: Long?) = kb?.let { String.format(Locale.US, "%.1f", it / 1024.0) } ?: ""

        return buildString {
            append(iso).append(',')
            append(now).append(',')
            append(levelPct).append(',')
            append(voltage ?: "").append(',')
            append(temperature ?: "").append(',')
            append(status).append(',')
            append(chargeUah ?: "").append(',')
            append(state).append(',')
            append(mb(memory.totalPss.toLong())).append(',')
            SUMMARY_KEYS.forEach { key ->
                append(mb(memory.getMemoryStat(key)?.toLongOrNull())).append(',')
            }
            append(Debug.getNativeHeapAllocatedSize()).append(',')
            append(runtime.totalMemory() - runtime.freeMemory()).append(',')
            append(systemSampler.sample(now).joinToString(","))
        }
    }

    /** App, device and settings details for summary.txt, taken when the run starts. */
    private fun runInfo(cfg: Config): List<Pair<String, String>> {
        val pkg = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        } catch (e: Exception) {
            null
        }
        val installed = pkg?.lastUpdateTime
            ?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(it)) }
        val app = listOf(
            "App version" to "${pkg?.versionName} (${pkg?.longVersionCode}), installed $installed",
            "Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Settings" to "mode ${cfg.mode.name.lowercase()}, interval ${cfg.intervalSec} s, " +
                    "duration ${if (cfg.durationMin > 0) "${cfg.durationMin} min" else "none"}, " +
                    "wake lock ${if (cfg.holdWakeLock) "on" else "off"}",
        )
        val fromApp = try {
            appProbe.runInfo()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read run info from the app", e)
            emptyList()
        }
        return app + fromApp
    }

    // ---- summary ----

    /** Worked out from metrics.csv itself, so it is also right for a run continued after a restart. */
    private fun writeSummary(run: Run, endedBy: String) {
        val lines = readLines(run.ref).filter { it.isNotBlank() }
        if (lines.size < 2) {
            writeSummaryFile(run, "EnPULSE Benchmark Summary\nNo samples were recorded.\n")
            return
        }
        val columns = lines.first().split(',')
        val rows = lines.drop(1).map { it.split(',') }.filter { it.size == columns.size }
        if (rows.isEmpty()) return
        // Runs from older builds lack the newer columns; those read as empty.
        fun col(row: List<String>, name: String) =
            columns.indexOf(name).takeIf { it >= 0 }?.let { row[it] }.orEmpty()
        fun long(row: List<String>, name: String) = col(row, name).toLongOrNull()
        fun double(row: List<String>, name: String) =
            columns.indexOf(name).takeIf { it >= 0 }?.let { row[it].toDoubleOrNull() }

        val first = rows.first()
        val last = rows.last()
        val startMs = long(first, "timestamp_ms") ?: run.startedAt
        val endMs = long(last, "timestamp_ms") ?: startMs
        val hours = (endMs - startMs) / 3_600_000.0

        // Each row's state holds until the next row, and a row is written at every change.
        var pausedMs = 0L
        var longestGapMs = 0L
        rows.zipWithNext { a, b ->
            val gap = (long(b, "timestamp_ms") ?: 0L) - (long(a, "timestamp_ms") ?: 0L)
            if (col(a, "collection_state") == "PAUSED") pausedMs += gap
            if (gap > longestGapMs) longestGapMs = gap
        }

        val levelStart = long(first, "battery_level_pct")
        val levelEnd = long(last, "battery_level_pct")
        val chargeStart = long(first, "battery_charge_uah")
        val chargeEnd = long(last, "battery_charge_uah")
        val charged = rows.any { col(it, "battery_status") == "charging" }

        fun stats(name: String, scale: Double = 1.0): String {
            val values = rows.mapNotNull { double(it, name) }
            if (values.isEmpty()) return "n/a"
            return String.format(Locale.US, "%.1f / %.1f", values.average() / scale, values.max() / scale)
        }
        fun f2(value: Double) = String.format(Locale.US, "%.2f", value)
        val spanMs = (endMs - startMs).coerceAtLeast(1)

        // A process counter (CPU, GC) restarts from zero in a new process: count that row's value
        // as the change instead of a negative step.
        fun counterTotal(name: String): Double? {
            val values = rows.map { double(it, name) }
            if (values.all { it == null }) return null
            return values.zipWithNext { a, b ->
                if (a == null || b == null) 0.0 else if (b >= a) b - a else b
            }.sum()
        }

        // Share of the run during which a column had this value; a row's value holds until the next.
        fun share(name: String, value: String): String {
            var ms = 0L
            rows.zipWithNext { a, b ->
                if (col(a, name) == value) ms += (long(b, "timestamp_ms") ?: 0L) - (long(a, "timestamp_ms") ?: 0L)
            }
            return "${(ms * 100 / spanMs)}%"
        }

        fun firstLastMax(name: String): String {
            val values = rows.mapNotNull { double(it, name)?.toLong() }
            if (values.isEmpty()) return "n/a"
            return "${values.first()} -> ${values.last()} (max ${values.max()})"
        }

        val runInfo = prefs.getString("run_info", null).orEmpty().lines()
            .filter { it.contains('\t') }.map { it.substringBefore('\t') to it.substringAfter('\t') }
        val cpuMs = counterTotal("cpu_time_ms")
        val restarts = rows.map { col(it, "process_started_ms") }.filter { it.isNotEmpty() }
            .distinct().size.minus(1).coerceAtLeast(0)
        val networks = rows.map { col(it, "network") }.filter { it.isNotEmpty() }.distinct()
        val buckets = rows.map { col(it, "standby_bucket") }.filter { it.isNotEmpty() }.distinct()

        val ended = if (prefs.getBoolean("run_duration_reached", false)) "duration reached" else endedBy
        val planned = run.endsAt?.let { "${(it - run.startedAt) / 60_000} min" } ?: "no limit"
        val mib = 1024.0 * 1024.0

        writeSummaryFile(run, buildString {
            appendLine("EnPULSE Benchmark Summary")
            appendLine("=========================")
            appendLine("Scenario:        ${run.scenario}")
            appendLine("Device:          $deviceRole (${Build.MODEL})")
            appendLine("Start Time:      ${Date(startMs)}")
            appendLine("End Time:        ${Date(endMs)}")
            appendLine("Duration:        ${f2(hours)} hours (planned: $planned)")
            appendLine("Mode:            ${if (run.independent) "independent" else "follows data collection"}")
            appendLine("Ended by:        $ended")
            appendLine("Paused Dur:      ${f2(pausedMs / 3_600_000.0)} hours")
            appendLine("Interval:        ${run.intervalMs / 1000} seconds")
            appendLine("Samples:         ${rows.size}")
            appendLine("Longest gap:     ${longestGapMs / 1000} seconds")
            appendLine("Wake lock:       ${if (run.holdWakeLock) "held ($WAKE_LOCK_TAG)" else "not held"}")
            appendLine(
                "Battery optim.:  " + if (isBatteryOptimizationExempt()) "exempt" else
                    "NOT exempt (Doze can ignore the wake lock and delay samples)"
            )
            if (charged) appendLine("WARNING:         charging during the run; battery numbers are not drain")
            appendLine()
            if (levelStart != null && levelEnd != null) {
                appendLine("Initial Battery: $levelStart%")
                appendLine("Final Battery:   $levelEnd%")
                appendLine("Total Dropped:   ${levelStart - levelEnd}%")
                if (hours > 0) appendLine("Drain Rate:      ${f2((levelStart - levelEnd) / hours)} %/hr")
            }
            if (chargeStart != null && chargeEnd != null) {
                val dropped = chargeStart - chargeEnd
                appendLine("Initial Charge:  $chargeStart uAh")
                appendLine("Final Charge:    $chargeEnd uAh")
                appendLine("Charge Dropped:  $dropped uAh")
                if (hours > 0) appendLine("Avg Current:     ${f2(dropped / hours / 1000.0)} mA")
            } else {
                appendLine("Charge:          not reported by this device")
            }
            appendLine()
            appendLine("EnPULSE memory (avg / max)")
            appendLine("Total PSS:       ${stats("total_pss_mb")} MB")
            appendLine("Native heap:     ${stats("native_heap_allocated_bytes", mib)} MB allocated")
            appendLine("Java heap:       ${stats("java_heap_used_bytes", mib)} MB used")
            appendLine()
            appendLine("EnPULSE process")
            if (cpuMs != null) {
                appendLine(
                    "CPU time:        ${f2(cpuMs / 1000.0)} s, avg ${f2(cpuMs * 100.0 / spanMs)}% " +
                            "of one core (peak ${stats("cpu_pct").substringAfter("/ ")}%)"
                )
            }
            appendLine("Threads:         ${firstLastMax("threads")}")
            appendLine("Open files:      ${firstLastMax("open_fds")}")
            counterTotal("gc_count")?.let { gc ->
                appendLine("GC:              ${gc.toLong()} collections, ${f2((counterTotal("gc_time_ms") ?: 0.0) / 1000.0)} s")
            }
            appendLine("App data:        ${firstLastMax("app_data_kb")} KB")
            appendLine("Restarts:        $restarts")
            appendLine()
            appendLine("Device (share of the run)")
            appendLine("Screen on:       ${share("screen_on", "true")}")
            appendLine("Doze:            ${share("device_idle", "true")}")
            appendLine("Power saver:     ${share("power_save", "true")}")
            appendLine("Peer connected:  ${share("peer_connected", "true")}")
            appendLine("Networks:        ${networks.joinToString { "$it ${share("network", it)}" }}")
            appendLine("Standby buckets: ${buckets.joinToString()}")
            if (runInfo.isNotEmpty()) {
                appendLine()
                appendLine("Run details")
                runInfo.forEach { (k, v) -> appendLine((k + ":").padEnd(17) + v) }
            }
        })
    }

    private fun sanitize(text: String) = text.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
