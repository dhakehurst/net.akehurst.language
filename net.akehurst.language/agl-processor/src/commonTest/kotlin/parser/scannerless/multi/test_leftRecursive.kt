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

package net.akehurst.language.parser.scannerless.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_leftRecursive : test_ScannerlessParserAbstract() {

    // S = P | 'a' ;
    // P =  S*;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").multi(0,-1,r_S)
        b.rule(r_S).choiceEqual(r_P, r_a)
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


    @Test
    fun t() {
        TODO()
        fail("TODO")
    }

}