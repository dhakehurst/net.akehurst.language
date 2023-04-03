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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_ScottJohnstone_RightNulled_1 : test_ScanOnDemandParserAbstract() {
    /**
     * S = abAa | aBAa | aba
     * A = a | aA
     * B = b
     */
    private companion object {
        private val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("abAa")
                ref("aBAa")
                ref("aba")
            }
            concatenation("abAa") { literal("a"); literal("b"); ref("A"); literal("a") }
            concatenation("aBAa") { literal("a"); ref("B"); ref("A"); literal("a") }
            concatenation("aba") { literal("a"); literal("b"); literal("a") }
            choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("A1")
            }
            concatenation("A1") { literal("a"); ref("A") }
            concatenation("B") { literal("b") }
        }
        private const val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""
        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
        ),issues.error)
    }

    @Test
    fun a_fails() {
        val sentence = "a"
        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^",setOf("'b'"))
        ),issues.error)
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"
        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2,3,1,1),"ab^",setOf("'a'"))
        ),issues.error)
    }

    @Test
    fun aba() {
        val sentence = "aba"

        val expected = """
            S { aba {
              'a'
              'b'
              'a'
            } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abaa() {
        val sentence = "abaa"

        val expected = """
            S { aBAa {
              'a'
              B { 'b' }
              A { 'a' }
              'a'
            } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abaaa() {
        val sentence = "abaaa"

        val expected = """
            S { aBAa {
              'a'
              B { 'b' }
              A { A1 {
                'a'
                A { 'a' }
              } }
              'a'
            } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }
}