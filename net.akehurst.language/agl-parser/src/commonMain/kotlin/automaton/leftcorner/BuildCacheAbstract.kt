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

package net.akehurst.language.automaton.leftcorner

internal data class FirstOfResult(val needsFirstOfParentNext: Boolean, val result: LookaheadSetPart) {
    fun union(other: FirstOfResult) = FirstOfResult(this.needsFirstOfParentNext || other.needsFirstOfParentNext, this.result.union(other.result))
    fun endResult(firstOfParentNext: LookaheadSetPart) = when {
        needsFirstOfParentNext -> result.union(firstOfParentNext)
        else -> result
    }
}

internal abstract class BuildCacheAbstract(
    val stateSet: ParserStateSet
) : BuildCache {

    protected var _cacheOff = true

    protected val firstFollowCache = FirstFollowCache3()

    //TODO: use smaller array for done, but would to map rule number!
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })

    override fun switchCacheOn() {
        _cacheOff = false
    }

    /*
     * return the LookaheadSet for the given RulePosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the given RulePosition.
     *
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     * next() needs to be called to skip over empty rules (empty or empty lists)

    override fun expectedAt(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart {
        return when {
            rulePosition.isAtEnd -> ifReachedEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), BooleanArray(this.stateSet.runtimeRuleSet.runtimeRules.size))
                res.endResult(ifReachedEnd)
            }
        }
    }

    private fun firstOfRpNotEmpty(rulePosition: RulePosition, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            var result = LookaheadSetPart.EMPTY
            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) { // loop here to handle empties
                val nrps = mutableSetOf<RulePosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    val item = rp.item
                    when {
                        //item is null only when rp.isAtEnd
                        null == item /*rp.isAtEnd*/ -> needsNext = true
                        item.isEmptyRule -> nrps.addAll(rp.next())
                        else -> when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                            RuntimeRuleKind.EMBEDDED -> {
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                                val f =
                                    (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(
                                        item.embeddedStartRule,
                                        doneRp,
                                        BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size)
                                    )
                                result = result.union(f.result)
                                if (f.needsFirstOfParentNext) {
                                    needsNext = true
                                }
                            }
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val f = firstOfNotEmpty(item, doneRp, done)
                                result = result.union(f.result)
                                if (f.needsFirstOfParentNext) nrps.addAll(rp.next())
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

    private fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        return when {
            0 > rule.ruleNumber -> when { // handle special kinds of RuntimeRule
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.ruleNumber -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.RUNTIME_LOOKAHEAD_RULE_NUMBER == rule.ruleNumber -> TODO()
                else -> error("unsupported rule number $rule")
            }
            done[rule.ruleNumber] -> _firstOfNotEmpty[rule.ruleNumber] ?: FirstOfResult(false, LookaheadSetPart.EMPTY)
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

    private fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val pos = rule.rulePositionsAt[0]
        pos?.let {
            val item = pos.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                        val f =
                            (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result = result.union(f.result)
                        if (f.needsFirstOfParentNext) {
                            needsNext = true
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(pos, doneRp, done)
                        result = result.union(f.result)
                        needsNext = needsNext || f.needsFirstOfParentNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }
    */
}