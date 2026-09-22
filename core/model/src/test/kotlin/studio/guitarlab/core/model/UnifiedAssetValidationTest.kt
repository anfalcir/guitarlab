package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

class UnifiedAssetValidationTest {
    @Test fun rejectsUnsafeAssetPathAndInvalidHash() {
        val asset = ManagedAsset(
            assetId = "asset",
            role = AssetRole.SOURCE_ORIGINAL,
            relativePath = "../escape.wav",
            sha256 = "not-a-hash",
            byteSize = 1,
            format = "wav",
            createdAtEpochMs = 1,
            classification = AssetClassification.AUTHORITATIVE,
        )
        val project = GuitarProject(2, "p", "P", ProjectTemplate.BLANK, 1, 1, assets = listOf(asset))
        val codes = ProjectValidator.validate(project).map { it.code }.toSet()
        assertTrue("asset.path.invalid" in codes)
        assertTrue("asset.sha256.invalid" in codes)
    }
}
