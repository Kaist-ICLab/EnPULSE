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
import kaist.iclab.tracker.PhoneNumberHasher
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
    stateStorage: StateStorage<SensorState>,
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

    private val numberHasher = PhoneNumberHasher(context)

    private val alarmListener = AlarmListener(
        context,
        "kaist.iclab.tracker.${name}_REQUEST",
        0x13,
        configStateFlow.value.interval
    )

    private val smsRowCursor = ProviderRowCursor(context, "${name}_sms_id")
    private val mmsRowCursor = ProviderRowCursor(context, "${name}_mms_id")

    private val mainCallback = { _: Intent? ->
        val current = System.currentTimeMillis()
        val from = current - configStateFlow.value.interval - TimeUnit.DAYS.toMillis(1)

        // READ SMS. First poll only: start from the lookback window; after that, only rows not
        // yet emitted (see ProviderRowCursor).
        val lastSmsId = smsRowCursor.lastId()
        val (smsSelection, smsArgs) = if (lastSmsId != null) {
            "${Telephony.Sms._ID} > ?" to arrayOf(lastSmsId.toString())
        } else {
            "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.DATE} <= ?" to
                    arrayOf(from.toString(), current.toString())
        }
        var cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,   // Sender/receiver number
                Telephony.Sms.DATE,      // Timestamp
                Telephony.Sms.TYPE,      // Type (Incoming/Outgoing)
            ),
            smsSelection,
            smsArgs,
            Telephony.Sms._ID + " ASC"
        )

        var maxSmsId: Long? = null
        cursor?.use {
            while (it.moveToNext()) {
                maxSmsId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val number = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val hashedNumber = if(number != null) numberHasher.hash(number) else "UNKNOWN"
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            timestamp,
                            hashedNumber,
                            "SMS",
                            type
                        )
                    )
                }
            }
        }
        cursor?.close()
        // Advanced only after emitting, so a crash mid-poll re-emits rows rather than losing them.
        maxSmsId?.let { smsRowCursor.advanceTo(it) }

        // READ MMS (DATE is in seconds here)
        val lastMmsId = mmsRowCursor.lastId()
        val (mmsSelection, mmsArgs) = if (lastMmsId != null) {
            "${Telephony.Mms._ID} > ?" to arrayOf(lastMmsId.toString())
        } else {
            "${Telephony.Mms.DATE} > ? AND ${Telephony.Mms.DATE} <= ?" to
                    arrayOf((from / 1000).toString(), (current / 1000).toString())
        }
        cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(
                Telephony.Mms._ID,       // MMS ID
                Telephony.Mms.DATE,      // Timestamp
                Telephony.Mms.MESSAGE_BOX // Type (Incoming/Outgoing)
            ),
            mmsSelection,
            mmsArgs,
            Telephony.Mms._ID + " ASC"
        )

        var maxMmsId: Long? = null
        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getLong(it.getColumnIndexOrThrow(Telephony.Mms._ID))
                maxMmsId = number
                val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Mms.DATE))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX))
                val mmsAddress = getMmsAddress(context.contentResolver, number)
                val hashedMmsAddress = if(mmsAddress != null) numberHasher.hash(mmsAddress) else "UNKNOWN"

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            TimeUnit.SECONDS.toMillis(timestamp),
                            hashedMmsAddress,
                            "MMS",
                            type
                        )
                    )
                }
            }
        }
        cursor?.close()
        maxMmsId?.let { mmsRowCursor.advanceTo(it) }
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