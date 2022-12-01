/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.agl.automaton.FirstOf
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_FirstOf : test_AutomatonUtilsAbstract() {

    val sut = FirstOf()

    private fun assert(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart, expected: Set<RuntimeRule>) {
        val actual = sut.expectedAt(rulePosition, ifReachedEnd)
        assertEquals(LookaheadSetPart.createFromRuntimeRules(expected), actual)
    }

    @Test
    fun leftRecursive() {
        // S =  'a' | S1
        // S1 = S 'a'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")

        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(G, o0, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S1, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S1, o0, p1), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S1, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(a, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun leftRecursive2() {
        // S =  'a' | S 'a'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                concatenation { ref("S"); literal("a") }
            }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(G, o0, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, p1), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(a, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

}