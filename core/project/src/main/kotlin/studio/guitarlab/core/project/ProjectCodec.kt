package studio.guitarlab.core.project

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import studio.guitarlab.core.model.GuitarProject

class ProjectCodec(
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    fun encode(project: GuitarProject): String = json.encodeToString(project)

    /**
     * Decodes the persisted logical state using the pre-RC21 compatibility rules.
     *
     * Integrity checks for Drive manifests must run against this state before any new compatibility
     * repair is applied, otherwise a safe migration could be mistaken for remote corruption.
     */
    fun decodePersistedState(serialized: String): GuitarProject =
        UnifiedProjectMigrator.upgrade(
            TakeManagementPolicy.normalizeAll(json.decodeFromString(serialized)),
        )

    fun decode(serialized: String): GuitarProject =
        LegacyRecordingTakeRecoveryPolicy.recover(decodePersistedState(serialized))
}
