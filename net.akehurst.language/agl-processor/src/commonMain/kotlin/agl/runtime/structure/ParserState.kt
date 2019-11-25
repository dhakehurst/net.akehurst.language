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

import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.parser.ParseException

data class ParentRelation(
        val rulePosition: RulePosition,
        val lookahead: Set<RuntimeRule>
)

class ParserState(
        val number: StateNumber,
        val rulePosition: RulePosition,
        val stateSet: ParserStateSet
) {

    private var nextStates_cache: Set<ParserState>? = null
    private var _parentRelations: MutableSet<ParentRelation> = mutableSetOf()
    private var transitions_cache: MutableMap<RulePosition?,Set<Transition>?> = mutableMapOf()

    val parentRelations: Set<ParentRelation>
        get() {
            return this._parentRelations
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

    fun transitions(runtimeRuleSet: RuntimeRuleSet, previous:RulePosition?): Set<Transition> {
        val cache = this.transitions_cache[previous]
        if (null==cache) {
            val transitions = runtimeRuleSet.calcTransitions(this, previous)
            this.transitions_cache[previous] = transitions
            return transitions
        } else {
            return cache
        }
    }

    fun addParentRelation(value:ParentRelation) {
        val modified = this._parentRelations.add(value)
        if (modified) this.transitions_cache.clear()
    }
/*
    fun next(runtimeRuleSet: RuntimeRuleSet): Set<ParserState> {
        if (null == nextStates_cache) {
            this.nextStates_cache = runtimeRuleSet.fetchNextStates(this)
        }
        return this.nextStates_cache ?: throw ParseException("shouild never be null")
    }
*/
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
