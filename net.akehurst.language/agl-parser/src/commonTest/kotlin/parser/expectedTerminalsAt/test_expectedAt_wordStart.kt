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
package net.akehurst.language.parser.expectedTerminalsAt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals

class test_expectedAt_wordStart {

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            choiceLongest("S") { ref("class"); ref("interface") }
            concatenation("class") { literal("class"); ref("NAME"); literal(";") }
            concatenation("interface") { literal("interface"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        } as RuntimeRuleSet
    }

    /**
     * expected: map of each spine's usedPosition to the expected terminals for that position
     */
    private fun check(text: String, position: Int, expectedUsedPosition: Int, expected: Map<Int, Set<String>>) {
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val res = sut.expectedAt(text, position, ParseOptionsDefault(goalRuleName = "S"))
        assertEquals(expectedUsedPosition, res.usedPosition, "usedPosition for '$text' at $position")
        val actual = res.spines
            .groupBy { it.usedPosition }
            .mapValues { (_, sps) -> sps.flatMap { it.expectedNextTerminals }.map { it.tag }.toSet() }
        assertEquals(expected, actual, "expected for '$text' at $position")
    }

    @Test
    fun start_of_text() = check("", 0, 0, mapOf(0 to setOf("'class'", "'interface'")))

    // 'cla' can only be an unfinished 'class', parsing 'cla' as a complete word fails before position 3
    @Test
    fun unfinished_first_word() = check("cla", 3, 0, mapOf(0 to setOf("'class'")))

    // 'class' might be unfinished (e.g. 'classX') or complete
    @Test
    fun end_of_keyword() = check("class", 5, 0, mapOf(0 to setOf("'class'"), 5 to setOf("NAME")))

    @Test
    fun after_whitespace() = check("class ", 6, 6, mapOf(6 to setOf("NAME")))

    // 'A' might be the start of a NAME, or a complete NAME
    @Test
    fun middle_of_word_after_whitespace() = check("class Abc", 7, 6, mapOf(6 to setOf("NAME"), 7 to setOf("';'")))

    @Test
    fun end_of_word_after_whitespace() = check("class Abc", 9, 6, mapOf(6 to setOf("NAME"), 9 to setOf("';'")))

    @Test
    fun end_of_word_after_several_whitespace() = check("class   Abc", 11, 8, mapOf(8 to setOf("NAME"), 11 to setOf("';'")))

    // at 10 only ';' is expected, which cannot start with 'xyz', and parsing 'class Abc xyz' fails before 13
    @Test
    fun word_not_valid_here() = check("class Abc xyz", 13, 10, emptyMap())

    // the text is a complete sentence, so end-of-text is expected
    @Test
    fun end_of_complete_sentence() = check("class Abc ;", 11, 10, mapOf(10 to setOf("';'"), 11 to setOf("<EOT>")))

    @Test
    fun after_complete_sentence() = check("class Abc ; ", 12, 12, mapOf(12 to setOf("<EOT>")))
}
