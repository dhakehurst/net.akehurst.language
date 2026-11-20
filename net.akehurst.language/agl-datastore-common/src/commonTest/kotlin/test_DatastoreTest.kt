package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionIdentity
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreTestResult
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastoreTest {

    /** how a TestStore behaves */
    private class Behaviour(
        val failOpen: Boolean = false,
        /** if not null, read emits this many items of its own (and ignores its input); otherwise it passes its input on */
        val sourceCount: Int? = null,
        /** if true, read fails on its first input item */
        val failOnInput: Boolean = false,
        /** if true, read() itself fails */
        val failRead: Boolean = false,
    ) {
        /** the number of items the source has emitted, i.e. read from its store */
        var emitted = 0
        var opened = 0
        var closed = 0
    }

    private class TestStore(
        identity: DatastoreIdentity,
        configuration: DatastoreConfiguration,
        val behaviour: Behaviour,
    ) : DatastoreAbstract(identity, configuration) {
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}

        override suspend fun onOpen() {
            if (behaviour.failOpen) error("cannot open ${identity.value}")
            behaviour.opened++
        }

        override suspend fun onClose() {
            behaviour.closed++
        }

        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
            if (behaviour.failRead) error("cannot read ${identity.value}")
            val n = behaviour.sourceCount
            return when {
                null != n -> flow {
                    val am = accessorMutator()
                    for (i in 1..n) {
                        behaviour.emitted++
                        val obj = "item$i"
                        emit(DataItemDefault(null, DataItemIdentity(obj), am.toTypedObject(obj, StdLibDefault.AnyType), provenanceFor(null, obj)))
                    }
                }

                else -> input.map {
                    if (behaviour.failOnInput) error("cannot process input of ${identity.value}")
                    it
                }
            }
        }

        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    }

    private companion object {
        val PROVIDER = DatastoreProviderIdentity("test.Test")

        fun registry(behaviours: Map<String, Behaviour>) = DatastoreProviderRegistryDefault().also {
            it.register(object : DatastoreProvider {
                override val identity = PROVIDER
                override val schema = datastoreProviderSchema {
                    datastore { }
                    access { }
                }

                override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
                    TestStore(datastoreIdentity, configuration, behaviours[datastoreIdentity.value]!!)
            })
        }

        fun store(id: String, behaviour: Behaviour): Pair<TestStore, DatastoreConfiguration> {
            val reg = registry(mapOf(id to behaviour))
            val ds = reg.provide(PROVIDER, DatastoreIdentity(id), datastoreConfiguration(PROVIDER, reg) {}) as TestStore
            return Pair(ds, accessConfiguration(PROVIDER, reg) {})
        }

        /** a chain of parts in the order of 'behaviours' */
        fun composition(behaviours: Map<String, Behaviour>): Pair<DatastoreCompositionDefault, AccessCompositionConfiguration> {
            val reg = registry(behaviours)
            val cfg = datastoreConfiguration(PROVIDER, reg) {}
            val comp = DatastoreCompositionDefault(object : DatastoreCompositionConfiguration {
                override val identity = DatastoreCompositionIdentity("test")
                override val name = ""
                override val description = ""
                override val part = behaviours.keys.associate { Pair(PROVIDER, DatastoreIdentity(it)) to cfg }
                override val connection = emptyList<DatastoreConnection>()
            })
            comp.create(reg)
            val acc = accessConfiguration(PROVIDER, reg) {}
            val access = object : AccessCompositionConfiguration {
                override val datastoreConfiguration = comp.configuration
                override val part = comp.datastore.keys.associateWith { acc }
            }
            return Pair(comp, access)
        }

        fun ids(vararg id: String) = id.map { DatastoreIdentity(it) }
    }

    // --- DatastoreAbstract defaults ---

    @Test
    fun testConnection_success_opens_and_closes() = runTest {
        val b = Behaviour()
        val (sut, _) = store("a", b)

        val actual = sut.testConnection()

        assertEquals(DatastoreTestResult(true, "Connected"), actual)
        assertFalse(sut.isOpen)
        assertEquals(1, b.opened)
        assertEquals(1, b.closed)
    }

    @Test
    fun testConnection_failure_is_a_result() = runTest {
        val (sut, _) = store("a", Behaviour(failOpen = true))

        val actual = sut.testConnection()

        assertEquals(DatastoreTestResult(false, "cannot open a"), actual)
        assertFalse(sut.isOpen)
    }

    @Test
    fun testConnection_when_open_leaves_it_open() = runTest {
        val b = Behaviour()
        val (sut, _) = store("a", b)
        sut.open()

        val actual = sut.testConnection()

        assertTrue(actual.success)
        assertTrue(sut.isOpen)
        assertEquals(0, b.closed)
        sut.close()
    }

    @Test
    fun testAccess_success() = runTest {
        val b = Behaviour(sourceCount = 5)
        val (sut, acc) = store("a", b)

        val actual = sut.testAccess(acc)

        assertEquals(DatastoreTestResult(true, "Read 1 item(s)", itemCount = 1), actual)
        assertFalse(sut.isOpen)
        assertEquals(1, b.closed)
    }

    @Test
    fun testAccess_respects_limit() = runTest {
        val b = Behaviour(sourceCount = 10)
        val (sut, acc) = store("a", b)

        val actual = sut.testAccess(acc, limit = 3)

        assertTrue(actual.success)
        assertEquals(3, actual.itemCount)
        assertEquals(3, b.emitted)
    }

    @Test
    fun testAccess_no_items_succeeds_with_warning() = runTest {
        val (sut, acc) = store("a", Behaviour(sourceCount = 0))

        val actual = sut.testAccess(acc, limit = 3)

        assertTrue(actual.success)
        assertEquals(0, actual.itemCount)
        assertEquals(listOf("No items were found"), actual.warnings)
    }

    @Test
    fun testAccess_failure_is_a_result() = runTest {
        val b = Behaviour(failRead = true)
        val (sut, acc) = store("a", b)

        val actual = sut.testAccess(acc, emptyFlow())

        assertEquals(DatastoreTestResult(false, "cannot read a"), actual)
        assertFalse(sut.isOpen)
        assertEquals(1, b.closed)
    }

    @Test
    fun testAccess_open_failure_is_a_result() = runTest {
        val (sut, acc) = store("a", Behaviour(failOpen = true, sourceCount = 1))

        val actual = sut.testAccess(acc)

        assertEquals(DatastoreTestResult(false, "cannot open a"), actual)
    }

    // --- DatastoreCompositionDefault ---

    @Test
    fun composition_testConnection_gives_a_result_per_part_in_chain_order() = runTest {
        val (sut, _) = composition(linkedMapOf("a" to Behaviour(), "b" to Behaviour(failOpen = true), "c" to Behaviour()))

        val actual = sut.testConnection()

        assertEquals(ids("a", "b", "c"), actual.keys.toList())
        assertEquals(DatastoreTestResult(true, "Connected"), actual[DatastoreIdentity("a")])
        assertEquals(DatastoreTestResult(false, "cannot open b"), actual[DatastoreIdentity("b")])
        assertEquals(DatastoreTestResult(true, "Connected"), actual[DatastoreIdentity("c")])
        assertFalse(sut.isOpen)
    }

    @Test
    fun composition_testAccess_success() = runTest {
        val src = Behaviour(sourceCount = 5)
        val mid = Behaviour()
        val (sut, acc) = composition(linkedMapOf("src" to src, "mid" to mid))

        val actual = sut.testAccess(acc, limit = 2)

        assertEquals(ids("src", "mid"), actual.keys.toList())
        assertEquals(DatastoreTestResult(true, "Read 2 item(s)", itemCount = 2), actual[DatastoreIdentity("src")])
        assertEquals(DatastoreTestResult(true, "Gave 2 item(s)", itemCount = 2), actual[DatastoreIdentity("mid")])
        assertFalse(sut.isOpen)
        assertEquals(1, src.closed)
        assertEquals(1, mid.closed)
    }

    @Test
    fun composition_testAccess_downstream_failure_reports_later_parts_not_tested() = runTest {
        val src = Behaviour(sourceCount = 5)
        val end = Behaviour()
        val (sut, acc) = composition(linkedMapOf("src" to src, "mid" to Behaviour(failOnInput = true), "end" to end))

        val actual = sut.testAccess(acc, limit = 3)

        assertEquals(ids("src", "mid", "end"), actual.keys.toList())
        assertTrue(actual[DatastoreIdentity("src")]!!.success)
        assertEquals(DatastoreTestResult(false, "cannot process input of mid", itemCount = 0), actual[DatastoreIdentity("mid")])
        assertEquals(DatastoreTestResult(false, "Not tested: an earlier part failed"), actual[DatastoreIdentity("end")])
        assertEquals(1, end.closed)
        assertTrue(src.emitted <= 3)
    }

    @Test
    fun composition_testAccess_open_failure_reports_later_parts_not_tested() = runTest {
        val a = Behaviour(sourceCount = 1)
        val (sut, acc) = composition(linkedMapOf("a" to a, "b" to Behaviour(failOpen = true), "c" to Behaviour()))

        val actual = sut.testAccess(acc)

        assertEquals(DatastoreTestResult(true, "Read 1 item(s)", itemCount = 1), actual[DatastoreIdentity("a")])
        assertEquals(DatastoreTestResult(false, "cannot open b"), actual[DatastoreIdentity("b")])
        assertEquals(DatastoreTestResult(false, "Not tested: an earlier part failed"), actual[DatastoreIdentity("c")])
        assertEquals(1, a.closed)
    }

    @Test
    fun composition_testAccess_never_reads_the_source_past_the_limit() = runTest {
        val src = Behaviour(sourceCount = 100)
        val (sut, acc) = composition(linkedMapOf("src" to src, "mid" to Behaviour(), "end" to Behaviour()))

        val actual = sut.testAccess(acc, limit = 3)

        assertEquals(3, src.emitted)
        actual.values.forEach { assertEquals(3, it.itemCount) }
    }

    @Test
    fun composition_testAccess_when_open_leaves_it_open() = runTest {
        val src = Behaviour(sourceCount = 2)
        val (sut, acc) = composition(linkedMapOf("src" to src))
        sut.open()

        val actual = sut.testAccess(acc)

        assertTrue(actual[DatastoreIdentity("src")]!!.success)
        assertTrue(sut.isOpen)
        assertEquals(0, src.closed)
        sut.close()
    }
}
