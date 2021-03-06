package pl.jakubweg.jetrun.component

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.jakubweg.jetrun.component.WorkoutState.None
import pl.jakubweg.jetrun.component.WorkoutState.Started
import pl.jakubweg.jetrun.component.WorkoutState.Started.*
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.InitialWaiting
import pl.jakubweg.jetrun.component.WorkoutState.Started.WaitingForLocation.NoPermission
import javax.inject.Inject

sealed class WorkoutState {
    object None : WorkoutState()
    sealed class Started : WorkoutState() {
        sealed class WaitingForLocation : Started() {
            object NoPermission : WaitingForLocation()
            object InitialWaiting : WaitingForLocation()
            class AfterPauseWaiting(val lastKnownLocation: LocationSnapshot) : WaitingForLocation()
        }

        sealed class Paused : Started() {
            object ByUser : Paused()
        }

        object Active : Started()

        object RequestedStop : Started()
        object RequestedPause : Started()
        object RequestedResume : Started()
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

    @MainThread
    fun start() {
        check(_workoutState.value is None) { "Workout already started" }

        if (location.hasLocationPermission) {
            startGettingLocation()
            _workoutState.value = InitialWaiting
        } else {
            _workoutState.value = NoPermission
        }

        timer.start(1000L, this::timerCallback)
    }

    private var locationProviderRequestId: LocationRequestId = 0
    private var pausedAt: Long = 0L

    @MainThread
    private fun startGettingLocation() {
        synchronized(this) {
            if (locationProviderRequestId == 0)
                locationProviderRequestId = location.start()
        }
    }

    fun stopWorkout() {
        if (_workoutState.value is None) return
        _workoutState.value = RequestedStop
    }

    fun pauseWorkout() {
        if (_workoutState.value is Started && _workoutState.value !is RequestedStop) {
            if (_workoutState.value != Paused.ByUser)
                pausedAt = time.currentTimeMillis()
            _workoutState.value = RequestedPause
        }
    }

    fun resumeWorkout() {
        if (_workoutState.value is Paused) {
            _workoutState.value = RequestedResume
        }
    }

    private suspend fun timerCallback() {
        when (val currentState = _workoutState.value) {
            NoPermission -> {
                if (location.hasLocationPermission) {
                    withContext(Dispatchers.Main) { startGettingLocation() }
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

            Paused.ByUser -> {
                val duration = time.currentTimeMillis() - pausedAt
                if (duration >= 60_000) {
                    stopLocationUpdates()
                }
            }

            is WaitingForLocation.AfterPauseWaiting -> {
                val location = location.lastKnownLocation.value
                if (location != null && location !== currentState.lastKnownLocation) {
                    _workoutState.value = Active
                    sendLocationUpdateToStatsComponent(location)
                }
            }

            Active -> {
                location.lastKnownLocation.value?.also {
                    sendLocationUpdateToStatsComponent(it)
                }
            }

            RequestedPause -> {
                location.lastKnownLocation.value?.also {
                    sendLocationUpdateToStatsComponent(it)
                }
                _workoutState.value = Paused.ByUser
                stats.onPaused()
            }

            RequestedResume -> {
                if (location.hasLocationPermission) {
                    withContext(Dispatchers.Main) { startGettingLocation() }

                    _workoutState.value = location.lastKnownLocation.value?.let {
                        WaitingForLocation.AfterPauseWaiting(it)
                    } ?: InitialWaiting
                } else
                    _workoutState.value = NoPermission
            }

            RequestedStop, None -> {
                cleanUp()
                _workoutState.value = None
            }
            else -> println("WARNING^WorkoutTrackerComponent: unknown state ${currentState.javaClass.simpleName}")
        }
    }

    private fun sendLocationUpdateToStatsComponent(location: LocationSnapshot) {
        stats.update(location, time.currentTimeMillis())
    }

    @VisibleForTesting
    fun cleanUp() {
        timer.stop()
        stopLocationUpdates()
        stats.resetStats()
    }

    private fun stopLocationUpdates() {
        if (locationProviderRequestId != 0) {
            location.stop(locationProviderRequestId)
            locationProviderRequestId = 0
        }
    }
}