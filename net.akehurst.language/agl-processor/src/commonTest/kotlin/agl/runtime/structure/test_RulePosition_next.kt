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
import kotlin.test.assertFailsWith

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
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
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
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
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
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_a, r_b, r_c)
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

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.POSITION_MULIT_ITEM).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.MULTI__ITEM, RulePosition.POSITION_MULIT_ITEM),
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
                RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_SEPARATOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_sep() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        assertFailsWith(IllegalStateException::class) {
            val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.POSITION_SLIST_SEPARATOR).next()
        }
    }

    @Test
    fun sList0n__S_Sep_sep() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_SEPARATOR).next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.POSITION_SLIST_ITEM)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__Sep_item() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        assertFailsWith(IllegalStateException::class) {
            val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_ITEM).next()
        }
    }

    @Test
    fun sList0n__S_Item_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = setOf()
//TODO: shoud this throw IllegalState ?
        assertEquals(expected, actual)
    }

    // -- S = ['a' / ',']+ ;
    val sList1n = runtimeRuleSet {
        sList("S", 1, -1, "'a'", "c")
        literal("c", ",")
        literal("'a'", "a")
    }

    @Test
    fun sList1n__S_Empty_start() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val actual = RulePosition(S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.START_OF_RULE).next()
        val expected: Set<RulePosition> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Empty_end() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val actual = RulePosition(S, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.END_OF_RULE).next()
        val expected: Set<RulePosition> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Item_start() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val rp = RulePosition(S, RuntimeRuleItem.SLIST__ITEM, RulePosition.START_OF_RULE)
        val actual = rp.next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                RulePosition(S, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Item_sep() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val rp = RulePosition(S, RuntimeRuleItem.SLIST__ITEM, RulePosition.POSITION_SLIST_SEPARATOR)
        assertFailsWith(IllegalStateException::class) {
            val actual = rp.next()
        }

        //assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Sep_sep() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val rp = RulePosition(S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_SEPARATOR)
        val actual = rp.next()
        val expected: Set<RulePosition> = setOf(
                RulePosition(S, RuntimeRuleItem.SLIST__ITEM, RulePosition.POSITION_SLIST_ITEM)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Sep_Item() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S)
        val G = SM.startState

        val rp = RulePosition(S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.POSITION_SLIST_ITEM)
        assertFailsWith(IllegalStateException::class) {
            val actual = rp.next()
        }
    }
}