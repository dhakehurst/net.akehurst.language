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

package net.akehurst.language.parser.scannerless.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_BillotLang_UBDA : test_ScannerlessParserAbstract() {
    /**
     * A = AA | 'a'
     */
    /**
     * A = A1 | 'a' ;
     * A1 = A A ;
     */
    private val S = runtimeRuleSet {
        choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("A1"); literal("a") }
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
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aa() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S {
              S2 {
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaa() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aaa"

        val expected1 = """
            S {
              S1 {
                S { 'a' }
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()


        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaaa() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aaaa"

        val expected1 = """
         S { S2 {
            S { S2 {
                S { 'a' }
                S { 'a' }
              } }
            S { S2 {
                S { 'a' }
                S { 'a' }
              } }
          } }
        """.trimIndent()


        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun a10() {
        val rrb = this.S
        val goal = "S"
        val sentence = "a".repeat(10)

        val expected1 = """
            S {
              S1 {
                S { 'a' }
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()


       super.test(rrb, goal, sentence, expected1)
    }
}