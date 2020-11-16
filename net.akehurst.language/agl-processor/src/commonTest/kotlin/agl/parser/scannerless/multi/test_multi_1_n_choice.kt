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

package net.akehurst.language.parser.scanondemand.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_multi_1_n_choice : test_ScanOnDemandParserAbstract() {

    // S = AB+
    // AB = a | b
    private val S = runtimeRuleSet {
        multi("S",1,-1,"AB")
        choice("AB",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            literal("b")
        }
    }

    @Test
    fun empty_fails() {
        val rrs = S
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun a() {
        val rrs = S
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { AB {'a'} }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun b() {
        val rrs = S
        val goal = "S"
        val sentence = "b"

        val expected = """
            S { AB|1 {'b'} }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun aa() {
        val rrs = S
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S { AB{'a'} AB{'a'} }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun aaa() {
        val rrs = S
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S { AB{'a'} AB{'a'} AB{'a'} }
        """.trimIndent()

        val actual =  super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a50() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S { "+"AB{'a'} ".repeat(50)+" }"

        val actual =  super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a500() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S { "+"AB{'a'} ".repeat(500)+" }"

        val actual =  super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a2000() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S { "+"AB{'a'} ".repeat(2000)+" }"

        val actual =  super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }
}