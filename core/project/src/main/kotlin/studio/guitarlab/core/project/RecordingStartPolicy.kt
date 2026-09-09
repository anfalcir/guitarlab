package studio.guitarlab.core.project

object RecordingStartPolicy {
    const val START_DELAY_SECONDS: Int = 5

    fun countdownSequence(): List<Int> = (START_DELAY_SECONDS downTo 1).toList()
}
