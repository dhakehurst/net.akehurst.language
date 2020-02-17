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

package net.akehurst.language.parser.scannerless.embedded

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_embedded1 : test_ScannerlessParserAbstract() {

    val Sn = runtimeRuleSet {
        concatenation("S") { literal("a"); ref("B"); literal("a"); }
        concatenation("B") { literal("b") }
    }

    @Test
    fun Sn_a_fails() {
        val rrb = this.Sn
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun Sn_aba() {
        val rrb = this.Sn
        val goal = "S"
        val sentence = "aba"

        val expected = """
            S {
              'a'
              B { 'b' }
              'a'
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    // B = b ;
    val B = runtimeRuleSet {
        concatenation("B") { literal("b") }
    }
    // S = a gB a ;
    // gB = grammar B ;
    val S = runtimeRuleSet {
        concatenation("S") { literal("a"); ref("gB"); literal("a"); }
        embedded("gB", B, B.findRuntimeRule("B"))
    }

    @Test
    fun empty_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun d_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun a_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ab_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = "ab"

        //val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        //}
       // assertEquals(1, ex.location.line)
       // assertEquals(1, ex.location.column)
    }

    @Test
    fun aba() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aba"

        val expected = """
            S {
              'a'
              B { 'b' }
              'a'
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }
}