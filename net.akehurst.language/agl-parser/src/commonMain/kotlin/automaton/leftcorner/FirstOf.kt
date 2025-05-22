/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.automaton.api.AutomatonKind

//internal data class FirstOfResult(val needsFirstOfParentNext: Boolean, val result: LookaheadSetPart) {
//    fun union(other: FirstOfResult) = FirstOfResult(this.needsFirstOfParentNext || other.needsFirstOfParentNext, this.result.union(other.result))
//    fun endResult(firstOfParentNext: LookaheadSetPart) = when {
//        needsFirstOfParentNext -> result.union(firstOfParentNext)
//        else -> result
//    }
//}

internal class FirstOf(
) {

    // index by RuntimeRule.number
    private val _firstOfNotEmpty = hashMapOf<Int, FirstOfResult>()

    /*
     * return the LookaheadSet for the given RulePosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the given RulePosition.
     *
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     * next() needs to be called to skip over empty rules (empty or empty lists)
    */
    fun expectedAt(rulePosition: RulePositionRuntime, ifReachedEnd: LookaheadSetPart): LookaheadSetPart {
        this._firstOfNotEmpty.clear()
        return when {
            rulePosition.isAtEnd -> ifReachedEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), hashMapOf<Int, Boolean>())
                when {
                    res.needsFirstOfParentNext -> when {
                        rulePosition.isAtEnd -> res.result.union(ifReachedEnd)
                        else -> {
                            val next = rulePosition.next()
                            val notSelf = next - rulePosition
                            val ne = notSelf.map { nrp -> this.expectedAt(nrp, ifReachedEnd) }
                                .fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                            res.result.union(ne)
                        }
                    }

                    else -> res.result
                }
                //res.endResult(ifReachedEnd)
            }
        }
    }

    private fun firstOfRpNotEmpty(rulePosition: RulePositionRuntime, doneRp: MutableMap<RulePositionRuntime, FirstOfResult>, done: MutableMap<Int, Boolean>): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            var result = LookaheadSetPart.EMPTY
            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) { // loop here to handle empties
                val nrps = mutableSetOf<RulePositionRuntime>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    if (rp.isAtEnd) {
                        needsNext = true
                    } else {
                        for (item in rp.items) {
                            val rhs = item.rhs
                            when {
                                else -> when (rhs) {
                                    is RuntimeRuleRhsEmpty -> nrps.addAll(rp.next())
                                    is RuntimeRuleRhsGoal -> TODO()
                                    is RuntimeRuleRhsEmbedded -> {
                                        val embSS = rhs.embeddedRuntimeRuleSet.fetchStateSetFor(rhs.embeddedStartRule.tag, AutomatonKind.LOOKAHEAD_1)
                                        val f = embSS.firstOf.firstOfNotEmpty(
                                            rhs.embeddedStartRule,
                                            doneRp,
                                            hashMapOf()
                                        )
                                        val embSkipTerms = rhs.embeddedRuntimeRuleSet.skipTerminals
                                        result = result.union(f.result).union(LookaheadSetPart.createFromRuntimeRules(embSkipTerms))
                                        if (f.needsFirstOfParentNext) {
                                            needsNext = true
                                        }
                                    }

                                    is RuntimeRuleRhsTerminal -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))

                                    is RuntimeRuleRhsNonTerminal -> {
                                        val f = firstOfNotEmpty(item, doneRp, done)
                                        result = result.union(f.result)
                                        if (f.needsFirstOfParentNext) {
                                            val rpnxt = rp.next()
                                            val notSelf = rpnxt - rp
                                            nrps.addAll(notSelf)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
                rps = nrps
            }
            existing = FirstOfResult(needsNext, result)
            doneRp[rulePosition] = existing
        }
        return existing
    }

    private fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePositionRuntime, FirstOfResult>, done: MutableMap<Int, Boolean>): FirstOfResult {
        return when {
            0 > rule.ruleNumber -> when (rule.ruleNumber) {
                // handle special kinds of RuntimeRule
                RuntimeRuleSet.GOAL_RULE_NUMBER -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER -> firstOfNotEmptySafe(rule, doneRp, done)//TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.RUNTIME_LOOKAHEAD_RULE_NUMBER -> TODO()
                else -> error("unsupported rule number $rule")
            }

            done.containsKey(rule.ruleNumber) -> _firstOfNotEmpty[rule.ruleNumber] ?: FirstOfResult(false, LookaheadSetPart.EMPTY)
            else -> {
                var result: FirstOfResult? = null//_firstOfNotEmpty[rule.number]
                if (null == result) {
                    done[rule.ruleNumber] = true
                    result = firstOfNotEmptySafe(rule, doneRp, done)
                    _firstOfNotEmpty[rule.ruleNumber] = result
                }
                result
            }
        }
    }

    private fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePositionRuntime, FirstOfResult>, done: MutableMap<Int, Boolean>): FirstOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val rps = rule.rulePositionsAtStart
        for (rp in rps) {
            for (item in rp.items) {
                val rhs = item.rhs
                when {
                    item.isEmptyTerminal -> needsNext = true //should not happen
                    item.isEmptyListTerminal -> needsNext = true //should not happen
                    else -> when (rhs) {
                        is RuntimeRuleRhsGoal -> error("should never happen")
                        is RuntimeRuleRhsEmbedded -> {
                            val embSS = rhs.embeddedRuntimeRuleSet.fetchStateSetFor(rhs.embeddedStartRule.tag, AutomatonKind.LOOKAHEAD_1)
                            val f = embSS.firstOf.firstOfNotEmpty(rhs.embeddedStartRule, doneRp, hashMapOf())
                            val embSkipTerms = rhs.embeddedRuntimeRuleSet.skipTerminals
                            result = result.union(f.result).union(LookaheadSetPart.createFromRuntimeRules(embSkipTerms))
                            if (f.needsFirstOfParentNext) {
                                needsNext = true
                            }
                        }

                        is RuntimeRuleRhsTerminal -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                        is RuntimeRuleRhsNonTerminal -> {
                            val f = firstOfRpNotEmpty(rp, doneRp, done)
                            result = result.union(f.result)
                            needsNext = needsNext || f.needsFirstOfParentNext
                        }
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

}