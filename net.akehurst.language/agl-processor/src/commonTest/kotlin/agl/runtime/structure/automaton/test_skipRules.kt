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

class test_skipRules{

    companion object {
        // skip WS = "\s+" ;
        // skip COMMENT = "//[^\n]*$"
        // S = 'a' ;
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "//[^\\n]*$", true)
            concatenation("S") { literal("a") }
        }


        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule

        val s0 = rrs.startingState(S)

        val skipSS = rrs.skipParserStateSet
        val sk0 = skipSS.startState
        val skG = sk0.runtimeRule
        val skS = skG.rhs.items[0]
        val skC = skS.rhs.items[RuntimeRuleItem.MULTI__ITEM]
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")
    }

    @Test
    fun parentPosition() {
        var actual = skipSS.parentPosition[skG]
        var expected = emptySet<RulePosition>()
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skS]
        expected = setOf(
                RulePosition(G, 0, 0)
        )
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skC]
        expected = setOf(
                RulePosition(skS, 0, 0)
        )
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skWS]
        expected = setOf(
                RulePosition(skC, 0, 0)
        )
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skCM]
        expected = setOf(
                RulePosition(skC, 1, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun firstTerminals() {
        var actual = skipSS.firstTerminals[RulePosition(skG,0,0)]
        var expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skS, 0, 0)]
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC,0,0)]
        expected = setOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC,1,0)]
        expected = setOf(skCM)
        assertEquals(expected, actual)

    }

    @Test
    fun calcFirstAt() {

        var actual = s0.stateSet.calcFirstAt(RulePosition(G,0,0),BooleanArray(rrs.runtimeRules.size))
        var expected = setOf(a)
        assertEquals(expected,actual)

        actual = s0.stateSet.calcFirstAt(RulePosition(G,0,1),BooleanArray(rrs.runtimeRules.size))
        expected = setOf(rrs.END_OF_TEXT)
        assertEquals(expected,actual)

        actual = s0.stateSet.calcFirstAt(RulePosition(G,0,RulePosition.END_OF_RULE),BooleanArray(rrs.runtimeRules.size))
        expected = setOf()
        assertEquals(expected,actual)


        actual = s0.stateSet.calcFirstAt(RulePosition(S,1,0),BooleanArray(rrs.runtimeRules.size))
        expected = setOf(a)
        assertEquals(expected,actual)

        actual = s0.stateSet.calcFirstAt(RulePosition(S,1,0),BooleanArray(rrs.runtimeRules.size))
        expected = setOf(a)
        assertEquals(expected,actual)




    }

}