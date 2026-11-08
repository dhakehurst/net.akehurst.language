package net.akehurst.datastore.api

import kotlinx.coroutines.flow.Flow
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.language.base.api.QualifiedName
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

/**
 * A reference to a secret, given as the argument of a sensitive parameter instead of its value,
 * e.g. argument(PASSWORD, secret("keychain", "rm-tool/david")).
 * The secret is read from the named SecretSource (net.akehurst.kotlinx.secure.storage) when the datastore is opened; the configuration only holds the reference.
 */
data class SecretReference(
    /** the identity of the SecretSource */
    val source: String,
    /** the key of the secret in that source */
    val key: String
) {
    override fun toString(): String = "secret($source, $key)"
}

interface DatastoreProviderRegistry {
    /**
     * the registered providers, in the order they were registered.
     * Registering a provider with an identity that is already registered replaces it, keeping its position.
     */
    val providers: List<DatastoreProvider>

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

    /**
     * the default value of the parameter, null if it has none.
     * A DatastoreConfiguration gives it (valueOf, getAs...) when no argument is given for the parameter,
     * and a required parameter with a default may be left out.
     */
    val defaultValue: Any? get() = null

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? = when (parameterIdentity) {
        identity -> this
        else -> null
    }

}

interface DsConfigParameterGroup : DsConfigParameterInfo {
    val name:String
    val parameterInfo: List<DsConfigParameterInfo>

    /**
     * true if the group holds parameters that most users need not change, e.g. a group named "Advanced".
     * A GUI may show an advanced group folded. Inside a DsConfigParameterChoice each alternative can have its own advanced group.
     */
    val advanced: Boolean get() = false

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
    /** the parameters of this provider's datastore and access configurations */
    val schema: DatastoreProviderSchema

    fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration):Datastore
}

interface DatastoreConfiguration {

    val providerIdentity:DatastoreProviderIdentity

    val argument: Map<String, Any>

    /** true if the parameter is declared sensitive in the provider's schema; its value must not be logged or shown */
    fun isSensitive(parameterIdentity: String): Boolean = false

    /** the default value of the parameter, from the provider's schema; null if it has none */
    fun defaultValueOf(parameterIdentity: String): Any? = null

    /** the argument given for the parameter, or else its default value; null if neither */
    fun valueOf(parameterIdentity: String): Any? = argument[parameterIdentity] ?: defaultValueOf(parameterIdentity)

    fun getAsString(parameterIdentity:String): String = valueOf(parameterIdentity) as String
    fun getAsStringOrNull(parameterIdentity:String): String? = valueOf(parameterIdentity) as String?

    fun getAsInt(parameterIdentity: String): Int = (valueOf(parameterIdentity) as Number).toInt()
    fun getAsIntOrNull(parameterIdentity: String): Int? = (valueOf(parameterIdentity) as Number?)?.toInt()

    fun getAsLong(parameterIdentity: String): Long = (valueOf(parameterIdentity) as Number).toLong()
    fun getAsLongOrNull(parameterIdentity: String): Long? = (valueOf(parameterIdentity) as Number?)?.toLong()

    fun getAsDouble(parameterIdentity: String): Double = (valueOf(parameterIdentity) as Number).toDouble()
    fun getAsDoubleOrNull(parameterIdentity: String): Double? = (valueOf(parameterIdentity) as Number?)?.toDouble()

    fun getAsBoolean(parameterIdentity: String): Boolean = valueOf(parameterIdentity) as Boolean
    fun getAsBooleanOrNull(parameterIdentity: String): Boolean? = valueOf(parameterIdentity) as Boolean?

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

    /**
     * true if this datastore can delete items. If false, delete() throws UnsupportedOperationException,
     * e.g. for a read-only datastore.
     */
    val supportsDelete: Boolean get() = false

    /**
     * Deletes the items with the given identities.
     * Like write, it must be called, and the flow collected, inside a transaction; the deletions are applied by commit().
     * 'accessConfiguration' selects which objects the deletion deals with, as for read and write.
     * Returns the identities of the items that are deleted.
     * Throws UnsupportedOperationException unless 'supportsDelete' is true.
     */
    suspend fun delete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> =
        throw UnsupportedOperationException("Datastore '${identity.value}' does not support delete")

}

/**
 * Implemented by a datastore whose objects' types cannot all be found from their class, e.g. because one
 * implementation class is used by several types whose definitions come from a connected tool.
 * The accessor-mutator of a composition (and the default one of DatastoreAbstract) asks it first,
 * and finds the type by its class only if it returns null.
 */
interface DatastoreObjectTyping {
    /**
     * the qualified name of the type of 'obj', or null if this datastore does not decide it (then its class does).
     * A name, not a TypeInstance, so that it is looked up in whichever types are in use: this datastore's own, or a composition's merged ones.
     */
    fun qualifiedTypeNameOf(obj: Any): QualifiedName?
}

object DatastoreExt{
    /** opens this datastore, runs 'block', and closes the datastore even if 'block' throws */
    suspend fun <D : Datastore, R> D.use(block: suspend (D) -> R): R {
        open()
        try {
            return block(this)
        } finally {
            close()
        }
    }
}