package net.akehurst.datastore.common

import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry


fun datastoreComposition(
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: DatastoreCompositionBuilder.() -> Unit
): DatastoreComposition {
    val b = DatastoreCompositionBuilder(registry)
    b.init()
    return b.build().also {
        it.create(registry)
    }
}

@DslMarker
annotation class DatastoreCompositionDslMarker

@DatastoreCompositionDslMarker
class DatastoreCompositionBuilder(
    private val _registry: DatastoreProviderRegistry
) {

    private val _cfg = mutableMapOf<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>()

    fun datastore(id: String, providerId: DatastoreProviderIdentity, init: DatastoreConfigurationBuilder.() -> Unit) {
        val b = DatastoreConfigurationBuilder(_registry,providerId, false)
        b.init()
        val ds = b.build()
        _cfg[Pair(providerId, DatastoreIdentity(id))] = ds
    }

    fun connect() {

    }

    fun build(): DatastoreComposition = DatastoreCompositionDefault(DatastoreCompositionConfigurationDefault(_cfg))
}

//--------------------------

fun DatastoreComposition.accessConfiguration(
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: AccessCompositeConfigurationBuilder.() -> Unit
) = accessCompositeConfiguration(this.configuration, registry, init)

fun accessCompositeConfiguration(
    datastoreConfiguration: DatastoreCompositionConfiguration,
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: AccessCompositeConfigurationBuilder.() -> Unit
): AccessCompositionConfiguration {
    val b = AccessCompositeConfigurationBuilder(datastoreConfiguration,registry)
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreCompositeConfigurationDslMarker

class AccessCompositeConfigurationBuilder(
    private val _datastoreConfiguration: DatastoreCompositionConfiguration,
    private val _registry: DatastoreProviderRegistry
) {

    private val _cfg = mutableMapOf<DatastoreIdentity, DatastoreConfiguration>()

    fun access(id: String,init: DatastoreConfigurationBuilder.() -> Unit) {
        check(_datastoreConfiguration.part.keys.any { it.second.value == id }) { "DatastoreComposition part with id '$id' not found." }
        val providerId = _datastoreConfiguration.part.keys.first { it.second.value == id }.first
        val b = DatastoreConfigurationBuilder(_registry,providerId, false)
        b.init()
        val acc = b.build()
        _cfg[DatastoreIdentity(id)] = acc
    }
    fun build(): AccessCompositionConfiguration = AccessCompositionConfigurationDefault(_datastoreConfiguration,_cfg)
}