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
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.RulePosition

//FIXME: REPEAT - because no MPP test-fixtures
abstract class test_AutomatonUtilsAbstract {
    companion object {
        val o0 = OptionNum(0)
        val o1 = OptionNum(1)
        val o2 = OptionNum(2)
        val o3 = OptionNum(3)
        val p1 = 1
        val p2 = 2
        val p3 = 3
        val p4 = 4
        val p5 = 5
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val EMPTY = RuntimeRuleSet.EMPTY
        val EMPTY_LIST = RuntimeRuleSet.EMPTY_LIST
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD
        val UNDEFINED = RuntimeRuleSet.UNDEFINED_RULE

        const val ER = RulePosition.END_OF_RULE
        const val EOR = RulePosition.END_OF_RULE
        const val SR = RulePosition.START_OF_RULE
        const val SOR = RulePosition.START_OF_RULE

        /**
         * Option value for when there is no option
         */
        val oN = RulePosition.OPTION_NONE

        val oOE = RulePosition.OPTION_OPTIONAL_EMPTY
        val oOI = RulePosition.OPTION_OPTIONAL_ITEM

        /**
         * Option for SimpleList Item
         */
        val oLI = RulePosition.OPTION_MULTI_ITEM

        /**
         * Option for SimpleList Empty
         */
        val oLE = RulePosition.OPTION_MULTI_EMPTY

        /**
         * Option for SeparatedList Empty
         */
        val oSE = RulePosition.OPTION_SLIST_EMPTY

        /**
         * Option for SeparatedList Item
         */
        val oSI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Option for SeparatedList Separator
         */
        val oSS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Position of SimpleList Item
         */
        const val PMI = RulePosition.POSITION_MULIT_ITEM

        /**
         * Position of SeparatedList Item
         */
        const val PLI = RulePosition.POSITION_SLIST_ITEM

        /**
         * Position of SeparatedList Separator
         */
        const val PLS = RulePosition.POSITION_SLIST_SEPARATOR

        val WIDTH = ParseAction.WIDTH
        val HEIGHT = ParseAction.HEIGHT
        val GRAFT = ParseAction.GRAFT
        val GOAL = ParseAction.GOAL

        fun RP(rr: RuntimeRule): RulePositionRuntime = RP(rr, oN, EOR)
        fun RP(rr: RuntimeRule, opt: OptionNum, pos: Int): RulePositionRuntime = RulePositionRuntime(rr, opt, pos)
        fun LHS(content: Set<RuntimeRule>) = LookaheadSetPart(content.contains(RT), content.contains(EOT), false, content.minus(RT).minus(EOT))
        fun LHS(vararg rrs: RuntimeRule) = LHS(rrs.toSet())
    }

}
