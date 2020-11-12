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

class test_sList_compositeMulti {

    // S = [nl / ';']*
    // nl = N cnm
    // cnm = cn*
    // cn = ',' N
    // N = "[0-9]+"

    companion object {

        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "nl", "SMI")
            concatenation("nl") { ref("N"); ref("cnm") }
            multi("cnm", 0, -1, "cn")
            concatenation("cn") { ref("CMR"); ref("N"); }
            literal("CMR", ",")
            literal("SMI", ";")
            pattern("N", "[0-9]+")
        }

        val S = rrs.findRuntimeRule("S")
        val Se = S.rhs.items[RuntimeRuleItem.SLIST__EMPTY_RULE]
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRules.first()
        val nl = rrs.findRuntimeRule("nl")
        val cnm = rrs.findRuntimeRule("cnm")
        val cn = rrs.findRuntimeRule("cn")
        val c = rrs.findRuntimeRule("CMR")
        val i = rrs.findRuntimeRule("SMI")
        val n = rrs.findRuntimeRule("N")
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_n = SM.runtimeRuleSet.createLookaheadSet(setOf(n))
        val lhs_c = SM.runtimeRuleSet.createLookaheadSet(setOf(c))
        val lhs_i = SM.runtimeRuleSet.createLookaheadSet(setOf(i))
        val lhs_nU = SM.runtimeRuleSet.createLookaheadSet(setOf(n, UP))
        val lhs_ciU = SM.runtimeRuleSet.createLookaheadSet(setOf(c,i,UP))

        val s0 = rrs.startingState(S)
        val s1 = SM.states[listOf(RulePosition(n,0,RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(Se,0,RulePosition.END_OF_RULE))]
    }

    @Test
    fun firstOf() {

        val rulePositions = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(n, UP)), // G = . S
                Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_U, setOf(n)), // So0 = . nl / ';'
                Triple(RulePosition(S, 2, RulePosition.START_OF_RULE), lhs_U, setOf(UP)), // So2 = . E
                Triple(RulePosition(nl, 0, RulePosition.START_OF_RULE), lhs_U, setOf(n)), // nl = . N cmn

                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)),                 // G = S .
                Triple(RulePosition(S, RuntimeRuleItem.SLIST__SEPARATOR, RulePosition.SLIST_SEPARATOR_POSITION), lhs_U, setOf(UP)),    // So0 = nl . / ';'
                Triple(RulePosition(S, 2, RulePosition.END_OF_RULE), lhs_U, setOf(UP)),                 // So2 = E .
                Triple(RulePosition(nl, 0, 1), lhs_U, setOf(UP)),                               // nl = N . cnm
                Triple(RulePosition(cnm, 0, RulePosition.START_OF_RULE), lhs_U, setOf(UP)),             // cnm = . cn
                Triple(RulePosition(cnm, 1, RulePosition.START_OF_RULE), lhs_U, setOf(UP)), // cnm = . E
                Triple(RulePosition(cn, 0, RulePosition.START_OF_RULE), lhs_U, setOf(UP))  // cn = . ',' N
        )


        for (t in rulePositions) {
            val rp = t.first
            val lhs = t.second
            val expected = t.third

            val actual = SM.firstOf(rp, lhs.content)

            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    fun calcClosure_G_0_0() {
        val cl_G = ClosureItem(null, RulePosition(G, 0, 0), RulePosition(G, 0, 0), lhs_U)
        val cl_G_So0 = ClosureItem(cl_G, RulePosition(S, 0, 0), RulePosition(S, 1, RulePosition.SLIST_SEPARATOR_POSITION), lhs_i)
        val cl_G_So0_nl = ClosureItem(cl_G_So0, RulePosition(nl, 0, 0), RulePosition(nl, 0, 1), lhs_i)
        val cl_G_So1 = ClosureItem(cl_G, RulePosition(S, 1, 0), RulePosition(G, 0, 0), lhs_U)

        val actual = SM.calcClosure(ClosureItem(null, RulePosition(G, 0, 0), null, lhs_U))
        val expected = setOf(
                cl_G, cl_G_So0, cl_G_So1
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s0_widthInto() {

        val actual = s0.widthInto(null).toList()

        val expected = listOf(
                Pair(RulePosition(n, 0, RulePosition.END_OF_RULE), lhs_ciU),
                Pair(RulePosition(Se, 0, RulePosition.END_OF_RULE), lhs_U)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_ciU, LookaheadSet.EMPTY,null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_U, LookaheadSet.EMPTY,null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0.rulePositions).toList()

        val expected = listOf(
                HeightGraft(
                        null,
                        listOf(RulePosition(S, 0, 0)),
                                listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)),
                        lhs_U,
                        lhs_U
                )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_U, LookaheadSet.UP,null) { _, _ -> true }
//                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}