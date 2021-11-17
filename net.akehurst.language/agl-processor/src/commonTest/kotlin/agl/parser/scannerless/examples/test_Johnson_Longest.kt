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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

internal class test_Johnson_Longest : test_ScanOnDemandParserAbstract() {
    /**
     * S = S S S | S S | 'a' ;
     */
    /**
     * S = S3 | S2 | 'a' ;
     * S3 = S S S ;
     * S2 = S S ;
     */

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S3")
                ref("S2")
                literal("a")
            }
            concatenation("S3") { ref("S"); ref("S"); ref("S") }
            concatenation("S2") { ref("S"); ref("S") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
        ),issues)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S|2 { 'a' }
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
    fun aa() {
        val sentence = "aa"

        val expected = """
            S|1 {
              S2 {
                S|2 { 'a' }
                S|2 { 'a' }
              }
            }
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
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S {
              S3 {
                S|2 { 'a' }
                S|2 { 'a' }
                S|2 { 'a' }
              }
            }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 3,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aaaa() {
        val sentence = "aaaa"

        val expected = """
         S|1 { S2 {
            S { S3 {
                S|2 { 'a' }
                S|2 { 'a' }
                S|2 { 'a' }
              } }
            S|2 { 'a' }
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 5,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a10() {
        val sentence = "a".repeat(10)

        val expected = """
             S|1 { S2 {
                S|1 { S2 {
                    S|1 { S2 {
                        S|1 { S2 {
                            S { S3 {
                                S|2 { 'a' }
                                S|2 { 'a' }
                                S|2 { 'a' }
                              } }
                            S|1 { S2 {
                                S|2 { 'a' }
                                S|2 { 'a' }
                              } }
                          } }
                        S|1 { S2 {
                            S|2 { 'a' }
                            S|2 { 'a' }
                          } }
                      } }
                    S|1 { S2 {
                        S|2 { 'a' }
                        S|2 { 'a' }
                      } }
                  } }
                S|2 { 'a' }
              } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @ExperimentalTime
    @Test
    fun time() {
        val parser = ScanOnDemandParser(rrs)
        val times = mutableListOf<Duration>()

        for (i in 1..25) {
            val text = "a".repeat(i)
            //warm up
            parser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            //time it
            val time = TimeSource.Monotonic.measureTime {
                parser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            }
            times.add(time)
        }

        times.forEach {
            println(it.inMilliseconds.toInt())
        }
    }
}