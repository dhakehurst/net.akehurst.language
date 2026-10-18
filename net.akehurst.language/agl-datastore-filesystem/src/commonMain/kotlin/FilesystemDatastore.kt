package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.kotlinx.filesystem.api.DirectoryHandle
import net.akehurst.kotlinx.filesystem.api.FileHandle
import net.akehurst.kotlinx.filesystem.api.FileSystemObjectHandle
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

class FilesystemDatastore(
    override val identity: DatastoreIdentity,
    override val configuration: DatastoreConfiguration
) : Datastore {

    companion object {
        class FilesystemConfig(val configuration: DatastoreConfiguration) {

        }
    }

    private val _parsedConfig by lazy { FilesystemConfig(configuration) }
    private val _issues = IssueHolder()

    override suspend fun types(): TypesDomain = typesDomain("Filesystem",true) {
        namespace("filesystem") {
            data("FileSystemObjectHandle", implementation = FileSystemObjectHandle::class) {
                propertyOf(setOf(REF, VAL),"name","String")
            }
            data("DirectoryHandle", implementation = DirectoryHandle::class) {
                supertype("FileSystemObjectHandle")
            }
            data("FileHandle", implementation = FileHandle::class) {
                supertype("FileSystemObjectHandle")
                propertyOf(setOf(REF, VAL),"extension","String")
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
    override suspend fun read(accessConfiguration: DatastoreConfiguration, input: Flow<TypedObject>): Flow<TypedObject> {
        return input.map {
            TODO()
        }
    }

    /**
     * input: Asm
     * output: String
     */
    override suspend fun write(accessConfiguration: DatastoreConfiguration, input: Flow<TypedObject>): Flow<TypedObject> {
        return input.map {
            TODO()
        }
    }
}