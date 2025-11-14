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

class test_hiddenRight4 : test_LeftCornerParserAbstract() {

    // S = A
    // A = a B a?
    // B = b C b?
    // C = c Ac?
    // Ac = A c

    // S = A
    // A = 'a' B oa
    // oa = 'a' | ea
    // ea = <empty>
    // B = 'b' C ob
    // ob = 'b' | eb
    // eb = <empty>
    // C = 'c' oAc
    // oAc = Ac | ec
    // Ac = A 'c'
    // ec = <empty>


    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A") }
            concatenation("A") { literal("a"); ref("B"); ref("oa") }
            choice("oa", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("ea")
            }
            concatenation("ea") { empty() }
            concatenation("B") { literal("b"); ref("C"); ref("ob") }
            choice("ob", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("eb")
            }
            concatenation("eb") { empty() }
            concatenation("C") { literal("c"); ref("oAc") }
            choice("oAc", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("Ac")
                ref("ec")
            }
            concatenation("Ac") { ref("A"); literal("c") }
            concatenation("ec") { empty() }
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
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'b'"))
            ), issues.errors
        )
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1, null), sentence, setOf("A"), setOf("'c'"))
            ), issues.errors
        )
    }

    @Test
    fun abc() {
        val sentence = "abc"

        val expected = """
         S { A {
            'a'
            B {
              'b'
              C {
                'c'
                oAc|1 { ec { §empty } }
              }
              ob|1 { eb { §empty } }
            }
            oa|1 { ea { §empty } }
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
    fun abcb() {
        val sentence = "abcb"

        val expected = """
         S { A {
            'a'
            B {
              'b'
              C {
                'c'
                oAc|1 { ec { §empty } }
              }
              ob { 'b' }
            }
            oa|1 { ea { §empty } }
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
    fun abcba() {
        val sentence = "abcba"

        val expected = """
         S { A {
            'a'
            B {
              'b'
              C {
                'c'
                oAc|1 { ec { §empty } }
              }
              ob { 'b' }
            }
            oa { 'a' }
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
    fun abcabcba() {
        val sentence = "abcabccba"

        val expected = """
         S { A {
            'a'
            B {
              'b'
              C {
                'c'
                oAc { Ac {
                    A {
                      'a'
                      B {
                        'b'
                        C {
                          'c'
                          oAc|1 { ec { §empty } }
                        }
                        ob|1 { eb { §empty } }
                      }
                      oa|1 { ea { §empty } }
                    }
                    'c'
                  } }
              }
              ob { 'b' }
            }
            oa { 'a' }
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