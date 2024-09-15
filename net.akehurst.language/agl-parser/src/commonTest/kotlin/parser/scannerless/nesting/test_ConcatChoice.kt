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

package net.akehurst.language.parser.leftcorner.nesting

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_ConcatChoice : test_LeftCornerParserAbstract() {

    // rhs = concatenation | choice
    // choice = choicePriority | choiceLongest
    // choiceLongest = [concatenation / '|' ]2+
    // choicePriority = [concatenation / '<' ]2+
    // concatenation = concatItem+
    // concatItem = terminal | group
    // group = '(' groupedContent ')'
    // groupedContent = concatenation | choice

    // S = C | H
    // H = H1 | H2
    // H1 = [C / 's1']2+
    // H2 = [C / 's2']2+
    // C = I+
    // I = t | G
    // G = o CH c
    // CH = C | H

    private companion object {

        val rrs = runtimeRuleSet {
            //skip("WS") { pattern("\\s+") }
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C")
                ref("H")
            }
            choice("H",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("H1")
                ref("H2")
            }
            sList("H1",2,-1,"C","s1")
            sList("H2",2,-1,"C","s2")
            multi("C",1,-1,"I")
            choice("I",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("t")
                ref("G")
            }
            concatenation("G"){ ref("o"); ref("CH"); ref("c") }
            choice("CH",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C")
                ref("H")
            }
            literal("s1","s1")
            literal("s2","s2")
            literal("t","t")
            literal("o","o")
            literal("c","c")
        }
    }

    @Test
    fun ottc() {
        val goal = "S"
        val sentence = "ottc"

        val expected = """
            S { C { I { G {
              o:'o'
              CH { C {
                I { t:'t' }
                I { t:'t' }
              } }
              c:'c'
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


}