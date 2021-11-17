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

package net.akehurst.language.parser.scanondemand.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_RuntimeParser_parse_concatenation_nonTerm_literal : test_ScanOnDemandParserAbstract() {

    // S = ab 'c';
    // ab = 'a' 'b' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("ab"); literal("c") }
            concatenation("ab") { literal("a"); literal("b") }
        }
        val goal = "S"
    }

    @Test
    fun abc() {
        val sentence = "abc"

        val expected = """
            S{ ab {'a' 'b'} 'c' }
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