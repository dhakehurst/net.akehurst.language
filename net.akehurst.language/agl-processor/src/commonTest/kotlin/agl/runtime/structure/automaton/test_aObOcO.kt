/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

class test_aObOcO {
    /*
        S = a? b? c?;
     */
    companion object {

        val rrs = runtimeRuleSet {
            concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
            multi("aOpt", 0, 1, "'a'")
            multi("bOpt", 0, 1, "'b'")
            multi("cOpt", 0, 1, "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val aOpt = rrs.findRuntimeRule("aOpt")
        val aOpt_E = aOpt.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val bOpt = rrs.findRuntimeRule("bOpt")
        val cOpt = rrs.findRuntimeRule("cOpt")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val G = SM.startState.runtimeRule

        val s0 = SM.startState
        val s1 = SM.states[RulePosition(a, 0, RulePosition.END_OF_RULE)]
        val s2 = SM.states[RulePosition(aOpt_E, 0, RulePosition.END_OF_RULE)]

        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_bcU = rrs.createLookaheadSet(setOf(b, c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
    }


    @Test
    fun firstOf() {

        var actual = s0.stateSet.firstOf(RulePosition(G, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        var expected = setOf(a, b, c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(G, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(S, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)) // S = . a? b? c?
        expected = setOf(a, b, c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(S, 0, 1), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? . b? c?
        expected = setOf(b, c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(S, 0, 2), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? b? . c?
        expected = setOf(c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(S, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? b? c? .
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(aOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(aOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(bOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(b)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(bOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(cOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(c)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(cOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)
    }

    @Test
    fun calcLookahead() {

        var actual = s0.stateSet.calcLookaheadDown(RulePosition(G, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        var expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(G, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)) // S = . a? b? c?
        expected = setOf(b, c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, 1), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? . b? c?
        expected = setOf(c, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, 2), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? b? . c?
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))  // S = a? b? c? .
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(aOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(aOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(bOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(bOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(cOpt, RuntimeRuleItem.MULTI__ITEM, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(cOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)
    }

    @Test
    fun createClosure() {
        val cl_G = ClosureItem(null, RulePosition(G, 0, 0), RulePosition(G, 0, 0),lhs_bcU)
        val cl_G_S = ClosureItem(cl_G, RulePosition(S, 0, 0),RulePosition(G, 0, 0), lhs_bcU)
        val cl_G_S_aOpt0 = ClosureItem(cl_G_S, RulePosition(aOpt, 0, 0), RulePosition(G, 0, 0),lhs_bcU)
        //val cl_G_S_aOpt0_a = ClosureItem(cl_G_S_aOpt0, RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_bcU)
        val cl_G_S_aOpt1 = ClosureItem(cl_G_S, RulePosition(aOpt, 1, 0), RulePosition(G, 0, 0),lhs_bcU)
        val cl_G_S_aOpt1_E = ClosureItem(cl_G_S_aOpt1, RulePosition(aOpt_E, 0, RulePosition.END_OF_RULE),RulePosition(G, 0, 0), lhs_bcU)

        var actual = SM.calcClosure(cl_G)
        var expected = setOf(
                cl_G, cl_G_S, cl_G_S_aOpt0, cl_G_S_aOpt1
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY,null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY,null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

        @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0.rulePosition).toList()

        val expected = listOf(
                HeightGraft(
                        RulePosition(test_leftRecursive.G, 0, 0),
                        RulePosition(aOpt, 0, 0),
                        RulePosition(aOpt, 0, RulePosition.END_OF_RULE),
                        lhs_bcU,
                        lhs_U
                )
        )
        assertEquals(expected, actual)

    }
}