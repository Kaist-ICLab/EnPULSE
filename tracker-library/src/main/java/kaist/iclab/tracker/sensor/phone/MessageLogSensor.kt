package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.provider.Telephony
import androidx.core.net.toUri
import kaist.iclab.tracker.listener.AlarmListener
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit


class MessageLogSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<MessageLogSensor.Config, MessageLogSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_message,
    descriptionResId = R.string.sensor_desc_message,
    icon = Icons.Default.Email
) {
    data class Config(
        val interval: Long
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val number: String,
        val messageType: String,
        val contactType: Int
    ) : SensorEntity()

    override val permissions: Array<String> = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_SMS
    )
    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    ).toTypedArray()

    private val alarmListener = AlarmListener(
        context,
        "kaist.iclab.tracker.${name}_REQUEST",
        0x13,
        configStateFlow.value.interval
    )

    private val mainCallback = { _: Intent? ->
        val current = System.currentTimeMillis()
        val from = current - configStateFlow.value.interval - TimeUnit.DAYS.toMillis(1)

        // READ SMS
        var cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,   // Sender/receiver number
                Telephony.Sms.DATE,      // Timestamp
                Telephony.Sms.TYPE,      // Type (Incoming/Outgoing)
            ),
            "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(from.toString(), current.toString()),
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val number = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            timestamp,
                            number,
                            "SMS",
                            type
                        )
                    )
                }
            }
        }
        cursor?.close()

        // READ MMS
        cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(
                Telephony.Mms._ID,       // MMS ID
                Telephony.Mms.DATE,      // Timestamp
                Telephony.Mms.MESSAGE_BOX // Type (Incoming/Outgoing)
            ),
            "${Telephony.Mms.DATE} > ? AND ${Telephony.Mms.DATE} <= ?",
            arrayOf((from / 1000).toString(), (current / 1000).toString()),
            Telephony.Mms.DATE + " DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getLong(it.getColumnIndexOrThrow(Telephony.Mms._ID))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Mms.DATE))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX))

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            TimeUnit.SECONDS.toMillis(timestamp),
                            getMmsAddress(context.contentResolver, number) ?: "UNKNOWN",
                            "MMS",
                            type
                        )
                    )
                }
            }
        }
        cursor?.close()
        Unit
    }

    private fun getMmsAddress(contentResolver: ContentResolver, mmsId: Long): String? {
        // Build the URI for the MMS address table using the MMS ID
        val uri = "content://mms/$mmsId/addr".toUri()
        val projection = arrayOf(Telephony.Mms.Addr.ADDRESS)

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS))
            }
        }
        return null
    }

    override fun onStart() {
        alarmListener.addListener(mainCallback)
    }

    override fun onStop() {
        alarmListener.removeListener(mainCallback)
    }
}