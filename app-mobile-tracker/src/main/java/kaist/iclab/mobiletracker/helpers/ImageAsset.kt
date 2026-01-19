package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/**
 * Helper function to load an image from assets directory
 *
 * Usage:
 * ```kotlin
 * ImageAsset(
 *     assetPath = "your_image.png",
 *     contentDescription = "Description",
 *     modifier = Modifier.size(200.dp)
 * )
 * ```
 */
@Composable
fun ImageAsset(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        loadBitmapFromAssets(context, assetPath)
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

/**
 * Loads a bitmap from the assets directory
 */
private fun loadBitmapFromAssets(context: Context, assetPath: String): android.graphics.Bitmap? {
    return try {
        val inputStream = context.assets.open(assetPath)
        BitmapFactory.decodeStream(inputStream).also {
            inputStream.close()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

