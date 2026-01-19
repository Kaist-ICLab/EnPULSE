package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kaist.iclab.tracker.listener.AccessibilityListener
import kaist.iclab.tracker.listener.core.AccessibilityEventInfo
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import java.util.UUID

class UserInteractionSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<UserInteractionSensor.Config, UserInteractionSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    companion object {
        var instance: WeakReference<UserInteractionSensor>? = null

        // Event types to filter out (noisy events)
        private val FILTERED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_VIEW_SCROLLED,           // 4096 - very noisy during scrolling
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,  // 2048 - fires constantly
            AccessibilityEvent.TYPE_VIEW_SELECTED            // 4 - fires during list scrolling
        )
    }

    init {
        instance = WeakReference(this)
    }

    /*No attribute required... can not be data class*/
    class Config : SensorConfig

    @Serializable
    data class Entity(
        val eventId: String,
        val received: Long,
        val timestamp: Long,
        val packageName: String,
        val className: String,
        val eventType: Int,
        val text: String
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE
    ).toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else null
    ).toTypedArray()

    private val accessibilityListener = AccessibilityListener()

    private val mainCallback = mainCallback@{ e: AccessibilityEventInfo ->
        if (e is AccessibilityEventInfo.Interrupt) return@mainCallback // Ignore interrupts
        val event = e as AccessibilityEventInfo.Event

        event.event?.let { accessibilityEvent ->
            val pkg = accessibilityEvent.packageName
            val cls = accessibilityEvent.className
            val eventType = accessibilityEvent.eventType

            // Filter out noisy event types
            if (eventType in FILTERED_EVENT_TYPES) return@mainCallback

            if (pkg != null && cls != null) {
                val timestamp = System.currentTimeMillis()

                // Emit the event to all listeners
                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            UUID.randomUUID().toString(),
                            timestamp,
                            timestamp,
                            pkg.toString(),
                            cls.toString(),
                            eventType,
                            accessibilityEvent.text.toString()
                        )
                    )
                }
            }
        }
    }

    override fun onStart() {
        accessibilityListener.addListener(mainCallback)
    }

    override fun onStop() {
        accessibilityListener.removeListener(mainCallback)
    }
}