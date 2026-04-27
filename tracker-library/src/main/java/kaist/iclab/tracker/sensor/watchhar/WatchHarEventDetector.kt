package kaist.iclab.tracker.sensor.watchhar

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class WatchHarEventDetector(
    context: Context,
    numThreads: Int,
) {
    companion object {
        private const val MODEL_PATH = "imu_event_detector.tflite"
        private const val INPUT_BYTES = 1 * 150 * 6 * 4
        private const val OUTPUT_BYTES = 1 * 4
    }

    private val interpreterOptions = Interpreter.Options().apply {
        setNumThreads(numThreads)
        setUseXNNPACK(true)
    }

    private val interpreter = Interpreter(loadModelFile(context, MODEL_PATH), interpreterOptions)
    private val inputBuffer = ByteBuffer.allocateDirect(INPUT_BYTES).order(ByteOrder.nativeOrder())
    private val inputFloats = inputBuffer.asFloatBuffer()
    private val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_BYTES).order(ByteOrder.nativeOrder())

    @Synchronized
    fun run(imuData: FloatArray): Float {
        inputBuffer.rewind()
        inputFloats.rewind()
        outputBuffer.rewind()
        inputFloats.put(imuData)
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        return outputBuffer.getFloat(0)
    }

    fun close() {
        interpreter.close()
    }
}
