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

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
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
            val action: ParseAction,
            val firstOf: LookaheadSetPart,
            val firstOfNext: LookaheadSetPart
        ) {
            val to: List<RulePosition>
                get() = when (this.action) {
                    ParseAction.WIDTH, ParseAction.EMBED -> this.firstOf.fullContent.map { RulePosition(it, 0, RulePosition.END_OF_RULE) }
                    ParseAction.HEIGHT, ParseAction.GRAFT -> this.parent?.rulePosition?.next()?.toList() ?: emptyList()
                    ParseAction.GOAL -> listOf(prev.atEnd())
                }
        }

        class PossibleState(
            val firstOfCache: Map<RulePosition, MutableMap<RulePosition?, LookaheadSetPart>>,
            val rulePosition: RulePosition
        ) {
            val isAtStart: Boolean get() = this.rulePosition.isAtStart
            val isAtEnd: Boolean get() = this.rulePosition.isAtEnd
            val isGoalStart: Boolean get() = this.rulePosition.isGoal && this.isAtStart
            val isGoalEnd: Boolean get() = this.rulePosition.isGoal && this.isAtEnd

            val outTransInfo = mutableMapOf<RulePosition?, MutableSet<PossibleTransInfo>>()

            val mergedTransInfo: Set<TransInfo> by lazy {
                val tis = this.outTransInfo.values.flatten().toSet()
                val tisAtEnd = tis.filter { it.to.first().isAtEnd }
                //TODO: need to split things at end!
                val mergedAtEnd = tisAtEnd.map {
                    val lhs = when (it.action) {
                        ParseAction.GOAL -> LookaheadSetPart.EOT
                        ParseAction.WIDTH, ParseAction.EMBED -> {
                            it.to.map { tgt ->
                                if (tgt.rule == RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD) {
                                    LookaheadSetPart.EMPTY
                                } else {
                                    this.firstOfCache[tgt]!![this.rulePosition]!!
                                }
                            }.reduce { acc, l -> acc.union(l) }
                        }

                        ParseAction.HEIGHT, ParseAction.GRAFT -> it.firstOfNext
                    }
                    TransInfo(setOf(setOf(it.prev)), it.action, it.to.toSet(), setOf(LookaheadInfoPart(lhs, LookaheadSetPart.EMPTY)))
                }
                val tisNotAtEnd = tis - tisAtEnd
                val groupNotAtEnd = tisNotAtEnd.groupBy { Pair(it.action, it.to) }
                val mergedNotAtEnd = groupNotAtEnd.map { me ->
                    val prev = me.value.map { it.prev.let { setOf(it) } }.toSet()
                    val parentFirstOfNext = me.value.map { tr ->
                        //tr.parent?.rulePosition?.next()?.map { this.firstOfNext(tr.prev) } ?: emptyList()
                        tr.parentFirstOfNext
                    }
                    val action = me.key.first
                    val to = me.key.second
                    val lookaheadSet = when (action) {
                        ParseAction.GOAL -> LookaheadSetPart.EOT
                        ParseAction.WIDTH, ParseAction.EMBED -> me.value.map { tr ->
                            val p = when {
                                this.isAtStart -> tr.prev ?: this.rulePosition
                                else -> this.rulePosition
                            }
                            this.firstOfCache[tr.to.first()]!!.get(this.rulePosition)!!
                        }.reduce { acc, it -> acc.union(it) }

                        ParseAction.HEIGHT, ParseAction.GRAFT -> me.value.map { tr -> tr.firstOfNext }.reduce { acc, it -> acc.union(it) }
                    }
                    val lhInfo = parentFirstOfNext.map { LookaheadInfoPart(lookaheadSet, it) }.toSet()
                    TransInfo(prev, action, to.toSet(), lhInfo)
                }.toSet()
                mergedNotAtEnd + mergedAtEnd
            }

            val allPrev get() = outTransInfo.values.flatMap { it.map { it.prev } }.toSet().toList()

            fun setTransInfo(prev: RulePosition, parent: PossibleState?, parentFirstOfNext: LookaheadSetPart, firstOf: LookaheadSetPart, firstOfNext: LookaheadSetPart) {
                val action = when {
                    rulePosition.isGoal -> when {
                        rulePosition.isAtEnd -> ParseAction.GOAL    // RP(G,0,EOR)
                        rulePosition.isAtStart -> ParseAction.WIDTH // RP(G,0,SOR)
                        else -> error("should not happen")
                    }

                    this.rulePosition.isAtEnd -> when {
                        null == parent -> error("should not happen")
                        parent.isGoalStart -> ParseAction.GRAFT
                        parent.isAtStart -> ParseAction.HEIGHT
                        parent.isAtEnd -> error("should not happen")
                        else -> ParseAction.GRAFT
                    }

                    else -> ParseAction.WIDTH
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
                    val action = me.key.first
                    val to = me.key.second
                    val lhInfo = me.value.flatMap { it.lookahead }.toSet()
                    TransInfo(prev, action, to, lhInfo)
                }.toSet()
                return mergedTis
            }

        val RulePosition.canMergeState: Boolean get() = this.isAtEnd.not()// && this.next().all { it.isAtEnd.not() }
        val RulePosition.cannotMergeState: Boolean get() = this.isAtEnd //|| this.next().any { it.isAtEnd }

    }

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItemLC1>>()


    private val _mergedStates = mutableMapOf<RulePosition,StateInfo>()
    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableMap<Set<RulePosition>, TransInfo>>()

    private val _allRulePositionsForStates: List<RulePosition>
        get() = this.stateSet.usedNonTerminalRules
            .flatMap { it.rulePositionsNotAtStart } +
                this.stateSet.usedTerminalRules.map { it.asTerminalRulePosition }

    init {
        this._mergedStates[stateSet.startRulePosition] = StateInfo(setOf(stateSet.startRulePosition))
    }

    override fun clearAndOff() {
        //TODO
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }



    override fun mergedStateInfoFor(rulePositions: List<RulePosition>): StateInfo {
        val sis = rulePositions.mapNotNull { _mergedStates[it] }.toSet()
        if (Debug.CHECK) check(1==sis.size)
        return sis.first()
    }

    override fun stateInfo(): Set<StateInfo> = this.stateInfo4()

    /*
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
                    val to = mergedPossibleStates.keys.firstOrNull { it.containsAll(tr.to) }?.toSet() ?: tr.to
                    TransInfo(prev, tr.action, to, tr.lookahead)
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
                        val to = mgdPossibleStates.keys.firstOrNull { it.containsAll(tr.to) }?.toSet() ?: tr.to
                        TransInfo(prev, tr.action, to, tr.lookahead)
                    }.toSet()
                    StateInfo(rulePositions, possibleTrans)
                }
                originalNumStates = mergedNum
                mergedNum = updMergedStates.size
            }

            return updMergedStates.toSet()//mergedStateInfo
        }
    */
    internal fun stateInfo2(): Set<StateInfo> {
        data class L1Trans(
            val parent: RulePosition?,
            val action: ParseAction,
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
                        else -> stateSet.firstOf.expectedAt(nx, followAtEnd)
                    }
                }.reduce { acc, l -> acc.union(l) }
            }

            fun outTransitions(parentOf: Map<Pair<RulePosition, RuntimeRule>, Set<L1State>>): Set<L1Trans> {
                return when {
                    null == parent && rulePosition.isAtEnd -> emptySet() // G,0,EOR
                    rulePosition.isAtEnd -> {
                        val action = when {
                            parent!!.rulePosition.isGoal -> ParseAction.GOAL
                            parent!!.rulePosition.isAtStart -> ParseAction.HEIGHT
                            else -> ParseAction.GRAFT
                        }
                        val targets = parent!!.rulePosition.next()
                        val to = targets.map { tgt ->
                            // expectedAt(tgt) - follow(parent) if atEnd
                            val grd = stateSet.firstOf.expectedAt(tgt, parentFollowAtEnd)
                            val up = when (action) {
                                ParseAction.HEIGHT -> parentFollowAtEnd
                                ParseAction.EMBED,
                                ParseAction.WIDTH,
                                ParseAction.GRAFT,
                                ParseAction.GOAL -> LookaheadSetPart.EMPTY
                            }
                            Triple(tgt, grd, up)
                        }
                        to.map { p -> L1Trans(parent!!.rulePosition, action, p.first, p.second, p.third) }.toSet()
                    }

                    else -> {
                        val action = ParseAction.WIDTH
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
            val action: ParseAction,
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
                    val rps = rp.items.flatMap { it.rulePositions }
                    for (childRP in rps) {
                        val childLhs = state.expectedAt
                        val childState = L1State(state, childRP, childLhs)
                        if (l1States.contains(childState).not()) {
                            l1States.add(childState)
                            val pr = childState.prev
                            parentOf[Pair(pr, childRP.rule)].add(state)
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
            val rp = st.rulePosition//.toList()
            val trans = st.outTransitions.map { tr ->
                val prev = tr.prev.map { pr -> merged.first { it.rulePosition.containsAll(pr) }.rulePosition }.toSet()
                val action = tr.action
                val to = merged.first { it.rulePosition.containsAll(tr.to) }
                val lh = tr.lookahead.map { LookaheadInfoPart(it.guard, it.up) }.toSet()
                TransInfo(prev, action, to.rulePosition, lh)
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
            StateInfo(rp).also { it.possibleTrans = it.possibleTrans + trans }
        }.toSet()
        return stateInfo
    }

    private fun stateInfo3(): Set<StateInfo> {
        val stateRps = this.stateSet.usedNonTerminalRules.flatMap { it.rulePositionsNotAtStart } +
                this.stateSet.usedTerminalRules.map { it.asTerminalRulePosition }
        val rulePosition = this.stateSet.startRulePosition
        val context = rulePosition
        val parentNextFollow = LookaheadSetPart.EOT
        val parentParentNextFollow = LookaheadSetPart.EOT
        firstFollowCache.processAllClosures(context, rulePosition, parentNextFollow)
        val stateInfo = mutableMapOf<RulePosition, StateInfo>()
        val transInfoBySrc = mutableMapOf<RulePosition, Set<TransInfo>>()
        for (srcRp in stateRps) {
            var transInfo = emptySet<TransInfo>()
            for (ctx in firstFollowCache.possibleContextsFor(srcRp)) {
                val tis = when {
                    srcRp.isAtEnd -> {
                        val hgtis = mutableSetOf<TransInfo>()
                        for (ctxCtx in firstFollowCache.possibleContextsFor(ctx)) {
                            val pns = this.firstFollowCache.parentInContext(ctxCtx, ctx, srcRp.rule)
                            val hgInfo = pns.map { parentNext ->
                                val action = when {
                                    parentNext.rulePosition.isGoal -> ParseAction.GOAL
                                    parentNext.firstPosition -> ParseAction.HEIGHT
                                    else -> ParseAction.GRAFT
                                }
                                val tgt = parentNext.rulePosition
                                val follow = parentNext.expectedAt
                                val grd = follow
                                val up = when (action) {
                                    ParseAction.HEIGHT -> parentNext.parentExpectedAt
                                    ParseAction.EMBED,
                                    ParseAction.WIDTH,
                                    ParseAction.GRAFT,
                                    ParseAction.GOAL -> LookaheadSetPart.EMPTY
                                }
                                TransInfo(setOf(setOf(ctx)), action, setOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                                //HeightGraftInfo(action, listOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                            }.toSet()
                            val merged = mergeTransInfo(transInfo, hgInfo)
                            hgtis.addAll(merged)
                        }
                        transInfo = mergeTransInfo(transInfo, hgtis)
                    }

                    else -> {
                        val parentFollow = when {
                            srcRp.isGoal -> LookaheadSetPart.EOT
                            else -> {
                                val ctxCtxs = this.firstFollowCache.possibleContextsFor(ctx)
                                val x = ctxCtxs.flatMap { cc ->
                                    this.firstFollowCache.parentInContext(cc, ctx, srcRp.rule)
                                }.toSet()
                                x.map { it.expectedAt }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                            }
                        }
                        val ftis = this.firstFollowCache.firstTerminalInContext(ctx, srcRp, parentFollow) //emptySet(),  parentFollow)
                        val wis = ftis.map { firstTermInfo ->
                            val action = when {
                                firstTermInfo.terminalRule.isEmbedded -> ParseAction.EMBED
                                else -> ParseAction.WIDTH
                            }
                            val trp = firstTermInfo.terminalRule.asTerminalRulePosition
                            val lhs = firstTermInfo.parentExpectedAt
                            TransInfo(setOf(setOf(ctx)), action, setOf(trp), setOf(LookaheadInfoPart(lhs, LookaheadSetPart.EMPTY)))
                            //WidthInfo(action, trp, lhs)
                        }
                        transInfo = mergeTransInfo(transInfo, wis.toSet())
                    }
                }
            }
            val mergedTrans = mergeTransInfo(transInfoBySrc[srcRp], transInfo)
            transInfoBySrc[srcRp] = mergedTrans

            //transInfoBySrc[srcRp].addAll(transInfo)
            transInfo.forEach { ti ->
                val si = StateInfo(ti.to)
                ti.to.forEach {
                    val existing = stateInfo[it]
                    when {
                        (null == existing) -> stateInfo[it] = si
                        existing.rulePositions == si.rulePositions -> Unit //OK same state
                        else -> {
                            val mergedState = StateInfo(existing.rulePositions + si.rulePositions)
                            mergedState.possibleTrans = mergeTransInfo(existing.possibleTrans, si.possibleTrans)
                            mergedState.rulePositions.forEach {
                                stateInfo[it] = mergedState
                            }
                        }
                    }
                }
            }
        }
        stateInfo[rulePosition] = StateInfo(setOf(rulePosition))
        for ((srcRp, outTrans) in transInfoBySrc) {
            stateInfo[srcRp]!!.possibleTrans = outTrans
        }

        val transInfoBySrc2 = mutableMapOf<RulePosition, Set<TransInfo>>()
        val stateInfo2 = mutableMapOf<RulePosition, StateInfo>()
        for ((srcRp, outTrans) in transInfoBySrc) {
            val srcStateInfo = stateInfo[srcRp]!!
            val mergedTrans = mergeTransInfo(srcStateInfo.possibleTrans, outTrans)
            transInfoBySrc2[srcRp] = mergedTrans
            srcStateInfo.possibleTrans = mergedTrans
            mergedTrans.forEach { ti ->
                val si = StateInfo(ti.to)
                ti.to.forEach {
                    val existing = stateInfo2[it]
                    when {
                        (null == existing) -> stateInfo2[it] = si
                        existing.rulePositions == si.rulePositions -> Unit //OK same state
                        else -> {
                            val mergedState = StateInfo(existing.rulePositions + si.rulePositions)
                            mergedState.rulePositions.forEach {
                                stateInfo2[it] = mergedState
                            }
                        }
                    }
                }
            }
        }

        stateInfo2[rulePosition] = StateInfo(setOf(rulePosition))
        for ((srcRp, outTrans) in transInfoBySrc) {
            stateInfo2[srcRp]!!.possibleTrans = outTrans
        }
        return stateInfo2.values.toSet()
    }

    private fun stateInfo4(): Set<StateInfo> {
        val startRulePosition = this.stateSet.startRulePosition
        val context = startRulePosition
        val parentNextFollow = LookaheadSetPart.EOT
        firstFollowCache.processAllClosures(context, startRulePosition, parentNextFollow)

        val mergedStateInfos = calcStateInfos()
        for (stateInfo in mergedStateInfos) {
            val transInfo = mutableSetOf<TransInfo>()
            for (srcRp in stateInfo.rulePositions) {
                this._mergedStates[srcRp] = stateInfo
                val ti = when {
                    srcRp.isAtEnd -> calcTransInfoForComplete(srcRp)
                    else -> calcTransInfoForIncomplete(srcRp)
                }
                transInfo.addAll(ti)
            }
            val merged = mergeTransInfo(transInfo, emptySet())
            stateInfo.possibleTrans = merged
        }
        //val mergedStatesByTransition = mergeStatesByTransition(mergedStateInfos)
        return mergedStateInfos
    }

    private fun mergeStates(rulePositions: Iterable<RulePosition>): Set<StateInfo> {
        val stateRpBefore = rulePositions.flatMap { s ->  s.next().map { n -> Pair(n,s) } }.toSet()
        val before = lazyMutableMapNonNull<RulePosition,MutableSet<RulePosition>> { mutableSetOf<RulePosition>() }
        stateRpBefore.forEach { before[it.first].add(it.second) }

        val stateRpsCanMerge = rulePositions.filter { it.canMergeState }
        val stateRpsNotToMerge = rulePositions.filter { it.cannotMergeState }
        val groupedStateRpsCanMerge = stateRpsCanMerge.groupBy { srcRp ->
            Pair(before[srcRp].flatMap { it.items }, srcRp.items)
        }
        val mergedSateInfoCanMerge = groupedStateRpsCanMerge.flatMap { me ->
            when {
                //me.key.second.isEmpty() -> me.value.map { StateInfo(setOf(it)) }
                else -> listOf(StateInfo(me.value.toSet()))
            }
        }
        val stateInfoNotMerged = stateRpsNotToMerge.map { StateInfo(setOf(it)) }
        return mergedSateInfoCanMerge.toSet() + stateInfoNotMerged.toSet()
    }

    private fun calcStateInfos(): Set<StateInfo> {
        val stateRps = this._allRulePositionsForStates
        val merged = mergeStates(stateRps)
        return merged
    }

    private fun calcTransInfoForComplete(srcRp: RulePosition): Set<TransInfo> {
        if (Debug.CHECK) check(srcRp.isAtEnd)
        val transInfo = mutableSetOf<TransInfo>()
        val contexts = firstFollowCache.possibleContextsFor(srcRp)
        for (ctx in contexts) {
            val hgtis = mutableSetOf<TransInfo>()
            val contextContexts = firstFollowCache.possibleContextsFor(ctx)
            for (ctxCtx in contextContexts) {
                val pns = this.firstFollowCache.parentInContext(ctxCtx, ctx, srcRp.rule)
                val hgInfo = pns.map { parentNext ->
                    val action = when {
                        parentNext.rulePosition.isGoal -> ParseAction.GOAL
                        parentNext.firstPosition -> ParseAction.HEIGHT
                        else -> ParseAction.GRAFT
                    }
                    val tgt = parentNext.rulePosition
                    val follow = parentNext.expectedAt
                    val grd = follow
                    val up = when (action) {
                        ParseAction.HEIGHT -> parentNext.parentExpectedAt
                        ParseAction.EMBED,
                        ParseAction.WIDTH,
                        ParseAction.GRAFT,
                        ParseAction.GOAL -> LookaheadSetPart.EMPTY
                    }
                    TransInfo(setOf(setOf(ctx)), action, setOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                    //HeightGraftInfo(action, listOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                }.toSet()
                hgtis.addAll(hgInfo)
            }
            transInfo.addAll(hgtis)
        }
        val mergedTransInfo = mergeTransInfo(transInfo, emptySet())
        return mergedTransInfo
    }

    private fun calcTransInfoForIncomplete(srcRp: RulePosition): Set<TransInfo> {
        if (Debug.CHECK) check(srcRp.isAtEnd.not())
        val transInfo = mutableSetOf<TransInfo>()
        val contexts = firstFollowCache.possibleContextsFor(srcRp)
        for (ctx in contexts) {
            val parentFollow = when {
                srcRp.isGoal -> LookaheadSetPart.EOT
                else -> {
                    val ctxCtxs = this.firstFollowCache.possibleContextsFor(ctx)
                    val x = ctxCtxs.flatMap { cc ->
                        this.firstFollowCache.parentInContext(cc, ctx, srcRp.rule)
                    }.toSet()
                    x.map { it.expectedAt }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                }
            }
            val ftis = this.firstFollowCache.firstTerminalInContext(ctx, srcRp, parentFollow) //emptySet(),  parentFollow)
            val wis = ftis.map { firstTermInfo ->
                val action = when {
                    firstTermInfo.terminalRule.isEmbedded -> ParseAction.EMBED
                    else -> ParseAction.WIDTH
                }
                val trp = firstTermInfo.terminalRule.asTerminalRulePosition
                val lhs = firstTermInfo.parentExpectedAt
                TransInfo(setOf(setOf(ctx)), action, setOf(trp), setOf(LookaheadInfoPart(lhs, LookaheadSetPart.EMPTY)))
            }
            transInfo.addAll(wis.toSet())
        }
        val mergedTransInfo = mergeTransInfo(transInfo, emptySet())
        return mergedTransInfo
    }

    private fun mergeStatesByTransition(states: Set<StateInfo>): Set<StateInfo> {
        val statesByRp = mutableMapOf<RulePosition, StateInfo>()
        for (si in states) {
            for (rp in si.rulePositions) {
                statesByRp[rp] = si
            }
        }
        for (si in states) {
            when {
                si.rulePositions.first().isAtEnd -> statesByRp[si.rulePositions.first()] = si
                si.possibleTrans.isEmpty() -> statesByRp[si.rulePositions.first()] = si
                else -> {
                    for (ti in si.possibleTrans) {
                        for (rp in ti.to) {
                            val existing = statesByRp[rp]
                            when {
                                (null == existing) -> error("??")
                                existing.rulePositions == si.rulePositions -> Unit //OK same state
                                else -> {
                                    val mergedState = StateInfo(existing.rulePositions + si.rulePositions)
                                    mergedState.rulePositions.forEach {
                                        statesByRp[it] = mergedState
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return statesByRp.values.toSet()
    }

    private fun mergeTransInfo(ti1: Set<TransInfo>?, ti2: Set<TransInfo>): Set<TransInfo> {
        val total = if (null == ti1) ti2 else ti1 + ti2
        val embed = mutableSetOf<TransInfo>()
        val width = mutableSetOf<TransInfo>()
        val heightComplete = mutableSetOf<TransInfo>()
        val graftComplete = mutableSetOf<TransInfo>()
        val heightIncomplete = mutableSetOf<TransInfo>()
        val graftIncomplete = mutableSetOf<TransInfo>()
        val goals = mutableSetOf<TransInfo>()

        for (ti in total) {
            when (ti.action) {
                ParseAction.GOAL -> goals.add(ti)
                ParseAction.EMBED -> embed.add(ti)
                ParseAction.WIDTH -> width.add(ti)
                ParseAction.GRAFT -> when {
                    ti.to.first().isAtEnd -> graftComplete.add(ti)
                    else -> graftIncomplete.add(ti)
                }

                ParseAction.HEIGHT -> when {
                    ti.to.first().isAtEnd -> heightComplete.add(ti)
                    else -> heightIncomplete.add(ti)
                }

            }
        }

        val mw = mergeWidth(width)
        val mhc = mergeHeightComplete(heightComplete)
        val mhi = mergeHeightIncomplete(heightIncomplete)
        val mgc = mergeGraftComplete(graftComplete)
        val mgi = mergeGraftIncomplete(graftIncomplete)
        val res = mw + mhc + mhi + mgc + mgi + goals + embed
        return res
    }

    private fun mergeWidth(transInfo: Set<TransInfo>): Set<TransInfo> {
        return when {
            transInfo.isEmpty() -> transInfo
            else -> {
                val grouped = transInfo.groupBy { it.to }
                val merged = grouped.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val action = ParseAction.WIDTH
                    val to = me.value.flatMap { it.to }.toSet()
                    val grd = me.value.flatMap { it.lookahead.map { it.guard } }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                    val lhs = setOf(LookaheadInfoPart(grd, LookaheadSetPart.EMPTY))
                    TransInfo(prev, action, to, lhs)
                }.toSet()
                merged
            }
        }
    }

    private fun mergeHeightComplete(transInfo: Set<TransInfo>): Set<TransInfo> {
        return when {
            transInfo.isEmpty() -> transInfo
            else -> {
                val grouped = transInfo.groupBy { it.to }
                val merged = grouped.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val action = ParseAction.HEIGHT
                    val to = me.key
                    val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lookahead }.toSet())
                    TransInfo(prev, action, to, lhs)
                }.toSet()
                merged
            }
        }
    }

    private fun mergeHeightIncomplete(transInfo: Set<TransInfo>): Set<TransInfo> {
        return when {
            transInfo.isEmpty() -> transInfo
            else -> {
                //val grouped = transInfo.groupBy { Pair(it.to, it.lookahead) }
                val grouped = transInfo.groupBy { it.to }
                val merged = grouped.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val action = ParseAction.HEIGHT
                    val to = me.key
                    //val lhs = me.key.second
                    val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lookahead }.toSet())
                    TransInfo(prev, action, to, lhs)
                }.toSet()
                merged
            }
        }
    }

    private fun mergeGraftComplete(transInfo: Set<TransInfo>): Set<TransInfo> {
        return when {
            transInfo.isEmpty() -> transInfo
            else -> {
                val grouped = transInfo.groupBy { it.to }
                val merged = grouped.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val action = ParseAction.GRAFT
                    val to = me.key
                    val grd = me.value.flatMap { it.lookahead.map { it.guard } }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                    val lhs = setOf(LookaheadInfoPart(grd, LookaheadSetPart.EMPTY))
                    TransInfo(prev, action, to, lhs)
                }.toSet()
                merged
            }
        }
    }

    private fun mergeGraftIncomplete(transInfo: Set<TransInfo>): Set<TransInfo> {
        return when {
            transInfo.isEmpty() -> transInfo
            else -> {
                val grouped = transInfo.groupBy { it.to }
                val merged = grouped.map { me ->
                    val prev = me.value.flatMap { it.prev }.toSet()
                    val action = ParseAction.GRAFT
                    val to = me.key
                    val grd = me.value.flatMap { it.lookahead.map { it.guard } }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                    val lhs = setOf(LookaheadInfoPart(grd, LookaheadSetPart.EMPTY))
                    TransInfo(prev, action, to, lhs)
                }.toSet()
                merged
            }
        }
    }

    override fun widthInto(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        // the 'to' state is the first Terminal the fromState.rulePosition
        // if there are multiple fromState.rulePositions then they should have same firstOf or they would not be merged.
        // after a WIDTH, fromState becomes the prevState, therefore
        // the lookahead is the firstOf the parent.next of the 'to' state, in the context of the fromStateRulePositions
        if (Debug.OUTPUT_SM_BUILD) Debug.debug(Debug.IndentDelta.INC_AFTER) { "START calcWidthInfo($prevState, $fromState) - ${fromState.rulePositions.map { it.rule.tag }}" }
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
            val action = when {
                firstTermInfo.terminalRule.isEmbedded -> ParseAction.EMBED
                else -> ParseAction.WIDTH
            }
            val rp = firstTermInfo.terminalRule.asTerminalRulePosition
            val lhs = firstTermInfo.parentExpectedAt
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
        wisMerged.forEach {wi->
            this._mergedStates[wi.to] = StateInfo(setOf(wi.to))
        }
        return wisMerged.toSet()
    }

    override fun heightOrGraftInto(prevPrev: ParserState, prevState: ParserState, fromState: ParserState): Set<TransInfo> {
        val calc = calcAndCacheHeightOrGraftInto(prevPrev, prevState, fromState)//, upCls)
        return calc
    }

    private fun calcAndCacheHeightOrGraftInto(
        prevPrev: ParserState,
        prev: ParserState,
        from: ParserState
    ): Set<TransInfo> {//, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        val hgi = calcHeightOrGraftInto(prevPrev, prev, from)//, upCls)
        cacheHeightOrGraftInto(prev, from, hgi)
        return hgi
    }

    private fun cacheHeightOrGraftInto(prev: ParserState, from: ParserState, hgis: Set<TransInfo>) {
        val key = Pair(prev.rulePositions, from.runtimeRules)
        val map = this._heightOrGraftInto[key] ?: run {
            val x = mutableMapOf<Set<RulePosition>, TransInfo>()
            this._heightOrGraftInto[key] = x
            x
        }
        for (hg in hgis) {
            val existing = map[hg.to]
            if (null == existing) {
                map[hg.to] = hg
            } else {
                val lhs = hg.lookahead.union(existing.lookahead)
                map[hg.to] = TransInfo(existing.prev, hg.action, hg.to, lhs)
            }
        }
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(prevPrev: ParserState, prev: ParserState, from: ParserState): Set<TransInfo> {//, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        //FirstFollow3
        val rps = mutableSetOf<RulePosition>()
        val hgInfo = prevPrev.rulePositions.flatMap { contextContext ->
            prev.rulePositions.flatMap { context ->
                val parentsOfFrom = from.runtimeRules.flatMap { fr ->
                    this.firstFollowCache.parentInContext(contextContext, context, fr)
                }.toSet()
                parentsOfFrom.map { parentNext ->
                    val action = when {
                        parentNext.rulePosition.isGoal -> ParseAction.GOAL
                        parentNext.firstPosition -> ParseAction.HEIGHT
                        else -> ParseAction.GRAFT
                    }
                    val tgt = parentNext.rulePosition
                    val grd = parentNext.expectedAt
                    val up = parentNext.parentExpectedAt
                    rps.add(tgt)
                    TransInfo(setOf(setOf(context)), action, setOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                    //HeightGraftInfo(action, listOf(tgt), setOf(LookaheadInfoPart(grd, up)))
                }
            }
        }.toSet()
        val merged = mergeTransInfo(hgInfo, emptySet())

        //val rpsNotMerge = rps.filter { it.cannotMergeState }
        val mergedSateInfoNotAtEnd = mergeStates(rps)
        mergedSateInfoNotAtEnd.forEach { si ->
            si.rulePositions.forEach {
                this._mergedStates[it] = si
            }
        }
        //rpsNotMerge.forEach { this._mergedStates[it] = StateInfo(setOf(it)) }
        return merged
    }

    private fun mergeHeightGraft(hgInfo: Set<HeightGraftInfo>) = mergeHeightGraft1(hgInfo)

    private fun mergeHeightGraft1(hgInfo: Set<HeightGraftInfo>): Set<HeightGraftInfo> {
        val infoAtEnd = hgInfo.filter { it.parentNext.first().isAtEnd }
        val infoNotAtEnd = hgInfo.filter { it.parentNext.first().isAtEnd.not() }
        val infoAtEndMerged = infoAtEnd.groupBy { Pair(it.action, it.parentNext) }
            .map { me ->
                val action = me.key.first
                val parentNext = me.key.second
                //val lhs = me.value.map { it.lhs }.toSet().reduce { acc, e -> acc.union(e) }
                val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lhs }.toSet())
                //val upLhs = me.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
                HeightGraftInfo(action, parentNext, lhs)
            }
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

    private fun mergeHeightGraft2(hgInfo: Set<HeightGraftInfo>): Set<HeightGraftInfo> {
        val infoAtEnd = hgInfo.filter { it.parentNext.first().isAtEnd }
        val infoNotAtEnd = hgInfo.filter { it.parentNext.first().isAtEnd.not() }
        val infoAtEndMerged = infoAtEnd.groupBy { Pair(it.action, it.parentNext) }
            .map { me ->
                val action = me.key.first
                val parentNext = me.key.second
                //val lhs = me.value.map { it.lhs }.toSet().reduce { acc, e -> acc.union(e) }
                val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lhs }.toSet())
                //val upLhs = me.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
                HeightGraftInfo(action, parentNext, lhs)
            }
        val infoNotAtEndHeight = infoNotAtEnd.filter { it.action == ParseAction.HEIGHT }
        val infoNotAtEndGraft = infoNotAtEnd.filter { it.action == ParseAction.GRAFT }
        val infoNotAtEndHeightGrouped = infoNotAtEndHeight.groupBy { it.parentNext }
        val infoNotAtEndGraftGrouped = infoNotAtEndGraft.groupBy { it.lhs }
        val mergedHeightInfo = infoNotAtEndHeightGrouped.map { me ->
            val action = ParseAction.HEIGHT
            val parentNext = me.key
            val lhs = LookaheadInfoPart.merge(me.value.flatMap { it.lhs }.toSet())
            HeightGraftInfo(action, parentNext, lhs)
        }.toSet()
        val mergedGraftInfo = infoNotAtEndGraftGrouped.map { me ->
            val action = ParseAction.GRAFT
            val grd = me.value.flatMap { it.lhs.map { it.guard } }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val lhs = setOf(LookaheadInfoPart(grd, LookaheadSetPart.EMPTY))
            val parentNext = me.value.flatMap { it.parentNext }.toSet().toList()
            HeightGraftInfo(action, parentNext, lhs)
        }.toSet()
        val r = (infoAtEndMerged + mergedHeightInfo + mergedGraftInfo).toSet()
        return r
    }

    private fun pFirstTerm(rp: RulePosition, done: MutableSet<RulePosition> = mutableSetOf()): Set<RuntimeRule> {
        return if (done.contains(rp)) {
            emptySet()
        } else {
            when {
                rp.isTerminal -> setOf(rp.rule as RuntimeRule)
                //rp.item!!.isTerminal -> rp.items
                else -> {
                    done.add(rp)
                    rp.items.flatMap { it.rulePositionsAtStart.flatMap { pFirstTerm(it, done) } }.toSet()
                    //rp.item!!.rulePositionsAt[0].flatMap { pFirstTerm(it, done) }.toSet()
                }
            }
        }
    }

}