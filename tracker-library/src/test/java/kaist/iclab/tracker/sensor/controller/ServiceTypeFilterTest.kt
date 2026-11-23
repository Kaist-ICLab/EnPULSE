package kaist.iclab.tracker.sensor.controller

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceTypeFilterTest {
    private val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    private val microphone = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    private val connectedDevice = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    private val health = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
    private val specialUse = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE

    // Below Android 14 the health type doesn't exist, so the caller passes null.
    @Test
    fun api30KeepsAllTypesWithoutHealthPermission() {
        val types = listOf(location, microphone, connectedDevice)
        assertEquals(types, BackgroundController.dropUnusableHealthType(types, null, false))
    }

    @Test
    fun api33KeepsAllTypesWithoutHealthPermission() {
        val types = listOf(location, microphone)
        assertEquals(types, BackgroundController.dropUnusableHealthType(types, null, false))
    }

    @Test
    fun api33KeepsAllTypesWithHealthPermission() {
        val types = listOf(location, microphone)
        assertEquals(types, BackgroundController.dropUnusableHealthType(types, null, true))
    }

    @Test
    fun api34DropsOnlyHealthWithoutHealthPermission() {
        val types = listOf(health, specialUse, location, microphone)
        assertEquals(
            listOf(specialUse, location, microphone),
            BackgroundController.dropUnusableHealthType(types, health, false)
        )
    }

    @Test
    fun api34KeepsHealthWithHealthPermission() {
        val types = listOf(health, specialUse, location, microphone)
        assertEquals(types, BackgroundController.dropUnusableHealthType(types, health, true))
    }
}
