package kaist.iclab.tracker.sensor.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/** Timing schedules as the dashboard writes them (see ANDROID-CORE-FLOW-REVIEW C-11). */
class TimingConfigParsingTest {

    @Test
    fun `whole-number schedules parse as before`() {
        val config = TimingSensor.Config.fromJson(
            """[{"value":"Morning","kind":"fixed","timeOfDay":[32400000]},
                {"value":"Random","kind":"esm","minInterval":600000,"maxInterval":1200000,
                 "startOfDay":28800000,"endOfDay":79200000,"numSurvey":3}]"""
        )
        assertEquals(listOf("Morning", "Random"), config.schedules.map { it.value })
        assertEquals(listOf(32_400_000L), config.schedules[0].timeOfDay)
        assertEquals(600_000L, config.schedules[1].minInterval)
    }

    @Test
    fun `decimal milliseconds from fractional minutes are rounded instead of failing`() {
        // 1.1 min and 0.1 min as the dashboard computes them (minutes * 60000).
        val config = TimingSensor.Config.fromJson(
            """[{"value":"Random","kind":"esm","minInterval":6000.000000000001,
                 "maxInterval":66000.00000000001,"numSurvey":2}]"""
        )
        assertEquals(6_000L, config.schedules.single().minInterval)
        assertEquals(66_000L, config.schedules.single().maxInterval)
        assertEquals(2, config.schedules.single().numSurvey)
    }

    @Test
    fun `unknown fields are ignored`() {
        val config = TimingSensor.Config.fromJson(
            """[{"value":"Morning","kind":"fixed","timeOfDay":[32400000],"nextDay":true,"label":"x"}]"""
        )
        assertEquals("Morning", config.schedules.single().value)
    }
}
