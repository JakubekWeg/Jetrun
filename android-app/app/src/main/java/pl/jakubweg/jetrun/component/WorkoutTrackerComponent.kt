package pl.jakubweg.jetrun.component

import androidx.lifecycle.MutableLiveData
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started.RequestedStop
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.*
import pl.jakubweg.jetrun.util.nonMutable
import javax.inject.Inject

sealed class WorkoutState {
    object None : WorkoutState()
    sealed class Started : WorkoutState() {
        sealed class WaitingForLocation : Started() {
            object NoPermission : WaitingForLocation()
            object InitialWaiting : WaitingForLocation()
        }

        object RequestedStop : Started()
    }
}

class WorkoutTrackerComponent @Inject constructor(
    private val timer: TimerCoroutineComponent,
    private val location: LocationProviderComponent
) {
    private val _workoutState = MutableLiveData<WorkoutState>(None)

    val workoutState = _workoutState.nonMutable

    fun start() {
        check(_workoutState.value is None) { "Workout already started" }

        if (location.hasLocationPermission) {
            location.start()
            _workoutState.postValue(InitialWaiting)
        } else {
            _workoutState.postValue(NoPermission)
        }

        timer.start(1000L, this::timerCallback)
    }

    fun stopWorkout() {
        if (_workoutState.value is None) return
        _workoutState.postValue(RequestedStop)
    }

    private fun timerCallback() {
        val currentState = _workoutState.value ?: return
        when (currentState) {
            NoPermission -> {
                if (location.hasLocationPermission) {
                    location.start()
                    _workoutState.postValue(InitialWaiting)
                }
            }
            RequestedStop -> {
                timer.stop()
                location.stop()
                _workoutState.postValue(None)
            }
            else -> println("WARNING^WorkoutTrackerComponent: unknown state ${currentState.javaClass.simpleName}")
        }
    }
}