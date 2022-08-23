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
    val includesRT: Boolean,
    val includesEOT:Boolean,
    val matchANY:Boolean,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSetPart(false, false, false, emptySet())
        val RT = LookaheadSetPart(true, false, false, emptySet())
        val ANY = LookaheadSetPart(false, false, true, emptySet())
        val EOT = LookaheadSetPart(false, true, false,emptySet())

        fun createFromRuntimeRules(fullContent:Set<RuntimeRule>): LookaheadSetPart {
            val includeRT = fullContent.contains(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
            val includeEOT = fullContent.contains(RuntimeRuleSet.END_OF_TEXT)
            val matchAny = fullContent.contains(RuntimeRuleSet.ANY_LOOKAHEAD)
            val content = fullContent.minus(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD).minus(RuntimeRuleSet.END_OF_TEXT).minus(RuntimeRuleSet.ANY_LOOKAHEAD)
            return LookaheadSetPart(includeRT, includeEOT, matchAny, content)
        }
    }

    val regex by lazy {
        val str = this.content.joinToString(prefix = "(", separator = ")|(", postfix = ")") {
            if (it.isPattern) it.value else "\\Q${it.value}\\E"
        }
        Regex(str)
    }

    val fullContent:Set<RuntimeRule> get() {
        val cont = mutableSetOf<RuntimeRule>()
        if (this.includesRT) cont.add(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
        if (this.includesEOT) cont.add(RuntimeRuleSet.END_OF_TEXT)
        if (this.matchANY) cont.add(RuntimeRuleSet.ANY_LOOKAHEAD)
        cont.addAll(this.content)
        return cont
    }

    fun union(lhs: LookaheadSetPart) = when {
        this.matchANY -> ANY
        lhs.matchANY -> ANY
        else -> LookaheadSetPart(
            this.includesRT || lhs.includesRT,
            this.includesEOT || lhs.includesEOT,
            false,
            this.content.union(lhs.content)
        )
    }

    fun containsAll(other: LookaheadSetPart):Boolean = when {
        this.matchANY -> true
        this.includesEOT.not() && other.includesEOT -> false
        this.includesRT.not() && other.includesRT -> false
        else -> this.fullContent.containsAll(other.fullContent)
    }

    override fun toString(): String = "LHS(${this.fullContent.joinToString{it.tag}})"
}

internal data class LookaheadInfoPart(
    val guard:LookaheadSetPart,
    val up:LookaheadSetPart
) {
    companion object {
        val EMPTY = LookaheadInfoPart(LookaheadSetPart.EMPTY,LookaheadSetPart.EMPTY)
        fun merge(initial: Set<LookaheadInfoPart>): Set<LookaheadInfoPart> = merge2(initial)
        fun merge1(initial: Set<LookaheadInfoPart>): Set<LookaheadInfoPart> {
            return when(initial.size) {
                1 -> initial
                else -> {
                    val merged = initial
                        .groupBy { it.up }
                        .map { me2 ->
                            val up = me2.key
                            val guard = me2.value.map { it.guard }.reduce { acc, l -> acc.union(l) }
                            LookaheadInfoPart(guard, up)
                        }.toSet()
                        .groupBy { it.guard }
                        .map { me ->
                            val guard = me.key
                            val up = me.value.map { it.up }.reduce { acc, l -> acc.union(l) }
                            LookaheadInfoPart(guard, up)
                        }.toSet()
                    when(merged.size) {
                        1 -> merged
                        else -> {
                            val sortedMerged = merged.sortedByDescending { it.guard.fullContent.size }
                            val result = mutableSetOf<LookaheadInfoPart>()
                            val mergedIntoOther = mutableSetOf<LookaheadInfoPart>()
                            for (i in sortedMerged.indices) {
                                var lh1 = sortedMerged[i]
                                if (mergedIntoOther.contains(lh1)) {
                                    //do nothing
                                } else {
                                    for (j in i + 1 until sortedMerged.size) {
                                        val lh2 = sortedMerged[j]
                                        if (lh1.guard.containsAll(lh2.guard)) {
                                            lh1 = LookaheadInfoPart(lh1.guard, lh1.up.union(lh2.up))
                                            mergedIntoOther.add(lh2)
                                        } else {
                                            //result.add(lh2)
                                        }
                                    }
                                    result.add(lh1)
                                }
                            }
                            result
                        }
                    }
                }
            }
        }
        fun merge2(initial: Set<LookaheadInfoPart>): Set<LookaheadInfoPart> {
            var r = LookaheadInfoPart(LookaheadSetPart.EMPTY,LookaheadSetPart.EMPTY)
            initial.forEach { lh ->
                val g = r.guard.union(lh.guard)
                val u = r.up.union(lh.up)
                r = LookaheadInfoPart(g,u)
            }
            return setOf(r)
        }
    }

}

internal data class TransInfo(
    val prev:Set<Set<RulePosition>>,
    val parent:Set<RulePosition>,
    val action: Transition.ParseAction,
    val to:Set<RulePosition>,
    val lookahead: Set<LookaheadInfoPart>
)
internal data class StateInfo(
    val rulePositions: List<RulePosition>,
    val possibleTrans:Set<TransInfo>
) {
    val possiblePrev: Set<Set<RulePosition>> get() = possibleTrans.flatMap { it.prev }.toSet()
}

internal data class WidthInfo(
    val action: Transition.ParseAction,
    val to: RulePosition,
    val lookaheadSet: LookaheadSetPart
)

internal data class HeightGraftInfo(
    val action: Transition.ParseAction,
    val parent: List<RulePosition>, //needed for prevGuard on Transition
    val parentNext: List<RulePosition>, // to state
    val lhs:Set<LookaheadInfoPart>
) {
    override fun toString(): String {
        val lhsStr = lhs.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString {  it.tag }}](${it.up.fullContent.joinToString {  it.tag }})" }
        return "HeightGraftInfo(action=$action, parent=$parent, parentNext=$parentNext, lhs=$lhsStr)"
    }
}