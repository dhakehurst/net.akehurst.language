/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** a format definition with errors is not used for formatting, its issues are the result (David FD2) */
class test_FormatDefinitionErrors {

    class TestObject

    private val types = typesDomain("Test", true) {
        namespace("net.akehurst.language.format.processor.test_FormatDefinitionErrors") {
            data("TestObject")
        }
    }

    private fun format(str: String) = Agl.formatByReflection(FormatString(str), types, TestObject())

    @Test
    fun unknown_rule_type() {
        val res = format(
            """
            namespace net.akehurst.language.format.processor.test_FormatDefinitionErrors
            format T {
              Nope -> "X"
            }
            """
        )
        assertNull(res.sentence)
        assertEquals(1, res.issues.errors.size, res.issues.toString())
        assertTrue(res.issues.errors.first().message.contains("'Nope'"), res.issues.toString())
    }

    @Test
    fun syntax_error() {
        val res = format(
            """
            namespace net.akehurst.language.format.processor.test_FormatDefinitionErrors
            format T {
              TestObject -> 
            """
        )
        assertNull(res.sentence)
        assertTrue(res.issues.errors.isNotEmpty(), res.issues.toString())
    }

    @Test
    fun extends_not_found() {
        val res = format(
            """
            namespace net.akehurst.language.format.processor.test_FormatDefinitionErrors
            format T : X {
              TestObject -> "T"
            }
            """
        )
        assertNull(res.sentence)
        assertEquals(1, res.issues.errors.size, res.issues.toString())
    }

    @Test
    fun no_errors_formats() {
        val res = format(
            """
            namespace net.akehurst.language.format.processor.test_FormatDefinitionErrors
            format T {
              TestObject -> "T"
            }
            """
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals("T", res.sentence)
    }
}
