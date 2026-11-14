package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.SecretReference
import net.akehurst.kotlinx.secure.storage.SecretSourceInMemory
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistryDefault
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_Secrets {

    private class SecretDatastore(
        identity: DatastoreIdentity,
        configuration: DatastoreConfiguration,
        secrets: SecretSourceRegistryDefault,
    ) : DatastoreAbstract(identity, configuration, secrets) {
        var passwordSeenOnOpen: Any? = null
        var tokenSeenOnRead: Any? = null
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}
        override suspend fun onOpen() {
            passwordSeenOnOpen = configuration.argument["password"]
        }

        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
            tokenSeenOnRead = accessConfiguration.argument["token"]
            return emptyFlow()
        }

        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
    }

    private companion object {
        val IDENTITY = DatastoreProviderIdentity("test.Secret")

        val provider = object : DatastoreProvider {
            override val identity = IDENTITY
            override val schema = datastoreProviderSchema {
                datastore {
                    parameter("url", "URL", false) { type("String") }
                    parameter("password", "Password", false) {
                        type("String")
                        sensitive()
                    }
                    parameter("pin", "PIN", true) {
                        type("Integer")
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

    private fun secrets(vararg secret: Pair<String, String>) = SecretSourceRegistryDefault().also {
        it.register(SecretSourceInMemory("mem", secret.toMap()))
    }

    private fun config() = datastoreConfiguration(IDENTITY, registry) {
        argument("url", "http://example.com")
        argument("password", secret("mem", "rm/david"))
    }

    @Test
    fun sensitive_parameter_can_be_a_reference() {
        val cfg = config()
        assertEquals(SecretReference("mem", "rm/david"), cfg.argument["password"])
        assertTrue(cfg.toString().contains("****"))
    }

    @Test
    fun non_sensitive_parameter_cannot_be_a_reference() {
        val e = assertFailsWith<IllegalStateException> {
            datastoreConfiguration(IDENTITY, registry) {
                argument("url", secret("mem", "url"))
                argument("password", "x")
            }
        }
        assertTrue(e.message!!.contains("not sensitive"), e.message)
    }

    @Test
    fun open_reads_the_secret_and_close_forgets_it() = runTest {
        val sut = SecretDatastore(DatastoreIdentity("ds"), config(), secrets("rm/david" to "s3cret"))

        sut.open()
        assertEquals("s3cret", sut.passwordSeenOnOpen)
        assertEquals("s3cret", sut.configuration.getAsString("password"))
        assertFalse(sut.configuration.toString().contains("s3cret"))

        sut.close()
        assertEquals(SecretReference("mem", "rm/david"), sut.configuration.argument["password"])
    }

    @Test
    fun open_fails_if_the_secret_is_missing() = runTest {
        val sut = SecretDatastore(DatastoreIdentity("ds"), config(), secrets())

        val e = assertFailsWith<IllegalStateException> { sut.open() }
        assertTrue(e.message!!.contains("not found"), e.message)
        assertFalse(sut.isOpen)
    }

    @Test
    fun open_fails_if_the_source_is_not_registered() = runTest {
        val sut = SecretDatastore(DatastoreIdentity("ds"), config(), SecretSourceRegistryDefault())

        val e = assertFailsWith<IllegalStateException> { sut.open() }
        assertTrue(e.message!!.contains("not registered"), e.message)
        assertFalse(sut.isOpen)
    }

    @Test
    fun secret_is_converted_to_the_parameter_type() = runTest {
        val cfg = datastoreConfiguration(IDENTITY, registry) {
            argument("url", "http://example.com")
            argument("password", "x")
            argument("pin", secret("mem", "pin"))
        }
        val sut = SecretDatastore(DatastoreIdentity("ds"), cfg, secrets("pin" to "1234"))

        sut.open()
        assertEquals(1234, sut.configuration.getAsInt("pin"))
    }

    @Test
    fun access_configuration_secret_is_read_by_read() = runTest {
        val sut = SecretDatastore(DatastoreIdentity("ds"), config(), secrets("rm/david" to "s3cret", "tok" to "t0k"))
        val acc = accessConfiguration(IDENTITY, registry) {
            argument("token", secret("mem", "tok"))
        }

        sut.open()
        sut.read(acc, emptyFlow()).toList()

        assertEquals("t0k", sut.tokenSeenOnRead)
    }
}
