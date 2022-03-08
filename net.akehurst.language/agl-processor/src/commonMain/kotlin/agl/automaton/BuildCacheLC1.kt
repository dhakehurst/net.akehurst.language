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
import net.akehurst.language.collections.LazyMapNonNull
import net.akehurst.language.collections.MutableQueue
import net.akehurst.language.collections.mutableQueueOf

internal class ClosureItemLC1(
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

internal class BuildCacheLC1(
    stateSet: ParserStateSet
) : BuildCacheAbstract(stateSet) {


    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItemLC1>>()
    private val _upClosureForRuntimeRule = mutableMapOf<RuntimeRule, Set<ClosureItemLC1>>()

    //private val _dnClosureForRulePosition = mutableMapOf<RulePosition, Set<ClosureItemLC1>>()
    private val _dnClosure = mutableMapOf<RulePosition, Set<ClosureItemLC1>>()

    private val _stateInfo = mutableMapOf<List<RulePosition>, StateInfo>()

    // from-state-listOf-rule-positions -> mapOf
    //    to-state-terminal-rule -> WidthInfo
    private val _widthInto = mutableMapOf<List<RulePosition>, MutableMap<RuntimeRule, WidthInfo>>()

    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableMap<List<RulePosition>, HeightGraftInfo>>()

    override fun buildCaches() {
        val goalRule = this.stateSet.startState.runtimeRules.first()
        val G_0_0 = goalRule.rulePositions.first()
        val done = mutableMapOf<Pair<RulePosition?, LookaheadSet>, Boolean>()

        //traverse down and collect closure
        //val cls = this.traverseRulePositions(ClosureItemLC1(null, G_0_0, null, LookaheadSetPart.UP))
        //calcAndCacheWidthInfo(listOf(G_0_0), cls)
        cacheStateInfo(listOf(G_0_0.atEnd()), listOf())
    }

    override fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> = this.stateInfo2()

    private fun stateInfo1(): Set<StateInfo> {
        //= this._stateInfo.values.toSet()
        // prev of startState = null
        // closureDown(startState)
        // FOR each RP in closure
        //   stateX = RP.next
        //   stateX.prev.add( startState )
        //   IF RP.isNotAtEnd THEN
        //     closureDown(stateX)
        //     ...
        data class PossibleState(val rulePositions: List<RulePosition>) {
            val isAtEnd: Boolean = this.rulePositions.any { it.isAtEnd } //all in state should be either atEnd or notAtEnd
            val nextStates get() = this.rulePositions.flatMap { rp -> rp.next().map { PossibleState(listOf(it)) } }.toSet()
        }

        data class RpToDo(
            val state: PossibleState,
            val prev: PossibleState?,
            //      val lookaheadSet: LookaheadSetPart
        )

        val possiblePrev = LazyMapNonNull<PossibleState, MutableSet<PossibleState?>>() { mutableSetOf() }

        // Enumerate all rule-positions used in state definitions
        val allStateRPs = this.stateSet.usedRules.flatMap { rr ->
            rr.rulePositions.filter { rp -> rp.isAtStart.not() }
        }
        // compute RPs merged into one state - i.e. same ?
        val allMergedStateRps = allStateRPs.groupBy { rp -> listOf(rp) } //this.firstOf(rp, LookaheadSetPart.UP ) }.values //TODO: fix parameter to firstOf

        val stateInfos = mutableSetOf<StateInfo>()

        // previous of start state is null
        val done = mutableSetOf<PossibleState>()
        val s0 = PossibleState(this.stateSet.startState.rulePositions)
        possiblePrev[s0].add(null)
        val todo = mutableQueueOf(RpToDo(s0, null))
        while (todo.isNotEmpty) {
            val stateToDo = todo.dequeue()
            done.add(stateToDo.state)
            var stateRpNexts = stateToDo.state.nextStates
            while (stateRpNexts.isNotEmpty()) {
                stateRpNexts.forEach {
                    possiblePrev[it].add(stateToDo.prev)
                    if (done.contains(it).not()) {
                        todo.enqueue(RpToDo(it, stateToDo.prev))
                    }
                }
                stateRpNexts = stateRpNexts.flatMap { it.nextStates }.toSet()
            }
            for (rp in stateToDo.state.rulePositions) {
                val dnCls = this.dnClosureLC1(rp)
                //TODO: single loop!
                val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
                val termRps = filt.flatMap { it.rulePosition.item!!.rulePositions }.toSet()
                val rps = filt.flatMap { cls -> cls.allRulePositionsButTop }.toSet()
                val rpNexts = rps.flatMap { it.next() }.toSet() + termRps //.map { rpn -> this.fetchCompatibleState(listOf(rpn)) } }
                rpNexts.forEach {
                    val ss = PossibleState(listOf(it))
                    possiblePrev[ss].add(stateToDo.state)
                    if (it.runtimeRule.isNonTerminal && done.contains(ss).not()) {
                        todo.enqueue(RpToDo(ss, stateToDo.state))
                    }
                }
            }
        }

        //TODO("merge states ans speed up!")

        return possiblePrev.entries.map { StateInfo(it.key.rulePositions, it.value.map { it?.rulePositions ?: emptyList() }) }.toSet()
    }

    private fun stateInfo2(): Set<StateInfo> {
        data class PossibleState(val rulePosition: RulePosition, val prev: RulePosition?, val firstOf: LookaheadSetPart, val firstOfNext: LookaheadSetPart) {
            val isAtEnd: Boolean = this.rulePosition.isAtEnd
            //override fun hashCode(): Int = rulePosition.hashCode()
            //override fun equals(other: Any?): Boolean =when(other) {
            //    !is PossibleState -> false
            //    else -> this.rulePosition ==other.rulePosition
            //}

            override fun toString(): String = "${this.rulePosition}-prev->${prev}[${firstOfNext.fullContent.joinToString { it.tag }}]"
        }

        data class RpToDo(
            val state: PossibleState,
            val prev: PossibleState?,
            val parent: PossibleState?
        )

        val possiblePrev = LazyMapNonNull<PossibleState, MutableSet<PossibleState?>>() { mutableSetOf() }
        val done = mutableSetOf<PossibleState>()
        val rpS0 = this.stateSet.startState.rulePositions.first()
        val firstOfS0 = firstOf(rpS0, LookaheadSetPart.UP)
        val s0 = PossibleState(rpS0, null, firstOfS0, LookaheadSetPart.UP)
        possiblePrev[s0].add(null)
        possiblePrev[PossibleState(rpS0, null, LookaheadSetPart.UP, LookaheadSetPart.EMPTY)].add(null)
        val todo = mutableQueueOf(RpToDo(s0, s0, null))
        while (todo.isNotEmpty) {
            val stateToDo = todo.dequeue()
            val state = stateToDo.state
            val parent = stateToDo.parent
            val parentFirstOf = parent?.firstOf ?: LookaheadSetPart.UP
            val parentFirstOfNext = parent?.firstOfNext ?: LookaheadSetPart.UP
            done.add(stateToDo.state)
            val stateFirstOfNext = when {
                state.isAtEnd -> stateToDo.parent?.rulePosition?.let { firstOf(it, parentFirstOfNext) } ?: LookaheadSetPart.UP
                else -> state.rulePosition.next().map { firstOf(it, parentFirstOfNext) }.reduce { acc, it -> acc.union(it) }
            }
            val rule = stateToDo.state.rulePosition.item
            if (null != rule) {
                val ruleRps = rule.rulePositions
                for (ruleRp in ruleRps) {
                    val firstOf = firstOf(ruleRp, stateFirstOfNext)
                    val firstOfNext = when {
                        ruleRp.isAtEnd -> stateFirstOfNext
                        else -> ruleRp.next().map { firstOf(it, stateFirstOfNext) }.reduce { acc, it -> acc.union(it) }
                    }
                    val ruleRpState = PossibleState(ruleRp, stateToDo.prev?.rulePosition, firstOf, firstOfNext)
                    if (ruleRp.isAtStart.not()) possiblePrev[ruleRpState].add(stateToDo.prev)
                    val nextPrev = when {
                        ruleRp.isAtStart -> stateToDo.prev
                        ruleRp.isAtEnd -> stateToDo.prev
                        else -> ruleRpState
                    }
                    if (done.contains(ruleRpState).not()) {
                        todo.enqueue(RpToDo(ruleRpState, nextPrev, state))
                    }
                }
            } else {
                //no items for rule
            }
        }
        //TODO("merge states ans speed up!")
        // need to compute transition info, because merging relies on it,
        // merge states a and b if a.outgoing == b.outgoing.
        // tr1==tr2 if tr2.to==tr2.to && tr1.lh==tr2.lh

        return possiblePrev.entries.map { StateInfo(listOf(it.key.rulePosition), it.value.map { it?.let { listOf(it.rulePosition) } ?: emptyList() }) }.toSet()
    }

    override fun widthInto(fromStateRulePositions: List<RulePosition>): Set<WidthInfo> {
        return if (this._cacheOff) {
            val dnCls = fromStateRulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
            val calc = calcAndCacheWidthInfo(fromStateRulePositions, dnCls)
            calc
        } else {
            this._widthInto[fromStateRulePositions]?.values?.toSet() ?: run {
                val dnCls = fromStateRulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
                val calc = calcAndCacheWidthInfo(fromStateRulePositions, dnCls)
                calc
            }
        }
    }

    override fun heightGraftInto(prevStateRulePositions: List<RulePosition>, fromStateRuntimeRules: List<RuntimeRule>): Set<HeightGraftInfo> {
        return if (this._cacheOff) {
            // have to ensure somehow that from grows into prev
            // have to do closure down from prev,
            // upCls is the closure down from prev
            val upCls = prevStateRulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
            val calc = calcAndCacheHeightOrGraftInto(prevStateRulePositions, fromStateRuntimeRules, upCls)
            calc
        } else {
            val key = Pair(prevStateRulePositions, fromStateRuntimeRules)
            this._heightOrGraftInto[key]?.values?.toSet() ?: run {
                val upCls = prevStateRulePositions.flatMap { this.dnClosureLC1(it) }.toSet()
                val calc = calcAndCacheHeightOrGraftInto(prevStateRulePositions, fromStateRuntimeRules, upCls)
                calc
            }
        }
    }

    private fun cacheStateInfo(rulePositions: List<RulePosition>, prev: List<RulePosition>) {
        val existing = this._stateInfo[rulePositions]
        if (null == existing) {
            this._stateInfo[rulePositions] = StateInfo(rulePositions, listOf(prev))
        } else {
            val pp = existing.possiblePrev.union(listOf(prev)).toList()
            this._stateInfo[rulePositions] = StateInfo(rulePositions, pp)
        }
    }

    private fun calcAndCacheWidthInfo(fromRulePositions: List<RulePosition>, dnCls: Set<ClosureItemLC1>): Set<WidthInfo> {
        val wis = calcWidthInfo(fromRulePositions, dnCls)
        cacheWidthInfo(fromRulePositions, wis)
        return wis
    }

    private fun calcWidthInfo(fromRulePositions: List<RulePosition>, dnCls: Set<ClosureItemLC1>): Set<WidthInfo> {
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

    private fun cacheWidthInfo(fromRulePositions: List<RulePosition>, wis: Set<WidthInfo>) {
        val map = this._widthInto[fromRulePositions] ?: run {
            val x = mutableMapOf<RuntimeRule, WidthInfo>()
            this._widthInto[fromRulePositions] = x
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
        val hgi = calcHeightOrGraftInto(from, upCls)
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
    private fun calcHeightOrGraftInto(from: List<RuntimeRule>, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        // upCls is the closure down from prev
        //TODO: can we reduce upCls at this point ?
        val grouped = mutableListOf<HeightGraftInfo>()
        for (fromRp in from) {
            val upFilt = upCls.filter { fromRp == it.rulePosition.item }
            val res = upFilt.flatMap { clsItem ->
                val ancestors = clsItem.allPrev.map { it.rulePosition.runtimeRule }
                val parent = clsItem.rulePosition
                val upLhs = clsItem.parentItem?.lookaheadSet ?: LookaheadSetPart.UP
                val pns = parent.next()
                pns.map { parentNext ->
                    val lhs = this.stateSet.buildCache.firstOf(parentNext, upLhs)// this.stateSet.expectedAfter(parentNext)
                    HeightGraftInfo(ancestors, listOf(parent), listOf(parentNext), lhs, setOf(upLhs))
                }
            }
            // the HeightGraftInfo in res will always have 1 element in parentNext, see above
            // so we can groupBy the first element of parentNext, as it is the only one
            val grpd = res.groupBy { Pair(it.ancestors, it.parentNext[0].isAtEnd) }//, it.lhs) }
                .map {
                    val ancestors = emptyList<RuntimeRule>()//it.key.first as List<RuntimeRule>
                    val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
                    val parent = it.value.flatMap { it.parent }.toSet().toList()
                    val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lhs) }
                    val upLhs = it.value.flatMap { it.upLhs }.toSet()//.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.upLhs) }
                    HeightGraftInfo(ancestors, (parent), (parentNext), lhs, upLhs)
                }
            grouped.addAll(grpd)
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

        val groupedLhs = grouped.groupBy { listOf(it.parent, it.parentNext) }
            .map {
                val ancestors = emptyList<RuntimeRule>()
                val parent = it.key[0]
                val parentNext = it.key[1]
                val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lhs) }
                val upLhs = it.value.flatMap { it.upLhs }.toSet().fold(setOf<LookaheadSetPart>()) { acc, e -> if (acc.any { it.containsAll(e) }) acc else acc + e }
                HeightGraftInfo(ancestors, parent, parentNext, lhs, upLhs)
            }
        return groupedLhs.toSet()
        //return grouped.toSet() //TODO: gives too many heads in some cases where can be grouped2
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
        val items = mutableSetOf<ClosureItemLC1>(topItem)
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