package pl.jakubweg.jetrun.component

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.jakubweg.jetrun.component.SnapshotOfferResult.Accepted
import javax.inject.Inject

data class WorkoutStats constructor(
    val totalMeters: Double,
    val totalMillis: Long,
)

sealed class SnapshotOfferResult {
    object Accepted : SnapshotOfferResult()
}

class WorkoutStatsComponent @Inject constructor(
) {
    private val _stats = MutableStateFlow(WorkoutStats(0.0, 0))
    val stats = _stats.asStateFlow()


    private var lastLocationSnapshot: LocationSnapshot? = null
    fun takeSnapshot(snapshot: LocationSnapshot): SnapshotOfferResult {
        val last = lastLocationSnapshot
        if (last != null) {
            require(snapshot.timestamp > last.timestamp) { "Invalid timestamp inside location last=${last.timestamp} now=${snapshot.timestamp}" }
            val distance = last.distanceTo(snapshot)
            val duration = snapshot.timestamp - last.timestamp

            val current = stats.value
            _stats.value = current.copy(
                totalMeters = current.totalMeters + distance,
                totalMillis = current.totalMillis + duration,
            )
        }

        lastLocationSnapshot = snapshot
        return Accepted
    }
}