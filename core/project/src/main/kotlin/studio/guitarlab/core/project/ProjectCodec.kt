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
    fun decode(serialized: String): GuitarProject = json.decodeFromString(serialized)
}
