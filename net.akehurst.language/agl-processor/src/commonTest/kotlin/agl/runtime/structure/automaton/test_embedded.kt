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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class test_embedded{

    companion object {
        /*
            B = b ;

            S = a gB a ;
            gB = grammar B ;
         */
        val rrsB = runtimeRuleSet {
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("gB"); literal("a"); }
            embedded("gB", rrsB, rrsB.findRuntimeRule("B"))
        }

        val S = rrs.findRuntimeRule("S")
        val gB = rrs.findRuntimeRule("gB")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule

        val B = rrsB.findRuntimeRule("B")
        val bT = rrsB.findRuntimeRule("'b'")

        val s0 = rrs.startingState(S)
        val psm = s0.stateSet
        val psmB = rrsB.startingState(B).stateSet
    }

    @Test
    fun parentPosition() {
        var actual = psm.parentPosition[G]
        var expected = emptySet<RulePosition>()
        assertEquals(expected, actual)

        actual = psm.parentPosition[S]
        expected = setOf(
                RulePosition(G, 0, 0)
        )
        assertEquals(expected, actual)

        actual = psm.parentPosition[a]
        expected = setOf(
                RulePosition(S, 0, 0),
                RulePosition(S, 0, 2)
        )
        assertEquals(expected, actual)

        actual = psm.parentPosition[gB]
        expected = setOf(
                RulePosition(S, 0, 1)
        )
        assertEquals(expected, actual)


        actual = psmB.parentPosition[bT]
        expected = setOf(
                RulePosition(B, 0, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun firstOf() {

        var actual = psm.firstOf(RulePosition(G,0,0),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        var expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(G,0,1),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.END_OF_TEXT)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(G,0,RulePosition.END_OF_RULE),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf()
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,0),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,1),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(bT)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,2),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,RulePosition.END_OF_RULE),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.END_OF_TEXT)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(gB,0,RulePosition.END_OF_RULE),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected,actual)


        actual = psmB.firstOf(RulePosition(bT,0,RulePosition.END_OF_RULE),setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected,actual)
    }

}