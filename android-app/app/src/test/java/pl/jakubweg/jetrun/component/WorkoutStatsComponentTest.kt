package pl.jakubweg.jetrun.component

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import pl.jakubweg.jetrun.component.WorkoutState.Started.Active
import pl.jakubweg.jetrun.util.anyNonNull

class WorkoutStatsComponentTest {
    @Test
    fun `Starts with empty stats`() {
        val c = WorkoutStatsComponent()
        val stats = c.stats.value
        assertEquals(0.0, stats.totalMeters, 0.0)
        assertEquals(0, stats.totalMillis)
    }

    @Test
    fun `Updates time when provided two the same location updates`() {
        val c = WorkoutStatsComponent()
        val location = LocationSnapshot(0.0, 0.0)

        assertEquals(0L, c.stats.value.totalMillis)

        c.update(location, Active, 1000)
        assertEquals(0L, c.stats.value.totalMillis)

        c.update(location, Active, 2300)
        assertEquals(1300L, c.stats.value.totalMillis)

        c.update(location, Active, 4301)
        assertEquals(3301, c.stats.value.totalMillis)
    }

    @Test
    fun `Updates distance when provided different locations`() {
        val c = WorkoutStatsComponent()

        assertEquals(0.0, c.stats.value.totalMeters, .0)

        c.update(mock(LocationSnapshot::class.java).apply {
            `when`(distanceTo(anyNonNull())).thenReturn(1000.0)
        }, Active, 1000)
        assertEquals(0.0, c.stats.value.totalMeters, .0)

        c.update(mock(LocationSnapshot::class.java).apply {
            `when`(distanceTo(anyNonNull())).thenReturn(300.0)
        }, Active, 2300)
        assertEquals(1000.0, c.stats.value.totalMeters, .0)

        c.update(
            LocationSnapshot(0.0, 0.0),
            Active, 4301
        )
        assertEquals(1300.0, c.stats.value.totalMeters, 0.0)
    }

    @Test
    fun `Updates current average speed`() = WorkoutStatsComponent().run {
        assertEquals(0.0, stats.value.currentAverageSpeed, .0)

        kotlin.run {
            update(mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenReturn(10.0)
            }, Active, 1000)
            assertEquals(0.0, stats.value.currentAverageSpeed, .0)

            update(mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenReturn(7.0)
            }, Active, 3500L)

            // km/h
            val speed = (10.0 / 1000.0) / (2500L / 1000.0 / 60.0 / 60.0)
            assertEquals(speed, stats.value.currentAverageSpeed, 0.001)
        }


        kotlin.run {
            update(mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenThrow(IllegalStateException::class.java)
            }, Active, 5500L)

            // km/h
            val speed = (17.0 / 1000.0) / (4500L / 1000.0 / 60.0 / 60.0)
            assertEquals(speed, stats.value.currentAverageSpeed, 0.001)
        }
    }

    @Test
    fun `Updates current average speed only by latest location (ignores locations older 15 seconds)`() =
        WorkoutStatsComponent().run {
            assertEquals(0.0, stats.value.currentAverageSpeed, .0)

            update(mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenReturn(10.0)
            }, Active, 0)

            for (i in 1..15) {
                update(mock(LocationSnapshot::class.java).apply {
                    `when`(distanceTo(anyNonNull())).thenReturn(7.0)
                }, Active, i * 1000L)
            }

            assertEquals(
                ((10.0 + 7.0 * 14) / 1000.0) / (15_000L / 1000.0 / 60.0 / 60.0),
                stats.value.currentAverageSpeed,
                0.001
            )


            for (i in 1..5) {
                update(mock(LocationSnapshot::class.java).apply {
                    `when`(distanceTo(anyNonNull())).thenReturn(6.0)
                }, Active, (15 + i) * 1000L)
            }

            assertEquals(
                ((4 * 6 + 7.0 * 11) / 1000.0) / (15_000L / 1000.0 / 60.0 / 60.0),
                stats.value.currentAverageSpeed,
                0.001
            )
        }

    @Test
    fun `Doesn't updates distance when provided the same locations`() =
        WorkoutStatsComponent().run {
            assertEquals(0.0, stats.value.totalMeters, .0)

            val location = mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenReturn(10.0)
            }

            update(location, Active, 0)

            assertEquals(0.0, stats.value.totalMeters, .0)
            assertEquals(0, stats.value.totalMillis)

            update(location, Active, 1234)

            assertEquals(0.0, stats.value.totalMeters, .0)
            assertEquals(1234, stats.value.totalMillis)

            update(mock(LocationSnapshot::class.java).apply {
                `when`(distanceTo(anyNonNull())).thenReturn(11.0)
            }, Active, 2465)

            assertEquals(10.0, stats.value.totalMeters, .0)
            assertEquals(2465, stats.value.totalMillis)
        }
}
