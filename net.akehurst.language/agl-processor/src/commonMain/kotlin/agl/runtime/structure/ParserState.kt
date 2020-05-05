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

import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.collections.transitiveClosure

data class ParentRelation(
        val rulePosition: RulePosition,
        val lookahead: Set<RuntimeRule>
)

class ParserState(
        val number: StateNumber,
        val rulePosition: RulePosition,
        val stateSet: ParserStateSet
) {

    companion object {
        val multiRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePosition
            when {
                previousRp.isAtEnd -> gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.MULIT_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                    }

                }
                else -> true
            }
        }
        val sListRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePosition
            when {
                previousRp.isAtEnd -> (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.SLIST_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                    }
                }
                previousRp.position == RulePosition.SLIST_SEPARATOR_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                    }
                }
                else -> true
            }
        }
    }

    internal var transitions_cache: MutableMap<ParserState?, Set<Transition>?> = mutableMapOf()

    val parentRelations: List<ParentRelation> by lazy {
        if (rulePosition.runtimeRule.kind == RuntimeRuleKind.GOAL) {
            emptyList()
        } else {
            this.stateSet.parentRelation(this.rulePosition.runtimeRule).toList()
        }
    }

    val items: Set<RuntimeRule>
        inline get() {
            return this.rulePosition.items
        }
    val runtimeRule: RuntimeRule
        inline get() {
            return this.rulePosition.runtimeRule
        }
    val choice: Int
        inline get() {
            return this.rulePosition.choice
        }
    val position: Int
        inline get() {
            return this.rulePosition.position
        }

    val isAtEnd: Boolean
        inline get() {
            return this.rulePosition.isAtEnd
        }

    fun growsInto(prevRp: ParserState?): List<ParentRelation> {
        //handles the up part of the closure.
        return when {
            null == prevRp -> this.parentRelations
            prevRp.isAtEnd -> emptyList()
            else -> this.parentRelations.filter { pr ->
                when {
                    pr.rulePosition == prevRp.rulePosition -> true
                    else -> {
                        val prevClosure = prevRp.createClosure(pr.lookahead) //this.createClosure(prevRp, null)
                        prevClosure.content.any {
                            it.runtimeRule == pr.rulePosition.runtimeRule //&& it.lookahead == pr.lookahead
                        }
                    }
                }
            }
        }
    }

    fun createClosure(parentLookahead: Set<RuntimeRule>): RulePositionClosure {
        // create closure from this.rulePosition (root of the state) down
        val stateMap = this.stateSet
        val firstParentLh = parentLookahead//parentRelation?.lookahead ?: emptySet()
        val rootWlh = RulePositionWithLookahead(this.rulePosition, firstParentLh) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitiveClosure { parent ->
            val parentRP = parent.rulePosition
            val parentLh = parent.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    val lh = this.calcLookahead(parent, childRP, parentLh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        return RulePositionClosure(ClosureNumber(-1), this.rulePosition, closureSet)
    }

    fun transitions(previous: ParserState?): Set<Transition> {
        val cache = this.transitions_cache[previous]
        val trans = if (null == cache) {
            //TODO: remove dependency on previous when calculating transitions! ?
            val transitions = this.calcTransitions(previous)
            this.transitions_cache[previous] = transitions
            transitions
        } else {
            cache
        }
       // val filtered = this.growsInto(previous)
        return trans
    }

    private fun calcTransitions(previous: ParserState?): Set<Transition> { //TODO: add previous in order to filter parent relations
        val heightTransitions = mutableSetOf<Transition>()
        val graftTransitions = mutableSetOf<Transition>()
        val widthTransitions = mutableSetOf<Transition>()
        val goalTransitions = mutableSetOf<Transition>()
        val embeddedTransitions = mutableSetOf<Transition>()

        val goal = this.runtimeRule.kind == RuntimeRuleKind.GOAL && this.isAtEnd //from.rulePosition.position==1//from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = this
            goalTransitions += Transition(this, to, action, emptySet(), null) { _, _ -> true }
        } else {
            if (this.isAtEnd) {
                if (this.parentRelations.isEmpty()) {
                    //end of skip
                    val action = Transition.ParseAction.GOAL
                    val to = this
                    goalTransitions += Transition(this, to, action, emptySet(), null) { _, _ -> true }
                } else {
                    val filteredRelations = this.growsInto(previous)
                    for (parentRelation in filteredRelations) {
                        if (parentRelation.rulePosition.runtimeRule.kind == RuntimeRuleKind.GOAL) {
                            when {
                                this.runtimeRule == RuntimeRuleSet.END_OF_TEXT -> {
                                    graftTransitions += this.createGraftTransition(parentRelation)
                                }
                                (parentRelation.lookahead.isEmpty()) -> {
                                    // must be end of skip. TODO: can do something better than this!
                                    val action = Transition.ParseAction.GOAL
                                    val to = this
                                    goalTransitions += Transition(this, to, action, emptySet(), null) { _, _ -> true }
                                }
                                else -> {
                                    graftTransitions += this.createGraftTransition(parentRelation)
                                }
                            }
                        } else {
                            val height = parentRelation.rulePosition.isAtStart
                            val graft = parentRelation.rulePosition.isAtStart.not()
                            if (height) {
                                heightTransitions += this.createHeightTransition(parentRelation)
                            }
                            if (graft) {
                                graftTransitions += this.createGraftTransition(parentRelation)
                            }
                        }
                    }
                }
            } else {
                val parentRelations = this.stateSet.fetchOrCreateParseState(this.rulePosition.atEnd()).parentRelations
                if (parentRelations.isEmpty()) {
                    val prLh = when (this.runtimeRule.kind) {
                        RuntimeRuleKind.GOAL -> this.rulePosition.next().flatMap { it.items }.toSet()
                        else -> emptySet<RuntimeRule>()
                    }
                    val closure = this.createClosure(prLh)
                    for (closureRPlh in closure.content) {
                        when (closureRPlh.runtimeRule.kind) {
                            RuntimeRuleKind.TERMINAL -> widthTransitions += this.createWidthTransition(closureRPlh)
                            RuntimeRuleKind.EMBEDDED -> embeddedTransitions += this.createEmbeddedTransition(closureRPlh)
                        }
                    }
                } else {
                    val filteredRelations = this.growsInto(previous)
                    for (parentRelation in filteredRelations) {
                        val closure = this.createClosure(parentRelation.lookahead)
                        for (closureRPlh in closure.content) {
                            when (closureRPlh.runtimeRule.kind) {
                                RuntimeRuleKind.TERMINAL -> widthTransitions += this.createWidthTransition(closureRPlh)
                                RuntimeRuleKind.EMBEDDED -> embeddedTransitions += this.createEmbeddedTransition(closureRPlh)
                            }
                        }
                    }
                }
            }
        }

        //TODO: merge transitions with everything duplicate except lookahead (merge lookaheads)
        //not sure if this should be before or after the h/g conflict test.

        val conflictHeightTransitionPairs = mutableSetOf<Pair<Transition, Transition>>()
        for (trh in heightTransitions) {
            for (trg in graftTransitions) {
                if (trg.lookaheadGuard.containsAll(trh.lookaheadGuard)) {
                    val newTr = Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
                    conflictHeightTransitionPairs.add(Pair(trh, trg))
                }
            }
        }

        val conflictHeightTransitions = conflictHeightTransitionPairs.map {
            val trh = it.first
            val trg = it.second
            Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
        }
        //Note: need to do the conflict stuff to make multi work, at present!

        //val newHeightTransitions = (heightTransitions - conflictHeightTransitionPairs.map { it.first }) + conflictHeightTransitions

        val newHeightTransitions = heightTransitions
        val mergedWidthTransitions = widthTransitions.groupBy { it.to }.map {
            Transition(this, it.key, Transition.ParseAction.WIDTH, it.value.flatMap { it.lookaheadGuard }.toSet(), null) { _, _ -> true }
        }

        val mergedHeightTransitions = newHeightTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(this, it.key.first, Transition.ParseAction.HEIGHT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second) { _, _ -> true }
        }

        val mergedGraftTransitions = graftTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(this, it.key.first, Transition.ParseAction.GRAFT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second, it.value[0].runtimeGuard)
        }

        val transitions = mergedHeightTransitions + mergedGraftTransitions + mergedWidthTransitions + goalTransitions + embeddedTransitions
        return transitions.toSet()
    }

    private fun createWidthTransition(closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.WIDTH
        val lookaheadGuard = closureRPlh.lookahead
        val toRP = RulePosition(closureRPlh.runtimeRule, closureRPlh.choice, RulePosition.END_OF_RULE)
        val to = this.stateSet.fetchOrCreateParseState(toRP) //TODO: state also depends on lh (or parentRels!)
        return Transition(this, to, action, lookaheadGuard, null) { _, _ -> true }
    }

    private fun createHeightTransition(parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT

        val toSet = parentRP.next().map { this.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, this.rulePosition, parentRelation.lookahead)
                setOf(Transition(this, to, action, lookaheadGuard, prevGuard) { _, _ -> true })
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> to.stateSet.calcLookahead(parentRelation, this.rulePosition)
                    }
                    val lookaheadGuard = lh //parentRelation.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition) //parentRelation.lookahead //this.calcLookahead(RulePositionWithLookahead(to.rulePosition, parentRelation.lookahead), from.rulePosition, parentRelation.lookahead)
                    Transition(this, to, action, lookaheadGuard, prevGuard) { _, _ -> true }
                }
            }

        }.toSet()
    }

    private fun createGraftTransition(parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val toSet = parentRP.next().map { this.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        val runtimeGuard: Transition.(GrowingNode, RulePosition?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.runtimeRule.rhs.kind) {
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, this.rulePosition, parentRelation.lookahead)
                setOf(Transition(this, to, action, lookaheadGuard, prevGuard, runtimeGuard))
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> to.stateSet.calcLookahead(parentRelation, this.rulePosition)
                    }
                    val lookaheadGuard = lh //toParent.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition)  //this.calcLookahead(RulePositionWithLookahead(toParent.rulePosition, toParent.lookahead), from.rulePosition, toParent.lookahead)
                    Transition(this, to, action, lookaheadGuard, prevGuard, runtimeGuard)
                }
            }

        }.toSet()
    }

    private fun createEmbeddedTransition(closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.EMBED
        val lookaheadGuard = closureRPlh.lookahead
        val toRP = RulePosition(closureRPlh.runtimeRule, closureRPlh.choice, RulePosition.END_OF_RULE)
        val to = this.stateSet.fetchOrCreateParseState(toRP)
        return Transition(this, to, action, lookaheadGuard, null) { _, _ -> true }
    }

    private fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return when (childRP.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> useParentLH(parent, ifEmpty)
            RuntimeRuleKind.EMBEDDED -> useParentLH(parent, ifEmpty)
            //val rr = childRP.runtimeRule
            //rr.embeddedRuntimeRuleSet!!.firstTerminals[rr.embeddedStartRule!!.number]
            //}
            RuntimeRuleKind.GOAL -> when (childRP.position) {
                0 -> childRP.runtimeRule.rhs.items.drop(1).toSet()
                else -> emptySet()
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                when {
                    childRP.isAtEnd -> useParentLH(parent, ifEmpty)
                    else -> {
                        //this childRP will not itself be applied to Height or GRAFT,
                        // however it should carry the FIRST of next in the child,
                        // so that this childs children can use it if needed
                        childRP.items.flatMap { fstChildItem ->
                            val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                            nextRPs.flatMap { nextChildRP ->
                                if (nextChildRP.isAtEnd) {
                                    if (null == parent) {
                                        ifEmpty
                                    } else {
                                        calcLookahead(null, parent.rulePosition, parent.lookahead)
                                    }
                                } else {
                                    val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextChildRP]
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
        }
    }

    private fun useParentLH(parent: RulePositionWithLookahead?, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
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
                        val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextRP] ?: error("should never happen")
                        if (lh.isEmpty()) {
                            error("should never happen")
                        } else {
                            lh
                        }
                    }
                }.toSet()
            }
        }
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.number.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.number.value == other.number.value
        } else {
            false
        }
    }

    override fun toString(): String {
        return "State(${this.number.value}-${rulePosition})"
    }

}
