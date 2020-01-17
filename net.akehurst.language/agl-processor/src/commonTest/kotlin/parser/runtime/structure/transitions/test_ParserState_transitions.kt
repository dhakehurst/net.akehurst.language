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

class test_ParserState_transitions {

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_S = rb.rule("S").empty()
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    // choice equal
    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__G_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_b, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_c, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun choiceEquals__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, 0, 0))
        s1.addParentRelation(ParentRelation(s0.rulePosition, setOf(RuntimeRuleSet.END_OF_TEXT)))

        val actual = s1.transitions(rrs, null)
        val expected = setOf(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_1_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, 1, 0))

        val actual = s1.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_b, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, 2, 0))

        val actual = s1.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_c, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    // choice priority

    // concatenation

    // - multi

    // -- S = 'a'? ;
    @Test
    fun multi01__G_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__emptyItem_0_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE))

        val actual = s1.transitions(rrs, s0.rulePosition)
        val expected = setOf<Transition>(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE)), Transition.ParseAction.HEIGHT, setOf(RuntimeRuleSet.END_OF_TEXT), RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.START_OF_RULE)) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Empty_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE))
        s1.transitions(rrs, s0.rulePosition)
        val s2 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE))

        val actual = s2.transitions(rrs, s0.rulePosition)
        val expected = setOf<Transition>(
                Transition(s2, s0.stateSet.fetchOrCreateParseState(RulePosition(s0.runtimeRule, 0, 1)), Transition.ParseAction.GRAFT, setOf(RuntimeRuleSet.END_OF_TEXT), s0.rulePosition) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_start() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.START_OF_RULE))

        val actual = s1.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_mid() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        s0.transitions(rrs, null)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.MULIT_ITEM_POSITION))

        val actual = s1.transitions(rrs, RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.START_OF_RULE))
        val expected = setOf<Transition>(
                Transition(s1, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    // - sList

    // -- S = ['a' / ',']* ;
    @Test
    fun sList0n__G_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s0, s0.stateSet.fetch(RulePosition(r_a, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(r_c, RuntimeRuleSet.END_OF_TEXT), null) { true },
                Transition(s0, s0.stateSet.fetch(RulePosition(r_S.emptyRuleItem, 0, RulePosition.END_OF_RULE)), Transition.ParseAction.WIDTH, setOf(RuntimeRuleSet.END_OF_TEXT), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Empty_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)
        val s1 = s0.stateSet.fetchOrCreateParseState(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE))

        val actual = s1.transitions(rrs, null)
        val expected = setOf<Transition>(
                Transition(s1, s1, Transition.ParseAction.GOAL, emptySet(), null) { true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val rrs = rb.ruleSet()
        val s0 = rrs.startingState(r_S)

        val actual = s0.transitions(rrs, null)
        val expected = setOf<Transition>(

        )

        assertEquals(expected, actual)
    }
}