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

package net.akehurst.language.parser.scannerless.listSeparated

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_literal_a01 : test_ScannerlessParserAbstract() {

    // S = ['a' / ',']?
    private fun literal_a01(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").separatedList(0, 1, b.literal(","), r_a)
        return b
    }

    @Test
    fun empty() {
        val b = literal_a01()
        val goal = "S"
        val sentence = ""

        val expected = "S { §empty }"

        super.test(b, goal, sentence, expected)
    }

    @Test
    fun a() {
        val b = literal_a01()
        val goal = "S"
        val sentence = "a"

        val expected = "S {'a'}"

        super.test(b, goal, sentence, expected)
    }

    @Test
    fun aa_fails() {
        val b = literal_a01()
        val goal = "S"
        val sentence = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun ac_fails() {
        val b = literal_a01()
        val goal = "S"
        val sentence = "a,"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun aca_fails() {
        val b = literal_a01()
        val goal = "S"
        val sentence = "a,a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

}