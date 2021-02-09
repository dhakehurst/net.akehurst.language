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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity2 : test_ScanOnDemandParserAbstract() {
    /**
     * S = 'a' | S S ;
     */
    /**
     * S = 'a' | S1 ;
     * S1 = S S ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val rrb = RuntimeRuleSetBuilder()
        val ra = rrb.literal("a")
        val rS = rrb.rule("S").build()
        val rS1 = rrb.rule("S1").concatenation(rS, rS)
        rS.rhsOpt = RuntimeRuleItem(RuntimeRuleRhsItemsKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, RuntimeRuleListKind.NONE,-1, 0, arrayOf(ra, rS1))
        return rrb
    }

    @Test
    fun S_S_empty() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun S_S_a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S {
              S1 {
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaa"

        val expected1 = """
            S|1 {
              S1 {
                S|1 {
                  S1 {
                    S { 'a' }
                    S { 'a' }
                  }
                }
                S { 'a' }
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)//, expected2)
    }


}