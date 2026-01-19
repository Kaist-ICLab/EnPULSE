package com.example.listener_test_app.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.listener_test_app.ui.theme.AndroidtrackerTheme
import com.example.listener_test_app.viewmodel.NotificationViewModel

@Composable
fun NotificationTest(
    viewModel: NotificationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NotificationTestScreen(
        addCallback = {
            viewModel.addCallback()
            Toast.makeText(context, "Callback added!", Toast.LENGTH_SHORT).show()
        },
        removeCallback = {
            viewModel.removeCallback()
            Toast.makeText(context, "Callback removed!", Toast.LENGTH_SHORT).show()
        },
        postNotification = {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "test_channel"

            val channel = NotificationChannel(
                channelId,
                "Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            notificationManager.notify(1, notification)
        },
        checkNotificationListeningPermission = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
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
fun NotificationTestScreen(
    modifier: Modifier = Modifier,
    addCallback: () -> Unit = {},
    removeCallback: () -> Unit = {},
    postNotification: () -> Unit = {},
    checkNotificationListeningPermission: () -> Unit = {},
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

        Button(
            onClick = postNotification,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Post Notification")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Permissions",
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(
            onClick = checkNotificationListeningPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Notification listening")
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
fun NotificationTestScreenPreview() {
    AndroidtrackerTheme {
        NotificationTestScreen()
    }
}