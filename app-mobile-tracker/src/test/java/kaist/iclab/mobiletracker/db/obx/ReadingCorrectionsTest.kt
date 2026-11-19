package kaist.iclab.mobiletracker.db.obx

import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCorrectionsTest {

    private fun step(timestamp: Long, steps: Long, eventId: String, id: Long = 0, received: Long = 0) =
        StepEntity(id = id, eventId = eventId, timestamp = timestamp, steps = steps, received = received)

    private fun select(incoming: List<StepEntity>, existing: List<StepEntity> = emptyList()) =
        selectReadingsToStore(incoming, existing) { it.duration to it.steps }

    @Test
    fun `a new reading is stored as it is`() {
        val result = select(listOf(step(1000, 5, "new")))

        assertEquals("new", result.single().eventId)
        assertEquals(0L, result.single().id)
    }

    @Test
    fun `an unchanged re-read is dropped even if its received time differs`() {
        val result = select(
            incoming = listOf(step(1000, 5, "new", received = 200)),
            existing = listOf(step(1000, 5, "old", id = 7, received = 100))
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a correction keeps the stored event id and gets a new row`() {
        val result = select(
            incoming = listOf(step(1000, 9, "new")),
            existing = listOf(step(1000, 5, "old", id = 7))
        ).single()

        assertEquals("old", result.eventId)
        assertEquals(0L, result.id)
        assertEquals(9L, result.steps)
    }

    @Test
    fun `the newest reading in a batch wins`() {
        val result = select(listOf(step(1000, 5, "first"), step(1000, 9, "second"))).single()

        assertEquals(9L, result.steps)
    }

    @Test
    fun `different device types are separate readings`() {
        val phone = step(1000, 5, "phone")
        val watch = step(1000, 5, "watch").apply { deviceType = 1 }

        assertEquals(2, select(listOf(phone, watch)).size)
    }
}
