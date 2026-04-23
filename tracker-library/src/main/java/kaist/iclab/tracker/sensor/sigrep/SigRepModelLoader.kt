package kaist.iclab.tracker.sensor.sigrep

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

internal fun loadSigRepModelFile(context: Context, modelName: String): MappedByteBuffer {
    context.assets.openFd(modelName).use { fileDescriptor ->
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
}
