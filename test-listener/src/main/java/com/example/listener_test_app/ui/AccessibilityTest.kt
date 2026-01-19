package com.example.listener_test_app.ui

import android.annotation.SuppressLint
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.listener_test_app.ui.theme.AndroidtrackerTheme
import com.example.listener_test_app.viewmodel.AccessibilityViewModel

@Composable
fun AccessibilityTest(
    viewModel: AccessibilityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AccessibilityTestScreen(
        addCallback = {
            viewModel.addCallback()
            Toast.makeText(context, "Callback added!", Toast.LENGTH_SHORT).show()
        },
        removeCallback = {
            viewModel.removeCallback()
            Toast.makeText(context, "Callback removed!", Toast.LENGTH_SHORT).show()
        },
        checkAccessibilityListeningPermission = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        },
        checkBackgroundProcessPermission = {
            val packageName: String = context.packageName
            val intent = Intent()

            val pm = context.getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
                intent.setData("package:$packageName".toUri())
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Already enabled!", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier
    )
}

@SuppressLint("BatteryLife")
@Composable
fun AccessibilityTestScreen(
    modifier: Modifier = Modifier,
    addCallback: () -> Unit = {},
    removeCallback: () -> Unit = {},
    checkAccessibilityListeningPermission: () -> Unit = {},
    checkBackgroundProcessPermission: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        Text(
            "Actions",
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(
            onClick = addCallback,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Add Listener")
        }
        Button(
            onClick = removeCallback,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Remove Listener")
        }

//        Button(
//            onClick = postNotification,
//            modifier = Modifier
//                .fillMaxWidth()
//        ) {
//            Text("Post Notification")
//        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Permissions",
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(
            onClick = checkAccessibilityListeningPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Accessibility event listening")
        }

        Button(
            onClick = checkBackgroundProcessPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Background process")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccessibilityTestScreenPreview() {
    AndroidtrackerTheme {
        AccessibilityTestScreen()
    }
}