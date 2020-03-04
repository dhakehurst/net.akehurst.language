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

package net.akehurst.language.agl.runtime.structure.transitions

import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals

class test_multi_0_n_literal {

    private fun check(actual: Set<Transition>, expected: Set<Transition>) {
        assertEquals(expected, actual)
    }

    // S = 'a'*
    @Test
    fun empty() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").multi(0, -1, r_a)
        val rrs = rrb.ruleSet()
        val s0 = rrs.startingState(r_S)

        this.check(s0.transitions(rrs, null), setOf(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(r_a,RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true }
        ))

        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE), emptySet())
        this.check(s1.transitions(rrs, s0), setOf(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE)), Transition.ParseAction.HEIGHT, setOf(RuntimeRuleSet.END_OF_TEXT), RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.START_OF_RULE)) { _, _ -> true }
        ))

        val s2 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE), emptySet())
        this.check(s2.transitions(rrs, s0), setOf(
                Transition(s2, s0.stateSet.fetch(RulePosition(s0.runtimeRule, 0, 1)), Transition.ParseAction.GRAFT, setOf(RuntimeRuleSet.END_OF_TEXT), RulePosition(s0.runtimeRule, 0, 0)) { _, _ -> true }
        ))
    }

    @Test
    fun a() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").multi(0, -1, r_a)
        val rrs = rrb.ruleSet()
        val s0 = rrs.startingState(r_S)

        this.check(s0.transitions(rrs, null), setOf(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(r_a,RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { _, _ -> true }
        ))

        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_a, 0, RulePosition.END_OF_RULE), emptySet())
        this.check(s1.transitions(rrs, s0), setOf(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.MULIT_ITEM_POSITION)), Transition.ParseAction.HEIGHT, setOf(r_a, RuntimeRuleSet.END_OF_TEXT), RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.START_OF_RULE)){ _, _ -> true },
                Transition(s1, s0.stateSet.fetch(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)), Transition.ParseAction.HEIGHT, setOf(RuntimeRuleSet.END_OF_TEXT), RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.START_OF_RULE)) { _, _ -> true }
        ))

        val s2 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE), emptySet())
        this.check(s2.transitions(rrs, s0), setOf(
                Transition(s2, s0.stateSet.fetch(RulePosition(s0.runtimeRule, 0, 1)), Transition.ParseAction.GRAFT, setOf(RuntimeRuleSet.END_OF_TEXT), RulePosition(s0.runtimeRule, 0, 0)) { _, _ -> true }
        ))
    }


}