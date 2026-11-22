package net.akehurst.datastore.text

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private object RefsTestProvenance : DataItemProvenance

class test_AglTextDatastore_references {

    companion object {
        val grammarStr = """
            namespace test
            grammar Refs {
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = def | use ;
                def = 'def' ID ;
                use = 'use' ref ;
                leaf ref = ID ;
                leaf ID = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val formatStr = $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use $ref"
            }
        """.trimIndent()

        val crossReferenceStr = """
            namespace test.Refs
                identify Def by id
                references {
                    in Use {
                        property ref refers-to Def
                    }
                }
        """.trimIndent()

        val helloGrammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = 'Hello' NAME '!' ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val helloFormatStr = $$"""
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

    private fun refsDatastore(): Datastore = AglTextProvider().provide(DatastoreIdentity("txt"), datastoreConfiguration(AglTextProvider.IDENTITY) {
        argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
        argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
        argument(AglTextProvider.CONFIG_CROSS_REFERENCE_STR, crossReferenceStr)
    })

    private suspend fun unitItem(sut: Datastore, id: String, text: String): DataItem =
        DataItemDefault(null, DataItemIdentity(id), sut.accessorMutator().toTypedObject(ParsableUnit(text), StdLibDefault.AnyType), RefsTestProvenance)

    private fun unitOf(item: DataItem) = item.data.self as ParsableUnit

    @Test
    fun read_keeps_locations() = runTest {
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, helloGrammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, helloFormatStr)
        })
        sut.open()
        val actual = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, "u", "Hello World !"))).toList()

        val unit = unitOf(actual[0])
        val loc = unit.locationOf(unit.asm)
        assertNotNull(loc)
        assertEquals(0, loc.position)
        assertEquals("Hello World !".length, loc.length)
        // the location says which unit it is in
        assertEquals(listOf(DataItemIdentity("u")), loc.sentenceIdentity)
        // a TypedObject of the element gives the same location
        assertEquals(loc, unit.locationOf(sut.accessorMutator().toTypedObject(unit.asm, StdLibDefault.AnyType)))
        // no cross-reference definition, so no references
        assertTrue(unit.resolvedReferences.isEmpty())
    }

    @Test
    fun read_resolves_references_within_unit() = runTest {
        val sut = refsDatastore()
        sut.open()
        val actual = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, "u", "def a use a"))).toList()

        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        val unit = unitOf(actual[0])
        assertEquals(1, unit.resolvedReferences.size)
        val ref = unit.resolvedReferences[0]
        assertEquals(6, ref.sourceLocation?.position)
        assertEquals(0, ref.targetLocation?.position)
        assertEquals(unit.locationOf(ref.target), ref.targetLocation)
    }

    @Test
    fun read_does_not_resolve_across_units_by_default() = runTest {
        val sut = refsDatastore()
        sut.open()
        val input = flowOf(unitItem(sut, "a", "def a"), unitItem(sut, "b", "use a"))
        val actual = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, input).toList()

        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        assertTrue(actual[1].issues.errors.isNotEmpty(), "the reference to 'a' should not resolve")
        assertNull(unitOf(actual[1]).asm)
    }

    @Test
    fun read_resolves_across_units_when_configured() = runTest {
        val sut = refsDatastore()
        sut.open()
        // the reference comes before the definition, so it needs both passes
        val input = flowOf(unitItem(sut, "b", "use a"), unitItem(sut, "a", "def a"))
        val access = accessConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS, true)
        }
        val actual = sut.read(access, input).toList()

        assertEquals(2, actual.size)
        assertEquals(listOf(DataItemIdentity("b")), actual[0].qualifiedIdentity)
        assertEquals(listOf(DataItemIdentity("a")), actual[1].qualifiedIdentity)
        assertTrue(actual[0].issues.isEmpty(), actual[0].issues.toString())
        assertTrue(actual[1].issues.isEmpty(), actual[1].issues.toString())

        val use = unitOf(actual[0])
        val def = unitOf(actual[1])
        assertTrue(use.asm is AsmStructure)
        assertEquals(1, use.resolvedReferences.size)
        val ref = use.resolvedReferences[0]
        // the target is in the other unit
        assertEquals(listOf(DataItemIdentity("a")), ref.targetLocation?.sentenceIdentity)
        assertEquals(def.locationOf(ref.target), ref.targetLocation)
        assertNotNull(use.locationOf(use.asm))
    }

    @Test
    fun read_across_units_same_identity_gives_issue() = runTest {
        val sut = refsDatastore()
        sut.open()
        val input = flowOf(unitItem(sut, "a", "def a"), unitItem(sut, "a", "def b"))
        val access = accessConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS, true)
        }
        val actual = sut.read(access, input).toList()

        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        assertTrue(actual[1].issues.errors.any { it.message.contains("same identity") }, actual[1].issues.toString())
    }

    @Test
    fun write_clears_locations_and_references() = runTest {
        val sut = refsDatastore()
        sut.open()
        val read = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, "u", "def a"))).toList()
        val unit = unitOf(read[0])
        assertNotNull(unit.locationOf(unit.asm))

        sut.beginTransaction()
        val written = sut.write(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(read[0])).toList()

        assertTrue(written[0].issues.errors.isEmpty(), written[0].issues.toString())
        assertSame(unit, unitOf(written[0]))
        assertNull(unit.locationOf(unit.asm))
        assertTrue(unit.resolvedReferences.isEmpty())
    }
}
