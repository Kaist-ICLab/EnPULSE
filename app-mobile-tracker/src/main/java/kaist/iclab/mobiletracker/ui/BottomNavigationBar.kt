package kaist.iclab.mobiletracker.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Bottom navigation bar with three tabs: Home, Data, and Settings
 * Following Android official documentation: https://developer.android.com/develop/ui/compose/components/navigation-bar
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Define destinations with localized strings
    val destinations = listOf(
        Destination(Screen.Data.route, context.getString(R.string.nav_data), Icons.Outlined.Info, Icons.Filled.Info),
        Destination(Screen.Home.route, context.getString(R.string.nav_home), Icons.Outlined.Home, Icons.Filled.Home),
        Destination(Screen.Setting.route, context.getString(R.string.nav_settings), Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    
    // Observe current route changes using currentBackStackEntryAsState
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Map sub-routes to their parent tabs
    val subRouteToTabMap = mapOf(
        Screen.Account.route to Screen.Setting.route,
        Screen.Language.route to Screen.Setting.route,
        Screen.Permission.route to Screen.Setting.route,
        Screen.PhoneSensor.route to Screen.Setting.route,
        Screen.ServerSync.route to Screen.Setting.route,
        Screen.About.route to Screen.Setting.route
    )
    
    // Determine which tab should be highlighted
    // First check if current route is a root tab, otherwise check if it's a sub-route
    val activeTabRoute = when {
        currentRoute in destinations.map { it.route } -> currentRoute
        currentRoute in subRouteToTabMap -> subRouteToTabMap[currentRoute]
        else -> null
    }
    
    // Find current destination index
    val currentIndex = activeTabRoute?.let { route ->
        destinations.indexOfFirst { it.route == route }
    } ?: -1
    
    // Manage selected destination state using rememberSaveable
    // Initialize with current route index if found, otherwise default to 0
    var selectedDestination by rememberSaveable {
        mutableIntStateOf(if (currentIndex >= 0) currentIndex else 0)
    }
    
    // Sync selectedDestination with current route when route changes
    LaunchedEffect(currentRoute) {
        if (currentIndex >= 0) {
            selectedDestination = currentIndex
        }
    }

    NavigationBar(
        modifier = modifier.height(Styles.NAVIGATION_BAR_HEIGHT),
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = AppColors.White
    ) {
        destinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedDestination == index,
                onClick = {
                    selectedDestination = index
                    navController.navigate(route = destination.route) {
                        // Pop up to the start destination to avoid building up a back stack
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selectedDestination == index) {
                            destination.filledIcon
                        } else {
                            destination.outlinedIcon
                        },
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.PrimaryColor,
                    selectedTextColor = AppColors.PrimaryColor,
                    indicatorColor = AppColors.Transparent,
                    unselectedIconColor = AppColors.NavigationBarUnselected,
                    unselectedTextColor = AppColors.NavigationBarUnselected
                )
            )
        }
    }
}

private data class Destination(
    val route: String,
    val label: String,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Bottom navigation bar style constants
 */
private object Styles {
    val NAVIGATION_BAR_HEIGHT = 110.dp
}
