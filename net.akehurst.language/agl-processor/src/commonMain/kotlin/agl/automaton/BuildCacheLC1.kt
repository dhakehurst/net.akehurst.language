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

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.collections.MutableQueue
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf

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

            /*
            val prev: List<RulePosition> by lazy {
                if (null == this.parentItem) {
                    emptyList<RulePosition>()
                } else {
                    var x: ClosureItemLC1 = this.parentItem
                    while (x.rulePosition.isAtStart && x.rulePosition.runtimeRule.kind != RuntimeRuleKind.GOAL) {
                        x = x.parentItem!!
                    }
                    listOf(x.rulePosition)
                }
            }
        */
            //val hasLooped: Boolean get() = allPrev.any { this.equivalentOf(it) }

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
            val action: Transition.ParseAction,
            val firstOf: LookaheadSetPart,
            val firstOfNext: LookaheadSetPart
        ) {
            val to: List<RulePosition>
                get() = when (this.action) {
                    Transition.ParseAction.WIDTH, Transition.ParseAction.EMBED -> this.firstOf.fullContent.map { RulePosition(it, 0, RulePosition.END_OF_RULE) }
                    Transition.ParseAction.HEIGHT, Transition.ParseAction.GRAFT -> this.parent?.rulePosition?.next()?.toList() ?: emptyList()
                    Transition.ParseAction.GOAL -> emptyList()
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
                val groupedTis = tis.groupBy { Pair(it.action, it.to) }
                val mergedTis = groupedTis.map { me ->
                    val prev = me.value.map { it.prev.let { listOf(it) } ?: emptyList() }.toSet().toList()
                    val parent = me.value.mapNotNull { it.parent?.rulePosition }.toSet().toList()
                    //val parentFirstOfNext = me.value.map { it.parentFirstOfNext }
                    val parentFirstOfNext = me.value.flatMap { tr -> tr.parent?.rulePosition?.next()?.map { this.firstOfNext(tr.prev) } ?: emptyList() }
                    val action = me.key.first
                    val to = me.key.second
                    val lookaheadSet = when (action) {
                        Transition.ParseAction.GOAL -> LookaheadSetPart.UP
                        Transition.ParseAction.WIDTH, Transition.ParseAction.EMBED -> me.value.map { tr ->
                            val p = when {
                                this.isAtStart -> tr.prev ?: this.rulePosition
                                else -> this.rulePosition
                            }
                            this.firstOfCache[tr.to.first()]!!.get(this.rulePosition)!!
                        }.reduce { acc, it -> acc.union(it) }
                        Transition.ParseAction.HEIGHT, Transition.ParseAction.GRAFT -> me.value.map { tr -> tr.firstOfNext }.reduce { acc, it -> acc.union(it) }
                    }
                    TransInfo(prev, parent, parentFirstOfNext, action, to, lookaheadSet)
                }.toSet()
                mergedTis
            }

            val allPrev get() = outTransInfo.values.flatMap { it.map { it.prev } }.toSet().toList()

            fun setTransInfo(prev: RulePosition, parent: PossibleState?, firstOf: LookaheadSetPart, firstOfNext: LookaheadSetPart) {
                val action = when {
                    rulePosition.runtimeRule.isGoal -> when {
                        rulePosition.isAtEnd -> Transition.ParseAction.GOAL    // RP(G,0,EOR)
                        rulePosition.isAtStart -> Transition.ParseAction.WIDTH // RP(G,0,SOR)
                        else -> error("should not happen")
                    }
                    this.rulePosition.isAtEnd -> when {
                        null == parent -> error("should not happen")
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
                set.add(PossibleTransInfo(prev, parent, action, firstOf, firstOfNext))
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
                    val prev = me.value.flatMap { it.prev }.toSet().toList()
                    val parent = me.value.flatMap { it.parent }.toSet().toList()
                    val parentFirstOfNext = me.value.flatMap { it.parentFirstOfNext }.toSet().toList()
                    val action = me.key.first
                    val to = me.key.second
                    val lookaheadSet = me.value.map { it.lookaheadSet }.reduce { acc, it -> acc.union(it) }
                    TransInfo(prev, parent, parentFirstOfNext, action, to, lookaheadSet)
                }.toSet()
                return mergedTis
            }

        val StateInfo.allPrev: List<List<RulePosition>> get() = this.possibleTrans.flatMap { it.prev }.toSet().toList()

    }

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItemLC1>>()
    private val _dnClosure = mutableMapOf<RulePosition, Set<ClosureItemLC1>>()
    private val _stateInfo = mutableMapOf<List<RulePosition>, StateInfo>()

    // from-state-listOf-rule-positions -> mapOf
    //    to-state-terminal-rule -> WidthInfo
    private val _widthInto = mutableMapOf<ParserState, MutableMap<RuntimeRule, WidthInfo>>()

    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableMap<List<RulePosition>, HeightGraftInfo>>()

    override fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> {
        data class RpToDo(
            val state: PossibleState,
            val prev: PossibleState,
            val prevForChildren: PossibleState,
            val stateFirstOfNext: LookaheadSetPart
        )

        val firstOfCache = lazyMutableMapNonNull<RulePosition, MutableMap<RulePosition?, LookaheadSetPart>> { mutableMapOf() }
        val possibleStates = mutableMapOf<RulePosition, PossibleState>()
        val done = mutableSetOf<Pair<PossibleState, PossibleState?>>()

        // Start State [G(0,0,SOR)]
        val rpGstart = this.stateSet.startState.rulePositions.first()
        val stateGstart = PossibleState(firstOfCache, rpGstart)
        possibleStates[rpGstart] = stateGstart
        val firstOfGstart = this.firstOf(rpGstart, LookaheadSetPart.UP)
        firstOfCache[rpGstart][null] = firstOfGstart
        firstOfCache[rpGstart][rpGstart] = firstOfGstart
        stateGstart.setTransInfo(rpGstart, null, firstOfGstart, LookaheadSetPart.UP)

        // End State [G(0,0,EOR)]
        val rpGend = rpGstart.atEnd()
        val stateGend = PossibleState(firstOfCache, rpGend)
        possibleStates[rpGend] = stateGend
        stateGend.setTransInfo(rpGstart, null, LookaheadSetPart.EMPTY, LookaheadSetPart.EMPTY)

        // Other states
        val todo = mutableQueueOf(RpToDo(stateGstart, stateGstart, stateGstart, LookaheadSetPart.UP))
        //TODO speed up! by eliminating calls to firstOf
        while (todo.isNotEmpty) {
            val stateToDo = todo.dequeue()
            val state = stateToDo.state
            val prevForChildren = stateToDo.prevForChildren
            val stateFirstOfNext = stateToDo.stateFirstOfNext
            val rule = stateToDo.state.rulePosition.item
            if (null != rule) {
                val ruleRps = rule.rulePositions
                for (childRp in ruleRps) {
                    val ruleRpState = possibleStates[childRp] ?: PossibleState(firstOfCache, childRp)
                    val childRpPrev = prevForChildren
                    val d = Pair(ruleRpState, childRpPrev)
                    if (done.contains(d).not()) {
                        done.add(d)
                        val childRpPrevForChildren = when {
                            childRp.isAtStart -> childRpPrev
                            else -> ruleRpState
                        }
                        val firstOf = firstOf(childRp, stateFirstOfNext)
                        firstOfCache[childRp][childRpPrev.rulePosition] = firstOf
                        val firstOfNext = when {
                            childRp.isAtEnd -> stateFirstOfNext
                            else -> childRp.next().map { firstOf(it, stateFirstOfNext) }.reduce { acc, it -> acc.union(it) }
                        }
                        if (childRp.isAtStart.not()) {
                            ruleRpState.setTransInfo(childRpPrev.rulePosition, state, firstOf, firstOfNext)
                            possibleStates[childRp] = ruleRpState
                        }
                        todo.enqueue(RpToDo(ruleRpState, childRpPrev, childRpPrevForChildren, firstOfNext))
                    } else {
                        //do not recurse
                    }
                }
            } else {
                //no items for rule
            }
        }

        val mergedStates = mergeStates(possibleStates)

        //return possibleStates.values.map { StateInfo(listOf(it.rulePosition), it.outTransInfo.keys.map { it?.let { listOf(it) } ?: emptyList() }) }.toSet()
        return mergedStates
    }

    private fun calcAction(parent: PossibleState?, state: PossibleState) = when {
        state.isGoalStart -> Transition.ParseAction.WIDTH
        state.isGoalEnd -> Transition.ParseAction.GOAL
        state.isAtEnd -> when {
            null == parent -> error("should not happen")
            parent.isAtStart -> when {
                parent.isGoalStart -> Transition.ParseAction.GRAFT //special case for parent=RP(G,0,SOR)
                else -> Transition.ParseAction.HEIGHT
            }
            else -> Transition.ParseAction.GRAFT
        }
        else -> Transition.ParseAction.WIDTH
    }

    private fun mergeStates(unmerged: Map<RulePosition, PossibleState>): Set<StateInfo> {
        // merge states with transitions to the same (next) state with same action
        val statesAtEnd = unmerged.filter { it.key.isAtEnd || it.key.isGoal }
        val statesAtEndMapped = statesAtEnd.values.map { state ->
            val rulePositions = listOf(state.rulePosition)
            val possibleTrans = state.mergedTransInfo.toList()
            StateInfo(rulePositions, possibleTrans)
        }
        val statesNotAtEnd = unmerged.filter { it.key.isAtEnd.not() && it.key.isGoal.not() }
        val groupedByOutgoing = statesNotAtEnd.values.groupBy { it.mergedTransInfo.map { ti -> Pair(ti.action, ti.to) } }
        val mergedPossibleStates = groupedByOutgoing.map { me ->
            val rulePositions = me.value.map { it.rulePosition }
            val possiblePrev = me.value.flatMap { it.allPrev }.toSet().toList()
            val possibleTrans = me.value.flatMap { it.mergedTransInfo }.toSet().toList()
            StateInfo(rulePositions, possibleTrans)
        }.union(statesAtEndMapped).associateBy { it.rulePositions }
        val updatedMergedStates = mergedPossibleStates.values.map { state ->
            val rulePositions = state.rulePositions
            val possibleTrans = state.possibleTrans.map { tr ->
                val prev = tr.prev.map { p ->
                    when {
                        p.isEmpty() -> p
                        p.any { it.isAtStart } -> p
                        else -> mergedPossibleStates.keys.firstOrNull { it.containsAll(p) } ?: p
                    }
                }
                val parent = when {
                    tr.parent.isEmpty() -> tr.parent
                    tr.parent.any { it.isAtStart } -> tr.parent
                    else -> mergedPossibleStates.keys.firstOrNull { it.containsAll(tr.parent) } ?: tr.parent
                }
                val to = mergedPossibleStates.keys.firstOrNull { it.containsAll(tr.to) } ?: tr.to
                TransInfo(prev, parent, tr.parentFirstOfNext, tr.action, to, tr.lookaheadSet)
            }
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
                val possibleTrans = me.value.flatMap { it.mergedTransInfo }.toSet().toList()
                StateInfo(rulePositions, possibleTrans)
            }.union(atEnd).associateBy { it.rulePositions }
            updMergedStates = mgdPossibleStates.values.map { state ->
                val rulePositions = state.rulePositions
                val possibleTrans = state.possibleTrans.map { tr ->
                    val prev = tr.prev.map { p ->
                        when {
                            p.isEmpty() -> p
                            p.any { it.isAtStart } -> p
                            else -> mgdPossibleStates.keys.firstOrNull { it.containsAll(p) } ?: p
                        }
                    }
                    val parent = when {
                        tr.parent.isEmpty() -> tr.parent
                        tr.parent.any { it.isAtStart } -> tr.parent
                        else -> mgdPossibleStates.keys.firstOrNull { it.containsAll(tr.parent) } ?: tr.parent
                    }
                    val to = mgdPossibleStates.keys.firstOrNull { it.containsAll(tr.to) } ?: tr.to
                    TransInfo(prev, parent, tr.parentFirstOfNext, tr.action, to, tr.lookaheadSet)
                }
                StateInfo(rulePositions, possibleTrans)
            }
            originalNumStates = mergedNum
            mergedNum = updMergedStates.size
        }

        return updMergedStates.toSet()//mergedStateInfo
    }

    override fun widthInto(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        return if (this._cacheOff) {
            val calc = calcWidthInfo(prevState, fromState)
            calc
        } else {
            this._widthInto[fromState]?.values?.toSet() ?: run {
                val calc = calcAndCacheWidthInfo(prevState, fromState)
                calc
            }
        }
    }

    override fun heightGraftInto(prevState: ParserState, fromState: ParserState): Set<HeightGraftInfo> {
        return if (this._cacheOff) {
            // have to ensure somehow that from grows into prev
            // have to do closure down from prev,
            // upCls is the closure down from prev
            val upCls = prevState.rulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
            val calc = calcAndCacheHeightOrGraftInto(prevState.rulePositions, fromState.runtimeRules, upCls)
            calc
        } else {
            val key = Pair(prevState.rulePositions, fromState.runtimeRules)
            this._heightOrGraftInto[key]?.values?.toSet() ?: run {
                val upCls = prevState.rulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
                val calc = calcAndCacheHeightOrGraftInto(prevState.rulePositions, fromState.runtimeRules, upCls)
                calc
            }
        }
    }

    private fun calcAndCacheWidthInfo(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        val wis = calcWidthInfo(prevState, fromState)
        cacheWidthInfo(fromState, wis)
        return wis
    }

    private fun calcWidthInfo(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        // the 'to' state is the first Terminal the fromState.rulePosition
        // if there are multiple fromState.rulePositions then they should have same firstOf or they would not be merged.
        // after a WIDTH, fromState becomes the prevState, therefore
        // the lookahead is the firstOf the parent.next of the 'to' state, in the context of the fromStateRulePositions
        val firstTerminals = this.firstTerminal(prevState,fromState)
        val wis = firstTerminals.map { rr ->
            val upCls = fromState.rulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
            val upFilt = upCls.filter { rr == it.rulePosition.item }
            val lhs = upFilt.map { it.lookaheadSet }.reduce{ acc, it -> acc.union(it) }
            val follow = this.followInContext(prevState, rr).toSet()
            check(lhs.fullContent==follow)
            val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
            WidthInfo(rp, lhs)
        }
        return wis.toSet()
    }

    private fun calcWidthInfo1(fromRulePositions: List<RulePosition>, dnCls: Set<ClosureItemLC1>): Set<WidthInfo> {
        // lookahead comes from down closure
        val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
        val grouped = filt.groupBy { it.rulePosition.item!! }.map {
            val rr = it.key
            val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
            val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lookaheadSet) }
            WidthInfo(rp, lhs)
        }.toSet()
        //don't group them, because we need the info on the lookahead for the runtime calc of next lookaheads
        return grouped
    }

    private fun cacheWidthInfo(fromState: ParserState, wis: Set<WidthInfo>) {
        val map = this._widthInto[fromState] ?: run {
            val x = mutableMapOf<RuntimeRule, WidthInfo>()
            this._widthInto[fromState] = x
            x
        }
        for (wi in wis) {
            val existing = map[wi.to.runtimeRule]
            if (null == existing) {
                map[wi.to.runtimeRule] = wi
            } else {
                val lhs = wi.lookaheadSet.union(existing.lookaheadSet)
                map[wi.to.runtimeRule] = WidthInfo(wi.to, lhs)
            }
        }
    }

    private fun calcAndCacheHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        val hgi = calcHeightOrGraftInto(prev, from, upCls)
        cacheHeightOrGraftInto(prev, from, hgi)
        return hgi
    }

    private fun cacheHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>, hgis: Set<HeightGraftInfo>) {
        val key = Pair(prev, from)
        var map = this._heightOrGraftInto[key] ?: run {
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
                val upLhs = hg.upLhs.union(existing.upLhs)
                //map[hg.parent] = HeightGraftInfo(hg.ancestors, hg.parent, hg.parentNext, lhs, upLhs)
                map[hg.parentNext] = HeightGraftInfo(hg.ancestors, hg.parent, hg.parentNext, lhs, upLhs)
            }
        }
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        val info = prev.flatMap { pr ->
            val pps = from.flatMap { fr ->
                this.firstFollowCache.parentInContext(pr, fr)
            }
            pps.flatMap { pp ->
                pp.next().map { ppn ->
                    val firstOf = this.firstFollowCache.firstOfInContext(pr, ppn)
                    val lhs = LookaheadSet.createFromRuntimeRules(this.stateSet, firstOf).part
                    val upLhs = emptySet<LookaheadSetPart>()
                    HeightGraftInfo(emptyList(), listOf(pp), listOf(ppn), lhs, upLhs)
                }
            }
        }.toSet()

        // upCls is the closure down from prev
        //TODO: can we reduce upCls at this point ?
        val hgis = mutableListOf<HeightGraftInfo>()
        for (fromRp in from) {
            val upFilt = upCls.filter { fromRp == it.rulePosition.item }
            val res = upFilt.flatMap { clsItem ->
                val ancestors = clsItem.allPrev.map { it.rulePosition.runtimeRule }
                val parent = clsItem.rulePosition
                val upLhs = clsItem.parentItem?.lookaheadSet ?: LookaheadSetPart.UP
                val pns = parent.next()
                pns.map { parentNext ->
                    val lhs = this.firstOf(parentNext, upLhs)// this.stateSet.expectedAfter(parentNext)
                    val uLhs = this.calcLookaheadDown(parentNext, upLhs)
                    HeightGraftInfo(ancestors, listOf(parent), listOf(parentNext), lhs, setOf(upLhs))
                }
            }

            // the HeightGraftInfo in res will always have 1 element in parentNext, see above
            // so we can groupBy the first element of parentNext, as it is the only one
            // val grpd = res.groupBy { Pair(it.ancestors, it.parentNext) }//it.parentNext[0].isAtEnd) }//, it.lhs) }
            //     .map {
            //         val ancestors = emptyList<RuntimeRule>()//it.key.first as List<RuntimeRule>
            //         val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
            //         val parent = it.value.flatMap { it.parent }.toSet().toList()
            //         val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lhs) }
            //         val upLhs = it.value.flatMap { it.upLhs }.toSet()//.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.upLhs) }
            //         HeightGraftInfo(ancestors, (parent), (parentNext), lhs, upLhs)
            //     }
            hgis.addAll(res)
        }
        //TODO: need atEnd and notAtEnd to be separate states
        // val grouped2 = grouped.groupBy { listOf(it.parent.first().runtimeRule.kind == RuntimeRuleKind.GOAL, it.lhs, it.upLhs) }
        //     .map {
        //         val parent = it.value.flatMap { it.parent }.toSet().toList()
        //         val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
        //         val lhs = it.key[1] as LookaheadSet
        //         val upLhs = it.key[2] as LookaheadSet
        //         HeightGraftInfo(parent, parentNext, lhs, upLhs)
        //     }
        //return grouped2.toSet() //TODO: returns wrong because for {A,B}-H->{C,D} maybe only A grows into C & B into D
        //val toMerge = hgis.filter {  }
        //val groupedLhs = hgis.groupBy { listOf(it.parent, it.parentNext) }
        //    .map {
        //        val ancestors = emptyList<RuntimeRule>()
        //        val parent = it.key[0]
        //        val parentNext = it.key[1]
        //        val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lhs) }
        //        val upLhs = it.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
        //        HeightGraftInfo(ancestors, parent, parentNext, lhs, upLhs)
        //    }
        // return groupedLhs.toSet()
        //return grouped.toSet() //TODO: gives too many heads in some cases where can be grouped2

        //group if not at end and outgoing (action,to) are the same
        val atEnd = hgis.filter { it.parentNext.first().isAtEnd }
        val notAtEnd = hgis.filter { it.parentNext.first().isAtEnd.not() }
        val toMerge = notAtEnd.groupBy { it.lhs }
        val merged = toMerge.map { me ->
            val ancestors = emptyList<RuntimeRule>()
            val parent = me.value.flatMap { it.parent }.toSet().toList()
            val parentNext = me.value.flatMap { it.parentNext }.toSet().toList()
            val lhs = me.key
            val upLhs = me.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
            HeightGraftInfo(ancestors, parent, parentNext, lhs, upLhs)
        }
        return (atEnd + merged).toSet()
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

    private fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: LookaheadSetPart): LookaheadSetPart {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
            else -> {
                val next = rulePosition.next()
                next.map {
                    firstOf(it, ifReachEnd)
                }.fold(LookaheadSetPart.EMPTY) { acc, it ->
                    acc.union(it)
                }
            }
        }
    }

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
                                    val lhs = firstOf(chNx, item.lookaheadSet)
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
                                        val lhs = firstOf(chNx, item.lookaheadSet)
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


}