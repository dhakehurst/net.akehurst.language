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

import net.akehurst.language.agl.parser.InputFromCharSequence
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.collections.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_ParseGraph {

    @Test
    fun construct() {
        val rrs = RuntimeRuleSet()
        val goalRule = RuntimeRule(rrs, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = ""
        val input = InputFromCharSequence(text)

        val sut = ParseGraph(goalRule, input, 10,10)

        assertNotNull(sut)
    }

    @Test
    fun canGrow_empty() {
        val rrs = RuntimeRuleSet()
        val goalRule = RuntimeRule(rrs, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = ""
        val input = InputFromCharSequence(text)
        val sut = ParseGraph(goalRule, input, 10,10)

        val actual = sut.canGrow

        assertEquals(false, actual)
    }

    @Test
    fun start() {
        val rrs = RuntimeRuleSet()

        val userGoalRule = RuntimeRule(rrs, 0, "a", "", RuntimeRuleKind.TERMINAL, false, false)
        val text = "a"
        val input = InputFromCharSequence(text)
        val sut = ParseGraph(userGoalRule, input, 10,10)

        val gr = RuntimeRuleSet.createGoalRule(userGoalRule)
        val startState = rrs.startingState(userGoalRule, emptySet())
        val startLocation = InputLocation(0, 0, 1, 0)
        sut.start(startState,startLocation)

        val actual = sut.canGrow

        assertEquals(true, actual)
    }

}