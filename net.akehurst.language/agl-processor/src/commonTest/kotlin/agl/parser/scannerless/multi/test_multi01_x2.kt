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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_multi01_x2 : test_ScanOnDemandParserAbstract() {

    // S = A B V 'd'
    // A = 'a'?
    // B = 'b'?
    // V = "[a-c]"
    private companion object {
        private val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("V"); literal("d") }
            multi("A", 0, 1, "'a'")
            literal("'a'", "a")
            multi("B", 0, 1, "'b'")
            literal("'b'", "b")
            pattern("V", "[a-c]")
        }
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'","'b'","V"), e.expected)
    }

    @Test
    fun abcd() {
        val goal = "S"
        val sentence = "abcd"

        val expected = """
             S {
              A { 'a' }
              B { 'b' }
              V:'c'
              'd'
            }
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
    fun acd() {
        val goal = "S"
        val sentence = "acd"

        val expected = """
             S {
              A { 'a' }
              B|1 { §empty }
              V:'c'
              'd'
            }
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