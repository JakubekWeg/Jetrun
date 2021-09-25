package pl.jakubweg.jetrun.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.jakubweg.jetrun.util.nonMutable
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

data class LocationSnapshot constructor(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long
) {
    constructor(lat: Double, lng: Double) : this(lat, lng, 0.0, 0)

    fun distanceTo(other: LocationSnapshot): Double {
        // https://stackoverflow.com/a/8050255/13773788
        val pk = (180f / Math.PI).toFloat()

        val a1 = latitude / pk
        val a2 = longitude / pk
        val b1 = other.latitude / pk
        val b2 = other.longitude / pk

        val t1 = cos(a1) * cos(a2) * cos(b1) * cos(b2)
        val t2 = cos(a1) * sin(a2) * cos(b1) * sin(b2)
        val t3 = sin(a1) * sin(b1)
        val tt = acos(t1 + t2 + t3)

        return if (tt.isNaN())
            0.0
        else
            6366000.0 * tt
    }
}

private val Location.asSnapshot: LocationSnapshot
    get() = LocationSnapshot(latitude, longitude, altitude, time)

typealias LocationRequestId = Int

class LocationProviderComponent @Inject constructor(
    private val context: Context,
    private val locationManager: LocationManager,
    defaultDispatcher: CoroutineDispatcher,
) : LocationListener {
    private var nextRequestId = 1
    private val activeRequestIds = mutableSetOf<LocationRequestId>()
    private val _lastKnownLocation = MutableLiveData<LocationSnapshot?>()
    val lastKnownLocation = _lastKnownLocation.nonMutable

    private val scope = CoroutineScope(defaultDispatcher)


    @VisibleForTesting
    val hasLocationPermission
        get() = context
            .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @MainThread
    fun start(): LocationRequestId {
        check(hasLocationPermission)

        synchronized(this) {
            val needsToActivateUpdates = activeRequestIds.isEmpty()
            if (needsToActivateUpdates)
                activateAndroidProvider()

            val thisRequestId: LocationRequestId = nextRequestId++
            activeRequestIds.add(thisRequestId)
            return thisRequestId
        }
    }

    @AnyThread
    fun stop(id: LocationRequestId) {
        synchronized(this) {
            activeRequestIds.remove(id)
            val needsToDeactivate = activeRequestIds.isEmpty()
            if (needsToDeactivate)
                deactivateAndroidProvider()
        }
    }

    private fun deactivateAndroidProvider() {
        locationManager.removeUpdates(this)
    }

    @SuppressLint("MissingPermission")
    private fun activateAndroidProvider() {
        locationManager.requestLocationUpdates(GPS_PROVIDER, 1000L, 0f, this)
    }

    @MainThread
    override fun onLocationChanged(location: Location) {
        scope.launch {
            _lastKnownLocation.postValue(location.asSnapshot)
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extra: Bundle) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
}