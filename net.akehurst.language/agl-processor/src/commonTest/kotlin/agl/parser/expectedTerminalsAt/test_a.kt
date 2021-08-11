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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

class test_a {

    private data class Data(val sentence: String, val position: Int, val expected: List<String>)

    // skip WS = "\s+" ;
    // S = 'a' ;
    private companion object {
        val rrs = runtimeRuleSet {
            skip("WS") { pattern("\\s+") }
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val parser = ScanOnDemandParser(rrs)

        val testData = listOf(
            Data("", 0, listOf("a")),
            Data(" ", 0, listOf("a")),
            Data(" ", 1, listOf("a")),
            Data("a", 0, listOf("a")),
            Data("a", 1, listOf()),
            Data(" a", 0, listOf("a")),
            Data(" a", 1, listOf("a")),
            Data(" a", 2, listOf()),
            Data("a ", 0, listOf("a")),
            Data("a ", 1, listOf()),
            Data("a ", 2, listOf()),
            Data(" a ", 0, listOf("a")),
            Data(" a ", 1, listOf("a")),
            Data(" a ", 2, listOf()),
            Data(" a ", 3, listOf()),
        )
    }

    @Test
    fun test() {
        for(data in testData) {
            val sentence = data.sentence
            val position = data.position

            val result = parser.expectedTerminalsAt(goal, sentence, position, AutomatonKind.LOOKAHEAD_1)
            val actual = result.filter { it.isEmptyRule.not() }.map { it.value }
            val expected = data.expected
            assertEquals(expected, actual,data.toString())
        }
    }

}