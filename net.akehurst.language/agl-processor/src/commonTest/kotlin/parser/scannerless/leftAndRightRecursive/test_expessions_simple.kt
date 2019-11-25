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

package net.akehurst.language.parser.scannerless.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_expessions_simple : test_ScannerlessParserAbstract() {

    // S = E
    // E = 'a' | I
    // I = E 'o' E ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_o = b.literal("o")
        val r_I = b.rule("I").build()
        val r_E = b.rule("E").choiceEqual(r_a, r_I)
        b.rule(r_I).concatenation(r_E, r_o, r_E)
        val r_S = b.rule("S").concatenation(r_E)
        return b
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { E {'a'} }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoa"

        val expected = """
            S { E{I { E{'a'} 'o' E{'a'} } }}
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aoaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoaoa"

        //think this should be excluded because of priority I < 'a'
        val expected1 = """
            S { E { I {
                E { I {
                    E { 'a' }
                    'o'
                    E { 'a' }
                  } }
                'o'
                E { 'a' }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }


    @Test
    fun aoaoaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoaoaoa"

        val expected = """
             S { E { I {
                  E { I {
                      E { I {
                          E { 'a' }
                          'o'
                          E { 'a' }
                        } }
                      'o'
                      E { 'a' }
                    } }
                  'o'
                  E { 'a' }
                } } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

}