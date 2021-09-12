package pl.jakubweg.jetrun.component

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class WorkoutStats constructor(
    val totalMeters: Double,
    val totalMillis: Long,
    /** Kilometers per hour */
    val currentAverageSpeed: Double
)

sealed class SnapshotOfferResult {
    object Accepted : SnapshotOfferResult()
}

class WorkoutStatsComponent @Inject constructor(
) {
    private val _stats = MutableStateFlow(WorkoutStats(0.0, 0, 0.0))
    val stats = _stats.asStateFlow()

    private var lastLocationSnapshot: LocationSnapshot? = null
    private var lastTimestamp = 0L

    private data class DistanceTimestamp(val totalDistance: Double, val timestamp: Long)

    private var distanceQueue = ArrayDeque<DistanceTimestamp>()

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

    private fun advanceStatsAndCalculateAverageSpeedKMH(
        currentTimeMillis: Long,
        totalDistance: Double
    ): Double {
        distanceQueue.removeAll { it.timestamp < currentTimeMillis - 15_000 }
        val latest = DistanceTimestamp(totalDistance, currentTimeMillis)
        val distanceForAverage = latest.totalDistance - distanceQueue.first().totalDistance
        val millisForAverage = latest.timestamp - distanceQueue.first().timestamp
        val speedKMH = (distanceForAverage / 1000.0) / (millisForAverage / 1000.0 / 60.0 / 60.0)
        distanceQueue.addLast(latest)
        return speedKMH
    }

    fun onPaused() {
        _stats.value = _stats.value.copy(currentAverageSpeed = 0.0)
        lastLocationSnapshot = null
        lastTimestamp = 0L
    }
}