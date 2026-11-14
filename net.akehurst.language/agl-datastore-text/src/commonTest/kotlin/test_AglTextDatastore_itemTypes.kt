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
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private object ItemTypesTestProvenance : DataItemProvenance

class test_AglTextDatastore_itemTypes {

    companion object {
        val grammarStr = """
            namespace test
            grammar Reqs {
                skip leaf WS = "\s+" ;
                unit = module ;
                module = 'module' path '{' content* '}' ;
                path = [ID / '.']+ ;
                content = requirement | section ;
                requirement = 'requirement' ID TEXT ;
                section = 'section' TEXT '{' requirement* '}' ;
                leaf ID = "[a-zA-Z_][a-zA-Z0-9_]*" ;
                leaf TEXT = "'[^']*'" ;
            }
        """.trimIndent()

        val formatStr = $$"""
            namespace test
            format Reqs {
                Requirement -> "requirement $id $text"
            }
        """.trimIndent()

        val crossReferenceStr = """
            namespace test.Reqs
                identify Requirement by id
        """.trimIndent()

        val textA = """
            module a.b {
              requirement R1 'shall do one'
              requirement R2 'shall do two'
            }
        """.trimIndent()

        val textB = """
            module c {
              requirement R3 'shall do three'
            }
        """.trimIndent()

        val textSections = """
            module s {
              section 'first' {
                requirement S1 'in first'
              }
              section 'second' { }
            }
        """.trimIndent()
    }

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(AglTextProvider())
    }

    private fun config(itemTypes: List<String>?, crossReference: Boolean = true): DatastoreConfiguration =
        datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
            if (crossReference) argument(AglTextProvider.CONFIG_CROSS_REFERENCE_STR, crossReferenceStr)
            itemTypes?.let { argument(AglTextProvider.CONFIG_ITEM_TYPES, it) }
        }

    private suspend fun sut(itemTypes: List<String>?, crossReference: Boolean = true): Datastore =
        AglTextProvider().provide(DatastoreIdentity("txt"), config(itemTypes, crossReference)).also { it.open() }

    private suspend fun unitItem(sut: Datastore, id: String, text: String): DataItem =
        DataItemDefault(null, DataItemIdentity(id), sut.accessorMutator().toTypedObject(ParsableUnit(text), StdLibDefault.AnyType), ItemTypesTestProvenance)

    private suspend fun read(sut: Datastore, vararg units: Pair<String, String>, acrossUnits: Boolean = false): List<DataItem> {
        val access = accessConfiguration(AglTextProvider.IDENTITY) {
            if (acrossUnits) argument(AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS, true)
        }
        return sut.read(access, flowOf(*units.map { (id, text) -> unitItem(sut, id, text) }.toTypedArray())).toList()
    }

    private fun ids(items: List<DataItem>) = items.map { it.qualifiedIdentity.joinToString("/") { id -> id.value } }

    private fun textOf(item: DataItem) = item.data.getProperty("text").self.toString()

    @Test
    fun without_item_types_one_item_per_unit() = runTest {
        val sut = sut(null)
        val actual = read(sut, "a" to textA, "b" to textB)

        assertEquals(listOf("a", "b"), ids(actual))
        actual.forEach { assertTrue(it.issues.isEmpty(), it.issues.toString()) }
        assertTrue((actual[0].data.self as ParsableUnit).asm is AsmStructure)
    }

    @Test
    fun requirement_items_are_children_of_the_unit_item() = runTest {
        val sut = sut(listOf("Requirement"))
        val actual = read(sut, "a" to textA)

        assertEquals(listOf("a", "a/Requirement:R1", "a/Requirement:R2"), ids(actual))
        actual.forEach { assertTrue(it.issues.isEmpty(), it.issues.toString()) }
        // the unit's item is as without item types
        assertTrue((actual[0].data.self as ParsableUnit).asm is AsmStructure)
        assertSame(actual[0], actual[1].parent)
        assertSame(actual[0], actual[2].parent)
        // the data is the element, typed as Requirement
        assertEquals("test.Reqs.Requirement", actual[1].data.type.qualifiedTypeName.value)
        assertTrue(actual[1].data.self is AsmStructure)
        assertEquals("'shall do one'", textOf(actual[1]))
        assertEquals("'shall do two'", textOf(actual[2]))
    }

    @Test
    fun qualified_type_name() = runTest {
        val sut = sut(listOf("test.Reqs.Requirement"))
        val actual = read(sut, "a" to textA)

        assertEquals(listOf("a", "a/Requirement:R1", "a/Requirement:R2"), ids(actual))
    }

    @Test
    fun requirement_items_are_children_of_the_module_item() = runTest {
        val sut = sut(listOf("Module", "Requirement"))
        val actual = read(sut, "a" to textA)

        // Module has no identifying property, so it gets its index
        assertEquals(listOf("a", "a/Module:0", "a/Module:0/Requirement:R1", "a/Module:0/Requirement:R2"), ids(actual))
        assertSame(actual[0], actual[1].parent)
        assertSame(actual[1], actual[2].parent)
        assertSame(actual[1], actual[3].parent)
        assertEquals("test.Reqs.Module", actual[1].data.type.qualifiedTypeName.value)
    }

    @Test
    fun type_without_identifying_property_gets_index_identities() = runTest {
        val sut = sut(listOf("Section", "Requirement"))
        val actual = read(sut, "s" to textSections)

        assertEquals(listOf("s", "s/Section:0", "s/Section:0/Requirement:S1", "s/Section:1"), ids(actual))
        assertEquals("'second'", textOf(actual[3]))
    }

    @Test
    fun without_cross_references_all_get_index_identities() = runTest {
        val sut = sut(listOf("Requirement"), crossReference = false)
        val actual = read(sut, "a" to textA)

        assertEquals(listOf("a", "a/Requirement:0", "a/Requirement:1"), ids(actual))
    }

    @Test
    fun index_identities_are_per_parent_item() = runTest {
        val sut = sut(listOf("Requirement"), crossReference = false)
        // identities by index never clash, so use a second unit of the same text to check identities are per parent item
        val actual = read(sut, "a" to textA, "b" to textA)

        assertEquals(listOf("a", "a/Requirement:0", "a/Requirement:1", "b", "b/Requirement:0", "b/Requirement:1"), ids(actual))
    }

    @Test
    fun clashing_identifying_values_get_a_suffix() = runTest {
        val sut = sut(listOf("Requirement"))
        val text = """
            module d {
              requirement R1 'first'
              requirement R1 'second'
            }
        """.trimIndent()
        val actual = read(sut, "d" to text)

        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        assertEquals(listOf("d", "d/Requirement:R1", "d/Requirement:R1~1"), ids(actual))
        assertEquals("'second'", textOf(actual[2]))
    }

    @Test
    fun element_location_is_in_the_provenance() = runTest {
        val sut = sut(listOf("Requirement"))
        val actual = read(sut, "a" to textA)

        assertEquals(AglTextProvenance(DatastoreIdentity("txt")), actual[0].provenance)
        val prov = actual[2].provenance as AglTextProvenance
        assertEquals(DatastoreIdentity("txt"), prov.datastoreIdentity)
        val loc = assertNotNull(prov.elementLocation)
        // "  requirement R2 'shall do two'" is line 3
        assertEquals(3, loc.startLine)
        assertEquals(3, loc.startColumn)
        assertEquals(3, loc.endLine)
        assertEquals(3 + "requirement R2 'shall do two'".length - 1, loc.endColumn)
        assertEquals("requirement R2 'shall do two'", textA.substring(loc.position, loc.position + loc.length).trim())
    }

    @Test
    fun element_location_over_several_lines() = runTest {
        val sut = sut(listOf("Section"))
        val actual = read(sut, "s" to textSections)

        val loc = assertNotNull((actual[1].provenance as AglTextProvenance).elementLocation)
        assertEquals(2, loc.startLine)
        assertEquals(3, loc.startColumn)
        assertEquals(4, loc.endLine)
        assertEquals(3, loc.endColumn)
    }

    @Test
    fun syntax_error_gives_no_element_items() = runTest {
        val sut = sut(listOf("Requirement"))
        val text = """
            module a {
              requirement R1 'ok'
              requirement 'no id'
            }
        """.trimIndent()
        val actual = read(sut, "a" to text, "b" to textB)

        assertEquals(listOf("a", "b", "b/Requirement:R3"), ids(actual))
        assertTrue(actual[0].issues.errors.isNotEmpty())
        assertNull((actual[0].data.self as ParsableUnit).asm)
    }

    @Test
    fun works_with_resolve_across_units() = runTest {
        val sut = sut(listOf("Module", "Requirement"))
        val actual = read(sut, "a" to textA, "b" to textB, acrossUnits = true)

        assertEquals(
            listOf("a", "a/Module:0", "a/Module:0/Requirement:R1", "a/Module:0/Requirement:R2", "b", "b/Module:0", "b/Module:0/Requirement:R3"),
            ids(actual)
        )
        actual.forEach { assertTrue(it.issues.isEmpty(), it.issues.toString()) }
        assertEquals("'shall do three'", textOf(actual[6]))
    }

    @Test
    fun unknown_item_type_fails_the_read() = runTest {
        val sut = sut(listOf("Nope"))
        val e = assertFailsWith<IllegalStateException> { read(sut, "a" to textA) }
        assertTrue(e.message!!.contains("Nope"), e.message)
    }
}
