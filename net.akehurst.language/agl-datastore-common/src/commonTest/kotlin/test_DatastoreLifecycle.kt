package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.use
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastoreLifecycle {

    private class RecordingDatastore(
        identity: DatastoreIdentity,
        configuration: DatastoreConfiguration,
        val log: MutableList<String>,
        val failOpen: Boolean = false,
    ) : DatastoreAbstract(identity, configuration) {
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}
        override suspend fun onOpen() {
            if (failOpen) error("cannot open ${identity.value}")
            log.add("open ${identity.value}")
        }

        override suspend fun onClose() {
            log.add("close ${identity.value}")
        }

        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
    }

    private companion object {
        val PROVIDER = DatastoreProviderIdentity("test.Recording")

        fun config(registry: DatastoreProviderRegistryDefault) = datastoreConfiguration(PROVIDER, registry) {}
        fun access(registry: DatastoreProviderRegistryDefault) = accessConfiguration(PROVIDER, registry) {}

        fun registry(log: MutableList<String>, failOpen: Set<String> = emptySet()) = DatastoreProviderRegistryDefault().also {
            it.register(object : DatastoreProvider {
                override val identity = PROVIDER
                override val configuration = datastoreProviderSchema {
                    datastore { }
                    access { }
                }

                override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
                    RecordingDatastore(datastoreIdentity, configuration, log, datastoreIdentity.value in failOpen)
            })
        }

        fun composition(registry: DatastoreProviderRegistryDefault, vararg ids: String): DatastoreCompositionDefault {
            val cfg = config(registry)
            val comp = DatastoreCompositionDefault(object : DatastoreCompositionConfiguration {
                override val part = ids.associate { Pair(PROVIDER, DatastoreIdentity(it)) to cfg }
                override val connection = emptyList<DatastoreConnection>()
            })
            comp.create(registry)
            return comp
        }

        fun accessFor(comp: DatastoreCompositionDefault, registry: DatastoreProviderRegistryDefault) = object : AccessCompositionConfiguration {
            override val datastoreConfiguration = comp.configuration
            override val part = comp.datastore.keys.associateWith { access(registry) }
        }
    }

    @Test
    fun read_when_not_open_fails() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log)
        val sut = RecordingDatastore(DatastoreIdentity("a"), config(reg), log)

        assertFalse(sut.isOpen)
        assertFailsWith<IllegalStateException> { sut.read(access(reg), emptyFlow()) }
        assertFailsWith<IllegalStateException> { sut.write(access(reg), emptyFlow()) }
    }

    @Test
    fun open_read_close() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log)
        val sut = RecordingDatastore(DatastoreIdentity("a"), config(reg), log)

        sut.open()
        assertTrue(sut.isOpen)
        assertEquals(emptyList(), sut.read(access(reg), emptyFlow()).toList())
        assertFailsWith<IllegalStateException> { sut.open() }
        sut.close()
        assertFalse(sut.isOpen)
        sut.close() // does nothing
        assertEquals(listOf("open a", "close a"), log)
    }

    @Test
    fun flow_collected_after_close_fails() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log)
        val sut = RecordingDatastore(DatastoreIdentity("a"), config(reg), log)

        sut.open()
        val flow = sut.read(access(reg), emptyFlow())
        sut.close()
        assertFailsWith<IllegalStateException> { flow.toList() }
    }

    @Test
    fun use_closes_even_if_block_throws() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log)
        val sut = RecordingDatastore(DatastoreIdentity("a"), config(reg), log)

        assertFailsWith<IllegalArgumentException> {
            sut.use { throw IllegalArgumentException("in block") }
        }
        assertFalse(sut.isOpen)
        assertEquals(listOf("open a", "close a"), log)
    }

    @Test
    fun composition_opens_in_chain_order_and_closes_in_reverse() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log)
        val sut = composition(reg, "a", "b", "c")

        assertFailsWith<IllegalStateException> { sut.read(accessFor(sut, reg)) }
        sut.use {
            assertTrue(it.isOpen)
            it.read(accessFor(it, reg)).toList()
        }
        assertFalse(sut.isOpen)
        assertEquals(listOf("open a", "open b", "open c", "close c", "close b", "close a"), log)
    }

    @Test
    fun composition_open_failure_closes_parts_already_opened() = runTest {
        val log = mutableListOf<String>()
        val reg = registry(log, failOpen = setOf("c"))
        val sut = composition(reg, "a", "b", "c")

        assertFailsWith<IllegalStateException> { sut.open() }
        assertFalse(sut.isOpen)
        assertEquals(listOf("open a", "open b", "close b", "close a"), log)
    }
}
