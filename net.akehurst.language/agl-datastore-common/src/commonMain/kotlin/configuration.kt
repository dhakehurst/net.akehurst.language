package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
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

    companion object {
        /** the namespace that holds the associations generated from the connections */
        const val COMPOSITION_NAMESPACE = "composition"
    }

    // cached, so the same types and accessor-mutator (which finds the association-owned properties) are used throughout
    private var _types: TypesDomain? = null
    private var _accessorMutator: ObjectGraphAccessorMutator? = null

    /**
     * for each connection, the target objects created by read/write, keyed by the source object.
     * The derived association end on the source type finds its target here.
     */
    private val _targetBySource = mutableMapOf<DatastoreConnection, MutableMap<Any, Any>>()

    override suspend fun types(): TypesDomain = _types ?: createTypes().also { _types = it }

    override suspend fun accessorMutator(): ObjectGraphAccessorMutator = _accessorMutator ?: ObjectGraphAccessorMutatorByReflection(
        typesDomain = types(),
        issues = issues,
        locationMap = LocationMapDefault(),
    ).also { _accessorMutator = it }

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
     *   FileHandle.parsableUnit : ParsableUnit?  (derived, the ParsableUnit created from that FileHandle)
     * Ends whose type is not a DataType (e.g. String) are constructor arguments, not association ends.
     * Both properties are owned by the association, so the parts' types are not changed.
     */
    private fun createAssociations(types: TypesDomain, ns: TypesNamespace, connection: DatastoreConnection) {
        val targetDef = types.findFirstDefinitionByPossiblyQualifiedNameOrNull(connection.targetType.asPossiblyQualifiedName) as? DataType
            ?: error("Connection '${connection.source}' -> '${connection.target}': target type '${connection.targetType}' is not a DataType")
        val targetEndName = targetDef.name.value.replaceFirstChar { it.lowercaseChar() }
        val targets = _targetBySource.getOrPut(connection) { mutableMapOf() }
        connection.end.forEach { connEnd ->
            val endDef = dataTypeOrNull(types, connEnd.typeName)
            if (null != endDef) {
                AssociationBuilder(ns, mutableListOf()).apply {
                    // end i becomes a property on the type of the other end
                    end(endDef.qualifiedName.value, setOf(REF, VAR, STR), connEnd.name, ownedByAssociation = true)
                    end(targetDef.qualifiedName.value, setOf(REF, VAL, DER), targetEndName, isNullable = true, byEvaluation = { _ -> { obj -> targets[obj] } }, ownedByAssociation = true)
                }.build()
            }
        }
    }

    private fun dataTypeOrNull(types: TypesDomain, typeName: String): DataType? =
        types.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName.asPossiblyQualifiedName) as? DataType

    /**
     * Reads the parts in order, passing each part's output to the next part's input.
     * If a connection is configured from the previous part to the next, each item is adapted through it.
     */
    override suspend fun read(configuration: AccessCompositionConfiguration): Flow<DataItem> {
        var previous: Datastore? = null
        var flow = emptyFlow<DataItem>()
        for (ds in datastore.values) {
            val dsCfg = configuration.part[ds.identity]
                ?: error("Can't find access configuration for datastore '${ds.identity}'")
            val prev = previous
            val conn = prev?.let { p -> this.configuration.connection.firstOrNull { it.source == p.identity && it.target == ds.identity } }
            val input = when {
                null != prev && null != conn -> adapt(conn, flow)
                else -> flow
            }
            flow = ds.read(dsCfg, input)
            previous = ds
        }
        return flow
    }

    /**
     * Creates the connection's target object for each source item.
     * Each end's expression is evaluated with $self = the source item's data.
     * Ends with a primitive type are constructor arguments, ends with a DataType are set through the association.
     */
    private suspend fun adapt(connection: DatastoreConnection, input: Flow<DataItem>): Flow<DataItem> {
        val types = types()
        val am = accessorMutator()
        val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(am)
        val targetType = connection.targetType.asPossiblyQualifiedName
        val (assocEnds, ctorEnds) = connection.end.partition { null != dataTypeOrNull(types, it.typeName) }
        val targets = _targetBySource.getOrPut(connection) { mutableMapOf() }
        return input.map { item ->
            val evc = EvaluationContext.ofSelf(item.data)
            val ctorArgs = ctorEnds.associate { end -> Pair(end.name, interpreter.evaluateStr(evc, end.expression)) }
            val data = am.createStructureValueSuspend(targetType, ctorArgs)
            assocEnds.forEach { end ->
                am.setProperty(data, end.name, interpreter.evaluateStr(evc, end.expression))
            }
            targets[item.data.self] = data.self
            DataItemDefault(
                parent = item,
                identity = item.identity,
                data = data,
                provenance = ConnectionProvenance(connection.source, connection.target)
            )
        }
    }

    override suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>) {
        TODO("Not yet implemented")
    }

}

/** Provenance of a DataItem created by adapting a source item through a DatastoreConnection. */
data class ConnectionProvenance(
    val source: DatastoreIdentity,
    val target: DatastoreIdentity
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
    override val end: List<DatastoreConnectionEnd>
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