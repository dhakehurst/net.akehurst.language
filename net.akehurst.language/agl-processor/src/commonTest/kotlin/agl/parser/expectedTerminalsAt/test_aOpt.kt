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

package net.akehurst.language.parser.expectedTerminalsAt

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

class test_aOpt {

    private data class Data(val sentence: String, val position: Int, val expected: List<RuntimeRule>)

    // skip WS = "\s+" ;
    // S = 'a'? ;
    private companion object {
        val rrs = runtimeRuleSet {
            skip("WS") { pattern("\\s+") }
            concatenation("S") { ref("aOpt") }
            multi("aOpt", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val parser = ScanOnDemandParser(rrs)
        val a = rrs.findTerminalRule("'a'")
        val EOT = RuntimeRuleSet.END_OF_TEXT

        val testData = listOf(
            Data("", 0, listOf(a, EOT)),
            Data(" ", 0, listOf(a, EOT)),
            Data(" ", 1, listOf(a, EOT)),
            Data("a", 0, listOf(a, EOT)),
            Data("a", 1, listOf(EOT)),
            Data(" a", 0, listOf(a, EOT)),
            Data(" a", 1, listOf(a, EOT)),
            Data(" a", 2, listOf(EOT)),
            Data("a ", 0, listOf(a, EOT)),
            Data("a ", 1, listOf(EOT)),
            Data("a ", 2, listOf(EOT)),
            Data(" a ", 0, listOf(a, EOT)),
            Data(" a ", 1, listOf(a, EOT)),
            Data(" a ", 2, listOf(EOT)),
            Data(" a ", 3, listOf(EOT)),
        )
    }

    @Test
    fun test() {
        for(data in testData) {
            val sentence = data.sentence
            val position = data.position

            val result = parser.expectedTerminalsAt(goal, sentence, position, AutomatonKind.LOOKAHEAD_1)
            val actual = result.filter { it.isEmptyRule.not() }
            val expected = data.expected
            assertEquals(expected, actual, "${data}")
        }
    }

}