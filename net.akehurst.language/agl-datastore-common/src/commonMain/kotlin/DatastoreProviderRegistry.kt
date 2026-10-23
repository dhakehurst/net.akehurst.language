package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry

class DatastoreProviderRegistryDefault() : DatastoreProviderRegistry {
    companion object {
        val GLOBAL = DatastoreProviderRegistryDefault()
    }

    private val _provider = mutableMapOf<DatastoreProviderIdentity, DatastoreProvider>()

    override fun register(provider: DatastoreProvider) {
        _provider[provider.identity] = provider
    }

    override fun getOrNull(providerIdentity: DatastoreProviderIdentity): DatastoreProvider? = _provider[providerIdentity]

    override fun provide(providerIdentity: DatastoreProviderIdentity, datastoreIdentity: DatastoreIdentity, datastoreConfiguration: DatastoreConfiguration): Datastore {
        val prov = _provider[providerIdentity] ?: error("Provider with identity '${providerIdentity.value}' not found.'")
        return prov.provide(datastoreIdentity, datastoreConfiguration)
    }
}