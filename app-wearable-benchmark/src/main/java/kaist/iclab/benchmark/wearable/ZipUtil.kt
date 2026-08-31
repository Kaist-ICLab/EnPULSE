package kaist.iclab.benchmark.wearable

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtil {
    fun zipFolder(sourceFolderPath: String, zipFilePath: String, includeSelf: Boolean = false) {
        val sourceFolder = File(sourceFolderPath)
        val zipFile = File(zipFilePath)
        val baseDir = if (includeSelf) (sourceFolder.parentFile ?: sourceFolder) else sourceFolder
        Log.d("ZipUtil", "Zipping folder: $sourceFolderPath to $zipFilePath")
        var fileCount = 0

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceFolder.walkTopDown().forEach { file ->
                if (!file.isDirectory) {
                    fileCount++
                    val entryName = baseDir.toPath().relativize(file.toPath()).toString()
                    Log.d("ZipUtil", "Adding entry: $entryName")
                    val zipEntry = ZipEntry(entryName)
                    zos.putNextEntry(zipEntry)

                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        Log.d(
            "ZipUtil",
            "Zipping complete. Total files: $fileCount, zip size: ${zipFile.length()} bytes"
        )
    }
}
