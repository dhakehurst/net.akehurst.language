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
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.api.Matchable
import net.akehurst.language.sentence.common.SentenceDefault
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ScannerFromMatchables is used by the language.editor (AglComponents.simpleScanner, AglTokenizer)
 * for scanning text on the main thread. These tests cover the way the editor calls it.
 */
class test_ScannerFromMatchables {

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        } as RuntimeRuleSet

        // the editor receives the matchables from the processor's scanner
        val matchables: List<Matchable> = ScannerOnDemand(RegexEnginePlatform, rrs.terminals).matchables

        fun sut() = ScannerFromMatchables(RegexEnginePlatform) { matchables }

        val UNDEFINED = RuntimeRuleSet.UNDEFINED_RULE.tag

        fun List<net.akehurst.language.sppt.api.LeafData>.str() = this.map { "${it.name}:${it.position}+${it.length}" }
    }

    @Test
    fun empty_text() {
        val res = sut().scan(SentenceDefault("", null), ScanOptionsDefault(true, false, 0, 0))
        assertEquals(emptyList(), res.allTokens)
    }

    @Test
    fun no_options() {
        val res = sut().scan(SentenceDefault("class A;", null))
        assertEquals(listOf("'class':0+5", "WS:5+1", "NAME:6+1", "';':7+1"), res.allTokens.str())
    }

    @Test
    fun not_by_line__single_list_of_all_tokens() {
        // the language service sends result.tokensByLine; when resultsByLine is false it is one list
        val res = sut().scan(SentenceDefault("class A;\nclass B;", null), ScanOptionsDefault(true, false, 0, 0))
        assertEquals(1, res.tokensByLine.size)
        assertEquals(res.allTokens, res.tokensByLine[0])
        assertEquals(
            listOf("'class':0+5", "WS:5+1", "NAME:6+1", "';':7+1", "WS:8+1", "'class':9+5", "WS:14+1", "NAME:15+1", "';':16+1"),
            res.allTokens.str()
        )
    }

    @Test
    fun offsetPosition__as_used_by_editor_line_scan() {
        // AglTokenizer.getLineTokensByScan scans one line with offset = start of that line in the whole text
        val res = sut().scan(SentenceDefault("class B;\n", null), ScanOptionsDefault(true, false, 0, 9))
        assertEquals(listOf("'class':9+5", "WS:14+1", "NAME:15+1", "';':16+1", "WS:17+1"), res.allTokens.str())
    }

    @Test
    fun startAtPosition() {
        val res = sut().scan(SentenceDefault("class A;", null), ScanOptionsDefault(true, false, 6, 0))
        assertEquals(listOf("NAME:6+1", "';':7+1"), res.allTokens.str())
    }

    @Test
    fun undefined_text_in_middle_and_at_end() {
        val res = sut().scan(SentenceDefault("class ## A;%%", null), ScanOptionsDefault(true, false, 0, 0))
        assertEquals(
            listOf("'class':0+5", "WS:5+1", "$UNDEFINED:6+2", "WS:8+1", "NAME:9+1", "';':10+1", "$UNDEFINED:11+2"),
            res.allTokens.str()
        )
    }

    @Test
    fun undefined_text_with_offset() {
        val res = sut().scan(SentenceDefault("#A", null), ScanOptionsDefault(true, false, 0, 100))
        assertEquals(listOf("$UNDEFINED:100+1", "NAME:101+1"), res.allTokens.str())
    }

    @Test
    fun longest_match__keyword_prefix_of_identifier() {
        val res = sut().scan(SentenceDefault("classes class", null), ScanOptionsDefault(true, false, 0, 0))
        assertEquals(listOf("NAME:0+7", "WS:7+1", "'class':8+5"), res.allTokens.str())
    }

    @Test
    fun by_line_with_offset() {
        val res = sut().scan(SentenceDefault("class A;\nB", null), ScanOptionsDefault(true, true, 0, 9))
        assertEquals(
            listOf(
                listOf("'class':9+5", "WS:14+1", "NAME:15+1", "';':16+1", "WS:17+1"),
                listOf("NAME:18+1")
            ),
            res.tokensByLine.map { it.str() }
        )
    }
}
