package pl.jakubweg.jetrun.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.WorkoutState
import pl.jakubweg.jetrun.component.WorkoutTrackerComponent

@RunWith(JUnit4::class)
class CurrentWorkoutViewModelTest {
    private val workoutState = MutableStateFlow<WorkoutState>(WorkoutState.None)
    private val tracker = mock(WorkoutTrackerComponent::class.java).apply {
        `when`(workoutState).thenReturn(this@CurrentWorkoutViewModelTest.workoutState)
    }
    private val vm = CurrentWorkoutViewModel(tracker)

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
}