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

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_Processor_Ambiguity1 : test_ScanOnDemandParserAbstract() {
    //TODO: make this use || ambiguous choice
    /**
     * S : 'a' | 'a' S B B ;
     * B : 'b' ? ;
     */
    /**
     * S : 'a' | S1 ;
     * S1 = 'a' S B B ;
     * B : 'b' ? ;
     */
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S"); ref("B"); ref("B") }
            multi("B", 0, 1, "'b'")
            literal("'b'", "b")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { 'a' }
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
    fun aa() {
        val sentence = "aa"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §empty }
                B { §empty }
              }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected1)
        )
    }

    @Test
    fun aab() {
        val sentence = "aab"

        val expected1 = """
            S|1 {
              S1 {
                'a'
                S { 'a' }
                B { 'b' }
                B|1 { §empty }
              }
            }
        """.trimIndent()

        val expected2 = """
            S|1 {
              S1 {
                'a'
                S { 'a' }
                B|1 { §empty }
                B { 'b' }
              }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1, //TODO: can we make this 1 by merging states?
            expectedTrees = arrayOf(expected1)
        )
    }

    @Test
    fun aabb() {
        val sentence = "aabb"

        val expected1 = """
            S|1 {
              S1 {
                'a'
                S { 'a' }
                B { 'b' }
                B { 'b' }
              }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected1)
        )
    }

    @Test
    fun aaabb() {
        val sentence = "aaabb"

        val expected1 = """
            S|1 { S1 {
              'a'
              S|1 { S1 {
                  'a'
                  S { 'a' }
                  B { 'b' }
                  B|1 { §empty }
              } }
              B { 'b' }
              B|1 { §empty }
            } }
        """.trimIndent()

        val expected2 = """
            S { S1 {
              'a'
              S { S1 {
                  'a'
                  S { 'a' }
                  B { §empty }
                  B { 'b' }
              } }
              B { 'b' }
              B { §empty }
            } }
        """.trimIndent()

        val expected3 = """
            S { S1 {
              'a'
              S { S1 {
                  'a'
                  S { 'a' }
                  B { 'b' }
                  B { §empty }
              } }
              B { §empty }
              B { 'b' }
            } }
        """.trimIndent()

        val expected4 = """
            S { S1 {
              'a'
              S { S1 {
                  'a'
                  S { 'a' }
                  B { §empty }
                  B { 'b' }
              } }
              B { §empty }
              B { 'b' }
            } }
        """.trimIndent()

        val expected5 = """
            S|1 { S1 {
              'a'
              S|1 { S1 {
                  'a'
                  S { 'a' }
                  B { 'b' }
                  B { 'b' }
              } }
              B|1 { §empty }
              B|1 { §empty }
            } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1, //TODO: can we make this 1 by merging states?
            expectedTrees = arrayOf(expected5)
        )
    }
}