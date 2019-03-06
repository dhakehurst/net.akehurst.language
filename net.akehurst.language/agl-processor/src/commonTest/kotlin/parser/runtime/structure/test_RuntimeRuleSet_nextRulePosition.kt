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

class test_RuntimeRuleSet_nextRulePosition {

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_S = rb.rule("S").empty()
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 0, 0), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 0, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // choice equal
    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_0_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 0, 0), r_a)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 0, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_0_0__b() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 0, 0), r_b)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_1_0__b() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 1, 0), r_b)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 1, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 2, 0), r_c)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, 2, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b, r_c)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, 2, 0), r_a)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    // choice priority

    // concatenation

    // multi

    // S = 'a'? ;
    @Test
    fun multi01__S_E_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_E_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), r_a)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_E_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__EMPTY_RULE, 1), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_I_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 0), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_I_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 1), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_I_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 0), r_a)
        val expected: Set<RulePosition> = setOf(
            RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 1),
            RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_I_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 1), r_a)
        val expected: Set<RulePosition> = setOf(
            RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, 1),
            RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    // sList

    // S = ['a' / ',']* ;
    @Test
    fun sList0n__S_E_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 0), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.END_OF_RULE))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 0), r_a)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_0__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 0), r_c)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_1__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 1), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_1__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 1), r_a)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_1__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 1), r_c)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 2), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 2), r_a)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_E_2__c() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE, 2), r_c)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__E() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 0), r_S.emptyRuleItem)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_I_0__a() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 0), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 0), r_c)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1), r_S.emptyRuleItem)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 1), r_c)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2), r_S.emptyRuleItem)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, 2), r_c)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0), r_S.emptyRuleItem)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 0), r_c)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1), r_S.emptyRuleItem)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 1), r_c)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2), r_S.emptyRuleItem)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2), r_a)
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
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual: Set<RulePosition> = sut.nextRulePosition(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2), r_c)
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }
}