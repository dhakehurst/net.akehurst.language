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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_LanguageProcessor_scan {

    private companion object {

        fun test(grammarStr: String, sentence: String, expected: List<LeafData>) {
            val pr = Agl.processorFromString<Any, Any>(grammarStr).also {
                check(it.issues.errors.isEmpty()) { it.issues.toString() }
            }
            val result = pr.processor!!.scan(sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertEquals(expected.size, result.allTokens.size)
            for (i in expected.indices) {
                assertEquals(expected[i], result.allTokens[i])
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
        val sentence = SentenceDefault("ab", null)
        val tokens = pr.processor!!.scan(sentence.text).allTokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertEquals("ab", sentence.textAt(tokens[0].position, tokens[0].length))
        //assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].position)
        assertEquals(2, sentence.textAt(tokens[0].position, tokens[0].length).length)

    }

    @Test
    fun a_a() {
        val pr = Agl.processorFromStringSimple(GrammarString("namespace test grammar Test { a = 'a';}"))
        val sentence = SentenceDefault("a", null)
        val tokens = pr.processor!!.scan(sentence.text).allTokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertEquals("a", sentence.textAt(tokens[0].position, tokens[0].length))
        //assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].position)
        assertEquals(1,sentence.textAt(tokens[0].position, tokens[0].length).length)

    }

    @Test
    fun a_aa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aa").allTokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(2, tokens.size)
    }

    @Test
    fun a_aaa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aaa").allTokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
    }

    @Test
    fun ab_aba() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a'; b = 'b'; }")
        val sentence = SentenceDefault("aba", null)
        val tokens = pr.processor!!.scan(sentence.text).allTokens
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
        assertEquals("a", sentence.textAt(tokens[0].position, tokens[0].length))
        assertEquals("b", sentence.textAt(tokens[1].position, tokens[1].length))
        assertEquals("a", sentence.textAt(tokens[2].position, tokens[2].length))

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
        var tokens = pr.processor!!.scan("a").allTokens
        var tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(1, tokens.size)

        tokens = pr.processor!!.scan("a b").allTokens
        tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(3, tokens.size)

        tokens = pr.processor!!.scan("""
             a
             b
        """).allTokens
        tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(5, tokens.size)
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
            LeafData("'class'", false, 0, 5, emptyList()), //"class",
            LeafData("WS", true, 5, 1, emptyList()), //" ",
            LeafData("NAME", true, 6, 1, emptyList()), //"A",
            LeafData("';'", false, 7, 1, emptyList()), //";",
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
            LeafData("'class'", false, 0, 5, emptyList()), // "class",
            LeafData("WS", true, 5, 1, emptyList()), // " ",
            LeafData("'class'", false, 6, 5, emptyList()), // "class",
            LeafData("';'", false, 11, 1, emptyList()), // ";",
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
            LeafData("<UNDEFINED>", false, 0, 1, emptyList()), // "/",
            LeafData("'prefix'", false, 1, 6, emptyList()), //"prefix",
            LeafData("WHITESPACE", true, 7, 1, emptyList()), //" ",
            LeafData("NAME", true, 8, 3, emptyList()), //"abc",
        )

        test(grammarStr, "/prefix abc", expected)

    }
}