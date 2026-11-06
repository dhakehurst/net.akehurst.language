package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class test_DatastoreProviderRegistry {

    private fun provider(id: String) = object : DatastoreProvider {
        override val identity = DatastoreProviderIdentity(id)
        override val schema = datastoreProviderSchema { }
        override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
    }

    @Test
    fun providers_empty() {
        val registry = DatastoreProviderRegistryDefault()
        assertTrue(registry.providers.isEmpty())
    }

    @Test
    fun providers_in_registration_order() {
        val registry = DatastoreProviderRegistryDefault()
        val b = provider("b")
        val a = provider("a")
        val c = provider("c")
        registry.register(b)
        registry.register(a)
        registry.register(c)
        assertEquals(listOf(b, a, c), registry.providers)
    }

    @Test
    fun providers_reregister_replaces_in_place() {
        val registry = DatastoreProviderRegistryDefault()
        val a1 = provider("a")
        val b = provider("b")
        val a2 = provider("a")
        registry.register(a1)
        registry.register(b)
        registry.register(a2)
        assertEquals(listOf(a2, b), registry.providers)
        assertSame(a2, registry.getOrNull(DatastoreProviderIdentity("a")))
    }

    @Test
    fun providers_is_a_snapshot() {
        val registry = DatastoreProviderRegistryDefault()
        val a = provider("a")
        registry.register(a)
        val list = registry.providers
        registry.register(provider("b"))
        assertEquals(listOf(a), list)
    }
}
