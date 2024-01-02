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

internal class test_bodmas_exprOpRuleExpr_Longest : test_LeftCornerParserAbstract() {

    // S = E
    // E = var | I | '(' E ')'
    // I = E op E ;
    // op = '/' | '*' | '+' | '-'
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
            choice("op", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("/")
                literal("*")
                literal("+")
                literal("-")
            }
            concatenation("par") { literal("("); ref("E"); literal(")") }
            concatenation("var") { pattern("[a-z]+") }
        }
        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { E { var { "[a-z]+":'a' } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b() {
        val sentence = "a+b"

        val expected = """
            S { E { I {
              E { var { "[a-z]+":'a' } }
              op { '+' }
              E { var { "[a-z]+":'b' } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun vavav() {
        val sentence = "v+v+v"

        val expected = """
            S { E { I {
                E { I {
                    E { var { "[a-z]+":'v' } }
                    op { '+' }
                    E { var { "[a-z]+":'v' } }
                  } }
                op { '+' }
                E { var { "[a-z]+":'v' } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun vavavav() {
        val sentence = "v+v+v+v"

        val expected = """
 S { E { I {
      E { I {
          E { I {
              E { var { "[a-z]+" : 'v' } }
              op { '+' }
              E { var { "[a-z]+" : 'v' } }
            } }
          op { '+' }
          E { var { "[a-z]+" : 'v' } }
        } }
      op { '+' }
      E { var { "[a-z]+" : 'v' } }
    } } }
        """.trimIndent()


        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun vavmv() {
        val sentence = "v+v*v"

        val expected = """
            S { E { I {
                E { var { "[a-z]+":'v' } }
                op { '+' }
                E { I {
                    E { var { "[a-z]+":'v' } }
                    op { '*' }
                    E { var { "[a-z]+":'v' } }
                  } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }
}