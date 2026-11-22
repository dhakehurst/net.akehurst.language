package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionIdentity
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class test_DatastoreCompositionBuilder {

    private companion object {
        val IDENTITY = DatastoreProviderIdentity("test.Provider")

        val provider = object : DatastoreProvider {
            override val identity = IDENTITY
            override val schema = datastoreProviderSchema {
                datastore { parameter("url", "URL", true) { type("String") } }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val registry = DatastoreProviderRegistryDefault().also { it.register(provider) }
    }

    @Test
    fun identity_name_and_description() {
        val cfg = DatastoreCompositionBuilder("doors-reqs", registry).apply {
            name = "DOORS requirements"
            description = "what it is for"
            datastore("a", IDENTITY) { }
        }.build().configuration
        assertEquals(DatastoreCompositionIdentity("doors-reqs"), cfg.identity)
        assertEquals("DOORS requirements", cfg.name)
        assertEquals("what it is for", cfg.description)
    }

    @Test
    fun name_and_description_default_to_empty() {
        val cfg = DatastoreCompositionBuilder("c1", registry).apply {
            datastore("a", IDENTITY) { }
        }.build().configuration
        assertEquals(DatastoreCompositionIdentity("c1"), cfg.identity)
        assertEquals("", cfg.name)
        assertEquals("", cfg.description)
    }

    @Test
    fun invalid_identity_is_refused() {
        listOf("", "1a", "a b", "a.b").forEach { id ->
            val e = assertFailsWith<IllegalArgumentException>(id) { DatastoreCompositionBuilder(id, registry) }
            assertTrue(e.message!!.contains("Composition identity '$id' is not valid"), e.message)
        }
    }

    @Test
    fun default_class() {
        val cfg = DatastoreCompositionConfigurationDefault(DatastoreCompositionIdentity("c1"), emptyMap())
        assertEquals("", cfg.name)
        assertEquals("", cfg.description)
        assertEquals(emptyList(), cfg.connection)
    }
}
