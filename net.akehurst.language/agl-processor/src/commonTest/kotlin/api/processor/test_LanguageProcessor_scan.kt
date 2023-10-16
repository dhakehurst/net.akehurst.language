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
import net.akehurst.language.api.sppt.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_LanguageProcessor_scan {

    private companion object {

        fun test(grammarStr: String, sentence: String, expectedNumTokens: Int): List<LeafData> {
            val pr = Agl.processorFromString<Any, Any>(grammarStr)
            val tokens = pr.processor!!.scan(sentence)
            assertNotNull(tokens)
            val tokenStr = tokens.joinToString("") { it.matchedText }
            assertEquals(sentence, tokenStr)
            assertEquals(expectedNumTokens, tokens.size)
            return tokens
        }

    }

    @Test
    fun scan_literal_empty_a() {
        val grammarStr = "namespace test grammar Test { a = 'x'; }"
        val sentence = "ab"
        val tokens = test(grammarStr, sentence, 1)

        assertEquals(0, tokens[0].location.position)
        assertEquals(2, tokens[0].matchedText.length)
    }

    @Test
    fun scan_pattern_empty_a() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = \"[x]*\"; }")
        val tokens = pr.processor!!.scan("ab")
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
    fun scan_a_a() {
        val pr = Agl.processorFromStringDefault("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("a")
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
    fun scan_a_aa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aa")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(2, tokens.size)
    }

    @Test
    fun scan_a_aaa() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a';}")
        val tokens = pr.processor!!.scan("aaa")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
    }

    @Test
    fun scan_ab_aba() {
        val pr = Agl.processorFromString<Any, Any>("namespace test grammar Test { a = 'a'; b = 'b'; }")
        val tokens = pr.processor!!.scan("aba")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
        assertEquals("a", tokens[0].matchedText)
        assertEquals("b", tokens[1].matchedText)
        assertEquals("a", tokens[2].matchedText)

    }

    @Test
    fun scan_end_of_line() {
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
        var tokens = pr.processor!!.scan("a")
        var tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(1, tokens.size)

        tokens = pr.processor!!.scan("a b")
        tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")
        assertEquals(3, tokens.size)

        TODO("test eol")
    }

    @Test
    fun scan_bug() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WHITESPACE = "\s+" ;
                S = 'prefix' NAME;
                NAME = "[a-z]+" ;
            }
        """.trimIndent()

        //test(grammarStr, "prefix abc", 3)
        //test(grammarStr, "prefixabc", 2)
        test(grammarStr, "/prefix abc", 4)

    }
}