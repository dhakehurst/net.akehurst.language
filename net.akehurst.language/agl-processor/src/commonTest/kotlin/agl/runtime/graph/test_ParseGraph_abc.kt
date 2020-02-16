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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.parser.scannerless.InputFromCharSequence
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ParseGraph_abc {

    /*
     * S = A B C ;
     * A = 'a' ;
     * B = 'b' ;
     * C = 'c' ;
     */

    @Test
    fun start() {
        val rrs = RuntimeRuleSet(listOf())
        val r_a = RuntimeRule(0, "a", "a", RuntimeRuleKind.TERMINAL, false, false)
        val r_b = RuntimeRule(1, "b","b", RuntimeRuleKind.TERMINAL, false, false)
        val r_c = RuntimeRule(2, "c", "c", RuntimeRuleKind.TERMINAL, false, false)
        val r_A = RuntimeRule(3,"A","A", RuntimeRuleKind.NON_TERMINAL, false, false)
        r_A.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE,-1, 0, arrayOf(r_a))
        val r_B = RuntimeRule(3,"B", "B", RuntimeRuleKind.NON_TERMINAL, false, false)
        r_B.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE,-1, 0, arrayOf(r_b))
        val r_C = RuntimeRule(3,"C", "C", RuntimeRuleKind.NON_TERMINAL, false, false)
        r_C.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE,-1, 0, arrayOf(r_c))
        val r_S = RuntimeRule(0,"S", "", RuntimeRuleKind.NON_TERMINAL, false, false)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE,-1, 0, arrayOf(r_A, r_B, r_C))

        val text = "a"
        val input = InputFromCharSequence(text)
        val sut = ParseGraph(r_S,input)

        val gr = RuntimeRuleSet.createGoalRule(r_S)
        val startState = rrs.startingState(r_S)
        sut.start(startState)

        assertEquals(RuntimeRuleKind.GOAL, gr.kind)
        assertEquals(true, sut.canGrow)
        assertEquals(1, sut.growingHead.values.size)

        val head = sut.growingHead.values.first()
        ///assertEquals(gr, head.previous.values.first().node.runtimeRule)
        //assertEquals(0, head.previous.values.first().node.currentState.position)
    }

    @Test
    fun s1() {
        val rrs = RuntimeRuleSet(listOf())
        val userGoalRule = RuntimeRule(0,"a", "a", RuntimeRuleKind.TERMINAL, false, false)
        val text = "a"
        val input = InputFromCharSequence(text)
        val sut = ParseGraph(userGoalRule,input)

        val gr = RuntimeRuleSet.createGoalRule(userGoalRule)
        val startState = RulePositionWithLookahead(RulePosition(gr,0,0), emptySet())
        //sut.start(startState, rrs)
TODO()
        val actual = sut.canGrow

        assertEquals(true, actual)
    }
}