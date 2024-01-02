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

internal class test_bodmas_exprOpRuleExpr_Priority : test_LeftCornerParserAbstract() {

    // S = E
    // E = var | I | '(' E ')'
    // I = E op E ;
    // op = '/' < 'M' < '+' < '-'
    // var = "[a-z]+"
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("var")
                ref("I")
                ref("par")
            }
            concatenation("I") { ref("E"); ref("op"); ref("E") }
            choice("op", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                literal("/")
                literal("M")
                literal("+")
                literal("-")
            }
            concatenation("par") { literal("("); ref("E"); literal(")") }
            concatenation("var") { pattern("[a-z]+") }
        }
        val goal = "S"
    }

    @Test
    fun v() {
        val sentence = "v"

        val expected = """
            S { E { var { "[a-z]+":'v' } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun vav() {
        val sentence = "v+v"

        val expected = """
            S { E|1 { I {
              E{ var { "[a-z]+":'v' } }
              op|2 { '+' }
              E{var { "[a-z]+":'v' } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun vavav() {
        val sentence = "v+v+v"

        //think this should be excluded because of priority I < 'a'
        val expected1 = """
            S { E|1 { I {
                E|1 { I {
                    E { var { "[a-z]+":'v' } }
                    op|2 { '+' }
                    E { var { "[a-z]+":'v' } }
                  } }
                op|2 { '+' }
                E { var { "[a-z]+":'v' } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected1)
    }

    @Test
    fun vavavav() {
        val sentence = "v+v+v+v"

        val expected = """
 S { E|1 { I {
      E|1 { I {
          E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              op|2 { '+' }
              E { var { "[a-z]+" : 'v' } }
            } }
          op|2 { '+' }
          E { var { "[a-z]+" : 'v' } }
        } }
      op|2 { '+' }
      E { var { "[a-z]+" : 'v' } }
    } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}