package pl.jakubweg.jetrun.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import pl.jakubweg.jetrun.R

@Composable
fun ComposableMapView() {
    val mapOptions = remember {
        GoogleMapOptions()
            .compassEnabled(false)
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .mapToolbarEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)
//            .minZoomPreference(8F)
            .maxZoomPreference(17F)
    }

    val mapView = rememberMapViewWithLifecycle(mapOptions)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.onCreate(null)
            view.onStart()
            view.onResume()
            view.getMapAsync { map ->
            }
        }
    )
}

@Composable
private fun rememberMapViewWithLifecycle(mapOptions: GoogleMapOptions): MapView {
    val context = LocalContext.current

    CheckIfApiKeyIsMissing(context)

    val mapView = remember(mapOptions) {
        MapView(context, mapOptions)
    }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            val currentState = lifecycle.currentState
            if (currentState.isAtLeast(Lifecycle.State.RESUMED))
                mapView.onPause()
            if (currentState.isAtLeast(Lifecycle.State.STARTED))
                mapView.onStop()
            if (currentState.isAtLeast(Lifecycle.State.CREATED))
                mapView.onDestroy()

            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
private fun CheckIfApiKeyIsMissing(context: Context) {
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

@Composable
private fun rememberMapLifecycleObserver(mapView: MapView) =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
    }