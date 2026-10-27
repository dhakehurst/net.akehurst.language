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
data class FilesystemProvenance(
    val datastoreIdentity: DatastoreIdentity,
    val path: String
) : DataItemProvenance

class FilesystemDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration
) : DatastoreAbstract(identity, configuration) {

    companion object {

        class FileContent(val file: FileHandle)

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
                    propertyOf(setOf(REF, VAL), "extension", "String")
                    propertyOfWithBinding<FileHandle, FileContent>(
                        setOf(REF, VAL), "content", "FileContent",
                        accessorSuspend = { FileContent(it) }
                    )
                }
                data("FileContent", implementation = FileContent::class) {
                    propertyOfWithBinding<FileContent, String>(
                        setOf(REF, VAR), "asString", "String",
                        accessorSuspend = { it.file.readContent() },
                        mutatorSuspend = { it, value -> it.file.writeContent(value ?: "") }
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
     * input: Asm
     * output: String
     */
    override suspend fun doWrite(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<DataItem>
    ): Flow<DataItem> {
        return input.map {
            TODO()
        }
    }
}