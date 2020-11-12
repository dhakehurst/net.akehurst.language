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
        val G = rrs.startingState(S).runtimeRules.first()

        val B = rrsB.findRuntimeRule("B")
        val bT = rrsB.findRuntimeRule("'b'")
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD


        val s0 = rrs.startingState(S)
        val psm = s0.stateSet
        val psmB = rrsB.startingState(B).stateSet
    }

    @Test
    fun firstOf() {
//TODO
        var actual = psm.firstOf(RulePosition(G,0,0),setOf(UP))
        var expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(G,0,RulePosition.END_OF_RULE),setOf(UP))
        expected = setOf(UP)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,0),setOf(UP))
        expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,1),setOf(UP))
        expected = setOf(bT)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,2),setOf(UP))
        expected = setOf(a)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(S,0,RulePosition.END_OF_RULE),setOf(UP))
        expected = setOf(UP)
        assertEquals(expected,actual)

        actual = psm.firstOf(RulePosition(gB,0,RulePosition.END_OF_RULE),setOf(UP))
        expected = setOf(a)
        assertEquals(expected,actual)


        actual = psmB.firstOf(RulePosition(bT,0,RulePosition.END_OF_RULE),setOf(UP))
        expected = setOf(a)
        assertEquals(expected,actual)
    }

}