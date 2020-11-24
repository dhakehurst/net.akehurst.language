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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_BillotLang_UBDA : test_ScanOnDemandParserAbstract() {
    /**
     * A = 'a' | AA
     */
    /**
     * A = 'a' | A1 ;
     * A1 = A A ;
     */
    private val rrs = runtimeRuleSet {
        choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("a"); ref("A1") }
        concatenation("A1") { ref("A"); ref("A"); }
    }

    @Test
    fun empty_fails() {
        val goal = "A"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun a() {
        val goal = "A"
        val sentence = "a"

        val expected = """
            A { 'a' }
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
    fun aa() {
        val goal = "A"
        val sentence = "aa"

        val expected = """
            A|1 {
              A1 {
                A { 'a' }
                A { 'a' }
              }
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
    fun aaa() {
        val goal = "A"
        val sentence = "aaa"

        val expected = """
         A|1 { A1 {
            A|1 { A1 {
                A { 'a' }
                A { 'a' }
              } }
            A { 'a' }
          } }
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
    fun aaaa() {
        val goal = "A"
        val sentence = "aaaa"

        val expected = """
         A|1 { A1 {
            A|1 { A1 {
                A { 'a' }
                A { 'a' }
              } }
            A|1 { A1 {
                A { 'a' }
                A { 'a' }
              } }
          } }
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
    fun a10() {
        val goal = "A"
        val sentence = "a".repeat(10)

        val expected = """
            A|1 {
              A1 {
                A { 'a' }
                A { 'a' }
                A { 'a' }
              }
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