/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.processor

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class test_LanguageProcessor_scan {

    private companion object {

        fun test(grammarStr: String, sentence: String, expected: List<LeafData>) {
            val pr = Agl.processorFromString<Any, Any>(grammarStr).also {
                check(it.issues.errors.isEmpty()) { it.issues.toString() }
            }
            val result = pr.processor!!.scan(sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertEquals(expected.size, result.tokens.size)
            for (i in expected.indices) {
                assertEquals(expected[i], result.tokens[i])
            }
        }

    }

    @Test
    fun literal_a__blank() {
        val grammarStr = "namespace test grammar Test { a = 'a' ; }"
        val sentence = ""
        val expected = listOf<LeafData>()
        test(grammarStr, sentence, expected)

    }

    @Test
    fun literal_empty__blank() {
        val grammarStr = "namespace test grammar Test { a = ; }"
        val sentence = ""
        val expected = listOf<LeafData>()
        test(grammarStr, sentence, expected)

    }

    @Test
    fun pattern_empty_a() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = \"[x]*\"; }")
        val tokens = pr.processor!!.scan("ab").tokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertEquals("ab", tokens[0].matchedText)
        //assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].location.position)
        assertEquals(2, tokens[0].matchedText.length)

    }

    @Test
    fun a_a() {
        val pr = Agl.processorFromStringDefault("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("a").tokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertEquals("a", tokens[0].matchedText)
        //assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].location.position)
        assertEquals(1, tokens[0].matchedText.length)

    }

    @Test
    fun a_aa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aa").tokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(2, tokens.size)
    }

    @Test
    fun a_aaa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aaa").tokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
    }

    @Test
    fun ab_aba() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a'; b = 'b'; }")
        val tokens = pr.processor!!.scan("aba").tokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
        assertEquals("a", tokens[0].matchedText)
        assertEquals("b", tokens[1].matchedText)
        assertEquals("a", tokens[2].matchedText)

    }

    @Test
    fun end_of_line() {
        val pr = Agl.processorFromString<Any, Any>(
            """
            namespace test
            grammar Test {
                skip WHITESPACE = "\s+" ;
                chars = char+ ;
                char = "[a-z]" ;
            }
        """.trimIndent()
        )
        var tokens = pr.processor!!.scan("a").tokens
        var tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(1, tokens.size)

        tokens = pr.processor!!.scan("a b").tokens
        tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(3, tokens.size)

        TODO("test eol")
    }

    @Test
    fun class_A() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = 'class' NAME ';' ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val expected = listOf(
            LeafData("'class'", false, InputLocation(0, 1, 1, 5), "class", emptyList()),
            LeafData("WS", true, InputLocation(5, 6, 1, 1), " ", emptyList()),
            LeafData("NAME", true, InputLocation(6, 7, 1, 1), "A", emptyList()),
            LeafData("';'", false, InputLocation(7, 8, 1, 1), ";", emptyList()),
        )

        test(grammarStr, "class A;", expected)
    }

    @Test
    fun class_class() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = 'class' NAME ';' ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val expected = listOf(
            LeafData("'class'", false, InputLocation(0, 1, 1, 5), "class", emptyList()),
            LeafData("WS", true, InputLocation(5, 6, 1, 1), " ", emptyList()),
            LeafData("'class'", false, InputLocation(6, 7, 1, 5), "class", emptyList()),
            LeafData("';'", false, InputLocation(11, 12, 1, 1), ";", emptyList()),
        )

        test(grammarStr, "class class;", expected)
    }

    @Test
    fun bug1() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WHITESPACE = "\s+" ;
                S = 'prefix' NAME;
                leaf NAME = "[a-z]+" ;
            }
        """.trimIndent()

        val expected = listOf(
            LeafData("<UNDEFINED>", false, InputLocation(0, 1, 1, 1), "/", emptyList()),
            LeafData("'prefix'", false, InputLocation(1, 2, 1, 6), "prefix", emptyList()),
            LeafData("WHITESPACE", true, InputLocation(7, 8, 1, 1), " ", emptyList()),
            LeafData("NAME", true, InputLocation(8, 9, 1, 3), "abc", emptyList()),
        )

        test(grammarStr, "/prefix abc", expected)

    }
}