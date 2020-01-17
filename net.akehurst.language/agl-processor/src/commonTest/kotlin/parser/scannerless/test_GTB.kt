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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.Test

class test_GTB : test_ScannerlessParserAbstract() {

    /*
     * from [https://ac.els-cdn.com/S1571066104052211/1-s2.0-S1571066104052211-main.pdf?_tid=ebfa8627-2763-446d-b750-084833f9dd4c&acdnat=1548755247_c9590c54393a9cf75f34499780c7b400]
     * The Grammar Tool Box: A Case Study Comparing GLR Parsing Algorithms, Adrian Johnstone, Elizabeth Scott, Giorgios Economopoulos
     *
     * S = 'a' | A B | A 'z' ;
     * A = 'a' ;
     * B = 'b' | <empty> ;
     *
     */
    private fun S(): RuntimeRuleSetBuilder {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_b = rrb.literal("b")
        val r_A = rrb.rule("A").concatenation(r_a)
        val r_B = rrb.rule("B").build()
        val r_be= rrb.empty(r_B)
        r_B.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE,RuntimeRuleChoiceKind.LONGEST_PRIORITY,-1,0,arrayOf(r_b, r_be))
        val r_S1 = rrb.rule("S1").concatenation(r_A, r_B)
        val r_S2 = rrb.rule("S2").concatenation(r_A, rrb.literal("z"))
        val r_S = rrb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_a, r_S1, r_S2)
        return rrb
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S {
              'a'
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

}