package kaist.iclab.field_tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.ui.theme.MainTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(title: String, canNavigateBack: Boolean, navigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.shadow(elevation = 2.dp)
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HeaderPreview() {
    MainTheme {
        Header("Settings", false, {})
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HeaderPreview2() {
    MainTheme {
        Header("Settings", true, {})
    }
}