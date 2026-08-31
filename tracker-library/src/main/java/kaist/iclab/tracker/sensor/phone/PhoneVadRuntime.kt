package kaist.iclab.tracker.sensor.phone

import android.content.Context
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class PhoneVadPrediction(
    val isSpeech: Boolean,
    val speechProbability: Float,
    val inferenceTimeMs: Float
)

class PhoneVadRuntime(context: Context) : AutoCloseable {
    companion object {
        private const val MODEL_FILE = "audio/yamnet_classification.tflite"
        const val INPUT_SAMPLES = 15_600
        const val OUTPUT_CLASSES = 521
        private const val PCM_NORMALIZER = 32_768f
        private const val SPEECH_CLASS_INDEX = 0 // "Speech" is typically index 0 in YAMNet
        private const val SPEECH_THRESHOLD = 0.5f
    }

    private val modelBuffer = loadModelFile(context, MODEL_FILE)
    private val interpreter = Interpreter(
        modelBuffer,
        Interpreter.Options().apply {
            setNumThreads(2)
            setUseXNNPACK(true)
        }
    )
    
    private val input = ByteBuffer.allocateDirect(INPUT_SAMPLES * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(OUTPUT_CLASSES * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val inputFloats = input.asFloatBuffer()
    private val outputFloats = output.asFloatBuffer()
    private val scores = FloatArray(OUTPUT_CLASSES)

    init {
        validateContract()
    }

    @Synchronized
    fun run(pcm16: ShortArray): PhoneVadPrediction {
        val startedNs = SystemClock.elapsedRealtimeNanos()
        runModel(pcm16)
        val elapsedNs = SystemClock.elapsedRealtimeNanos() - startedNs
        
        val speechProb = scores[SPEECH_CLASS_INDEX]
        val isSpeech = speechProb > SPEECH_THRESHOLD
        
        return PhoneVadPrediction(
            isSpeech = isSpeech,
            speechProbability = speechProb,
            inferenceTimeMs = elapsedNs / 1_000_000f
        )
    }

    override fun close() {
        interpreter.close()
    }

    private fun runModel(pcm16: ShortArray) {
        require(pcm16.size == INPUT_SAMPLES) {
            "YAMNet expects $INPUT_SAMPLES PCM samples, got ${pcm16.size}"
        }
        inputFloats.rewind()
        for (sample in pcm16) {
            inputFloats.put(sample / PCM_NORMALIZER)
        }
        input.rewind()
        output.rewind()
        interpreter.run(input, output)
        outputFloats.rewind()
        outputFloats.get(scores)
    }

    private fun validateContract() {
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        require(inputTensor.shape().contentEquals(intArrayOf(INPUT_SAMPLES))) {
            "Unexpected YAMNet input shape: ${inputTensor.shape().contentToString()}"
        }
        require(inputTensor.dataType() == DataType.FLOAT32)
        require(outputTensor.shape().contentEquals(intArrayOf(1, OUTPUT_CLASSES))) {
            "Unexpected YAMNet output shape: ${outputTensor.shape().contentToString()}"
        }
        require(outputTensor.dataType() == DataType.FLOAT32)
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).also {
            fileInputStream.close()
            assetFileDescriptor.close()
        }
    }
}
