package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry

fun datastoreConfiguration(
    providerIdentity: DatastoreProviderIdentity,
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: DatastoreConfigurationBuilder.() -> Unit
): DatastoreConfiguration {
    val b = DatastoreConfigurationBuilder(registry,providerIdentity, false)
    b.init()
    return b.build()
}

fun accessConfiguration(
    providerIdentity: DatastoreProviderIdentity,
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: DatastoreConfigurationBuilder.() -> Unit
): DatastoreConfiguration {
    val b = DatastoreConfigurationBuilder(registry,providerIdentity, true)
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreConfigurationDslMarker

@DatastoreProviderConfigurationDslMarker
class DatastoreConfigurationBuilder(
    private val _registry: DatastoreProviderRegistry,
    private val _providerIdentity: DatastoreProviderIdentity,
    private val _access:Boolean,
) {

    private val provider = _registry.getOrNull(_providerIdentity) ?: error("Provider not found: '${_providerIdentity.value}'")
    val _argument= mutableMapOf<String, Any>()

    fun argument(key: String, value: Any) {
        if (_access) {
            check(provider.configuration.access.getParameterOrNull(key) != null) { "Parameter '$key' is missing" }
        } else {
            check(provider.configuration.datastore.getParameterOrNull(key) != null) { "Parameter '$key' is missing" }
        }
        _argument[key] = value
    }

    fun build() = object : DatastoreConfiguration {
        override val providerIdentity = _providerIdentity
        override val argument = _argument
    }
}