package pl.jakubweg.jetrun.component

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.util.anyNonNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class LocationProviderComponentTest : TestCase() {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `hasLocationPermission property calls context method`() {
        val result = AtomicInteger(PERMISSION_GRANTED)
        val context = mock(Context::class.java)
        val locationManager = mock(LocationManager::class.java)
        val testDispatcher = TestCoroutineDispatcher()
        `when`(context.checkSelfPermission(anyString())).thenAnswer { result.get() }

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )


        assertTrue(c.hasLocationPermission)
        result.set(PERMISSION_DENIED)
        assertFalse(c.hasLocationPermission)

        verify(context, times(2)).checkSelfPermission(anyString())
    }

    @Test
    fun `Start method requests updates and stop requests listener removal`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        val id = c.start()

        // check if .start() checks permission
        verify(context, times(1)).checkSelfPermission(anyString())
        verify(locationManager, times(1)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )

        c.stop(id)

        verify(locationManager, times(1)).removeUpdates(anyNonNull() as LocationListener)
    }

    @Test(expected = IllegalStateException::class)
    fun `Start method fails if permission missing`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_DENIED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        c.start()
    }

    @Test
    fun `Publishes location`() {
        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(1.0)
        `when`(location.longitude).thenReturn(2.0)
        `when`(location.altitude).thenReturn(3.0)
        `when`(location.time).thenReturn(4)

        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        val locationManager = mock(LocationManager::class.java)
        val listener = AtomicReference<LocationListener?>(null)
        `when`(
            locationManager.requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                anyNonNull() as LocationListener
            )
        ).then {
            listener.set(it.getArgument<LocationListener>(3))
        }

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        assertNull(c.lastKnownLocation.value)
        c.start()
        listener.get()!!.onLocationChanged(location)
        assertNull(c.lastKnownLocation.value)
        testDispatcher.resumeDispatcher()
        testDispatcher.runBlockingTest { }

        val value = c.lastKnownLocation.value
        assertNotNull(value)
        value!!
        assertEquals(1.0, value.latitude)
        assertEquals(2.0, value.longitude)
        assertEquals(3.0, value.altitude)
        assertEquals(4, value.timestamp)
    }

    @Test
    fun `Publishes location after stop and then start again`() {
        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(1.0)

        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        val locationManager = mock(LocationManager::class.java)
        val listener = AtomicReference<LocationListener?>(null)
        `when`(
            locationManager.requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                anyNonNull() as LocationListener
            )
        ).then {
            listener.set(it.getArgument<LocationListener>(3))
        }

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        assertNull(c.lastKnownLocation.value)
        val id = c.start()
        listener.get()!!.onLocationChanged(location)
        assertNull(c.lastKnownLocation.value)
        testDispatcher.resumeDispatcher()
        testDispatcher.runBlockingTest { }

        assertEquals(1.0, c.lastKnownLocation.value?.latitude ?: 0.0, 0.0)

        c.stop(id)
        testDispatcher.resumeDispatcher()
        testDispatcher.pauseDispatcher()

        c.start()
        val newLocation = mock(Location::class.java)
        `when`(newLocation.latitude).thenReturn(2.0)
        listener.get()!!.onLocationChanged(newLocation)
        testDispatcher.resumeDispatcher()

        `when`(location.latitude).thenReturn(2.0)
        assertEquals(2.0, c.lastKnownLocation.value?.latitude ?: 0.0, 0.0)
    }

    @Test
    fun `Multiple concurrent requests work properly`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        val id1 = c.start()

        verify(locationManager, times(1)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )

        val id2 = c.start()
        // should still be 1 as provider was already activated
        verify(locationManager, times(1)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )

        c.stop(id1)
        // should not have called, because there is still one request
        verify(locationManager, times(0)).removeUpdates(anyNonNull() as LocationListener)

        c.stop(id1)
        // check if ignores this stop call
        verify(locationManager, times(0)).removeUpdates(anyNonNull() as LocationListener)

        c.stop(id2)
        // now should be removed
        verify(locationManager, times(1)).removeUpdates(anyNonNull() as LocationListener)
    }


    @Test
    fun `Starting listener with requireStart = false activates system updates if permission is granted`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        c.start(requireStart = false)

        verify(locationManager, times(1)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )
    }

    @Test
    fun `Starting listener with requireStart = false doesn't activate system updates if permission is denied`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_DENIED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        c.start(requireStart = false)

        verify(locationManager, times(0)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )
    }


    @Test
    fun `Starting listener with requireStart = false will activate listener when start called again`() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_DENIED)

        val locationManager = mock(LocationManager::class.java)

        val testDispatcher = TestCoroutineDispatcher()
        testDispatcher.pauseDispatcher()

        val c = LocationProviderComponent(
            context = context,
            locationManager = locationManager,
            defaultDispatcher = testDispatcher
        )

        c.start(requireStart = false)

        verify(locationManager, times(0)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )

        `when`(context.checkSelfPermission(anyString())).thenReturn(PERMISSION_GRANTED)

        c.start(requireStart = false)

        verify(locationManager, times(1)).requestLocationUpdates(
            anyString(),
            anyLong(),
            anyFloat(),
            anyNonNull() as LocationListener
        )
    }
}

