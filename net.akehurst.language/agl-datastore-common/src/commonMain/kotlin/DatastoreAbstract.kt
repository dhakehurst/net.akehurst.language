package net.akehurst.datastore.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
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
 */
abstract class DatastoreAbstract(
    override val identity: DatastoreIdentity,
    override val configuration: DatastoreConfiguration
) : Datastore {

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

    final override suspend fun types(): TypesDomain =
        _types ?: _mutex.withLock { _types ?: createTypes().also { _types = it } }

    final override suspend fun accessorMutator(): ObjectGraphAccessorMutator =
        _accessorMutator ?: types().let { types ->
            _mutex.withLock { _accessorMutator ?: createAccessorMutator(types).also { _accessorMutator = it } }
        }

}
