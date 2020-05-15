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
    private val S = runtimeRuleSet {
        choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("a"); ref("A1") }
        concatenation("A1") { ref("A"); ref("A"); }
    }

    @Test
    fun empty_fails() {
        val rrb = this.S
        val goal = "A"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun a() {
        val rrb = this.S
        val goal = "A"
        val sentence = "a"

        val expected1 = """
            A|1 { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aa() {
        val rrb = this.S
        val goal = "A"
        val sentence = "aa"

        val expected1 = """
            A {
              A1 {
                A|1 { 'a' }
                A|1 { 'a' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaa() {
        val rrb = this.S
        val goal = "A"
        val sentence = "aaa"

        val expected1 = """
         A { A1 {
            A { A1 {
                A|1 { 'a' }
                A|1 { 'a' }
              } }
            A|1 { 'a' }
          } }
        """.trimIndent()


        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaaa() {
        val rrb = this.S
        val goal = "A"
        val sentence = "aaaa"

        val expected1 = """
         A { A2 {
            A { A2 {
                A { 'a' }
                A { 'a' }
              } }
            A { A2 {
                A { 'a' }
                A { 'a' }
              } }
          } }
        """.trimIndent()


        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun a10() {
        val rrb = this.S
        val goal = "A"
        val sentence = "a".repeat(10)

        val expected1 = """
            A {
              A1 {
                A { 'a' }
                A { 'a' }
                A { 'a' }
              }
            }
        """.trimIndent()


        super.test(rrb, goal, sentence, expected1)
    }
}