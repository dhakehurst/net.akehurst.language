/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Sentences of grammars with empty rules must be accepted whatever the processing order,
 * whatever was parsed before, and whether or not the automaton was built up front.
 */
class test_empty_orderAndRecursion {

    private companion object {
        fun parser(rrs: RuntimeRuleSet) = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

        fun assertParses(parser: LeftCornerParser, sentence: String, context: String) {
            val res = parser.parse(sentence, ParseOptionsDefault(goalRuleName = "S"))
            assertTrue(null != res.sppt && res.issues.errors.isEmpty(), "$context '$sentence': ${res.issues}")
        }

        /** fresh, after each of 'before', and pre-built */
        fun assertParsesInAllModes(rrsFor: () -> RuntimeRuleSet, sentence: String, before: List<String>) {
            assertParses(parser(rrsFor()), sentence, "fresh")
            for (b in before) {
                val p = parser(rrsFor())
                p.parse(b, ParseOptionsDefault(goalRuleName = "S"))
                assertParses(p, sentence, "after '$b'")
            }
            val p = parser(rrsFor())
            p.buildFor("S")
            assertParses(p, sentence, "pre-built")
        }
    }

    // S = N1 N3 'b' | N1 'a' ; N1 = <EMPTY> ; N2 = N3 ; N3 = S | <EMPTY>
    // A node that was already grown got a new previous at the same position and was never grown over it,
    // so 'bb' was rejected in most runs (the result depended on hash set iteration order).
    private fun lateEdge() = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("N1"); ref("N3"); literal("b") }
            concatenation { ref("N1"); literal("a") }
        }
        choice("N1", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { empty() } }
        choice("N2", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { ref("N3") } }
        choice("N3", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("S") }
            concatenation { empty() }
        }
    }

    @Test
    fun lateEdge_bb() {
        repeat(20) { assertParsesInAllModes(::lateEdge, "bb", listOf("", "a", "b", "aa", "ab", "ba")) }
    }

    @Test
    fun lateEdge_b_bbb() {
        repeat(5) {
            assertParsesInAllModes(::lateEdge, "b", listOf("a", "bb"))
            assertParsesInAllModes(::lateEdge, "bbb", listOf("a", "bb"))
        }
    }

    // S = 'a' N3 ; N3 = N1 N3 | <EMPTY> ; N1 = N3 'b'
    // The first-set of N3 was computed with N3 (under computation) taken as not nullable, so 'b' was missing
    // from the lookahead after 'a' and 'ab' was rejected.
    private fun nullableRecursion() = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { literal("a"); ref("N3") } }
        choice("N3", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("N1"); ref("N3") }
            concatenation { empty() }
        }
        choice("N1", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { ref("N3"); literal("b") } }
    }

    @Test
    fun nullableRecursion_a() = assertParsesInAllModes(::nullableRecursion, "a", listOf("ab"))

    @Test
    fun nullableRecursion_ab() = assertParsesInAllModes(::nullableRecursion, "ab", listOf("a", "abb"))

    @Test
    fun nullableRecursion_abb() = assertParsesInAllModes(::nullableRecursion, "abb", listOf("a", "ab"))

    // S = N1 'a' N1 | <EMPTY> ; N1 = S 'b' | S
    // At position 1 the empty N1 can GRAFT into the outer S or HEIGHT into an inner S;
    // preferring the GRAFT dropped the HEIGHT that is needed, so 'aabb' was rejected.
    private fun graftAndHeight() = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("N1"); literal("a"); ref("N1") }
            concatenation { empty() }
        }
        choice("N1", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("S"); literal("b") }
            concatenation { ref("S") }
        }
    }

    @Test
    fun graftAndHeight_aabb() = assertParsesInAllModes(::graftAndHeight, "aabb", listOf("a", "ab", "aab"))

    @Test
    fun graftAndHeight_a_ab_aab() {
        for (s in listOf("a", "ab", "ba", "aab", "aba")) assertParsesInAllModes(::graftAndHeight, s, listOf("aabb"))
    }

    // S = N3 | S N1 'a' | N3 N3 ; N1 = <EMPTY> | N2 'b' ; N2 = S N1 | 'a' 'b' ; N3 = <EMPTY>
    private fun leftRecursiveEmpties() = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("N3") }
            concatenation { ref("S"); ref("N1"); literal("a") }
            concatenation { ref("N3"); ref("N3") }
        }
        choice("N1", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { empty() }
            concatenation { ref("N2"); literal("b") }
        }
        choice("N2", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("S"); ref("N1") }
            concatenation { literal("a"); literal("b") }
        }
        choice("N3", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { empty() } }
    }

    @Test
    fun leftRecursiveEmpties_bba() = assertParsesInAllModes(::leftRecursiveEmpties, "bba", listOf("a", "ba"))

    // S = 'a' N3 S | N2 N1 N2 ; N1 = <EMPTY> ; N2 = N3 N3 'b' ; N3 = <EMPTY> | 'b'
    // A node that was made a head again (it gained a new previous after it was grown) was removed from the GSS,
    // together with its tree data, when the last stack above it was dropped before it was grown again,
    // so it was never grown over its new previous. 'abb' was rejected after parsing 'bb' and with a pre-built automaton.
    private fun regrowDropped() = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { literal("a"); ref("N3"); ref("S") }
            concatenation { ref("N2"); ref("N1"); ref("N2") }
        }
        choice("N1", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { empty() } }
        choice("N2", RuntimeRuleChoiceKind.AMBIGUOUS) { concatenation { ref("N3"); ref("N3"); literal("b") } }
        choice("N3", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { empty() }
            concatenation { literal("b") }
        }
    }

    @Test
    fun regrowDropped_abb() = assertParsesInAllModes(::regrowDropped, "abb", listOf("bb", "bbb", "b", "ab"))
}
