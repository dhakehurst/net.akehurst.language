package net.akehurst.datastore.common

import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreConnectionEnd
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
     * Each source item is adapted to an instance of 'targetType', whose constructor arguments are given by the connection's ends.
     * 'name' is needed when there are several connections from 'source' to 'target' with the same 'targetType' (fan-out),
     * see DatastoreConnection.identity.
     */
    fun connect(source: String, target: String, targetType: String, name: String? = null, init: DatastoreConnectionBuilder.() -> Unit) {
        check(_cfg.keys.any { it.second.value == source }) { "Datastore with id '$source' not defined (define datastores before connecting them)." }
        check(_cfg.keys.any { it.second.value == target }) { "Datastore with id '$target' not defined (define datastores before connecting them)." }
        val b = DatastoreConnectionBuilder(DatastoreIdentity(source), DatastoreIdentity(target), targetType, name)
        b.init()
        val conn = b.build()
        check(_connection.none { it.source == conn.source && it.target == conn.target && it.identity == conn.identity }) {
            "A connection from '$source' to '$target' with identity '${conn.identity}' is already defined, give the connections different names."
        }
        _connection.add(conn)
    }

    fun build(): DatastoreComposition = DatastoreCompositionDefault(DatastoreCompositionConfigurationDefault(_cfg, _connection))
}

@DatastoreCompositionDslMarker
class DatastoreConnectionBuilder(
    private val _source: DatastoreIdentity,
    private val _target: DatastoreIdentity,
    private val _targetType: String,
    private val _name: String? = null,
) {
    private val _end = mutableListOf<DatastoreConnectionEnd>()

    /**
     * name: the targetType constructor argument
     * typeName: (possibly qualified) name of the type of the expression result
     * expression: evaluated with `$self` = the source item's data
     */
    fun end(name: String, typeName: String, expression: String) {
        check(_end.none { it.name == name }) { "Connection end '$name' already defined" }
        _end.add(DatastoreConnectionEndDefault(name, typeName, expression))
    }

    fun build(): DatastoreConnection = DatastoreConnectionDefault(_source, _target, _targetType, _end, _name)
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
        val b = DatastoreConfigurationBuilder(_registry, providerId, true)
        b.init()
        val acc = b.build()
        _cfg[DatastoreIdentity(id)] = acc
    }
    fun build(): AccessCompositionConfiguration = AccessCompositionConfigurationDefault(_datastoreConfiguration,_cfg)
}