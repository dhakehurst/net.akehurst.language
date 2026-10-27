package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.kotlinx.filesystem.UserFileSystemDefault
import net.akehurst.kotlinx.filesystem.api.DirectoryHandle
import net.akehurst.kotlinx.filesystem.api.FileHandle
import net.akehurst.kotlinx.filesystem.api.FileSystemObjectHandle
import net.akehurst.kotlinx.text.toRegexFromGlob
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain

/**
 * Provenance of a DataItem read from a FilesystemDatastore.
 */
data class FilesystemIssueLocation(
    val path: String?
) : DataItemIssueLocation

data class FilesystemProvenance(
    val datastoreIdentity: DatastoreIdentity,
    val path: String
) : DataItemProvenance

class FilesystemDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration
) : DatastoreAbstract(identity, configuration) {

    companion object {

        /**
         * The content of a file. Setting 'asString' stages the new content in the datastore's transaction;
         * the file is written on commit. Reading 'asString' gives the staged content, if any, else the file's content.
         */
        class FileContent(val file: FileHandle, internal val datastore: FilesystemDatastore) {
            override fun hashCode(): Int = file.hashCode()
            override fun equals(other: Any?): Boolean = other is FileContent && other.file == this.file && other.datastore === this.datastore
            override fun toString(): String = "FileContent(${file.path})"
        }

        class FilesystemConfig(val configuration: DatastoreConfiguration) {
            suspend fun fsRoot(): DirectoryHandle {
                val rootPathStr = configuration.getAsStringOrNull(FilesystemProvider.CONFIG_FS_ROOT)
                    ?: error("Filesystem Root is a compulsory configuration argument")
                return UserFileSystemDefault.getDirectory(rootPathStr)
                    ?: error("Filesystem Root '$rootPathStr' is not found")
            }
        }
    }

    private val _parsedConfig by lazy { FilesystemConfig(configuration) }

    override suspend fun createTypes(): TypesDomain =
        typesDomain("Filesystem", true) {
            namespace("filesystem") {
                data("FileSystemObjectHandle", implementation = FileSystemObjectHandle::class) {
                    propertyOf(setOf(REF, VAL), "name", "String")
                }
                data("DirectoryHandle", implementation = DirectoryHandle::class) {
                    supertype("FileSystemObjectHandle")
                }
                data("FileHandle", implementation = FileHandle::class) {
                    supertype("FileSystemObjectHandle")
                    // a new file, at a path relative to the filesystem root; it is created on commit
                    constructor_ {
                        parameter(emptySet(), "path", "String")
                        execution { args -> newFile(args[0] as String) }
                    }
                    propertyOf(setOf(REF, VAL), "extension", "String")
                    propertyOfWithBinding<FileHandle, FileContent>(
                        setOf(REF, VAL), "content", "FileContent",
                        accessorSuspend = { FileContent(it, this@FilesystemDatastore) }
                    )
                }
                data("FileContent", implementation = FileContent::class) {
                    propertyOfWithBinding<FileContent, String>(
                        setOf(REF, VAR), "asString", "String",
                        accessorSuspend = { it.datastore.contentOf(it.file) },
                        mutatorSuspend = { it, value -> it.datastore.stageContent(it.file, value ?: "") }
                    )
                }
            }
        }

    /**
     * input: empty
     * output: the files and directories under the filesystem root whose path, relative to the root, matches the access glob
     */
    override suspend fun doRead(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<DataItem>
    ): Flow<DataItem> {
        // the glob selects files by their path relative to the filesystem root; it defaults to all ('**')
        val globStr = accessConfiguration.getAsStringOrNull(FilesystemProvider.ACCESS_GLOB) ?: "**"
        val glob = globStr.toRegexFromGlob('/')
        val am = accessorMutator()
        val root = _parsedConfig.fsRoot()
        val rootPath = root.path.trimEnd('/') + "/"
        return root.listContent()
            .filter { glob.matches(it.path.removePrefix(rootPath)) }
            .map {
                DataItemDefault(
                    parent = null,
                    identity = DataItemIdentity(it.path),
                    data = am.toTypedObject(it, StdLibDefault.AnyType),
                    provenance = FilesystemProvenance(identity, it.path)
                )
            }
            .asFlow()
    }

    /**
     * input: DataItems with FileHandle data, whose content has been set (staged) through 'content.asString'
     * output: the same items. The staged content is written to the files on commit, and discarded on rollback.
     * An item whose data is not a FileHandle of this datastore gets an error issue.
     */
    override suspend fun doWrite(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<DataItem>
    ): Flow<DataItem> {
        return input.map { item ->
            when (item.data.self) {
                is FileHandle -> item
                else -> DataItemDefault(
                    item.parent, item.identity, item.data, item.provenance,
                    net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
                        item.issues.all.toList() + IssueDefault(IssueKind.ERROR, FilesystemIssueLocation(null), "the write input to a FilesystemDatastore must be a FileHandle.")
                    )
                )
            }
        }
    }

    // --- new files ---

    /**
     * A file that does not exist yet, at 'relativePath' under the filesystem root.
     * It is created on commit, if content was set for it; until then nothing is on disk.
     */
    class NewFileHandle internal constructor(
        val relativePath: String,
        rootPath: String,
        internal val datastore: FilesystemDatastore
    ) : FileHandle {
        override val parent: DirectoryHandle? get() = null
        override val name: String = relativePath.substringAfterLast('/')
        override val path: String = rootPath + relativePath
        override val absolutePath: String get() = path
        override val extension: String get() = if (name.contains('.')) name.substringAfterLast('.') else ""
        override suspend fun exists(): Boolean = false
        override suspend fun readContent(): String? = null
        override suspend fun writeContent(content: String) = error("Set 'content.asString' of a new file; it is written on commit")
        override suspend fun openAsZipDirectory(): DirectoryHandle? = null
        override fun hashCode(): Int = path.hashCode()
        override fun equals(other: Any?): Boolean = other is NewFileHandle && other.path == this.path && other.datastore === this.datastore
        override fun toString(): String = "NewFileHandle($relativePath)"
    }

    private var _rootPath: String? = null

    override suspend fun onOpen() {
        _rootPath = _parsedConfig.fsRoot().path.trimEnd('/') + "/"
    }

    /** a handle for a new file at 'relativePath' (e.g. 'out/new.txt') under the filesystem root */
    internal fun newFile(relativePath: String): NewFileHandle {
        checkOpen()
        val segments = relativePath.split('/')
        check(relativePath.isNotBlank() && !relativePath.startsWith("/") && segments.none { it.isEmpty() || it == "." || it == ".." }) {
            "The path of a new file must be relative to the filesystem root, without '.' or '..' segments: '$relativePath'"
        }
        return NewFileHandle(relativePath, _rootPath!!, this)
    }

    /** the directory holding 'file', creating missing directories if 'create'; null if not found, or an entry on the path is not a directory */
    private suspend fun directoryFor(file: NewFileHandle, create: Boolean): DirectoryHandle? {
        var dir: DirectoryHandle = _parsedConfig.fsRoot()
        for (seg in file.relativePath.split('/').dropLast(1)) {
            dir = when (val e = dir.entry(seg)) {
                null -> if (create) dir.createDirectory(seg) ?: return null else return null
                is DirectoryHandle -> e
                else -> error("Cannot create '${file.relativePath}': '$seg' is not a directory")
            }
        }
        return dir
    }

    // --- staged content: file path -> (file, content), applied on commit ---

    private val _staged = mutableMapOf<String, Pair<FileHandle, String>>()

    internal suspend fun contentOf(file: FileHandle): String? = _staged[file.path]?.second ?: file.readContent()

    internal fun stageContent(file: FileHandle, content: String) {
        checkInTransaction()
        _staged[file.path] = Pair(file, content)
    }

    /** checks that each new file can be created: no entry on its path is a file where a directory is needed, and its name is not a directory */
    override suspend fun onPrepare() {
        _staged.values.map { it.first }.filterIsInstance<NewFileHandle>().forEach { file ->
            val dir = directoryFor(file, create = false)
            if (null != dir) {
                check(dir.entry(file.name).let { null == it || it is FileHandle }) { "Cannot create '${file.relativePath}': it is a directory" }
            }
        }
    }

    /** writes the staged contents; a new file is created first, with any missing directories (an existing file at its path is overwritten) */
    override suspend fun onCommit() {
        try {
            _staged.values.forEach { (file, content) ->
                val f = when (file) {
                    is NewFileHandle -> directoryFor(file, create = true)?.getOrCreateFile(file.name)
                        ?: error("Cannot create '${file.relativePath}'")
                    else -> file
                }
                f.writeContent(content)
            }
        } finally {
            _staged.clear()
        }
    }

    override suspend fun onRollback() {
        _staged.clear()
    }
}