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

import net.akehurst.language.agl.runtime.structure.RuleOptionPosition
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
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD

        const val EOR = RuleOptionPosition.END_OF_RULE
        const val SOR = RuleOptionPosition.START_OF_RULE

        /**
         * Option for SimpleList Item
         */
        const val OMI = RuleOptionPosition.OPTION_MULTI_ITEM

        /**
         * Option for SimpleList Empty
         */
        const val OME = RuleOptionPosition.OPTION_MULTI_EMPTY

        /**
         * Option for SeparatedList Empty
         */
        const val OLE = RuleOptionPosition.OPTION_SLIST_EMPTY

        /**
         * Option for SeparatedList Item
         */
        const val OLI = RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Option for SeparatedList Separator
         */
        const val OLS = RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        /**
         * Position of SimpleList Item
         */
        const val PMI = RuleOptionPosition.POSITION_MULIT_ITEM

        /**
         * Position of SeparatedList Item
         */
        const val PLI = RuleOptionPosition.POSITION_SLIST_ITEM

        /**
         * Position of SeparatedList Separator
         */
        const val PLS = RuleOptionPosition.POSITION_SLIST_SEPARATOR

        val WIDTH = Transition.ParseAction.WIDTH
        val HEIGHT = Transition.ParseAction.HEIGHT
        val GRAFT = Transition.ParseAction.GRAFT
        val GOAL = Transition.ParseAction.GOAL

        fun RP(rr: RuntimeRule): RuleOptionPosition = RP(rr, o0, EOR)
        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RuleOptionPosition = RuleOptionPosition(rr, opt, pos)
        fun LHS(content: Set<RuntimeRule>) = LookaheadSetPart(content.contains(RT), content.contains(EOT), false, content.minus(RT).minus(EOT))
        fun LHS(vararg rrs: RuntimeRule) = LHS(rrs.toSet())
    }

}
