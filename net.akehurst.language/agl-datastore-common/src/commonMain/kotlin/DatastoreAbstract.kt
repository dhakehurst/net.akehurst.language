package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.TransactionState
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistry
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistryDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
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
 *
 * It keeps the transaction state too: write fails unless a transaction is active.
 * Override onBeginTransaction(), onPrepare(), onCommit() and onRollback(); they do nothing by default,
 * which suits a datastore that does not change anything (e.g. one that only formats text).
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

    /** creates this datastore's accessor-mutator over its types. Called once. */
    protected open suspend fun createAccessorMutator(types: TypesDomain): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(
        typesDomain = types,
        issues = accessorMutatorIssues,
        locationMap = LocationMapDefault(),
    )

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

    final override suspend fun types(): TypesDomain =
        _types ?: _mutex.withLock { _types ?: createTypes().also { _types = it } }

    final override suspend fun accessorMutator(): ObjectGraphAccessorMutator =
        _accessorMutator ?: types().let { types ->
            _mutex.withLock { _accessorMutator ?: createAccessorMutator(types).also { _accessorMutator = it } }
        }

}
