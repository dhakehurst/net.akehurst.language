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

package net.akehurst.language.parser.scanondemand.choiceAmbiguous

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_a1bOa2 : test_ScanOnDemandParserAbstract() {

    // S = S1 < a
    // S1 = a b?
    private val deterministic= runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("S1")
            literal("a")
        }
        concatenation("S1") { literal("a"); ref("bOpt") }
        multi("bOpt",0,1,"'b'")
        literal("'b'","b")
    }

    @Test
    fun deterministic_empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(deterministic, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun deterministic_a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S|1 {
              'a'
            }
        """.trimIndent()

        super.test(deterministic, goal, sentence, expected)

    }

    @Test
    fun deterministic_ab() {
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.test(deterministic, goal, sentence, expected)

    }

    //TODO: more tests

    // S = S1 || a
    // S1 = a b?
    private val ambiguous = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            ref("S1")
            literal("a")
        }
        concatenation("S1") { literal("a"); ref("bOpt") }
        multi("bOpt",0,1,"'b'")
        literal("'b'","b")
    }

    @Test
    fun ambiguous_empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(ambiguous, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ambiguous_a() {
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S|1 {
              'a'
            }
        """.trimIndent()

        val expected2 = """
         S { S1 {
            'a'
            bOpt|1 { §empty }
          } }
        """.trimIndent()

        super.test(ambiguous, goal, sentence, expected1, expected2)

    }

    @Test
    fun ambiguous_ab() {
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.test(ambiguous, goal, sentence, expected)

    }

}