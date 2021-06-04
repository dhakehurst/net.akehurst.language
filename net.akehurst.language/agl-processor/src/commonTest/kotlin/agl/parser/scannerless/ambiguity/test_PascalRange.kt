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

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.rightRecursive.test_hiddenRight3
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_PascalRange : test_ScanOnDemandParserAbstract() {

    /*
     * expr : range | real ;
     * range: integer '..' integer ;
     * integer : "[0-9]+" ;
     * real : "([0-9]+[.][0-9]*)|([.][0-9]+)" ;
     *
     */
    private companion object {
        val S = runtimeRuleSet {
            choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("range")
                ref("real")
            }
            concatenation("range") { ref("integer"); literal(".."); ref("integer") }
            concatenation("real") { pattern("([0-9]+[.][0-9]*)|([.][0-9]+)") }
            concatenation("integer") { pattern("[0-9]+") }
        }
        val goal = "expr"
    }

    @Test
    fun expr_empty_fails() {
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(S, goal, sentence,1)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun expr_1() {
        val sentence = "1."

        val expected = """
            expr|1 {
              real { "([0-9]+[.][0-9]*)|([.][0-9]+)" : '1.' }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun expr_1_to_5() {
        val sentence = "1..5"

        val expected = """
            expr {
              range {
                integer { "[0-9]+" : '1' }
                '..'
                integer { "[0-9]+" : '5' }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }
}