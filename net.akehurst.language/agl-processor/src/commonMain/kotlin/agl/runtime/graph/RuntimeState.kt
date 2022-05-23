/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.Transition

internal class RuntimeState(
    val state: ParserState,
    val runtimeLookaheadSet: Set<LookaheadSet>,
) {
    val isAtEnd: Boolean get() = state.isAtEnd
    val optionList: List<Int> get() = state.optionList

    fun transitions(previousState: RuntimeState): List<Transition> = this.state.transitions(previousState, this)

    private val hashCode_cache = arrayOf(state, runtimeLookaheadSet).contentHashCode()
    override fun hashCode(): Int = this.hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is RuntimeState -> false
        this.state != other.state -> false
        this.runtimeLookaheadSet != other.runtimeLookaheadSet -> false
        else -> true
    }

    override fun toString(): String = "RS{${state.rulePositions}[${runtimeLookaheadSet.joinToString(separator = "|") { it.fullContent.joinToString { it.tag } }}]}"
}