package net.akehurst.datastore.api

import kotlinx.coroutines.flow.Flow
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import kotlin.jvm.JvmInline

@JvmInline
value class DatastoreProviderIdentity(val value:String)

@JvmInline
value class DatastoreIdentity(val value:String)

@JvmInline
value class DataItemIdentity(val value:String)

/**
 * Where a DataItem came from.
 * Each Datastore implementation defines its own provenance by implementing this interface.
 */
interface DataItemProvenance

/**
 * Where, within a DataItem, an issue was found (e.g. a position in parsed text, or a spreadsheet cell).
 * Each Datastore implementation defines its own issue location by implementing this interface.
 */
interface DataItemIssueLocation

/**
 * A unit of data passed to and from Datastore read/write.
 */
interface DataItem {
    /** the item this item was derived from, if any */
    val parent: DataItem?
    val identity: DataItemIdentity

    /** the identities of the parents (outermost first) followed by this item's identity */
    val qualifiedIdentity: List<DataItemIdentity> get() = (parent?.qualifiedIdentity ?: emptyList()) + identity

    val data: TypedObject
    val provenance: DataItemProvenance

    /** issues found when this item was read or written, e.g. parse errors. If there are errors, 'data' may be nothing. */
    val issues: IssueCollection<DataItemIssueLocation>
}

interface DatastoreProviderRegistry {
    fun register(provider: DatastoreProvider)
    fun getOrNull(providerIdentity: DatastoreProviderIdentity): DatastoreProvider?
    fun provide(providerIdentity: DatastoreProviderIdentity, datastoreIdentity: DatastoreIdentity, datastoreConfiguration: DatastoreConfiguration): Datastore
}

sealed interface DsConfigParameterInfo {
    fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition?
}

interface DsConfigParameterDefinition : DsConfigParameterInfo{
    val identity: String
    val name: String
    val optional: Boolean
    val description:String
    val type: TypeDefinition

    /**
     * true if the value must not be shown, e.g. a password or an API token.
     * A DatastoreConfiguration masks the values of sensitive parameters in toString().
     */
    val sensitive: Boolean get() = false

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? = when (parameterIdentity) {
        identity -> this
        else -> null
    }

}

interface DsConfigParameterGroup : DsConfigParameterInfo {
    val name:String
    val parameterInfo: List<DsConfigParameterInfo>

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? =
        parameterInfo.firstNotNullOfOrNull { pi ->
            pi.getParameterOrNull(parameterIdentity)
        }
}

interface DsConfigParameterChoice : DsConfigParameterInfo {
    val alternative: Map<String, DsParameterSet>

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? =
        alternative.firstNotNullOfOrNull { alt ->
            alt.value.getParameterOrNull(parameterIdentity)
        }
}

interface DsParameterSet {
    val parameterInfo: List<DsConfigParameterInfo>

    fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition?
}

interface DatastoreProviderSchema {
    val datastore: DsParameterSet
    val access:DsParameterSet

}

interface DatastoreProvider {
    val identity:DatastoreProviderIdentity
    val configuration: DatastoreProviderSchema

    fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration):Datastore
}

interface DatastoreConfiguration {

    val providerIdentity:DatastoreProviderIdentity

    val argument: Map<String, Any>

    /** true if the parameter is declared sensitive in the provider's schema; its value must not be logged or shown */
    fun isSensitive(parameterIdentity: String): Boolean = false

    fun getAsString(parameterIdentity:String): String = argument[parameterIdentity] as String
    fun getAsStringOrNull(parameterIdentity:String): String? = argument[parameterIdentity] as String?

    fun getAsInt(parameterIdentity: String): Int = (argument[parameterIdentity] as Number).toInt()
    fun getAsIntOrNull(parameterIdentity: String): Int? = (argument[parameterIdentity] as Number?)?.toInt()

    fun getAsLong(parameterIdentity: String): Long = (argument[parameterIdentity] as Number).toLong()
    fun getAsLongOrNull(parameterIdentity: String): Long? = (argument[parameterIdentity] as Number?)?.toLong()

    fun getAsDouble(parameterIdentity: String): Double = (argument[parameterIdentity] as Number).toDouble()
    fun getAsDoubleOrNull(parameterIdentity: String): Double? = (argument[parameterIdentity] as Number?)?.toDouble()

    fun getAsBoolean(parameterIdentity: String): Boolean = argument[parameterIdentity] as Boolean
    fun getAsBooleanOrNull(parameterIdentity: String): Boolean? = argument[parameterIdentity] as Boolean?

}

enum class TransactionState {
    /** no transaction */
    NONE,

    /** begun; changes are staged, not applied */
    ACTIVE,

    /** prepare() succeeded: commit() is expected to succeed */
    PREPARED
}

/**
 * Transactions, with two-phase commit so that several datastores (e.g. the parts of a composition) commit all or nothing.
 * A write stages its changes; nothing is applied until commit().
 */
interface Transactional {
    val transactionState: TransactionState

    /** starts a transaction. Fails if one is already active. */
    suspend fun beginTransaction()

    /** checks that commit() will succeed (e.g. validation, locks, permissions), and fails if not. Changes nothing. */
    suspend fun prepare()

    /** applies the staged changes. Calls prepare() first if it was not called. */
    suspend fun commit()

    /** discards the staged changes and ends the transaction. Does nothing if there is no transaction. */
    suspend fun rollback()
}

/**
 * Runs 'block' in a transaction: begins, runs the block, then prepares and commits.
 * If anything fails, the transaction is rolled back and the failure is rethrown.
 */
suspend fun <T : Transactional, R> T.transaction(block: suspend (T) -> R): R {
    beginTransaction()
    try {
        val result = block(this)
        prepare()
        commit()
        return result
    } catch (t: Throwable) {
        if (transactionState != TransactionState.NONE) {
            try {
                rollback()
            } catch (r: Throwable) {
                t.addSuppressed(r)
            }
        }
        throw t
    }
}

/**
 * A composition's commit failed after some of its datastores had committed, so the composition is not all-or-nothing.
 * The datastores in 'notCommitted' were rolled back.
 */
class PartialCommitException(
    val committed: List<DatastoreIdentity>,
    val notCommitted: List<DatastoreIdentity>,
    cause: Throwable
) : RuntimeException(
    "Commit failed part way: committed [${committed.joinToString { it.value }}], not committed (rolled back) [${notCommitted.joinToString { it.value }}]: ${cause.message}",
    cause
)

interface Datastore : Transactional {
    val identity:DatastoreIdentity

    val configuration:DatastoreConfiguration

    /** true between open() and close() */
    val isOpen: Boolean

    /**
     * Prepares the datastore for read and write, e.g. connects and logs in to a remote tool.
     * read and write fail unless the datastore is open. Fails if it is already open.
     * After close() it can be opened again.
     */
    suspend fun open()

    /** Releases what open() acquired, e.g. disconnects; an active transaction is rolled back first. Does nothing if the datastore is not open. */
    suspend fun close()

    /**
     * The types of the data this datastore reads and writes.
     * They are fixed for this datastore: they may depend on its datastore configuration, never on an access configuration.
     * Returns the same instance on every call (see DatastoreAbstract).
     */
    suspend fun types() : TypesDomain

    suspend fun accessorMutator() : ObjectGraphAccessorMutator

    /**
     * 'accessConfiguration' selects which objects are read (e.g. a glob, or a query); it filters, it does not change their types.
     */
    suspend fun read(accessConfiguration:DatastoreConfiguration, input:Flow<DataItem>): Flow<DataItem>

    /** must be called, and the flow collected, inside a transaction; the changes are applied by commit() */
    suspend fun write(accessConfiguration:DatastoreConfiguration, input:Flow<DataItem>): Flow<DataItem>

}

/** opens this datastore, runs 'block', and closes the datastore even if 'block' throws */
suspend fun <D : Datastore, R> D.use(block: suspend (D) -> R): R {
    open()
    try {
        return block(this)
    } finally {
        close()
    }
}

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
    suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>)
}

/** opens this composition, runs 'block', and closes the composition even if 'block' throws */
suspend fun <C : DatastoreComposition, R> C.use(block: suspend (C) -> R): R {
    open()
    try {
        return block(this)
    } finally {
        close()
    }
}
