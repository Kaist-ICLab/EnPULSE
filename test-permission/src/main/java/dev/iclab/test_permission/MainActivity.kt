package dev.iclab.test_permission

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.iclab.test_permission.ui.theme.AndroidtrackerTheme
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.Permission
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.permission.PermissionState


class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: AndroidPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = AndroidPermissionManager(this)
        permissionManager.bind(this)

        setContent {
            AndroidtrackerTheme {
                PermissionScreen(permissionManager)
            }
        }
    }
}


@Composable
fun PermissionScreen(permissionManager: PermissionManager) {
    val permissions = Permission.supportedPermissions.flatMap { it.ids.toList() }.toTypedArray()
    // Permissions are automatically registered when getPermissionFlow() is called
    val permissionStateMap = permissionManager.getPermissionFlow(permissions).collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Permissions", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(Permission.supportedPermissions.size) { idx ->
                val permission = Permission.supportedPermissions[idx]
                PermissionItem(
                    permission = permission,
                    state = permissionStateMap.value[permission.ids.first()] ?: PermissionState.NOT_REQUESTED,
                    onRequest = { permissionManager.request(permission.ids) }
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    permission: Permission,
    state: PermissionState,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(permission.name, style = MaterialTheme.typography.bodyLarge)
            Text("Status: $state", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequest,
                enabled = state != PermissionState.UNSUPPORTED && state != PermissionState.GRANTED
            ) {
                Text(
                    when (state) {
                        PermissionState.UNSUPPORTED -> "Hardware Not Available"
                        PermissionState.GRANTED -> "Already Granted"
                        else -> "Request Permission"
                    }
                )
            }
        }
    }
}