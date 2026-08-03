package kaist.iclab.mobiletracker.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for repository data models and enums.
 * Tests [SensorInfo], [SensorDetailInfo], [SensorRecord], [DateFilter], [PageSize], [SortOrder].
 */
class DataModelsTest {

    // ========== SensorInfo Tests ==========

    @Test
    fun `SensorInfo can be created with all fields`() {
        val info = SensorInfo(
            sensorId = "location",
            displayName = "Location Sensor",
            recordCount = 100,
            lastRecordedTime = 1700000000000L,
            isWatchSensor = false,
            isPhoneSensor = true
        )
        assertEquals("location", info.sensorId)
        assertEquals("Location Sensor", info.displayName)
        assertEquals(100, info.recordCount)
        assertEquals(1700000000000L, info.lastRecordedTime)
        assertFalse(info.isWatchSensor)
        assertTrue(info.isPhoneSensor)
    }

    @Test
    fun `SensorInfo defaults isWatchSensor and isPhoneSensor to false`() {
        val info = SensorInfo(
            sensorId = "test",
            displayName = "Test",
            recordCount = 0,
            lastRecordedTime = null
        )
        assertFalse(info.isWatchSensor)
        assertFalse(info.isPhoneSensor)
    }

    @Test
    fun `SensorInfo lastRecordedTime can be null`() {
        val info = SensorInfo(
            sensorId = "test",
            displayName = "Test",
            recordCount = 0,
            lastRecordedTime = null
        )
        assertNull(info.lastRecordedTime)
    }

    @Test
    fun `SensorInfo equality`() {
        val a = SensorInfo("s1", "Sensor 1", 10, 12345L)
        val b = SensorInfo("s1", "Sensor 1", 10, 12345L)
        assertEquals(a, b)
    }

    // ========== SensorDetailInfo Tests ==========

    @Test
    fun `SensorDetailInfo can be created with all fields`() {
        val detail = SensorDetailInfo(
            sensorId = "heartRate",
            displayName = "Heart Rate",
            totalRecords = 500,
            todayRecords = 42,
            lastRecordedTime = 1700000000000L,
            lastSyncTimestamp = 1699999000000L,
            isWatchSensor = true,
            isPhoneSensor = false
        )
        assertEquals("heartRate", detail.sensorId)
        assertEquals(500, detail.totalRecords)
        assertEquals(42, detail.todayRecords)
        assertTrue(detail.isWatchSensor)
        assertFalse(detail.isPhoneSensor)
        assertNotNull(detail.lastSyncTimestamp)
    }

    @Test
    fun `SensorDetailInfo defaults lastSyncTimestamp to null`() {
        val detail = SensorDetailInfo(
            sensorId = "test",
            displayName = "Test",
            totalRecords = 0,
            todayRecords = 0,
            lastRecordedTime = null
        )
        assertNull(detail.lastSyncTimestamp)
    }

    // ========== SensorRecord Tests ==========

    @Test
    fun `SensorRecord stores fields as map`() {
        val record = SensorRecord(
            id = 1L,
            timestamp = 1700000000000L,
            fields = mapOf(
                "latitude" to "37.5665",
                "longitude" to "126.978",
                "accuracy" to "15.0"
            )
        )
        assertEquals(1L, record.id)
        assertEquals(3, record.fields.size)
        assertEquals("37.5665", record.fields["latitude"])
    }

    @Test
    fun `SensorRecord with empty fields`() {
        val record = SensorRecord(id = 2L, timestamp = 0L, fields = emptyMap())
        assertTrue(record.fields.isEmpty())
    }

    // ========== DateFilter Tests ==========

    @Test
    fun `DateFilter has all expected values`() {
        val values = DateFilter.entries.toTypedArray()
        assertEquals(5, values.size)
        assertTrue(values.contains(DateFilter.TODAY))
        assertTrue(values.contains(DateFilter.LAST_7_DAYS))
        assertTrue(values.contains(DateFilter.LAST_30_DAYS))
        assertTrue(values.contains(DateFilter.ALL_TIME))
        assertTrue(values.contains(DateFilter.CUSTOM))
    }

    @Test
    fun `DateFilter valueOf works`() {
        assertEquals(DateFilter.TODAY, DateFilter.valueOf("TODAY"))
        assertEquals(DateFilter.LAST_7_DAYS, DateFilter.valueOf("LAST_7_DAYS"))
    }

    // ========== PageSize Tests ==========

    @Test
    fun `PageSize has correct values`() {
        assertEquals(25, PageSize.SIZE_25.value)
        assertEquals(50, PageSize.SIZE_50.value)
        assertEquals(100, PageSize.SIZE_100.value)
        assertEquals(250, PageSize.SIZE_250.value)
        assertEquals(500, PageSize.SIZE_500.value)
        assertEquals(1000, PageSize.SIZE_1000.value)
    }

    @Test
    fun `PageSize has 6 options`() {
        assertEquals(6, PageSize.entries.size)
    }

    // ========== SortOrder Tests ==========

    @Test
    fun `SortOrder has exactly 2 values`() {
        val values = SortOrder.entries.toTypedArray()
        assertEquals(2, values.size)
        assertTrue(values.contains(SortOrder.NEWEST_FIRST))
        assertTrue(values.contains(SortOrder.OLDEST_FIRST))
    }
}
