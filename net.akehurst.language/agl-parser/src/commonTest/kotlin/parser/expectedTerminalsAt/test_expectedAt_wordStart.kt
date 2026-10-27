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
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        } as RuntimeRuleSet
    }

    private fun check(text: String, position: Int, expectedUsedPosition: Int, expected: Set<String>) {
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val res = sut.expectedAt(text, position, ParseOptionsDefault(goalRuleName = "S"))
        assertEquals(expectedUsedPosition, res.usedPosition, "usedPosition for '$text' at $position")
        assertEquals(expected, res.spines.flatMap { it.expectedNextTerminals }.map { it.tag }.toSet(), "expected for '$text' at $position")
    }

    @Test
    fun unfinished_first_word() = check("cla", 3, 0, setOf("'class'"))

    @Test
    fun after_whitespace() = check("class ", 6, 6, setOf("NAME"))

    @Test
    fun middle_of_word_after_whitespace() = check("class Abc", 7, 6, setOf("NAME"))

    @Test
    fun end_of_word_after_whitespace() = check("class Abc", 9, 6, setOf("NAME"))

    @Test
    fun end_of_word_after_several_whitespace() = check("class   Abc", 11, 8, setOf("NAME"))
}
