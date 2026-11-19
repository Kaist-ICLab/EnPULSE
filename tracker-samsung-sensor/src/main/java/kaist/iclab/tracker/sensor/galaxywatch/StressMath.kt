package kaist.iclab.tracker.sensor.galaxywatch

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure RMSSD / outlier math used by [StressSensor], kept separate from the sensor so it can be
 * unit-tested on the JVM.
 */
internal object StressMath {
    // HR bounds 30–220 bpm → IBI bounds 60000/220–60000/30 ms.
    const val MIN_IBI_MS = 60 * 1000 / 220
    const val MAX_IBI_MS = 60 * 1000 / 30

    const val MAD_MULTIPLIER = 3.0

    fun ibisSince(windowStart: Long, timestamps: LongArray, values: IntArray): IntArray {
        val result = ArrayList<Int>(values.size)
        for (i in timestamps.indices) {
            if (timestamps[i] >= windowStart) result.add(values[i])
        }
        return result.toIntArray()
    }

    /**
     * Median +/- [MAD_MULTIPLIER] * MAD computed from [ibis], restricted to physiologically
     * plausible values ([MIN_IBI_MS]..[MAX_IBI_MS]). Callers apply the returned bounds via
     * [filterIbis] - typically to a different (e.g. smaller, overlapping) set of IBIs than
     * the one the bounds were derived from, so the outlier reference stays stable across
     * windows of different sizes drawn from the same underlying data.
     *
     * Returns an empty range (nothing passes) if there's no in-range data to anchor a
     * median on, and an all-inclusive range if the data has no spread (MAD == 0) to derive
     * a meaningful bound from.
     */
    fun outlierBounds(ibis: IntArray): ClosedFloatingPointRange<Double> {
        val inRange = ibis.filter { it in MIN_IBI_MS..MAX_IBI_MS }.map { it.toDouble() }
        if (inRange.isEmpty()) return Double.POSITIVE_INFINITY..Double.NEGATIVE_INFINITY
        val med = median(inRange)
        val mad = median(inRange.map { abs(it - med) })
        if (mad == 0.0) return Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY
        return (med - MAD_MULTIPLIER * mad)..(med + MAD_MULTIPLIER * mad)
    }

    fun filterIbis(ibis: IntArray, bounds: ClosedFloatingPointRange<Double>): IntArray {
        return ibis.filter { it in MIN_IBI_MS..MAX_IBI_MS && it.toDouble() in bounds }.toIntArray()
    }

    fun rmssd(ibis: IntArray): Float {
        var sumSq = 0.0
        for (i in 1 until ibis.size) {
            val d = (ibis[i] - ibis[i - 1]).toDouble()
            sumSq += d * d
        }
        return sqrt(sumSq / (ibis.size - 1)).toFloat()
    }

    fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2]
        else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    fun percentile(values: FloatArray, p: Double): Float {
        if (values.isEmpty()) return Float.NaN
        val sorted = values.copyOf().also { it.sort() }
        if (sorted.size == 1) return sorted[0]
        val rank = p * (sorted.size - 1)
        val lo = rank.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.size - 1)
        val frac = rank - lo
        return (sorted[lo] * (1 - frac) + sorted[hi] * frac).toFloat()
    }
}
