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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_n_P_Im : test_ScanOnDemandParserAbstract() {

    // S =  n < P < I ;      //  infix < propertyCall < name
    // n = 'a' ;             // "[a-z]+"
    // P = S 'p' n ;         // S '.' name
    // I = [S / 'o']2+ ;     // [S / '+']2+

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                literal("a")
                ref("P")
                ref("I")
            }
            sList("I",2,-1,"S","'o'")
            literal("'o'","o")
            concatenation("P") { ref("S"); literal("p"); literal("a") }
            //preferenceFor("S") {
            //    left("I", setOf())
            //    left("I", setOf())
            //    left("I", setOf())
            //}
        }

        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { 'a' }
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
    fun apa() {
        val sentence = "apa"

        val expected = """
            S|1 { P { S { 'a' } 'p' 'a' } }
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
    fun aoa() {
        val sentence = "aoa"

        val expected = """
            S|2 { I { S{'a'} 'o' S{'a'} } }
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
    fun aoaoa() {
        val sentence = "aoaoa"

        val expected = """
            S { I { S{'a'} 'o' S{'a'} 'o' S{'a'} } }
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
    fun aoaoaoaoa() {
        val sentence = "aoaoaoaoa"

        val expected = """
            S { I { S{'a'} 'o' S{'a'} 'o' S{'a'} 'o' S{'a'} 'o' S{'a'} } }
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
    fun apaoa() {
        val sentence = "apaoa"

        val expected = """
             S { I {
                S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { 'a' }
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
    fun aoapa() {
        val sentence = "aoapa"

        val expected = """
             S { I {
                  S { 'a' }
                  'o'
                  S { P {
                    S { 'a' }
                    'p'
                    'a'
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
    fun apaoapaoapa() {
        val sentence = "apaoapaoapa"

        val expected = """
             S { I {
                S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { P {
                    S { 'a' }
                    'p'
                    'a'
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
    fun apaoapaoapaoapa() {
        val sentence = "apaoapaoapaoapa"

        val expected = """
         S { I {
            S { P {
                S { 'a' }
                'p'
                'a'
              } }
            'o'
            S { P {
                S { 'a' }
                'p'
                'a'
              } }
            'o'
            S { P {
                S { 'a' }
                'p'
                'a'
              } }
            'o'
            S { P {
                S { 'a' }
                'p'
                'a'
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