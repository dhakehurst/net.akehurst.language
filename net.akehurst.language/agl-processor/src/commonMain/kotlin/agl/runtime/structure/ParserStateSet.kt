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

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule, this.possibleEndOfText)
        val goalRP = RulePosition(goalRule, 0, 0)
        val startState = this.fetchOrCreateParseState(goalRP, this.possibleEndOfText.toSet())
        startState
    }

    // runtimeRule -> set of rulePositions where the rule is used
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        //TODO: possibly faster to pre cache this! and goal rules currently not included!
        val s = this.runtimeRuleSet.runtimeRules.flatMap { rr ->
            val rps = rr.rulePositions
            val f = rps.filter { rp ->
                rp.items.contains(childRR)
            }
            f
        }.toSet()
        if (childRR==this.userGoalRule) {
            s + this.startState.rulePosition
        } else {
            s
        }
    }
    internal val ancestorPosition = lazyMapNonNull<RuntimeRule, Set<List<RulePosition>>> { childRR ->
        if (possibleEndOfText.contains(childRR)) {
            setOf(listOf(this.startState.rulePosition.next().first()))
        } else {
            val init: Set<List<RulePosition>> = parentPosition[childRR].map { listOf(it) }.toSet()
            val x: Set<List<RulePosition>> = init.transitiveClosure(false) { list ->
                val pp = parentPosition[list.last().runtimeRule]
                if (pp.isEmpty()) {
                    setOf(list)
                } else {
                    pp.mapNotNull { rp ->
                        val indexOfRp = list.indexOf(rp)
                        when (indexOfRp) {
                            -1 -> list + rp //not contained, so carry on
                            0 -> list + rp //no parent, should never happen I think !
                            else -> {
                                null
                            }
                        }
                    }.toSet()
                }
            }.toSet()
            x
        }
    }
    internal val parentRelations = lazyMapNonNull<RuntimeRule, Set<ParentRelation>> { childRR ->
        if (possibleEndOfText.contains(childRR)) {
            setOf(ParentRelation(this.startState.rulePosition.next().first(), emptySet()))
        } else {
            val init = ancestorPosition[childRR]
            val x = init.map { list ->
                val d = list.dropLast(1) //don't need the goal from end, this is used as the initAcc
                d.foldRight(ParentRelation(this.startState.rulePosition, possibleEndOfText.toSet())) { rp, acc ->
                    val lh = this.calcLookahead(acc, rp)
                    ParentRelation(rp, lh)
                }
            }.toSet()
            x
        }
    }

    /*
     * A RulePosition with Lookahead identifies a set of Parser states.
     * we index the map with RulePositionWithLookahead because that is what is used to create a new state,
     * and thus it should give fast lookup
     */
    internal val states = mutableMapOf<RulePosition, ParserState>()

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
        return this.states[rulePosition] ?: throw ParserException("should never be null")
    }

    private fun calcLookahead(parent: ParentRelation, childRP: RulePosition): Set<RuntimeRule> {
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
                                    if (null == parent) {
                                        TODO() //ifEmpty
                                    } else {
                                        this.calcLookahead(parent, nextChildRP)
                                    }
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