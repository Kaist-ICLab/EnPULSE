package kaist.iclab.mobiletracker.viewmodels.home

import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HomeUiState] data class.
 * Verifies default values and copy semantics.
 */
class HomeUiStateTest {

    @Test
    fun `default state has tracking inactive`() {
        val state = HomeUiState()
        assertFalse(state.isTrackingActive)
    }

    @Test
    fun `default state has all sensor counts at zero`() {
        val state = HomeUiState()
        assertEquals(0, state.locationCount)
        assertEquals(0, state.appUsageCount)
        assertEquals(0, state.activityCount)
        assertEquals(0, state.stepCount)
        assertEquals(0, state.batteryCount)
        assertEquals(0, state.notificationCount)
        assertEquals(0, state.screenCount)
        assertEquals(0, state.connectivityCount)
        assertEquals(0, state.bluetoothCount)
        assertEquals(0, state.ambientLightCount)
        assertEquals(0, state.appListChangeCount)
        assertEquals(0, state.callLogCount)
        assertEquals(0, state.dataTrafficCount)
        assertEquals(0, state.deviceModeCount)
        assertEquals(0, state.mediaCount)
        assertEquals(0, state.messageLogCount)
        assertEquals(0, state.userInteractionCount)
        assertEquals(0, state.wifiScanCount)
    }

    @Test
    fun `default state has all watch sensor counts at zero`() {
        val state = HomeUiState()
        assertEquals(0, state.watchHeartRateCount)
        assertEquals(0, state.watchAccelerometerCount)
        assertEquals(0, state.watchEDACount)
        assertEquals(0, state.watchPPGCount)
        assertEquals(0, state.watchSkinTemperatureCount)
    }

    @Test
    fun `default watch status is DISCONNECTED`() {
        val state = HomeUiState()
        assertEquals(WatchConnectionStatus.DISCONNECTED, state.watchStatus)
    }

    @Test
    fun `default connected devices is empty`() {
        val state = HomeUiState()
        assertTrue(state.connectedDevices.isEmpty())
    }

    @Test
    fun `default userName is null`() {
        val state = HomeUiState()
        assertNull(state.userName)
    }

    @Test
    fun `copy preserves unmodified fields`() {
        val original = HomeUiState()
        val modified = original.copy(
            isTrackingActive = true,
            locationCount = 42,
            userName = "Alice"
        )
        assertTrue(modified.isTrackingActive)
        assertEquals(42, modified.locationCount)
        assertEquals("Alice", modified.userName)
        // Unchanged fields remain defaults
        assertEquals(0, modified.batteryCount)
        assertEquals(WatchConnectionStatus.DISCONNECTED, modified.watchStatus)
    }

    @Test
    fun `equality works for identical states`() {
        val a = HomeUiState(isTrackingActive = true, locationCount = 10)
        val b = HomeUiState(isTrackingActive = true, locationCount = 10)
        assertEquals(a, b)
    }

    @Test
    fun `inequality for different states`() {
        val a = HomeUiState(locationCount = 10)
        val b = HomeUiState(locationCount = 20)
        assertNotEquals(a, b)
    }
}
