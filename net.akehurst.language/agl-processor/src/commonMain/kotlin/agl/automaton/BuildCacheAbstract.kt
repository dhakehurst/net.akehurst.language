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
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.collections.mutableStackOf

internal abstract class BuildCacheAbstract(
    val stateSet: ParserStateSet
) : BuildCache {

    private data class FirstOfResult(val needsFirstOfParentNext: Boolean, val result: LookaheadSetPart) {
        fun union(other: FirstOfResult) = FirstOfResult(this.needsFirstOfParentNext || other.needsFirstOfParentNext, this.result.union(other.result))
        fun endResult(firstOfParentNext: LookaheadSetPart) = when {
            needsFirstOfParentNext -> result.union(firstOfParentNext)
            else -> result
        }
    }

    protected var _cacheOff = true

    protected val firstFollowCache = FirstFollowCache()

    //TODO: use smaller array for done, but would to map rule number!
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })

    override fun switchCacheOn() {
        _cacheOff = false
    }

    /**
     * return list of first terminals expected at the given RulePosition
     * RulePosition should never be 'atEnd' and there should always be a
     * non-empty list of "real" terminals ('empty' terminals permitted)
     */
    fun firstTerminal(prev: ParserState, fromState: ParserState): List<RuntimeRule> {
        return prev.rulePositions.flatMap {  prevRp ->
            fromState.rulePositions.flatMap { fromRp ->
                this.firstTerminal(prevRp, fromRp)
            }
        }.toSet().toList()
    }

    fun firstOfInContext(prev: ParserState, fromState: ParserState) {

    }

    fun followInContext(prev: ParserState, fromState: ParserState) {

    }

    private fun firstTerminal(prev:RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return if (this.firstFollowCache.containsFirstTerminal(prev,rulePosition)) {
            this.firstFollowCache.firstTerminal(prev,rulePosition) ?: error("Internal error, no firstTerminal created for $rulePosition")
        } else {
            calcFirstAndFollowFor(prev, rulePosition)
            this.firstFollowCache.firstTerminal(prev,rulePosition) ?: error("Internal error, no firstTerminal created for $rulePosition")
        }
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
                        val f =
                            (embSS.buildCache as BuildCacheAbstract).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
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


    /**
     * traverse down the closure of rulePosition.
     * record the firstTerminal and the lookahead of each rulePosition in the closure
     *
     * only call this if certain need to calc FirstAndFollow.
     * can check if needed by testing _firstTerminal[rulePosition]==null
     */
    private fun calcFirstAndFollowFor(prev: RulePosition, rulePosition: RulePosition) {
        data class Task(
            val prev: RulePosition,
            val prevForChildren: RulePosition,
            val parent: RulePosition,
            val rulePosition: RulePosition,
            val calcFirstTerminal: Boolean,
            val calcLookahead: Boolean,
            val future:(futureTerminal:RuntimeRule)->Unit = {_-> }
        )

        when {
            this.firstFollowCache.containsFirstTerminal(prev, rulePosition) -> Unit //already calculated, don't repeat
            else -> {
                //handle special case for Goal
                when {
                    rulePosition.isGoal && rulePosition.isAtStart ->  {
                        val goalEndRp = this.stateSet.endState.rulePositions.first()
                        this.firstFollowCache.addFirstTerminalInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        this.firstFollowCache.addFirstOfInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        this.firstFollowCache.addFollowInContext(prev, rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    }
                    else -> Unit
                }

                val lookaheads = mutableListOf<RulePosition>()
                val todoList = mutableStackOf<Task>()
                // handle rulePosition first as it has no parent defined
                when {
                    rulePosition.isTerminal -> Unit // do nothing
                    rulePosition.item!!.isTerminal -> this.firstFollowCache.addFirstTerminalInContext(prev, rulePosition, rulePosition.item!!)
                    else -> {
                        val childPrev = prev
                        val childRulePositions = rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            when {
                                childRp.isTerminal -> this.firstFollowCache.addFirstTerminalInContext(prev, childRp, childRp.item!!)
                                else -> {
                                    val childPrevForChildren = when {
                                        childRp.isAtStart -> childPrev
                                        else -> childRp
                                    }
                                    todoList.push(Task(childPrev, childPrevForChildren, rulePosition, childRp, true, true){futureTerminal->
                                        this.firstFollowCache.addFirstTerminalInContext(prev, rulePosition, futureTerminal)
                                    })
                                }
                            }
                            /*
                            // if childNextRp is atEnd then firstOf is firstNonEmptyTerminal of parent.next - assume it is already calculated
                            // else lookahead is firstNonEmptyTerminal of childNextRp.next
                            when {
                                childRp.isAtEnd -> Unit
                                else -> {
                                    val next = childRp.next()
                                    for(childNextRp in next) {
                                        val childPrevForChildren = when {
                                            childNextRp.isAtStart -> prev
                                            else -> rulePosition
                                        }
                                        todoList.push(Task(childPrev, childPrevForChildren, rulePosition, childNextRp, true, false))
                                    }
                                }
                            }
                             */
                        }
                    }
                }

                // handle rest of Tasks
                val done = mutableSetOf<Pair<RulePosition, RulePosition>>() // Pair(prev,rulePos)
                done.add(Pair(prev, rulePosition))
                while (todoList.isNotEmpty) {
                    val td = todoList.pop()
                    when { //maybe check if already calc'd
                        td.rulePosition.isAtEnd -> when {
                            td.rulePosition.isTerminal -> {
                                this.firstFollowCache.addFirstTerminalInContext(td.prev, td.parent, td.rulePosition.runtimeRule)
                                //td.future.invoke(td.rulePosition.runtimeRule)
                            }
                            td.calcLookahead -> {
                                TODO()
                            }
                            else -> Unit
                        }
                        td.rulePosition.item!!.isTerminal -> {
                            this.firstFollowCache.addFirstTerminalInContext(td.prev, td.rulePosition, td.rulePosition.item!!)
                            td.future.invoke(td.rulePosition.item!!)
                        }
                        else -> {
                            val childPrev = td.prevForChildren
                            val childRulePositions = td.rulePosition.item!!.rulePositionsAt[0]
                            for (childRp in childRulePositions) {
                                val d = Pair(childPrev, childRp)
                                if (done.contains(d).not()) {
                                    done.add(d)
                                    val childPrevForChildren = when {
                                        childRp.isAtStart -> childPrev
                                        else -> childRp
                                    }
                                    todoList.push(Task(childPrev, childPrevForChildren, td.rulePosition, childRp, true, true){futureTerminal->
                                        this.firstFollowCache.addFirstTerminalInContext(td.prev, td.rulePosition, futureTerminal)
                                        td.future.invoke(futureTerminal)
                                    })
                                    /*
                                if (td.calcLookahead) { // only calc lookahead if needed
                                    // if childNextRp is atEnd then lookahead is firstNonEmptyTerminal of parent.next
                                    // else lookahead is firstNonEmptyTerminal of childNextRp.next
                                    when {
                                        childRp.isAtEnd -> {
                                            val next = td.rulePosition.next()
                                            next.forEach { todoList.push(Task(childPrev, childPrevForChildren, td.rulePosition, it, true, false)) }
                                        }
                                        else -> {
                                            val next = childRp.next()
                                            next.forEach { todoList.push(Task(childPrev, childPrevForChildren, td.rulePosition, it, true, false)) }
                                        }
                                    }
                                } else {
                                    //do nothing
                                }
                                 */
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}