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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test

internal class test_a : test_ExpectedTerminasAtAbstract() {

    // skip leaf WS = "\s+" ;
    // S = 'a' ;
    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("a") }
        }
        val goal = "S"

        val testData = listOf(
            TestData("", 0, listOf("'a'")),
            TestData(" ", 0, listOf("'a'")),
            TestData(" ", 1, listOf("'a'")),
            TestData("a", 0, listOf("'a'")),
            TestData("a", 1, listOf()),
            TestData(" a", 0, listOf("'a'")),
            TestData(" a", 1, listOf("'a'")),
            TestData(" a", 2, listOf()),
            TestData("a ", 0, listOf("'a'")),
            TestData("a ", 1, listOf()),
            TestData("a ", 2, listOf()),
            TestData(" a ", 0, listOf("'a'")),
            TestData(" a ", 1, listOf("'a'")),
            TestData(" a ", 2, listOf()),
            TestData(" a ", 3, listOf()),
            TestData("ab", 0, listOf("'a'")),
            TestData("ab", 1, listOf()),
            TestData("ab", 2, listOf(RuntimeRuleSet.END_OF_TEXT_TAG)),
        )

    }

    @Test
    fun all() {
        for (i in testData.indices) {
            val td = testData[i]
            println("Test[$i]: At ${td.position} in '${td.sentence}'")
            test(rrs, goal, td)
        }
    }

    @Test
    fun one() {
        val i = 2
        val td = testData[i]
        println("Test[$i]: At ${td.position} in '${td.sentence}'")
        test(rrs, goal, td)
    }

}