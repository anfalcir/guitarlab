package studio.guitarlab.core.project

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Maps logical IDs to one confined, collision-resistant path segment. */
internal object ManagedStorageKey {
    private val safe = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}")

    fun from(id: String): String {
        require(id.isNotBlank()) { "Managed identifier must not be blank." }
        if (safe.matches(id) && id != "." && id != "..") return id
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "id-$digest"
    }
}
