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

import net.akehurst.language.api.parser.ParserException

class ParserStateSet(

) {

    private var nextState = 0

    /*
     * A RulePosition with Lookahead identifies a set of Parser states.
     * we index the map with RulePositionWithLookahead because that is what is used to create a new state,
     * and thus it should give fast lookup
     */
    internal val states = mutableMapOf<RulePosition, ParserState>()

    internal fun fetchOrCreateParseState(rulePosition: RulePosition): ParserState {
        val existing = this.states[rulePosition]
        return if (null == existing) {
            val v = ParserState(StateNumber(this.nextState++), rulePosition, this)
            this.states[rulePosition] = v
            v
        } else {
            existing
        }
    }
/*
    internal fun fetchNextParseState(rulePosition: RulePositionWithLookahead, parent: ParserState?): ParserState {
        val possible = this.states[rulePosition]
        //val parentAncestors = if (null == parent) emptyList() else parent.ancestors + parent
        val existing = possible.find { ps -> ps.directParent?.rulePositionWlh == parent?.rulePositionWlh }
        return if (null == existing) {
            throw ParseException("next states should be already created")
        } else {
            existing
        }
    }

    internal fun fetchAll(rulePosition: RulePositionWithLookahead) :Set<ParserState> {
        return this.states[rulePosition]
    }

    internal fun fetch(rulePosition: RulePositionWithLookahead, directParent: ParserState?) :ParserState {
        return this.fetchAll(rulePosition).first {
            it.directParent==directParent
        }
    }
*/
    internal  fun fetch(rulePosition: RulePosition) :ParserState {
        return this.states[rulePosition] ?: throw ParserException("should never be null")
    }

}