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

package net.akehurst.language.parser.scannerless.ambiguity

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity1 : test_ScannerlessParserAbstract() {
    //TODO: make this use || ambiguouse choice
    /**
     * S : 'a' S B B | 'a' ;
     * B : 'b' ? ;
     */
    /**
     * S : S1 | 'a' ;
     * S1 = 'a' S B B ;
     * B : 'b' ? ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_B = b.rule("B").multi(0, 1, b.literal("b"))
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_a, r_S, r_B, r_B)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, arrayOf(r_S1, r_a))
        return b
    }

    @Test
    fun S_S_empty_fails() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun S_S_a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aa() {
        val rrb = this.S()
        val goal = "S"
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

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aab() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aab"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { 'b' }
                B { §empty }
              }
            }
        """.trimIndent()

        val expected2 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §empty }
                B { 'b' }
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aabb() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aabb"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { 'b' }
                B { 'b' }
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aaabb() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaabb"

        val expected1 = """
            S { S1 {
              'a'
              S { S1 {
                  'a'
                  S { 'a' }
                  B { 'b' }
                  B { §empty }
              } }
              B { 'b' }
              B { §empty }
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
            S { S1 {
              'a'
              S { S1 {
                  'a'
                  S { 'a' }
                  B { 'b' }
                  B { 'b' }
              } }
              B { §empty }
              B { §empty }
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected5) //expected1, expected2, expected3, expected4, expected5)
    }
}