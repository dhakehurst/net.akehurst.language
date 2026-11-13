package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_ListParameter {

    private companion object {
        val ID = DatastoreProviderIdentity("test.list")

        val schema = datastoreProviderSchema {
            datastore {
                parameter("paths", "Paths", false) { type("List") }
            }
            access { }
        }
    }

    @Test
    fun std_List_is_kind_LIST() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        assertEquals(ParameterKind.LIST, draft.parameter["paths"]!!.definition.kind)
    }

    @Test
    fun a_List_value_loads_as_lines() {
        val pd = ConfigurationDraft(ID, schema.datastore).parameter["paths"]!!
        pd.load(listOf("/A/B", "/A/C"))
        assertEquals("/A/B\n/A/C", pd.text)
    }

    @Test
    fun text_saves_as_List_of_non_blank_trimmed_lines() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        val pd = draft.parameter["paths"]!!
        pd.text = "/A/B\n\n /A/C \n"
        assertEquals(listOf("/A/B", "/A/C"), pd.argument().getOrNull())
        assertTrue(draft.parameterIssues.isEmpty())
        assertTrue(draft.schemaIssues.isEmpty(), draft.schemaIssues.toString())
        assertEquals(listOf("/A/B", "/A/C"), draft.build()?.argument?.get("paths"))
    }

    @Test
    fun a_List_value_round_trips() {
        val pd = ConfigurationDraft(ID, schema.datastore).parameter["paths"]!!
        pd.load(listOf("/A/B", "/A/C"))
        assertEquals(listOf("/A/B", "/A/C"), pd.argument().getOrNull())
    }

    @Test
    fun empty_text_is_not_set() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        val pd = draft.parameter["paths"]!!
        pd.text = ""
        assertFalse(pd.isSet)
        assertEquals("Required", draft.parameterIssues["paths"])
    }
}
