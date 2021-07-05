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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class test_Agl_scan {

    @Test
    fun scan_literal_empty_a() {
        val sut = Agl.processorFromRuleList(listOf("a = '';"))
        val tokens = sut.scan("ab")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0].isLiteral)
        assertTrue(tokens[0].isLeaf)
        assertEquals("a", tokens[0].matchedText)
        assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].identity.startPosition)
        assertEquals(1, tokens[0].matchedTextLength)

    }

    @Test
    fun scan_pattern_empty_a() {
        val sut = Agl.processorFromRuleList(listOf("a = \"[x]*\";"))
        val tokens = sut.scan("ab")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0].isLiteral)
        assertTrue(tokens[0].isLeaf)
        assertEquals("a", tokens[0].matchedText)
        assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].identity.startPosition)
        assertEquals(1, tokens[0].matchedTextLength)

    }

    @Test
    fun scan_a_a() {
        val sut = Agl.processorFromRuleList(listOf("a = 'a';"))
        val tokens = sut.scan("a")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0].isLiteral)
        assertTrue(tokens[0].isLeaf)
        assertEquals("a", tokens[0].matchedText)
        assertEquals(1, tokens[0].identity.runtimeRuleNumber)
        assertEquals(0, tokens[0].identity.startPosition)
        assertEquals(1, tokens[0].matchedTextLength)

    }

    @Test
    fun scan_a_aa() {
        val sut = Agl.processorFromRuleList(listOf("a = 'a';"))
        val tokens = sut.scan("aa")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(2, tokens.size)
    }

    @Test
    fun scan_a_aaa() {
        val sut = Agl.processorFromRuleList(listOf("a = 'a';"))
        val tokens = sut.scan("aaa")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
    }

    @Test
    fun scan_ab_aba() {
        val sut = Agl.processorFromRuleList(listOf("a = 'a';", "b = 'b';"))
        val tokens = sut.scan("aba")
        val tokenStr = tokens.map { it.toString() }.joinToString(", ")
        println("tokens = ${tokenStr}")

        assertNotNull(tokens)
        assertEquals(3, tokens.size)
        assertEquals("a", tokens[0].matchedText)
        assertEquals("b", tokens[1].matchedText)
        assertEquals("a", tokens[2].matchedText)


    }
}