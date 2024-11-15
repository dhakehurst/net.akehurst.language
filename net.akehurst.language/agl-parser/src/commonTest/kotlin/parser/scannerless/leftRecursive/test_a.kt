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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class  test_a : test_LeftCornerParserAbstract() {

    private companion object {
        // S =  'a' | S1 ;
        // S1 = S 'a' ;
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
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
                parseError(InputLocation(0,1,1,1),sentence, setOf("<GOAL>"),setOf("'a'"))
            ), issues.errors)
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
    fun aa() {
        val sentence = "aa"

        val expected = """
            S { S1 { S { 'a' } 'a' } }
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
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S {
                S1 {
                    S {
                        S1 {
                            S { 'a' }
                            'a'
                        }
                    }
                    'a'
                }
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
    fun aaaaa() {
        val sentence = "aaaaa"

        val expected = """
             S { S1 {
                S { S1 {
                    S { S1 {
                        S { S1 {
                            S { 'a' }
                            'a'
                          } }
                        'a'
                      } }
                    'a'
                  } }
                'a'
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
    fun a50() {
        val sentence = "a".repeat(50)

        val expected = "S { S1 { ".repeat(49) + "S { 'a' }" + "'a' } }".repeat(49)

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a150() {
        val sentence = "a".repeat(150)

        val expected = "S { S1 { ".repeat(149) + "S { 'a' }" + "'a' } }".repeat(149)

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a500() {
        val sentence = "a".repeat(500)

        val expected = "S { S1 { ".repeat(499) + "S { 'a' }" + "'a' } }".repeat(499)

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    //@Test - toString of SPPT takes too long
    fun a2000() {
        val sentence = "a".repeat(2000)

        val expected = "S { S1 { ".repeat(1999) + "S { 'a' }" + "'a' } }".repeat(1999)

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }
}