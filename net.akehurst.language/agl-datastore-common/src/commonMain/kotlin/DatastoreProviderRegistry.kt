package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity

class DatastoreProviderRegistry {
    companion object {
        val GLOBAL = DatastoreProviderRegistry()
    }

    private val _provider = mutableMapOf<DatastoreProviderIdentity, DatastoreProvider>()

    fun register(provider: DatastoreProvider) {
        _provider[provider.identity] = provider
    }

    fun provide(providerIdentity: DatastoreProviderIdentity, datastoreIdentity: DatastoreIdentity, datastoreConfiguration: DatastoreConfiguration): Datastore {
        val prov = _provider[providerIdentity] ?: error("Provider with identity '${providerIdentity.value}' not found.'")
        return prov.provide(datastoreIdentity, datastoreConfiguration)
    }
}