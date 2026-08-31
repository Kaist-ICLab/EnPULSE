package kaist.iclab.benchmark

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class WatchDataReceiverService : WearableListenerService() {

    companion object {
        private const val TAG = "WatchDataReceiver"
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)

        Log.i(TAG, "Channel opened: ${channel.path}")
        if (channel.path.startsWith("/benchmark_data/")) {
            val fileName = channel.path.substringAfterLast("/")
            val zipFile = File(cacheDir, fileName)
            val channelClient = Wearable.getChannelClient(this)

            channelClient.receiveFile(channel, android.net.Uri.fromFile(zipFile), false)
                .addOnSuccessListener {
                    Log.i(TAG, "File receiver registered successfully for: ${channel.path}")
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to register file receiver for: ${channel.path}", it)
                }
        }
    }

    override fun onInputClosed(
        channel: ChannelClient.Channel,
        closeReason: Int,
        appSpecificErrorCode: Int
    ) {
        super.onInputClosed(channel, closeReason, appSpecificErrorCode)
        Log.i(TAG, "onInputClosed: path=${channel.path}, reason=$closeReason")

        if (channel.path.startsWith("/benchmark_data/")) {
            val fileName = channel.path.substringAfterLast("/")
            val zipFile = File(cacheDir, fileName)

            if (closeReason == ChannelClient.ChannelCallback.CLOSE_REASON_NORMAL) {
                // File is now fully written! Unzip it
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ZipUtil.unzipToMediaStore(
                            this@WatchDataReceiverService,
                            zipFile.absolutePath
                        )
                        Log.i(TAG, "Successfully extracted via MediaStore")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to unzip file", e)
                    } finally {
                        zipFile.delete()
                    }
                }
            } else {
                Log.e(TAG, "File transfer failed or aborted. closeReason=$closeReason")
                zipFile.delete()
            }
        }
    }
}
