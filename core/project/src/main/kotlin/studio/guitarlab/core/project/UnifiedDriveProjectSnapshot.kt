package studio.guitarlab.core.project

import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import studio.guitarlab.core.model.GuitarProject

data class FrozenUnifiedDriveProjectSnapshot(
    val manifest: DriveProjectRevisionManifest,
    val localAssets: List<DriveLocalAsset>,
    private val stagingDirectory: File,
) : Closeable {
    override fun close() {
        stagingDirectory.deleteRecursively()
    }
}

/**
 * Freezes one persisted project into immutable content-addressed objects without audio conversion.
 *
 * project.json is its own object. Every referenced managed media file is copied byte-for-byte into
 * staging and deduplicated by SHA-256. The manifest keeps the path mapping while the remote object
 * identity remains content-only, so metadata edits never force media re-upload.
 */
class UnifiedDriveProjectSnapshotBuilder(
    private val rootDirectory: File,
    private val codec: ProjectCodec = ProjectCodec(),
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    fun freeze(project: GuitarProject, confirmedRevisionId: String?): FrozenUnifiedDriveProjectSnapshot {
        val revisionId = BackupRevisionIdentity.forProject(project)
        require(confirmedRevisionId != revisionId) {
            "A project already confirmed at this revision does not require a new snapshot."
        }

        val projectDirectory = File(File(rootDirectory, "projects"), ManagedStorageKey.from(project.id)).canonicalFile
        require(projectDirectory.isDirectory) { "Project directory is missing." }

        val stagingRoot = File(rootDirectory, "backup-staging").also { it.mkdirs() }
        val staging = File(stagingRoot, "u8-snapshot-${UUID.randomUUID()}")
        require(staging.mkdirs()) { "Could not create Drive snapshot staging." }
        val objectsDirectory = File(staging, "objects").also { require(it.mkdirs()) }

        try {
            val stagedByHash = linkedMapOf<String, DriveLocalAsset>()
            val stateFile = File(staging, "project.json")
            stateFile.writeText(codec.encode(project), Charsets.UTF_8)
            val stateAsset = stageObject(stateFile, objectsDirectory, stagedByHash)

            val trackedByPath = project.assets.associateBy { normalize(it.relativePath) }
            val entries = referencedPaths(project).map { relativePath ->
                val source = confinedProjectFile(projectDirectory, relativePath)
                val tracked = trackedByPath[relativePath]
                if (tracked != null) {
                    require(source.length() == tracked.byteSize) { "Managed asset size drift: $relativePath" }
                    require(BackupHashing.sha256(source).equals(tracked.sha256, true)) {
                        "Managed asset hash drift: $relativePath"
                    }
                }
                val temp = File(staging, "copy-${UUID.randomUUID()}.bin")
                source.inputStream().buffered().use { input ->
                    temp.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                require(temp.length() == source.length() && temp.length() > 0L) {
                    "Referenced media changed while snapshotting: $relativePath"
                }
                val staged = stageObject(temp, objectsDirectory, stagedByHash)
                require(BackupHashing.sha256(source) == staged.identity.sha256) {
                    "Referenced media changed while snapshotting: $relativePath"
                }
                DriveProjectFileEntry(relativePath, staged.identity)
            }

            val assets = stagedByHash.values.map { it.identity }
            val manifest = DriveProjectRevisionManifest(
                projectId = project.id,
                revisionId = revisionId,
                baseRevisionId = confirmedRevisionId,
                createdAtEpochMs = nowEpochMs(),
                canonicalProjectStateSha256 = UnifiedProjectRevision.sha256(project),
                assets = assets,
                projectStateAsset = stateAsset.identity,
                fileEntries = entries,
            )
            return FrozenUnifiedDriveProjectSnapshot(manifest, stagedByHash.values.toList(), staging)
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun stageObject(
        source: File,
        objectsDirectory: File,
        stagedByHash: MutableMap<String, DriveLocalAsset>,
    ): DriveLocalAsset {
        require(source.isFile && source.length() > 0L) { "Drive snapshot object is empty." }
        val identity = DriveAssetObject(BackupHashing.sha256(source), source.length())
        stagedByHash[identity.sha256]?.let { existing ->
            require(existing.identity == identity) { "SHA-256 collision with different size." }
            if (source.parentFile != objectsDirectory) source.delete()
            return existing
        }
        val destination = File(objectsDirectory, identity.sha256)
        if (source != destination) {
            try {
                Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return DriveLocalAsset(identity, destination).also { stagedByHash[identity.sha256] = it }
    }

    private fun referencedPaths(project: GuitarProject): List<String> = (
        project.assets.map { it.relativePath } +
            project.clips.flatMap { listOfNotNull(it.managedSourcePath, it.managedEditProxyPath) }
        )
        .map(::normalize)
        .distinct()
        .sorted()

    private fun confinedProjectFile(projectDirectory: File, relativePath: String): File {
        DriveProjectFileEntry(relativePath, DriveAssetObject("0".repeat(64), 1))
        val candidate = File(projectDirectory, relativePath).canonicalFile
        require(candidate.path.startsWith(projectDirectory.path + File.separator) && candidate.isFile) {
            "Referenced project media is missing: $relativePath"
        }
        return candidate
    }

    private fun normalize(path: String): String {
        val portable = path.replace('\\', '/')
        require(portable == path) { "Project media path is not canonical." }
        return portable
    }
}
