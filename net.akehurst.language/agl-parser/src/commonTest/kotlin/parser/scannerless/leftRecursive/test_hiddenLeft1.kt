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

package net.akehurst.language.parser.leftcorner.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_hiddenLeft1 : test_LeftCornerParserAbstract() {

    // S = B S 'c' | 'a'
    // B = 'b' | <empty>
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B"); ref("S"); literal("c") }
                concatenation { literal("a") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("Be")
            }
            concatenation("Be") { empty() }
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
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'b'", "'a'"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { 'a' }
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
    fun bac() {
        val sentence = "bac"

        val expected = """
         S {
            B { 'b' }
            S { 'a' }
            'c'
         }
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
         S {
            B { Be { §empty } }
            S { 'a' }
            'c'
         }
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
    fun acc() {
        val sentence = "acc"

        val expected = """
         S {
            B {  Be { §empty } }
            S {
                B { Be { §empty } }
                S { 'a' }
                'c'
            }
            'c'
         }
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
    fun bacc() {
        val sentence = "bacc"

        val expected = """
         S {
            B { 'b' }
            S {
                B { Be { §empty } }
                S { 'a' }
                'c'
            }
            'c'
         }
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
    fun several() {
        val sentences = listOf(
            "a",
            "acc"
        )
        for (s in sentences) {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, (rrs as RuntimeRuleSet).terminals), rrs)
            val r = parser.parseForGoal(goal, s)
            println(rrs.usedAutomatonToString(goal))
            assertTrue(r.issues.errors.isEmpty(), r.issues.toString())
        }
    }
}