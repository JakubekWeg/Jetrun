package pl.jakubweg.jetrun.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@Composable
fun RecordWorkoutScreen() {
    val configuration = LocalConfiguration.current

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        LandscapeLayout()
    else
        VerticalLayout()
}

@ExperimentalAnimationApi
@Composable
fun LandscapeLayout() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End
    ) {
        InformationSection(modifier = Modifier.fillMaxHeight().weight(20f), isLandscape = true)

        MapSection(
            modifier = Modifier
                .fillMaxHeight()
                .weight(weight = 50f)
        )
    }
}


@ExperimentalAnimationApi
@Composable
private fun VerticalLayout() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        MapSection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f)
        )

        InformationSection(modifier = Modifier.fillMaxWidth(), isLandscape = false)
    }
}

@ExperimentalAnimationApi
@Composable
private fun MapSection(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            var mapIsVisible by remember { mutableStateOf(false) }

            rememberCoroutineScope().launch {
                delay(250L)
                mapIsVisible = true
            }
            AnimatedVisibility(
                visible = mapIsVisible,
                enter = fadeIn(0f)
            ) {
                ComposableMapView(modifier = Modifier.fillMaxSize())
            }
        }

        MissingGPSIndicator()

        WorkoutPausedIndicator(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun InformationSection(modifier: Modifier, isLandscape: Boolean) {
    Surface(
        elevation = 20.dp,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WorkoutTypeSelector()

            if (isLandscape) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PrimaryMeter(
                        modifier = Modifier,
                        value = "5.8km",
                        name = "Total distance"
                    )

                    FloatingActionButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                    }

                    PrimaryMeter(
                        modifier = Modifier,
                        value = "5:14",
                        name = "Total time"
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    PrimaryMeter(
                        modifier = Modifier.weight(1f),
                        value = "5.7km",
                        name = "Total distance"
                    )

                    FloatingActionButton(onClick = {}, modifier = Modifier.padding(8.dp)) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                    }

                    PrimaryMeter(
                        modifier = Modifier.weight(1f),
                        value = "6:14",
                        name = "Total time"
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryMeter(modifier: Modifier = Modifier, value: String, name: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            maxLines = 1
        )

        val color = MaterialTheme.colors.onBackground
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            color = remember(color) { color.copy(alpha = .6f) }
        )
    }
}

@Composable
private fun WorkoutTypeSelector() {
    OutlinedButton(
        onClick = { },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Icon(imageVector = Icons.Default.DirectionsBike, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            modifier = Modifier.animateContentSize(),
            text = "Riding a bike",
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
}

@Composable
private fun MissingGPSIndicator() {
    val shape = RoundedCornerShape(100)
    Row(
        modifier = Modifier
            .padding(20.dp)
            .shadow(elevation = 8.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colors.error)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = MaterialTheme.colors.onError
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Default.GpsNotFixed,
            contentDescription = null,
            tint = tint
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Getting GPS signal",
            color = tint
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun WorkoutPausedIndicator(modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect("") {
        repeat(1000) {
            delay(2500L)
            visible = !visible
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
    ) {
        val tint = MaterialTheme.colors.onPrimary
        Button(
            modifier = Modifier.padding(12.dp),
            onClick = {}) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = tint
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "FINISH WORKOUT",
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}
