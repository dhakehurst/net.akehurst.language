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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_abOcLiteral : test_ScannerlessParserAbstract() {

    // r = a b > c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun abOcLiteral(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val ra = b.rule("a").concatenation(b.literal("a"))
        val rb = b.rule("b").concatenation(b.literal("b"))
        val rc = b.rule("c").concatenation(b.literal("c"))
        val rab = b.rule("ab").concatenation(ra, rb)
        b.rule("S").choicePriority(rab, rc)
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = this.abOcLiteral()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    @Test
    fun a_fails() {
        val rrb = this.abOcLiteral()
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ab() {
        val rrb = this.abOcLiteral()
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S {
              ab {
                a { 'a' }
                b { 'b' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun abc_fails() {
        val rrb = this.abOcLiteral()
        val goal = "S"
        val sentence = "abc"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun c() {
        val rrb = this.abOcLiteral()
        val goal = "S"
        val sentence = "c"

        val expected = """
            S {
              c { 'c' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

}