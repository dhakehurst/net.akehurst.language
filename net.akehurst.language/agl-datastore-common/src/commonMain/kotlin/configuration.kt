package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.emitAll
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreConnectionEnd
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderSchema
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry
import net.akehurst.datastore.api.PartialCommitException
import net.akehurst.datastore.api.TransactionState
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectSuspending
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypesNamespace
import net.akehurst.language.types.builder.AssociationBuilder
import net.akehurst.language.types.builder.typesDomain

class DatastoreProviderSchemaDefault(
    override val datastore: DsParameterSet,
    override val access: DsParameterSet
) : DatastoreProviderSchema {

}

class DsParameterSetDefault(
    override val parameterInfo: List<DsConfigParameterInfo>
) : DsParameterSet {
    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? =
        parameterInfo.firstNotNullOfOrNull { pi ->
            pi.getParameterOrNull(parameterIdentity)
        }

}

class DatastoreCompositionDefault(
    override val configuration: DatastoreCompositionConfiguration
) : DatastoreComposition {

    val issues = IssueHolder()

    val datastore = mutableMapOf<DatastoreIdentity, Datastore>()

    override fun create(registry: DatastoreProviderRegistry) {
        configuration.part.forEach { (ids, cfg) ->
            val (provId, dsId) = ids
            datastore[dsId] = registry.provide(provId, dsId, cfg)
        }
    }

    private var _isOpen = false
    override val isOpen: Boolean get() = _isOpen

    override suspend fun open() {
        check(_isOpen.not()) { "Composition is already open" }
        val opened = mutableListOf<Datastore>()
        try {
            for (ds in datastore.values) {
                ds.open()
                opened.add(ds)
            }
        } catch (t: Throwable) {
            closeAll(opened.reversed())?.let { t.addSuppressed(it) }
            throw t
        }
        _isOpen = true
    }

    override suspend fun close() {
        if (_isOpen) {
            val rb = try {
                rollback(); null
            } catch (t: Throwable) {
                t
            }
            _isOpen = false
            val cl = closeAll(datastore.values.reversed())
            when {
                null != rb -> throw rb.also { e -> cl?.let { e.addSuppressed(it) } }
                null != cl -> throw cl
            }
        }
    }

    private var _transactionState = TransactionState.NONE
    override val transactionState: TransactionState get() = _transactionState

    override suspend fun beginTransaction() {
        check(_isOpen) { "Composition is not open" }
        check(_transactionState == TransactionState.NONE) { "Composition already has a transaction" }
        val begun = mutableListOf<Datastore>()
        try {
            for (ds in datastore.values) {
                ds.beginTransaction()
                begun.add(ds)
            }
        } catch (t: Throwable) {
            rollbackAll(begun.reversed())?.let { t.addSuppressed(it) }
            throw t
        }
        _transactionState = TransactionState.ACTIVE
    }

    /** prepares every part; if one fails, every part is rolled back and the failure is rethrown */
    override suspend fun prepare() {
        check(_transactionState == TransactionState.ACTIVE) { "Composition has no active transaction to prepare" }
        try {
            for (ds in datastore.values) {
                ds.prepare()
            }
        } catch (t: Throwable) {
            _transactionState = TransactionState.NONE
            rollbackAll(datastore.values.reversed())?.let { t.addSuppressed(it) }
            throw t
        }
        _transactionState = TransactionState.PREPARED
    }

    /**
     * prepares (if not yet prepared), then commits the parts in chain order.
     * If a part fails to commit, it and the parts after it are rolled back; if parts before it had committed,
     * a PartialCommitException names them, otherwise the failure is rethrown (nothing was committed).
     */
    override suspend fun commit() {
        check(_transactionState != TransactionState.NONE) { "Composition has no transaction to commit" }
        if (_transactionState == TransactionState.ACTIVE) prepare()
        val parts = datastore.values.toList()
        for ((i, ds) in parts.withIndex()) {
            try {
                ds.commit()
            } catch (t: Throwable) {
                _transactionState = TransactionState.NONE
                val notCommitted = parts.subList(i, parts.size)
                rollbackAll(notCommitted.reversed())?.let { t.addSuppressed(it) }
                when {
                    0 == i -> throw t
                    else -> throw PartialCommitException(parts.subList(0, i).map { it.identity }, notCommitted.map { it.identity }, t)
                }
            }
        }
        _transactionState = TransactionState.NONE
    }

    override suspend fun rollback() {
        if (_transactionState != TransactionState.NONE) {
            _transactionState = TransactionState.NONE
            rollbackAll(datastore.values.reversed())?.let { throw it }
        }
    }

    /** rolls back each datastore, in the given order, even if one fails; returns the first failure */
    private suspend fun rollbackAll(datastores: List<Datastore>): Throwable? = forAll(datastores) { it.rollback() }

    /** closes each datastore, in the given order, even if closing one fails; returns the first failure */
    private suspend fun closeAll(datastores: List<Datastore>): Throwable? = forAll(datastores) { it.close() }

    /** does 'action' for each datastore, in the given order, even if it fails for one; returns the first failure */
    private suspend fun forAll(datastores: List<Datastore>, action: suspend (Datastore) -> Unit): Throwable? {
        var first: Throwable? = null
        for (ds in datastores) {
            try {
                action(ds)
            } catch (t: Throwable) {
                val f = first
                if (null == f) first = t else f.addSuppressed(t)
            }
        }
        return first
    }

    companion object {
        /** the namespace that holds the associations generated from the connections */
        const val COMPOSITION_NAMESPACE = "composition"
        private const val SELF = "\$self"
        private val PATH_REGEX = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
    }

    // cached, so the same types are used throughout
    private var _types: TypesDomain? = null
    private var _accessorMutator: ObjectGraphAccessorMutator? = null

    override suspend fun types(): TypesDomain = _types ?: createTypes().also { _types = it }

    /**
     * an accessor-mutator over the composed types, e.g. for building expressions.
     * It holds no links: each read has its own accessor-mutator (see read), and the items it emits are typed by that one.
     */
    override suspend fun accessorMutator(): ObjectGraphAccessorMutator = _accessorMutator ?: newAccessorMutator().also { _accessorMutator = it }

    /** a new accessor-mutator over the composed types, with its own (empty) store of association links */
    private suspend fun newAccessorMutator(): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(
        typesDomain = types(),
        issues = issues,
        locationMap = LocationMapDefault(),
    )

    /**
     * Merges the types of all parts, then adds a namespace for the composition,
     * with an association for each connection end whose type is a DataType.
     */
    private suspend fun createTypes(): TypesDomain {
        val merged = datastore.values.fold(typesDomain("Composition", true) {}) { acc, it ->
            acc.merge(it.types())
        }
        if (configuration.connection.isNotEmpty()) {
            val imports = configuration.connection
                .flatMap { conn -> listOf(conn.targetType) + conn.end.map { it.typeName } }
                .mapNotNull { tn -> merged.findFirstDefinitionByPossiblyQualifiedNameOrNull(tn.asPossiblyQualifiedName)?.namespace?.qualifiedName?.value }
                .distinct()
                .map { Import(it) }
            val compNs = merged.findOrCreateNamespace(COMPOSITION_NAMESPACE.asQualifiedName, imports)
            merged.resolveImports()
            configuration.connection.forEach { conn -> createAssociations(merged, compNs, conn) }
        }
        return merged
    }

    /**
     * For connection 'fs -> txt' with target type ParsableUnit and end 'owner: FileHandle', creates
     *   ParsableUnit.owner : FileHandle          (set when the ParsableUnit is created by read/write)
     *   FileHandle.parsableUnit : ParsableUnit?  (the opposite end of the same link)
     * The links are stored by the accessor-mutator's ExternalGetter (getLinkEnd/setLinkEnd), not in the objects.
     * Ends whose type is not a DataType (e.g. String) are constructor arguments, not association ends.
     * Both properties are owned by the association, so the parts' types are not changed.
     */
    private fun createAssociations(types: TypesDomain, ns: TypesNamespace, connection: DatastoreConnection) {
        val targetDef = types.findFirstDefinitionByPossiblyQualifiedNameOrNull(connection.targetType.asPossiblyQualifiedName) as? DataType
            ?: error("Connection '${connection.source}' -> '${connection.target}': target type '${connection.targetType}' is not a DataType")
        val targetEndName = connection.identity
        connection.end.forEach { connEnd ->
            val endDef = dataTypeOrNull(types, connEnd.typeName)
            if (null != endDef) {
                AssociationBuilder(ns, mutableListOf()).apply {
                    // end i becomes a property on the type of the other end
                    end(endDef.qualifiedName.value, setOf(REF, VAR, STR), connEnd.name, ownedByAssociation = true)
                    // both ends are owned by the association: the links are held by the accessor-mutator's ExternalGetter, not by the objects
                    end(targetDef.qualifiedName.value, setOf(REF, VAR, STR), targetEndName, isNullable = true, ownedByAssociation = true)
                }.build()
            }
        }
    }

    private fun dataTypeOrNull(types: TypesDomain, typeName: String): DataType? =
        types.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName.asPossiblyQualifiedName) as? DataType

    /**
     * Reads the parts in order, passing each part's output to the next part's input.
     * If connections are configured from the previous part to the next, each item is adapted through each of them (fan-out).
     */
    /**
     * Each read has its own accessor-mutator, which holds the links that the read creates.
     * Every item emitted (and its parents) is typed by it, so navigating from an item's data, e.g.
     * item.data.getProperty("parsableUnit"), follows that read's links. Two reads never share links,
     * and the links are freed with the items.
     */
    override suspend fun read(configuration: AccessCompositionConfiguration): Flow<DataItem> {
        check(_isOpen) { "Composition is not open" }
        val am = newAccessorMutator()
        var previous: Datastore? = null
        var flow = emptyFlow<DataItem>()
        for (ds in datastore.values) {
            val dsCfg = configuration.part[ds.identity]
                ?: error("Can't find access configuration for datastore '${ds.identity}'")
            val prev = previous
            val conns = prev?.let { p -> this.configuration.connection.filter { it.source == p.identity && it.target == ds.identity } } ?: emptyList()
            val input = when {
                conns.isNotEmpty() -> adapt(conns, flow, am)
                else -> flow
            }
            flow = ds.read(dsCfg, input)
            previous = ds
        }
        return flow.map { retype(it, am) }
    }

    /** the item, with its data and the data of its parents typed by 'am' */
    private fun retype(item: DataItem, am: ObjectGraphAccessorMutator): DataItem = DataItemDefault(
        parent = item.parent?.let { retype(it, am) },
        identity = item.identity,
        data = if (item.data.accessor === am) item.data else am.typedAs(item.data.self, item.data.type),
        provenance = item.provenance,
        issues = item.issues
    )

    /**
     * For each source item, creates the target object of each connection.
     * Each end's expression is evaluated with $self = the source item's data.
     * Ends with a primitive type are constructor arguments, ends with a DataType are set through the association.
     * The created item's identity is the connection's identity.
     */
    private suspend fun adapt(connections: List<DatastoreConnection>, input: Flow<DataItem>, am: ObjectGraphAccessorMutator): Flow<DataItem> {
        val types = types()
        val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(am)
        val adapters = connections.map { connection ->
            val targetType = connection.targetType.asPossiblyQualifiedName
            val (assocEnds, ctorEnds) = connection.end.partition { null != dataTypeOrNull(types, it.typeName) }
            val adapter: suspend (DataItem) -> DataItem = { item ->
                val evc = EvaluationContext.ofSelf(item.data)
                val ctorArgs = ctorEnds.associate { end -> Pair(end.name, interpreter.evaluateStr(evc, end.expression)) }
                val data = am.createStructureValueSuspend(targetType, ctorArgs)
                assocEnds.forEach { end ->
                    // link from the end object's side: its end name (the connection identity) is unique for each connection,
                    // whereas the target's end name (e.g. 'owner') can be the same in several connections (fan-out)
                    val endObj = interpreter.evaluateStr(evc, end.expression)
                    am.setProperty(endObj, connection.identity, data)
                }
                DataItemDefault(
                    parent = item,
                    identity = DataItemIdentity(connection.identity),
                    data = data,
                    provenance = ConnectionProvenance(connection.source, connection.target, connection.identity)
                )
            }
            adapter
        }
        return input.transform { item ->
            adapters.forEach { emit(it.invoke(item)) }
        }
    }

    /**
     * Writes the parts in reverse chain order. Between two parts, each item written by the later part is mapped back
     * to the source object of a connection (see reverseAdapt), which is then written by the earlier part.
     * Items that cannot be mapped back are emitted, with an error issue, after the items of the first part.
     */
    override suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        check(_isOpen) { "Composition is not open" }
        checkInTransaction()
        val parts = datastore.values.toList()
        val notWritten = mutableListOf<DataItem>()
        var flow = input
        for (i in parts.indices.reversed()) {
            val ds = parts[i]
            val dsCfg = configuration.part[ds.identity]
                ?: error("Can't find access configuration for datastore '${ds.identity}'")
            flow = ds.write(dsCfg, flow)
            if (i > 0) {
                val prev = parts[i - 1]
                val conns = this.configuration.connection.filter { it.source == prev.identity && it.target == ds.identity }
                if (conns.isNotEmpty()) flow = reverseAdapt(conns, flow, notWritten)
            }
        }
        return flow
            .onStart { check(_isOpen) { "Composition is not open" }; checkInTransaction(); notWritten.clear() }
            .onCompletion { cause -> if (null == cause) emitAll(notWritten.asFlow()) }
    }

    private fun checkInTransaction() {
        check(_transactionState == TransactionState.ACTIVE) { "Composition has no active transaction; write inside a transaction" }
    }

    /**
     * The reverse of adapt. For each target item, and each connection whose source end ('$self') links it to a source object:
     * the value of each primitive end of the target is assigned to the source along that end's expression,
     * and the source item is emitted (the target item's parent, if that holds the source object).
     * A target item with errors is not assigned; its issues are passed on with the source item.
     * A target item that no connection links to a source object is added to 'notWritten' with an error issue.
     */
    private suspend fun reverseAdapt(connections: List<DatastoreConnection>, input: Flow<DataItem>, notWritten: MutableList<DataItem>): Flow<DataItem> {
        val types = types()
        val reversers = connections.map { connection ->
            val (assocEnds, valueEnds) = connection.end.partition { null != dataTypeOrNull(types, it.typeName) }
            val sourceEnd = assocEnds.firstOrNull { it.expression.trim() == SELF }
                ?: error("Connection '${connection.identity}': cannot write, it has no end of a DataType with expression '$SELF' to link back to the source")
            val paths = valueEnds.associateWith { end ->
                assignablePathOrNull(end.expression)
                    ?: error("Connection '${connection.identity}': cannot write, the expression '${end.expression}' of end '${end.name}' is not an assignable property path")
            }
            Pair(connection, Pair(sourceEnd, paths))
        }
        return input.transform { item ->
            var linked = false
            for ((connection, rev) in reversers) {
                val (sourceEnd, paths) = rev
                val source = item.data.getPropertySuspend(sourceEnd.name)
                if (source.isNothing) continue
                linked = true
                if (item.issues.errors.isEmpty()) {
                    paths.forEach { (end, path) -> assign(source, path, item.data.getPropertySuspend(end.name)) }
                }
                val parent = item.parent
                val sourceItem = when {
                    null != parent && parent.data.self == source.self -> parent
                    else -> DataItemDefault(null, item.identity, source, ConnectionProvenance(connection.source, connection.target, connection.identity))
                }
                emit(withIssues(sourceItem, item.issues))
            }
            if (!linked) {
                notWritten.add(
                    withIssues(
                        item, net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
                            listOf(IssueDefault(IssueKind.ERROR, CompositionIssueLocation(item.qualifiedIdentity), "the item is not linked to a source object by any connection, so it cannot be written"))
                        )
                    )
                )
            }
        }
    }

    /** assigns 'value' to the property at the end of 'path', navigating the path from 'obj' */
    private suspend fun assign(obj: TypedObject, path: List<String>, value: TypedObject) {
        var o = obj
        for (name in path.dropLast(1)) o = o.getPropertySuspend(name)
        o.setPropertySuspend(path.last(), value)
    }

    /** 'a.b.c' or '$self.a.b.c' as [a, b, c]; null if the expression is not such a path */
    private fun assignablePathOrNull(expression: String): List<String>? {
        val e = expression.trim().removePrefix("$SELF.")
        return when {
            PATH_REGEX.matches(e) -> e.split('.')
            else -> null
        }
    }

    private fun withIssues(item: DataItem, issues: IssueCollection<DataItemIssueLocation>): DataItem = when {
        issues.isEmpty() -> item
        else -> DataItemDefault(item.parent, item.identity, item.data, item.provenance, net.akehurst.kotlinx.issues.ram.IssueHolder(item.issues.all + issues.all))
    }

}

/** Location of an issue raised by a composition: the qualified identity of the item. */
data class CompositionIssueLocation(
    val itemIdentity: List<DataItemIdentity>
) : DataItemIssueLocation

/** Provenance of a DataItem created by adapting a source item through a DatastoreConnection. */
data class ConnectionProvenance(
    val source: DatastoreIdentity,
    val target: DatastoreIdentity,
    val connection: String
) : DataItemProvenance

data class DatastoreConnectionEndDefault(
    override val name: String,
    override val typeName: String,
    override val expression: String
) : DatastoreConnectionEnd

data class DatastoreConnectionDefault(
    override val source: DatastoreIdentity,
    override val target: DatastoreIdentity,
    override val targetType: String,
    override val end: List<DatastoreConnectionEnd>,
    override val name: String? = null
) : DatastoreConnection

data class DataItemDefault(
    override val parent: DataItem?,
    override val identity: DataItemIdentity,
    override val data: TypedObject,
    override val provenance: DataItemProvenance,
    override val issues: IssueCollection<DataItemIssueLocation> = net.akehurst.kotlinx.issues.ram.IssueHolder()
) : DataItem

class DatastoreCompositionConfigurationDefault(
    override val part: Map<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>,
    override val connection: List<DatastoreConnection> = emptyList()
) : DatastoreCompositionConfiguration {

}

class AccessCompositionConfigurationDefault(
    override val datastoreConfiguration: DatastoreCompositionConfiguration,
    override val part: Map<DatastoreIdentity, DatastoreConfiguration>
) : AccessCompositionConfiguration {

}