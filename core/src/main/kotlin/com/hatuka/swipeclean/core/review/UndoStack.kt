package com.hatuka.swipeclean.core.review

/**
 * Bounded LIFO stack: pushing beyond [capacity] silently drops the oldest entry.
 * Used for the last ~20 swipe actions of a session.
 */
class UndoStack<T>(val capacity: Int = DEFAULT_CAPACITY) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val items = ArrayDeque<T>()

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()

    fun push(item: T) {
        if (items.size == capacity) items.removeFirst()
        items.addLast(item)
    }

    fun pop(): T? = items.removeLastOrNull()

    fun peek(): T? = items.lastOrNull()

    fun clear() = items.clear()

    companion object {
        const val DEFAULT_CAPACITY = 20
    }
}
