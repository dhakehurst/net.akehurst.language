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

package net.akehurst.language.agl.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates

import net.akehurst.language.agl.runtime.structure.*
import parser.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates.assertDeepEquals
import kotlin.test.Test

class test_leftRecursion {

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(StateNumber(1), RulePosition(r_a, 0, 2), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0),emptyList(), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }


    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S1_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(StateNumber(1), RulePosition(r_a, 0, 2), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S1_0_1_EOT() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(StateNumber(1), RulePosition(r_a, 0, 2), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0),emptyList(), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S1_0_1_() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(StateNumber(1), RulePosition(r_a, 0, 2), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(StateNumber(1), RulePosition(r_a, 0, 0), emptyList(),emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }
}