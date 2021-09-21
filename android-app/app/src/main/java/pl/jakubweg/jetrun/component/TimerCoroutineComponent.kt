package pl.jakubweg.jetrun.component

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class TimerCoroutineComponent @Inject constructor(
    dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(dispatcher)
    private val started = AtomicBoolean()
    private var job: Job? = null

    fun start(delayMillis: Long, callback: suspend () -> Unit) {
        val changedStatus = started.compareAndSet(false, true)
        check(changedStatus) { "Timer was already started" }

        this.job = scope.launch {
            while (true) {
                callback.invoke()
                delay(delayMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        started.set(false)
    }
}