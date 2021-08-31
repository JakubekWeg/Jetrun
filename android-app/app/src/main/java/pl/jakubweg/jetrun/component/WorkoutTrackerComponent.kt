package pl.jakubweg.jetrun.component

import androidx.lifecycle.MutableLiveData
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started.RequestedStop
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation
import pl.jakubweg.jetrun.util.nonMutable
import javax.inject.Inject

sealed class WorkoutState {
    object None : WorkoutState()
    sealed class Started : WorkoutState() {
        object WaitingForLocation : Started()
        object RequestedStop : Started()
    }
}

class WorkoutTrackerComponent @Inject constructor(
    private val timer: TimerCoroutineComponent
) {
    private val _workoutState = MutableLiveData<WorkoutState>(None)

    val workoutState = _workoutState.nonMutable

    fun start() {
        check(_workoutState.value is None) { "Workout already started" }
        _workoutState.postValue(WaitingForLocation)

        timer.start(1000L, this::timerCallback)
    }

    fun stopWorkout() {
        if (_workoutState.value is None) return
        _workoutState.postValue(RequestedStop)
    }

    private fun timerCallback() {
        val currentState = _workoutState.value ?: return
        when (currentState) {
            RequestedStop -> {
                timer.stop()
                _workoutState.postValue(None)
            }
            else -> println("WARNING^WorkoutTrackerComponent: unknown state ${currentState.javaClass.simpleName}")
        }
    }
}