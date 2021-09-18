package pl.jakubweg.jetrun.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.vm.UserViewModel

sealed class BottomNavigationLocation(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    object MyStats : BottomNavigationLocation("me", Icons.Filled.AccountCircle, "My stats")
    object WorkoutsList : BottomNavigationLocation("workouts", Icons.Filled.Timeline, "History")
    object RecordWorkout : BottomNavigationLocation("record", Icons.Filled.DirectionsRun, "Record")
}

val allBottomNavigationEntries = listOf(
    BottomNavigationLocation.MyStats,
    BottomNavigationLocation.WorkoutsList,
    BottomNavigationLocation.RecordWorkout
)

@Composable
fun MainAppScreen(
    state: AuthState.Signed,
    userViewModel: UserViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                allBottomNavigationEntries.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(text = screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (navController.currentDestination?.route == screen.route)
                                return@BottomNavigationItem
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = "me"
        ) {
            composable(BottomNavigationLocation.MyStats.route) {
                Text(text = "My workout stats here")
            }

            composable(BottomNavigationLocation.WorkoutsList.route) {
                Column {
                    Text(text = "My list is here")
                    Button(onClick = {
                        navController.navigate(BottomNavigationLocation.MyStats.route) {
                            this.launchSingleTop = true
                        }
                    }) {
                        Text("GOTO STATS")
                    }
                }
            }

            composable(BottomNavigationLocation.RecordWorkout.route) {
                RecordWorkoutScreen()
            }
        }
    }

}

@Composable
private fun RecordWorkoutScreen() {
    ComposableMapView()
}


