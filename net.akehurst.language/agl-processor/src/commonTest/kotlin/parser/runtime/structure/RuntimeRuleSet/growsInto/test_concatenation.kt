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


    // S = 'a' ;
    @Test
    fun concatenation1__G_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val actual = sut.growsInto(r_S, s0, RulePositionState(1, RulePosition(r_a, 0,0),setOf(RuntimeRuleSet.END_OF_TEXT),setOf(RuntimeRuleSet.END_OF_TEXT) ))
        val expected = setOf(
            RulePositionState(3, RulePosition(gr, 0, 1), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT)), //TODO: really lookahead should be emptySet!
            RulePositionState(4, RulePosition(r_S, 0, RulePosition.END_OF_RULE), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )

        assertDeepEquals(expected, actual)
    }

    // S = 'a' ;
    @Test
    fun concatenation1__G_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(0, RulePosition(gr, 0, 1), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(gr, 0, RulePosition.END_OF_RULE), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT)) //TODO: really lookahead should be emptySet!
        )

        assertDeepEquals(expected, actual)
    }

    // S = 'a' ;
    @Test
    fun concatenation1__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s2 = RulePositionState(0, RulePosition(r_S, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s2, s2.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_S, 0, RulePosition.END_OF_RULE), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT)) //TODO: really lookahead should be emptySet!
        )

        assertDeepEquals(expected, actual)
    }


    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__G_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val actual = sut.nextPossibleRulePositionStates(gr, s0, s0.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(gr, 0, 1), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT)), //TODO: really lookahead should be emptySet!
            RulePositionState(2, RulePosition(r_S, 0, 0), emptySet(),setOf(r_b))
        )
        assertDeepEquals(expected, actual)
    }

    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__G_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(0, RulePosition(gr, 0, 1), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(gr, 0, RulePosition.END_OF_RULE), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT)) //TODO: really lookahead should be emptySet!
        )

        assertDeepEquals(expected, actual)
    }

    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s2 = RulePositionState(0, RulePosition(r_S, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s2, s2.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_S, 0, 1), emptySet(),setOf(r_c))
        )

        assertDeepEquals(expected, actual)
    }

    // S = 'a' 'b' 'c' ;
    @Test
    fun concatenation3__S_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s2 = RulePositionState(0, RulePosition(r_S, 0, 1),  emptySet(),setOf(r_c))

        val actual = sut.nextPossibleRulePositionStates(gr, s2, s2.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_S, 0, 1), emptySet(),setOf(r_c))
        )

        assertDeepEquals(expected, actual)
    }

}