package net.akehurst.datastore.text

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private object TestProvenance : DataItemProvenance

class test_AglTextDatastore {

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
    }

    @Test
    fun construct() {
        val cfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        assertNotNull(sut)
        assertEquals("test", sut.identity.value)
    }

    @Test
    fun types() = runTest {
        val cfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.types()
        val expected = typesDomain("Test", true) {
            namespace("test.Test") {
                data("S") {
                    propertyOf(setOf(REF, VAR), "name", "String")
                }
            }
        }

        assertEquals(expected.asString(), actual.asString())
    }

    @Test
    fun accessorMutator() = runTest {
        val cfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.accessorMutator()

        assertNotNull(actual)
    }

    @Test
    fun read() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val cfg = accessConfiguration(AglTextProvider.IDENTITY) {

        }
        val actual = sut.read(cfg, flowOf(DataItemDefault(null, DataItemIdentity("test"), og.toTypedObject("Hello World !", StdLibDefault.String), TestProvenance))).toList()

        val expected = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }

        assertNotNull(actual)
        assertEquals(expected.asString(), (actual[0].data.self as AsmStructure).asString())
    }

    @Test
    fun read_parse_error_gives_item_with_issues() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        }
        val sut = AglTextProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        val cfg = accessConfiguration(AglTextProvider.IDENTITY) {}
        val input = flowOf(
            DataItemDefault(null, DataItemIdentity("bad"), og.toTypedObject("not valid", StdLibDefault.String), TestProvenance),
            DataItemDefault(null, DataItemIdentity("good"), og.toTypedObject("Hello World !", StdLibDefault.String), TestProvenance),
        )

        val actual = sut.read(cfg, input).toList()

        // the bad item does not stop the read
        assertEquals(2, actual.size)
        assertTrue(actual[0].issues.errors.isNotEmpty())
        assertTrue(og.isNothing(actual[0].data))
        assertTrue(actual[1].issues.errors.isEmpty())
        assertIs<AsmStructure>(actual[1].data.self)
    }

    @Test
    fun qualifiedIdentity() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        }
        val sut = AglTextProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        val cfg = accessConfiguration(AglTextProvider.IDENTITY) {}
        val root = DataItemDefault(null, DataItemIdentity("root"), og.toTypedObject("Hello World !", StdLibDefault.String), TestProvenance)

        val actual = sut.read(cfg, flowOf(root)).toList()

        assertEquals(listOf(DataItemIdentity("root")), root.qualifiedIdentity)
        assertEquals(listOf(DataItemIdentity("root"), DataItemIdentity("0")), actual[0].qualifiedIdentity)
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val asm = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }.root[0] as AsmStructure
        val cfg = accessConfiguration(AglTextProvider.IDENTITY) { }
        val actual = sut.write(cfg, flowOf(DataItemDefault(null, DataItemIdentity("test"), og.toTypedObject(asm, StdLibDefault.AnyType), TestProvenance))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].data.self as String)
    }

    @Test
    fun configuration_missing_required_parameter_fails() {
        val e = assertFailsWith<IllegalStateException> {
            datastoreConfiguration(AglTextProvider.IDENTITY) { }
        }
        assertTrue(e.message!!.contains(AglTextProvider.CONFIG_FORMAT_STR), e.message)
    }

    @Test
    fun configuration_wrong_value_type_fails() {
        val e = assertFailsWith<IllegalStateException> {
            datastoreConfiguration(AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, 42)
            }
        }
        assertTrue(e.message!!.contains("must be a"), e.message)
    }

    @Test
    fun configuration_unknown_parameter_fails() {
        assertFailsWith<IllegalStateException> {
            datastoreConfiguration(AglTextProvider.IDENTITY) {
                argument("not.a.parameter", "x")
            }
        }
    }
}
