package pl.jakubweg.jetrun.component

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
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
import pl.jakubweg.jetrun.component.WorkoutState.Started.*
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.InitialWaiting
import pl.jakubweg.jetrun.util.anyNonNull
import pl.jakubweg.jetrun.util.assertIs

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class WorkoutTrackerComponentTest : TestCase() {
    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val timeComponent = FakeTimeComponent()
    private val workoutStatsComponent = mock(WorkoutStatsComponent::class.java).apply {
        `when`(
            this.update(
                anyNonNull(),
                anyLong()
            )
        ).thenReturn(SnapshotOfferResult.Accepted)
    }

    private val lastLocationData = MutableLiveData<LocationSnapshot>()

    private val locationProviderComponent = mock(LocationProviderComponent::class.java).apply {
        `when`(this.hasLocationPermission).thenReturn(true)
        `when`(this.lastKnownLocation).thenReturn(lastLocationData)
    }

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
        val c = createComponent()

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

        verify(locationProviderComponent, times(1)).start()
        verify(locationProviderComponent, times(1)).stop()
    }

    @Test
    fun `cleanUp() gets called and forwards stop to other components`() {
        val location = locationProviderComponent
        val timer = mock(TimerCoroutineComponent::class.java)
        val c = createComponent(location = location, timer = timer)

        c.cleanUp()

        verify(location, times(1)).stop()
        verify(timer, times(1)).stop()
    }

    @Test
    fun `tracker waits for permission being granted before requesting location updates`() {
        testCoroutineDispatcher.pauseDispatcher()
        val c = createComponent()

        `when`(locationProviderComponent.hasLocationPermission).thenReturn(false)

        c.start()
        assertTrue(c.workoutState.value is Started)

        repeat(4) {
            testCoroutineDispatcher.advanceTimeBy(2000L)
        }

        assertTrue(c.workoutState.value is WaitingForLocation)
        assertTrue(c.workoutState.value is WaitingForLocation.NoPermission)

        `when`(locationProviderComponent.hasLocationPermission).thenReturn(true)

        testCoroutineDispatcher.advanceTimeBy(5000L)

        assertTrue(c.workoutState.value is InitialWaiting)
    }

    @Test
    fun `tracker changes status to active when got location`() {
        testCoroutineDispatcher.pauseDispatcher()
        val snapshot = mock(LocationSnapshot::class.java)
        val c = createComponent()

        c.start()

        testCoroutineDispatcher.advanceTimeBy(3000L)
        lastLocationData.value = snapshot
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertTrue(c.workoutState.value is Active)
    }

    @Test
    fun `tracker forwards location to stats component`() {
        testCoroutineDispatcher.pauseDispatcher()
        val snapshot = mock(LocationSnapshot::class.java)
        val c = createComponent()

        c.start()

        lastLocationData.value = snapshot
        testCoroutineDispatcher.advanceTimeBy(2500L)

        verify(workoutStatsComponent, times(3)).update(anyNonNull(), anyLong())
    }

    @Test
    fun `pauses when requested pause`() {
        val c = createComponent()

        assertIs(None, c.workoutState.value)

        c.pauseWorkout() // should ignore, because workout still not started
        assertIs(None, c.workoutState.value)

        c.start()
        assertIs(Started::class, c.workoutState.value)

        c.pauseWorkout()
        assertIs(RequestedPause::class, c.workoutState.value)

        timeComponent.advanceTimeMillis(1500L)
        testCoroutineDispatcher.advanceTimeBy(1500L)

        assertIs(Paused::class, c.workoutState.value)
        assertIs(Paused.ByUser, c.workoutState.value)
    }

    @Test
    fun `pause prevents stats component from being updated and calls onPaused method`() {
        val c = createComponent()

        c.start()

        lastLocationData.value = mock(LocationSnapshot::class.java)

        timeComponent.advanceTimeMillis(1200L)
        testCoroutineDispatcher.advanceTimeBy(1200L)

        assertIs(Active::class, c.workoutState.value)
        // check if it was called
        verify(workoutStatsComponent, times(1))
            .update(anyNonNull(), anyLong())
        verify(workoutStatsComponent, times(0))
            .onPaused()

        c.pauseWorkout()
        lastLocationData.value = mock(LocationSnapshot::class.java)

        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertIs(Paused::class, c.workoutState.value)
        // it should not have been called
        verify(workoutStatsComponent, times(2))
            .update(anyNonNull(), anyLong())

        verify(workoutStatsComponent, times(1))
            .onPaused()

        for (i in 1..10) {
            timeComponent.advanceTimeMillis(i * 1000L)
            testCoroutineDispatcher.advanceTimeBy(i * 1000L)
        }

        verify(workoutStatsComponent, times(2))
            .update(anyNonNull(), anyLong())
        verify(workoutStatsComponent, times(1))
            .onPaused()
    }

    @Test
    fun `resume() waits for new location and then makes workout active again`() {
        val c = createComponent()

        c.start()
        lastLocationData.value = mock(LocationSnapshot::class.java)

        timeComponent.advanceTimeMillis(1200L)
        testCoroutineDispatcher.advanceTimeBy(1200L)

        assertIs(Active::class, c.workoutState.value)

        val locationThatShouldNotBeProvidedAgain = mock(LocationSnapshot::class.java)
        lastLocationData.value = locationThatShouldNotBeProvidedAgain

        c.pauseWorkout()
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)
        verify(workoutStatsComponent, times(2))
            .update(anyNonNull(), anyLong())

        `when`(workoutStatsComponent.update(anyNonNull(), anyLong())).thenAnswer {
            require(it.getArgument<Any?>(0) !== locationThatShouldNotBeProvidedAgain)
            return@thenAnswer SnapshotOfferResult.Accepted
        }

        assertIs(Paused::class, c.workoutState.value)

        c.resumeWorkout()
        assertIs(RequestedResume::class, c.workoutState.value)
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertIs(WaitingForLocation::class, c.workoutState.value)
        assertIs(WaitingForLocation.AfterPauseWaiting::class, c.workoutState.value)

        lastLocationData.value = mock(LocationSnapshot::class.java)
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertIs(Active::class, c.workoutState.value)
        verify(workoutStatsComponent, times(3))
            .update(anyNonNull(), anyLong())
    }


    private fun createComponent(
        timer: TimerCoroutineComponent = TimerCoroutineComponent(
            testCoroutineDispatcher
        ),
        location: LocationProviderComponent = locationProviderComponent,
        stats: WorkoutStatsComponent = workoutStatsComponent
    ) = WorkoutTrackerComponent(
        timer = timer,
        location = location,
        stats = stats,
        time = timeComponent,
    )
}