package com.example.listener_test_app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.listener_test_app.ui.theme.AndroidtrackerTheme
import com.example.listener_test_app.viewmodel.MainViewModel
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.BroadcastListener

private const val ACTION_NAME_BROADCAST = "ACTION_TEST_BROADCAST"
private const val ACTION_NAME_ALARM = "ACTION_TEST_ALARM"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    ),
                    1
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            AndroidtrackerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { innerPadding ->
                        ListenerTestApp(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                )
            }
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun ListenerTestApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val broadcastListener = BroadcastListener(
        context = context,
        actionNames = arrayOf(ACTION_NAME_BROADCAST)
    )
    val alarmListener = AlarmListener(
        context = context,
        actionName = ACTION_NAME_ALARM,
        actionCode = 1001,
        actionIntervalInMilliseconds = 1000L,
    )

    val mainViewModel: MainViewModel = viewModel()

    val postNotification = {
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
    }

    val sendBroadcast = {
        val intent = Intent(ACTION_NAME_BROADCAST)
        context.sendBroadcast(intent)
    }

    val sendAlarm = {
        // Manually trigger an alarm (for testing purposes)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerTime = SystemClock.elapsedRealtime() + 500  // 0.5 seconds delay
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerTime,
            alarmListener.getPendingIntent()
        )
    }

    ListenerTestScreen(
        addCallback = {
            mainViewModel.addListener(
                alarmListener = alarmListener,
                broadcastListener = broadcastListener,
                alarmActionName = ACTION_NAME_ALARM,
                broadcastActionName = ACTION_NAME_BROADCAST
            )
        },
        removeCallback = {
            mainViewModel.removeListener(
                alarmListener = alarmListener,
                broadcastListener = broadcastListener
            )
        },
        postNotification = postNotification,
        sendBroadcast = sendBroadcast,
        sendAlarm = sendAlarm,
        count = mainViewModel.count,
        modifier = modifier.padding(15.dp)
    )
}

@Composable
fun ListenerTestScreen(
    addCallback: () -> Unit,
    removeCallback: () -> Unit,
    postNotification: () -> Unit,
    sendBroadcast: () -> Unit,
    sendAlarm: () -> Unit,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            "Callbacks: $count",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(5.dp)
        )

        Row(
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Button(
                onClick = addCallback,
                modifier = Modifier.weight(1F)
            ) {
                Text("Add Callback")
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = removeCallback,
                modifier = Modifier.weight(1F)
            ) {
                Text("Remove Callback")
            }
        }

        Text(
            "Actions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
        Text(
            "Alarms have some delay, so please be patient :)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(5.dp)
        )
        Row(
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Button(
                onClick = postNotification,
                modifier = Modifier.weight(1F)
            ) {
                Text("Notification")
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = sendBroadcast,
                modifier = Modifier.weight(1F)
            ) {
                Text("Broadcast")
            }
        }
        Row(
            modifier = Modifier.padding(bottom = 5.dp)
        ) {
            Button(
                onClick = sendAlarm,
                modifier = Modifier.weight(1F)
            ) {
                Text("Alarm")
            }

            Spacer(Modifier.width(10.dp))

            Spacer(
                modifier = Modifier.weight(1F)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ListenerTestScreenPreview() {
    MaterialTheme {
        ListenerTestScreen(
            addCallback = {},
            removeCallback = {},
            postNotification = {},
            sendBroadcast = {},
            sendAlarm = {},
            count = 5,
            modifier = Modifier.padding(15.dp)
        )
    }
}