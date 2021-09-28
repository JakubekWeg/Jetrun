package pl.jakubweg.jetrun.component

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started
import pl.jakubweg.jetrun.component.WorkoutState.Started.*
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.InitialWaiting
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.NoPermission
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

    private val lastLocationData = MutableStateFlow<LocationSnapshot?>(null)

    private val locationProviderComponent = mock(LocationProviderComponent::class.java).apply {
        `when`(this.hasLocationPermission).thenReturn(true)
        `when`(this.lastKnownLocation).thenReturn(lastLocationData)
        `when`(this.start(anyBoolean())).thenReturn(1)
    }

    @Before
    fun before() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
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

        assertTrue(c.workoutState.value is None)

        testCoroutineDispatcher.cleanupTestCoroutines()

        verify(locationProviderComponent, times(1)).start()
        verify(locationProviderComponent, times(1)).stop(anyInt())
    }

    @Test
    fun `cleanUp() gets called and forwards stop to other components`() {
        val location = locationProviderComponent
        val timer = mock(TimerCoroutineComponent::class.java)
        val c = createComponent(location = location, timer = timer)

        c.cleanUp()

        // should be zero as it didn't start
        verify(location, times(0)).stop(anyInt())
        verify(timer, times(1)).stop()
        verify(workoutStatsComponent, times(1)).resetStats()
    }

    @Test
    fun `tracker waits for permission being granted before requesting location updates`() {
        Dispatchers.setMain(testCoroutineDispatcher)
        testCoroutineDispatcher.pauseDispatcher()
        val c = createComponent()

        `when`(locationProviderComponent.hasLocationPermission).thenReturn(false)

        c.start()
        assertTrue(c.workoutState.value is Started)

        repeat(4) {
            testCoroutineDispatcher.advanceTimeBy(2000L)
        }

        assertTrue(c.workoutState.value is WaitingForLocation)
        assertTrue(c.workoutState.value is NoPermission)

        `when`(locationProviderComponent.hasLocationPermission).thenReturn(true)

        testCoroutineDispatcher.advanceTimeBy(5000L)

        assertIs(InitialWaiting, c.workoutState.value)

        Dispatchers.resetMain()
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
        Dispatchers.setMain(testCoroutineDispatcher)
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
        Dispatchers.resetMain()
    }

    @Test
    fun `resume() waits for sets status to NoPermission is permission is still missing`() {
        `when`(locationProviderComponent.hasLocationPermission).thenReturn(false)
        val c = createComponent()

        c.start()
        assertIs(NoPermission::class, c.workoutState.value)

        c.pauseWorkout()
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertIs(Paused::class, c.workoutState.value)

        c.resumeWorkout()
        assertIs(RequestedResume::class, c.workoutState.value)
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        assertIs(WaitingForLocation::class, c.workoutState.value)
        assertIs(NoPermission::class, c.workoutState.value)
    }

    @Test
    fun `When paused by user for longer then 60 seconds then location provider gets paused and then restarted when resumed`() {
        `when`(locationProviderComponent.hasLocationPermission).thenReturn(true)
        val c = createComponent()

        c.start()
        verify(locationProviderComponent, times(1)).start(anyBoolean())
        verify(locationProviderComponent, times(0)).stop(anyInt())

        c.pauseWorkout()
        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        // check if nothing stopped
        verify(locationProviderComponent, times(1)).start(anyBoolean())
        verify(locationProviderComponent, times(0)).stop(anyInt())

        assertIs(Paused::class, c.workoutState.value)

        for (i in 1..60) {
            timeComponent.advanceTimeMillis(1000L)
            testCoroutineDispatcher.advanceTimeBy(1000L)
        }

        // check if requested provider to stop
        verify(locationProviderComponent, times(1)).start(anyBoolean())
        verify(locationProviderComponent, times(1)).stop(anyInt())

        c.resumeWorkout()

        timeComponent.advanceTimeMillis(1000L)
        testCoroutineDispatcher.advanceTimeBy(1000L)

        // check if provider requested updates again
        verify(locationProviderComponent, times(2)).start(anyBoolean())
        verify(locationProviderComponent, times(1)).stop(anyInt())

        assertIs(WaitingForLocation::class, c.workoutState.value)

        c.pauseWorkout()

        repeat(5) {
            timeComponent.advanceTimeMillis(1000L)
            testCoroutineDispatcher.advanceTimeBy(1000L)
        }

        // should not pause as time since pause is zero seconds
        verify(locationProviderComponent, times(2)).start(anyBoolean())
        verify(locationProviderComponent, times(1)).stop(anyInt())
        assertIs(Paused::class, c.workoutState.value)

        for (i in 1..60) {
            timeComponent.advanceTimeMillis(1000L)
            testCoroutineDispatcher.advanceTimeBy(1000L)
        }

        // check if requested provider to stop
        verify(locationProviderComponent, times(2)).start(anyBoolean())
        verify(locationProviderComponent, times(2)).stop(anyInt())
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