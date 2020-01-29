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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_literal_a1n : test_ScannerlessParserAbstract() {

    // S = [a / ',']+
    // a = 'a'
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r0 = b.literal("a")
        val r1 = b.rule("S").separatedList(1, -1, b.literal(","), r0)
        return b
    }

    @Test
    fun empty() {
        val b = S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun a() {
        val b = S()
        val goal = "S"
        val sentence = "a"

        val expected = "S {'a'}"

        super.test(b, goal, sentence, expected)
    }

    @Test
    fun aa_fails() {
        val b = S()
        val goal = "S"
        val sentence = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun aca() {
        val b = S()
        val goal = "S"
        val sentence = "a,a"

        val expected = "S {'a' ',' 'a'}"

        super.test(b, goal, sentence, expected)
    }

    @Test
    fun acaa_fails() {
        val b = S()
        val goal = "S"
        val sentence = "a,aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(b, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)
    }

    @Test
    fun acaca() {
        val b = S()
        val goal = "S"
        val sentence = "a,a,a"

        val expected = "S {'a' ',' 'a' ',' 'a'}"

        super.test(b, goal, sentence, expected)
    }

    @Test
    fun acax100() {
        val b = S()
        val goal = "S"
        val sentence = "a"+",a".repeat(99)

        val expected = "S {'a'"+" ',' 'a'".repeat(99)+"}"

        super.test(b, goal, sentence, expected)
    }

}