package kaist.iclab.benchmark

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

object ZipUtil {
    private const val TAG = "ZipUtil"

    fun unzipToMediaStore(context: Context, zipFilePath: String) {
        val resolver = context.contentResolver
        val zipFile = File(zipFilePath)
        Log.i(
            TAG,
            "unzipToMediaStore: zipFilePath=$zipFilePath, exists=${zipFile.exists()}, size=${zipFile.length()} bytes"
        )

        val defaultFolderName = zipFile.nameWithoutExtension
        ZipInputStream(FileInputStream(zipFilePath)).use { zis ->
            var zipEntry = zis.nextEntry
            if (zipEntry == null) {
                Log.w(TAG, "unzipToMediaStore: No entries found in zip file!")
            }
            while (zipEntry != null) {
                Log.i(
                    TAG,
                    "unzipToMediaStore: Entry found: ${zipEntry.name}, size=${zipEntry.size}, isDir=${zipEntry.isDirectory}"
                )
                if (!zipEntry.isDirectory) {
                    val entryName = zipEntry.name
                    val parts = entryName.split("/")
                    val fileName = parts.last()
                    val relativeDir = if (parts.size > 1) {
                        val folderName = parts.dropLast(1).joinToString("/")
                        "${Environment.DIRECTORY_DOWNLOADS}/EnPULSE/$folderName/"
                    } else {
                        if (defaultFolderName != "Benchmarks_All") {
                            "${Environment.DIRECTORY_DOWNLOADS}/EnPULSE/$defaultFolderName/"
                        } else {
                            "${Environment.DIRECTORY_DOWNLOADS}/EnPULSE/"
                        }
                    }
                    Log.i(
                        TAG,
                        "unzipToMediaStore: Extracting $fileName to relativeDir=$relativeDir"
                    )

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        val mimeType = when {
                            fileName.endsWith(".csv") -> "text/csv"
                            fileName.endsWith(".txt") -> "text/plain"
                            fileName.endsWith(".zip") -> "application/zip"
                            else -> "application/octet-stream"
                        }
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, relativeDir)
                    }

                    try {
                        val uri = resolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        if (uri != null) {
                            resolver.openOutputStream(uri, "w")?.use { fos ->
                                zis.copyTo(fos)
                            }
                            Log.i(TAG, "Extracted $fileName to $relativeDir successfully")
                        } else {
                            Log.e(TAG, "Failed to insert into MediaStore: $fileName")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing to MediaStore: $fileName", e)
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }
    }
}
