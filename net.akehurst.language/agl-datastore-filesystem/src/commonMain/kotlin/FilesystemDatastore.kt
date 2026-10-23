package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.kotlinx.filesystem.UserFileSystem
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
                return UserFileSystem.getDirectory(rootPathStr)
                    ?: error("Filesystem Root '$rootPathStr' is not found")
            }
        }
    }

    private val _parsedConfig by lazy { FilesystemConfig(configuration) }
    private val _issues = IssueHolder()

    override suspend fun types(): TypesDomain = typesDomain("Filesystem", true) {
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

    override suspend fun accessorMutator() = ObjectGraphAccessorMutatorByReflection(
        typesDomain = types(),
        locationMap = LocationMapDefault(),
        issues = _issues,
    )

    /**
     * input: empty
     * output: File
     */
    override suspend fun read(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<TypedObject>
    ): Flow<TypedObject> {
        val glob = "**".toRegexFromGlob('/') //TODO
        val am = accessorMutator()
        return _parsedConfig.fsRoot().listContent()
            .filter { glob.matches(it.path) }
            .map { am.toTypedObject(it, StdLibDefault.AnyType) }
            .asFlow()
    }

    /**
     * input: Asm
     * output: String
     */
    override suspend fun write(
        accessConfiguration: DatastoreConfiguration,
        input: Flow<TypedObject>
    ): Flow<TypedObject> {
        return input.map {
            TODO()
        }
    }
}