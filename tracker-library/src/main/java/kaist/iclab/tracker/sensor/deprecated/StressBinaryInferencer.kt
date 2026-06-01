// Deprecated: backed the old TFLite-based StressSensor. Kept here, commented out,
// for reference only. Original content below.
//
//package kaist.iclab.tracker.sensor.sigrep
//
//import android.content.Context
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.Interpreter
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import kotlin.math.exp
//
//internal class StressBinaryInferencer(
//    context: Context,
//    numThreads: Int,
//) {
//    companion object {
//        const val MODEL_PATH = "sigrep/stress_binary_merged.tflite"
//        const val DECISION_THRESHOLD = 0.5f
//        private const val BYTES_PER_FLOAT = 4
//        private val SUPPORTED_MODALITIES = setOf("acc", "ppg", "hr")
//    }
//
//    data class Prediction(
//        val logit: Float,
//        val probability: Float,
//        val isHighStress: Boolean,
//    )
//
//    private val interpreter = Interpreter(
//        loadSigRepModelFile(context, MODEL_PATH),
//        Interpreter.Options().apply {
//            setNumThreads(numThreads)
//            setUseXNNPACK(true)
//        },
//    )
//
//    private data class InputSlot(
//        val interpreterIndex: Int,
//        val shape: IntArray,
//        val elements: Int,
//        val buffer: ByteBuffer,
//    )
//
//    private val slots: Map<String, InputSlot> = resolveInputSlots()
//
//    private val outputBuffer: ByteBuffer = run {
//        val t = interpreter.getOutputTensor(0)
//        check(t.dataType() == DataType.FLOAT32) {
//            "Unexpected output dtype: ${t.dataType()} (expected FLOAT32)"
//        }
//        val elements = t.shape().fold(1) { a, v -> a * v }
//        ByteBuffer.allocateDirect(elements * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
//    }
//
//    private fun resolveInputSlots(): Map<String, InputSlot> {
//        val count = interpreter.inputTensorCount
//        check(count == SUPPORTED_MODALITIES.size) {
//            "Expected ${SUPPORTED_MODALITIES.size} inputs (acc/ppg/hr), got $count"
//        }
//        val resolved = HashMap<String, InputSlot>(count)
//        for (i in 0 until count) {
//            val tensor = interpreter.getInputTensor(i)
//            val rawName = tensor.name()
//            val lower = rawName.lowercase()
//            val modality = when {
//                "acc" in lower -> "acc"
//                "ppg" in lower -> "ppg"
//                "hr" in lower -> "hr"
//                else -> error("Unrecognized input tensor name: '$rawName'")
//            }
//            require(modality !in resolved) {
//                "Duplicate mapping for modality '$modality' (tensor '$rawName')"
//            }
//            check(tensor.dataType() == DataType.FLOAT32) {
//                "Unexpected dtype for $modality: ${tensor.dataType()} (expected FLOAT32)"
//            }
//            val shape = tensor.shape()
//            val elements = shape.fold(1) { acc, v -> acc * v }
//            val buffer = ByteBuffer
//                .allocateDirect(elements * BYTES_PER_FLOAT)
//                .order(ByteOrder.nativeOrder())
//            resolved[modality] = InputSlot(i, shape, elements, buffer)
//        }
//        check(resolved.keys == SUPPORTED_MODALITIES) {
//            "Unexpected input tensor set: ${resolved.keys}"
//        }
//        return resolved
//    }
//
//    fun inputShape(modality: String): IntArray =
//        slots.getValue(modality).shape.copyOf()
//
//    @Synchronized
//    fun predict(acc: FloatArray, ppg: FloatArray, hr: FloatArray): Prediction {
//        feed("acc", acc)
//        feed("ppg", ppg)
//        feed("hr", hr)
//
//        val ordered = Array<Any>(slots.size) { idx ->
//            val slot = slots.values.first { it.interpreterIndex == idx }
//            slot.buffer.rewind()
//            slot.buffer
//        }
//        outputBuffer.rewind()
//        val outputs = HashMap<Int, Any>(1).apply { put(0, outputBuffer) }
//
//        interpreter.runForMultipleInputsOutputs(ordered, outputs)
//
//        outputBuffer.rewind()
//        val logit = outputBuffer.getFloat(0)
//        check(logit.isFinite()) { "Model returned non-finite logit: $logit" }
//        val prob = 1f / (1f + exp(-logit))
//        return Prediction(
//            logit = logit,
//            probability = prob,
//            isHighStress = prob >= DECISION_THRESHOLD,
//        )
//    }
//
//    private fun feed(modality: String, values: FloatArray) {
//        val slot = slots.getValue(modality)
//        require(values.size == slot.elements) {
//            "Shape mismatch for '$modality': got=${values.size}, " +
//                    "expected=${slot.elements} (shape=${slot.shape.toList()})"
//        }
//        for (i in values.indices) {
//            val v = values[i]
//            require(v.isFinite()) { "Non-finite sample in '$modality' at index $i: $v" }
//        }
//        slot.buffer.rewind()
//        val fb = slot.buffer.asFloatBuffer()
//        fb.rewind()
//        fb.put(values)
//    }
//
//    fun close() {
//        interpreter.close()
//    }
//}
