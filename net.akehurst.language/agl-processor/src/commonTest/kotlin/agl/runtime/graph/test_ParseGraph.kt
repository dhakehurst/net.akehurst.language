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

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_ParseGraph {

    @Test
    fun construct() {
        val rrs = RuntimeRuleSet()
        val goalRule = RuntimeRule(rrs.number, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = ""
        val input = InputFromString(rrs.terminalRules.size,text)

        val sut = ParseGraph(goalRule, input, 10,10)

        assertNotNull(sut)
    }

    @Test
    fun canGrow_empty() {
        val rrs = RuntimeRuleSet()
        val goalRule = RuntimeRule(rrs.number, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = ""
        val input = InputFromString(rrs.terminalRules.size,text)
        val sut = ParseGraph(goalRule, input, 10,10)

        val actual = sut.canGrow

        assertEquals(false, actual)
    }

    @Test
    fun start() {
        val rrs = RuntimeRuleSet()

        val userGoalRule = RuntimeRule(rrs.number, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = "a"
        val input = InputFromString(rrs.terminalRules.size,text)
        val sut = ParseGraph(userGoalRule, input, 10,10)

        val gr = RuntimeRuleSet.createGoalRule(userGoalRule)
        val startState = rrs.fetchStateSetFor(userGoalRule, AutomatonKind.LOOKAHEAD_1).startState
        sut.start(startState,0, startState.stateSet.createLookaheadSet(setOf(RuntimeRuleSet.END_OF_TEXT)))

        val actual = sut.canGrow

        assertEquals(true, actual)
    }

}