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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

internal abstract class test_AutomatonUtilsAbstract {
    companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD

        val EOR = RulePosition.END_OF_RULE
        val SOR = RulePosition.START_OF_RULE

        val OMI = RulePosition.OPTION_MULTI_ITEM
        val OME = RulePosition.OPTION_MULTI_EMPTY
        val OLE = RulePosition.OPTION_SLIST_EMPTY
        val OLI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR
        val OLS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        val PMI = RulePosition.POSITION_MULIT_ITEM
        val PLI = RulePosition.POSITION_SLIST_ITEM
        val PLS = RulePosition.POSITION_SLIST_SEPARATOR

        val WIDTH = Transition.ParseAction.WIDTH
        val HEIGHT = Transition.ParseAction.HEIGHT
        val GRAFT = Transition.ParseAction.GRAFT
        val GOAL = Transition.ParseAction.GOAL
      //  val GRAFT_OR_HEIGHT = Transition.ParseAction.GRAFT_OR_HEIGHT

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT

        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePosition = RulePosition(rr, opt, pos)
    }

}

internal abstract class test_AutomatonAbstract : test_AutomatonUtilsAbstract() {

    fun <T1,T2,T3> List<Triple<T1,T2,T3>>.testAll(f:(arg1:T1,arg2:T2,arg3:T3)->Unit) {
        for(data in this) {
            f.invoke(data.first,data.second,data.third)
        }
    }

    /**
    listOf(
        /* G = S . */ Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)),
        /* G = . S */ Triple(RulePosition(G, 0, 0), lhs_U, setOf(UP))
    ).testAll { rp, lhs, expected ->
        val actual = SM.buildCache.firstOf(rp, lhs)
        assertEquals(expected, actual, "failed $rp")
    }
    */
    @Test
    abstract fun firstOf()

    /**
    val s0 = SM.startState
    val actual = s0.widthInto(null).toList()

    val expected = s0_widthInto_expected
    assertEquals(expected.size, actual.size)
    for (i in 0 until actual.size) {
    assertEquals(expected[i], actual[i])
    }
     */
    @Test
    abstract fun s0_widthInto()


}