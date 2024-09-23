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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.automaton.api.ParseAction

//FIXME: REPEAT - because no MPP test-fixtures
internal abstract class test_AutomatonUtilsAbstract {
    companion object {
        const val o0 = 0
        const val o1 = 1
        const val o2 = 2
        const val p1 = 1
        const val p2 = 2
        const val p3 = 3
        const val p4 = 4
        const val p5 = 5
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val EMPTY = RuntimeRuleSet.EMPTY
        val EMPTY_LIST = RuntimeRuleSet.EMPTY_LIST
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD
        val UNDEFINED = RuntimeRuleSet.UNDEFINED_RULE

        const val ER = RulePositionRuntime.END_OF_RULE
        const val EOR = RulePositionRuntime.END_OF_RULE
        const val SR = RulePositionRuntime.START_OF_RULE
        const val SOR = RulePositionRuntime.START_OF_RULE

        /**
         * Option for SimpleList Item
         */
        const val OMI = RulePositionRuntime.OPTION_MULTI_ITEM

        /**
         * Option for SimpleList Empty
         */
        const val OME = RulePositionRuntime.OPTION_MULTI_EMPTY

        /**
         * Option for SeparatedList Empty
         */
        const val OLE = RulePositionRuntime.OPTION_SLIST_EMPTY

        /**
         * Option for SeparatedList Item
         */
        const val OLI = RulePositionRuntime.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Option for SeparatedList Separator
         */
        const val OLS = RulePositionRuntime.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Position of SimpleList Item
         */
        const val PMI = RulePositionRuntime.POSITION_MULIT_ITEM

        /**
         * Position of SeparatedList Item
         */
        const val PLI = RulePositionRuntime.POSITION_SLIST_ITEM

        /**
         * Position of SeparatedList Separator
         */
        const val PLS = RulePositionRuntime.POSITION_SLIST_SEPARATOR

        val WIDTH = ParseAction.WIDTH
        val HEIGHT = ParseAction.HEIGHT
        val GRAFT = ParseAction.GRAFT
        val GOAL = ParseAction.GOAL

        fun RP(rr: RuntimeRule): RulePositionRuntime = RP(rr, o0, EOR)
        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePositionRuntime = RulePositionRuntime(rr, opt, pos)
        fun LHS(content: Set<RuntimeRule>) = LookaheadSetPart(content.contains(RT), content.contains(EOT), false, content.minus(RT).minus(EOT))
        fun LHS(vararg rrs: RuntimeRule) = LHS(rrs.toSet())
    }

}
