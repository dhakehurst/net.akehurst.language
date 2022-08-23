/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf
import net.akehurst.language.collections.mutableStackOf

internal class FirstFollowCache4(
    val usedRules: Set<RuntimeRule>
    //probably needs Automaton Kind for Embedded rules
) {

    internal companion object {

        data class FirstTerminalInfo(
            val embeddedRule: RuntimeRule,
            val terminalRule: RuntimeRule,
//            val followNext: FollowDeferred
        ) {
//            override fun toString(): String = "${terminalRule.tag}[$followNext]"
        }

        data class ParentOfInContext(
//            val parentFollowAtEnd: FollowDeferred,
//            val parentNextInfo: Set<Pair<RulePosition, FollowDeferred>>,
            val parent: RulePosition
        )
    }

    fun clear() {

    }

    // entry point from calcWidth
    // target states for WIDTH transition, rulePosition should NOT be atEnd
 //   fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred): Set<FirstTerminalInfo> {
        // calculate firstTerm by closure down from rulePosition.
        // calculate follow of firstTerm by using the parentFollow of the terminal in the closure
//    }

    // target states for HEIGHT or GRAFT transition, rulePosition should be atEnd
    // entry point from calcHeightGraft
 //   fun parentInContext(contextContext: RulePosition, context: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        // each used rule from the RuleSet has a RulePosition in which it is the item (where it is used)
//    }


}