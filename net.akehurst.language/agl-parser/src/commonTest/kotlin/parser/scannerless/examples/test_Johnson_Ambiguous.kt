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

package net.akehurst.language.parser.leftcorner.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

internal class test_Johnson_Ambiguous : test_LeftCornerParserAbstract() {
    /**
     * S = S S S || S S || 'a' ;
     */
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                concatenation { ref("S"); ref("S"); ref("S") }
                concatenation { ref("S"); ref("S") }
                literal("a")
            }
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
            S|2 { 'a' }
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

        val expected = """
            S|1 {
                S { 'a' }
                S { 'a' }
            }
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
    fun aaa() {
        val sentence = "aaa"

        val expected1 = """
            S|0 {
                S { 'a' }
                S { 'a' }
                S { 'a' }
            }
        """.trimIndent()

        val expected2 = """
             S|1 {
                S {
                    S { 'a' }
                    S { 'a' }
                  }
                S { 'a' }
              }
        """

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected2, expected1)
        )
    }

    @Test
    fun aaaa() {
        val sentence = "aaaa"

        val expected1 = """
         S|1 {
            S|1 {
                S { 'a' }
                S { 'a' }
              }
            S|1 {
                S { 'a' }
                S { 'a' }
              }
          }
        """.trimIndent()

        val expected2 = """
            S|1 {
              S|1 {
                S|1 {
                  S|2 { 'a' }
                  S|2 { 'a' }
                }
                S|2 { 'a' }
              }
              S|2 { 'a' }
            }
        """.trimIndent()

        val expected3 = """
            S {
              S {
                 S {
                    S { 'a' }
                    S { 'a' }
                    S { 'a' }
                 }
                 S { 'a' }
              }
            }
        """.trimIndent()

        val expected4 = """
            S|0 {
              S|1 {
                S|2 { 'a' }
                S|2 { 'a' }
              }
              S|2 { 'a' }
              S|2 { 'a' }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected1, expected2, expected4)
        )
    }

    @Test
    fun a10() {
        val sentence = "a".repeat(10)

        val expected = """
            S {
                S { 'a' }
                S { 'a' }
                S { 'a' }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @ExperimentalTime
    @Test
    fun time() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val times = mutableListOf<Duration>()

        for (i in 1..5) {
            val text = "a".repeat(i)
            //warm up
            parser.parseForGoal(goal, text)
            //time it
            val time = TimeSource.Monotonic.measureTime {
                parser.parseForGoal(goal, text)
            }
            times.add(time)
        }

        times.forEach {
            println(it.inWholeMilliseconds.toInt())
        }
    }
}