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
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_ab_cOa_bc : test_ScanOnDemandParserAbstract() {

    // S = ab_c > a_bc;
    // ab_c = ab 'c'
    // a_bc = 'a' bc
    // ab = 'a' 'b'
    // bc = 'b' 'c'
    private val rrs = runtimeRuleSet {
        choice("S",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("ab_c")
            ref("a_bc")
        }
        concatenation("ab_c") { ref("ab"); literal("c") }
        concatenation("a_bc") { literal("a"); ref("bc") }
        concatenation("ab") { literal("a"); literal("b") }
        concatenation("bc") { literal("b"); literal("c") }
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'"), ex.expected)
    }

    @Test
    fun a_fails() {
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
        assertEquals(setOf("'b'"), ex.expected)
    }

    @Test
    fun b_fails() {
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
        assertEquals(setOf("'b'"), ex.expected)
    }

    @Test
    fun c_fails() {
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
        assertEquals(setOf("'b'"), ex.expected)
    }

    @Test
    fun ab_fails() {
        val goal = "S"
        val sentence = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(3, ex.location.column)
        assertEquals(setOf("'c'"), ex.expected)
    }

    @Test
    fun abc() {
        val goal = "S"
        val sentence = "abc"

        val expected = """
         S|1 { a_bc {
            'a'
            bc { 'b' 'c' }
          } }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }

}