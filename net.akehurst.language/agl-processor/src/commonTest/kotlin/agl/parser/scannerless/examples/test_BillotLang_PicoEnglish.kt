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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class test_BillotLang_PicoEnglish : test_ScanOnDemandParserAbstract() {
    /**
     * S = NP VP | S PP
     * NP = 'n' | 'det' 'n' | NP PP
     * VP = 'v' NP
     * PP = 'prep' NP
     */
    /**
     * S = S1 | S2
     * S1 = NP VP
     * S2 = S PP
     * NP = NP1 | NP2 | NP3
     * NP1 = 'n'
     * NP2 = 'd' 'n'
     * NP3 = NP PP
     * VP = 'v' NP
     * PP = 'p' NP
     */
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("S1"); ref("S2") }
        concatenation("S1") { ref("NP"); ref("VP") }
        concatenation("S2") { ref("S"); ref("PP") }
        choice("NP", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("NP1"); ref("NP2"); ref("NP3") }
        concatenation("NP1") { literal("n") }
        concatenation("NP2") { literal("d");literal("n") }
        concatenation("NP3") { ref("NP"); ref("PP") }
        concatenation("VP") { literal("v"); ref("NP") }
        concatenation("PP") { literal("p"); ref("NP") }
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun nvn() {
        val goal = "S"
        val sentence = "nvn"

        val expected = """
         S { S1 {
            NP { NP1 { 'n' } }
            VP {
              'v'
              NP { NP1 { 'n' } }
            }
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

}