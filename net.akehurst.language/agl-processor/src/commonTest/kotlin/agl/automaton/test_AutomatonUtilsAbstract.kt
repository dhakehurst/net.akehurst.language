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

import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

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
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD
        val UNDEFINED = RuntimeRuleSet.UNDEFINED_RULE

        const val ER = RulePosition.END_OF_RULE
        const val EOR = RulePosition.END_OF_RULE
        const val SR = RulePosition.START_OF_RULE
        const val SOR = RulePosition.START_OF_RULE

        /**
         * Option for SimpleList Item
         */
        const val OMI = RulePosition.OPTION_MULTI_ITEM

        /**
         * Option for SimpleList Empty
         */
        const val OME = RulePosition.OPTION_MULTI_EMPTY

        /**
         * Option for SeparatedList Empty
         */
        const val OLE = RulePosition.OPTION_SLIST_EMPTY

        /**
         * Option for SeparatedList Item
         */
        const val OLI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Option for SeparatedList Separator
         */
        const val OLS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

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

        fun RP(rr: RuntimeRule): RulePosition = RP(rr, o0, EOR)
        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePosition = RulePosition(rr, opt, pos)
        fun LHS(content: Set<RuntimeRule>) = LookaheadSetPart(content.contains(RT), content.contains(EOT), false, content.minus(RT).minus(EOT))
        fun LHS(vararg rrs: RuntimeRule) = LHS(rrs.toSet())
    }

}
