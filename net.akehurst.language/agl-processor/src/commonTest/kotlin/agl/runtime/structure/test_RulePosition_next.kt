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

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_RulePosition_next {

    private companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val EOR = RulePosition.END_OF_RULE
        val SOR = RulePosition.START_OF_RULE

        val OMI = RulePosition.OPTION_MULTI_ITEM
        val OME = RulePosition.OPTION_MULTI_EMPTY
        val OLE = RulePosition.OPTION_SLIST_EMPTY
        val OLI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR
        val OLS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        val PMI = RulePosition.POSITION_MULIT_ITEM
        val PLI = RulePosition.POSITION_SLIST_ITEM
        val PLS = RulePosition.POSITION_SLIST_SEPARATOR

        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePosition = RulePosition(rr, opt, pos)
    }

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_S = rb.rule("S").empty()
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RP(r_S, 0, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, 0, EOR))

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

        val actual: Set<RulePosition> = RP(r_S, 0, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, 0, EOR))

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

        val actual: Set<RulePosition> = RP(r_S, 1, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, 1, EOR))

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

        val actual: Set<RulePosition> = RP(r_S, 2, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, 2, EOR))

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

        val actual: Set<RulePosition> = RP(r_S, OME, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, OME, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Empty_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RP(r_S, OME, EOR).next()
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

        val actual: Set<RulePosition> = RP(r_S, OMI, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, OMI, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_mid() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").multi(0, 1, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RP(r_S, OMI, PMI).next()
        val expected: Set<RulePosition> = setOf(
                RP(r_S, OMI, PMI),
                RP(r_S, OMI, EOR)
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

        val actual: Set<RulePosition> = RP(r_S, PMI, EOR).next()
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

        val actual: Set<RulePosition> = RP(r_S, OLE, SOR).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, OLE, EOR))

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

        val actual: Set<RulePosition> = RP(r_S, OLE, EOR).next()
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

        val actual: Set<RulePosition> = RP(r_S, OLI, SOR).next()
        val expected: Set<RulePosition> = setOf(
                RP(r_S, OLI, EOR),
                RP(r_S, OLS, PLS)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_sep() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RP(r_S, OLS, PLS).next()
        val expected: Set<RulePosition> = setOf(RP(r_S, OLI, PLI))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_end() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RulePosition> = RP(r_S, OLI, EOR).next()
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

        val actual: Set<RulePosition> = RP(r_S, OLS, EOR).next()
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
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val actual = RP(S, OLE, SOR).next()
        val expected: Set<RulePosition> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Empty_end() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val actual = RP(S, OLE, EOR).next()
        val expected: Set<RulePosition> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Item_start() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val rp = RP(S, OLI, SOR)
        val actual = rp.next()
        val expected: Set<RulePosition> = setOf(
                RP(S, OLS, PLS),
                RP(S, OLI, EOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Sep_sep() {
        val S = sList1n.findRuntimeRule("S")
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val rp = RP(S, OLS, PLS)
        val actual = rp.next()
        val expected: Set<RulePosition> = setOf(
                RP(S, OLI, PLI)
        )

        assertEquals(expected, actual)
    }

}