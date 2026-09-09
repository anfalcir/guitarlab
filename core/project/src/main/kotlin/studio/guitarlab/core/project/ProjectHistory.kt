package studio.guitarlab.core.project

import java.util.ArrayDeque
import studio.guitarlab.core.model.GuitarProject

/**
 * Bounded in-memory history of project metadata snapshots.
 * GuitarProject contains references/metadata only; immutable managed audio bytes are never copied, rewritten or deleted here.
 */
class ProjectHistory(private val capacity: Int = DEFAULT_CAPACITY) {
    private val undoStack = ArrayDeque<GuitarProject>()
    private val redoStack = ArrayDeque<GuitarProject>()

    init {
        require(capacity > 0) { "History capacity must be positive." }
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoDepth: Int get() = undoStack.size
    val redoDepth: Int get() = redoStack.size

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun record(previous: GuitarProject, current: GuitarProject) {
        require(previous.id == current.id) { "History snapshots must belong to the same project." }
        if (previous == current) return
        undoStack.addLast(previous)
        trimToCapacity(undoStack)
        redoStack.clear()
    }

    fun undo(current: GuitarProject): GuitarProject? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeLast()
        require(previous.id == current.id) { "History snapshot belongs to a different project." }
        redoStack.addLast(current)
        trimToCapacity(redoStack)
        return previous
    }

    fun redo(current: GuitarProject): GuitarProject? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeLast()
        require(next.id == current.id) { "History snapshot belongs to a different project." }
        undoStack.addLast(current)
        trimToCapacity(undoStack)
        return next
    }

    private fun trimToCapacity(stack: ArrayDeque<GuitarProject>) {
        while (stack.size > capacity) stack.removeFirst()
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 50
    }
}
