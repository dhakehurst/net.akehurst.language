package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DsConfigParameterDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_DsConfigParameterDefault {

    private companion object {
        val ID = DatastoreProviderIdentity("test.Defaults")

        val provider = object : DatastoreProvider {
            override val identity = ID
            override val schema = datastoreProviderSchema {
                datastore {
                    parameter("url", "URL", false) { type("String") }
                    parameter("timeout", "Timeout", false) { type("Integer"); default(30L) }
                    group("Advanced", advanced = true) {
                        parameter("strict", "Strict", true) { type("Boolean"); default(true) }
                        parameter("ratio", "Ratio", true) { type("Real"); default(0.5) }
                        parameter("encoding", "Encoding", true) { type("String") }
                    }
                }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val registry = DatastoreProviderRegistryDefault().also { it.register(provider) }
    }

    @Test
    fun no_default_by_default() {
        val def = object : DsConfigParameterDefinition {
            override val identity = "x"
            override val name = "X"
            override val optional = true
            override val description = ""
            override val type = provider.schema.datastore.getParameterOrNull("url")!!.type
        }
        assertNull(def.defaultValue)
        assertNull(provider.schema.datastore.getParameterOrNull("encoding")!!.defaultValue)
    }

    @Test
    fun schema_holds_the_default() {
        assertEquals(30L, provider.schema.datastore.getParameterOrNull("timeout")!!.defaultValue)
        assertEquals(true, provider.schema.datastore.getParameterOrNull("strict")!!.defaultValue)
    }

    @Test
    fun default_must_be_of_the_parameter_type() {
        assertFailsWith<IllegalStateException> {
            datastoreProviderSchema {
                datastore { parameter("timeout", "Timeout", true) { type("Integer"); default("thirty") } }
                access { }
            }
        }
    }

    @Test
    fun configuration_falls_back_to_the_default() {
        val cfg = datastoreConfiguration(ID, registry) {
            argument("url", "http://x")
        }
        // a required parameter with a default may be left out
        assertFalse(cfg.argument.containsKey("timeout"))
        assertEquals(30, cfg.getAsInt("timeout"))
        assertEquals(30L, cfg.getAsLongOrNull("timeout"))
        assertTrue(cfg.getAsBoolean("strict"))
        assertEquals(0.5, cfg.getAsDouble("ratio"))
        assertNull(cfg.getAsStringOrNull("encoding"))
        assertEquals(30L, cfg.valueOf("timeout"))
        assertEquals(30L, cfg.defaultValueOf("timeout"))
    }

    @Test
    fun argument_overrides_the_default() {
        val cfg = datastoreConfiguration(ID, registry) {
            argument("url", "http://x")
            argument("timeout", 5L)
            argument("strict", false)
        }
        assertEquals(5, cfg.getAsInt("timeout"))
        assertFalse(cfg.getAsBoolean("strict"))
    }

    @Test
    fun required_parameter_without_default_is_still_required() {
        assertFailsWith<IllegalStateException> {
            datastoreConfiguration(ID, registry) { }
        }
    }

    @Test
    fun argument_holds_only_the_given_values() {
        val cfg = datastoreConfiguration(ID, registry) { argument("url", "http://x") }
        assertEquals(setOf("url"), cfg.argument.keys)
        assertEquals(30, cfg.getAsInt("timeout"))
    }
}
