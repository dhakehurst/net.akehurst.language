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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_a1bOa2 : test_ScannerlessParserAbstract() {

    // S = a1 b > a2;
    // a1 = 'a' ;
    // b = 'b' ;
    // a2 = 'a' ;
    private fun a1bOa2Literal(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_a1 = b.rule("a1").concatenation(r_a)
        val r_b = b.rule("b").concatenation(b.literal("b"))
        val r_a2 = b.rule("a2").concatenation(r_a)
        val r_a1b = b.rule("a1b").concatenation(r_a1, r_b)
        b.rule("S").choicePriority(r_a1b, r_a2)
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = this.a1bOa2Literal()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    @Test
    fun a() {
        val rrb = this.a1bOa2Literal()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S {
              a2 { 'a' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)

    }

    //TODO: more tests
}