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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.collections.MutableQueue
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableStackOf

internal class BuildCacheLC1(
    stateSet: ParserStateSet
) : BuildCacheAbstract(stateSet) {

    private companion object {
        class ClosureItemLC1(
            val parentItem: ClosureItemLC1?, //needed for height/graft
            val rulePosition: RulePosition,
            val next: RulePosition?,
            val lookaheadSet: LookaheadSetPart
        ) {
            val allPrev: Set<ClosureItemLC1> by lazy { if (null == parentItem) mutableSetOf() else parentItem.allPrev + parentItem }

            val allRulePositionsButTop: List<RulePosition> by lazy {
                if (null == parentItem) {
                    mutableListOf()
                } else {
                    parentItem.allRulePositionsButTop + rulePosition
                }
            }

            fun equivalentOf(other: ClosureItemLC1): Boolean {
                return this.rulePosition == other.rulePosition &&
                        this.lookaheadSet == other.lookaheadSet &&
                        this.parentItem?.lookaheadSet == other.parentItem?.lookaheadSet

            }

            private fun chain(): String {
                val p = if (null == parentItem) {
                    ""
                } else {
                    "${parentItem.chain()}->"
                }
                return "$p$rulePosition"
            }

            private val _hashCode = listOf(this.parentItem?.lookaheadSet, this.rulePosition, this.next, this.lookaheadSet).hashCode()
            override fun hashCode(): Int = _hashCode

            override fun equals(other: Any?): Boolean = when (other) {
                is ClosureItemLC1 -> other.rulePosition == this.rulePosition &&
                        other.next == this.next &&
                        other.lookaheadSet == this.lookaheadSet &&
                        other.parentItem?.lookaheadSet == this.parentItem?.lookaheadSet
                        && other.allPrev == this.allPrev
                else -> false
            }

            override fun toString(): String {
                return "${chain()}[${lookaheadSet.fullContent.joinToString { it.tag }}]"
            }
        }

        data class PossibleTransInfo(
            val prev: RulePosition,
            val parent: PossibleState?,
            val parentFirstOfNext: LookaheadSetPart,
            val action: Transition.ParseAction,
            val firstOf: LookaheadSetPart,
            val firstOfNext: LookaheadSetPart
        ) {
            val to: List<RulePosition>
                get() = when (this.action) {
                    Transition.ParseAction.WIDTH, Transition.ParseAction.EMBED -> this.firstOf.fullContent.map { RulePosition(it, 0, RulePosition.END_OF_RULE) }
                    Transition.ParseAction.HEIGHT, Transition.ParseAction.GRAFT -> this.parent?.rulePosition?.next()?.toList() ?: emptyList()
                    Transition.ParseAction.GOAL -> listOf(prev.atEnd())
                }
        }

        class PossibleState(
            val firstOfCache: Map<RulePosition, MutableMap<RulePosition?, LookaheadSetPart>>,
            val rulePosition: RulePosition
        ) {
            val isAtStart: Boolean get() = this.rulePosition.isAtStart
            val isAtEnd: Boolean get() = this.rulePosition.isAtEnd
            val isGoalStart: Boolean get() = this.rulePosition.runtimeRule.isGoal && this.isAtStart
            val isGoalEnd: Boolean get() = this.rulePosition.runtimeRule.isGoal && this.isAtEnd

            val outTransInfo = mutableMapOf<RulePosition?, MutableSet<PossibleTransInfo>>()

            val mergedTransInfo: Set<TransInfo> by lazy {
                val tis = this.outTransInfo.values.flatten().toSet()
                val tisAtEnd = tis.filter { it.to.first().isAtEnd }
                //TODO: need to split things at end!
                val mergedAtEnd = tisAtEnd.map {
                    val parent = it.parent?.let { listOf(it.rulePosition) } ?: emptyList()
                    val lhs = when (it.action) {
                        Transition.ParseAction.GOAL -> LookaheadSetPart.EOT
                        Transition.ParseAction.WIDTH, Transition.ParseAction.EMBED -> {
                            it.to.map { tgt ->
                                if (tgt.runtimeRule == RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD) {
                                    LookaheadSetPart.EMPTY
                                } else {
                                    this.firstOfCache[tgt]!![this.rulePosition]!!
                                }
                            }.reduce { acc, l -> acc.union(l) }
                        }
                        Transition.ParseAction.HEIGHT, Transition.ParseAction.GRAFT -> it.firstOfNext
                    }
                    TransInfo(setOf(setOf(it.prev)), parent.toSet(), it.action, it.to.toSet(), setOf(LookaheadInfoPart(lhs, LookaheadSetPart.EMPTY)))
                }
                val tisNotAtEnd = tis - tisAtEnd
                val groupNotAtEnd = tisNotAtEnd.groupBy { Pair(it.action, it.to) }
                val mergedNotAtEnd = groupNotAtEnd.map { me ->
                    val prev = me.value.map { it.prev.let { setOf(it) } }.toSet()
                    val parent = me.value.mapNotNull { it.parent?.rulePosition }.toSet()
                    //val parentFirstOfNext = me.value.map { it.parentFirstOfNext }
                    val parentFirstOfNext = me.value.map { tr ->
                        //tr.parent?.rulePosition?.next()?.map { this.firstOfNext(tr.prev) } ?: emptyList()
                        tr.parentFirstOfNext
                    }
                    val action = me.key.first
                    val to = me.key.second
                    val lookaheadSet = when (action) {
                        Transition.ParseAction.GOAL -> LookaheadSetPart.EOT
                        Transition.ParseAction.WIDTH, Transition.ParseAction.EMBED -> me.value.map { tr ->
                            val p = when {
                                this.isAtStart -> tr.prev ?: this.rulePosition
                                else -> this.rulePosition
                            }
                            this.firstOfCache[tr.to.first()]!!.get(this.rulePosition)!!
                        }.reduce { acc, it -> acc.union(it) }
                        Transition.ParseAction.HEIGHT, Transition.ParseAction.GRAFT -> me.value.map { tr -> tr.firstOfNext }.reduce { acc, it -> acc.union(it) }
                    }
                    val lhInfo = parentFirstOfNext.map { LookaheadInfoPart(lookaheadSet, it) }.toSet()
                    TransInfo(prev, parent, action, to.toSet(), lhInfo)
                }.toSet()
                mergedNotAtEnd + mergedAtEnd
            }

            val allPrev get() = outTransInfo.values.flatMap { it.map { it.prev } }.toSet().toList()

            fun setTransInfo(prev: RulePosition, parent: PossibleState?, parentFirstOfNext: LookaheadSetPart, firstOf: LookaheadSetPart, firstOfNext: LookaheadSetPart) {
                val action = when {
                    rulePosition.runtimeRule.isGoal -> when {
                        rulePosition.isAtEnd -> Transition.ParseAction.GOAL    // RP(G,0,EOR)
                        rulePosition.isAtStart -> Transition.ParseAction.WIDTH // RP(G,0,SOR)
                        else -> error("should not happen")
                    }
                    this.rulePosition.isAtEnd -> when {
                        null == parent -> error("should not happen")
                        parent.isGoalStart -> Transition.ParseAction.GRAFT
                        parent.isAtStart -> Transition.ParseAction.HEIGHT
                        parent.isAtEnd -> error("should not happen")
                        else -> Transition.ParseAction.GRAFT
                    }
                    else -> Transition.ParseAction.WIDTH
                }
                var set = outTransInfo[prev]
                if (null == set) {
                    set = mutableSetOf()
                    outTransInfo[prev] = set
                }
                set.add(PossibleTransInfo(prev, parent, parentFirstOfNext, action, firstOf, firstOfNext))
            }

            fun firstOfNext(prev: RulePosition?): LookaheadSetPart = this.firstOfCache[this.rulePosition]!!.get(prev)!!

            override fun hashCode(): Int = this.rulePosition.hashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is PossibleState -> false
                else -> this.rulePosition == other.rulePosition
            }

            override fun toString(): String = "${this.rulePosition}{${outTransInfo.entries.joinToString { "${it.key}" }}}"
        }

        val StateInfo.isAtEnd: Boolean get() = this.rulePositions.any { it.isAtEnd }
        val StateInfo.isGoal: Boolean get() = this.rulePositions.any { it.isGoal }

        val StateInfo.mergedTransInfo: Set<TransInfo>
            get() {
                val tis = this.possibleTrans
                val groupedTis = tis.groupBy { Pair(it.action, it.to) }
                val mergedTis = groupedTis.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val parent = me.value.flatMap { it.parent }.toSet()
                    val action = me.key.first
                    val to = me.key.second
                    val lhInfo = me.value.flatMap { it.lookahead }.toSet()
                    TransInfo(prev, parent, action, to, lhInfo)
                }.toSet()
                return mergedTis
            }

    }

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItemLC1>>()

    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableMap<List<RulePosition>, HeightGraftInfo>>()

    override fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> = this.stateInfo2()

    private fun mergeStates(unmerged: Map<RulePosition, PossibleState>): Set<StateInfo> {
        // merge states with transitions to the same (next) state with same action
        val statesAtEnd = unmerged.filter { it.key.isAtEnd || it.key.isGoal }
        val statesAtEndMapped = statesAtEnd.values.map { state ->
            val rulePositions = listOf(state.rulePosition)
            val possibleTrans = state.mergedTransInfo
            StateInfo(rulePositions, possibleTrans)
        }
        val statesNotAtEnd = unmerged.filter { it.key.isAtEnd.not() && it.key.isGoal.not() }
        val groupedByOutgoing = statesNotAtEnd.values.groupBy { it.mergedTransInfo.map { ti -> Pair(ti.action, ti.to) } }
        val mergedPossibleStates = groupedByOutgoing.map { me ->
            val rulePositions = me.value.map { it.rulePosition }
            val possibleTrans = me.value.flatMap { it.mergedTransInfo }.toSet()
            StateInfo(rulePositions, possibleTrans)
        }.union(statesAtEndMapped).associateBy { it.rulePositions }
        val updatedMergedStates = mergedPossibleStates.values.map { state ->
            val rulePositions = state.rulePositions
            val possibleTrans = state.possibleTrans.map { tr ->
                val prev = tr.prev.map { p ->
                    when {
                        p.isEmpty() -> p
                        p.any { it.isAtStart } -> p
                        else -> mergedPossibleStates.keys.firstOrNull { it.containsAll(p) }?.toSet() ?: p
                    }
                }.toSet()
                val parent = when {
                    tr.parent.isEmpty() -> tr.parent
                    tr.parent.any { it.isAtStart } -> tr.parent
                    else -> mergedPossibleStates.keys.firstOrNull { it.containsAll(tr.parent) }?.toSet() ?: tr.parent
                }
                val to = mergedPossibleStates.keys.firstOrNull { it.containsAll(tr.to) }?.toSet() ?: tr.to
                TransInfo(prev, parent, tr.action, to, tr.lookahead)
            }.toSet()
            StateInfo(rulePositions, possibleTrans)
        }

        var originalNumStates = unmerged.size
        var mergedNum = updatedMergedStates.size
        var updMergedStates = updatedMergedStates
        while (mergedNum != originalNumStates) {
            val atEnd = updMergedStates.filter { it.isAtEnd || it.isGoal }
            val nonAtEnd = updMergedStates.filter { it.isAtEnd.not() && it.isGoal.not() }
            val grpdByOutgoing = nonAtEnd.groupBy { it.mergedTransInfo.map { ti -> Pair(ti.action, ti.to) } }
            val mgdPossibleStates = grpdByOutgoing.map { me ->
                val rulePositions = me.value.flatMap { it.rulePositions }
                val possibleTrans = me.value.flatMap { it.mergedTransInfo }.toSet()
                StateInfo(rulePositions, possibleTrans)
            }.union(atEnd).associateBy { it.rulePositions }
            updMergedStates = mgdPossibleStates.values.map { state ->
                val rulePositions = state.rulePositions
                val possibleTrans = state.possibleTrans.map { tr ->
                    val prev = tr.prev.map { p ->
                        when {
                            p.isEmpty() -> p
                            p.any { it.isAtStart } -> p
                            else -> mgdPossibleStates.keys.firstOrNull { it.containsAll(p) }?.toSet() ?: p
                        }
                    }.toSet()
                    val parent = when {
                        tr.parent.isEmpty() -> tr.parent
                        tr.parent.any { it.isAtStart } -> tr.parent
                        else -> mgdPossibleStates.keys.firstOrNull { it.containsAll(tr.parent) }?.toSet() ?: tr.parent
                    }
                    val to = mgdPossibleStates.keys.firstOrNull { it.containsAll(tr.to) }?.toSet() ?: tr.to
                    TransInfo(prev, parent, tr.action, to, tr.lookahead)
                }.toSet()
                StateInfo(rulePositions, possibleTrans)
            }
            originalNumStates = mergedNum
            mergedNum = updMergedStates.size
        }

        return updMergedStates.toSet()//mergedStateInfo
    }

    internal fun stateInfo2(): Set<StateInfo> {
        data class L1Trans(
            val parent: RulePosition?,
            val action: Transition.ParseAction,
            val to: RulePosition,
            val guard: LookaheadSetPart,
            val up: LookaheadSetPart
        )

        class L1State(
            var parent: L1State?,
            val rulePosition: RulePosition, // position in rule (no need for position 0)
            val followAtEnd: LookaheadSetPart, // terminals expected at the end of the rule (same for all RPs for this rule)
        ) {
            val prev: RulePosition = when {
                null == parent -> rulePosition // RP(G,0,0)
                parent!!.rulePosition.isAtStart -> parent!!.prev
                else -> parent!!.rulePosition
            }
            val prevState: L1State
                get() = when {
                    null == parent -> this // RP(G,0,0)
                    parent!!.rulePosition.isAtStart -> parent!!.prevState
                    else -> parent!!
                }
            private val parentNextNotAtEnd: Set<RulePosition>
                get() = when (parent) {
                    null -> emptySet()
                    else -> parent!!.nextNotAtEnd
                }
            val nextNotAtEnd: Set<RulePosition>
                get() = when {
                    rulePosition.isAtEnd -> parentNextNotAtEnd
                    else -> rulePosition.next().flatMap { nxRp ->
                        when {
                            nxRp.isAtEnd -> parentNextNotAtEnd
                            else -> listOf(nxRp)
                        }
                    }.toSet()
                }

            val parentFollowAtEnd = this.parent?.followAtEnd ?: LookaheadSetPart.EOT //parent is null for G & UP comes after (G,0,-1)
            val expectedAt = when {
                rulePosition.isAtEnd -> followAtEnd
                rulePosition.isGoal -> LookaheadSetPart.EOT // follow of (G,0,0) is UP
                else -> rulePosition.next().map { nx ->
                    when {
                        nx.isAtEnd -> followAtEnd
                        nx.isGoal -> LookaheadSetPart.EOT
                        else -> expectedAt(nx, followAtEnd)
                    }
                }.reduce { acc, l -> acc.union(l) }
            }

            fun outTransitions(parentOf: Map<Pair<RulePosition, RuntimeRule>, Set<L1State>>): Set<L1Trans> {
                return when {
                    null == parent && rulePosition.isAtEnd -> emptySet() // G,0,EOR
                    rulePosition.isAtEnd -> {
                        val action = when {
                            parent!!.rulePosition.isGoal -> Transition.ParseAction.GOAL
                            parent!!.rulePosition.isAtStart -> Transition.ParseAction.HEIGHT
                            else -> Transition.ParseAction.GRAFT
                        }
                        val targets = parent!!.rulePosition.next()
                        val to = targets.map { tgt ->
                            // expectedAt(tgt) - follow(parent) if atEnd
                            val grd = expectedAt(tgt, parentFollowAtEnd)
                            val up = when (action) {
                                Transition.ParseAction.HEIGHT -> parentFollowAtEnd
                                Transition.ParseAction.EMBED,
                                Transition.ParseAction.WIDTH,
                                Transition.ParseAction.GRAFT,
                                Transition.ParseAction.GOAL -> LookaheadSetPart.EMPTY
                            }
                            Triple(tgt, grd, up)
                        }
                        to.map { p -> L1Trans(parent!!.rulePosition, action, p.first, p.second, p.third) }.toSet()
                    }
                    else -> {
                        val action = Transition.ParseAction.WIDTH
                        val tgts = pFirstTerm(this.rulePosition)
                        val to = tgts.flatMap { t ->
                            val pr = this.rulePosition
                            // tgt is an RP so we don;t have its follow,
                            // we can find parentOf(tgt) and follow(tgt)==follow(parentOf(tgt))
                            val tParent = parentOf[Pair(pr, t)]
                            val ls = tParent!!.map { tp -> tp.expectedAt }
                            ls.map { Pair(t.asTerminalRulePosition, it) }
                        }
                        val up = LookaheadSetPart.EMPTY
                        to.map { p -> L1Trans(parent?.rulePosition, action, p.first, p.second, up) }.toSet()
                    }
                }
            }

            private val id = arrayOf(parent?.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
            override fun hashCode(): Int = id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is L1State -> false
                else -> this.id.contentDeepEquals(other.id)
            }

            override fun toString(): String = when (parent) {
                this -> "State{$rulePosition[$followAtEnd]}"
                else -> "State{$rulePosition[$followAtEnd]}-->$parent"
            }
        }

        data class L0L1Trans(
            val prev: Set<Set<RulePosition>>,
            val parent: Set<RulePosition>,
            val to: Set<RulePosition>,
            val action: Transition.ParseAction,
            val lookahead: Set<LookaheadInfoPart>
        )

        class L0State(
            val rulePosition: Set<RulePosition>,
            val outTransitions: Set<L0L1Trans>
        ) {
            override fun hashCode(): Int = rulePosition.hashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is L0State -> false
                else -> this.rulePosition == other.rulePosition
            }

            override fun toString(): String = "State{$rulePosition}"
        }

        val startRP = this.stateSet.startRulePosition
        // G ends at G = S . but we have an implied UP after that, so followAtEnd of G is UP
        val startState = L1State(null, startRP, LookaheadSetPart.EOT)
        val finishRP = this.stateSet.finishRulePosition
        val finishState = L1State(null, finishRP, LookaheadSetPart.EOT)

        val parentOf = lazyMutableMapNonNull<Pair<RulePosition, RuntimeRule>, MutableSet<L1State>> { mutableSetOf() }
        val l1States = mutableSetOf(startState, finishState)
        val todo = mutableStackOf(startState)
        while (todo.isNotEmpty) {
            val state = todo.pop()
            val rp = state.rulePosition
            when {
                rp.isAtEnd -> l1States.add(state)
                else -> {
                    // item is only null if rp isTerminal (handled above)
                    val rps = rp.item!!.rulePositions
                    for (childRP in rps) {
                        val childLhs = state.expectedAt
                        val childState = L1State(state, childRP, childLhs)
                        if (l1States.contains(childState).not()) {
                            l1States.add(childState)
                            val pr = childState.prev
                            parentOf[Pair(pr, childRP.runtimeRule)].add(state)
                            todo.push(childState)
                        } else {
                            // already done
                        }
                    }
                }
            }
        }

        if (Debug.OUTPUT_SM_BUILD) {
            println("LR1 states: ${l1States.size}")
            println("LR1 transitions: ${l1States.flatMap { it.outTransitions(parentOf) }.size}")
            for (state in l1States) {
                if (state.rulePosition.isGoal || state.rulePosition.isAtStart.not()) {
                    val trs = state.outTransitions(parentOf)
                    if (Debug.CHECK) check(trs.isNotEmpty() || state.rulePosition.isGoal) { "No outTransitions for $state" }
                    trs.forEach { tr ->
                        val prevStr = "{${state.prev}[${state.prevState.followAtEnd.fullContent.joinToString { it.tag }}]}"
                        val fromStr = "${state.rulePosition}[${state.followAtEnd.fullContent.joinToString { it.tag }}]"
                        val lh = "${tr.action}[${tr.guard.fullContent.joinToString { it.tag }}](${tr.up.fullContent.joinToString { it.tag }})"
                        println("$prevStr $fromStr -- $lh --> ${tr.to}")
                    }
                }
            }
        }

        val l0States = mutableMapOf<RulePosition, L0State>()
        for (state in l1States) {
            if (state.rulePosition.isGoal || state.rulePosition.isAtStart.not()) {
                val trs = state.outTransitions(parentOf)
                val trans = trs.map { t ->
                    val parent = t.parent?.let { setOf(it) } ?: emptySet()
                    val action = t.action
                    val to = t.to
                    val grd = t.guard
                    val up = t.up
                    L0L1Trans(setOf(setOf(state.prev)), parent, setOf(to), action, setOf(LookaheadInfoPart(grd, up)))
                }.toSet()
                val groupTrans = trans.groupBy { tr -> Triple(tr.prev, tr.action, tr.to) }
                val mergeTrans = groupTrans.map { me ->
                    val prev = me.key.first
                    val action = me.key.second
                    val to = me.key.third
                    val parent = me.value.flatMap { it.parent }.toSet()
                    val lh = me.value.flatMap { it.lookahead }.toSet()
                    val mergedLh = LookaheadInfoPart.merge(lh)
                    L0L1Trans(prev, parent, to, action, mergedLh)
                }.toSet()
                val existing = l0States[state.rulePosition]
                if (null == existing) {
                    val st = L0State(setOf(state.rulePosition), mergeTrans)
                    l0States[state.rulePosition] = st
                } else {
                    val st = L0State(existing.rulePosition + setOf(state.rulePosition), existing.outTransitions + mergeTrans)
                    l0States[state.rulePosition] = st
                }
            }
        }

        if (Debug.OUTPUT_SM_BUILD) {
            println("")
            println("LR0 states")
            for (s in l0States.values) {
                for (t in s.outTransitions) {
                    println("${t.prev} $s -- ${t.action}${t.lookahead.joinToString { "[${it.guard}](${it.up})" }} --> ${t.to}")
                }
            }
        }

        val atEnd = l0States.values.filter { it.rulePosition.first().isAtEnd || it.rulePosition.first().isGoal }
        val notAtEnd = l0States.values.filter { it.rulePosition.first().isAtEnd.not() && it.rulePosition.first().isGoal.not() }
        ///*
        val mergeAtEnd = atEnd.map { st ->
            val rp = st.rulePosition
            val trans = st.outTransitions
            val groupTrans = trans.groupBy { tr -> Triple(tr.prev, tr.action, tr.to) }
            val mergeTrans = groupTrans.map { me ->
                val prev = me.key.first
                val action = me.key.second
                val to = me.key.third
                val parent = me.value.flatMap { it.parent }.toSet()
                val lh = me.value.flatMap { it.lookahead }.toSet()
                val mergedLh = LookaheadInfoPart.merge(lh)
                L0L1Trans(prev, parent, to, action, mergedLh)
            }.toSet()
            L0State(rp, mergeTrans)
        }
        //*/
        /*
        val mergeAtEnd = atEnd.map { st ->
            val rp = st.rulePosition
            val trans = st.outTransitions
            val groupTrans = trans.groupBy { tr -> Pair(tr.action, tr.to) }
            val mergeTrans = groupTrans.map { me ->
                val prev = me.value.flatMap { it.prev }.toSet()
                val action = me.key.first
                val to = me.key.second
                val parent = me.value.flatMap { it.parent }.toSet()
                val lh = me.value.flatMap { it.lookahead }.toSet()
                val mergedLh = LookaheadInfoPart.merge(lh)
                L0L1Trans(prev, parent, to, action, mergedLh)
            }.toSet()
            L0State(rp, mergeTrans)
        }
        */
        /*
        val groupNotAtEnd = notAtEnd.groupBy { st ->
            st.outTransitions.map { tr -> Triple(tr.action, tr.to,tr.parent.map { it.runtimeRule }) }.toSet()
        }
        val mergeNotAtEnd = groupNotAtEnd.map {
            val rp = it.value.flatMap { it.rulePosition }.toSet()
            val trans = it.value.flatMap { it.outTransitions }.toSet()
            val groupTrans = trans.groupBy { tr -> Triple(tr.prev, tr.action, tr.to) }
            val mergeTrans = groupTrans.map { me ->
                val prev = me.key.first
                val action = me.key.second
                val to = me.key.third
                val parent = me.value.flatMap { it.parent }.toSet()
                val lhs = me.value.flatMap { it.lookahead }.toSet()
                    .groupBy { it.up }
                    .map {
                        val up = it.key
                        val guard = it.value.map { it.guard }.reduce { acc, l -> acc.union(l) }
                        L0L1Lookahead(guard, up)
                    }.toSet()
                L0L1Trans(prev, parent, to, action, lhs)
            }.toSet()
            L0State(rp, mergeTrans)
        }
        */
        val mergeNotAtEnd = notAtEnd.map { st ->
            val rp = st.rulePosition
            val trans = st.outTransitions
            val groupTrans = trans.groupBy { tr -> Triple(tr.prev, tr.action, tr.to) }
            val mergeTrans = groupTrans.map { me ->
                val prev = me.key.first
                val action = me.key.second
                val to = me.key.third
                val parent = me.value.flatMap { it.parent }.toSet()
                val lh = me.value.flatMap { it.lookahead }.toSet()
                val mergedLh = LookaheadInfoPart.merge(lh)
                L0L1Trans(prev, parent, to, action, mergedLh)
            }.toSet()
            L0State(rp, mergeTrans)
        }
        val merged = mergeAtEnd + mergeNotAtEnd

        if (Debug.OUTPUT_SM_BUILD) {
            println("")
            println("merged LR0 states")
            for (s in merged) {
                for (t in s.outTransitions) {
                    println("${t.prev} $s -- ${t.action}${t.lookahead.joinToString { "[${it.guard}](${it.up})" }} --> ${t.to}")
                }
            }
        }

        val stateInfo = merged.map { st ->
            val rp = st.rulePosition.toList()
            val trans = st.outTransitions.map { tr ->
                val prev = tr.prev.map { pr -> merged.first { it.rulePosition.containsAll(pr) }.rulePosition }.toSet()
                val parent = tr.parent // only used for prevGuard - no need to map to state
                val action = tr.action
                val to = merged.first { it.rulePosition.containsAll(tr.to) }
                val lh = tr.lookahead.map { LookaheadInfoPart(it.guard, it.up) }.toSet()
                TransInfo(prev, parent, action, to.rulePosition, lh)
            }.toSet()
            /*
            val grouped = trans.groupBy { tr -> Triple(tr.prev, tr.action, tr.to) }
            val merged2 = grouped.map{ me->
                val prev = me.key.first
                val action = me.key.second
                val to = me.key.third
                val parent = me.value.flatMap { it.parent }.toSet()
                val lhs = me.value.flatMap { it.lookahead }.toSet()
                    .groupBy { it.guard }
                    .map {
                        val guard = it.key
                        val up = it.value.map { it.up }.reduce { acc, l -> acc.union(l) }
                        LookaheadInfoPart(guard, up)
                    }.toSet()
                TransInfo(prev, parent, action, to, lhs)
            }.toSet()
             */
            StateInfo(rp, trans)
        }.toSet()
        return stateInfo
    }

    override fun widthInto(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        // the 'to' state is the first Terminal the fromState.rulePosition
        // if there are multiple fromState.rulePositions then they should have same firstOf or they would not be merged.
        // after a WIDTH, fromState becomes the prevState, therefore
        // the lookahead is the firstOf the parent.next of the 'to' state, in the context of the fromStateRulePositions
        if (Debug.OUTPUT_SM_BUILD) Debug.debug(Debug.IndentDelta.INC_AFTER) { "START calcWidthInfo($prevState, $fromState) - ${fromState.rulePositions.map { it.item?.tag }}" }
        this.firstFollowCache.clear()
        //FirstFollow3
        val firstTerminals = prevState.rulePositions.flatMap { prev ->
            fromState.rulePositions.flatMap { from ->
                //TODO: can we do better thn parentFollow == RT here ?
                val parentFollow = when {
                    fromState.isGoal -> LookaheadSetPart.EOT
                    else -> LookaheadSetPart.RT
                }
                this.firstFollowCache.firstTerminalInContext(prev, from, parentFollow) //emptySet(),  parentFollow)
            }
        }.toSet()
        val wis = firstTerminals.map { firstTermInfo ->
            val lhs = firstTermInfo.nextContextFollow
            //if (Debug.CHECK) check(lhs_old.fullContent == follow) { "$lhs_old != [${followResolved.joinToString { it.tag }}] Follow($fromState,${rr.tag})" }
            val rp = firstTermInfo.embeddedRule.asTerminalRulePosition
            val action = when {
                firstTermInfo.embeddedRule.isEmbedded -> Transition.ParseAction.EMBED
                else -> Transition.ParseAction.WIDTH
            }
            WidthInfo(action, rp, lhs)
        }
        val wisMerged = wis.groupBy { Pair(it.to, it.action) }
            .map { me ->
                val rp = me.key.first
                val action = me.key.second
                val lhs = me.value.map { it.lookaheadSet }.reduce { a, e -> a.union(e) }
                WidthInfo(action, rp, lhs)
            }
        if (Debug.OUTPUT_SM_BUILD) Debug.debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcWidthInfo($prevState, $fromState)" }
        return wisMerged.toSet()
    }

    override fun heightOrGraftInto(prevPrev: ParserState, prevState: ParserState, fromState: ParserState): Set<HeightGraftInfo> {
        // return if (this._cacheOff) {
        // have to ensure somehow that from grows into prev
        // have to do closure down from prev,
        // upCls is the closure down from prev
        //val upCls = prevState.state.rulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
        val calc = calcAndCacheHeightOrGraftInto(prevPrev, prevState, fromState)//, upCls)
        return calc
        //} else {
        //    val key = Pair(prevState.rulePositions, fromState.runtimeRules)
        //    this._heightOrGraftInto[key]?.values?.toSet() ?: run {
        //        val upCls = prevState.rulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
        //        val calc = calcAndCacheHeightOrGraftInto(prevState.rulePositions, fromState.runtimeRules, upCls)
        //        calc
        //    }
        //}
    }

    private fun calcAndCacheHeightOrGraftInto(prevPrev: ParserState, prev: ParserState, from: ParserState): Set<HeightGraftInfo> {//, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        val hgi = calcHeightOrGraftInto(prevPrev, prev, from)//, upCls)
        cacheHeightOrGraftInto(prev, from, hgi)
        return hgi
    }

    private fun cacheHeightOrGraftInto(prev: ParserState, from: ParserState, hgis: Set<HeightGraftInfo>) {
        val key = Pair(prev.rulePositions, from.runtimeRules)
        val map = this._heightOrGraftInto[key] ?: run {
            val x = mutableMapOf<List<RulePosition>, HeightGraftInfo>()
            this._heightOrGraftInto[key] = x
            x
        }
        for (hg in hgis) {
            val existing = map[hg.parentNext]//map[hg.parent]
            if (null == existing) {
                map[hg.parentNext] = hg//map[hg.parent] = hg
            } else {
                val lhs = hg.lhs.union(existing.lhs)
                //map[hg.parent] = HeightGraftInfo(hg.ancestors, hg.parent, hg.parentNext, lhs, upLhs)
                map[hg.parentNext] = HeightGraftInfo(hg.action, hg.parentNext, lhs)
            }
        }
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(prevPrev: ParserState, prev: ParserState, from: ParserState): Set<HeightGraftInfo> {//, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        //FirstFollow3
        val info: Set<HeightGraftInfo> = prevPrev.rulePositions.flatMap { contextContext ->
            prev.rulePositions.flatMap { context ->
                val parentsOfFrom = from.runtimeRules.flatMap { fr ->
                    this.firstFollowCache.parentInContext(contextContext, context, emptySet(), fr)
                }.toSet()
                parentsOfFrom.map { parentNext ->
                    val action = when {
                        parentNext.rulePosition.isGoal -> Transition.ParseAction.GOAL
                        parentNext.firstPosition -> Transition.ParseAction.HEIGHT
                        else -> Transition.ParseAction.GRAFT
                    }
                    val tgt = parentNext.rulePosition
                    val follow = parentNext.follow
                    //val follow = this.firstFollowCache.followInContext(parentContext, tgt, parentFollowAtEnd)
                    val grd = follow
                    val up = when (action) {
                        Transition.ParseAction.HEIGHT -> parentNext.parentNextContextFollow
                        Transition.ParseAction.EMBED, Transition.ParseAction.WIDTH, Transition.ParseAction.GRAFT, Transition.ParseAction.GOAL -> LookaheadSetPart.EMPTY
                    }
                    HeightGraftInfo(action, listOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                }
            }
        }.toSet()
        val infoAtEnd = info.filter { it.parentNext.first().isAtEnd }
        val infoAtEndMerged = infoAtEnd.groupBy { Pair(it.action, it.parentNext) }
            .map { me ->
                val action = me.key.first
                val parentNext = me.key.second
                //val lhs = me.value.map { it.lhs }.toSet().reduce { acc, e -> acc.union(e) }
                val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lhs }.toSet())
                //val upLhs = me.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
                HeightGraftInfo(action, parentNext, lhs)
            }
        val infoNotAtEnd = info.filter { it.parentNext.first().isAtEnd.not() }
        val infoToMerge = infoNotAtEnd.groupBy { Pair(it.action, it.parentNext) }
        val mergedInfo = infoToMerge.map { me ->
            val action = me.key.first
            val parentNext = me.key.second
            //val lhs = me.value.map { it.lhs }.toSet().reduce { acc, e -> acc.union(e) }
            val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lhs }.toSet())
            //val upLhs = me.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
            HeightGraftInfo(action, parentNext, lhs)
        }.toSet()
        val r = (infoAtEndMerged + mergedInfo).toSet()
        return r
    }

    /*
    // return the 'closure' of the parent.rulePosition
    private fun traverseRulePositions(parent: ClosureItemLC1): Set<RuntimeRule> {
    return when {
        parent.rulePosition.isAtEnd -> {
            // cache but cannot traverse down
            calcAndCacheHeightOrGraftInto(parent.prev, listOf(parent.rulePosition.runtimeRule), parent.allPrev)
            cacheStateInfo(listOf(parent.rulePosition), parent.prev)
            val rr = parent.rulePosition.runtimeRule
            if (rr.kind == RuntimeRuleKind.TERMINAL || rr.kind == RuntimeRuleKind.EMBEDDED) {
                setOf(rr)
            } else {
                emptySet()
            }
        }
        parent.hasLooped -> emptySet() // do nothing
        else -> {
            val result = mutableSetOf<RuntimeRule>()
            when {
                parent.rulePosition.isAtStart && parent.rulePosition.runtimeRule.kind != RuntimeRuleKind.GOAL -> {
                    // no need to cache for SOR positions, but need to traverse
                    val runtimeRule = parent.rulePosition.item ?: error("should never be null as position != EOR")
                    for (rp in runtimeRule.rulePositions) {
                        when {
                            rp.isAtEnd -> {
                                val lhs = parent.lookaheadSet // atEnd, so use parent lookahead
                                val ci = ClosureItemLC1(parent, rp, null, lhs)
                                val dnCls = traverseRulePositions(ci)
                                result.addAll(dnCls)
                            }
                            else -> {
                                val nexts = rp.next()
                                for (next in nexts) {
                                    val lhs = firstOf(next, parent.lookaheadSet)
                                    val ci = ClosureItemLC1(parent, rp, next, lhs)
                                    val dnCls = traverseRulePositions(ci)
                                    result.addAll(dnCls)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // cache and traverse
                    val runtimeRule = parent.rulePosition.item ?: error("should never be null as position != EOR")
                    for (rp in runtimeRule.rulePositions) {
                        val lhs = parent.lookaheadSet
                        val nexts = rp.next()
                        for (next in nexts) {
                            val ci = ClosureItemLC1(parent, rp, next, lhs)
                            val dnCls = traverseRulePositions(ci)
                            when (rp.position) {
                                //                                 RulePosition.START_OF_RULE -> calcAndCacheWidthInfo(listOf(parent.rulePosition), dnCls)
                            }
                            result.addAll(dnCls)
                        }
                    }
                    cacheStateInfo(listOf(parent.rulePosition), parent.prev)
                }
            }
            result
            /*
            val runtimeRule = parent.rulePosition.item!!
            when (runtimeRule.kind) {
                RuntimeRuleKind.GOAL -> error("should never happen")
                RuntimeRuleKind.TERMINAL -> setOf(parent)
                RuntimeRuleKind.EMBEDDED -> setOf(parent)
                RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        // state = merge of all choices atEnd
                        // transitions are H/G for any prev
                        //val allOptions = runtimeRule.rulePositions.filter { it.isAtEnd }
                        // for(pp in items) {
                        //     val key = Pair(listOf(pp.rulePosition),allOptions)
                        //     this._heightOrGraftInto[key] = setOf(HeightGraftInfo(listOf(parent.rulePosition),parentNext,lhs,upLhs))
                        //}
                        //traverse down each option in the choice
                        val resultItems = mutableSetOf<ClosureItemLC1>()
                        for (rp in runtimeRule.rulePositions) {
                            val itemRule = rp.item
                            when {
                                null == itemRule -> null // must be atEnd, can't traverse down
                                else -> {
                                    val next = rp.next().first() // should be only one next
                                    val lhs = parent.lookaheadSet // next will be atEnd, so use parent lookahead
                                    val clsItem = ClosureItemLC1(parent, rp, next, lhs)
                                    val dnCls = traverseRulePositionsForRule(clsItem, upCls + parent)
                                    resultItems.addAll(dnCls)
                                }
                            }
                        }
                        resultItems
                    }
                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        val resultItems = mutableSetOf<ClosureItemLC1>()
                        for (rp in runtimeRule.rulePositions) {
                            // rp becomes a state  TODO: merging
                            // possible Prevs = (prev, ifAtEnd)

                            when (rp.position) {
                                RulePosition.END_OF_RULE -> {
                                    // can't traverse down, but should cache heightGraft
                                    cacheUpClosure(runtimeRule, upCls)
                                    val prev = parent.rulePosition
                                    calcAndCacheHeightOrGraftInto(listOf(prev), listOf(runtimeRule), upCls)
                                }
                                else -> {
                                    val next = rp.next().first() //TODO: what about if next is 'empty'
                                    val nextFstOf = firstOf(next, parent.lookaheadSet.content)
                                    val lhs = this.stateSet.createLookaheadSet(nextFstOf)

                                    //traverse down
                                    val clsItem = ClosureItemLC1(parent, rp, next, lhs)
                                    val dnCls = traverseRulePositionsForRule(clsItem, upCls + parent)
                                    cacheDnClosure(parent.rulePosition, dnCls)
                                    when {
                                        RulePosition.START_OF_RULE == rp.position -> {
                                            resultItems.addAll(dnCls)
                                        }
                                        else -> {
                                            calcAndCacheWidthInfo(listOf(rp), dnCls)
                                        }
                                    }
                                }
                            }
                        }
                        resultItems
                    }
                    RuntimeRuleRhsItemsKind.LIST -> TODO()
                }
            }
            */
        }
    }
    }
    */
/*
    private fun dnClosureLC1(rp: RulePosition): Set<ClosureItemLC1> {
        val upLhs: LookaheadSetPart = LookaheadSetPart.UP
        return when {
            rp.isAtEnd -> emptySet()
            _cacheOff -> {
                val lhs = calcLookaheadDown(rp, upLhs)
                val ci = ClosureItemLC1(null, rp, null, lhs)
                calcDnClosureLC1(ci)//, mutableSetOf(ci))
            }
            else -> {
                _dnClosure[rp] ?: run {
                    val lhs = calcLookaheadDown(rp, upLhs)
                    val ci = ClosureItemLC1(null, rp, null, lhs)
                    val v = calcDnClosureLC1(ci)//, mutableSetOf(ci))
                    _dnClosure[rp] = v
                    v
                }
            }
        }
    }
*/
    /*
    private fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: LookaheadSetPart): LookaheadSetPart {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
            else -> {
                val next = rulePosition.next()
                next.map {
                    expectedAt(it, ifReachEnd)
                }.fold(LookaheadSetPart.EMPTY) { acc, it ->
                    acc.union(it)
                }
            }
        }
    }
*/
    // does not go all the way down to terminals,
// stops just above,
// when item.rulePosition.items is a TERMINAL/EMBEDDED
    private fun calcDnClosureLC1_1(item: ClosureItemLC1, items: MutableSet<ClosureItemLC1> = mutableSetOf()): Set<ClosureItemLC1> {
        return when {
            item.rulePosition.isAtEnd -> items
            //items.any {
            //    it.rulePosition == item.rulePosition &&
            //             it.lookaheadSet == item.lookaheadSet &&
            //             it.parentItem?.lookaheadSet == item.parentItem?.lookaheadSet
            // } -> items
            else -> {
                //items.add(item)
                val rr = item.rulePosition.item
                if (null != rr) {
                    when (rr.kind) {
                        RuntimeRuleKind.TERMINAL,
                        RuntimeRuleKind.EMBEDDED -> Unit
                        RuntimeRuleKind.GOAL,
                        RuntimeRuleKind.NON_TERMINAL -> {
                            val chRps = rr.rulePositionsAt[0]
                            val potentialNewItems = chRps.flatMap { chRp ->
                                val chNext = chRp.next()
                                chNext.map { chNx ->
                                    val lhs = expectedAt(chNx, item.lookaheadSet)
                                    ClosureItemLC1(item, chRp, chNx, lhs)
                                }
                            }
                            val newItems = potentialNewItems.filter {
                                item.rulePosition.isAtEnd.not() &&
                                        items.any { existing -> existing.equivalentOf(it) }.not()
                            }
                            items.addAll(newItems)
                            for (ci in newItems) {
                                calcDnClosureLC1_1(ci, items)
                            }
                        }
                    }
                }
                items
            }
        }
    }

    private fun calcDnClosureLC1(topItem: ClosureItemLC1): Set<ClosureItemLC1> {
        val items = mutableSetOf(topItem)
        val newItemQueue = MutableQueue<ClosureItemLC1>()
        newItemQueue.enqueue(topItem)
        while (newItemQueue.isNotEmpty) {
            val item = newItemQueue.dequeue()
            when {
                item.rulePosition.isAtEnd -> Unit
                else -> {
                    val rr = item.rulePosition.item
                    if (null != rr) {
                        when (rr.kind) {
                            RuntimeRuleKind.TERMINAL, RuntimeRuleKind.EMBEDDED -> Unit
                            RuntimeRuleKind.GOAL, RuntimeRuleKind.NON_TERMINAL -> {
                                val chRps = rr.rulePositionsAt[0]
                                val potentialNewItems = chRps.flatMap { chRp ->
                                    val chNext = chRp.next()
                                    chNext.map { chNx ->
                                        val lhs = expectedAt(chNx, item.lookaheadSet)
                                        ClosureItemLC1(item, chRp, chNx, lhs)
                                    }
                                }
                                val newItems = potentialNewItems.filter {
                                    item.rulePosition.isAtEnd.not() &&
                                            items.any { existing -> existing.equivalentOf(it) }.not()
                                }
                                items.addAll(newItems)
                                for (ci in newItems) {
                                    newItemQueue.enqueue(ci)
                                }
                            }
                        }
                    }
                }
            }
        }
        return items
    }

    private fun pFirstTerm(rp: RulePosition, done: MutableSet<RulePosition> = mutableSetOf()): Set<RuntimeRule> {
        return if (done.contains(rp)) {
            emptySet()
        } else {
            when {
                rp.isTerminal -> setOf(rp.runtimeRule)
                rp.item!!.isTerminal -> setOf(rp.item!!)
                else -> {
                    done.add(rp)
                    val x = rp.item!!.rulePositionsAt[0].flatMap { pFirstTerm(it, done) }.toSet()
                    x
                }
            }
        }
    }

}