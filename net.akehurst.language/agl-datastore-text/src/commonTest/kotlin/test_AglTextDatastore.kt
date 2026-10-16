package net.akehurst.datastore.text

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    @Test
    fun construct() {
        val cfg = datastoreConfiguration {

        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        assertNotNull(sut)
        assertEquals("test", sut.identity.value)
    }

    @Test
    fun types() = runTest {
        val cfg = datastoreConfiguration {
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
        val cfg = datastoreConfiguration {
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
        val dscfg = datastoreConfiguration {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR,formatStr)
        }
        val prov = AglTextProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val cfg = datastoreConfiguration {

        }
        val actual = sut.read(cfg, flowOf(og.toTypedObject("Hello World !", StdLibDefault.String))).toList()

        val expected = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }

        assertNotNull(actual)
        assertEquals(expected.asString(), (actual[0].self as AsmStructure).asString())
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration {
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
        val cfg = datastoreConfiguration { }
        val actual = sut.write(cfg, flowOf(og.toTypedObject(asm, StdLibDefault.AnyType))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].self as String)
    }
}