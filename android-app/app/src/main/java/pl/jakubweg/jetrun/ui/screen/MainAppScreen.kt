package pl.jakubweg.jetrun.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

@ExperimentalAnimationApi
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
            startDestination = "record"
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

@ExperimentalAnimationApi
@Composable
private fun RecordWorkoutScreen() {
//    val configuration = LocalConfiguration.current
//    Text("Hello world")
//    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
//        ComposableMapView(modifier = Modifier.fillMaxSize())
//    else

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Column {
                var mapIsVisible by remember { mutableStateOf(false) }

                rememberCoroutineScope().launch {
                    delay(500L)
                    mapIsVisible = true
                }
                AnimatedVisibility(
                    visible = mapIsVisible,
                    enter = fadeIn(0f)
                ) {
                    ComposableMapView(modifier = Modifier.fillMaxSize())
                }
            }

            val shape = RoundedCornerShape(100)
            Text(
                modifier = Modifier
                    .padding(20.dp)
                    .shadow(elevation = 8.dp, shape = shape)
                    .clip(shape)
                    .background(MaterialTheme.colors.error)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                text = "Getting GPS signal",
                color = MaterialTheme.colors.onError
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "5.7km")
            Text(text = "6:14")
            FloatingActionButton(onClick = {}) {
                Icon(imageVector = Icons.Default.BrokenImage, contentDescription = null)
            }
        }
    }
}


