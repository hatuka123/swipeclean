package com.hatuka.swipeclean.core.move

import com.hatuka.swipeclean.core.media.MediaType

/**
 * Where MediaStore lets an app move media (RELATIVE_PATH). Images may live under DCIM/ or
 * Pictures/, videos under DCIM/, Movies/ or Pictures/. Paths always end with "/".
 */
object TargetPathRules {
    const val DEFAULT_TARGET = "Pictures/Found/"
    private const val NEW_FOLDER_PARENT = "Pictures"
    private const val MAX_NAME_LENGTH = 60
    private val FORBIDDEN_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001f]""")

    private val allowedRoots = mapOf(
        MediaType.IMAGE to setOf("DCIM", "Pictures"),
        MediaType.VIDEO to setOf("DCIM", "Movies", "Pictures"),
    )

    /** "Pictures//Found" → "Pictures/Found/"; null for empty paths or ones with "." / ".." segments. */
    fun normalize(path: String): String? {
        val segments = path.trim().split('/').map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty() || segments.any { it == "." || it == ".." }) return null
        return segments.joinToString("/", postfix = "/")
    }

    fun isValidFor(type: MediaType, path: String): Boolean {
        val normalized = normalize(path) ?: return false
        return normalized.substringBefore('/') in allowedRoots.getValue(type)
    }

    /** Works for both photos and videos. */
    fun isValidForAll(path: String): Boolean = MediaType.entries.all { isValidFor(it, path) }

    /** Path for a new folder the user named, e.g. "Trip 2024" → "Pictures/Trip 2024/". */
    fun newFolder(name: String): String? {
        val clean = name.replace(FORBIDDEN_CHARS, "").trim().trim('.').take(MAX_NAME_LENGTH).trim()
        if (clean.isEmpty()) return null
        return "$NEW_FOLDER_PARENT/$clean/"
    }

    /** Last path segment, for display ("Pictures/Found/" → "Found"). */
    fun displayName(path: String): String = normalize(path)?.trimEnd('/')?.substringAfterLast('/') ?: path

    fun sameFolder(a: String?, b: String?): Boolean =
        a != null && b != null && normalize(a).equals(normalize(b), ignoreCase = true)

    /** Folders offered in the picker for one item: valid for its type, excluding where it already is. */
    fun pickerOptions(existing: Collection<String>, type: MediaType, currentPath: String?): List<String> =
        existing.mapNotNull(::normalize)
            .distinctBy { it.lowercase() }
            .filter { isValidFor(type, it) && !sameFolder(it, currentPath) }
            .sortedBy { displayName(it).lowercase() }
}
