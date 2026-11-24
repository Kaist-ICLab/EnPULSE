package kaist.iclab.benchmark.wearable

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DataTransferManager {
    private const val TAG = "DataTransferManager"

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun sendFolderToPhone(context: Context, folderPath: String): Boolean =
        withContext(Dispatchers.IO) {
            val folderFile = File(folderPath)
            if (!folderFile.exists() || !folderFile.isDirectory) {
                showToast(context, "Folder does not exist")
                return@withContext false
            }

            val zipFile = File("${folderFile.absolutePath}.zip")
            ZipUtil.zipFolder(
                sourceFolderPath = folderPath,
                zipFilePath = zipFile.absolutePath,
                includeSelf = true
            )
            sendZipFileToPhone(context, zipFile, "Data sent successfully", "Failed to send data")
        }

    suspend fun sendAllDataToPhone(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            val baseDir = context.getExternalFilesDir("Benchmarks")
            if (baseDir == null || !baseDir.exists() || !baseDir.isDirectory) {
                showToast(context, "No data found")
                return@withContext false
            }

            val zipFile = File("${baseDir.absolutePath}_All.zip")
            ZipUtil.zipFolder(baseDir.absolutePath, zipFile.absolutePath)
            sendZipFileToPhone(
                context,
                zipFile,
                "All data sent successfully",
                "Failed to send all data"
            )
        }

    private fun sendZipFileToPhone(
        context: Context,
        zipFile: File,
        successMessage: String,
        failureMessage: String
    ): Boolean {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val phoneNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()

            if (phoneNode == null) {
                showToast(context, "No phone connected")
                return false
            }

            val channelClient = Wearable.getChannelClient(context)
            val channelPath = "/benchmark_data/${zipFile.name}"
            val channel = Tasks.await(channelClient.openChannel(phoneNode.id, channelPath))

            Tasks.await(channelClient.sendFile(channel, Uri.fromFile(zipFile)))

            Log.i(TAG, "Successfully sent: $channelPath to node ${phoneNode.id}")
            showToast(context, successMessage)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file to phone", e)
            showToast(context, failureMessage)
            false
        } finally {
            try {
                if (zipFile.exists()) {
                    zipFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete temporary zip: ${zipFile.path}", e)
            }
        }
    }
}
