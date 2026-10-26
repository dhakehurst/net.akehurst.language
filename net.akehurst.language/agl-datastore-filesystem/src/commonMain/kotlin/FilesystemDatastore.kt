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
    override val identity: DatastoreIdentity,
    override val configuration: DatastoreConfiguration
) : Datastore {

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
    private val _issues = IssueHolder()

    // cached, so that objects read by this datastore are typed by the same TypeDefinitions that a composition merges (and adds associations to)
    private val _types: TypesDomain by lazy {
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
                data("FileContent") {
                    propertyOfWithBinding<FileContent, String>(
                        setOf(REF, VAR), "asString", "String",
                        accessorSuspend = { it.file.readContent() },
                        mutatorSuspend = { it, value -> it.file.writeContent(value ?: "") }
                    )
                }
            }
            }
    }

    private val _accessorMutator by lazy {
        ObjectGraphAccessorMutatorByReflection(
            typesDomain = _types,
            locationMap = LocationMapDefault(),
            issues = _issues,
        )
    }

    override suspend fun types(): TypesDomain = _types

    override suspend fun accessorMutator() = _accessorMutator

    /**
     * input: empty
     * output: File
     */
    override suspend fun read(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<DataItem>
    ): Flow<DataItem> {
        val glob = "**".toRegexFromGlob('/') //TODO
        val am = accessorMutator()
        return _parsedConfig.fsRoot().listContent()
            .filter { glob.matches(it.path) }
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
    override suspend fun write(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<DataItem>
    ): Flow<DataItem> {
        return input.map {
            TODO()
        }
    }
}