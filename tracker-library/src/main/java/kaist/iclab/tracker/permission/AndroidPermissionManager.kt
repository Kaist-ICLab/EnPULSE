package kaist.iclab.tracker.permission

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.request.DataTypes
import kaist.iclab.tracker.R
import kaist.iclab.tracker.listener.AccessibilityListener
import kaist.iclab.tracker.listener.NotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class AndroidPermissionManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PermissionManager {
    companion object {
        private val TAG = AndroidPermissionManager::class.simpleName
        private const val PREFS_NAME = "permission_tracking"
        private const val KEY_PREFIX_REQUESTED = "permission_requested_"
        private const val KEY_PREFIX_HEALTH_STATE = "health_permission_state_"
    }

    private var activityWeakRef: WeakReference<ComponentActivity>? = null
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private val permissionStateFlow: MutableStateFlow<Map<String, PermissionState>> =
        MutableStateFlow(mapOf())

    private val permissionTrackingPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val specialPermissions = buildMap {
        put(Manifest.permission.PACKAGE_USAGE_STATS, ::requestPackageUsageStat)
        put(
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            ::requestBindNotificationListenerService
        )
        put(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, ::requestBindAccessibilityService)
        put(Manifest.permission.SYSTEM_ALERT_WINDOW, ::requestSystemAlertWindow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) put(
            Manifest.permission.SCHEDULE_EXACT_ALARM,
            ::requestScheduleExactAlarm
        )
        put(
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            ::requestIgnoreBatteryOptimizations
        )
    }

    val healthDataPermission = mapOf(
        DataTypes.STEPS.name to DataTypes.STEPS,
        DataTypes.SLEEP.name to DataTypes.SLEEP,
        DataTypes.EXERCISE.name to DataTypes.EXERCISE
    )

    override fun registerPermission(newPermissions: Array<String>) {
        permissionStateFlow.value = permissionStateFlow.value.toMutableMap().apply {
            putAll(newPermissions.associateWith { p -> getPermissionState(p) })
        }

        // If Samsung Health permissions were registered, trigger notifyChange to check their actual state
        val hasHealthPermissions = newPermissions.any { it in healthDataPermission.keys }
        if (hasHealthPermissions && activityWeakRef?.get() != null) {
            notifyChange()
        }
    }

    /**
     * Automatically registers permissions if they haven't been registered yet.
     * This ensures permissions are tracked in the permission state flow so that
     * notifyChange() can update them after permission requests.
     *
     * @param permissions Array of permission IDs to register
     */
    private fun ensurePermissionsRegistered(permissions: Array<String>) {
        val unregisteredPermissions = permissions.filter { it !in permissionStateFlow.value.keys }
        if (unregisteredPermissions.isNotEmpty()) {
            registerPermission(unregisteredPermissions.toTypedArray())
        }
    }

    /*Stores [activity] using a [WeakReference]. Call it on [Activity.onCreate]*/
    @MainThread
    override fun bind(activity: ComponentActivity) {
        activityWeakRef = WeakReference(activity)
        notifyChange()
        permissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                // Only mark permissions as requested if they were actually processed by Android
                // (i.e., they appear in the results map). Permissions not in the manifest
                // won't appear in results, so we shouldn't mark them as requested.
                results.keys.forEach { permission ->
                    if (!hasRequestedPermission(permission)) {
                        markPermissionRequested(permission)
                    }
                }
                notifyChange()
            }

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                notifyChange() // Call notifyChange() every time activity resumes
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                activityWeakRef?.clear()
                activityWeakRef = null
                permissionLauncher = null
            }
        })
    }

    /**
     * Notify change to stateflow by checking the current state of permissions.
     *
     * 1. special permissions (e.g., Manifest.permission.PACKAGE_USAGE_STATS) that
     * cannot be enabled directly within the app and must be granted through the settings screen.
     * Since it is difficult to register a callback for these permissions, this method will be utilized for that
     * permission state updates are properly propagated when the Activity resumes.
     *
     * 2. run-time permissions provide callback for permission state updates. This method will be utilized for that
     * permission state updates are properly propagated to flow.
     */
    private fun notifyChange() {
        val permissions = permissionStateFlow.value.keys
        permissionStateFlow.value = permissions
            .filter { it !in healthDataPermission.keys }.associateWith { getPermissionState(it) }

        // Query Samsung Health permissions only on Samsung devices if they have been registered
        val hasRegisteredHealthPermissions = permissions.any { it in healthDataPermission.keys }
        if (hasRegisteredHealthPermissions) {
            if (HardwareAvailabilityChecker.isSamsungDevice()) {
                querySamsungHealthPermissions()
            } else {
                setSamsungHealthPermissionsUnsupported()
            }
        }
    }

    private fun querySamsungHealthPermissions() {
        val store = try {
            HealthDataService.getStore(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HealthDataService store during query", e)
            setSamsungHealthPermissionsUnsupported()
            return
        }

        val healthDataPermissionSet = healthDataPermission.values.map {
            com.samsung.android.sdk.health.data.permission.Permission.of(it, AccessType.READ)
        }.toSet()

        store.getGrantedPermissionsAsync(healthDataPermissionSet).setCallback(
            Looper.getMainLooper(),
            { res: Set<com.samsung.android.sdk.health.data.permission.Permission>
                ->
                setHealthDataPermissionState(healthDataPermissionSet, res)
            },
            { error: Throwable ->
                if (error is AuthorizationException) {
                    Log.w(TAG, "Samsung Health SDK Authorization Error (2003): Could not get policy. App not registered as partner. Awaiting Developer Mode.")
                    // DO NOT set as unsupported here, so the user can still trigger the Toast and request it once dev mode is on.
                } else {
                    Log.e(TAG, "Error in getGrantedPermissionsAsync for Samsung Health", error)
                }
                
                // In case of a serious platform error, mark as unsupported
                if (error is PlatformInternalException) {
                    setSamsungHealthPermissionsUnsupported()
                }
            }
        )
    }

    private fun setSamsungHealthPermissionsUnsupported() {
        val healthPermissionStates =
            healthDataPermission.keys.associateWith { PermissionState.UNSUPPORTED }
        permissionStateFlow.value = permissionStateFlow.value.toMutableMap().apply {
            putAll(healthPermissionStates)
        }
        // Cache the unsupported state
        permissionTrackingPrefs.edit {
            healthDataPermission.keys.forEach { name ->
                putString("$KEY_PREFIX_HEALTH_STATE$name", PermissionState.UNSUPPORTED.name)
            }
        }
    }

    override fun getPermissionFlow(permissions: Array<String>): StateFlow<Map<String, PermissionState>> {
        // Auto-register permissions before observing the flow.
        // This ensures permissions are tracked so that state updates (via notifyChange()) 
        // can be properly propagated to observers.
        ensurePermissionsRegistered(permissions)

        val initialValue = permissionStateFlow.value.filterKeys { it in permissions }

        return permissionStateFlow.map { stateMap ->
            stateMap.filterKeys { it in permissions.toList() }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly, 
            initialValue = initialValue
        )
    }

    private fun getActivity(): ComponentActivity? {
        return activityWeakRef?.get()
    }

    private fun getPermissionState(permission: String): PermissionState {
        // For Samsung Health permissions, check if device is Samsung
        if (permission in healthDataPermission.keys) {
            // If not Samsung device, mark as UNSUPPORTED
            if (!HardwareAvailabilityChecker.isSamsungDevice()) {
                return PermissionState.UNSUPPORTED
            }
            
            // Read cached state if available to avoid flicker while querying SDK
            val cachedState = permissionTrackingPrefs.getString("$KEY_PREFIX_HEALTH_STATE$permission", null)
            if (cachedState != null) {
                return try { PermissionState.valueOf(cachedState) } catch (_: Exception) { PermissionState.NOT_REQUESTED }
            }
            
            // For Samsung devices, return NOT_REQUESTED initially if no cache
            // The actual state will be updated asynchronously by notifyChange() which queries the Samsung Health SDK
            return PermissionState.NOT_REQUESTED
        }

        return when (permission) {
            Manifest.permission.PACKAGE_USAGE_STATS -> getPackageUsageStatsPermissionState()
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> getBindAccessibilityServicePermissionState()
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> getBindNotificationListenerServicePermissionState()
            Manifest.permission.SYSTEM_ALERT_WINDOW -> getSystemAlertWindowPermissionState()
            Manifest.permission.SCHEDULE_EXACT_ALARM -> getScheduleExactAlarmPermissionState()
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> getIgnoreBatteryOptimizationsPermissionState()
            else -> getRuntimePermissionState(permission)
        }
    }

    private fun setHealthDataPermissionState(
        requestedPermission: Set<com.samsung.android.sdk.health.data.permission.Permission>,
        grantedPermission: Set<com.samsung.android.sdk.health.data.permission.Permission>
    ) {
        val permissionMap = requestedPermission.associate { p ->
            val isGranted = grantedPermission.any { it.dataType.name == p.dataType.name && it.accessType == p.accessType }
            val state = if (isGranted) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
            p.dataType.name to state
        }

        permissionStateFlow.value = permissionStateFlow.value.toMutableMap().apply {
            putAll(permissionMap)
        }
        
        // Cache the results
        permissionTrackingPrefs.edit {
            permissionMap.forEach { (name, state) ->
                putString("$KEY_PREFIX_HEALTH_STATE$name", state.name)
            }
        }
    }

    private fun getRuntimePermissionState(permission: String): PermissionState {
        // Check if hardware is available for permissions that require specific hardware
        if (!HardwareAvailabilityChecker.isHardwareAvailable(context, permission)) {
            return PermissionState.UNSUPPORTED
        }

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // If granted, clear the "requested" flag and return GRANTED
        if (isGranted) {
            clearPermissionRequested(permission)
            return PermissionState.GRANTED
        }

        val activity = getActivity()
        val shouldShowRationale = if (activity != null) {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        } else {
            false
        }

        // If shouldShowRationale is true, user denied but can still be asked
        if (shouldShowRationale) {
            return PermissionState.RATIONALE_REQUIRED
        }

        // Permission is denied and shouldShowRationale is false
        // Need to distinguish between "never requested" and "permanently denied"
        val hasRequestedBefore = hasRequestedPermission(permission)

        return if (hasRequestedBefore) {
            // We've requested before, but permission is denied and rationale can't be shown
            // This means the user has permanently denied it
            PermissionState.PERMANENTLY_DENIED
        } else {
            // Never requested before
            PermissionState.NOT_REQUESTED
        }
    }

    /**
     * Check if we've requested this permission before.
     * Used to distinguish between "never requested" and "permanently denied".
     */
    private fun hasRequestedPermission(permission: String): Boolean {
        return permissionTrackingPrefs.getBoolean("$KEY_PREFIX_REQUESTED$permission", false)
    }

    /**
     * Mark that we've requested this permission.
     * Called when we actually launch a permission request.
     */
    private fun markPermissionRequested(permission: String) {
        permissionTrackingPrefs.edit {
            putBoolean("$KEY_PREFIX_REQUESTED$permission", true)
        }
    }

    /**
     * Clear the "requested" flag for a permission.
     * Called when permission is granted.
     */
    private fun clearPermissionRequested(permission: String) {
        permissionTrackingPrefs.edit {
            remove("$KEY_PREFIX_REQUESTED$permission")
        }
    }

    private fun getPackageUsageStatsPermissionState(): PermissionState {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 36) {
            // Use checkOpNoThrow for API 36+ (recommended replacement for deprecated unsafeCheckOpNoThrow)
            // Reference: https://developer.android.com/reference/android/app/AppOpsManager
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else
        // Use unsafeCheckOpNoThrow for API 29-35 (available from API 29, deprecated in API 36)
        // Reference: https://developer.android.com/reference/android/app/AppOpsManager#unsafeCheckOpNoThrow
            @Suppress("DEPRECATION")
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        return if (mode == AppOpsManager.MODE_ALLOWED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
    }

    private fun getBindAccessibilityServicePermissionState(): PermissionState {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return PermissionState.NOT_REQUESTED

        val enabledServicesList = TextUtils.split(enabledServices, ":")
        val fullServiceName =
            "${context.packageName}/${AccessibilityListener::class.java.canonicalName}$${AccessibilityListener.AccessibilityServiceAdaptor::class.simpleName}"

        val isServiceRunning = accessibilityManager.getEnabledAccessibilityServiceList(
            FEEDBACK_ALL_MASK
        ).any { it.id == fullServiceName }

        return if (enabledServicesList.contains(fullServiceName) && isServiceRunning) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
    }

    private fun getBindNotificationListenerServicePermissionState(): PermissionState {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (notificationManager.isNotificationListenerAccessGranted(
                ComponentName(
                    context,
                    NotificationListener.NotificationListenerServiceAdaptor::class.java
                )
            )
        ) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
    }

    private fun getSystemAlertWindowPermissionState(): PermissionState {
        return if (Settings.canDrawOverlays(context)) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
    }

    private fun getScheduleExactAlarmPermissionState(): PermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return PermissionState.GRANTED
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (alarmManager.canScheduleExactAlarms()) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
    }

    private fun getIgnoreBatteryOptimizationsPermissionState(): PermissionState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_REQUESTED
        }
    }

    @SuppressLint("InlinedApi")
    override fun request(permissions: Array<String>) {
        /*Decide which permission to handle first\
        * 1. Special permission other than background location will handle first
        * 2. Background location permission will handle second
        * 3. If there is no special permission, handle normal permissions
        * */

        // Auto-register permissions before requesting.
        // This is critical: notifyChange() only updates permissions that are already in the flow.
        // If permissions aren't registered, notifyChange() won't update them after the user grants/denies.
        ensurePermissionsRegistered(permissions)

        var specialPermission: String = specialPermissions.keys.find { it in permissions } ?: ""
        var callback: () -> Unit = specialPermissions[specialPermission] ?: {}


        if (Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions) {
            /* ACCESS_FINE_LOCATION is required before turning on this permission */
            specialPermission =
                if (getPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) != PermissionState.GRANTED) {
                    Manifest.permission.ACCESS_FINE_LOCATION
                } else {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }
            callback = { requestNormalPermissions(arrayOf(specialPermission)) }
        } else if (Manifest.permission.BODY_SENSORS_BACKGROUND in permissions) {
            specialPermission =
                if (getPermissionState(Manifest.permission.BODY_SENSORS) != PermissionState.GRANTED) {
                    Manifest.permission.BODY_SENSORS
                } else {
                    Manifest.permission.BODY_SENSORS_BACKGROUND
                }
            callback = { requestNormalPermissions(arrayOf(specialPermission)) }
        } else if (HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND in permissions) {
            val prerequisite = permissions
                .filter {
                    it in listOf(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_SKIN_TEMPERATURE
                    )
                }
                .filter { getPermissionState(it) != PermissionState.GRANTED }

            specialPermission =
                if (prerequisite.isNotEmpty()) {
                    prerequisite.first()
                } else {
                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
                }
            callback = { requestNormalPermissions(arrayOf(specialPermission)) }
        }

        if (specialPermission != "") {
            scope.launch {
                permissionStateFlow.collect { permissionState ->
                    if (permissionState[specialPermission] == PermissionState.GRANTED) {
                        // Request a normal permission first that is required to grant a special permission
                        request(permissions.filter { it != specialPermission }.toTypedArray())
                        this.cancel()
                    }
                }
            }
            callback()
        } else {
            val normalPermission = permissions.filter { it !in healthDataPermission.keys }
            val healthPermission = permissions.filter { it in healthDataPermission.keys }
            requestNormalPermissions(normalPermission.toTypedArray())
            requestHealthDataPermission(healthPermission.toTypedArray())
        }
    }

    private fun requestNormalPermissions(permissions: Array<String>) {
        // Don't mark permissions as requested here - wait for the callback result.
        // This ensures we only mark permissions that were actually processable by Android
        // (i.e., they're in the manifest). Permissions not in the manifest won't appear
        // in the callback results, so they won't be marked as requested.
        permissionLauncher?.launch(permissions)
    }

    private fun requestHealthDataPermission(permissions: Array<String>) {
        if (permissions.isEmpty()) return
        
        Log.v(TAG, "Requesting Samsung Health permissions: ${permissions.joinToString()}")
        
        val store = try {
            HealthDataService.getStore(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HealthDataService store", e)
            return
        }

        val activity = getActivity() ?: return

        val possiblePermission = permissions
            .mapNotNull { id ->
                healthDataPermission[id]?.let {
                    com.samsung.android.sdk.health.data.permission.Permission.of(it, AccessType.READ)
                }
            }
            .toSet()

        if (possiblePermission.isEmpty()) {
            return
        }

        store.getGrantedPermissionsAsync(possiblePermission).setCallback(
            Looper.getMainLooper(),
            { res: Set<com.samsung.android.sdk.health.data.permission.Permission> ->
                Log.v(TAG, "Granted permissions check result: ${res.size}/${possiblePermission.size}")
                val allGranted = possiblePermission.all { req -> 
                    res.any { it.dataType.name == req.dataType.name && it.accessType == req.accessType }
                }
                
                if (allGranted) {
                    setHealthDataPermissionState(possiblePermission, res)
                } else {
                    Log.v(TAG, "Requesting missing health permissions...")
                    store.requestPermissionsAsync(possiblePermission, activity).setCallback(
                        Looper.getMainLooper(),
                        { res2: Set<com.samsung.android.sdk.health.data.permission.Permission> ->
                            setHealthDataPermissionState(possiblePermission, res2)
                        },
                        { error: Throwable ->
                            handleHealthDataError(error, activity)
                        }
                    )
                }
            },
            { error: Throwable ->
                handleHealthDataError(error, activity)
            }
        )
    }

    private fun handleHealthDataError(error: Throwable, activity: ComponentActivity) {
        when (error) {
            is ResolvablePlatformException -> {
                if (error.hasResolution) {
                    try {
                        error.resolve(activity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve health platform exception", e)
                    }
                }
            }

            is AuthorizationException -> {
                Log.w(TAG, "AuthorizationException: Samsung Health Data Developer mode might be needed")
                Handler(Looper.getMainLooper()).post {
                    val activity = getActivity()
                    val message = activity?.getString(R.string.msg_samsung_health_dev_mode_needed)
                        ?: context.getString(R.string.msg_samsung_health_dev_mode_needed)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
            
            is InvalidRequestException -> Log.e(TAG, "InvalidRequestException: ${error.message}")
            is PlatformInternalException -> Log.e(TAG, "PlatformInternalException: ${error.message}")
            else -> Log.e(TAG, "Unknown health data error: ${error.message}", error)
        }
    }

    private fun requestPackageUsageStat() {
        if (getPermissionState(Manifest.permission.PACKAGE_USAGE_STATS) == PermissionState.GRANTED) return
        getActivity()?.startActivity(createUsageAccessSettingsIntent())
    }

    private fun requestBindAccessibilityService() {
        if (getPermissionState(Manifest.permission.BIND_ACCESSIBILITY_SERVICE) == PermissionState.GRANTED) return
        getActivity()?.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestBindNotificationListenerService() {
        if (getPermissionState(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) == PermissionState.GRANTED) return
        getActivity()?.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestSystemAlertWindow() {
        if (getSystemAlertWindowPermissionState() == PermissionState.GRANTED) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        getActivity()?.startActivity(intent)
    }

    private fun requestScheduleExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (getPermissionState(Manifest.permission.SCHEDULE_EXACT_ALARM) == PermissionState.GRANTED) return
        getActivity()?.startActivity(createScheduleExactAlarmIntent())
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (getPermissionState(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PermissionState.GRANTED) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
        getActivity()?.startActivity(intent)
    }

    private fun createUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    private fun createScheduleExactAlarmIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    private fun createAppDetailsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Opens the appropriate settings page for a permission based on its ID.
     * This allows users to change or revoke granted permissions.
     *
     * @param permissionId The permission ID to open settings for
     */
    fun openPermissionSettings(permissionId: String) {
        val intent = when (permissionId) {
            Manifest.permission.PACKAGE_USAGE_STATS -> {
                createUsageAccessSettingsIntent().apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            in healthDataPermission.keys -> {
                // For Samsung Health permissions, try to open Samsung Health
                try {
                    context.packageManager.getLaunchIntentForPackage("com.sec.android.app.shealth")?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: createAppDetailsIntent()
                } catch (_: Exception) {
                    createAppDetailsIntent()
                }
            }
            else -> createAppDetailsIntent()
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: open general app settings
            context.startActivity(createAppDetailsIntent())
        }
    }
}