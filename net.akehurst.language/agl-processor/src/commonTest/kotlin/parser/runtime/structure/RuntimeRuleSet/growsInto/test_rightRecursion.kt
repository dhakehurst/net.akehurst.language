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

import net.akehurst.language.agl.runtime.structure.*
import parser.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates.assertDeepEquals
import kotlin.test.Test

class test_rightRecursion {

    companion object {
        val setEOT = setOf(RuntimeRuleSet.END_OF_TEXT)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun growsInto__G00__aE() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = test_concatenation.setEOT
        val glh = test_concatenation.setEOT
        val lookingFor = RulePositionState(StateNumber(1), rp_a_E, null,hlh, glh)

        val actual = sut.growsInto(r_S, s0, lookingFor)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0), null,emptySet(),setEOT)
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun simple_RRecursive_lookahead__S1_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = test_concatenation.setEOT
        val glh = test_concatenation.setEOT
        val lookingFor = RulePositionState(StateNumber(1), rp_a_E, null,hlh, glh)

        val actual = sut.growsInto(r_S, s0, lookingFor)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0),null, emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun simple_RRecursive_lookahead__S1_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val rp_a_E = RulePosition(r_a, 0, RulePosition.END_OF_RULE)
        val hlh = test_concatenation.setEOT
        val glh = test_concatenation.setEOT
        val lookingFor = RulePositionState(StateNumber(1), rp_a_E, null,hlh, glh)

        val actual = sut.growsInto(r_S, s0, lookingFor)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0), null,emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

}