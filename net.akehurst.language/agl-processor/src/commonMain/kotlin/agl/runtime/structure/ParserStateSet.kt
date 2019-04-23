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

class ParserStateSet(

) {

    /*
     * A RulePosition with Lookahead identifies a set of Parser states.
     * we index the map with RulePositionWithLookahead because that is what is used to create a new state,
     * and thus it should give fast lookup
     */
    private val states = lazyMapNonNull<RulePositionWithLookahead, MutableSet<ParserState>> {
        mutableSetOf()
    }

    internal fun fetchOrCreateParseState(rulePosition: RulePositionWithLookahead, parent: ParserState?): ParserState {
        val possible = this.states[rulePosition]
        val parentAncestors = if (null == parent) emptyList() else parent.ancestors + parent
        val existing = possible.find { ps -> ps.ancestors == parentAncestors }
        return if (null == existing) {
            val v = ParserState(parent, rulePosition, this)
            this.states[rulePosition].add(v)
            v
        } else {
            existing
        }
    }

    internal fun fetch(rulePosition: RulePositionWithLookahead) :Set<ParserState> {
        return this.states[rulePosition]
    }

    internal fun fetch(rulePosition: RulePositionWithLookahead, ancestors: List<ParserState>) :ParserState {
        return this.fetch(rulePosition).first {
            it.ancestors==ancestors
        }
    }

    internal  fun fetchAll(rulePosition: RulePosition) :Set<ParserState> {
        //TODO: this is not very efficient, skip stuff needs a rework
        return this.states.values.flatMap {
            it.filter {
                ps->ps.rulePosition.rulePosition==rulePosition
            }
        }.toSet()
    }

}