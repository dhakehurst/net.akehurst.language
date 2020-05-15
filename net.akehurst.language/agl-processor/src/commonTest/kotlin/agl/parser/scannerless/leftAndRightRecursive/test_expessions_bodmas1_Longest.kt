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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_expessions_bodmas1_Longest : test_ScanOnDemandParserAbstract() {

    // S = E
    // E = var | I | '(' E ')'
    // I = E op E ;
    // op = '/' | 'M' | '+' | '-'
    // var = "[a-z]+"
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_var = b.rule("var").concatenation(b.pattern("[a-z]+"))
        val r_op = b.rule("op").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,b.literal("/"), b.literal("*"),b.literal("+"),b.literal("-"))
        val r_I = b.rule("I").build()
        val r_par = b.rule("par").build()
        val r_E = b.rule("E").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_var, r_I, r_par)
        b.rule(r_par).concatenation(b.literal("("), r_E, b.literal(")"))
        b.rule(r_I).concatenation(r_E, r_op, r_E)
        val r_S = b.rule("S").concatenation(r_E)
        return b
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { E { var { "[a-z]+":'a' } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun vav() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "v+v"

        val expected = """
            S { E|1 { I {
              E { var { "[a-z]+":'v' } }
              op { '+' }
              E { var { "[a-z]+":'v' } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun vavav() {
        val rrb = this.S()
        val goal = "S"
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

        super.testStringResult(rrb, goal, sentence, expected1)
    }


    @Test
    fun vavavav() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "v+v+v+v"

        val expected = """
             S { E|1 { I {
                  E|1 { I {
                      E|1 { I {
                          E { var { "[a-z]+":'a' } }
                          op|2 { '+' }
                          E { var { "[a-z]+":'a' } }
                        } }
                      op|2 { '+' }
                      E { var { "[a-z]+":'a' } }
                    } }
                  op|2 { '+' }
                  E { var { "[a-z]+":'a' } }
                } } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

}