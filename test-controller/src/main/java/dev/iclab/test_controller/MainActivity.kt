package dev.iclab.test_controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.iclab.test_controller.ui.theme.AndroidtrackerTheme
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

class MainActivity : ComponentActivity() {
    private val couchbaseDB by lazy {
        CouchbaseDB(this.applicationContext)
    }
    private val stateStorage by lazy {
        CouchbaseStateStorage<ControllerState>(
            couchbaseDB,
            ControllerState(ControllerState.FLAG.READY, ""),
            ControllerState::class.java,
            "controller_state"
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = BackgroundController(
            context = this.applicationContext,
            controllerStateStorage = stateStorage,
            sensors = emptyList(),
            serviceNotification = BackgroundController.ServiceNotification(
                channelId = "test",
                channelName = "Test",
                notificationId = 1,
                title = "Test",
                description = "Test",
                icon = R.drawable.ic_launcher_foreground
            )
        )

        setContent {
            AndroidtrackerTheme {
                val controllerState = controller.controllerStateFlow.collectAsState()
                MyApp(
                    controllerState = controllerState.value,
                    onStart = {
                        controller.start()
                    },
                    onStop = {
                        controller.stop()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if ((!BackgroundController.ControllerService.isServiceRunning)
            && stateStorage.get().flag == ControllerState.FLAG.RUNNING
        ) {
            stateStorage.set(ControllerState(ControllerState.FLAG.READY, "service was termintated"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(
    controllerState: ControllerState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Controller Test App") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Controller State: ${controllerState.flag}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStart) {
                    Text("Start Controller")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onStop) {
                    Text("Stop Controller")
                }
            }
        }
    )
}