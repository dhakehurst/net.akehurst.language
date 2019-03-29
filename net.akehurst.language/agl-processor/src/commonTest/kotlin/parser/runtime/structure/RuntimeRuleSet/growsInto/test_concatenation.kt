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

package net.akehurst.language.agl.runtime.structure.RuntimeRuleSet.growsInto

import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RulePositionState
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import parser.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates.assertDeepEquals
import kotlin.test.Test

class test_concatenation {

    companion object {
        val setEOT = setOf(RuntimeRuleSet.END_OF_TEXT)
    }

    // growsInto(userGoal, startingAt, lookingFor)

    // S = 'a' ;
    @Test
    fun concatenation1__G_0_0__aE() {
        // starting from RP(G = . S $)
        // RP('a' .) growsInto RP(S = . 'a') with hlh = setOf( $ )

        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = setEOT
        val glh = setEOT

        val lookingFor = RulePositionState(1, rp_a_E, hlh, glh)

        val actual = sut.growsInto(r_S, s0, lookingFor)
        val expected = setOf(
            RulePositionState(2, RulePosition(r_S, 0, 0), setEOT, setEOT)
        )

        assertDeepEquals(expected, actual)
    }

    // S = 'a' ;
    @Test
    fun concatenation1__S_0_0__aE() {
        // starting from RP(S = . 'a')
        // RP('a' .) growsInto RP(S = . 'a') with hlh = setOf( $ )

        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s2 = RulePositionState(0, RulePosition(r_S, 0, 0),setEOT, setEOT)
        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = setEOT
        val glh = setEOT
        val lookingFor = RulePositionState(1, rp_a_E, hlh, glh)

        val actual = sut.growsInto(gr, s2, lookingFor)
        val expected = setOf(
            RulePositionState(0, RulePosition(r_S, 0, 0), setEOT, setEOT)
        )

        assertDeepEquals(expected, actual)
    }


    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__G_0_0__aE() {
        // starting from RP(G = . S $)
        // RP('a' .) growsInto RP(S = . 'a' 'b' 'c') with hlh = setOf( 'b' )
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = setEOT
        val glh = setEOT
        val lookingFor = RulePositionState(1, rp_a_E, hlh, glh)

        val actual = sut.growsInto(gr, s0, lookingFor)
        val expected = setOf(
            RulePositionState(3, RulePosition(r_S, 0, 0), setOf(r_b), setEOT)
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__S_0_0() {
        // starting from RP(S = . 'a' 'b' 'c')
        // RP('a' .) growsInto RP(S = . 'a' 'b' 'c') with hlh = setOf( 'b' )
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s2 = RulePositionState(0, RulePosition(r_S, 0, 0), setOf(r_b), setOf(RuntimeRuleSet.END_OF_TEXT))
        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = setEOT
        val glh = setEOT
        val lookingFor = RulePositionState(1, rp_a_E, hlh, glh)

        val actual = sut.growsInto(gr, s2, lookingFor)
        val expected = setOf(
            RulePositionState(0, RulePosition(r_S, 0, 0), setOf(r_b), setEOT)
        )

        assertDeepEquals(expected, actual)
    }

}