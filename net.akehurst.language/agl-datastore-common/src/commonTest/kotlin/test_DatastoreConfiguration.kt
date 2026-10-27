package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastoreConfiguration {

    private companion object {
        val IDENTITY = DatastoreProviderIdentity("test.Provider")

        val provider = object : DatastoreProvider {
            override val identity = IDENTITY
            override val configuration = datastoreProviderSchema {
                datastore {
                    parameter("url", "URL", false) { type("String") }
                    parameter("password", "Password", false) {
                        type("String")
                        sensitive()
                    }
                }
                access {
                    parameter("token", "API token", true) {
                        type("String")
                        sensitive()
                    }
                }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val registry = DatastoreProviderRegistryDefault().also { it.register(provider) }
    }

    @Test
    fun parameter_is_not_sensitive_by_default() {
        val url = provider.configuration.datastore.getParameterOrNull("url")!!
        assertFalse(url.sensitive)
    }

    @Test
    fun sensitive_parameter() {
        assertTrue(provider.configuration.datastore.getParameterOrNull("password")!!.sensitive)

        val cfg = datastoreConfiguration(IDENTITY, registry) {
            argument("url", "http://example.com")
            argument("password", "secret123")
        }

        assertTrue(cfg.isSensitive("password"))
        assertFalse(cfg.isSensitive("url"))
        assertEquals("secret123", cfg.getAsString("password"))
    }

    @Test
    fun toString_masks_sensitive_values() {
        val cfg = datastoreConfiguration(IDENTITY, registry) {
            argument("url", "http://example.com")
            argument("password", "secret123")
        }
        val acc = accessConfiguration(IDENTITY, registry) {
            argument("token", "tok456")
        }

        println(cfg)
        assertFalse(cfg.toString().contains("secret123"))
        assertTrue(cfg.toString().contains("http://example.com"))
        assertFalse(acc.toString().contains("tok456"))
        assertTrue(acc.isSensitive("token"))
    }
}
