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

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RulePositionWithLookahead
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.InputFromString
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ParseGraph_abc {

    /*
     * S = A B C ;
     * A = 'a' ;
     * B = 'b' ;
     * C = 'c' ;
     */

    @Test
    fun start() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }

        val text = "a"
        val input = InputFromString(rrs.terminalRules.size, text)
        val sut = ParseGraph(input, 0)

        val gr = rrs.goalRuleFor[rrs.findRuntimeRule("S")]
        val r_S = rrs.findRuntimeRule("S")
        val startState = rrs.fetchStateSetFor(r_S, AutomatonKind.LOOKAHEAD_1).startState
        sut.start(startState, 0, setOf(LookaheadSet.EMPTY), null)

        assertTrue(gr.isGoal)
        assertEquals(true, sut.canGrow)
        assertEquals(1, sut.numberOfHeads)

        //val head = sut.growingHead.values.first()
        ///assertEquals(gr, head.previous.values.first().node.runtimeRule)
        //assertEquals(0, head.previous.values.first().node.currentState.position)
    }

    @Test
    fun s1() {
        val rrs = runtimeRuleSet {
            literal("a", "a")
        }
        val text = "a"
        val input = InputFromString(rrs.terminalRules.size, text)
        val sut = ParseGraph(input, 0)

        val gr = rrs.goalRuleFor[rrs.findRuntimeRule("a")]
        val startState = RulePositionWithLookahead(RulePosition(gr, 0, 0), emptySet())
        //sut.start(startState, rrs)
        TODO()
        val actual = sut.canGrow

        assertEquals(true, actual)
    }
}