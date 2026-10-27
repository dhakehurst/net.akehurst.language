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

/**
 * expectedAt where tokens follow each other without whitespace,
 * the start of a possibly unfinished word is the start of the last token (not the end of the last whitespace)
 */
class test_expectedAt_tokenStart {

    private companion object {
        val navigation = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { ref("NAME"); literal("."); ref("NAME") }
            pattern("NAME", "[a-z]+")
        } as RuntimeRuleSet

        // no skip rules
        val noSkip = runtimeRuleSet {
            concatenation("S") { literal("if"); ref("NUM"); literal("then") }
            pattern("NUM", "[0-9]+")
        } as RuntimeRuleSet

        val abc = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            choiceLongest("S") { ref("ab"); ref("abcx") }
            concatenation("ab") { literal("a"); literal("b") }
            concatenation("abcx") { literal("abc"); literal("x") }
        } as RuntimeRuleSet
    }

    /**
     * expected: map of each spine's usedPosition to the expected terminals for that position
     */
    private fun check(rrs: RuntimeRuleSet, text: String, position: Int, expectedUsedPosition: Int, expected: Map<Int, Set<String>>, allTokenStarts: Boolean = false) {
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val res = sut.expectedAt(text, position, ParseOptionsDefault(goalRuleName = "S", expectedAtFromAllTokenStarts = allTokenStarts))
        assertEquals(expectedUsedPosition, res.usedPosition, "usedPosition for '$text' at $position")
        val actual = res.spines
            .groupBy { it.usedPosition }
            .mapValues { (_, sps) -> sps.flatMap { it.expectedNextTerminals }.map { it.tag }.toSet() }
        assertEquals(expected, actual, "expected for '$text' at $position")
    }

    @Test
    fun navigation_after_dot() = check(navigation, "x.", 2, 1, mapOf(1 to setOf("'.'"), 2 to setOf("NAME")))

    @Test
    fun navigation_partial_name_after_dot() = check(navigation, "x.fo", 4, 2, mapOf(2 to setOf("NAME"), 4 to setOf("<EOT>")))

    @Test
    fun navigation_partial_name_after_dot_with_whitespace_before() = check(navigation, "x . fo", 6, 4, mapOf(4 to setOf("NAME"), 6 to setOf("<EOT>")))

    @Test
    fun noSkip_partial_keyword() = check(noSkip, "i", 1, 0, mapOf(0 to setOf("'if'")))

    @Test
    fun noSkip_keyword() = check(noSkip, "if", 2, 0, mapOf(0 to setOf("'if'"), 2 to setOf("NUM")))

    @Test
    fun noSkip_number() = check(noSkip, "if1", 3, 2, mapOf(2 to setOf("NUM"), 3 to setOf("'then'")))

    // 'th' is not a token, so the word starts where the unscannable text starts
    @Test
    fun noSkip_partial_keyword_after_number() = check(noSkip, "if1th", 5, 3, mapOf(3 to setOf("'then'")))

    @Test
    fun noSkip_complete() = check(noSkip, "if1then", 7, 3, mapOf(3 to setOf("'then'"), 7 to setOf("<EOT>")))

    @Test
    fun lastTokenStart_only() = check(abc, "ab", 2, 1, mapOf(1 to setOf("'b'"), 2 to setOf("<EOT>")))

    @Test
    fun allTokenStarts() = check(abc, "ab", 2, 1, mapOf(0 to setOf("'abc'"), 1 to setOf("'b'"), 2 to setOf("<EOT>")), allTokenStarts = true)

    @Test
    fun allTokenStarts_navigation() = check(navigation, "x.fo", 4, 2, mapOf(0 to setOf("NAME"), 2 to setOf("NAME"), 4 to setOf("<EOT>")), allTokenStarts = true)
}
