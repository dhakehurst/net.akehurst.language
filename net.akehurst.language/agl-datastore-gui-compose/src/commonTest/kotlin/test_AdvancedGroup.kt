package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.common.datastoreProviderSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_AdvancedGroup {

    private companion object {
        val ID = DatastoreProviderIdentity("test.word")

        val schema = datastoreProviderSchema {
            datastore {
                parameter("file", "File", false) { type("String") }
                group("Connection") {
                    parameter("host", "Host", true) { type("String") }
                }
                group("Advanced", advanced = true) {
                    parameter("timeout", "Timeout", true) { type("Integer") }
                    parameter("strict", "Strict", true) { type("Boolean") }
                    parameter("retries", "Retries", false) { type("Integer"); default(3L) }
                    parameter("ratio", "Ratio", true) { type("Real"); default(0.5) }
                    parameter("verbose", "Verbose", true) { type("Boolean"); default(false) }
                }
                choice {
                    alternative("plain") {
                        group("Advanced", advanced = true) {
                            parameter("encoding", "Encoding", true) { type("String") }
                        }
                    }
                    alternative("styled") {
                        group("Advanced", advanced = true) {
                            parameter("keepStyles", "Keep styles", false) { type("Boolean") }
                        }
                    }
                }
            }
            access { }
        }

        val connection = schema.datastore.parameterInfo[1] as DsConfigParameterGroup
        val advanced = schema.datastore.parameterInfo[2] as DsConfigParameterGroup
        val choice = schema.datastore.parameterInfo[3] as DsConfigParameterChoice
        val plainAdvanced = choice.alternative["plain"]!!.parameterInfo[0] as DsConfigParameterGroup
        val styledAdvanced = choice.alternative["styled"]!!.parameterInfo[0] as DsConfigParameterGroup
    }

    @Test
    fun a_group_that_is_not_advanced_is_always_open() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        assertTrue(draft.advancedGroupMustBeOpen("1", connection))
    }

    @Test
    fun advanced_group_is_folded_when_nothing_is_set() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        assertFalse(draft.advancedGroupMustBeOpen("2", advanced))
        assertFalse(draft.advancedGroupMustBeOpen("3/0/0", plainAdvanced))
    }

    @Test
    fun advanced_group_must_be_open_when_a_parameter_is_set() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        draft.parameter["strict"]!!.boolean = false
        assertTrue(draft.advancedGroupMustBeOpen("2", advanced))

        draft.parameter["strict"]!!.boolean = null
        assertFalse(draft.advancedGroupMustBeOpen("2", advanced))

        draft.parameter["timeout"]!!.text = "30"
        assertTrue(draft.advancedGroupMustBeOpen("2", advanced))
    }

    @Test
    fun advanced_group_must_be_open_when_a_parameter_has_an_error() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        draft.parameter["timeout"]!!.text = "-"
        assertTrue(draft.advancedGroupMustBeOpen("2", advanced))

        // an error given by the host, e.g. a schema issue
        val draft2 = ConfigurationDraft(ID, schema.datastore)
        assertTrue(draft2.advancedGroupMustBeOpen("2", advanced, mapOf("timeout" to "too long")))
    }

    @Test
    fun advanced_group_in_an_alternative() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        draft.parameter["encoding"]!!.text = "UTF-8"
        assertTrue(draft.advancedGroupMustBeOpen("3/0/0", plainAdvanced))

        // the required parameter of the chosen alternative's advanced group has an error ("Required")
        draft.choose("3", "styled")
        assertTrue(draft.advancedGroupMustBeOpen("3/1/0", styledAdvanced))
    }

    @Test
    fun advanced_group_stays_folded_when_a_parameter_is_set_to_its_default() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        // 'retries' is required but has a default, so it is not an error
        assertFalse("retries" in draft.parameterIssues)
        assertFalse(draft.advancedGroupMustBeOpen("2", advanced))

        draft.parameter["retries"]!!.text = "3"
        draft.parameter["ratio"]!!.text = "0.50"
        draft.parameter["verbose"]!!.boolean = false
        assertFalse(draft.advancedGroupMustBeOpen("2", advanced))

        draft.parameter["verbose"]!!.boolean = true
        assertTrue(draft.advancedGroupMustBeOpen("2", advanced))
        draft.parameter["verbose"]!!.boolean = null

        draft.parameter["retries"]!!.text = "4"
        assertTrue(draft.advancedGroupMustBeOpen("2", advanced))
    }

    @Test
    fun the_default_is_not_written_into_the_configuration() {
        val draft = ConfigurationDraft(ID, schema.datastore)
        draft.parameter["file"]!!.text = "a.docx"
        draft.choose("3", "plain")
        val cfg = draft.build()!!
        assertFalse(cfg.argument.containsKey("retries"))
        assertEquals(3, cfg.getAsInt("retries"))
    }
}
