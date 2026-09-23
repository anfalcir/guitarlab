package studio.guitarlab.platform.separation

import kotlinx.serialization.json.*
import studio.guitarlab.core.separation.*

object RemoteManifestCodec {
    fun decode(bytes: ByteArray): RemoteResultManifest {
        require(bytes.size in 2..65536)
        val root = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        val schemaVersion = root.reqInt("schemaVersion")
        val common = Common(
            jobId = root.reqString("jobId"),
            projectId = root.reqString("projectId"),
            inputSha256 = root.reqString("inputSha256"),
            engine = root.reqString("engine"),
            engineRevision = root.reqString("engineRevision"),
            model = root.reqString("model"),
            modelSha256 = root.reqString("modelSha256"),
            sampleRate = root.reqInt("sampleRate"),
            channels = root.reqInt("channels"),
            frames = root.reqLong("frames"),
            durationSeconds = root.req("duration").jsonPrimitive.double,
        )
        return when (schemaVersion) {
            1 -> {
                val stems = root.req("stems").jsonArray.map { element ->
                    val stem = element.jsonObject
                    RemoteStem(
                        stem.reqString("name"),
                        stem.reqString("path"),
                        stem.reqLong("bytes"),
                        stem.reqString("sha256"),
                    )
                }
                common.manifest(schemaVersion = 1, stems = stems)
            }
            2 -> {
                val deliverables = root.req("deliverables").jsonArray.map { element ->
                    val artifact = element.jsonObject
                    RemoteReference(
                        name = artifact.reqString("name"),
                        role = artifact.reqString("role"),
                        path = artifact.reqString("path"),
                        bytes = artifact.reqLong("bytes"),
                        sha256 = artifact.reqString("sha256"),
                        sampleRate = artifact.reqInt("sampleRate"),
                        channels = artifact.reqInt("channels"),
                        frames = artifact.reqLong("frames"),
                        encoding = artifact.reqString("encoding"),
                    )
                }
                val recipeObject = root.req("referenceRecipe").jsonObject
                val recipe = RemoteReferenceRecipe(
                    version = recipeObject.reqString("version"),
                    targetPeakDbfs = recipeObject.req("targetPeakDbfs").jsonPrimitive.double,
                    sharedGainDb = recipeObject.req("sharedGainDb").jsonPrimitive.double,
                    backingStems = recipeObject.req("backingStems").jsonArray.map { it.jsonPrimitive.content },
                    guitarStem = recipeObject.reqString("guitarStem"),
                )
                common.manifest(
                    schemaVersion = 2,
                    deliverables = deliverables,
                    referenceRecipe = recipe,
                )
            }
            else -> error("unsupported remote result schema: $schemaVersion")
        }
    }

    private data class Common(
        val jobId: String,
        val projectId: String,
        val inputSha256: String,
        val engine: String,
        val engineRevision: String,
        val model: String,
        val modelSha256: String,
        val sampleRate: Int,
        val channels: Int,
        val frames: Long,
        val durationSeconds: Double,
    ) {
        fun manifest(
            schemaVersion: Int,
            stems: List<RemoteStem> = emptyList(),
            deliverables: List<RemoteReference> = emptyList(),
            referenceRecipe: RemoteReferenceRecipe? = null,
        ) = RemoteResultManifest(
            jobId = jobId,
            projectId = projectId,
            inputSha256 = inputSha256,
            engine = engine,
            engineRevision = engineRevision,
            model = model,
            modelSha256 = modelSha256,
            sampleRate = sampleRate,
            channels = channels,
            frames = frames,
            durationSeconds = durationSeconds,
            stems = stems,
            schemaVersion = schemaVersion,
            deliverables = deliverables,
            referenceRecipe = referenceRecipe,
        )
    }

    private fun JsonObject.req(key: String) = requireNotNull(get(key)) { "missing $key" }
    private fun JsonObject.reqString(key: String) = req(key).jsonPrimitive.content
    private fun JsonObject.reqInt(key: String) = req(key).jsonPrimitive.int
    private fun JsonObject.reqLong(key: String) = req(key).jsonPrimitive.long
}
