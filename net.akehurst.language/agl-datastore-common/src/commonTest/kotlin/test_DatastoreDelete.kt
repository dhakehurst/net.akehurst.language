package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.transaction
import net.akehurst.datastore.api.use
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastoreDelete {

    /** a read-only datastore: it does not override supportsDelete or doDelete */
    private class ReadOnlyDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}
        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
    }

    /** stages deletions and applies them to 'items' on commit */
    private class DeletingDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
        val items = mutableSetOf("a", "b", "c")
        private val staged = mutableListOf<String>()

        override val supportsDelete: Boolean get() = true
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}
        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
        override suspend fun doDelete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> =
            input.map { staged.add(it.value); it }

        override suspend fun onCommit() {
            items.removeAll(staged.toSet())
            staged.clear()
        }

        override suspend fun onRollback() = staged.clear()
    }

    private companion object {
        val PROVIDER = DatastoreProviderIdentity("test.Delete")

        val registry = DatastoreProviderRegistryDefault().also {
            it.register(object : DatastoreProvider {
                override val identity = PROVIDER
                override val schema = datastoreProviderSchema {
                    datastore { }
                    access { }
                }

                override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
            })
        }

        fun config() = datastoreConfiguration(PROVIDER, registry) {}
        fun access() = accessConfiguration(PROVIDER, registry) {}
        fun ids(vararg v: String) = flowOf(*v.map { DataItemIdentity(it) }.toTypedArray())
    }

    @Test
    fun readOnly_doesNotSupportDelete() = runTest {
        val ds = ReadOnlyDatastore(DatastoreIdentity("ro"), config())
        assertFalse(ds.supportsDelete)
        ds.use {
            it.transaction { d ->
                assertFailsWith<UnsupportedOperationException> { d.delete(access(), ids("a")) }
            }
        }
    }

    @Test
    fun interfaceDefault_throwsUnsupported() = runTest {
        // a Datastore that does not extend DatastoreAbstract gets the interface defaults
        val ds = ReadOnlyDatastore(DatastoreIdentity("ro"), config())
        val plain: Datastore = object : Datastore by ds {
            override val supportsDelete: Boolean get() = super<Datastore>.supportsDelete
            override suspend fun delete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> =
                super<Datastore>.delete(accessConfiguration, input)
        }
        assertFalse(plain.supportsDelete)
        assertFailsWith<UnsupportedOperationException> { plain.delete(access(), ids("a")) }
    }

    @Test
    fun delete_appliedOnCommit() = runTest {
        val ds = DeletingDatastore(DatastoreIdentity("del"), config())
        assertTrue(ds.supportsDelete)
        ds.use {
            ds.beginTransaction()
            val deleted = ds.delete(access(), ids("a", "c")).toList()
            assertEquals(listOf(DataItemIdentity("a"), DataItemIdentity("c")), deleted)
            assertEquals(setOf("a", "b", "c"), ds.items) // staged, not applied
            ds.commit()
            assertEquals(setOf("b"), ds.items)
        }
    }

    @Test
    fun delete_discardedOnRollback() = runTest {
        val ds = DeletingDatastore(DatastoreIdentity("del"), config())
        ds.use {
            ds.beginTransaction()
            ds.delete(access(), ids("a")).toList()
            ds.rollback()
            assertEquals(setOf("a", "b", "c"), ds.items)
        }
    }

    @Test
    fun delete_failsOutsideTransaction() = runTest {
        val ds = DeletingDatastore(DatastoreIdentity("del"), config())
        ds.use {
            assertFailsWith<IllegalStateException> { ds.delete(access(), ids("a")) }
        }
    }

    @Test
    fun delete_failsWhenClosed() = runTest {
        val ds = DeletingDatastore(DatastoreIdentity("del"), config())
        assertFailsWith<IllegalStateException> { ds.delete(access(), ids("a")) }
    }

    @Test
    fun delete_flowCollectedAfterCommit_fails() = runTest {
        val ds = DeletingDatastore(DatastoreIdentity("del"), config())
        ds.use {
            ds.beginTransaction()
            val flow = ds.delete(access(), ids("a"))
            ds.commit()
            assertFailsWith<IllegalStateException> { flow.toList() }
            assertEquals(setOf("a", "b", "c"), ds.items)
        }
    }
}
