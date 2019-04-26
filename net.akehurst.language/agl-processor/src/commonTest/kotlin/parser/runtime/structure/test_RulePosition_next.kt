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

package net.akehurst.language.agl.runtime.structure

import kotlin.test.Test
import kotlin.test.assertEquals

class test_RulePosition_next {

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_S = rb.rule("S").empty()
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, 0, 0).next()
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 0, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // choice equal
    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, 0, 0).next()
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 0, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_1_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, 1, 0).next()
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 1, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, 2, 0).next()
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 2, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // choice priority

    // concatenation

    // - multi

    // -- S = 'a'? ;
    @Test
    fun multi01__S_Empty_start() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.START_OF_RULE).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Empty_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_start() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.START_OF_RULE).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_mid() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.MULIT_ITEM_POSITION).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.MULIT_ITEM_POSITION),
                RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    // - sList

    // -- S = ['a' / ',']* ;
    @Test
    fun sList0n__S_Empty_start() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.START_OF_RULE).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Empty_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_start() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.START_OF_RULE).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE),
                RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.SLIST_SEPARATOR_POSITION)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 0).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 0).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_1__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_1__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_P_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }
}