package kaist.iclab.tracker.sync.ble

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Payload encoding shared by [BLESender] and [BLEReceiver]. Large payloads are gzipped and
 * flagged via [ENCODING_KEY] in the DataMap; items without the flag are read as plain UTF-8.
 */
internal object BLEPayload {
    const val ENCODING_KEY = "encoding"
    const val ENCODING_GZIP = "gzip"
    const val COMPRESSION_THRESHOLD_BYTES = 512

    fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(bytes.size / 3 + 64)
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    fun gunzip(bytes: ByteArray): ByteArray =
        GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
}
