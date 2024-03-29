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
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_aOpt {

    private data class Data(val sentence: String, val position: Int, val expected: Set<RuntimeRule>)

    // skip WS = "\s+" ;
    // S = 'a'? ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("aOpt") }
            multi("aOpt", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val parser = ScanOnDemandParser(rrs)
        val a = rrs.findTerminalRule("'a'")
        val EOT = RuntimeRuleSet.END_OF_TEXT

        val testData = listOf(
            Data("", 0, setOf(a)),
            Data(" ", 0, setOf(a)),
            Data(" ", 1, setOf(a)),
            Data("a", 0, setOf(a)),
            Data("a", 1, setOf()),
            Data(" a", 0, setOf(a)),
            Data(" a", 1, setOf(a)),
            Data(" a", 2, setOf()),
            Data("a ", 0, setOf(a)),
            Data("a ", 1, setOf()),
            Data("a ", 2, setOf()),
            Data(" a ", 0, setOf(a)),
            Data(" a ", 1, setOf(a)),
            Data(" a ", 2, setOf()),
            Data(" a ", 3, setOf()),
        )
    }

    @Test
    fun test() {
        for (data in testData) {
            val sentence = data.sentence
            val position = data.position

            val result = parser.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
            val actual = result.filter { it.isEmptyTerminal.not() }.toSet()
            val expected = data.expected
            assertEquals(expected, actual, "${data}")
        }
    }

}