package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreComposition
import net.akehurst.datastore.api.DatastoreCompositionExt.use
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DatastoreCompositionExt.accessConfiguration
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.kotlinx.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A composition source -> target, where the connection has a constructor end.
 * The source emits error items (e.g. as ZipDatastore does for a file that is not a zip archive);
 * they must not reach the target's constructor, and must reach the composition's output with their issues.
 */
class test_DatastoreCompositionErrorItem {

    class Source(val text: String)
    class Target(val text: String)

    private object Loc : DataItemIssueLocation

    /** what the source emits: null text = an error item with no data; 'error' = an error issue on the item */
    private data class Emit(val id: String, val text: String?, val error: String? = null)

    private class SourceDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration, val emits: List<Emit>) : DatastoreAbstract(identity, configuration) {
        override suspend fun createTypes(): TypesDomain = typesDomain("Src", true) {
            namespace("src") {
                data("Source", implementation = Source::class) {
                    propertyOfWithBinding<Source, String>(setOf(REF, VAL), "text", "String", accessor = { it.text })
                }
            }
        }

        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = flow {
            val am = accessorMutator()
            emits.forEach { e ->
                val data = e.text?.let { am.toTypedObject(Source(it), StdLibDefault.AnyType) } ?: am.nothing()
                val issues = IssueHolder<DataItemIssueLocation>(e.error?.let { listOf(IssueDefault(IssueKind.ERROR, Loc, it)) } ?: emptyList())
                emit(DataItemDefault(null, DataItemIdentity(e.id), data, provenanceFor(null, e.id), issues))
            }
        }

        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    }

    /** passes its input on; fails if an input is not a Target */
    private class TargetDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
        val received = mutableListOf<DataItem>()

        override suspend fun createTypes(): TypesDomain = typesDomain("Tgt", true) {
            namespace("tgt") {
                data("Target", implementation = Target::class) {
                    constructor_ {
                        parameter(setOf(REF, VAL), "text", "String")
                        execution { args -> Target(args[0] as String) }
                    }
                }
            }
        }

        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input.map {
            received.add(it)
            assertIs<Target>(it.data.self, "the target datastore got an input that is not a Target: $it")
            it
        }

        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    }

    private companion object {
        val PROVIDER_SRC = DatastoreProviderIdentity("test.error.Source")
        val PROVIDER_TGT = DatastoreProviderIdentity("test.error.Target")

        fun provider(id: DatastoreProviderIdentity, create: (DatastoreIdentity, DatastoreConfiguration) -> Datastore) = object : DatastoreProvider {
            override val identity = id
            override val schema = datastoreProviderSchema {
                datastore { }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = create(datastoreIdentity, configuration)
        }
    }

    private class Fixture(emits: List<Emit>, textExpression: String = "text") {
        var target: TargetDatastore? = null
        val registry = DatastoreProviderRegistryDefault().also {
            it.register(provider(PROVIDER_SRC) { i, c -> SourceDatastore(i, c, emits) })
            it.register(provider(PROVIDER_TGT) { i, c -> TargetDatastore(i, c).also { t -> target = t } })
        }
        val composition: DatastoreComposition = datastoreComposition("errors", registry) {
            datastore("src", PROVIDER_SRC) {}
            datastore("tgt", PROVIDER_TGT) {}
            connect("src", "tgt", "tgt.Target") {
                end("text", "String", textExpression)
            }
        }
        val access = composition.accessConfiguration(registry) {
            access("src") {}
            access("tgt") {}
        }

        suspend fun read(): List<DataItem> = composition.use { it.read(access).toList() }
    }

    @Test
    fun error_item_with_no_data_reaches_output_and_not_target() = runTest {
        val sut = Fixture(
            listOf(
                Emit("a", "A"),
                Emit("zipArchive", null, "The input is not a zip archive"),
                Emit("b", "B"),
            )
        )

        val actual = sut.read()

        assertEquals(listOf("target", "zipArchive", "target"), actual.map { it.identity.value })
        assertEquals(listOf("A", "B"), sut.target!!.received.map { (it.data.self as Target).text })

        val err = actual[1]
        assertTrue(err.data.isNothing)
        assertEquals(listOf("The input is not a zip archive"), err.issues.errors.map { it.message })
        assertEquals("A", assertIs<Target>(actual[0].data.self).text)
        assertTrue(actual[0].issues.errors.isEmpty())
    }

    @Test
    fun adapted_item_carries_source_issues() = runTest {
        val sut = Fixture(listOf(Emit("a", "A", "something is wrong with a")))

        val actual = sut.read()

        assertEquals(1, actual.size)
        assertEquals("A", assertIs<Target>(actual[0].data.self).text)
        assertEquals(listOf("something is wrong with a"), actual[0].issues.errors.map { it.message })
        assertEquals("a", actual[0].parent?.identity?.value)
    }

    @Test
    fun item_with_errors_whose_end_cannot_be_evaluated_reaches_output_and_not_target() = runTest {
        val sut = Fixture(listOf(Emit("a", "A", "something is wrong with a")), textExpression = "missing")

        val actual = sut.read()

        assertEquals(listOf("a"), actual.map { it.identity.value })
        assertEquals(listOf("something is wrong with a"), actual[0].issues.errors.map { it.message })
        assertTrue(sut.target!!.received.isEmpty())
    }
}
