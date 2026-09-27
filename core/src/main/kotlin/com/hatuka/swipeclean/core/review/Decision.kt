package com.hatuka.swipeclean.core.review

/**
 * What the user decided about one media item. Every state excludes the item from future decks,
 * so nothing the user already reviewed reappears unless they reset a folder on purpose.
 */
enum class DecisionState {
    /** Swiped right (or restored from the bin as "keep"). */
    KEEP,

    /** Swiped left: waits in the in-app bin; the file is untouched. */
    DELETE_PENDING,

    /** Swiped up: waits for the batched move; the file is untouched. */
    MOVE_PENDING,

    /** Move applied. */
    MOVED,

    /** Trashed/deleted through the system dialog. Kept as a tombstone so a file restored from
     *  the system trash (same MediaStore ID) is not shown again by surprise. */
    DELETED,
}
