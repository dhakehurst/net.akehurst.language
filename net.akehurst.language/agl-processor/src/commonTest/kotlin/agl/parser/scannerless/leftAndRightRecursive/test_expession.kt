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

package net.akehurst.language.parser.scanondemand.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_expession : test_ScanOnDemandParserAbstract() {

    // S =  n < P < I ;      //  infix < propertyCall < name
    // n = 'a' ;             // "[a-z]+"
    // P = S 'p' n ;         // S '.' name
    // I = [S / 'o']2+ ;         // [S / '+']2+
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_n = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, b.literal("p"), r_n)
        val r_I = b.rule("I").separatedList(2,-1,b.literal("o"), r_S)
        b.rule(r_S).choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,r_n, r_P, r_I)
        return b
    }

    private val rrs = runtimeRuleSet {
        
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun apa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apa"

        val expected = """
            S|1 { P { S { 'a' } 'p' 'a' } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoa"

        val expected = """
            S|2 { I { S{'a'} 'o' S{'a'} } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aoaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoaoa"

        val expected = """
            S|2 { I { S{'a'} 'o' S{'a'} 'o' S{'a'} } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }


    @Test
    fun apaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apaoa"

        val expected = """
             S|2 { I {
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { 'a' }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aoapa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoapa"

        val expected = """
             S|2 { I {
                  S { 'a' }
                  'o'
                  S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun apaoapaoapa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apaoapaoapa"

        val expected = """
             S|2 { I {
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }
}