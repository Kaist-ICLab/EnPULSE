package kaist.iclab.tracker.sensor.watchhar

import java.util.Arrays

internal class FloatRingBuffer(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var head = 0
    private var size = 0
    private var sum = 0f

    fun write(value: Float) {
        if (size == capacity) {
            sum -= buffer[head]
        }
        sum += value
        buffer[head] = value
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    fun write(values: FloatArray) {
        for (value in values) {
            write(value)
        }
    }

    fun getLastData(count: Int): FloatArray {
        require(count <= capacity) { "count is larger than buffer size" }
        val result = FloatArray(count)
        copyLastDataTo(result)
        return result
    }

    fun copyLastDataTo(destination: FloatArray) {
        val count = destination.size
        require(count <= capacity) { "Buffer size too small" }

        val start = (head - count + capacity) % capacity
        if (start + count <= capacity) {
            System.arraycopy(buffer, start, destination, 0, count)
        } else {
            val endChunkSize = capacity - start
            System.arraycopy(buffer, start, destination, 0, endChunkSize)
            System.arraycopy(buffer, 0, destination, endChunkSize, count - endChunkSize)
        }
    }

    fun clear() {
        head = 0
        size = 0
        sum = 0f
        Arrays.fill(buffer, 0f)
    }

    fun getAvg(): Float = if (size == 0) 0f else sum / size
}

internal class ShortRingBuffer(private val capacity: Int) {
    private val buffer = ShortArray(capacity)
    private var head = 0
    private var size = 0

    fun write(values: ShortArray) {
        for (value in values) {
            buffer[head] = value
            head = (head + 1) % capacity
            if (size < capacity) size++
        }
    }

    fun getLastData(count: Int): ShortArray {
        require(count <= size) { "count is larger than buffer size" }
        val result = ShortArray(count)
        val start = (head - count + capacity) % capacity
        if (start + count <= capacity) {
            System.arraycopy(buffer, start, result, 0, count)
        } else {
            val endChunkSize = capacity - start
            System.arraycopy(buffer, start, result, 0, endChunkSize)
            System.arraycopy(buffer, 0, result, endChunkSize, count - endChunkSize)
        }
        return result
    }

    fun clear() {
        head = 0
        size = 0
    }

    fun isFull(): Boolean = size == capacity
}
