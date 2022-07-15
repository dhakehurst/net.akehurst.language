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
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_bodmas_infix_sList_choiceEqual_simple : test_ScanOnDemandParserAbstract() {

    // S = E
    // E = 'v' | I | P
    // I = [E / op]2+ ;
    // op = '/' | '*' | '+' | '-'
    // P = '(' E ')'
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("v")
                ref("I")
                ref("P")
            }
            sList("I",2,-1,"E","op")
            choice("op", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                literal("/")
                literal("*")
                literal("+")
                literal("-")
            }
            concatenation("P") { literal("("); ref("E"); literal(")") }

        }
        val goal = "S"
    }

    @Test
    fun v() {
        val sentence = "v"

        val expected = """
            S { E { 'v' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }

    @Test
    fun vav() {
        val sentence = "v+v"

        val expected = """
         S { E|1 { I {
              E { 'v'  }
              op|2 { '+' }
              E { 'v'  }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }

    @Test
    fun vavav() {
        val sentence = "v+v+v"

        val expected = """
         S { E|1 { I {
              E {  'v'  }
              op|2 { '+' }
              E { 'v'  }
              op|2 { '+' }
              E { 'v'  }
            } } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            //printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }


    @Test
    fun vavavav() {
        val sentence = "v+v+v+v"

        val expected = """
          S { E|1 { I {
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }

    @Test
    fun vavavavav() {
        val sentence = "v+v+v+v+v"

        val expected = """
          S { E|1 { I {
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }

    @Test
    fun vdvmvavsv() {
        val sentence = "v/v*v+v-v"

        val expected = """
          S { E|1 { I {
              E { 'v' }
              op { '/' }
              E { 'v' }
              op|1 { '*' }
              E { 'v' }
              op|2 { '+' }
              E { 'v' }
              op|3 { '-' }
              E { 'v' }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }

    @Test
    fun pvdvpmpvavsvp() {
        val sentence = "(v/v)*(v+v-v)"

        val expected = """
            S { E { I {
              E { P {
                '('
                E { I {
                  E { 'v' } op { '/' } E { 'v' }
                } }
                ')'
              } }
              op { '*' }
              E { P {
                '('
                E { I {
                  E { 'v' }
                  op|2 { '+' }
                  E { 'v' }
                  op|3 { '-' }
                  E { 'v' }
                } }
                ')'
              } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1,expected)
    }
}