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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.scanner.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_ParseGraph {

    @Test
    fun construct() {
        val goalRule = RuntimeRule(0, 0, "a", false, false)
        val text = ""
        val scanner = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val sut = ParseGraph(SentenceDefault(text), scanner, 0)

        assertNotNull(sut)
    }

    @Test
    fun canGrow_empty() {
        val goalRule = RuntimeRule(0, 0, "a", false, false)
        val text = ""
        val scanner = ScannerOnDemand(RegexEnginePlatform, emptyList())
        val sut = ParseGraph(SentenceDefault(text), scanner, 0)

        val actual = sut.canGrow

        assertEquals(false, actual)
    }

    @Test
    fun start() {
        val userGoalRule = RuntimeRule(0, 0, "a", false, false)
        val text = "a"
        val scanner = ScannerOnDemand(RegexEnginePlatform, emptyList())
        val sut = ParseGraph(SentenceDefault(text), scanner, 0)

        //    val gr = rrs.goalRuleFor[rrs.findRuntimeRule("a")]
        //    val startState = rrs.fetchStateSetFor(userGoalRule, AutomatonKind.LOOKAHEAD_1).startState
        //    sut.start(startState, 0, setOf(startState.stateSet.createLookaheadSet(false,true,false, emptySet())), null)

        val actual = sut.canGrow

        assertEquals(true, actual)
    }

}