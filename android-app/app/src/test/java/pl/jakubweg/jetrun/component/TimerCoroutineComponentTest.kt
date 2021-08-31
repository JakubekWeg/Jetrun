package pl.jakubweg.jetrun.component

import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger
import kotlin.IllegalStateException

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class TimerCoroutineComponentTest : TestCase() {
    private val testDispatcher = TestCoroutineDispatcher()

    @After
    public override fun tearDown() {
        testDispatcher.resumeDispatcher()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `Timer cancels when no timers run`() = testDispatcher.runBlockingTest {
        val timer = TimerCoroutineComponent(testDispatcher)
        timer.stop()
    }

    @Test
    fun `Timer fires right after request`() = testDispatcher.runBlockingTest {
        val timer = TimerCoroutineComponent(testDispatcher)
        val atomicInteger = AtomicInteger(0)
        pauseDispatcher()
        timer.start(1000L) {
            atomicInteger.incrementAndGet()
            timer.stop()
        }
        assertEquals(0, atomicInteger.get())
        resumeDispatcher()
        assertEquals(1, atomicInteger.get())
        timer.stop()
    }

    @Test(expected = IllegalStateException::class)
    fun `Multiple starts fails`() {
        testDispatcher.pauseDispatcher()
        val timerCoroutineComponent = TimerCoroutineComponent(testDispatcher)
        try {
            timerCoroutineComponent.start(1) {}
            timerCoroutineComponent.start(1) {}
        } finally {
            timerCoroutineComponent.stop()
        }

    }

}