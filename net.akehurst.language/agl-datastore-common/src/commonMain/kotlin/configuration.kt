package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.language.objectgraph.api.TypedObject

class DatastoreCompositionDefault(
    val configuration: Map<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>
) : DatastoreComposition {

    val datastore = mutableMapOf<DatastoreIdentity, Datastore>()

    fun create(registry: DatastoreProviderRegistry = DatastoreProviderRegistry.GLOBAL) {
        configuration.forEach { (ids, cfg) ->
            val (provId, dsId) = ids
            datastore[dsId] = registry.provide(provId, dsId, cfg)
        }
    }

    override fun read(): Flow<TypedObject> {
        TODO("Not yet implemented")
    }

    override fun write(input: Flow<TypedObject>) {
        TODO("Not yet implemented")
    }

}