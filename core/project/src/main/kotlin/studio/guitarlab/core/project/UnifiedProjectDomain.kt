package studio.guitarlab.core.project

import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.CURRENT_PROJECT_SCHEMA_VERSION
import studio.guitarlab.core.model.GuitarProject

object UnifiedProjectMigrator {
    fun upgrade(project: GuitarProject): GuitarProject = when (project.schemaVersion) {
        CURRENT_PROJECT_SCHEMA_VERSION -> project
        1 -> project.copy(schemaVersion = CURRENT_PROJECT_SCHEMA_VERSION)
        else -> error("Schema de projeto não suportado: ${project.schemaVersion}")
    }
}

object UnifiedProjectRevision {
    private val canonicalJson = Json {
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }

    fun canonicalState(project: GuitarProject): String {
        val normalized = project.copy(
            assets = project.assets.sortedBy { it.assetId }.map { asset ->
                asset.copy(
                    provenance = asset.provenance?.let { provenance ->
                        provenance.copy(
                            inputAssetIds = provenance.inputAssetIds.sorted(),
                            inputSha256 = provenance.inputSha256.sorted(),
                            parameters = provenance.parameters.toSortedMap(),
                        )
                    }
                )
            },
            referenceBindings = project.referenceBindings.sortedBy { it.bindingId },
            preparation = project.preparation?.let { preparation ->
                preparation.copy(
                    activeStemAssetIds = preparation.activeStemAssetIds.toSortedMap(compareBy<AssetRole> { it.name }),
                    availableReferenceAssetIds = preparation.availableReferenceAssetIds.sorted(),
                )
            },
        )
        return canonicalJson.encodeToString(normalized)
    }

    fun sha256(project: GuitarProject): String = MessageDigest.getInstance("SHA-256")
        .digest(canonicalState(project).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

object ProjectAssetReachability {
    fun reachableAssetIds(project: GuitarProject): Set<String> {
        val byId = project.assets.associateBy { it.assetId }
        val roots = buildSet {
            project.assets.filter { it.classification == AssetClassification.AUTHORITATIVE }.forEach { add(it.assetId) }
            project.preparation?.let { state ->
                state.sourceAssetId?.let(::add)
                addAll(state.activeStemAssetIds.values)
                state.activeBackingAssetId?.let(::add)
                state.activeGuitarAssetId?.let(::add)
                addAll(state.availableReferenceAssetIds)
            }
            project.referenceBindings.forEach { add(it.assetId) }
        }
        val reachable = roots.toMutableSet()
        val queue = ArrayDeque(roots)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            byId[id]?.provenance?.inputAssetIds.orEmpty().forEach { parent ->
                if (reachable.add(parent)) queue.addLast(parent)
            }
        }
        return reachable
    }

    fun cleanupCandidates(project: GuitarProject): Set<String> =
        project.assets.mapTo(mutableSetOf()) { it.assetId } - reachableAssetIds(project)
}
