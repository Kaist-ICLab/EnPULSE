package dev.iclab.test_notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.iclab.test_notification.ui.theme.AndroidtrackerTheme

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        val notificationHandler = NotificationHandler(this.applicationContext)
        super.onCreate(savedInstanceState)
        notificationHandler.initNotification()
        setContent {
            AndroidtrackerTheme {
                NotificationControlScreen(
                    onPost = { notificationHandler.post() },
                    onPostService = { notificationHandler.runService() },
                    onStopService = { notificationHandler.stopService() },
                    onRemove = { notificationHandler.remove() }
                )
            }
        }
    }


}

@Composable
fun NotificationControlScreen(
    onPost: () -> Unit,
    onRemove: () -> Unit,
    onPostService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Notification Controls", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPost, modifier = Modifier.fillMaxWidth()) {
            Text("Post Notification")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
            Text("Remove Notification")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onPostService, modifier = Modifier.fillMaxWidth()) {
            Text("Post Service Notification")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onStopService, modifier = Modifier.fillMaxWidth()) {
            Text("Stop Service Notification")
        }
    }
}