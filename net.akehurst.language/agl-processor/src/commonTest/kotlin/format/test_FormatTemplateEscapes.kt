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
import kotlin.test.assertTrue

/** escapes in the text of a template are Kotlin style (David FD5) */
class test_FormatTemplateEscapes {

    class TestObject(val intValue: Int = 3)

    private val types = typesDomain("Test", true) {
        namespace("net.akehurst.language.format.processor.test_FormatTemplateEscapes") {
            data("TestObject")
        }
    }

    // [template] is the format DSL text between the quotes, as written in the format definition
    private fun test(template: String, expected: String) {
        val str = "namespace net.akehurst.language.format.processor.test_FormatTemplateEscapes\nformat T {\n  TestObject -> \"" + template + "\"\n}"
        val res = Agl.formatByReflection(FormatString(str), types, TestObject())
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(expected, res.sentence)
    }

    @Test
    fun backslash() = test("""a\\b""", "a\\b")

    @Test
    fun quote() = test("""a\"b""", "a\"b")

    @Test
    fun single_quote() = test("""a\'b""", "a'b")

    @Test
    fun dollar() = test("""a\${'$'}intValue""", "a\$intValue")

    @Test
    fun newline() = test("""a\nb""", "a\nb")

    @Test
    fun tab() = test("""a\tb""", "a\tb")

    @Test
    fun carriage_return() = test("""a\rb""", "a\rb")

    @Test
    fun backspace() = test("""a\bb""", "a\bb")

    @Test
    fun unicode() = test("""aAb""", "aAb")

    @Test
    fun backslash_then_n() = test("""a\\nb""", "a\\nb")

    @Test
    fun other_character() = test("""a\qb""", "aqb")

    @Test
    fun with_property() = test("""v=${'$'}intValue\t!""", "v=3\t!")
}
