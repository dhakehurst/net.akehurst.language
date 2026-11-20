package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreLocation
import net.akehurst.datastore.api.DatastorePlatform
import net.akehurst.datastore.api.DatastorePlatformNotSupportedException
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastorePlatforms {

    private class TestProvider(id: String, override val supportedPlatforms: Set<DatastorePlatform>) : DatastoreProvider {
        override val identity = DatastoreProviderIdentity(id)
        override val schema = datastoreProviderSchema {
            datastore { parameter("p", "P", true) { type("String") } }
            access { parameter("a", "A", true) { type("String") } }
        }
        var provided = 0
        override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore {
            provided++
            error("not used")
        }
    }

    private val everywhere = TestProvider("test.Everywhere", DatastorePlatform.ALL)
    private val nowhere = TestProvider("test.Nowhere", emptySet())
    private val here = TestProvider("test.Here", setOf(DatastorePlatform.current))
    private val elsewhere = TestProvider("test.Elsewhere", DatastorePlatform.ALL - DatastorePlatform.current)

    private val registry = DatastoreProviderRegistryDefault().also { r -> listOf(everywhere, nowhere, here, elsewhere).forEach { r.register(it) } }

    private fun cfg(provider: DatastoreProvider, location: DatastoreLocation = DatastoreLocation.ANY) =
        datastoreConfiguration(provider.identity, registry) { this.location = location }

    @Test
    fun default_supportedPlatforms_is_all() {
        val p = object : DatastoreProvider {
            override val identity = DatastoreProviderIdentity("test.Default")
            override val schema = datastoreProviderSchema { datastore { }; access { } }
            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }
        assertEquals(DatastorePlatform.ALL, p.supportedPlatforms)
        assertTrue(p.isSupportedOnCurrentPlatform)
    }

    @Test
    fun isSupportedOnCurrentPlatform() {
        assertTrue(everywhere.isSupportedOnCurrentPlatform)
        assertTrue(here.isSupportedOnCurrentPlatform)
        assertFalse(nowhere.isSupportedOnCurrentPlatform)
        assertFalse(elsewhere.isSupportedOnCurrentPlatform)
    }

    @Test
    fun registry_provide_throws_on_unsupported_platform() {
        val ex = assertFailsWith<DatastorePlatformNotSupportedException> {
            registry.provide(elsewhere.identity, DatastoreIdentity("ds"), cfg(elsewhere))
        }
        assertEquals(DatastorePlatform.current, ex.platform)
        assertEquals(elsewhere.supportedPlatforms, ex.supportedPlatforms)
        assertTrue(ex.message!!.contains("test.Elsewhere"), ex.message)
        assertEquals(0, elsewhere.provided, "the provider is not asked")
    }

    @Test
    fun registry_provide_on_supported_platform_asks_the_provider() {
        assertFailsWith<IllegalStateException> { registry.provide(here.identity, DatastoreIdentity("ds"), cfg(here)) }
        assertEquals(1, here.provided)
    }

    @Test
    fun location_allows() {
        assertTrue(DatastoreLocation.ANY.allows(DatastoreRole.CLIENT))
        assertTrue(DatastoreLocation.ANY.allows(DatastoreRole.SERVER))
        assertTrue(DatastoreLocation.CLIENT.allows(DatastoreRole.CLIENT))
        assertFalse(DatastoreLocation.CLIENT.allows(DatastoreRole.SERVER))
        assertFalse(DatastoreLocation.SERVER.allows(DatastoreRole.CLIENT))
        assertTrue(DatastoreLocation.SERVER.allows(DatastoreRole.SERVER))
    }

    @Test
    fun canRunHere() {
        assertTrue(everywhere.canRunHere(cfg(everywhere), DatastoreRole.CLIENT))
        assertTrue(everywhere.canRunHere(cfg(everywhere, DatastoreLocation.SERVER), DatastoreRole.SERVER))
        assertFalse(everywhere.canRunHere(cfg(everywhere, DatastoreLocation.SERVER), DatastoreRole.CLIENT))
        assertFalse(everywhere.canRunHere(cfg(everywhere, DatastoreLocation.CLIENT), DatastoreRole.SERVER))
        assertFalse(elsewhere.canRunHere(cfg(elsewhere), DatastoreRole.CLIENT))
        assertFalse(elsewhere.canRunHere(cfg(elsewhere), DatastoreRole.SERVER))
        // through the registry
        assertTrue(registry.canRunHere(cfg(here, DatastoreLocation.CLIENT), DatastoreRole.CLIENT))
        assertFalse(registry.canRunHere(cfg(elsewhere), DatastoreRole.CLIENT))
        assertFalse(DatastoreProviderRegistryDefault().canRunHere(cfg(everywhere), DatastoreRole.CLIENT), "provider not registered")
    }

    @Test
    fun location_default_is_any_and_is_kept_by_builder() {
        assertEquals(DatastoreLocation.ANY, cfg(everywhere).location)
        assertEquals(DatastoreLocation.SERVER, cfg(everywhere, DatastoreLocation.SERVER).location)
        assertTrue(cfg(everywhere, DatastoreLocation.SERVER).toString().contains("location=SERVER"))
    }

    @Test
    fun location_in_access_configuration_fails() {
        assertFailsWith<IllegalStateException> {
            accessConfiguration(everywhere.identity, registry) { location = DatastoreLocation.CLIENT }
        }
        assertEquals(DatastoreLocation.ANY, accessConfiguration(everywhere.identity, registry) { }.location)
    }

    @Test
    fun composition_supportedPlatforms_and_canRunHere() {
        val c = DatastoreCompositionBuilder("c", registry).apply {
            datastore("a", everywhere.identity) { location = DatastoreLocation.SERVER }
            datastore("b", here.identity) { }
        }.build().configuration
        assertEquals(setOf(DatastorePlatform.current), c.supportedPlatforms(registry))
        assertTrue(c.canRunHere(registry, DatastoreRole.SERVER))
        assertFalse(c.canRunHere(registry, DatastoreRole.CLIENT), "part 'a' is server only")

        val d = DatastoreCompositionBuilder("d", registry).apply {
            datastore("a", everywhere.identity) { }
            datastore("b", elsewhere.identity) { }
        }.build().configuration
        assertEquals(DatastorePlatform.ALL - DatastorePlatform.current, d.supportedPlatforms(registry))
        assertFalse(d.canRunHere(registry, DatastoreRole.SERVER))
        assertEquals(emptySet(), d.supportedPlatforms(DatastoreProviderRegistryDefault()), "providers not registered")
    }

    @Test
    fun json_saves_and_loads_location() {
        val c = DatastoreCompositionBuilder("c", registry).apply {
            datastore("a", everywhere.identity) { location = DatastoreLocation.SERVER }
            datastore("b", here.identity) { }
        }.build().configuration
        val text = DatastoreConfigurationJson.encode(c)
        assertTrue(text.contains("\"location\": \"SERVER\""), text)
        assertTrue(text.contains("\"location\": \"ANY\""), text)
        val loaded = DatastoreConfigurationJson.decode(text, registry)
        assertEquals(listOf(DatastoreLocation.SERVER, DatastoreLocation.ANY), loaded.part.values.map { it.location })
    }

    @Test
    fun json_without_location_is_any_and_bad_location_fails() {
        val text = """{ "composition": { "id": "c", "part": [ { "provider": "test.Everywhere", "id": "a", "configuration": { } } ] } }"""
        assertEquals(DatastoreLocation.ANY, DatastoreConfigurationJson.decode(text, registry).part.values.single().location)
        val bad = """{ "composition": { "id": "c", "part": [ { "provider": "test.Everywhere", "id": "a", "location": "MOON", "configuration": { } } ] } }"""
        val ex = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decode(bad, registry) }
        assertTrue(ex.message!!.contains("MOON"), ex.message)
    }
}
