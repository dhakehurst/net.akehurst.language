package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity


fun datastoreComposition(init: DatastoreCompositionBuilder.() -> Unit): DatastoreComposition {
    val b = DatastoreCompositionBuilder()
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreCompositionDslMarker

@DatastoreCompositionDslMarker
class DatastoreCompositionBuilder() {

    private val _cfg = mutableMapOf<Pair<DatastoreProviderIdentity,DatastoreIdentity>, DatastoreConfiguration>()

    fun datastore(id:String, providerId: DatastoreProviderIdentity, init: DatastoreConfigurationBuilder.() -> Unit) {
        val b = DatastoreConfigurationBuilder()
        b.init()
        val ds = b.build()
        _cfg[Pair(providerId,DatastoreIdentity(id))] = ds
    }

    fun connect() {

    }

    fun build(): DatastoreComposition = DatastoreCompositionDefault(_cfg)
}

//--------------------------

fun datastoreCompositeConfiguration(init: DatastoreCompositeConfigurationBuilder.() -> Unit): DatastoreCompositeConfiguration {
    val b = DatastoreCompositeConfigurationBuilder()
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreCompositeConfigurationDslMarker

class DatastoreCompositeConfigurationBuilder() {

    private val _cfg = mutableMapOf<DatastoreIdentity, DatastoreConfiguration>()

    fun build(): DatastoreCompositeConfiguration = DatastoreCompositeConfigurationDefault(_cfg)
}