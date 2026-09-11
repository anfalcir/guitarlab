package studio.guitarlab.app.io

import java.io.File

/** Removes only abandoned, regenerable files created directly in this app's cache directory. */
object AppCacheTemporaryCleaner {
    private val knownPrefixes = listOf(
        "guitarlab-import-",
        "guitarlab-aiff-",
        "guitarlab-resample-",
        "guitarlab-L-",
        "guitarlab-R-",
        "guitarlab-project-",
        "guitarlab-home-master-",
    )

    fun isEligible(cacheDir: File, candidate: File): Boolean {
        val canonicalCache = runCatching { cacheDir.canonicalFile }.getOrNull() ?: return false
        val canonicalCandidate = runCatching { candidate.canonicalFile }.getOrNull() ?: return false
        return canonicalCandidate.parentFile == canonicalCache &&
            canonicalCandidate.isFile &&
            knownPrefixes.any(canonicalCandidate.name::startsWith)
    }

    fun clean(cacheDir: File): Int = cacheDir.listFiles().orEmpty().count { candidate ->
        isEligible(cacheDir, candidate) && runCatching { candidate.delete() }.getOrDefault(false)
    }
}
