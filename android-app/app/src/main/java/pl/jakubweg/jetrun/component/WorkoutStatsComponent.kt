package pl.jakubweg.jetrun.component

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class WorkoutStats constructor(
    val totalMeters: Double,
    val totalMillis: Long,
    /** Kilometers per hour */
    val currentAverageSpeed: Double
)

sealed class SnapshotOfferResult {
    object Accepted : SnapshotOfferResult()
}

@Singleton
class WorkoutStatsComponent @Inject constructor(
) {
    private val _stats = MutableStateFlow(WorkoutStats(0.0, 0, 0.0))
    val stats = _stats.asStateFlow()

    @VisibleForTesting
    var lastLocationSnapshot: LocationSnapshot? = null

    @VisibleForTesting
    var lastTimestamp = 0L

    data class DistanceTimestamp(val totalDistance: Double, val timestamp: Long)

    @VisibleForTesting
    var distanceQueue = ArrayDeque<DistanceTimestamp>()

    fun update(
        latestSnapshot: LocationSnapshot,
        currentTimeMillis: Long
    ): SnapshotOfferResult {
        val last = lastLocationSnapshot
        if (last != null) {
            val distance = if (last === latestSnapshot) 0.0 else last.distanceTo(latestSnapshot)
            val duration = currentTimeMillis - lastTimestamp

            stats.value.also { current ->
                val speedKMH = advanceStatsAndCalculateAverageSpeedKMH(
                    currentTimeMillis,
                    current.totalMeters + distance
                )

                _stats.value = WorkoutStats(
                    totalMillis = current.totalMillis + duration,
                    totalMeters = current.totalMeters + distance,
                    currentAverageSpeed = speedKMH,
                )
            }
        } else {
            distanceQueue.addLast(DistanceTimestamp(0.0, currentTimeMillis))
        }

        lastTimestamp = currentTimeMillis
        lastLocationSnapshot = latestSnapshot
        return SnapshotOfferResult.Accepted
    }

    fun resetStats() {
        lastTimestamp = 0L
        lastLocationSnapshot = null
        distanceQueue.clear()
        _stats.value = WorkoutStats(0.0, 0, 0.0)
    }

    private fun advanceStatsAndCalculateAverageSpeedKMH(
        currentTimeMillis: Long,
        totalDistance: Double
    ): Double {
        distanceQueue.removeAll { it.timestamp < currentTimeMillis - 15_000 }
        val latest = DistanceTimestamp(totalDistance, currentTimeMillis)
        val first = distanceQueue.firstOrNull()
        distanceQueue.addLast(latest)
        return if (first != null) {
            val distanceForAverage = latest.totalDistance - first.totalDistance
            val millisForAverage = latest.timestamp - first.timestamp
            (distanceForAverage / 1000.0) / (millisForAverage / 1000.0 / 60.0 / 60.0)
        } else {
            0.0
        }
    }

    fun onPaused() {
        _stats.value = _stats.value.copy(currentAverageSpeed = 0.0)
        lastLocationSnapshot = null
        lastTimestamp = 0L
    }
}