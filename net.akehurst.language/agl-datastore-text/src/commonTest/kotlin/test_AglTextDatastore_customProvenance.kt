package net.akehurst.datastore.text

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglTextDatastore_customProvenance {

    data class InputProvenance(val name: String) : DataItemProvenance

    /** keeps the provenance of the input item, and says whether the item has a unit */
    data class MyProvenance(val datastore: String, val from: DataItemProvenance?, val hasUnit: Boolean) : DataItemProvenance

    class MyAglTextDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : AglTextDatastore(identity, configuration) {
        override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance =
            MyProvenance(identity.value, input?.provenance, source is ParsableUnit)
    }

    class MyAglTextProvider : AglTextProvider() {
        override val identity: DatastoreProviderIdentity get() = DatastoreProviderIdentity("test.MyAglTextProvider")
        override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
            MyAglTextDatastore(datastoreIdentity, configuration)
    }

    companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = 'Hello' NAME '!' ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val formatStr = $$"""
            namespace test
            format Test {
                S -> "Hello $name !"
            }
        """.trimIndent()
    }

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(AglTextProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(MyAglTextProvider())
    }

    private fun sut(): Datastore {
        val prov = MyAglTextProvider()
        val dscfg = datastoreConfiguration(prov.identity) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        }
        return prov.provide(DatastoreIdentity("test"), dscfg)
    }

    @Test
    fun default_provenance() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        }
        val sut = AglTextProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        val item = DataItemDefault(null, DataItemIdentity("unit"), og.toTypedObject(ParsableUnit("Hello World !"), StdLibDefault.AnyType), InputProvenance("in"))
        val actual = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(item)).toList()

        assertEquals(AglTextProvenance(DatastoreIdentity("test")), actual[0].provenance)
    }

    @Test
    fun read_custom_provenance() = runTest {
        val sut = sut()
        val og = sut.accessorMutator()
        sut.open()
        val input = flowOf(
            DataItemDefault(null, DataItemIdentity("unit"), og.toTypedObject(ParsableUnit("Hello World !"), StdLibDefault.AnyType), InputProvenance("unit")),
            DataItemDefault(null, DataItemIdentity("str"), og.toTypedObject("Hello World !", StdLibDefault.String), InputProvenance("str")),
            DataItemDefault(null, DataItemIdentity("bad"), og.toTypedObject(1, StdLibDefault.Integer), InputProvenance("bad")),
        )
        val actual = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, input).toList()

        assertEquals(
            listOf(
                MyProvenance("test", InputProvenance("unit"), true),
                MyProvenance("test", InputProvenance("str"), true),
                MyProvenance("test", InputProvenance("bad"), false),
            ),
            actual.map { it.provenance }
        )
    }

    @Test
    fun write_custom_provenance() = runTest {
        val sut = sut()
        val og = sut.accessorMutator()
        sut.open()
        val unit = ParsableUnit("Hello World !")
        val read = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(
            DataItemDefault(null, DataItemIdentity("unit"), og.toTypedObject(unit, StdLibDefault.AnyType), InputProvenance("unit"))
        )).toList()
        sut.beginTransaction()
        val actual = sut.write(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(
            read[0],
            DataItemDefault(null, DataItemIdentity("bad"), og.toTypedObject("x", StdLibDefault.String), InputProvenance("bad")),
        )).toList()

        assertEquals(
            listOf(
                MyProvenance("test", read[0].provenance, true),
                MyProvenance("test", InputProvenance("bad"), false),
            ),
            actual.map { it.provenance }
        )
    }
}
