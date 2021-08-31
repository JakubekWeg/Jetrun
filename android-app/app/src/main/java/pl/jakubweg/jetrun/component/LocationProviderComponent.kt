package pl.jakubweg.jetrun.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import pl.jakubweg.jetrun.util.nonMutable
import javax.inject.Inject

data class LocationSnapshot constructor(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long
)

private val Location.asSnapshot: LocationSnapshot
    get() = LocationSnapshot(latitude, longitude, altitude, time)


class LocationProviderComponent @Inject constructor(
    private val context: Context,
    private val locationManager: LocationManager,
    defaultDispatcher: CoroutineDispatcher,
) : LocationListener {
    private val _lastKnownLocation = MutableLiveData<LocationSnapshot?>()
    val lastKnownLocation = _lastKnownLocation.nonMutable

    private val scope = CoroutineScope(defaultDispatcher)

    @SuppressLint("MissingPermission")
    fun start() {
        check(hasLocationPermission)

        locationManager.requestLocationUpdates(GPS_PROVIDER, 1000L, 0f, this)
    }

    @VisibleForTesting
    val hasLocationPermission
        get() = context
            .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun stop() {
        locationManager.removeUpdates(this)
        scope.cancel()
    }

    @MainThread
    override fun onLocationChanged(location: Location) {
        scope.launch {
            _lastKnownLocation.postValue(location.asSnapshot)
        }
    }
}