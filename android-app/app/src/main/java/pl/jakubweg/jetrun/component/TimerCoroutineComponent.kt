package pl.jakubweg.jetrun.component

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class TimerCoroutineComponent @Inject constructor(
    dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(dispatcher)
    private val started = AtomicBoolean()

    fun start(delayMillis: Long, callback: () -> Unit) {
        val changedStatus = started.compareAndSet(false, true)
        check(changedStatus) { "Timer was already started" }

        scope.launch {
            while (true) {
                callback.invoke()
                delay(delayMillis)
            }
        }
    }

    fun stop() {
        scope.cancel()
        started.set(false)
    }
}