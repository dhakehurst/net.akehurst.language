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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

internal abstract class BuildCacheAbstract(
    val stateSet: ParserStateSet
) : BuildCache {

    private data class FirstOfResult(val needsFirstOfParentNext: Boolean, val result: LookaheadSetPart) {
        fun union(other:FirstOfResult) = FirstOfResult(this.needsFirstOfParentNext||other.needsFirstOfParentNext, this.result.union(other.result))
        fun endResult(firstOfParentNext:LookaheadSetPart) = when {
            needsFirstOfParentNext -> result.union(firstOfParentNext)
            else -> result
        }
    }
    private data class SecondOfResult(val needsFirstOfParentNext: Boolean, val needsSecondOfParent:Boolean, val result: LookaheadSetPart) {
        fun endResult(parentFirstOfNext: LookaheadSetPart, parentNextOf: LookaheadSetPart): LookaheadSetPart {
            var res = result
            if (needsFirstOfParentNext) res = res.union(parentFirstOfNext)
            if(needsSecondOfParent) res = res.union(parentNextOf)
            return res
        }
    }

    protected var _cacheOff = true

    //TODO: use smaller array for done, but would to map rule number!
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })

    override fun on() {
        _cacheOff = false
    }

    /*
     * return the LookaheadSet for the given RulePosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the given RulePosition.
     *
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     * next() needs to be called to skip over empty rules (empty or empty lists)
    */
    override fun firstOf(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart {
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
                                    (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
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
            0 > rule.number -> when { // handle special kinds of RuntimeRule
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.number -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.USE_PARENT_LOOKAHEAD_RULE_NUMBER == rule.number -> TODO()
                else -> error("unsupported rule number $rule")
            }
            done[rule.number] -> _firstOfNotEmpty[rule.number] ?: FirstOfResult(false, LookaheadSetPart.EMPTY)
            else -> {
                var result: FirstOfResult? = null//_firstOfNotEmpty[rule.number]
                if (null == result) {
                    done[rule.number] = true
                    result = firstOfNotEmptySafe(rule, doneRp, done)
                    _firstOfNotEmpty[rule.number] = result
                }
                result
            }
        }
    }

    private fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val pos = rule.rulePositionsAt[0]
        for (rp in pos) {
            val item = rp.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                        val f = (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result = result.union(f.result)
                        if (f.needsFirstOfParentNext) {
                            needsNext = true
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(rp, doneRp, done)
                        result = result.union(f.result)
                        needsNext = needsNext || f.needsFirstOfParentNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

    /*
     * return the 'next' LookaheadSet for the given RulePosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the next Terminal of the given RulePosition.
     * i.e. the second LookaheadSet
     * Don't like that this is needed, but it is necessary to provide the LookaheadSet for a WIDTH transition
     * which, in turn, needs the 'next' LookaheadSet to pass as argument to the SkipRule Parser so it knows when to terminate.
     *
     * a b c d e
     *    ^ current position in sentence
     *     ^ 'c' is the target (to) of the WIDTH transition - which is also firstOf(RulePosition)
     *       ^ 'd' needs to be the Lookahead which is passed to the skipParser
    */
    fun secondOf(rulePosition: RulePosition, parentFirstOfNext: LookaheadSetPart, parentNextOf:LookaheadSetPart): LookaheadSetPart {
        TODO("handle recursion")
        return when {
            rulePosition.isAtEnd -> parentNextOf
            null!=rulePosition.item && rulePosition.item!!.isTerminalOrEmbedded -> {
                val next = rulePosition.next()
                next.map {firstOf(it,parentFirstOfNext)}.reduce { acc, it -> acc.union(it) }
            }
            else -> {
                val next = rulePosition.next()
                val firstOfNext = next.map {firstOf(it,parentFirstOfNext)}.reduce { acc, it -> acc.union(it) }
                val nextOf = LookaheadSetPart.EMPTY // should never be needed by recursive call below
                val rpsOfItem = rulePosition.item?.rulePositionsAt?.get(0) ?: emptySet()
                rpsOfItem.map { secondOf(it, firstOfNext,nextOf) }.reduce { acc, it -> acc.union(it) } //FIXME
            }
        }
    }

     fun secondOf2(rulePosition: RulePosition, parentFirstOfNext: LookaheadSetPart, parentSecondOf:LookaheadSetPart): LookaheadSetPart {
        return when {
            rulePosition.isAtEnd -> parentSecondOf
            //null!=rulePosition.item && rulePosition.item!!.isTerminalOrEmbedded -> {
            //    //TODO check if this is done in the call to nextOfRpNotEmpty(...)
            //    val next = rulePosition.next()
            //    next.map {firstOf(it,parentFirstOfNext)}.reduce { acc, it -> acc.union(it) }
            //}
            else -> {
                //use hashMap as we don't need ordering of LinkedHashMap
                val res = secondOfRpNotEmpty(rulePosition, hashMapOf(), HashMap(this.stateSet.runtimeRuleSet.runtimeRules.size))
                val result = res.endResult(parentFirstOfNext, parentSecondOf)
                result
            }
        }
    }

    private fun secondOfRpNotEmpty(rulePosition: RulePosition, doneRp: MutableMap<RulePosition, SecondOfResult>, doneByRule: MutableMap<RuntimeRule, SecondOfResult>): SecondOfResult {
        var result = doneRp[rulePosition]
        if (null == result) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsFirstOfParentNext=false
            var needsSecondOfParent=false
            var nextOfResult = LookaheadSetPart.EMPTY

            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) { // loop here to handle empties
                val nrps = mutableSetOf<RulePosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    val item = rp.item
                    when {
                        //item is null only when rp.isAtEnd
                        null == item /*rp.isAtEnd*/ -> needsSecondOfParent = true
                        item.isEmptyRule -> nrps.addAll(rp.next())
                        else -> when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> needsFirstOfParentNext = true
                            RuntimeRuleKind.EMBEDDED -> {
                                //FIXME
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                                val f =
                                    (embSS.buildCache as BuildCacheAbstract).secondOfNotEmpty(item.embeddedStartRule, doneRp, HashMap(item.embeddedRuntimeRuleSet.runtimeRules.size))
                                nextOfResult = nextOfResult.union(f.result)
                                if (f.needsFirstOfParentNext) {
                                    needsFirstOfParentNext = true
                                }
                            }
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val f = secondOfNotEmpty(item, doneRp, doneByRule)
                                nextOfResult = nextOfResult.union(f.result)
                                if (f.needsFirstOfParentNext) {
                                    val next = rp.next().map { this.firstOf(it,) }
                                    val f = this.firstOf()
                                }
                                if (f.needsSecondOfParent) nrps.addAll(rp.next())
                            }
                        }
                    }
                }
                rps = nrps
            }
            result = SecondOfResult(needsFirstOfParentNext,needsSecondOfParent, nextOfResult)
            doneRp[rulePosition] = result
        }
        return result
    }

    private fun secondOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePosition, SecondOfResult>, doneByRule: MutableMap<RuntimeRule, SecondOfResult>): SecondOfResult {
        return when {
            0 > rule.number -> when {
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.number -> secondOfNotEmptySafe(rule, doneRp, doneByRule)
                RuntimeRuleSet.USE_PARENT_LOOKAHEAD_RULE_NUMBER == rule.number -> TODO()
                else -> error("unsupported rule number $rule")
            }
            null!=doneByRule[rule] -> doneByRule[rule]!!
            else -> {
                doneByRule[rule] = SecondOfResult(false,false, LookaheadSetPart.EMPTY) // place holder to mark 'doing it', stop recursion
                val result = secondOfNotEmptySafe(rule, doneRp, doneByRule)
                doneByRule[rule] = result
                result
            }
        }
    }

    private fun secondOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, SecondOfResult>, doneByRule: MutableMap<RuntimeRule, SecondOfResult>): SecondOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val pos = rule.rulePositionsAt[0]
        for (rp in pos) {
            val item = rp.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                        val f = (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result = result.union(f.result)
                        if (f.needsFirstOfParentNext) {
                            needsNext = true
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(rp, doneRp, done)
                        result = result.union(f.result)
                        needsNext = needsNext || f.needsFirstOfParentNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

}