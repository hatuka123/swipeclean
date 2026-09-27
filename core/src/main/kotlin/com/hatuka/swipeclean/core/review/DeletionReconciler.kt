package com.hatuka.swipeclean.core.review

data class DeletionOutcome(val deleted: List<Long>, val failed: List<Long>)

/**
 * After the system trash/delete dialog: an item counts as deleted when MediaStore no longer
 * returns it (trashed items are hidden from normal queries). Anything still visible stays in
 * the bin and is reported. A cancelled dialog deletes nothing.
 */
object DeletionReconciler {
    fun reconcile(requested: List<Long>, stillVisible: Set<Long>, confirmed: Boolean): DeletionOutcome =
        if (!confirmed) {
            DeletionOutcome(deleted = emptyList(), failed = emptyList())
        } else {
            val (failed, deleted) = requested.partition { it in stillVisible }
            DeletionOutcome(deleted, failed)
        }

    /** Splits a large deletion into chunks so each system request stays within Binder limits. */
    fun <T> chunks(items: List<T>, size: Int = CHUNK_SIZE): List<List<T>> = items.chunked(size)

    const val CHUNK_SIZE = 500
}
