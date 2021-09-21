package pl.jakubweg.jetrun.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.WorkoutState
import pl.jakubweg.jetrun.component.WorkoutStats
import pl.jakubweg.jetrun.component.WorkoutStatsComponent
import pl.jakubweg.jetrun.component.WorkoutTrackerComponent

@RunWith(JUnit4::class)
class CurrentWorkoutViewModelTest {
    private val workoutState = MutableStateFlow<WorkoutState>(WorkoutState.None)
    private val tracker = mock(WorkoutTrackerComponent::class.java)
    private val workoutStats = MutableStateFlow(WorkoutStats(5.213, 61202, 12.5))
    private val stats = mock(WorkoutStatsComponent::class.java)

    init {
        `when`(stats.stats).thenReturn(workoutStats)
        `when`(tracker.workoutState).thenReturn(workoutState)
    }

    private val vm = CurrentWorkoutViewModel(tracker, stats)

    @Test
    fun `forwards status from tracker`() {
        assertEquals(workoutState, vm.currentWorkoutStatus)
    }


    @Test
    fun `forwards start request calls`() {
        vm.onResumeOrPauseClicked()

        verify(tracker, times(1)).start()
        verify(tracker, times(0)).resumeWorkout()
        verify(tracker, times(0)).pauseWorkout()
        verify(tracker, times(0)).stopWorkout()
    }

    @Test
    fun `forwards pause request calls`() {
        workoutState.value = WorkoutState.Started.WaitingForLocation.InitialWaiting

        vm.onResumeOrPauseClicked()

        verify(tracker, times(0)).start()
        verify(tracker, times(0)).resumeWorkout()
        verify(tracker, times(1)).pauseWorkout()
        verify(tracker, times(0)).stopWorkout()

        workoutState.value = WorkoutState.Started.RequestedResume

        vm.onResumeOrPauseClicked()

        verify(tracker, times(0)).start()
        verify(tracker, times(0)).resumeWorkout()
        verify(tracker, times(2)).pauseWorkout()
        verify(tracker, times(0)).stopWorkout()

        workoutState.value = WorkoutState.Started.Active

        vm.onResumeOrPauseClicked()

        verify(tracker, times(0)).start()
        verify(tracker, times(0)).resumeWorkout()
        verify(tracker, times(3)).pauseWorkout()
        verify(tracker, times(0)).stopWorkout()
    }

    @Test
    fun `forwards resume request calls if workout is paused`() {
        workoutState.value = WorkoutState.Started.Paused.ByUser

        vm.onResumeOrPauseClicked()

        verify(tracker, times(0)).start()
        verify(tracker, times(1)).resumeWorkout()
        verify(tracker, times(0)).pauseWorkout()
        verify(tracker, times(0)).stopWorkout()
    }

    @Test
    fun `forwards finish request calls`() {
        vm.onFinishWorkoutClicked()

        verify(tracker, times(0)).start()
        verify(tracker, times(0)).resumeWorkout()
        verify(tracker, times(0)).pauseWorkout()
        verify(tracker, times(1)).stopWorkout()
    }

    @Test
    fun `forwards stats`() {
        assertTrue(vm.currentWorkoutStats === workoutStats)
    }
}