package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.PartialCommitException
import net.akehurst.datastore.api.TransactionState
import net.akehurst.datastore.api.transaction
import net.akehurst.datastore.api.use
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_DatastoreTransaction {

    /** records each lifecycle/transaction call; 'fail' names the calls that throw */
    private class TxDatastore(
        identity: DatastoreIdentity,
        configuration: DatastoreConfiguration,
        val log: MutableList<String>,
        val fail: Set<String>,
    ) : DatastoreAbstract(identity, configuration) {
        override suspend fun createTypes(): TypesDomain = typesDomain("Test", true) {}
        private fun record(what: String) {
            if (what in fail) error("$what ${identity.value} failed")
            log.add("$what ${identity.value}")
        }

        override suspend fun onBeginTransaction() = record("begin")
        override suspend fun onPrepare() = record("prepare")
        override suspend fun onCommit() = record("commit")
        override suspend fun onRollback() = record("rollback")
        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = emptyFlow()
    }

    private companion object {
        val PROVIDER = DatastoreProviderIdentity("test.Tx")

        /** 'fail' maps a datastore id to the calls that fail for it */
        fun registry(log: MutableList<String>, fail: Map<String, Set<String>> = emptyMap()) = DatastoreProviderRegistryDefault().also {
            it.register(object : DatastoreProvider {
                override val identity = PROVIDER
                override val configuration = datastoreProviderSchema {
                    datastore { }
                    access { }
                }

                override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
                    TxDatastore(datastoreIdentity, configuration, log, fail[datastoreIdentity.value] ?: emptySet())
            })
        }

        fun composition(registry: DatastoreProviderRegistryDefault, vararg ids: String): DatastoreCompositionDefault {
            val cfg = datastoreConfiguration(PROVIDER, registry) {}
            val comp = DatastoreCompositionDefault(object : DatastoreCompositionConfiguration {
                override val part = ids.associate { Pair(PROVIDER, DatastoreIdentity(it)) to cfg }
                override val connection = emptyList<DatastoreConnection>()
            })
            comp.create(registry)
            return comp
        }

        fun single(log: MutableList<String>, fail: Set<String> = emptySet()): Pair<Datastore, DatastoreConfiguration> {
            val reg = registry(log, mapOf("a" to fail))
            val ds = reg.provide(PROVIDER, DatastoreIdentity("a"), datastoreConfiguration(PROVIDER, reg) {})
            return Pair(ds, accessConfiguration(PROVIDER, reg) {})
        }
    }

    @Test
    fun write_outside_transaction_fails() = runTest {
        val log = mutableListOf<String>()
        val (sut, acc) = single(log)
        sut.use {
            assertFailsWith<IllegalStateException> { it.write(acc, emptyFlow()) }
        }
    }

    @Test
    fun transaction_prepares_then_commits() = runTest {
        val log = mutableListOf<String>()
        val (sut, acc) = single(log)
        sut.use {
            it.transaction { ds -> ds.write(acc, emptyFlow()).toList() }
            assertEquals(TransactionState.NONE, it.transactionState)
        }
        assertEquals(listOf("begin a", "prepare a", "commit a"), log)
    }

    @Test
    fun transaction_rolls_back_if_block_throws() = runTest {
        val log = mutableListOf<String>()
        val (sut, _) = single(log)
        sut.use {
            assertFailsWith<IllegalArgumentException> { it.transaction { throw IllegalArgumentException("in block") } }
            assertEquals(TransactionState.NONE, it.transactionState)
        }
        assertEquals(listOf("begin a", "rollback a"), log)
    }

    @Test
    fun close_rolls_back_active_transaction() = runTest {
        val log = mutableListOf<String>()
        val (sut, _) = single(log)
        sut.open()
        sut.beginTransaction()
        sut.close()
        assertEquals(listOf("begin a", "rollback a"), log)
    }

    @Test
    fun composition_commits_all() = runTest {
        val log = mutableListOf<String>()
        val sut = composition(registry(log), "a", "b")
        sut.use { it.transaction { } }
        assertEquals(listOf("begin a", "begin b", "prepare a", "prepare b", "commit a", "commit b"), log)
    }

    @Test
    fun composition_prepare_failure_rolls_back_all_and_commits_nothing() = runTest {
        val log = mutableListOf<String>()
        val sut = composition(registry(log, mapOf("b" to setOf("prepare"))), "a", "b", "c")
        sut.use {
            assertFailsWith<IllegalStateException> { it.transaction { } }
            assertEquals(TransactionState.NONE, it.transactionState)
        }
        assertEquals(listOf("begin a", "begin b", "begin c", "prepare a", "rollback c", "rollback b", "rollback a"), log)
    }

    @Test
    fun composition_commit_failure_after_a_commit_is_partial() = runTest {
        val log = mutableListOf<String>()
        val sut = composition(registry(log, mapOf("b" to setOf("commit"))), "a", "b", "c")
        sut.use {
            val e = assertFailsWith<PartialCommitException> { it.transaction { } }
            assertEquals(listOf("a"), e.committed.map { d -> d.value })
            assertEquals(listOf("b", "c"), e.notCommitted.map { d -> d.value })
        }
        assertEquals(
            listOf("begin a", "begin b", "begin c", "prepare a", "prepare b", "prepare c", "commit a", "rollback c", "rollback b"),
            log
        )
    }

    @Test
    fun composition_first_commit_failure_is_not_partial() = runTest {
        val log = mutableListOf<String>()
        val sut = composition(registry(log, mapOf("a" to setOf("commit"))), "a", "b")
        sut.use {
            val e = assertFailsWith<IllegalStateException> { it.transaction { } }
            assertEquals("commit a failed", e.message)
        }
    }
}
