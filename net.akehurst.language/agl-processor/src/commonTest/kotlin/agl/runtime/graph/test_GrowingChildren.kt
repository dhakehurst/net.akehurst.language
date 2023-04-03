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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuleOption
import net.akehurst.language.agl.runtime.structure.RulePosition

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_GrowingChildren {

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("CM", "/[*].*[*]/", true)
            pattern("WS", "\\+s", true)
            concatenation("S") { literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val WS = rrs.findRuntimeRule("WS")
        val CM = rrs.findRuntimeRule("CM")
        val a = rrs.findRuntimeRule("'a'")

        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val st_a = SM.createState(listOf(RulePosition(a, 0, RulePosition.END_OF_RULE)))
        val st_S_EOR = SM.createState(listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)))

        val ruleOptionList_G0 = setOf(RuleOption(G, 0))
        val ruleOptionList_S = setOf(RuleOption(S, 0))
    }

    @Test
    fun isEmpty_true() {
        val sut = GrowingChildren()
        assertEquals(true, sut.isEmpty)
        assertEquals("{}", sut.toString())
    }

    @Test
    fun isEmpty_skipAtStart_false() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws = SPPTLeafFromInput(input, WS, 0, 1, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws))

        assertEquals(false, sut.isEmpty)
        assertEquals("(0,1,null|null) -> WS", sut.toString())
    }

    @Test
    fun isEmpty_nonSkipAtStart_false() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, a, 0, 1, 0)

        val sut = GrowingChildren()
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        assertEquals(false, sut.isEmpty)
        assertEquals("(0,1,'a'|0) -> 'a'|0", sut.toString())
    }

    @Test
    fun hasSkipAtStart_false() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, a, 0, 1, 0)

        val sut = GrowingChildren()
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        assertEquals(false, sut.hasSkipAtStart)
        assertEquals("(0,1,'a'|0) -> 'a'|0", sut.toString())
    }

    @Test
    fun hasSkipAtStart_true() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, WS, 0, 1, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(leaf))

        assertEquals(true, sut.hasSkipAtStart)
        assertEquals("(0,1,null|null) -> WS", sut.toString())
    }

    @Test
    fun lastInitialSkipChild_isNullWhenEmpty() {
        val sut = GrowingChildren()

        val actual = sut.lastInitialSkipChild

        val expected: GrowingChildNode? = null
        assertEquals(expected, actual)
        assertEquals("{}", sut.toString())
    }

    @Test
    fun lastInitialSkipChild_when1InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val leaf = SPPTLeafFromInput(input, a, 1, 2, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.lastInitialSkipChild

        val expected = sut.firstChild(st_a.rulePositionIdentity)
        assertEquals(expected, actual)
        assertEquals("(0,2,'a'|0) -> WS, 'a'|0", sut.toString())
    }

    @Test
    fun lastInitialSkipChild_when2InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val cm = SPPTLeafFromInput(input, CM, 1, 5, 0)
        val leaf = SPPTLeafFromInput(input, a, 5, 6, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws))
            .appendSkipIfNotEmpty(emptySet(), listOf(cm))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.lastInitialSkipChild

        val expected = sut.firstChild(st_a.rulePositionIdentity)?.nextChild
        assertEquals(expected, actual)
        assertEquals("(0,6,'a'|0) -> WS, CM, 'a'|0", sut.toString())
    }

    @Test
    fun lastInitialSkipChild_when3InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val cm = SPPTLeafFromInput(input, CM, 1, 5, 0)
        val ws2 = SPPTLeafFromInput(input, WS, 5, 6, 0)
        val leaf = SPPTLeafFromInput(input, a, 6, 7, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws1))
            .appendSkipIfNotEmpty(emptySet(), listOf(cm))
            .appendSkipIfNotEmpty(emptySet(), listOf(ws2))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.lastInitialSkipChild

        val expected = sut.firstChild(st_a.rulePositionIdentity)?.nextChild!!.nextChild
        assertEquals(expected, actual)
        assertEquals("(0,7,'a'|0) -> WS, CM, WS, 'a'|0", sut.toString())
    }

    @Test
    fun firstNonSkipChild_isNullWhenEmpty() {
        val sut = GrowingChildren()

        val actual = sut.firstNonSkipChild(ruleOptionList_S)

        val expected: GrowingChildNode? = null
        assertEquals(expected, actual)
        assertEquals("{}", sut.toString())
    }

    @Test
    fun firstNonSkipChild_when1InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val leaf = SPPTLeafFromInput(input, a, 1, 2, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.firstNonSkipChild(ruleOptionList_S)
        val expected = sut.firstChild(st_a.rulePositionIdentity)?.nextChild
        assertEquals(expected, actual)
        assertEquals("(0,2,'a'|0) -> WS, 'a'|0", sut.toString())
    }

    @Test
    fun firstNonSkipChild_when2InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val cm = SPPTLeafFromInput(input, CM, 1, 5, 0)
        val leaf = SPPTLeafFromInput(input, a, 5, 6, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws))
            .appendSkipIfNotEmpty(emptySet(), listOf(cm))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.firstNonSkipChild(ruleOptionList_S)

        val expected = sut.firstChild(st_a.rulePositionIdentity)?.nextChild!!.nextChild
        assertEquals(expected, actual)
        assertEquals("(0,6,'a'|0) -> WS, CM, 'a'|0", sut.toString())
    }

    @Test
    fun firstNonSkipChild_when3InitialSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val cm = SPPTLeafFromInput(input, CM, 1, 5, 0)
        val ws2 = SPPTLeafFromInput(input, WS, 5, 6, 0)
        val leaf = SPPTLeafFromInput(input, a, 6, 7, 0)

        val sut = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws1))
            .appendSkipIfNotEmpty(emptySet(), listOf(cm))
            .appendSkipIfNotEmpty(emptySet(), listOf(ws2))
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))

        val actual = sut.firstNonSkipChild(ruleOptionList_S)

        val expected = sut.firstChild(st_a.rulePositionIdentity)?.nextChild!!.nextChild!!.nextChild
        assertEquals(expected, actual)
        assertEquals("(0,7,'a'|0) -> WS, CM, WS, 'a'|0", sut.toString())
    }

    @Test
    fun appendSkipIfNotEmpty_atStart() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val sut = GrowingChildren()

        val actual = sut.appendSkipIfNotEmpty(emptySet(), listOf(ws1))

        assertEquals(sut, actual)
        assertEquals(1, actual.length)
        assertEquals(0, actual.numberNonSkip)
        assertEquals(1, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(listOf(ws1), actual.firstChild(emptySet())?.children)
        assertEquals("(0,1,null|null) -> WS", actual.toString())
    }

    @Test
    fun appendSkipIfNotEmpty_atEnd() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, a, 0, 1, 0)
        val ws1 = SPPTLeafFromInput(input, WS, 1, 2, 0)

        val actual = GrowingChildren()
            .appendChild(st_a.rulePositionIdentity, listOf(leaf))
            .appendSkipIfNotEmpty(st_a.rulePositionIdentity, listOf(ws1))

        assertEquals(2, actual.length)
        assertEquals(1, actual.numberNonSkip)
        assertEquals(2, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(listOf(leaf), actual.firstChild(ruleOptionList_S)?.children)
        assertEquals(listOf(ws1), actual.firstChild(ruleOptionList_S)?.nextChild!!.children)
        assertEquals("(0,2,'a'|0) -> 'a'|0, WS", actual.toString())
    }

    @Test
    fun concatenate1() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val leaf = SPPTLeafFromInput(input, a, 1, 2, 0)
        val sut1 = GrowingChildren().appendSkipIfNotEmpty(st_a.rulePositionIdentity, listOf(ws1))
        val sut2 = GrowingChildren().appendChild(st_a.rulePositionIdentity, listOf(leaf))

        sut1.concatenate(sut2)

        assertEquals(2, sut1.length)
        assertEquals(1, sut1.numberNonSkip)
        assertEquals(2, sut1.nextInputPosition)
        assertEquals(0, sut1.startPosition)
        assertEquals(listOf(ws1), sut1.firstChild(st_a.rulePositionIdentity)?.children)
        assertEquals(listOf(leaf), sut1.firstChild(st_a.rulePositionIdentity)?.nextChild!!.children)
        assertEquals("(0,2,'a'|0) -> WS, 'a'|0", sut1.toString())
    }

    @Test
    fun concatenate2() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, a, 0, 1, 0)
        val ws1 = SPPTLeafFromInput(input, WS, 1, 2, 0)
        val sut1 = GrowingChildren().appendChild(st_a.rulePositionIdentity, listOf(leaf))
        val sut2 = GrowingChildren().appendSkipIfNotEmpty(st_a.rulePositionIdentity, listOf(ws1))

        sut1.concatenate(sut2)

        assertEquals(2, sut1.length)
        assertEquals(1, sut1.numberNonSkip)
        assertEquals(2, sut1.nextInputPosition)
        assertEquals(0, sut1.startPosition)
        assertEquals(listOf(leaf), sut1.firstChild(st_a.rulePositionIdentity)?.children)
        assertEquals(listOf(ws1), sut1.firstChild(st_a.rulePositionIdentity)?.nextChild!!.children)
        assertEquals("(0,2,'a'|0) -> 'a'|0, WS", sut1.toString())
    }

    @Test
    fun appendChild_atStart() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val leaf = SPPTLeafFromInput(input, a, 0, 1, 0)
        val sut = GrowingChildren()

        val actual = sut.appendChild(st_a.rulePositionIdentity, listOf(leaf))

        assertEquals(sut, actual)
        assertEquals(1, actual.length)
        assertEquals(1, actual.numberNonSkip)
        assertEquals(1, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(listOf(leaf), actual.firstChild(ruleOptionList_S)?.children)
        assertEquals("(0,1,'a'|0) -> 'a'|0", actual.toString())
    }

    @Test
    fun appendChild_goalAtStart() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val goal = SPPTBranchFromInputAndGrownChildren(input, G, 0, 0, 1, 0)
        val sut = GrowingChildren()

        val actual = sut.appendChild(s0.rulePositionIdentity, listOf(goal))

        assertEquals(sut, actual)
        assertEquals(1, actual?.length)
        assertEquals(1, actual?.numberNonSkip)
        assertEquals(1, actual?.nextInputPosition)
        assertEquals(0, actual?.startPosition)
        assertEquals(listOf(goal), actual?.firstChild(s0.rulePositionIdentity)?.children)
        assertEquals("(0,1,<GOAL>|0) -> <GOAL>|0", actual.toString())
    }

    @Test
    fun appendChild_goalAfterSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val goal = SPPTBranchFromInputAndGrownChildren(input, G, 0, 1, 2, 0)
        val sut = GrowingChildren()

        sut.appendSkipIfNotEmpty(emptySet(), listOf(ws1))
        val actual = sut.appendChild(s0.rulePositionIdentity, listOf(goal))!!

        assertEquals(sut, actual)
        assertEquals(2, actual.length)
        assertEquals(1, actual.numberNonSkip)
        assertEquals(2, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(listOf(ws1), actual.firstChild(s0.rulePositionIdentity)?.children)
        assertEquals(listOf(goal), actual.firstChild(s0.rulePositionIdentity)?.nextChild!!.children)
        assertEquals("(0,2,<GOAL>[0]) -> WS, <GOAL>", actual.toString())
    }

    @Test
    fun mergeOrDropWithPriority_secondGoalAtStart() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val goal1 = SPPTBranchFromInputAndGrownChildren(input, G, 0, 0, 2, 0)
        val goal2 = SPPTBranchFromInputAndGrownChildren(input, G, 0, 0, 3, 0)

        val sut1 = GrowingChildren()
            .appendChild(s0.rulePositionIdentity, listOf(goal1))

        val sut2 = GrowingChildren()
            .appendChild(s0.rulePositionIdentity, listOf(goal2))

        sut1.mergeOrDropWithPriority(sut2)
        val actual = sut1

        //assertNotEquals(sut1, actual)
        assertEquals(1, actual.length) //error!
        assertEquals(1, actual.numberNonSkip)
        assertEquals(3, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(null, actual.firstChild(ruleOptionList_G0)?.nextChild)
        assertNotNull(actual.firstChild(ruleOptionList_G0))
        assertEquals(listOf(goal1), actual.firstChild(ruleOptionList_G0)?.children)
        assertEquals(listOf(goal2), actual.firstChild(ruleOptionList_G0)?.children)
        assertEquals("(0,3,<GOAL>|0) -> <GOAL>|0", actual.toString())
    }

    @Test
    fun mergeOrDropWithPriority_secondGoalAfterSkip() {
        val input = InputFromString(rrs.terminalRules.size, "")
        val ws1 = SPPTLeafFromInput(input, WS, 0, 1, 0)
        val goal1 = SPPTBranchFromInputAndGrownChildren(input, G, 0, 1, 2, 0)
        val goal2 = SPPTBranchFromInputAndGrownChildren(input, G, 0, 1, 3, 0)
        var sut = GrowingChildren()

        val sut1 = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws1))
            .appendChild(s0.rulePositionIdentity, listOf(goal1))

        val sut2 = GrowingChildren()
            .appendSkipIfNotEmpty(emptySet(), listOf(ws1))
            .appendChild(s0.rulePositionIdentity, listOf(goal2))

        sut1.mergeOrDropWithPriority(sut2)
        val actual = sut1

        assertEquals(2, actual.length)
        assertEquals(1, actual.numberNonSkip)
        assertEquals(3, actual.nextInputPosition)
        assertEquals(0, actual.startPosition)
        assertEquals(listOf(ws1), actual.firstChild(s0.rulePositionIdentity)?.children)
        assertEquals(null, actual.firstChild(s0.rulePositionIdentity)?.nextChild)
        assertNotNull(actual.firstChild(s0.rulePositionIdentity)?.nextChildAlternatives)
        assertEquals(1, actual.firstChild(s0.rulePositionIdentity)?.nextChildAlternatives!!.size)
        assertEquals(2, actual.firstChild(s0.rulePositionIdentity)?.nextChildAlternatives!![ruleOptionList_G0]!!.size)
        assertEquals(listOf(goal1), actual.firstChild(s0.rulePositionIdentity)?.nextChildAlternatives!![ruleOptionList_G0]!![0].children)
        assertEquals(listOf(goal2), actual.firstChild(s0.rulePositionIdentity)?.nextChildAlternatives!![ruleOptionList_G0]!![1].children)
        // assertEquals(listOf(goal2), actual.lastChild?.children)
        assertEquals("(0,3,<GOAL>[0]) -> WS, <GOAL>", actual.toString())
    }

    @Test
    fun get_whenEmpty() {
    }

    @Test
    fun get_whenSkipAtStart() {
    }

    @Test
    fun get_when1NonSkipStart() {
    }

    @Test
    fun get_when3NonSkipStart() {
    }

    @Test
    fun get_when1NonSkipAfterSkip() {
    }

    @Test
    fun get_when3NonSkipAfterSkip() {
    }

    @Test
    fun get_when3AltNonSkipStart() {
    }

    @Test
    fun get_when3AltNonSkipAfterSkip() {
    }

    @Test
    fun get_when3NonSkipStartAnd2Has3Alts() {
    }

    @Test
    fun get_when3NonSkipStartAnd3Has3Alts() {
    }
}