package kaist.iclab.tracker.sensor.galaxywatch

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class StressMathTest {

    @Test
    fun `rmssd of a known IBI series`() {
        // Successive differences 10, -20, 10 -> mean square (100 + 400 + 100) / 3 = 200.
        val rmssd = StressMath.rmssd(intArrayOf(800, 810, 790, 800))
        assertEquals(sqrt(200.0).toFloat(), rmssd, 1e-4f)
    }

    @Test
    fun `rmssd of a constant series is zero`() {
        assertEquals(0f, StressMath.rmssd(intArrayOf(750, 750, 750)), 0f)
    }

    @Test
    fun `median of odd and even sized lists`() {
        assertEquals(3.0, StressMath.median(listOf(5.0, 1.0, 3.0)), 0.0)
        assertEquals(2.5, StressMath.median(listOf(4.0, 1.0, 3.0, 2.0)), 0.0)
    }

    @Test
    fun `outlier bounds are median plus or minus 3 MAD`() {
        // Median 800; absolute deviations 0, 10, 10, 20, 20 -> MAD 10 -> bounds 770..830.
        val bounds = StressMath.outlierBounds(intArrayOf(800, 790, 810, 780, 820))
        assertEquals(770.0, bounds.start, 1e-9)
        assertEquals(830.0, bounds.endInclusive, 1e-9)
    }

    @Test
    fun `outlier bounds ignore physiologically impossible beats`() {
        // 100 ms (600 bpm) and 5000 ms (12 bpm) are outside 30-220 bpm and must not shift the median.
        val withJunk = StressMath.outlierBounds(intArrayOf(100, 800, 790, 810, 780, 820, 5000))
        val clean = StressMath.outlierBounds(intArrayOf(800, 790, 810, 780, 820))
        assertEquals(clean, withJunk)
    }

    @Test
    fun `no spread gives all-inclusive bounds, no plausible data gives empty bounds`() {
        val flat = StressMath.outlierBounds(intArrayOf(800, 800, 800))
        assertTrue(1_000_000.0 in flat)

        val none = StressMath.outlierBounds(intArrayOf(50, 9000))
        assertFalse(800.0 in none)
    }

    @Test
    fun `filter keeps only plausible beats inside the bounds`() {
        val filtered = StressMath.filterIbis(intArrayOf(100, 775, 800, 900, 5000), 770.0..830.0)
        assertArrayEquals(intArrayOf(775, 800), filtered)
    }

    @Test
    fun `ibisSince keeps beats at or after the window start`() {
        val result = StressMath.ibisSince(
            windowStart = 2_000,
            timestamps = longArrayOf(1_000, 2_000, 3_000),
            values = intArrayOf(810, 820, 830)
        )
        assertArrayEquals(intArrayOf(820, 830), result)
    }

    @Test
    fun `20th percentile interpolates between ranks`() {
        // rank = 0.2 * 4 = 0.8 -> 10 * 0.2 + 20 * 0.8 = 18.
        assertEquals(18f, StressMath.percentile(floatArrayOf(50f, 10f, 40f, 20f, 30f), 0.20), 1e-4f)
    }

    @Test
    fun `percentile of one value is that value, so the first reading is never stressed`() {
        // StressSensor computes the threshold after inserting the current RMSSD, so with a single
        // reading `rmssd < threshold` is always false (the review's P1-13 "first reading is Low").
        val only = 42f
        assertEquals(only, StressMath.percentile(floatArrayOf(only), 0.20), 0f)
    }

    @Test
    fun `percentile of nothing is NaN`() {
        assertTrue(StressMath.percentile(floatArrayOf(), 0.20).isNaN())
    }
}
