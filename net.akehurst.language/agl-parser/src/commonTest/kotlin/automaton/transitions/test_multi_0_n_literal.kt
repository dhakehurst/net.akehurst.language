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

package net.akehurst.language.automaton.leftcorner.transitions

import net.akehurst.language.automaton.leftcorner.Transition
import kotlin.test.Test
import kotlin.test.assertEquals

class test_multi_0_n_literal {

    private fun check(actual: List<Transition>, expected: List<Transition>) {
        assertEquals(expected, actual)
    }

    @Test
    fun t() {
        TODO()
    }

    /*

    // S = 'a'*
    @Test
    fun empty() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").multi(0, -1, r_a)
        val rrs = rrb.ruleSet()
        val s0 = rrs.startingState(r_S)

        this.check(s0.transitions(null), listOf(
                Transition(s0, s0.stateSet.fetch(RuleOptionPosition(r_a, 0, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(r_a,RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true },
                Transition(s0, s0.stateSet.fetch(RuleOptionPosition(r_S.emptyRuleItem, 0, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true }
        ))

        val s1 = s0.stateSet.fetchOrCreateParseState(RuleOptionPosition(r_S.emptyRuleItem, 0, RuleOptionPosition.END_OF_RULE))
        this.check(s1.transitions(s0), listOf(
                Transition(s1, s0.stateSet.fetch(RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__EMPTY_RULE, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.HEIGHT, setOf(RuntimeRuleSet.END_OF_TEXT), RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__EMPTY_RULE, RuleOptionPosition.START_OF_RULE)) { _, _ -> true }
        ))

        val s2 = s0.stateSet.fetchOrCreateParseState(RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__EMPTY_RULE, RuleOptionPosition.END_OF_RULE))
        this.check(s2.transitions(s0), listOf(
                Transition(s2, s0.stateSet.fetch(RuleOptionPosition(s0.runtimeRule, 0, 1)), Transition.ParseAction.GRAFT, setOf(RuntimeRuleSet.END_OF_TEXT), RuleOptionPosition(s0.runtimeRule, 0, 0)) { _, _ -> true }
        ))
    }

    @Test
    fun a() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").multi(0, -1, r_a)
        val rrs = rrb.ruleSet()
        val s0 = rrs.startingState(r_S)

        this.check(s0.transitions(null), listOf(
                Transition(s0, s0.stateSet.fetch(RuleOptionPosition(r_a, 0, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(r_a,RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true },
                Transition(s0, s0.stateSet.fetch(RuleOptionPosition(r_S.emptyRuleItem, 0, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true }
        ))

        val s1 = s0.stateSet.fetchOrCreateParseState(RuleOptionPosition(r_a, 0, RuleOptionPosition.END_OF_RULE))
        this.check(s1.transitions(s0), listOf(
                Transition(s1, s0.stateSet.fetch(RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__ITEM, RuleOptionPosition.MULIT_ITEM_POSITION)), Transition.ParseAction.HEIGHT, setOf(r_a, RuntimeRuleSet.END_OF_TEXT), RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__ITEM, RuleOptionPosition.START_OF_RULE)){ _, _ -> true },
                Transition(s1, s0.stateSet.fetch(RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__ITEM, RuleOptionPosition.END_OF_RULE)), Transition.ParseAction.HEIGHT, setOf(RuntimeRuleSet.END_OF_TEXT), RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__ITEM, RuleOptionPosition.START_OF_RULE)) { _, _ -> true }
        ))

        val s2 = s0.stateSet.fetchOrCreateParseState(RuleOptionPosition(r_S, RuntimeRuleRhs.MULTI__EMPTY_RULE, RuleOptionPosition.END_OF_RULE))
        this.check(s2.transitions(s0), listOf(
                Transition(s2, s0.stateSet.fetch(RuleOptionPosition(s0.runtimeRule, 0, 1)), Transition.ParseAction.GRAFT, setOf(RuntimeRuleSet.END_OF_TEXT), RuleOptionPosition(s0.runtimeRule, 0, 0)) { _, _ -> true }
        ))
    }

*/
}