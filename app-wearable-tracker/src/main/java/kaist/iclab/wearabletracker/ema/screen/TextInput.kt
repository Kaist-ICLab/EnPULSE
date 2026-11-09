package kaist.iclab.wearabletracker.ema.screen

import android.app.Activity
import android.app.RemoteInput
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper

/**
 * Button that launches system remote input (keyboard/voice/handwriting).
 */
@Composable
internal fun TextInput(
    currentText: String,
    onResult: (String) -> Unit,
    onInputOpen: () -> Unit,
    onInputClosed: () -> Unit
) {
    val context = LocalContext.current

    // Voice input needs a speech recognizer activity, which may be missing (or unusable, e.g.
    // Google's in mainland China). Without one the mic button would crash the survey.
    val canRecognizeSpeech = remember {
        context.packageManager.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }

    // Pauses expiry while the system input screen is in front, and never crashes if it's missing.
    fun launchInput(launcher: ActivityResultLauncher<Intent>, intent: Intent) {
        onInputOpen()
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("MicroEmaScreen", "No activity for ${intent.action}", e)
            onInputClosed()
            Toast.makeText(context, "Input not available on this watch", Toast.LENGTH_SHORT).show()
        }
    }

    // 1. Launcher for Unified Input (Keyboard/Handwriting/Voice)
    val textLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onInputClosed()
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val bundle = RemoteInput.getResultsFromIntent(data)
            val text = bundle?.getCharSequence("ema_text_input")?.toString()
            if (!text.isNullOrBlank()) {
                onResult(text)
            }
        }
    }

    // 2. Launcher for Direct Voice-only Input (STT)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onInputClosed()
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val firstResult = results?.firstOrNull()
            if (!firstResult.isNullOrBlank()) {
                onResult(firstResult)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Manual Text Button (Keyboard) ---
        Button(
            onClick = {
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs = listOf(
                    RemoteInput.Builder("ema_text_input")
                        .setLabel("Enter your answer")
                        .build()
                )
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                launchInput(textLauncher, intent)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(backgroundColor = DarkSurface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Type",
                    modifier = Modifier.size(14.dp),
                    tint = MutedGrey
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = currentText.ifBlank { "Tap to type..." },
                    fontSize = 11.sp,
                    color = if (currentText.isBlank()) MutedGrey else Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- Direct Mic Button ---
        if (canRecognizeSpeech) Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                }
                launchInput(voiceLauncher, intent)
            },
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Speak",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
        }
    }
}
