package net.akehurst.datastore.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreObjectTyping
import net.akehurst.datastore.api.DatastoreTestResult
import net.akehurst.datastore.api.TransactionState
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistry
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistryDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.processor.ExternalGetterByReflection
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.types.api.TypesDomain

/**
 * Base class for Datastore implementations.
 *
 * It creates the types and the accessor-mutator once, and returns the same instances from every call of
 * types() and accessorMutator(). A composition relies on this: the objects a datastore emits must be typed
 * against the same TypeDefinitions that the composition merges.
 *
 * It also keeps the open/closed state: read and write fail unless the datastore is open.
 * Override onOpen()/onClose() to connect and disconnect, and implement doRead()/doWrite().
 * A datastore that can delete items overrides supportsDelete to return true, and doDelete().
 *
 * It keeps the transaction state too: write fails unless a transaction is active.
 * Override onBeginTransaction(), onPrepare(), onCommit() and onRollback(); they do nothing by default,
 * which suits a datastore that does not change anything (e.g. one that only formats text).
 *
 * Each item a datastore outputs gets its provenance from provenanceFor(); override it to give a custom provenance.
 *
 * testConnection() and testAccess() have defaults: testConnection opens and closes the datastore, testAccess reads at most
 * 'limit' items. A provider overrides them where it knows a better cheap test (e.g. counting items without reading them),
 * and can use whileOpenForTest() to open and close as the defaults do.
 */
abstract class DatastoreAbstract(
    override val identity: DatastoreIdentity,
    configuration: DatastoreConfiguration,
    /** where the secrets referenced by the configuration (SecretReference arguments) are read from, when the datastore is opened */
    private val secretSources: SecretSourceRegistry = SecretSourceRegistryDefault.GLOBAL
) : Datastore {

    private val _configuration = configuration
    private var _resolvedConfiguration: DatastoreConfiguration? = null

    /**
     * the configuration. While the datastore is open, its secret references are replaced by the secrets
     * (read when it was opened); after close() they are references again. Read secrets while open, do not keep them.
     */
    final override val configuration: DatastoreConfiguration get() = _resolvedConfiguration ?: _configuration

    /** issues raised by the accessor-mutator */
    protected val accessorMutatorIssues = IssueHolder()

    private val _mutex = Mutex()
    private var _types: TypesDomain? = null
    private var _accessorMutator: ObjectGraphAccessorMutator? = null

    /** creates this datastore's types. Called once. Suspending, so that e.g. a schema can be fetched from a remote tool. */
    protected abstract suspend fun createTypes(): TypesDomain

    /**
     * creates this datastore's accessor-mutator over its types. Called once.
     * If this datastore is a DatastoreObjectTyping, it is asked for the type of each object before its class is used.
     */
    protected open suspend fun createAccessorMutator(types: TypesDomain): ObjectGraphAccessorMutator {
        val byReflection = ExternalGetterByReflection(types, accessorMutatorIssues)
        return ObjectGraphAccessorMutatorByReflection(
            typesDomain = types,
            issues = accessorMutatorIssues,
            locationMap = LocationMapDefault(),
            externalGetter = when (this) {
                is DatastoreObjectTyping -> ExternalGetterWithObjectTyping(types, listOf(this), byReflection)
                else -> byReflection
            },
        )
    }

    private var _isOpen = false

    final override val isOpen: Boolean get() = _isOpen

    /** called by open(), e.g. to connect and log in. Does nothing by default. */
    protected open suspend fun onOpen() {}

    /** called by close(), e.g. to disconnect. Does nothing by default. */
    protected open suspend fun onClose() {}

    final override suspend fun open() {
        check(_isOpen.not()) { "Datastore '${identity.value}' is already open" }
        _resolvedConfiguration = _configuration.withSecretsResolved(secretSources)
        try {
            onOpen()
        } catch (t: Throwable) {
            _resolvedConfiguration = null
            throw t
        }
        _isOpen = true
    }

    final override suspend fun close() {
        if (_isOpen) {
            try {
                rollback()
            } finally {
                _isOpen = false
                try {
                    onClose()
                } finally {
                    _resolvedConfiguration = null
                }
            }
        }
    }

    private var _transactionState = TransactionState.NONE

    final override val transactionState: TransactionState get() = _transactionState

    /** called by beginTransaction() */
    protected open suspend fun onBeginTransaction() {}

    /** called by prepare(): throw if the staged changes cannot be committed */
    protected open suspend fun onPrepare() {}

    /** called by commit(): apply the staged changes */
    protected open suspend fun onCommit() {}

    /** called by rollback(): discard the staged changes */
    protected open suspend fun onRollback() {}

    final override suspend fun beginTransaction() {
        checkOpen()
        check(_transactionState == TransactionState.NONE) { "Datastore '${identity.value}' already has a transaction" }
        onBeginTransaction()
        _transactionState = TransactionState.ACTIVE
    }

    final override suspend fun prepare() {
        check(_transactionState == TransactionState.ACTIVE) { "Datastore '${identity.value}' has no active transaction to prepare" }
        onPrepare()
        _transactionState = TransactionState.PREPARED
    }

    final override suspend fun commit() {
        check(_transactionState != TransactionState.NONE) { "Datastore '${identity.value}' has no transaction to commit" }
        if (_transactionState == TransactionState.ACTIVE) prepare()
        onCommit()
        _transactionState = TransactionState.NONE
    }

    final override suspend fun rollback() {
        if (_transactionState != TransactionState.NONE) {
            try {
                onRollback()
            } finally {
                _transactionState = TransactionState.NONE
            }
        }
    }

    protected fun checkInTransaction() {
        check(_transactionState == TransactionState.ACTIVE) { "Datastore '${identity.value}' has no active transaction; write inside a transaction" }
    }

    protected fun checkOpen() {
        check(_isOpen) { "Datastore '${identity.value}' is not open" }
    }

    /** the read of this datastore; only called while it is open */
    protected abstract suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem>

    /** the write of this datastore; only called while it is open and in a transaction. Stage the changes, apply them in onCommit(). */
    protected abstract suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem>

    // checked when called, and again when the flow is collected, as the flow may be collected later
    final override suspend fun read(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        checkOpen()
        return doRead(accessConfiguration.withSecretsResolved(secretSources), input).onStart { checkOpen() }
    }

    final override suspend fun write(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        checkOpen()
        checkInTransaction()
        return doWrite(accessConfiguration.withSecretsResolved(secretSources), input).onStart { checkOpen(); checkInTransaction() }
    }

    /** false by default; a datastore that can delete items overrides it to return true, and implements doDelete() */
    override val supportsDelete: Boolean get() = false

    /** the delete of this datastore; only called while it is open, in a transaction, and if supportsDelete. Stage the deletions, apply them in onCommit(). */
    protected open suspend fun doDelete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> =
        throw UnsupportedOperationException("Datastore '${identity.value}' does not support delete")

    final override suspend fun delete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> {
        if (supportsDelete.not()) throw UnsupportedOperationException("Datastore '${identity.value}' does not support delete")
        checkOpen()
        checkInTransaction()
        return doDelete(accessConfiguration.withSecretsResolved(secretSources), input).onStart { checkOpen(); checkInTransaction() }
    }

    /**
     * Runs 'block' with the datastore open: opens it if it is not open, and closes it again afterwards if it opened it.
     * Never throws (except for cancellation): a failure of open, 'block' or close is a result with success = false and the
     * failure's message. For testConnection() and testAccess().
     */
    protected suspend fun whileOpenForTest(block: suspend () -> DatastoreTestResult): DatastoreTestResult {
        val openedHere = _isOpen.not()
        return try {
            if (openedHere) open()
            val result = try {
                block()
            } finally {
                if (openedHere) close()
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            DatastoreTestResult(success = false, message = testFailureMessage(t))
        }
    }

    /** opens (if not open) and closes again; "Connected", or the failure's message */
    override suspend fun testConnection(): DatastoreTestResult = whileOpenForTest {
        DatastoreTestResult(success = true, message = "Connected")
    }

    /** reads at most 'limit' items: read(access, input).take(limit); itemCount = the number read, with a warning if none */
    override suspend fun testAccess(access: DatastoreConfiguration, input: Flow<DataItem>, limit: Int): DatastoreTestResult = whileOpenForTest {
        require(limit >= 1) { "The limit must be at least 1, it is $limit" }
        val n = read(access, input).take(limit).count()
        when (n) {
            0 -> DatastoreTestResult(success = true, message = "Access works, but no items were found", itemCount = 0, warnings = listOf("No items were found"))
            else -> DatastoreTestResult(success = true, message = "Read $n item(s)", itemCount = n)
        }
    }

    /**
     * the provenance of an item this datastore outputs (read or write).
     * Override it to give a custom provenance. By default a DatastoreProvenance of this datastore's identity.
     * @param input the item the output item was produced from, or null if it was read from the store itself (e.g. a file)
     * @param source the object the output item was made from (e.g. a FileSystemObjectHandle, or a ParsableUnit), or null if none (e.g. an error item)
     */
    protected open fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = DatastoreProvenance(identity)

    final override suspend fun types(): TypesDomain =
        _types ?: _mutex.withLock { _types ?: createTypes().also { _types = it } }

    final override suspend fun accessorMutator(): ObjectGraphAccessorMutator =
        _accessorMutator ?: types().let { types ->
            _mutex.withLock { _accessorMutator ?: createAccessorMutator(types).also { _accessorMutator = it } }
        }

}

/** Provenance of a DataItem output by a datastore: the datastore's identity. The default of DatastoreAbstract.provenanceFor. */
data class DatastoreProvenance(
    val datastoreIdentity: DatastoreIdentity
) : DataItemProvenance

/** the message of a failed test: the failure's message, or else its class name */
fun testFailureMessage(t: Throwable): String = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "Failed"
