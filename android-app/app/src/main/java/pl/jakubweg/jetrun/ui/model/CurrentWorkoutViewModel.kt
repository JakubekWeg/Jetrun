package pl.jakubweg.jetrun.ui.model

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import pl.jakubweg.jetrun.component.WorkoutState
import pl.jakubweg.jetrun.component.WorkoutTrackerComponent
import javax.inject.Inject

@HiltViewModel
class CurrentWorkoutViewModel @Inject constructor(
    private val tracker: WorkoutTrackerComponent
) : ViewModel() {
    val currentWorkoutStatus: StateFlow<WorkoutState> get() = tracker.workoutState

    fun onFinishWorkoutClicked() {
        tracker.stopWorkout()
    }

    fun onResumeOrPauseClicked() {
        when (currentWorkoutStatus.value) {
            WorkoutState.None -> tracker.start()
            is WorkoutState.Started.Paused -> tracker.resumeWorkout()
            is WorkoutState.Started -> tracker.pauseWorkout()
        }
    }
}