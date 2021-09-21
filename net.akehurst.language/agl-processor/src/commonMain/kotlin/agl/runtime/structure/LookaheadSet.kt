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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.automaton.ParserStateSet

internal class LookaheadSet(
    val number: Int,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSet(-1, emptySet())
        val ANY = LookaheadSet(-1, setOf(RuntimeRuleSet.ANY_LOOKAHEAD))
        val EOT = LookaheadSet(-2, setOf(RuntimeRuleSet.END_OF_TEXT))
        val UP = LookaheadSet(-3, setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
    }

    fun resolve(runtimeLookahead: LookaheadSet): Set<RuntimeRule> {
        return when {
            UP == runtimeLookahead -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            ANY == this -> this.content
            //null == runtimeLookahead -> this.content
            UP == this -> runtimeLookahead.content
            else -> {
                var result = mutableSetOf<RuntimeRule>()
                for (rr in this.content) {
                    if (RuntimeRuleSet.USE_PARENT_LOOKAHEAD == rr) {
                        result.addAll(runtimeLookahead.content)
                    } else {
                        result.add(rr)
                    }
                }
                result
            }
        }
    }

    fun union(automaton:ParserStateSet, lookahead: LookaheadSet): LookaheadSet {
        return automaton.createLookaheadSet(this.content.union(lookahead.content))
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is LookaheadSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = "LookaheadSet{$number,${content}}"


}