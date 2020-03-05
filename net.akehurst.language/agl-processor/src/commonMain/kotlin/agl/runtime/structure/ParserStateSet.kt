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

import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.transitiveClosure

class ParserStateSet(
        val runtimeRuleSet: RuntimeRuleSet,
        val userGoalRule: RuntimeRule, //null if skip state set TODO: improve this!
        val possibleEndOfText: List<RuntimeRule>
) {

    private var nextState = 0

    /*
     * A RulePosition with Lookahead identifies a set of Parser states.
     * we index the map with RulePositionWithLookahead because that is what is used to create a new state,
     * and thus it should give fast lookup
     */
    internal val states = mutableMapOf<RulePosition, ParserState>()

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule, this.possibleEndOfText)
        val goalRP = RulePosition(goalRule, 0, 0)
        val startState = this.fetchOrCreateParseState(goalRP, this.possibleEndOfText.toSet())
        startState
    }

    // runtimeRule -> set of rulePositions where the rule is used
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        //TODO: possibly faster to pre cache this! and goal rules currently not included!
        if (childRR == RuntimeRuleSet.END_OF_TEXT) { //TODO: should this check for contains in possibleEndOfText, and what if something in endOfText is also valid mid text!
            setOf(RulePosition(this.startState.runtimeRule,0,1))
        } else {
            val s = this.runtimeRuleSet.runtimeRules.flatMap { rr ->
                val rps = rr.rulePositions
                val f = rps.filter { rp ->
                    rp.items.contains(childRR)
                }
                f
            }.toSet()
            if (childRR == this.userGoalRule) {
                s + this.startState.rulePosition
            } else {
                s
            }
        }
    }
    internal val ancestorPosition = lazyMapNonNull<RuntimeRule, Set<List<RuntimeRule>>> { childRR ->
        if (possibleEndOfText.contains(childRR)) {
            setOf(listOf(this.startState.rulePosition.next().first().runtimeRule))
        } else {
            val init: Set<List<RuntimeRule>> = parentPosition[childRR].map { listOf(it.runtimeRule) }.toSet()
            val x: Set<List<RuntimeRule>> = init.transitiveClosure(false) { list ->
                val pp = parentPosition[list.last()]
                if (pp.isEmpty()) {
                    setOf(list)
                } else {
                    pp.mapNotNull { rp ->
                        val lastIndexOfRp = list.lastIndexOf(rp.runtimeRule)
                        when (lastIndexOfRp) {
                            -1 -> list + rp.runtimeRule
                            0 -> list + rp.runtimeRule
                            else -> {
                                val firstIndexOfRp = list.indexOf(rp.runtimeRule)
                                val front = list.subList(0, lastIndexOfRp)
                                val repetition = list.subList(lastIndexOfRp + 1, list.size)
                                when {
                                    front.containsSubList(repetition) -> null
                                    else -> list + rp.runtimeRule
                                }
                            }
                        }
                    }
                }.toSet()
            }.toSet()
            x
        }
    }

    internal fun <T> List<T>.containsSubList(subList: List<T>): Boolean {
        //TODO: speed up by finding indexes of first item in subList
        val indexs = this.mapIndexedNotNull { index, t ->
            if (t == subList[0]) index else null
        }
        indexs.forEach { i ->
            val e = i + subList.size
            if (e <= this.size) {
                val x = this.subList(i, i + subList.size)
                if (x == subList) return true
            } else {
                return false
            }
        }
        return false
    }

    internal val _parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
    internal fun parentRelation(runtimeRule: RuntimeRule): Set<ParentRelation> {
        var set = _parentRelations[runtimeRule]
        return if (null == set) {
            _parentRelations[runtimeRule] = emptySet() //add this to halt recursion
            set = this.calcParentRelation(runtimeRule)
            _parentRelations[runtimeRule] = set
            set
        } else {
            set
        }
    }

    //val parentRelations = this.calcParentRelations()
    /*
    internal val parentRelations = lazyMapNonNull<RuntimeRule, Set<ParentRelation>> { childRR ->
        if (possibleEndOfText.contains(childRR)) {
            setOf(ParentRelation(this.startState.rulePosition.next().first(), emptySet()))
        } else {
            val init = ancestorPaths[childRR]!!
            val x = init.flatMap { list ->
                val d = list.drop(1)//don't need the goal from end, this is used as the initAcc
                val spr = setOf(ParentRelation(this.startState.rulePosition, possibleEndOfText.toSet()))
                d.fold(spr) { acc, rr ->
                    val rps = this.parentPosition[rr]//.filter { it.runtimeRule== }
                    val nAcc = rps.flatMap { rp ->
                        acc.map { pr ->
                            val lh = this.calcLookahead(pr, rp)
                            ParentRelation(rp, lh)
                        }.toSet()
                    }.toSet()
                    nAcc
                }
            }.toSet()
            x
        }
    }
     */

    private fun calcParentRelation(childRR: RuntimeRule): Set<ParentRelation> {
        val x = this.parentPosition[childRR].map { rp ->
            val lh = this.calcLookahead(rp)
            ParentRelation(rp, lh)
        }.toSet()
        return x
    }

    private fun calcPaths(): Map<RuntimeRule, Set<List<RuntimeRule>>> {
        val init = setOf(listOf(this.startState.runtimeRule))
        val paths = init.transitiveClosure { list ->
            val rr = list.last()
            val rps = rr.rulePositions
            val nls = rps.flatMap { rp ->
                rp.items.mapNotNull { nr ->
                    val lastIndexOf = list.lastIndexOf(nr)
                    when (lastIndexOf) {
                        -1 -> list + nr
                        0 -> list + nr
                        else -> {
                            val firstIndexOfRp = list.indexOf(nr)
                            val front = list.subList(0, lastIndexOf)
                            val repetition = list.subList(lastIndexOf + 1, list.size)
                            when {
                                front.containsSubList(repetition) -> null
                                else -> list + nr
                            }
                        }
                    }
                }
            }.toSet()
            nls
        }
        val ancestorPaths = mutableMapOf<RuntimeRule, Set<List<RuntimeRule>>>()
        paths.forEach {
            val rr = it.last()
            var set = ancestorPaths[rr]
            if (null == set) {
                set = mutableSetOf()
                ancestorPaths[rr] = set
            }
            (set as MutableSet).add(it)
        }
        return ancestorPaths
    }

    private fun calcParentRelations(): Map<RuntimeRule, Set<ParentRelation>> {
        val ancestorPaths = this.calcPaths()
        val parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
        ancestorPaths.keys.forEach { childRR ->
            if (possibleEndOfText.contains(childRR)) {
                parentRelations[childRR] = setOf(ParentRelation(this.startState.rulePosition.next().first(), emptySet()))
            } else {
                val init = ancestorPaths[childRR]!!
                val x = init.flatMap { list ->
                    val d = list.drop(1)//don't need the goal from end, this is used as the initAcc
                    val spr = setOf(ParentRelation(this.startState.rulePosition, possibleEndOfText.toSet()))
                    d.fold(spr) { acc, rr ->
                        val rps = this.parentPosition[rr]//.filter { it.runtimeRule== }
                        val nAcc = rps.flatMap { rp ->
                            acc.map { pr ->
                                val lh = this.calcLookahead(pr, rp)
                                ParentRelation(rp, lh)
                            }.toSet()
                        }.toSet()
                        nAcc
                    }
                }.toSet()
                parentRelations[childRR] = x
            }
        }
        return parentRelations
    }

    private fun calcParentRelations2(): Map<RuntimeRule, Set<ParentRelation>> {
        val parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
        val init = setOf(listOf(this.startState.runtimeRule))

        return parentRelations
    }

    internal fun fetchOrCreateParseState(rulePosition: RulePosition, lookahead: Set<RuntimeRule>): ParserState { //
        val existing = this.states[rulePosition]
        return if (null == existing) {
            val v = ParserState(StateNumber(this.nextState++), rulePosition, lookahead, this)
            this.states[rulePosition] = v
            v
        } else {
            existing
        }
    }

    /*
        internal fun fetchNextParseState(rulePosition: RulePositionWithLookahead, parent: ParserState?): ParserState {
            val possible = this.states[rulePosition]
            //val parentAncestors = if (null == parent) emptyList() else parent.ancestors + parent
            val existing = possible.find { ps -> ps.directParent?.rulePositionWlh == parent?.rulePositionWlh }
            return if (null == existing) {
                throw ParseException("next states should be already created")
            } else {
                existing
            }
        }

        internal fun fetchAll(rulePosition: RulePositionWithLookahead) :Set<ParserState> {
            return this.states[rulePosition]
        }

        internal fun fetch(rulePosition: RulePositionWithLookahead, directParent: ParserState?) :ParserState {
            return this.fetchAll(rulePosition).first {
                it.directParent==directParent
            }
        }
    */
    internal fun fetch(rulePosition: RulePosition): ParserState {
        return this.states[rulePosition]
                ?: throw ParserException("should never be null")
    }

    internal fun fetchOrNull(rulePosition: RulePosition): ParserState? {
        return this.states[rulePosition]
    }

    fun calcLookahead(childRP: RulePosition): Set<RuntimeRule> {
        return when {
            childRP.runtimeRule == this.startState.runtimeRule  -> {
                if (childRP.isAtStart) {
                    this.possibleEndOfText.toSet()
                } else {
                    emptySet()
                }
            }
            childRP.runtimeRule==userGoalRule && childRP.isAtEnd -> {
                this.possibleEndOfText.toSet()
            }
            childRP.isAtEnd -> { //use parent lookahead
                val prs = this.parentRelation(childRP.runtimeRule)
                prs.flatMap { parentRelation ->
                    val lh = parentRelation.lookahead
                    lh
                }.toSet()
            }
            else -> { // use first of next
                childRP.items.flatMap { fstChildItem ->
                    val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                    nextRPs.flatMap { nextChildRP ->
                        if (nextChildRP.isAtEnd) {
                            this.calcLookahead(nextChildRP)
                        } else {
                            val lh: Set<RuntimeRule> = this.runtimeRuleSet.firstTerminals2[nextChildRP] ?: throw ParserException("should never happen")
                            if (lh.isEmpty()) {
                                throw ParserException("should never happen")
                            } else {
                                lh
                            }
                        }
                    }
                }.toSet()
            }
        }
    }

    fun calcLookahead(parent: ParentRelation, childRP: RulePosition): Set<RuntimeRule> {
        return when (childRP.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> parent.lookahead
            RuntimeRuleKind.EMBEDDED -> parent.lookahead
            //val rr = childRP.runtimeRule
            //rr.embeddedRuntimeRuleSet!!.firstTerminals[rr.embeddedStartRule!!.number]
            //}
            RuntimeRuleKind.GOAL -> when (childRP.position) {
                0 -> childRP.runtimeRule.rhs.items.drop(1).toSet()
                else -> emptySet()
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                when {
                    childRP.isAtEnd -> parent.lookahead
                    else -> {
                        //this childRP will not itself be applied to Height or GRAFT,
                        // however it should carry the FIRST of next in the child,
                        // so that this childs children can use it if needed
                        childRP.items.flatMap { fstChildItem ->
                            val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                            nextRPs.flatMap { nextChildRP ->
                                if (nextChildRP.isAtEnd) {
                                    this.calcLookahead(parent, nextChildRP)
                                } else {
                                    val lh: Set<RuntimeRule> = this.runtimeRuleSet.firstTerminals2[nextChildRP] ?: throw ParserException("should never happen")
                                    if (lh.isEmpty()) {
                                        throw ParserException("should never happen")
                                    } else {
                                        lh
                                    }
                                }
                            }
                        }.toSet()
                    }
                }
            }
        }
    }
/*
    private fun useParentLH(parent: ParentRelation): Set<RuntimeRule> {
        return if (null == parent) {
            ifEmpty
        } else {
            if (parent.isAtEnd) {
                parent.lookahead
            } else {
                val nextRPs = parent.rulePosition.next()//nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                nextRPs.flatMap { nextRP ->
                    if (nextRP.isAtEnd) {
                        calcLookahead(null, parent.rulePosition, parent.lookahead)
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParserException("should never happen")
                        if (lh.isEmpty()) {
                            throw ParserException("should never happen")
                        } else {
                            lh
                        }
                    }
                }.toSet()
            }
        }
    }
*/
}