package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.fold
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderSchema
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
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

    override suspend fun read(configuration: AccessCompositionConfiguration): Flow<DataItem> {
        return datastore.values.asFlow().fold(emptyFlow<DataItem>()) { input, ds ->
            val dsCfg = configuration.part[ds.identity]
                ?: error("Can't find access configuration for datastore '${ds.identity}'")
            ds.read(dsCfg, input)
        }
    }

    override suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>) {
        TODO("Not yet implemented")
    }

}

data class DataItemDefault(
    override val parent: DataItem?,
    override val identity: DataItemIdentity,
    override val data: TypedObject,
    override val provenance: DataItemProvenance
) : DataItem

class DatastoreCompositionConfigurationDefault(
    override val part: Map<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>
) : DatastoreCompositionConfiguration {

}

class AccessCompositionConfigurationDefault(
    override val datastoreConfiguration: DatastoreCompositionConfiguration,
    override val part: Map<DatastoreIdentity, DatastoreConfiguration>
) : AccessCompositionConfiguration {

}