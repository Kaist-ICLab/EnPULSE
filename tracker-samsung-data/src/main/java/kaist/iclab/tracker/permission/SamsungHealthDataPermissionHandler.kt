package kaist.iclab.tracker.permission

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kaist.iclab.tracker.R

/**
 * [HealthDataPermissionHandler] backed by the Samsung Health Data SDK. Pass it to
 * [AndroidPermissionManager] to check and request read access to [HealthDataPermissions].
 */
class SamsungHealthDataPermissionHandler(private val context: Context) : HealthDataPermissionHandler {
    companion object {
        private val TAG = SamsungHealthDataPermissionHandler::class.simpleName
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"
    }

    private val dataTypes = mapOf(
        DataTypes.STEPS.name to DataTypes.STEPS,
        DataTypes.SLEEP.name to DataTypes.SLEEP,
        DataTypes.EXERCISE.name to DataTypes.EXERCISE
    )

    private fun readPermissions(ids: Set<String>): Set<Permission> =
        ids.mapNotNull { id -> dataTypes[id]?.let { Permission.of(it, AccessType.READ) } }.toSet()

    private fun toStates(requested: Set<Permission>, granted: Set<Permission>): Map<String, PermissionState> =
        requested.associate { p ->
            p.dataType.name to if (p in granted) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
        }

    override fun queryGranted(
        permissions: Set<String>,
        onResult: (Map<String, PermissionState>) -> Unit,
        onUnsupported: () -> Unit
    ) {
        val store = try {
            HealthDataService.getStore(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HealthDataService store during query", e)
            onUnsupported()
            return
        }

        val requested = readPermissions(permissions)
        if (requested.isEmpty()) return

        store.getGrantedPermissionsAsync(requested).setCallback(
            Looper.getMainLooper(),
            { granted: Set<Permission> -> onResult(toStates(requested, granted)) },
            { error: Throwable ->
                if (error is AuthorizationException) {
                    // Not set as unsupported, so the user can still request it once developer
                    // mode is on.
                    Log.w(TAG, "Samsung Health SDK Authorization Error (2003): Could not get policy. App not registered as partner. Awaiting Developer Mode.")
                } else {
                    Log.e(TAG, "Error in getGrantedPermissionsAsync for Samsung Health", error)
                }

                // In case of a serious platform error, mark as unsupported
                if (error is PlatformInternalException) {
                    onUnsupported()
                }
            }
        )
    }

    override fun request(
        activity: ComponentActivity,
        permissions: Set<String>,
        onResult: (Map<String, PermissionState>) -> Unit
    ) {
        val store = try {
            HealthDataService.getStore(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HealthDataService store", e)
            return
        }

        val requested = readPermissions(permissions)
        if (requested.isEmpty()) return

        store.getGrantedPermissionsAsync(requested).setCallback(
            Looper.getMainLooper(),
            { granted: Set<Permission> ->
                if (granted.containsAll(requested)) {
                    onResult(toStates(requested, granted))
                } else {
                    store.requestPermissionsAsync(requested, activity).setCallback(
                        Looper.getMainLooper(),
                        { granted2: Set<Permission> -> onResult(toStates(requested, granted2)) },
                        { error: Throwable -> handleError(error, activity) }
                    )
                }
            },
            { error: Throwable -> handleError(error, activity) }
        )
    }

    override fun settingsIntent(): Intent? = try {
        context.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } catch (_: Exception) {
        null
    }

    private fun handleError(error: Throwable, activity: ComponentActivity) {
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
                    Toast.makeText(
                        context,
                        activity.getString(R.string.msg_samsung_health_dev_mode_needed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            is InvalidRequestException -> Log.e(TAG, "InvalidRequestException: ${error.message}")
            is PlatformInternalException -> Log.e(TAG, "PlatformInternalException: ${error.message}")
            else -> Log.e(TAG, "Unknown health data error: ${error.message}", error)
        }
    }
}
