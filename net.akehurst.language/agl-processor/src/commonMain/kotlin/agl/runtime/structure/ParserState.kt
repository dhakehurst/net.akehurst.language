/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.collections.transitiveClosure

data class ParentRelation(
        val rulePosition: RulePosition,
        val lookahead: Set<RuntimeRule>
)

class ParserState(
        val number: StateNumber,
        val rulePosition: RulePosition,
        val stateSet: ParserStateSet
) {

    internal var transitions_cache: MutableMap<ParserState?,Set<Transition>?> = mutableMapOf()

    val parentRelations: Set<ParentRelation> by lazy {
        if (rulePosition.runtimeRule.kind==RuntimeRuleKind.GOAL) {
            emptySet()
        } else {
            this.stateSet.parentRelation(this.rulePosition.runtimeRule)
        }
    }

    val items: Set<RuntimeRule>
        inline get() {
            return this.rulePosition.items
        }
    val runtimeRule: RuntimeRule
        inline get() {
            return this.rulePosition.runtimeRule
        }
    val choice: Int
        inline get() {
            return this.rulePosition.choice
        }
    val position: Int
        inline get() {
            return this.rulePosition.position
        }

    val isAtEnd: Boolean
        inline get() {
            return this.rulePosition.isAtEnd
        }

    fun createClosure(parentLookahead: Set<RuntimeRule>): RulePositionClosure {
        // create path from root down, it may include items from root up
        val stateMap = this.stateSet
        val firstParentLh = parentLookahead//parentRelation?.lookahead ?: emptySet()
        val rootWlh = RulePositionWithLookahead(this.rulePosition, firstParentLh) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitiveClosure { parent ->
            val parentRP = parent.rulePosition
            val parentLh = parent.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    val lh = this.stateSet.runtimeRuleSet.calcLookahead(parent, childRP, parentLh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        return RulePositionClosure(ClosureNumber(-1), this.rulePosition, closureSet)
    }


    fun transitions(runtimeRuleSet: RuntimeRuleSet, previous:ParserState?): Set<Transition> {
        //val filteredRelations = this.parentRelations.filter { pr -> runtimeRuleSet.canGrowInto(pr, previous) }
        val cache = this.transitions_cache[previous]
        return if (null==cache) {
            //TODO: remove dependency on previous when calculating transitions! ?
            val transitions = runtimeRuleSet.calcTransitions(this, previous)
            this.transitions_cache[previous] = transitions
            transitions
        } else {
            cache
        }
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.number.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.number.value == other.number.value
        } else {
            false
        }
    }

    override fun toString(): String {
        return "State(${this.number.value}-${rulePosition})"
    }

}
