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

        /**
         * Position of SimpleList Item
         */
        val PMI = RulePosition.POSITION_MULIT_ITEM

        /**
         * Position of SeparatedList Item
         */
        val PLI = RulePosition.POSITION_SLIST_ITEM

        /**
         * Position of SeparatedList Separator
         */
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
        fun LHS(content: Set<RuntimeRule>) = LookaheadSetPart(content.contains(UP), content.contains(EOT), false, content.minus(UP))
        fun LHS(vararg rrs: RuntimeRule) = LHS(rrs.toSet())
    }

}
