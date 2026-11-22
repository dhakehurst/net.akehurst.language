package net.akehurst.datastore.common

import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DsConfigParameterGroup {

    @Test
    fun advanced_is_false_by_default_in_the_interface() {
        val group = object : DsConfigParameterGroup {
            override val name = "Group"
            override val parameterInfo = emptyList<DsConfigParameterInfo>()
        }
        assertFalse(group.advanced)
    }

    @Test
    fun builder_group_is_not_advanced_by_default() {
        val schema = datastoreProviderSchema {
            datastore {
                group("Connection") {
                    parameter("url", "URL", false) { type("String") }
                }
            }
            access { }
        }
        val group = schema.datastore.parameterInfo[0] as DsConfigParameterGroup
        assertEquals("Connection", group.name)
        assertFalse(group.advanced)
    }

    @Test
    fun builder_group_can_be_advanced_also_inside_an_alternative() {
        val schema = datastoreProviderSchema {
            datastore {
                parameter("url", "URL", false) { type("String") }
                group("Advanced", advanced = true) {
                    parameter("timeout", "Timeout", true) { type("Integer") }
                }
                choice {
                    alternative("word") {
                        parameter("file", "File", false) { type("String") }
                        group("Advanced", advanced = true) {
                            parameter("keepStyles", "Keep styles", true) { type("Boolean") }
                        }
                    }
                }
            }
            access { }
        }
        val group = schema.datastore.parameterInfo[1] as DsConfigParameterGroup
        assertTrue(group.advanced)
        assertEquals("timeout", schema.datastore.getParameterOrNull("timeout")?.identity)

        val choice = schema.datastore.parameterInfo[2] as DsConfigParameterChoice
        val altGroup = choice.alternative["word"]!!.parameterInfo[1] as DsConfigParameterGroup
        assertTrue(altGroup.advanced)
        assertEquals("keepStyles", schema.datastore.getParameterOrNull("keepStyles")?.identity)
    }
}
