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

package net.akehurst.language.parser.scanondemand.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_expessions_hidden_right : test_LeftCornerParserAbstract() {

    // Simplification from SText Expressions

    // S = E
    // E = I | P      Expression
    // I = E 'o' E ;  AssignmentExpression
    // P = C | n      PrimaryExpression
    // C = R A?       FeatureCall
    // a = 'a'        ArgumentList
    // R = v A?       ElementReferenceExpression
    // n = 'n'        PrimitiveValueExpression
    // v = 'v'        ID

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("I")
                ref("P")
            }
            concatenation("I") { ref("E"); literal("o"); ref("E") }
            choice("P", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C")
                ref("n")
            }
            concatenation("C") { ref("R"); ref("Ao") }
            concatenation("R") { ref("v"); ref("Ao") }
            optional("Ao", "a")
            literal("a", "a")
            literal("v", "v")
            literal("n", "n")
        }
        val goal = "S"
    }

    @Test
    fun v() {
        val sentence = "v"

        val expected = """
            S { E { P { C {
              R {
                v:'v'
                Ao { §empty }
              }
              Ao { §empty }
            } } } }
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
    fun von() {
        // var = 5
        // ID '=' number
        val sentence = "von"

        val expected = """
            S { E { I {
              E { P { C {
                R { v:'v' Ao { §empty } }
                Ao { §empty }
              } } }
              'o'
              E { P { n:'n' } }
            } } }
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