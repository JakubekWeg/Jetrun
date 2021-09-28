package pl.jakubweg.jetrun.component

interface TimeComponent {
    fun currentTimeMillis(): Long
}

class RealTimeComponent : TimeComponent {
    override fun currentTimeMillis() = System.currentTimeMillis()
}

class FakeTimeComponent : TimeComponent {
    private var _currentTime = 0L

    override fun currentTimeMillis() = _currentTime

    fun advanceTimeMillis(millis: Long) {
        require(millis >= 0) { "Millis are required to be >= 0, got $millis" }
        this.setTimeMillis(this._currentTime + millis)
    }

    fun setTimeMillis(millis: Long) {
        require(millis >= 0) { "Millis are required to be >= 0, got $millis" }
        this._currentTime = millis
    }
}