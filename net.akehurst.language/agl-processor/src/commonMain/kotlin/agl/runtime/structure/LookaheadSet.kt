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

import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserStateSet

internal class LookaheadSet(
    val number: Int,
    val includesUP: Boolean,
    val includesEOT: Boolean,
    val matchANY: Boolean,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSet(-1, false, false, false, emptySet())
        val ANY = LookaheadSet(-2, false, false, true, emptySet())
        val EOT = LookaheadSet(-3, false, true, false, emptySet())
        val UP = LookaheadSet(-4, true, false, false, emptySet())
        val UNCACHED_NUMBER = -5
    }

    val regex by lazy {
        val str = this.content.joinToString(prefix = "(", separator = ")|(", postfix = ")") {
            if (it.isPattern) it.value else "\\Q${it.value}\\E"
        }
        Regex(str)
    }

    val part get() = LookaheadSetPart(this.includesUP, this.includesEOT, this.matchANY, this.content)

    /**
     * runtimeLookahead must not include UP
     * replace UP in this with runtimeLookahead
     */
    fun resolveUP(runtimeLookahead: LookaheadSet): LookaheadSetPart {
        return when {
            runtimeLookahead.includesUP -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            UP == this -> runtimeLookahead.part
            else -> {
                val content = if (this.includesUP) this.content.union(runtimeLookahead.content) else this.content
                val eol = this.includesEOT || (this.includesUP && runtimeLookahead.includesEOT)
                val ma = this.matchANY || (this.includesUP && runtimeLookahead.matchANY)
                LookaheadSetPart(false, eol, ma, content)
            }
        }
    }

    fun union(automaton: ParserStateSet, lookahead: LookaheadSet): LookaheadSet {
        val up = this.includesUP || lookahead.includesUP
        val eol = this.includesEOT || lookahead.includesEOT
        val ma = this.matchANY || lookahead.matchANY
        return automaton.createLookaheadSet(up, eol, ma, this.content.union(lookahead.content))
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is LookaheadSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = when {
        this == ANY -> "LookaheadSet{$number,[ANY]}"
        this == UP -> "LookaheadSet{$number,[UP]}"
        this == EOT -> "LookaheadSet{$number,[EOT]}"
        else -> {
            val cont = mutableSetOf<RuntimeRule>()
            if (this.includesUP) cont.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
            if (this.includesEOT) cont.add(RuntimeRuleSet.END_OF_TEXT)
            if (this.matchANY) cont.add(RuntimeRuleSet.ANY_LOOKAHEAD)
            cont.addAll(this.content)
            "LookaheadSet{$number,${cont.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }}}"
        }
    }

}