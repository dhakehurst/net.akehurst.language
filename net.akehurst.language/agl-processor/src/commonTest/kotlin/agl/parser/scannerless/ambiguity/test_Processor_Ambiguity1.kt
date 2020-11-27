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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity1 : test_ScanOnDemandParserAbstract() {
    //TODO: make this use || ambiguouse choice
    /**
     * S : 'a' | 'a' S B B ;
     * B : 'b' ? ;
     */
    /**
     * S : 'a' | S1 ;
     * S1 = 'a' S B B ;
     * B : 'b' ? ;
     */
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { literal("a"); ref("S"); ref("B"); ref("B") }
        multi("B",0,1,"'b'")
        literal("'b'","b")
    }

    @Test
    fun S_S_empty_fails() {
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun S_S_a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun S_S_aa() {
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S|1 {
              S1 {
                'a'
                S { 'a' }
                B|1 { §empty }
                B|1 { §empty }
              }
            }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected1)
        )
    }

    @Test
    fun S_S_aab() {
        val goal = "S"
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

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected1)
        )
    }

    @Test
    fun S_S_aabb() {
        val goal = "S"
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

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected1)
        )
    }

    @Test
    fun S_S_aaabb() {
        val goal = "S"
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

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected5)
        )
    }
}