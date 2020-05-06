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

package net.akehurst.language.parser.scannerless.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class test_hiddenLeft1 : test_ScannerlessParserAbstract() {

    // S  = S1 | 'a'
    // S1 = E S 'a'    // S*; try right recursive also
    // E = empty
    private val S = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("S1")
            literal("a")
        }
        concatenation("S1") { ref("E"); ref("S"); literal("a") }
        concatenation("E") { empty() }
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
    fun a() {
        val rrb = this.S
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aa() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aa"

        val expected = """
         S { S1 {
            E { §empty }
            S { 'a' }
            'a'
          } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aaa() {
        val rrb = this.S
        val goal = "S"
        val sentence = "aaa"

        val expected = """
         S { S1 {
            E { §empty }
            S { 'a' }
            'a'
          } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }
}