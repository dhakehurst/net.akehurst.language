/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_nested_optionals : test_ScanOnDemandParserAbstract() {
    /*
        Derived from:

        grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
        extends = 'extends' [qualifiedName / ',']+ ;
        rules = rule+ ;
        rule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
     */
    /*
        S = 'a' R 'z' ;
        //rules = rule+ ;
        R = Os 'y' ;
        Os = Bo Co Do ;
        Bo = 'b'?
        Co = 'c'?
        Do = 'd'?
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("R"); literal("z") }
            concatenation("R") { ref("Os"); literal("y") }
            concatenation("Os") { ref("Bo"); ref("Co"); ref("Do") }
            multi("Bo", 0, 1, "'b'")
            multi("Co", 0, 1, "'c'")
            multi("Do", 0, 1, "'d'")
            literal("'b'", "b")
            literal("'c'", "c")
            literal("'d'", "d")
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
            ), issues
        )
    }

    @Test
    fun m_fails() {
        val sentence = "m"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^m", setOf("'a'"))
            ), issues
        )
    }

    @Test
    fun ayz() {
        val sentence = "ayz"

        val expected = """
            S {
              'a'
              R {
                Os {
                  Bo|1 { §empty }
                  Co|1 { §empty }
                  Do|1 { §empty }
                }
                'y'
              }
              'z'
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
    fun abyz() {
        val sentence = "abyz"

        val expected = """
            S {
              'a'
              R {
                Os {
                  Bo { 'b' }
                  Co|1 { §empty }
                  Do|1 { §empty }
                }
                'y'
              }
              'z'
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
    fun ba_fails() {
        val sentence = "ba"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "b^a", setOf("'c'", "'t'"))
            ), issues
        )
    }

    @Test
    fun acyz() {
        val sentence = "acyz"

        val expected = """
            S {
              'a'
              R {
                Os {
                  Bo|1 { §empty }
                  Co{ 'c' }
                  Do|1 { §empty }
                }
                'y'
              }
              'z'
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
    fun abct() {
        val sentence = "abct"

        val expected = """
            S { Opts {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt { 'c' }
                }
                't'
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
    fun adc_fails() {
        val sentence = "adc"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^dc", setOf("'b'", "'c'", "'t'"))
            ), issues
        )
    }

    @Test
    fun abd_fails() {
        val sentence = "abd"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1), "ab^d", setOf("'c'", "'t'"))
            ), issues
        )
    }

    @Test
    fun abcd_fails() {
        val sentence = "abcd"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(3, 4, 1, 1), "abc^d", setOf("'t'"))
            ), issues
        )
    }

    @Test
    fun bt() {
        val sentence = "bt"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt|1 { §empty }
                }
                't'
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
    fun bct() {
        val sentence = "bct"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt { 'c' }
                }
                't'
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
    fun ct() {
        val goal = "S"
        val sentence = "ct"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt { 'c' }
               }
               't'
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
    fun cb_fails() {
        val goal = "S"
        val sentence = "cb"

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "c^b", setOf("'t'"))
            ), issues
        )
    }
}