package pl.jakubweg.jetrun.ui.screen

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import pl.jakubweg.jetrun.R
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

class MapComposableViewModel : ViewModel() {
    private var mapReference = AtomicReference(WeakReference<GoogleMap>(null))

    fun setMap(mapView: GoogleMap) {
        mapReference.set(WeakReference(mapView))
    }
}

@Composable
fun ComposableMapView(modifier: Modifier) {
    val vm: MapComposableViewModel = viewModel()
    val mapOptions = rememberMapOptions()

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
    AndroidView(
        modifier = modifier,
        factory = { context -> MapView(context, mapOptions) },
        update = { view ->
            view.onCreate(null)
            view.onStart()
            view.onResume()
            view.getMapAsync { map ->
                vm.setMap(map)
            }
        }
    )
}

@Composable
private fun rememberMapOptions() = remember {
    GoogleMapOptions()
        .compassEnabled(false)
        .mapType(GoogleMap.MAP_TYPE_NORMAL)
        .mapToolbarEnabled(false)
        .rotateGesturesEnabled(false)
        .tiltGesturesEnabled(false)
//            .minZoomPreference(8F)
        .maxZoomPreference(17F)
}

@Composable
private fun CheckIfApiKeyIsMissing() {
    val context = LocalContext.current
    LaunchedEffect("") {
        val key = context.getString(R.string.google_maps_key)
        if (key.contains("YOUR_KEY_HERE"))
            Toast.makeText(
                context,
                "This app build is missing Google Maps API key!\nMap won't load!",
                Toast.LENGTH_LONG
            ).show()
    }
}