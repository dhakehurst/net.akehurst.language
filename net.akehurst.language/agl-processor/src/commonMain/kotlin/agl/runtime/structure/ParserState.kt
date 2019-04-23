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

import net.akehurst.language.api.parser.ParseException

class ParserState(
        val number:StateNumber,
        val directParent: ParserState?,
        val rulePositionWlh: RulePositionWithLookahead,
        val stateMap: ParserStateSet
) {

    private var nextStates_cache: Set<ParserState>? = null
    private var ancestors_cache:List<ParserState>? = null

    val items: Set<RuntimeRule>
        get() {
            return this.rulePositionWlh.items
        }
    val runtimeRule: RuntimeRule
        get() {
            return this.rulePositionWlh.runtimeRule
        }
    val choice: Int
        get() {
            return this.rulePositionWlh.choice
        }
    val position: Int
        get() {
            return this.rulePositionWlh.position
        }

    val isAtEnd: Boolean
        get() {
            return this.rulePositionWlh.isAtEnd
        }

    val ancestors:List<ParserState> get() {
        if (null==ancestors_cache) {
            this.ancestors_cache = if (null==this.directParent) emptyList() else this.directParent.ancestors + this.directParent
        }
        return this.ancestors_cache ?: throw ParseException("should never be null")
    }

    fun next(runtimeRuleSet: RuntimeRuleSet) : Set<ParserState> {
        if (null==nextStates_cache) {
            this.nextStates_cache = runtimeRuleSet.fetchNextStates(this)
        }
        return this.nextStates_cache ?: throw ParseException("shouild never be null")
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.number.hashCode() //rulePositionWlh.hashCode() * 31 + (directParent.hashCode()))
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.number.value == other.number.value
            //this.rulePositionWlh == other.rulePositionWlh && this.directParent == other.directParent
        } else {
            false
        }
    }

    override fun toString(): String {
        return "State(${directParent?.rulePositionWlh}-${rulePositionWlh})"
    }

}
