package net.akehurst.datastore.api

import kotlinx.coroutines.flow.Flow
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import kotlin.jvm.JvmInline

/**
 * One end of a DatastoreConnection: a constructor argument of the connection's targetType,
 * derived by evaluating 'expression' with `$self` = the source item's data.
 */
interface DatastoreConnectionEnd {
    val name: String

    /** (possibly qualified) name of the type of the expression result */
    val typeName: String

    val expression: String
}

/**
 * Connects the output of one datastore (source) to the input of another (target).
 * For each source item, an instance of targetType is created from the connection's ends.
 */
interface DatastoreConnection {
    val source: DatastoreIdentity
    val target: DatastoreIdentity

    /** optional name, needed to tell apart several connections from the same source to the same target (fan-out) */
    val name: String?

    /**
     * identifies the connection among the connections from the same source to the same target:
     * 'name' if given, otherwise the simple name of targetType with a lower-case first letter (e.g. 'parsableUnit').
     * It is the identity of the DataItems the connection creates, and the name of the derived association end on the source type.
     */
    val identity: String get() = name ?: targetType.substringAfterLast('.').replaceFirstChar { it.lowercaseChar() }

    /** (possibly qualified) name of a type, known to the target datastore, that the target reads as input */
    val targetType: String

    val end: List<DatastoreConnectionEnd>
}

interface DatastoreCompositionConfiguration {
    val part: Map<Pair<DatastoreProviderIdentity,DatastoreIdentity>, DatastoreConfiguration>
    val connection: List<DatastoreConnection>
}

interface AccessCompositionConfiguration {

    val datastoreConfiguration: DatastoreCompositionConfiguration
    val part: Map<DatastoreIdentity, DatastoreConfiguration>
}

/**
 * A composition's transaction spans all of its parts, and commits all or nothing:
 * beginTransaction() begins one on every part, prepare() prepares every part (if one fails, every part is rolled back),
 * and commit() commits the parts only once all are prepared. If a part still fails to commit, the parts not yet committed
 * are rolled back and a PartialCommitException names which parts committed.
 */
interface DatastoreComposition : Transactional {

    val configuration: DatastoreCompositionConfiguration

    fun create(registry: DatastoreProviderRegistry)

    /** true between open() and close() */
    val isOpen: Boolean

    /** opens the parts in chain order. If one fails, the parts already opened are closed again. */
    suspend fun open()

    /** rolls back an active transaction, then closes the parts in reverse chain order; every part is closed even if closing another one fails */
    suspend fun close()

    suspend fun types(): TypesDomain

    suspend fun accessorMutator(): ObjectGraphAccessorMutator
    suspend fun read(configuration: AccessCompositionConfiguration): Flow<DataItem>
    /**
     * Writes the items through the parts in reverse chain order, inside the composition's active transaction
     * (must be collected inside it, like Datastore.write); commit() applies the changes.
     * Each part's write output is mapped back to the previous part through the connections: the source object is found
     * through the link of the DataType end whose expression is '$self', and the value of each other end is assigned
     * to the source along its expression, which must be an assignable property path (e.g. 'content.asString').
     * An item that is not linked to a source object gets an error issue and is emitted as it is.
     * Returns the items written by the first part, plus the items that could not be written, with their issues.
     */
    suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>): Flow<DataItem>
}

object DatastoreCompositionExt {
    /** opens this composition, runs 'block', and closes the composition even if 'block' throws */
    suspend fun <C : DatastoreComposition, R> C.use(block: suspend (C) -> R): R {
        open()
        try {
            return block(this)
        } finally {
            close()
        }
    }
}