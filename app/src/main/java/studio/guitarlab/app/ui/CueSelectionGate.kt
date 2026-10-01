package studio.guitarlab.app.ui

/** UI-thread generation gate: cancelled or replaced preflights cannot commit a selection. */
internal class CueSelectionGate {
    private var generation = 0L
    fun begin(): Long = ++generation
    fun invalidate() { ++generation }
    fun accepts(ticket: Long): Boolean = ticket == generation
}
