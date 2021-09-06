package pl.jakubweg.jetrun.component

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started.Active
import pl.jakubweg.jetrun.component.WorkoutState.Started.RequestedStop
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.InitialWaiting
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.NoPermission
import javax.inject.Inject

sealed class WorkoutState {
    object None : WorkoutState()
    sealed class Started : WorkoutState() {
        sealed class WaitingForLocation : Started() {
            object NoPermission : WaitingForLocation()
            object InitialWaiting : WaitingForLocation()
        }

        object Active : Started()

        object RequestedStop : Started()
    }
}

class WorkoutTrackerComponent @Inject constructor(
    private val timer: TimerCoroutineComponent,
    private val location: LocationProviderComponent,
    private val stats: WorkoutStatsComponent,
    private val time: TimeComponent
) {
    private val _workoutState = MutableStateFlow<WorkoutState>(None)

    val workoutState = _workoutState.asStateFlow()

    fun start() {
        check(_workoutState.value is None) { "Workout already started" }

        if (location.hasLocationPermission) {
            location.start()
            _workoutState.value = InitialWaiting
        } else {
            _workoutState.value = NoPermission
        }

        timer.start(1000L, this::timerCallback)
    }

    fun stopWorkout() {
        if (_workoutState.value is None) return
        _workoutState.value = RequestedStop
    }

    private fun timerCallback() {
        when (val currentState = _workoutState.value) {
            NoPermission -> {
                if (location.hasLocationPermission) {
                    location.start()
                    _workoutState.value = InitialWaiting
                }
            }
            InitialWaiting -> {
                val location = location.lastKnownLocation.value
                if (location != null) {
                    _workoutState.value = Active
                    sendLocationUpdateToStatsComponent(location)
                }
            }
            Active -> {
                location.lastKnownLocation.value?.also {
                    sendLocationUpdateToStatsComponent(it)
                }
            }

            RequestedStop, None -> {
                cleanUp()
                _workoutState.value = None
            }
            else -> println("WARNING^WorkoutTrackerComponent: unknown state ${currentState.javaClass.simpleName}")
        }
    }

    private fun sendLocationUpdateToStatsComponent(location: LocationSnapshot) {
        stats.update(location, _workoutState.value, time.currentTimeMillis())
    }

    @VisibleForTesting
    fun cleanUp() {
        timer.stop()
        location.stop()
    }
}