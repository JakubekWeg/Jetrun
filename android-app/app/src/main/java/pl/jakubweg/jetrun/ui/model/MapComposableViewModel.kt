package pl.jakubweg.jetrun.ui.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import pl.jakubweg.jetrun.R
import pl.jakubweg.jetrun.component.LocationProviderComponent
import pl.jakubweg.jetrun.component.LocationRequestId
import pl.jakubweg.jetrun.component.LocationSnapshot
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class MapComposableViewModel @Inject constructor(
    private val location: LocationProviderComponent
) : ViewModel(), LocationSource {

    init {
        println("INIT")
    }

    var mapViewReference = WeakReference<MapView>(null)
    private var hadAnyLocation = false
    private var mapReference: GoogleMap? = null
    private var locationSourceListener: LocationSource.OnLocationChangedListener? = null
    private var locationRequestId: LocationRequestId = 0
    private var _visible = false
    private var mapStyleOptions: MapStyleOptions? = null

    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 14.5F
    }

    val lastKnownLocation = location.lastKnownLocation

    var visible: Boolean
        get() = _visible
        set(value) {
            if (_visible == value) return
            _visible = value
            considerMakingLocationRequest()
        }

    var darkTheme: Boolean = false

    private fun setMapPositionToBeLatestLocation(instant: Boolean): Boolean {
        val snapshot = location.lastKnownLocation.value ?: return false
        val map = mapReference ?: return false
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(snapshot.latitude, snapshot.longitude), DEFAULT_ZOOM_LEVEL
            ), if (instant) 1 else 300, null
        )
        return true
    }

    private fun considerMakingLocationRequest() {
        val shouldActivate = _visible && mapReference != null
        val isActive = locationRequestId != 0
        if (shouldActivate != isActive) {
            location.stop(locationRequestId)
            locationRequestId = if (shouldActivate) {
                setMapPositionToBeLatestLocation(instant = true)
                location.start(requireStart = false)
            } else 0
        }
    }

    @SuppressLint("MissingPermission")
    fun setMap(context: Context?, map: GoogleMap?) {
        mapReference = map
        considerMakingLocationRequest()
        map ?: return
        map.setLocationSource(this)
        map.isMyLocationEnabled = true

        context ?: return
        if (darkTheme) {
            if (mapStyleOptions == null)
                mapStyleOptions = MapStyleOptions
                    .loadRawResourceStyle(context, R.raw.map_style_dark)
            map.setMapStyle(mapStyleOptions)
        }
    }

    fun pingLocationSource(snapshot: LocationSnapshot?) {
        snapshot ?: return
        locationSourceListener?.onLocationChanged(
            snapshot.toAndroidLocation()
        )
        if (!hadAnyLocation) {
            if (setMapPositionToBeLatestLocation(instant = false))
                hadAnyLocation = true
        }
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        locationSourceListener = listener
        pingLocationSource(location.lastKnownLocation.value)
    }

    override fun deactivate() = Unit

    override fun onCleared() {
        super.onCleared()
        mapReference = null
        considerMakingLocationRequest()
    }

    private val baseGoogleMapOptions = GoogleMapOptions()
        .compassEnabled(false)
        .mapType(GoogleMap.MAP_TYPE_NORMAL)
        .mapToolbarEnabled(false)
        .rotateGesturesEnabled(false)
        .tiltGesturesEnabled(false)
        .maxZoomPreference(17F)

    fun createMapOptions(): GoogleMapOptions {
        return location.lastKnownLocation.value?.run {
            baseGoogleMapOptions.camera(
                CameraPosition.fromLatLngZoom(
                    LatLng(latitude, longitude),
                    DEFAULT_ZOOM_LEVEL
                )
            )
        } ?: baseGoogleMapOptions
    }
}