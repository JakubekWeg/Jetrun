package pl.jakubweg.jetrun.ui.model

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import pl.jakubweg.jetrun.component.WorkoutState
import pl.jakubweg.jetrun.component.WorkoutStats
import pl.jakubweg.jetrun.component.WorkoutStatsComponent
import pl.jakubweg.jetrun.component.WorkoutTrackerComponent
import javax.inject.Inject

@HiltViewModel
class CurrentWorkoutViewModel @Inject constructor(
    private val tracker: WorkoutTrackerComponent,
    private val stats: WorkoutStatsComponent,
) : ViewModel() {
    val currentWorkoutStatus: StateFlow<WorkoutState> = tracker.workoutState
    val currentWorkoutStats: StateFlow<WorkoutStats> = stats.stats

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