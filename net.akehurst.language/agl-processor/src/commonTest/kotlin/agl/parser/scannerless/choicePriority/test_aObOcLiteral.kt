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

package net.akehurst.language.parser.scannerless.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_aObOcLiteral : test_ScannerlessParserAbstract() {

    // S = a > b > c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun aObOcLiteral(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r1 = b.rule("a").concatenation(b.literal("a"))
        val r2 = b.rule("b").concatenation(b.literal("b"))
        val r3 = b.rule("c").concatenation(b.literal("c"))
        b.rule("S").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,r1, r2, r3)
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'","'b'","'c'"),ex.expected)
    }

    @Test
    fun a() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S {
              a { 'a' }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun b() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = "b"

        val expected = """
            S {
              b { 'b' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun c() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = "c"

        val expected = """
            S {
              c { 'c' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun d_fails() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ab_fails() {
        val rrb = this.aObOcLiteral()
        val goal = "S"
        val sentence = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

}