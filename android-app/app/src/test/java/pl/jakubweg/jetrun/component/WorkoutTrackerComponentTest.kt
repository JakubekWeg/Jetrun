package pl.jakubweg.jetrun.component

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
    fun `start() when started fails`() {
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
    fun `stopWorkout() stops workout`() {
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
    }

    private fun createComponent(
        timer: TimerCoroutineComponent = TimerCoroutineComponent(
            testCoroutineDispatcher
        )
    ) = WorkoutTrackerComponent(
        timer = timer
    )
}