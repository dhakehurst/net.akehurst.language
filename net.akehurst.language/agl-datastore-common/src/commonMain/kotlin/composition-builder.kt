package net.akehurst.datastore.common

import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
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

    private val _connection = mutableListOf<DatastoreConnection>()

    /**
     * Connect the output of datastore 'source' to the input of datastore 'target'.
     * Each source item is adapted to an instance of 'targetType', whose constructor arguments are given by expressions over the source item.
     */
    fun connect(source: String, target: String, targetType: String, init: DatastoreConnectionBuilder.() -> Unit) {
        check(_cfg.keys.any { it.second.value == source }) { "Datastore with id '$source' not defined (define datastores before connecting them)." }
        check(_cfg.keys.any { it.second.value == target }) { "Datastore with id '$target' not defined (define datastores before connecting them)." }
        val b = DatastoreConnectionBuilder(DatastoreIdentity(source), DatastoreIdentity(target), targetType)
        b.init()
        _connection.add(b.build())
    }

    fun build(): DatastoreComposition = DatastoreCompositionDefault(DatastoreCompositionConfigurationDefault(_cfg, _connection))
}

@DatastoreCompositionDslMarker
class DatastoreConnectionBuilder(
    private val _source: DatastoreIdentity,
    private val _target: DatastoreIdentity,
    private val _targetType: String,
) {
    private val _argument = mutableMapOf<String, String>()

    /** expression is evaluated with `$self` = the source item's data */
    fun argument(name: String, expression: String) {
        _argument[name] = expression
    }

    fun build(): DatastoreConnection = DatastoreConnectionDefault(_source, _target, _targetType, _argument)
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