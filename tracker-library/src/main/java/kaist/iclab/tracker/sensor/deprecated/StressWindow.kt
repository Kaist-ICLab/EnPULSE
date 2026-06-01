// Deprecated: ring buffer + linear resample/z-score helpers for the old
// TFLite-based StressSensor. Kept here, commented out, for reference only.
// Original content below.
//
//package kaist.iclab.tracker.sensor.sigrep
//
//import kotlin.math.sqrt
//
///**
// * Ring buffer of timestamped multi-channel samples.
// *
// * Samples arrive in patches from underlying sensors, so `append` is called once per
// * [DataPoint], not once per patch. When full, the oldest entry is evicted.
// */
//internal class TimestampedRingBuffer(
//    val channels: Int,
//    val capacity: Int,
//) {
//    init {
//        require(channels >= 1) { "channels must be >= 1" }
//        require(capacity >= 2) { "capacity must be >= 2 (for linear interpolation)" }
//    }
//
//    private val timestamps = LongArray(capacity)
//    private val values = FloatArray(capacity * channels)
//    private var head = 0
//    private var count = 0
//
//    fun append(timestampMs: Long, channelValues: FloatArray) {
//        require(channelValues.size == channels) {
//            "channelValues.size=${channelValues.size} != channels=$channels"
//        }
//        timestamps[head] = timestampMs
//        System.arraycopy(channelValues, 0, values, head * channels, channels)
//        head = (head + 1) % capacity
//        if (count < capacity) count++
//    }
//
//    fun clear() {
//        head = 0
//        count = 0
//    }
//
//    fun oldestTimestampMsOrNull(): Long? {
//        if (count == 0) return null
//        val tail = (head - count + capacity) % capacity
//        return timestamps[tail]
//    }
//
//    fun newestTimestampMsOrNull(): Long? {
//        if (count == 0) return null
//        val last = (head - 1 + capacity) % capacity
//        return timestamps[last]
//    }
//
//    /** Snapshot in chronological order: returns (timestamps[n], values[n*channels]). */
//    fun snapshot(): Pair<LongArray, FloatArray>? {
//        if (count == 0) return null
//        val ts = LongArray(count)
//        val vs = FloatArray(count * channels)
//        val start = (head - count + capacity) % capacity
//        if (start + count <= capacity) {
//            System.arraycopy(timestamps, start, ts, 0, count)
//            System.arraycopy(values, start * channels, vs, 0, count * channels)
//        } else {
//            val first = capacity - start
//            System.arraycopy(timestamps, start, ts, 0, first)
//            System.arraycopy(timestamps, 0, ts, first, count - first)
//            System.arraycopy(values, start * channels, vs, 0, first * channels)
//            System.arraycopy(values, 0, vs, first * channels, (count - first) * channels)
//        }
//        return ts to vs
//    }
//}
//
///**
// * Linearly resamples [windowStartMs, windowStartMs + windowDurationMs) onto a uniform
// * grid of `targetRateHz` samples, then applies per-channel z-score normalization.
// *
// * This replicates the Python pipeline's `resample_window_linear_arrays` + per-channel
// * `zscore` exactly as applied in `cache.py` when building training windows. The merged
// * tflite model expects its raw-input tensors to be in this distribution.
// *
// * Returns row-major flattened tensor of shape `(numTargetSamples, channels)`, or null
// * if the buffer does not fully cover the window.
// */
//internal fun resampleAndZscore(
//    srcTimestampsMs: LongArray,
//    srcValues: FloatArray,
//    channels: Int,
//    windowStartMs: Long,
//    windowDurationMs: Long,
//    targetRateHz: Int,
//): FloatArray? {
//    val n = srcTimestampsMs.size
//    if (n < 2) return null
//    if (srcValues.size != n * channels) return null
//
//    val windowEndMs = windowStartMs + windowDurationMs
//    if (srcTimestampsMs[0] > windowStartMs) return null
//    if (srcTimestampsMs[n - 1] < windowEndMs) return null
//
//    val numSamples = (windowDurationMs * targetRateHz / 1000L).toInt()
//    if (numSamples <= 0) return null
//
//    val out = FloatArray(numSamples * channels)
//    val stepMs = windowDurationMs.toDouble() / numSamples
//    var j = 0
//    for (i in 0 until numSamples) {
//        val t = windowStartMs + i * stepMs
//        while (j + 1 < n && srcTimestampsMs[j + 1] <= t) j++
//        val j1 = (j + 1).coerceAtMost(n - 1)
//        val t0 = srcTimestampsMs[j].toDouble()
//        val t1 = srcTimestampsMs[j1].toDouble()
//        val ratio = if (t1 > t0) (t - t0) / (t1 - t0) else 0.0
//        for (c in 0 until channels) {
//            val v0 = srcValues[j * channels + c]
//            val v1 = srcValues[j1 * channels + c]
//            out[i * channels + c] = (v0 + (v1 - v0) * ratio).toFloat()
//        }
//    }
//
//    for (c in 0 until channels) {
//        var mean = 0.0
//        for (i in 0 until numSamples) mean += out[i * channels + c]
//        mean /= numSamples
//        var variance = 0.0
//        for (i in 0 until numSamples) {
//            val d = out[i * channels + c] - mean
//            variance += d * d
//        }
//        variance /= numSamples
//        val std = sqrt(variance) + 1e-8
//        for (i in 0 until numSamples) {
//            out[i * channels + c] = ((out[i * channels + c] - mean) / std).toFloat()
//        }
//    }
//    return out
//}
//
//internal fun magnitude(x: Float, y: Float, z: Float): Float =
//    sqrt((x * x + y * y + z * z).toDouble()).toFloat()
