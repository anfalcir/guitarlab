package studio.guitarlab.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import studio.guitarlab.core.project.BackupCommitContract
import studio.guitarlab.core.project.BackupCommitRequest
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.ProjectBackupRemoteStore

class SafBackupRemoteStore(
    context: Context,
    private val treeUri: Uri,
) : ProjectBackupRemoteStore {
    private val resolver = context.applicationContext.contentResolver
    private val root: Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    override suspend fun listCommittedVersions(projectId: String?): List<BackupVersionDescriptor> = withContext(Dispatchers.IO) {
        ensurePermission()
        scanCommittedVersions(projectId)
    }

    override suspend fun findCommittedVersion(projectId: String, revisionId: String): BackupVersionDescriptor? = withContext(Dispatchers.IO) {
        ensurePermission()
        repeat(FIND_REVISION_ATTEMPTS) { attempt ->
            scanCommittedVersions(projectId).firstOrNull { it.revisionId == revisionId }?.let { return@withContext it }
            if (attempt < FIND_REVISION_ATTEMPTS - 1) delay(FIND_REVISION_DELAYS_MS[attempt])
        }
        null
    }

    override suspend fun commit(request: BackupCommitRequest): BackupVersionDescriptor = withContext(Dispatchers.IO) {
        ensurePermission()
        val projectDirectory = ensureDirectory(root, projectDirectoryName(request.projectId))
        val versionDirectory = ensureDirectory(projectDirectory.uri, versionDirectoryName(request.revisionId))

        // Deterministic revision folders make retrying idempotent. If the same revision already
        // became visible, return it instead of creating another physical copy.
        readCommittedVersion(versionDirectory, request.projectId)?.let { existing ->
            if (
                existing.revisionId == request.revisionId &&
                existing.sizeBytes == request.sizeBytes &&
                existing.sha256.equals(request.sha256, ignoreCase = true)
            ) return@withContext existing
        }

        val metadata = BackupCommitContract.metadata(request)
        try {
            val metadataUri = upsertFile(versionDirectory.uri, METADATA_NAME, MIME_TEXT)
            writeProperties(metadataUri, metadata, "O provedor não permitiu gravar os metadados do backup.")

            val packageUri = upsertFile(versionDirectory.uri, PACKAGE_NAME, MIME_BINARY)
            resolver.openOutputStream(packageUri, "w")?.buffered()?.use { output ->
                request.packageFile.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: error("O provedor não permitiu gravar o pacote GuitarLab.")

            // The package URI returned by create/open is authoritative even if the provider has not
            // refreshed its directory listing yet. Verify the actual remote bytes through that URI.
            val remoteDigest = digest(packageUri)
            require(remoteDigest.sizeBytes == request.sizeBytes) { "Upload incompleto: tamanho remoto divergente." }
            require(remoteDigest.sha256.equals(request.sha256, ignoreCase = true)) { "O arquivo enviado não passou na verificação de integridade." }

            val commitProperties = BackupCommitContract.commitMarker(request)
            val commitUri = upsertFile(versionDirectory.uri, COMMIT_NAME, MIME_TEXT)
            writeProperties(commitUri, commitProperties, "O provedor não permitiu confirmar o backup.")

            // Do not require an immediately refreshed directory listing here. Cloud-backed SAF
            // providers can be eventually consistent even after the bytes are durable. Re-read the
            // exact URIs just written and validate the complete commit contract directly.
            val committed = BackupCommitContract.parseCommittedOrNull(
                remoteId = versionDirectory.documentId,
                metadata = readProperties(metadataUri),
                commit = readProperties(commitUri),
                observedPackageSize = remoteDigest.sizeBytes,
                expectedProjectId = request.projectId,
            ) ?: error("O backup foi gravado, mas a confirmação de integridade não pôde ser validada.")
            require(
                committed.revisionId == request.revisionId &&
                    committed.sizeBytes == request.sizeBytes &&
                    committed.sha256.equals(request.sha256, true)
            ) { "A confirmação do backup não corresponde ao arquivo enviado." }
            committed
        } catch (error: Throwable) {
            // Keep incomplete data for forensic/retry safety. It is invisible to restore and is
            // removed only after the grace period by cleanupIncomplete.
            throw error
        }
    }

    override suspend fun copyPackage(version: BackupVersionDescriptor, destination: File): Unit = withContext(Dispatchers.IO) {
        ensurePermission()
        val versionUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, version.remoteId)
        val packageNode = child(versionUri, PACKAGE_NAME) ?: error("Pacote do backup não encontrado.")
        resolver.openInputStream(packageNode.uri)?.buffered()?.use { input ->
            destination.outputStream().buffered().use { output -> input.copyTo(output) }
        } ?: error("O Android não conseguiu abrir o backup remoto.")
        Unit
    }

    override suspend fun deleteVersion(version: BackupVersionDescriptor): Boolean = withContext(Dispatchers.IO) {
        ensurePermission()
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, version.remoteId)
        DocumentsContract.deleteDocument(resolver, uri)
    }

    override suspend fun cleanupIncomplete(olderThanEpochMs: Long): Int = withContext(Dispatchers.IO) {
        ensurePermission()
        var deleted = 0
        children(root).filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }.forEach { projectDirectory ->
            children(projectDirectory.uri)
                .filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR && it.displayName.startsWith(VERSION_PREFIX) }
                .forEach candidateLoop@ { candidate ->
                    if (readCommittedVersion(candidate, expectedProjectId = null) != null) return@candidateLoop
                    val metadataCreatedAt = child(candidate.uri, METADATA_NAME)?.let { metadataNode ->
                        runCatching { readProperties(metadataNode.uri).getProperty("backupCreatedAtEpochMs")?.toLongOrNull() }.getOrNull()
                    }
                    val legacyEncodedCreatedAt = candidate.displayName.removePrefix(VERSION_PREFIX).substringBefore('_').toLongOrNull()
                    val ageReference = candidate.lastModified.takeIf { it > 0L }
                        ?: metadataCreatedAt
                        ?: legacyEncodedCreatedAt
                        ?: return@candidateLoop
                    if (ageReference < olderThanEpochMs && DocumentsContract.deleteDocument(resolver, candidate.uri)) deleted++
                }
        }
        deleted
    }

    suspend fun probeReadWriteDelete(): String = withContext(Dispatchers.IO) {
        ensurePermission()
        val folder = createDirectory(root, "_guitarlab_probe_${UUID.randomUUID()}")
        var removed = false
        try {
            val token = UUID.randomUUID().toString()
            val file = createFile(folder.uri, "probe.txt", MIME_TEXT)
            resolver.openOutputStream(file, "w")?.bufferedWriter()?.use { it.write(token) }
                ?: error("A pasta selecionada não aceita escrita.")
            val readBack = resolver.openInputStream(file)?.bufferedReader()?.use { it.readText() }
                ?: error("A pasta selecionada não permite leitura.")
            require(readBack == token) { "A verificação de leitura/escrita da pasta falhou." }
            removed = DocumentsContract.deleteDocument(resolver, folder.uri)
            require(removed) { "A pasta selecionada não permite excluir arquivos de teste com segurança." }
            document(root)?.displayName ?: treeUri.lastPathSegment ?: "Pasta selecionada"
        } finally {
            if (!removed) runCatching { DocumentsContract.deleteDocument(resolver, folder.uri) }
        }
    }

    fun hasPersistedReadWritePermission(): Boolean = resolver.persistedUriPermissions.any { permission ->
        permission.uri == treeUri && permission.isReadPermission && permission.isWritePermission
    }

    private fun ensurePermission() {
        require(hasPersistedReadWritePermission()) { "A permissão da pasta de backup foi revogada. Selecione a pasta novamente." }
    }

    private fun scanCommittedVersions(projectId: String?): List<BackupVersionDescriptor> {
        val expectedDirectoryName = projectId?.let(::projectDirectoryName)
        val projectDirectories = children(root).filter { node ->
            node.mimeType == DocumentsContract.Document.MIME_TYPE_DIR &&
                (expectedDirectoryName == null || node.displayName == expectedDirectoryName)
        }
        return projectDirectories.flatMap { projectDirectory ->
            children(projectDirectory.uri)
                .filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR && it.displayName.startsWith(VERSION_PREFIX) }
                .mapNotNull { versionDirectory -> readCommittedVersion(versionDirectory, projectId) }
        }.sortedWith(
            compareByDescending<BackupVersionDescriptor> { it.projectUpdatedAtEpochMs }
                .thenByDescending { it.backupCreatedAtEpochMs }
                .thenByDescending { it.remoteId },
        )
    }

    private fun readCommittedVersion(node: DocNode, expectedProjectId: String?): BackupVersionDescriptor? = runCatching {
        val metadataNode = child(node.uri, METADATA_NAME) ?: return null
        val commitNode = child(node.uri, COMMIT_NAME) ?: return null
        val packageNode = child(node.uri, PACKAGE_NAME) ?: return null
        val metadata = readProperties(metadataNode.uri)
        val commit = readProperties(commitNode.uri)
        BackupCommitContract.parseCommittedOrNull(
            remoteId = node.documentId,
            metadata = metadata,
            commit = commit,
            observedPackageSize = packageNode.size.takeIf { it > 0L },
            expectedProjectId = expectedProjectId,
        ) ?: return null
    }.getOrNull()

    private fun writeProperties(uri: Uri, values: Properties, errorMessage: String) {
        resolver.openOutputStream(uri, "w")?.buffered()?.use { values.store(it, null) } ?: error(errorMessage)
    }

    private fun readProperties(uri: Uri): Properties = Properties().also { values ->
        resolver.openInputStream(uri)?.buffered()?.use(values::load) ?: error("Arquivo de metadados remoto indisponível.")
    }

    private data class Digest(val sizeBytes: Long, val sha256: String)
    private fun digest(uri: Uri): Digest {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        resolver.openInputStream(uri)?.buffered()?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) {
                    size += read
                    messageDigest.update(buffer, 0, read)
                }
            }
        } ?: error("O provedor não permitiu verificar o arquivo enviado.")
        return Digest(size, messageDigest.digest().joinToString("") { "%02x".format(it) })
    }

    private data class DocNode(
        val documentId: String,
        val uri: Uri,
        val displayName: String,
        val mimeType: String,
        val lastModified: Long,
        val size: Long,
    )

    private fun document(uri: Uri): DocNode? = resolver.query(uri, COLUMNS, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) null else cursorNode(cursor, uri)
    }

    private fun children(parent: Uri): List<DocNode> {
        val parentId = DocumentsContract.getDocumentId(parent)
        val queryUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        return resolver.query(queryUri, COLUMNS, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    add(cursorNode(cursor, DocumentsContract.buildDocumentUriUsingTree(treeUri, id)))
                }
            }
        }.orEmpty()
    }

    private fun cursorNode(cursor: android.database.Cursor, uri: Uri) = DocNode(
        documentId = cursor.getString(0),
        uri = uri,
        displayName = cursor.getString(1).orEmpty(),
        mimeType = cursor.getString(2).orEmpty(),
        lastModified = if (cursor.isNull(3)) 0L else cursor.getLong(3),
        size = if (cursor.isNull(4)) -1L else cursor.getLong(4),
    )

    private fun child(parent: Uri, name: String): DocNode? = children(parent).firstOrNull { it.displayName == name }

    private fun ensureDirectory(parent: Uri, name: String): DocNode = child(parent, name)?.also {
        require(it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) { "Conflito de nome na pasta de backup: $name" }
    } ?: createDirectory(parent, name)

    private fun createDirectory(parent: Uri, name: String): DocNode {
        val uri = DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: error("Não foi possível criar a pasta de backup '$name'.")
        // The URI returned by createDocument is authoritative. Some cloud providers can delay a
        // subsequent metadata query, so use a safe fallback instead of reporting a false failure.
        return document(uri) ?: DocNode(
            documentId = DocumentsContract.getDocumentId(uri),
            uri = uri,
            displayName = name,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            lastModified = 0L,
            size = -1L,
        )
    }

    private fun upsertFile(parent: Uri, name: String, mime: String): Uri =
        child(parent, name)?.uri ?: createFile(parent, name, mime)

    private fun createFile(parent: Uri, name: String, mime: String): Uri =
        DocumentsContract.createDocument(resolver, parent, mime, name) ?: error("Não foi possível criar '$name' no backup.")

    private fun projectDirectoryName(projectId: String): String =
        "project_" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(projectId.toByteArray(Charsets.UTF_8))

    private fun versionDirectoryName(revisionId: String): String = "$VERSION_PREFIX$revisionId"

    companion object {
        private const val VERSION_PREFIX = "v_"
        private const val METADATA_NAME = "metadata.properties"
        private const val PACKAGE_NAME = "project.guitarlab"
        private const val COMMIT_NAME = "COMMITTED"
        private const val MIME_TEXT = "text/plain"
        private const val MIME_BINARY = "application/octet-stream"
        private const val FIND_REVISION_ATTEMPTS = 4
        private val FIND_REVISION_DELAYS_MS = longArrayOf(150L, 350L, 750L)
        private val COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        suspend fun adoptAndProbe(context: Context, uri: Uri): String {
            val resolver = context.contentResolver
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val alreadyPersisted = resolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission && permission.isWritePermission
            }
            if (!alreadyPersisted) resolver.takePersistableUriPermission(uri, flags)
            try {
                return SafBackupRemoteStore(context, uri).probeReadWriteDelete()
            } catch (error: Throwable) {
                if (!alreadyPersisted) runCatching { resolver.releasePersistableUriPermission(uri, flags) }
                throw error
            }
        }

        fun releasePersistedPermission(context: Context, uri: Uri) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, flags) }
        }
    }
}
