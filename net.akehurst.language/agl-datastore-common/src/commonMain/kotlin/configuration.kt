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
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderSchema
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectSuspending
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
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

    override suspend fun types(): TypesDomain =
        datastore.values.fold(typesDomain("Composition", true) {}) { acc, it ->
            acc.merge(it.types())
        }

    override suspend fun accessorMutator() = ObjectGraphAccessorMutatorByReflection(
        typesDomain = types(),
        issues = issues,
        locationMap = LocationMapDefault(),
    )

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
                null != prev && null != conn -> adapt(conn, prev, ds, flow)
                else -> flow
            }
            flow = ds.read(dsCfg, input)
            previous = ds
        }
        return flow
    }

    private suspend fun adapt(connection: DatastoreConnection, source: Datastore, target: Datastore, input: Flow<DataItem>): Flow<DataItem> {
        val sourceAm = source.accessorMutator()
        val targetAm = target.accessorMutator()
        val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(sourceAm)
        val targetType = connection.targetType.asPossiblyQualifiedName
        return input.map { item ->
            val evc = EvaluationContext.ofSelf(item.data)
            val args = connection.argument.mapValues { (_, expression) -> interpreter.evaluateStr(evc, expression) }
            val data = targetAm.createStructureValueSuspend(targetType, args)
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

data class DatastoreConnectionDefault(
    override val source: DatastoreIdentity,
    override val target: DatastoreIdentity,
    override val targetType: String,
    override val argument: Map<String, String>
) : DatastoreConnection

data class DataItemDefault(
    override val parent: DataItem?,
    override val identity: DataItemIdentity,
    override val data: TypedObject,
    override val provenance: DataItemProvenance
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