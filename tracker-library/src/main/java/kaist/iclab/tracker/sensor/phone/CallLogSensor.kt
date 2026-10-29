package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import kaist.iclab.tracker.listener.AlarmListener
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import kaist.iclab.tracker.MetaData
import kaist.iclab.tracker.PhoneNumberHasher
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CallLogSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>
) : BaseSensor<CallLogSensor.Config, CallLogSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_call_log,
    descriptionResId = R.string.sensor_desc_call_log,
    icon = Icons.Default.Call
) {
    data class Config(
        val interval: Long
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val duration: Long,
        val number: String,
        val type: Int
    ) : SensorEntity()

    override val permissions: Array<String> = listOfNotNull(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
        } else null,
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else null
    ).toTypedArray()

    private val numberHasher = PhoneNumberHasher(context)

    private val alarmListener = AlarmListener(
        context,
        "kaist.iclab.tracker.${name}_REQUEST",
        0x11,
        configStateFlow.value.interval
    )

    private val rowCursor = ProviderRowCursor(context, "${name}_call_id")

    private val mainCallback = { _: Intent? ->
        val current = System.currentTimeMillis()
        // First poll only: start from the lookback window. After that, only rows not yet emitted.
        val lastId = rowCursor.lastId()
        val (selection, selectionArgs) = if (lastId != null) {
            "${CallLog.Calls._ID} > ?" to arrayOf(lastId.toString())
        } else {
            val from = current - configStateFlow.value.interval - TimeUnit.DAYS.toMillis(5)
            "${CallLog.Calls.DATE} BETWEEN ? AND ?" to arrayOf(from.toString(), current.toString())
        }
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.DATE,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            ),
            selection,
            selectionArgs,
            CallLog.Calls._ID + " ASC"
        )
        var maxId: Long? = null
        cursor?.use {
            while (it.moveToNext()) {
                maxId = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls._ID))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val number = numberHasher.hash(it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "")
                val duration = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            timestamp,
                            duration.toLong(),
                            number,
                            type
                        )
                    )
                }
            }
        }
        cursor?.close()
        // Advanced only after emitting, so a crash mid-poll re-emits rows rather than losing them.
        maxId?.let { rowCursor.advanceTo(it) }
        Unit
    }

    override fun onStart() {
        alarmListener.addListener(mainCallback)
    }

    override fun onStop() {
        alarmListener.removeListener(mainCallback)
    }
}