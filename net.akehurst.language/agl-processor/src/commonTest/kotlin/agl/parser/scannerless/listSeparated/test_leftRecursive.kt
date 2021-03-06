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

package net.akehurst.language.parser.scanondemand.listSeparated

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.leftRecursive.test_a
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class test_leftRecursive  : test_ScanOnDemandParserAbstract() {

    // S = E
    // E = 'a' | L
    // L = [ E / ',' ]+;

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("L")
            }
            sList("L",1,-1,"E","','")
            literal("','",",")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test(rrs, goal, inputText,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun a() {
        TODO("does not stop")
        val sentence = "a"

        val expected = "S { E { 'a' } }"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aca() {
        TODO("does not stop")
        val sentence = "a,a"

        val expected = """
         S { E|1 { L {
              E { 'a' }
              ','
              E { 'a' }
            } } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun acaca() {
        TODO("does not stop")
        val sentence = "a,a,a"

        val expected = """
         S { E|1 { L {
              E { 'a' }
              ','
              E { 'a' }
            } } }
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