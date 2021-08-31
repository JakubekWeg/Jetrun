package pl.jakubweg.jetrun.component

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started
import pl.jakubweg.jetrun.component.WorkoutState.Started.RequestedStop
import pl.jakubweg.jetrun.util.anyNonNull
import kotlin.IllegalStateException

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class WorkoutTrackerComponentTest : TestCase() {
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun `Default workout state is none`() {
        val c = createComponent()
        assertTrue(c.workoutState.value is None)
    }

    @Test(expected = IllegalStateException::class)
    fun `start() fails if already started`() {
        val c = createComponent()
        c.start()
        c.start()
    }

    @Test
    fun `start() changes status`() {
        val c = createComponent()
        c.start()
        assertTrue(c.workoutState.value is Started)
    }


    @Test
    fun `start() requests timer to start`() {
        val timer = mock(TimerCoroutineComponent::class.java)
        val c = createComponent(timer = timer)

        c.start()

        verify(timer, times(1)).start(anyLong(), anyNonNull())
    }

    @Test
    fun `stopWorkout() stops workout and does stop location service`() {
        val location = mock(LocationProviderComponent::class.java)
        `when`(location.hasLocationPermission).thenReturn(true)
        val c = createComponent(location = location)

        testCoroutineDispatcher.pauseDispatcher()
        c.start()
        testCoroutineDispatcher.advanceTimeBy(5000L)
        assertTrue(c.workoutState.value is Started)
        c.stopWorkout()
        assertTrue(c.workoutState.value is RequestedStop)

        testCoroutineDispatcher.advanceTimeBy(1000L)
        testCoroutineDispatcher.resumeDispatcher()

        assertTrue(c.workoutState.value is None)

        testCoroutineDispatcher.cleanupTestCoroutines()

        verify(location, times(1)).start()
        verify(location, times(1)).stop()
    }

    @Test
    fun `tracker waits for permission being granted before requesting location updates`() {
        testCoroutineDispatcher.pauseDispatcher()
        val location = mock(LocationProviderComponent::class.java)
        val c = createComponent(location = location)

        `when`(location.hasLocationPermission).thenReturn(false)

        c.start()
        assertTrue(c.workoutState.value is Started)

        testCoroutineDispatcher.advanceTimeBy(2000L)
        testCoroutineDispatcher.advanceTimeBy(2000L)
        testCoroutineDispatcher.advanceTimeBy(2000L)

        assertTrue(c.workoutState.value is Started.WaitingForLocation)
        assertTrue(c.workoutState.value is Started.WaitingForLocation.NoPermission)

        `when`(location.hasLocationPermission).thenReturn(true)

        testCoroutineDispatcher.advanceTimeBy(5000L)

        assertTrue(c.workoutState.value is Started.WaitingForLocation.InitialWaiting)
    }

    private fun createComponent(
        timer: TimerCoroutineComponent = TimerCoroutineComponent(
            testCoroutineDispatcher
        ),
        location: LocationProviderComponent = mock(LocationProviderComponent::class.java)
    ) = WorkoutTrackerComponent(
        timer = timer,
        location = location,
    )
}