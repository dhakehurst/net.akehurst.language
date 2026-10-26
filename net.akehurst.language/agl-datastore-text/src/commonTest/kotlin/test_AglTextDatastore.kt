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
import kotlin.test.assertNotNull

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
        val acc = accessConfiguration(AglTextProvider.IDENTITY) { }

        val actual = sut.types(acc)
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
        val acc = accessConfiguration(AglTextProvider.IDENTITY) { }

        val actual = sut.accessorMutator(acc)

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
        val acc = accessConfiguration(AglTextProvider.IDENTITY) { }
        val og = sut.accessorMutator(acc)

        val cfg = accessConfiguration(AglTextProvider.IDENTITY) {

        }
        val actual = sut.read(cfg, flowOf(DataItemDefault(null, DataItemIdentity("test"), og.toTypedObject("Hello World !", StdLibDefault.String), TestProvenance))).toList()

        val expected = asmSimple(sut.types(acc)) {
            element("S") {
                propertyString("name", "World")
            }
        }

        assertNotNull(actual)
        assertEquals(expected.asString(), (actual[0].data.self as AsmStructure).asString())
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val acc = accessConfiguration(AglTextProvider.IDENTITY) { }
        val og = sut.accessorMutator(acc)

        val asm = asmSimple(sut.types(acc)) {
            element("S") {
                propertyString("name", "World")
            }
        }.root[0] as AsmStructure
        val cfg = datastoreConfiguration(AglTextProvider.IDENTITY) { }
        val actual = sut.write(cfg, flowOf(DataItemDefault(null, DataItemIdentity("test"), og.toTypedObject(asm, StdLibDefault.AnyType), TestProvenance))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].data.self as String)
    }
}