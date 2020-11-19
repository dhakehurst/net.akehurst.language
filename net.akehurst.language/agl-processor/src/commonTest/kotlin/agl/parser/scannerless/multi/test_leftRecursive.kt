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

package net.akehurst.language.parser.scanondemand.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_leftRecursive : test_ScanOnDemandParserAbstract() {

    // S = P | 'a' ;
    // P =  S+ ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").multi(1,-1,r_S)
        b.rule(r_S).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_P, r_a)
        return b
    }

    private val rrs = runtimeRuleSet {
        choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("P")
            literal("a")
        }
        multi("P",1,-1,"S")
    }

    @Test
    fun a() {
        fail("this does not terminate if we parse until !canGrow, ok it parse until first goal found!")
        val goal = "S"
        val sentence = "a"

        val expected = """
            S|1 { 'a' }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }


    @Test
    fun t() {
        TODO()
        fail("TODO")
    }

}