package pl.jakubweg.jetrun.ui.screen

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView
import pl.jakubweg.jetrun.R
import pl.jakubweg.jetrun.ui.model.MapComposableViewModel

@Composable
fun ComposableMapView(modifier: Modifier) {
    val vm: MapComposableViewModel = hiltViewModel()

    val mapOptions = remember { vm.createMapOptions() }

    val mapView by remember { mutableStateOf<MapView?>(null) }

    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            mapView?.apply {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> onCreate(null)
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_STOP -> onStop()
                    Lifecycle.Event.ON_DESTROY -> onDestroy()
                    else -> throw IllegalStateException()
                }
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            mapView?.apply {
                val currentState = lifecycle.currentState
                if (currentState.isAtLeast(Lifecycle.State.RESUMED))
                    onPause()
                if (currentState.isAtLeast(Lifecycle.State.STARTED))
                    onStop()
                if (currentState.isAtLeast(Lifecycle.State.CREATED))
                    onDestroy()
            }

            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    CheckIfApiKeyIsMissing()

    MapViewLocationUpdater(vm)

    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isDarkTheme) {
        vm.darkTheme = isDarkTheme
    }

    AndroidView(
        modifier = modifier,
        factory = { context -> MapView(context, mapOptions) },
        update = { view ->
            view.onCreate(null)
            view.onStart()
            view.onResume()
            view.getMapAsync { map ->
                vm.setMap(view.context, map)
            }
        }
    )
}

@Composable
private fun MapViewLocationUpdater(vm: MapComposableViewModel) {
    val location by vm.lastKnownLocation.observeAsState()
    LaunchedEffect(location) {
        vm.pingLocationSource(location)
    }

    DisposableEffect("") {
        vm.visible = true
        onDispose { vm.visible = false }
    }
}

@Composable
private fun CheckIfApiKeyIsMissing() {
    val context = LocalContext.current
    LaunchedEffect("") {
        val key = context.getString(R.string.google_maps_key)
        // do not replace this string in the following statement, change only inside strings.xml files
        if (key.contains("YOUR_KEY_HERE"))
            Toast.makeText(
                context,
                "This app build is missing Google Maps API key!\nMap won't load!",
                Toast.LENGTH_LONG
            ).show()
    }
}