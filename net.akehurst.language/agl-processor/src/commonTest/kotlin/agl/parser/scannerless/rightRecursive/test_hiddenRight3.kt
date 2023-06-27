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

package net.akehurst.language.parser.scanondemand.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_hiddenRight3 : test_ScanOnDemandParserAbstract() {

    // S = A
    // A = a B?
    // B = b C?
    // C = c A?

    // S = A
    // A = a oB
    // oB = B | eB
    // B = 'b' oC
    // eB = <empty>
    // oC = C | eC
    // C = 'c' oA
    // eC = <empty>
    // oA = A | eA
    // eA = <empty>
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A") }
            concatenation("A") { literal("a"); ref("oB") }
            choice("oB", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("B")
                ref("eB")
            }
            concatenation("B") { literal("b"); ref("oC") }
            concatenation("eB") { empty() }
            choice("oC", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C")
                ref("eC")
            }
            concatenation("C") { literal("c"); ref("oA") }
            concatenation("eC") { empty() }
            choice("oA", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("A")
                ref("eA")
            }
            concatenation("eA") { empty() }
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
        ),issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
         S { A {
            'a'
            oB|1 { eB { §empty } }
          } }
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
    fun ab() {
        val sentence = "ab"

        val expected = """
         S { A {
            'a'
            oB { B {
                'b'
                oC|1 { eC { §empty } }
              } }
          } }
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
    fun abc() {
        val sentence = "abc"

        val expected = """
         S { A {
            'a'
            oB { B {
                'b'
                oC { C {
                    'c'
                    oA|1 { eA { §empty } }
                  } }
              } }
          } }
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
    fun abca() {
        val sentence = "abca"

        val expected = """
         S { A {
            'a'
            oB { B {
                'b'
                oC { C {
                    'c'
                    oA { A {
                        'a'
                        oB|1 { eB { §empty } }
                      } }
                  } }
              } }
          } }
        """.trimIndent()

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }
}