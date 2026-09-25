package studio.guitarlab.core.project

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectValidator

data class PersistedUnifiedDriveProjectSnapshot(
    val manifest: DriveProjectRevisionManifest,
    val localAssets: List<DriveLocalAsset>,
)

class FileUnifiedDriveSnapshotStore(
    private val directory: File,
) {
    fun persist(snapshot: FrozenUnifiedDriveProjectSnapshot): PersistedUnifiedDriveProjectSnapshot {
        directory.mkdirs()
        val projectDirectory = projectDirectory(snapshot.manifest.projectId)
        if (projectDirectory.exists()) {
            val existing = load(snapshot.manifest.projectId)
                ?: error("Existing Drive snapshot staging is unreadable.")
            require(
                existing.manifest.revisionId == snapshot.manifest.revisionId &&
                    existing.manifest.manifestSha256 == snapshot.manifest.manifestSha256
            ) { "A different frozen Drive snapshot is already pending for this project." }
            return existing
        }

        val temporary = File(
            directory,
            ".${ManagedStorageKey.from(snapshot.manifest.projectId)}-${UUID.randomUUID()}.tmp",
        )
        require(temporary.mkdirs()) { "Could not create durable Drive snapshot staging." }
        try {
            val objects = File(temporary, OBJECTS_DIRECTORY).also { require(it.mkdirs()) }
            File(temporary, MANIFEST_FILE).writeBytes(snapshot.manifest.canonicalBytes())

            val byHash = snapshot.localAssets.associateBy { it.identity.sha256 }
            require(byHash.size == snapshot.localAssets.size) {
                "Frozen snapshot contains duplicate local objects."
            }
            require(snapshot.manifest.assets.map { it.sha256 }.toSet() == byHash.keys) {
                "Frozen snapshot object set does not match its manifest."
            }
            snapshot.manifest.assets.forEach { identity ->
                val local = requireNotNull(byHash[identity.sha256]) {
                    "Frozen snapshot object is missing."
                }
                require(local.identity == identity) { "Frozen snapshot object identity mismatch." }
                val destination = File(objects, identity.sha256)
                local.file.copyTo(destination, overwrite = false)
                verifyObject(destination, identity)
            }

            loadFromDirectory(temporary, snapshot.manifest.projectId)
            moveDirectory(temporary, projectDirectory)
            return requireNotNull(load(snapshot.manifest.projectId))
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    fun load(projectId: String): PersistedUnifiedDriveProjectSnapshot? {
        val projectDirectory = projectDirectory(projectId)
        if (!projectDirectory.isDirectory) return null
        return loadFromDirectory(projectDirectory, projectId)
    }

    fun pendingAssetHashes(): Set<String> = directory
        .listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
        .orEmpty()
        .flatMapTo(mutableSetOf()) { projectDirectory ->
            loadFromDirectory(projectDirectory, null).manifest.assets.map { it.sha256 }
        }

    fun remove(projectId: String) {
        projectDirectory(projectId).deleteRecursively()
    }

    fun clearAll() {
        directory.deleteRecursively()
    }

    fun cleanupTemporaryDirectories(): Int {
        var removed = 0
        directory.listFiles { file -> file.isDirectory && file.name.startsWith(".") }
            .orEmpty()
            .forEach { if (it.deleteRecursively()) removed++ }
        return removed
    }

    private fun loadFromDirectory(
        projectDirectory: File,
        expectedProjectId: String?,
    ): PersistedUnifiedDriveProjectSnapshot {
        val manifestFile = File(projectDirectory, MANIFEST_FILE)
        require(manifestFile.isFile) { "Durable Drive snapshot manifest is missing." }
        val manifest = DriveProjectRevisionManifest.parseCanonical(manifestFile.readBytes())
        if (expectedProjectId != null) {
            require(manifest.projectId == expectedProjectId) {
                "Durable Drive snapshot belongs to another project."
            }
        }
        val objects = File(projectDirectory, OBJECTS_DIRECTORY)
        require(objects.isDirectory) { "Durable Drive snapshot object directory is missing." }
        val localAssets = manifest.assets.map { identity ->
            val file = File(objects, identity.sha256).canonicalFile
            require(file.parentFile == objects.canonicalFile) {
                "Durable Drive snapshot object escaped staging."
            }
            verifyObject(file, identity)
            DriveLocalAsset(identity, file)
        }
        val extra = objects.listFiles().orEmpty()
            .filter { it.isFile }
            .map { it.name }
            .toSet() - manifest.assets.map { it.sha256 }.toSet()
        require(extra.isEmpty()) { "Durable Drive snapshot contains undeclared objects." }
        return PersistedUnifiedDriveProjectSnapshot(manifest, localAssets)
    }

    private fun verifyObject(file: File, identity: DriveAssetObject) {
        require(file.isFile && file.length() == identity.sizeBytes) {
            "Durable Drive snapshot object size mismatch."
        }
        require(BackupHashing.sha256(file) == identity.sha256) {
            "Durable Drive snapshot object hash mismatch."
        }
    }

    private fun projectDirectory(projectId: String): File =
        File(directory, ManagedStorageKey.from(projectId))

    private fun moveDirectory(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private companion object {
        const val MANIFEST_FILE = "manifest.v3"
        const val OBJECTS_DIRECTORY = "objects"
    }
}

class UnifiedDriveLocalRestoreValidator(
    private val codec: ProjectCodec = ProjectCodec(),
) : DriveRestoreValidator {
    override fun validate(stagingDirectory: File, plan: DriveRestorePlan) {
        require(stagingDirectory.isDirectory) { "Restore staging directory is missing." }
        require(stagingDirectory.walkTopDown().none { Files.isSymbolicLink(it.toPath()) }) {
            "Restore staging must not contain symbolic links."
        }

        val stateFile = File(stagingDirectory, PROJECT_FILE)
        require(stateFile.isFile) { "Restored project.json is missing." }
        val serialized = stateFile.readText(Charsets.UTF_8)
        val persistedProject = codec.decodePersistedState(serialized)
        require(persistedProject.id == plan.manifest.projectId) {
            "Restored project identity does not match the manifest."
        }
        require(UnifiedProjectRevision.sha256(persistedProject) == plan.manifest.canonicalProjectStateSha256) {
            "Restored project state digest does not match the manifest."
        }
        val project = codec.decode(serialized)
        val issues = ProjectValidator.validate(project)
        require(issues.isEmpty()) {
            "Restored project is invalid: ${issues.joinToString { it.code }}"
        }

        val entries = plan.manifest.fileEntries.associateBy { it.relativePath }
        require(entries.size == plan.manifest.fileEntries.size) {
            "Restore manifest contains duplicate paths."
        }
        require(project.assets.distinctBy { it.relativePath }.size == project.assets.size) {
            "Restored project contains duplicate managed media paths."
        }

        referencedPaths(project).forEach { relativePath ->
            val entry = requireNotNull(entries[relativePath]) {
                "Restored project references media absent from the Drive manifest: $relativePath"
            }
            val file = confined(stagingDirectory, relativePath)
            require(file.isFile) { "Restored project media is missing: $relativePath" }
            require(
                file.length() == entry.asset.sizeBytes &&
                    BackupHashing.sha256(file) == entry.asset.sha256
            ) { "Restored project media failed manifest verification: $relativePath" }
        }
        project.assets.forEach { asset ->
            val entry = requireNotNull(entries[asset.relativePath]) {
                "Restored managed asset is absent from the Drive manifest: ${asset.relativePath}"
            }
            require(
                entry.asset.sizeBytes == asset.byteSize &&
                    entry.asset.sha256 == asset.sha256
            ) { "Restored managed asset metadata is inconsistent: ${asset.relativePath}" }
        }

        val expectedFiles =
            (setOf(PROJECT_FILE) + plan.manifest.fileEntries.map { it.relativePath }).toSet()
        val actualFiles = stagingDirectory.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(stagingDirectory).path.replace(File.separatorChar, '/') }
            .toSet()
        require(actualFiles == expectedFiles) {
            "Restore staging contains missing or undeclared files."
        }
    }

    private fun referencedPaths(project: GuitarProject): Set<String> =
        (
            project.assets.map { it.relativePath } +
                project.clips.flatMap {
                    listOfNotNull(it.managedSourcePath, it.managedEditProxyPath)
                }
            ).toSet()

    private fun confined(root: File, relativePath: String): File {
        DriveProjectFileEntry(relativePath, DriveAssetObject("0".repeat(64), 1L))
        val canonicalRoot = root.canonicalFile
        val file = File(canonicalRoot, relativePath).canonicalFile
        require(file.path.startsWith(canonicalRoot.path + File.separator)) {
            "Restore path escaped staging."
        }
        return file
    }

    private companion object {
        const val PROJECT_FILE = "project.json"
    }
}

data class UnifiedDriveLocalPublication(
    val project: GuitarProject,
    val action: DriveConflictAction,
)

class UnifiedDriveLocalProjectPublisher(
    rootDirectory: File,
    private val codec: ProjectCodec = ProjectCodec(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val projectsDirectory = File(rootDirectory, "projects").also { it.mkdirs() }

    fun publish(
        stagingDirectory: File,
        plan: DriveRestorePlan,
        action: DriveConflictAction,
    ): UnifiedDriveLocalPublication {
        require(action != DriveConflictAction.KEEP_LOCAL) {
            "KEEP_LOCAL does not publish remote bytes."
        }
        val source = codec.decode(File(stagingDirectory, PROJECT_FILE).readText(Charsets.UTF_8))
        require(source.id == plan.descriptor.projectId) {
            "Restore publication project identity mismatch."
        }

        val target = when (action) {
            DriveConflictAction.USE_DRIVE -> source
            DriveConflictAction.IMPORT_AS_COPY -> ProjectLifecyclePolicy.duplicateSnapshot(
                source = source,
                newProjectId = nextAvailableProjectId(),
                newName = restoredCopyName(source.name),
                nowEpochMs = nowEpochMs(),
            )
            DriveConflictAction.KEEP_LOCAL -> error("unreachable")
        }
        val issues = ProjectValidator.validate(target)
        require(issues.isEmpty()) {
            "Restore publication would create an invalid project: ${issues.joinToString { it.code }}"
        }

        val destination = File(projectsDirectory, ManagedStorageKey.from(target.id))
        if (action == DriveConflictAction.IMPORT_AS_COPY) {
            require(!destination.exists()) { "Restore copy identity already exists locally." }
        }

        val publishDirectory = File(
            projectsDirectory,
            ".restore-publish-${UUID.randomUUID()}",
        )
        val previousDirectory = File(
            projectsDirectory,
            ".restore-previous-${UUID.randomUUID()}",
        )
        require(publishDirectory.mkdirs()) { "Could not create restore publication staging." }
        var publicationSucceeded = false
        try {
            copyTree(stagingDirectory, publishDirectory)
            if (target != source) {
                File(publishDirectory, PROJECT_FILE).writeText(
                    codec.encode(target),
                    Charsets.UTF_8,
                )
            }
            verifyPublishedTree(publishDirectory, target, plan)

            var previousMoved = false
            try {
                if (destination.exists()) {
                    moveDirectory(destination, previousDirectory)
                    previousMoved = true
                }
                moveDirectory(publishDirectory, destination)
            } catch (publicationError: Throwable) {
                if (destination.exists()) destination.deleteRecursively()
                if (previousMoved && previousDirectory.exists()) {
                    try {
                        moveDirectory(previousDirectory, destination)
                    } catch (rollbackError: Throwable) {
                        publicationError.addSuppressed(rollbackError)
                        throw publicationError
                    }
                }
                throw publicationError
            }
            publicationSucceeded = true
            previousDirectory.deleteRecursively()
            return UnifiedDriveLocalPublication(target, action)
        } finally {
            publishDirectory.deleteRecursively()
            if (publicationSucceeded) previousDirectory.deleteRecursively()
        }
    }

    private fun verifyPublishedTree(
        directory: File,
        project: GuitarProject,
        plan: DriveRestorePlan,
    ) {
        val decoded = codec.decode(File(directory, PROJECT_FILE).readText(Charsets.UTF_8))
        require(decoded == project) { "Restore publication project.json did not round-trip." }
        val issues = ProjectValidator.validate(decoded)
        require(issues.isEmpty()) { "Restore publication validation failed." }
        plan.manifest.fileEntries.forEach { entry ->
            val file = File(directory, entry.relativePath).canonicalFile
            require(file.path.startsWith(directory.canonicalPath + File.separator)) {
                "Published media escaped project root."
            }
            require(file.isFile && file.length() == entry.asset.sizeBytes) {
                "Published media size mismatch."
            }
            require(BackupHashing.sha256(file) == entry.asset.sha256) {
                "Published media hash mismatch."
            }
        }
    }

    private fun copyTree(source: File, destination: File) {
        source.walkTopDown().forEach { item ->
            require(!Files.isSymbolicLink(item.toPath())) {
                "Restore staging contains a symbolic link."
            }
            val relative = item.relativeTo(source).path
            if (relative.isEmpty()) return@forEach
            val target = File(destination, relative)
            if (item.isDirectory) {
                require(target.mkdirs() || target.isDirectory)
            } else {
                target.parentFile?.let { require(it.mkdirs() || it.isDirectory) }
                item.copyTo(target, overwrite = false)
            }
        }
    }

    private fun moveDirectory(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private fun nextAvailableProjectId(): String {
        repeat(32) {
            val id = idGenerator()
            require(id.isNotBlank()) { "Restore copy id generator returned a blank id." }
            if (!File(projectsDirectory, ManagedStorageKey.from(id)).exists()) return id
        }
        error("Could not allocate a unique local project identity.")
    }

    private fun restoredCopyName(name: String): String {
        val suffix = " (cópia)"
        val base = name.trim().take((24 - suffix.length).coerceAtLeast(1)).trimEnd()
        return (base.ifBlank { "Projeto" } + suffix).take(24)
    }

    private companion object {
        const val PROJECT_FILE = "project.json"
    }
}
