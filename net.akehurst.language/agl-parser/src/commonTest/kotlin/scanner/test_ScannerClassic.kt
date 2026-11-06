/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.akehurst.language.scanner.common

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.sentence.common.SentenceDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_ScannerClassic {

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        } as RuntimeRuleSet
    }

    @Test
    fun blank() {
        val sut = ScannerClassic(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("", null))
        assertEquals(emptyList(), res.allTokens)
    }

    @Test
    fun scan__longer_pattern_beats_literal_prefix() {
        val sut = ScannerClassic(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("classes", null))
        assertEquals(listOf("NAME:0+7"), res.allTokens.map { "${it.name}:${it.position}+${it.length}" })
    }

    @Test
    fun scan__same_length_literal_beats_pattern() {
        val sut = ScannerClassic(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("class", null))
        assertEquals(listOf("'class':0+5"), res.allTokens.map { "${it.name}:${it.position}+${it.length}" })
    }

    @Test
    fun scan__literalOverLongerPattern_flag() {
        val sut = ScannerClassic(RegexEnginePlatform, rrs.terminals, literalOverLongerPattern = true)
        val res = sut.scan(SentenceDefault("classes", null))
        assertEquals(listOf("'class':0+5", "NAME:5+2"), res.allTokens.map { "${it.name}:${it.position}+${it.length}" })
    }

    @Test
    fun parse__keyword_prefix_of_identifier() {
        val sut = LeftCornerParser(ScannerClassic(RegexEnginePlatform, rrs.terminals), rrs)
        val res = sut.parse("class classes;", ParseOptionsDefault(goalRuleName = "S"))
        assertNotNull(res.sppt, res.issues.toString())
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun parse__keyword_prefix_of_identifier__literalOverLongerPattern_fails() {
        val sut = LeftCornerParser(ScannerClassic(RegexEnginePlatform, rrs.terminals, literalOverLongerPattern = true), rrs)
        val res = sut.parse("class classes;", ParseOptionsDefault(goalRuleName = "S"))
        assertNull(res.sppt)
    }

    @Test
    fun scan__resultsByLine() {
        val sut = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("class A;\nclass B;\nclass C;", null), ScanOptionsDefault(resultsByLine = true))
        val actual = res.tokensByLine.map { ln -> ln.map { "${it.name}:${it.position}+${it.length}" } }
        val expected = listOf(
            listOf("'class':0+5", "WS:5+1", "NAME:6+1", "';':7+1", "WS:8+1"),
            listOf("'class':9+5", "WS:14+1", "NAME:15+1", "';':16+1", "WS:17+1"),
            listOf("'class':18+5", "WS:23+1", "NAME:24+1", "';':25+1"),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun scan__resultsByLine__token_over_eol_is_split() {
        val sut = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("A  \n  B", null), ScanOptionsDefault(resultsByLine = true))
        val actual = res.tokensByLine.map { ln -> ln.map { "${it.name}:${it.position}+${it.length}" } }
        val expected = listOf(
            listOf("NAME:0+1", "WS:1+2", "WS:3+1"),
            listOf("WS:4+2", "NAME:6+1"),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun scan__resultsByLine__offsetPosition() {
        val sut = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val res = sut.scan(SentenceDefault("A\nB", null), ScanOptionsDefault(resultsByLine = true, offsetPosition = 10))
        val actual = res.tokensByLine.map { ln -> ln.map { "${it.name}:${it.position}+${it.length}" } }
        val expected = listOf(
            listOf("NAME:10+1", "WS:11+1"),
            listOf("NAME:12+1"),
        )
        assertEquals(expected, actual)
    }
}
