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

class test_abc_OR_abd {

    // S =  ABC | ABD
    // ABC = a b c
    // ABD = a b d

    companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABC")
                ref("ABD")
            }
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }
            concatenation("ABD") { literal("a"); literal("b"); literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRules.first()
        val ABC = rrs.findRuntimeRule("ABC")
        val ABD = rrs.findRuntimeRule("ABD")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val s0 = SM.startState
        val s1 = SM.states[listOf(RulePosition(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION))]
        val s3 = SM.states[listOf(RulePosition(S, 0, RulePosition.END_OF_RULE))]

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_b = SM.runtimeRuleSet.createLookaheadSet(setOf(b))
        val lhs_aU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
    }

    @Test
    fun firstOf() {
        val rulePositions = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // G = . S
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // G = S .
                Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // S = . ABC
                Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // S = ABC .
                Triple(RulePosition(S, 1, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // S = . ABD
                Triple(RulePosition(S, 1, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // S = ABD .
                Triple(RulePosition(ABC, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // ABC = . a b c
                Triple(RulePosition(ABC, 0, 1), lhs_U, setOf(b)), // ABC = a . b c
                Triple(RulePosition(ABC, 0, 2), lhs_U, setOf(c)), // ABC = a b . c
                Triple(RulePosition(ABC, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // ABC = a b c .
                Triple(RulePosition(ABD, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // ABD = . a b d
                Triple(RulePosition(ABD, 0, 1), lhs_U, setOf(b)), // ABD = a . b d
                Triple(RulePosition(ABD, 0, 2), lhs_U, setOf(d)), // ABD = a b . d
                Triple(RulePosition(ABD, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)) // ABD = a b d .
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
    fun s0_widthInto() {
        val actual = s0.widthInto(null)
        val expected = setOf(
                Pair(RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_b)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0.rulePositions).toList()

        val expected = listOf(
                HeightGraft(
                        RulePosition(G, 0, 0),
                        setOf(RulePosition(S, 0, 0)),
                        listOf(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION)),
                        lhs_a, lhs_U),
                HeightGraft(
                        RulePosition(G, 0, 0),
                        setOf(RulePosition(S, 0, 0)),
                        listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)),
                        lhs_U, lhs_U)
        )
        assertEquals(expected, actual)

    }
}