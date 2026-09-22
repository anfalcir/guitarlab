package studio.guitarlab.core.project

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.CURRENT_PROJECT_SCHEMA_VERSION
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.ProjectValidator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class UnifiedProjectDomainTest {
    private fun project(assets: List<ManagedAsset> = emptyList(), preparation: PreparationState? = null) = GuitarProject(
        id = "project-id",
        name = "Unified",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 2,
        assets = assets,
        preparation = preparation,
    )

    private fun asset(
        id: String,
        role: AssetRole,
        classification: AssetClassification,
        parents: List<String> = emptyList(),
    ) = ManagedAsset(
        assetId = id,
        role = role,
        relativePath = "media/assets/$id.wav",
        sha256 = java.security.MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) },
        byteSize = 128,
        format = "wav-f32",
        sampleRateHz = 48_000,
        channelCount = 2,
        frameCount = 48_000,
        createdAtEpochMs = 1,
        classification = classification,
        lifecycle = AssetLifecycle.MANAGED,
        provenance = parents.takeIf { it.isNotEmpty() }?.let { AssetProvenance("derived", inputAssetIds = it) },
    )

    @Test fun legacySchemaUpgradesWithoutPreparation() {
        val decoded = ProjectCodec().decode("""{"schemaVersion":1,"id":"legacy","name":"Legacy","template":"BLANK","createdAtEpochMs":1,"updatedAtEpochMs":1}""")
        assertEquals(CURRENT_PROJECT_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(null, decoded.preparation)
        assertTrue(decoded.assets.isEmpty())
    }

    @Test fun canonicalRevisionIgnoresAssetInventoryOrdering() {
        val source = asset("source", AssetRole.SOURCE_ORIGINAL, AssetClassification.AUTHORITATIVE)
        val stem = asset("stem", AssetRole.STEM_GUITAR, AssetClassification.DERIVED, listOf(source.assetId))
        assertEquals(UnifiedProjectRevision.sha256(project(listOf(source, stem))), UnifiedProjectRevision.sha256(project(listOf(stem, source))))
        assertNotEquals(UnifiedProjectRevision.sha256(project(listOf(source))), UnifiedProjectRevision.sha256(project(listOf(source, stem))))
    }

    @Test fun renameChangesRevisionButNeverIdentity() {
        val original = project()
        val renamed = original.copy(name = "Renamed", updatedAtEpochMs = 3)
        assertEquals(original.id, renamed.id)
        assertNotEquals(UnifiedProjectRevision.sha256(original), UnifiedProjectRevision.sha256(renamed))
    }

    @Test fun reachabilityKeepsAuthoritativeAndDerivedParents() {
        val source = asset("source", AssetRole.SOURCE_ORIGINAL, AssetClassification.AUTHORITATIVE)
        val backing = asset("backing", AssetRole.REFERENCE_BACKING, AssetClassification.DERIVED, listOf(source.assetId))
        val orphan = asset("orphan", AssetRole.PRACTICE_EXPORT, AssetClassification.DERIVED)
        val state = PreparationState(PreparationStatus.READY, source.assetId, activeBackingAssetId = backing.assetId)
        val candidate = project(listOf(source, backing, orphan), state)
        assertEquals(setOf("source", "backing"), ProjectAssetReachability.reachableAssetIds(candidate))
        assertEquals(setOf("orphan"), ProjectAssetReachability.cleanupCandidates(candidate))
    }

    @Test fun validatorRejectsDanglingAndStagingAssets() {
        val staging = asset("staging", AssetRole.SOURCE_ORIGINAL, AssetClassification.AUTHORITATIVE).copy(lifecycle = AssetLifecycle.STAGING)
        val candidate = project(listOf(staging), PreparationState(sourceAssetId = "missing"))
        val codes = ProjectValidator.validate(candidate).map { it.code }.toSet()
        assertTrue("asset.lifecycle.staging" in codes)
        assertTrue("preparation.asset.missing" in codes)
    }

    @Test fun randomizedInventoryOrderHasStableRevision() {
        val base = (0 until 32).map { index ->
            asset("asset-$index", if (index == 0) AssetRole.SOURCE_ORIGINAL else AssetRole.STEM_OTHER, if (index == 0) AssetClassification.AUTHORITATIVE else AssetClassification.DERIVED)
        }
        val expected = UnifiedProjectRevision.sha256(project(base))
        repeat(100) { seed ->
            assertEquals(expected, UnifiedProjectRevision.sha256(project(base.shuffled(Random(seed)))), "seed=$seed")
        }
        assertFalse(expected.isBlank())
    }

    @Test fun portablePackageRoundTripsUnifiedAssetsWithoutMutatingOriginalIdentity() {
        val root = Files.createTempDirectory("u1-portable").toFile()
        try {
            val source = asset("source", AssetRole.SOURCE_ORIGINAL, AssetClassification.AUTHORITATIVE)
            val original = project(listOf(source), PreparationState(PreparationStatus.SOURCE_READY, source.assetId))
            val projectDirectory = File(root, "source-project").also { it.mkdirs() }
            File(projectDirectory, source.relativePath).apply { parentFile.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
            val bytes = ByteArrayOutputStream().also { ProjectBundleWriter().write(original, projectDirectory, it) }.toByteArray()
            val restored = ProjectBundleReader(root).read(ByteArrayInputStream(bytes), nowEpochMs = 10)
            assertNotEquals(original.id, restored.id)
            assertEquals(original.assets, restored.assets)
            assertEquals(original.preparation, restored.preparation)
        } finally {
            root.deleteRecursively()
        }
    }
}
