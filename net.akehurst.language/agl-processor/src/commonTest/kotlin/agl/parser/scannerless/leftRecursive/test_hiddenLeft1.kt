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

package net.akehurst.language.parser.scanondemand.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class test_hiddenLeft1 : test_ScanOnDemandParserAbstract() {

    // S = B S 'c' | 'a'
    // B = 'b' | <empty>

    // S = S1 | 'a'
    // S1 = B S 'c'
    // B = 'b' | Be
    // Be = <empty>
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                literal("a")
            }
            concatenation("S1") { ref("B"); ref("S"); literal("c") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("Be")
            }
            concatenation("Be") { empty() }
        }
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence, 1)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S|1 { 'a' }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2,//TODO can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun bac() {
        val goal = "S"
        val sentence = "bac"

        val expected = """
         S { S1 {
            B { 'b' }
            S|1 { 'a' }
            'c'
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 3,//TODO can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ac() {
        val goal = "S"
        val sentence = "ac"

        val expected = """
         S { S1 {
            B|1 { Be { §empty } }
            S|1 { 'a' }
            'c'
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2,//TODO can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun bacc() {
        val goal = "S"
        val sentence = "bacc"

        val expected = """
         S { S1 {
            B { 'b' }
            S { S1 {
                B|1 { Be { §empty } }
                S|1 { 'a' }
                'c'
              } }
            'c'
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 by merging states?
                expectedTrees = *arrayOf(expected)
        )
    }
}