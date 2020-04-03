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

class test_a1bOa2 : test_ScannerlessParserAbstract() {

    // S = S1 || a
    // S1 = a b?
    private fun ambiguous(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_bOpt = b.rule("bOpt").multi(0,1,b.literal("b"))
        val r_S1 = b.rule("S1").concatenation(r_a, r_bOpt)
        b.rule("S").choice(RuntimeRuleChoiceKind.AMBIGUOUS,r_S1, r_a)
        return b
    }

    @Test
    fun ambiguous_empty_fails() {
        val rrb = this.ambiguous()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ambiguous_a() {
        val rrb = this.ambiguous()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S {
              'a'
            }
        """.trimIndent()

        val expected2 = """
         S { S1 {
            'a'
            bOpt { §empty }
          } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1, expected2)

    }

    @Test
    fun ambiguous_ab() {
        val rrb = this.ambiguous()
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)

    }


    // S = S1 < a
    // S1 = a b?
    private fun deterministic(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_bOpt = b.rule("bOpt").multi(0,1,b.literal("b"))
        val r_S1 = b.rule("S1").concatenation(r_a, r_bOpt)
        b.rule("S").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,r_S1, r_a)
        return b
    }

    @Test
    fun deterministic_empty_fails() {
        val rrb = this.deterministic()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun deterministic_a() {
        val rrb = this.deterministic()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S {
              'a'
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)

    }

    @Test
    fun deterministic_ab() {
        val rrb = this.deterministic()
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)

    }

    //TODO: more tests
}