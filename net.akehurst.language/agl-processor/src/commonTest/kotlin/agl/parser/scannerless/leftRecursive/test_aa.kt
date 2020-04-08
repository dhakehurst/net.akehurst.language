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

package net.akehurst.language.parser.scannerless.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_aa : test_ScannerlessParserAbstract() {

    // S  = P | 'a' ;
    // P  = S | P1 ;  // S*
    // P1 = P S ;    // S*; try right recursive also
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P1 = b.rule("P1").build()
        val r_P = b.rule("P").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_S, r_P1)
        b.rule(r_P1).concatenation(r_P, r_S)
        b.rule(r_S).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_P, r_a)
        return b
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

    //@Test
    fun aa() {
        TODO("does not terminate!")
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S { P { P1 {
                P { S { 'a' } }
                S { 'a' }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

}