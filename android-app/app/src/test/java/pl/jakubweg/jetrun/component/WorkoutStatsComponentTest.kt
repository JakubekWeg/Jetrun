package pl.jakubweg.jetrun.component

import org.junit.Assert.*
import org.junit.Test
import pl.jakubweg.jetrun.component.SnapshotOfferResult.Accepted

class WorkoutStatsComponentTest {
    @Test
    fun `Starts with empty stats`() {
        val c = WorkoutStatsComponent()
        val stats = c.stats.value
        assertEquals(0.0, stats.totalMeters, 0.0)
        assertEquals(0, stats.totalMillis)
    }

    @Test
    fun `Updates stats when provided two location snapshot`() {
        val c = WorkoutStatsComponent()

        assertTrue(c.takeSnapshot(LocationSnapshot(1.0, 1.0, 1.0, 1000)) is Accepted)

        c.stats.value.apply {
            assertEquals(0.0, totalMeters, 0.0)
            assertEquals(0, totalMillis)
        }

        assertTrue(c.takeSnapshot(LocationSnapshot(1.0, 2.0, 1.0, 2200)) is Accepted)

        c.stats.value.apply {
            assertNotEquals(0.0, totalMeters, 1.0)
            assertEquals(1200, totalMillis)
        }
    }
}