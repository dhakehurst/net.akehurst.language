/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_hiddenRight2 : test_LeftCornerParserAbstract() {

    // S = S C B | 'a'
    // B = 'b' | <empty>
    // C = 'c'

    // S = S1 | 'a'
    // S1 = S C B
    // B = 'b' | Be
    // Be = <empty>
    // C = 'c'
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                literal("a")
            }
            concatenation("S1") { ref("S"); ref("C"); ref("B") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("Be")
            }
            concatenation("Be") { empty() }
            concatenation("C") { literal("c") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S|1 { 'a' }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ac() {
        val sentence = "ac"

        val expected = """
         S { S1 {
            S|1 { 'a' }
            C { 'c' }
            B|1 { Be { §empty } }
          } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acb() {
        val sentence = "acb"

        val expected = """
         S { S1 {
            S|1 { 'a' }
            C{ 'c' }
            B { 'b' }
          } }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun accb() {
        val sentence = "accb"

        val expected = """
         S { S1 {
            S { S1 {
                S|1 { 'a' }
                C { 'c' }
                B|1 { Be { §empty } }
              } }
            C { 'c' }
            B { 'b' }
          } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }
}