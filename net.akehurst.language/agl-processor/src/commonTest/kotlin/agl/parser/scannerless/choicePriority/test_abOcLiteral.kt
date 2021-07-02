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

package net.akehurst.language.parser.scanondemand.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class test_abOcLiteral : test_ScanOnDemandParserAbstract() {

    // r = a b > c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private companion object {
        val S = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("ab")
                ref("c")
            }
            concatenation("ab") { ref("a"); ref("b") }
            concatenation("a") { literal("a") }
            concatenation("b") { literal("b") }
            concatenation("c") { literal("c") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(S, goal, sentence,1)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'", "'c'"), ex.expected)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(S, goal, sentence,1)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
        assertEquals(setOf("'b'"), ex.expected)
    }

    @Test
    fun ab() {
        val sentence = "ab"

        val expected = """
            S {
              ab {
                a { 'a' }
                b { 'b' }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun abc_fails() {
        val sentence = "abc"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(S, goal, sentence,1)
        }
        assertEquals(1, ex.location.line)
        assertEquals(3, ex.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), ex.expected)
    }

    @Test
    fun c() {
        val sentence = "c"

        val expected = """
            S|1 {
              c { 'c' }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}