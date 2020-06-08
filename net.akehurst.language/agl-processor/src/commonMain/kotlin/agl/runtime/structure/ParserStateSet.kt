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
import net.akehurst.language.collections.lazyMapNonNull

class ParserStateSet(
        val number:Int,
        val runtimeRuleSet: RuntimeRuleSet,
        val userGoalRule: RuntimeRule, //null if skip state set TODO: improve this!
        val possibleEndOfText: List<RuntimeRule>
) {

    private var nextState = 0
    private var nextParentRelation = 0

    /*
     * A RulePosition identifies a Parser state.
     * LR(0) states
     * The parentRelation can be used to determine the LR(1) related lookahead
     */
    internal val states = mutableMapOf<RulePosition, ParserState>()

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule, this.possibleEndOfText)
        val goalRP = RulePosition(goalRule, 0, 0)
        val startState = this.fetchOrCreateParseState(goalRP)
        startState
    }

    private val userGoalParentRelation : ParentRelation by lazy {
        ParentRelation(this, this.nextParentRelation++, this.startState.rulePosition, possibleEndOfText.toSet())
    }

    // runtimeRule -> set of rulePositions where the rule is used
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        //TODO: possibly faster to pre cache this! and goal rules currently not included!
        if (childRR == RuntimeRuleSet.END_OF_TEXT) { //TODO: should this check for contains in possibleEndOfText, and what if something in endOfText is also valid mid text!
            setOf(RulePosition(this.startState.runtimeRule, 0, 1))
        } else {
            val s = this.runtimeRuleSet.parentPosition[childRR]
            if (childRR == this.userGoalRule) {
                s + this.startState.rulePosition
            } else {
                s
            }
        }
    }

    internal val _lookahead = mutableMapOf<RulePosition, Set<RuntimeRule>>()
    fun getLookahead(rp: RulePosition) : Set<RuntimeRule>{
        var set = this._lookahead[rp]
        if (null==set) {
            set = this.calcLookahead1(rp, BooleanArray(this.runtimeRuleSet.runtimeRules.size))
            this._lookahead[rp] = set
        }
        return set
    }

    internal val _parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
    internal fun parentRelation(runtimeRule: RuntimeRule): Set<ParentRelation> {
        var set = _parentRelations[runtimeRule]
        return if (null == set) {
            val t = if (runtimeRule == this.userGoalRule) {
                setOf(this.userGoalParentRelation)
            } else {
                emptySet()
            }
            set = t + this.calcParentRelation(runtimeRule)
            _parentRelations[runtimeRule] = set
            set
        } else {
            set
        }
    }

    private fun calcParentRelation(childRR: RuntimeRule): Set<ParentRelation> {
        val x = this.parentPosition[childRR].map { rp ->
            val lh = getLookahead(rp)
            ParentRelation(this, this.nextParentRelation++, rp, lh)
        }.toSet()
        return x
    }

    internal fun fetchOrCreateParseState(rulePosition: RulePosition): ParserState { //
        val existing = this.states[rulePosition]
        return if (null == existing) {
            val v = ParserState(StateNumber(this.nextState++), rulePosition, this)
            this.states[rulePosition] = v
            v
        } else {
            existing
        }
    }

    internal fun fetch(rulePosition: RulePosition): ParserState {
        return this.states[rulePosition]
                ?: error("should never be null")
    }

    internal fun fetchOrNull(rulePosition: RulePosition): ParserState? {
        return this.states[rulePosition]
    }

    fun calcLookahead1(rp: RulePosition, done: BooleanArray): Set<RuntimeRule> {
        //TODO("try and split this so we do different things depending on the 'rule type/position' multi/slist/mid/begining/etc")
        return when {
            rp.runtimeRule.number >=0 && done[rp.runtimeRule.number] -> emptySet()
            rp.runtimeRule == this.startState.runtimeRule -> {
                if (rp.isAtStart) {
                    this.possibleEndOfText.toSet()
                } else {
                    emptySet()
                }
            }
            rp.isAtEnd -> { //use parent lookahead
                val pps = this.parentPosition[rp.runtimeRule]
                pps.flatMap { parentRp ->
                    val newDone = done //.copyOf() //TODO: do we need to copy?
                    newDone[rp.runtimeRule.number] = true
                    val lh = this.calcLookahead1(parentRp, newDone)
                    lh
                }.toSet()
            }
            else -> { // use first of next
                rp.items.flatMap { fstChildItem ->
                    val nextRPs = rp.next() //nextRulePosition(childRP, fstChildItem)
                    nextRPs.flatMap { nextChildRP ->
                        if (nextChildRP.isAtEnd) {
                            this.calcLookahead1(nextChildRP, done)
                        } else {
                            val lh: Set<RuntimeRule> = this.runtimeRuleSet.firstTerminals2[nextChildRP]
                                    ?: error("should never happen")
                            if (lh.isEmpty()) {
                                error("should never happen")
                            } else {
                                lh
                            }
                        }
                    }
                }.toSet()
            }
        }
    }

    /*
        fun calcLookahead(childRP: RulePosition): Set<RuntimeRule> {
            return when {
                childRP.runtimeRule == this.startState.runtimeRule  -> {
                    if (childRP.isAtStart) {
                        this.possibleEndOfText.toSet()
                    } else {
                        emptySet()
                    }
                }
    //            childRP.runtimeRule==userGoalRule && childRP.isAtEnd -> {
    //                this.possibleEndOfText.toSet()
    //            }
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
    */
    fun calcLookahead(parentLh: Set<RuntimeRule>, childRP: RulePosition): Set<RuntimeRule> {
        return when (childRP.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> parentLh
            RuntimeRuleKind.EMBEDDED -> parentLh
            //val rr = childRP.runtimeRule
            //rr.embeddedRuntimeRuleSet!!.firstTerminals[rr.embeddedStartRule!!.number]
            //}
            RuntimeRuleKind.GOAL -> when (childRP.position) {
                0 -> childRP.runtimeRule.rhs.items.drop(1).toSet()
                else -> emptySet()
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                when {
                    childRP.isAtEnd -> parentLh
                    else -> {
                        //this childRP will not itself be applied to Height or GRAFT,
                        // however it should carry the FIRST of next in the child,
                        // so that this childs children can use it if needed
                        childRP.items.flatMap { fstChildItem ->
                            val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                            nextRPs.flatMap { nextChildRP ->
                                if (nextChildRP.isAtEnd) {
                                    this.calcLookahead(parentLh, nextChildRP)
                                } else {
                                    val lh: Set<RuntimeRule> = this.runtimeRuleSet.firstTerminals2[nextChildRP] ?: throw ParserException("should never happen")
                                    if (lh.isEmpty()) {
                                        error("should never happen")
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

    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean = when(other) {
        is ParserStateSet -> this.number == other.number
        else -> false
    }
    override fun toString(): String = "ParserStateSet{$number}"
}