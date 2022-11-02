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

package net.akehurst.language.parser.scanondemand.nesting

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_multi_level_same_lookahead : test_ScanOnDemandParserAbstract() {

    // S = A | B
    // A = Y a
    // B = Y b
    // Y = U | V
    // U = Q u?
    // V = Q v?
    // Q = q


    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("A")
                ref("B")
            }
            concatenation("A") { ref("Y"); literal("a") }
            concatenation("B") { ref("Y"); literal("b") }
            choice("Y", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("U")
                ref("V")
            }
            concatenation("U") { ref("Q"); ref("uOpt") }
            concatenation("V") { ref("Q"); ref("vOpt") }
            multi("uOpt", 0, 1, "'u'")
            multi("vOpt", 0, 1, "'v'")
            literal("'u'", "u")
            literal("'v'", "v")
            concatenation("Q") { literal("q") }
        }
    }

    @Test
    fun qa() {
        val goal = "S"
        val sentence = "qa"

        val expected = """
            S { A {
              Y { V {
                Q { 'q' }
                vOpt { §empty }
              } }
              'a'
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
    fun qua() {
        val goal = "S"
        val sentence = "qua"

        val expected = """
            S { A {
              Y { U {
                Q { 'q' }
                uOpt { 'u' }
              } }
              'a'
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
    fun qva() {
        val goal = "S"
        val sentence = "qva"

        val expected = """
            S { A {
              Y { V {
                Q { 'q' }
                vOpt { 'v' }
              } }
              'a'
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
    fun qb() {
        val goal = "S"
        val sentence = "qb"

        val expected = """
            S { B {
              Y { V {
                Q { 'q' }
                vOpt { §empty }
              } }
              'b'
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
    fun qub() {
        val goal = "S"
        val sentence = "qub"

        val expected = """
            S { B {
              Y { U {
                Q { 'q' }
                uOpt { 'u' }
              } }
              'b'
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
    fun qvb() {
        val goal = "S"
        val sentence = "qvb"

        val expected = """
            S { B {
              Y { V {
                Q { 'q' }
                vOpt { 'v' }
              } }
              'b'
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