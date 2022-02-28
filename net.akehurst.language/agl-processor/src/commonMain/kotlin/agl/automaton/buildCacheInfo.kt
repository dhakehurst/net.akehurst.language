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

internal data class LookaheadSetPart(
    val includesUP: Boolean,
    val includesEOT:Boolean,
    val matchANY:Boolean,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSetPart(false, false, false, emptySet())
        val UP = LookaheadSetPart(true, false, false, emptySet())
        val ANY = LookaheadSetPart(false, false, true, emptySet())
        val EOT = LookaheadSetPart(false, true, false,emptySet())
    }

    val regex by lazy {
        val str = this.content.joinToString(prefix = "(", separator = ")|(", postfix = ")") {
            if (it.isPattern) it.value else "\\Q${it.value}\\E"
        }
        Regex(str)
    }

    val fullContent:Set<RuntimeRule> get() {
        val cont = mutableSetOf<RuntimeRule>()
        if (this.includesUP) cont.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        if (this.includesEOT) cont.add(RuntimeRuleSet.END_OF_TEXT)
        if (this.matchANY) cont.add(RuntimeRuleSet.ANY_LOOKAHEAD)
        cont.addAll(this.content)
        return cont
    }

    fun union(lhs: LookaheadSetPart) = when {
        this.matchANY -> ANY
        lhs.matchANY -> ANY
        else -> LookaheadSetPart(
            this.includesUP || lhs.includesUP,
            this.includesEOT || lhs.includesEOT,
            false,
            this.content.union(lhs.content)
        )
    }

    fun containsAll(other: LookaheadSetPart):Boolean = when {
        this.matchANY -> true
        this.includesEOT.not() && other.includesEOT -> false
        this.includesUP.not() && other.includesUP -> false
        else -> this.fullContent.containsAll(other.fullContent)
    }

}

internal data class FirstOfResult(
    val needsNext: Boolean,
    val result: LookaheadSetPart
)

internal data class StateInfo(
    val rulePositions: List<RulePosition>,
    val possiblePrev: List<List<RulePosition>>
)

internal data class WidthInfo(
    val to: RulePosition,
    val lookaheadSet: LookaheadSetPart
)

internal data class HeightGraftInfo(
    val ancestors: List<RuntimeRule>,
    val parent: List<RulePosition>,
    val parentNext: List<RulePosition>, // to state
    val lhs: LookaheadSetPart,
    val upLhs: Set<LookaheadSetPart>
) {
    override fun toString(): String {
        val ancestorsStr = ancestors.joinToString(prefix = "[", postfix = "]", separator = "-") { it.tag }
        val cont1 = mutableSetOf<RuntimeRule>()
        if (lhs.includesUP) cont1.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        if (lhs.includesEOT) cont1.add(RuntimeRuleSet.END_OF_TEXT)
        if (lhs.matchANY) cont1.add(RuntimeRuleSet.ANY_LOOKAHEAD)
        cont1.addAll(lhs.content)
        val ul = upLhs.map {
            val cont2 = mutableSetOf<RuntimeRule>()
            if (it.includesUP) cont2.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
            if (it.includesEOT) cont2.add(RuntimeRuleSet.END_OF_TEXT)
            if (it.matchANY) cont2.add(RuntimeRuleSet.ANY_LOOKAHEAD)
            cont2.addAll(it.content)
            cont2
        }
        val lhsStr = cont1.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }
        val upLhsStr = ul.joinToString(prefix = "[", postfix = "]", separator = "|") { it.joinToString(separator = ",") {  it.tag }}
        return "HeightGraftInfo(ancestors=$ancestorsStr, parent=$parent, parentNext=$parentNext, lhs=$lhsStr, upLhs=$upLhsStr)"
    }
}