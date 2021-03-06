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

class ClosureItemLC1(
    val parentItem: ClosureItemLC1?, //needed for height/graft
    val rulePosition: RulePosition,
    val next: RulePosition?,
    val lookaheadSet: LookaheadSet
) {
    val allPrev: Set<ClosureItemLC1> = if (null == parentItem) mutableSetOf() else parentItem.allPrev + parentItem

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

    fun hasLooped(): Boolean = allPrev.any {
        it.rulePosition == this.rulePosition &&
                it.lookaheadSet == this.lookaheadSet &&
                it.parentItem?.lookaheadSet == this.parentItem?.lookaheadSet
    }

    private fun chain(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.chain()}->"
        }
        return "$p$rulePosition"
    }

    private val _hashCode = listOf(this.rulePosition, this.next, this.lookaheadSet).hashCode()
    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when (other) {
        is ClosureItemLC1 -> other.rulePosition == this.rulePosition &&
                other.lookaheadSet == this.lookaheadSet &&
                other.parentItem?.lookaheadSet == this.parentItem?.lookaheadSet
        else -> false
    }

    override fun toString(): String {
        return "${chain()}$lookaheadSet"
    }
}

class BuildCacheLC1(
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
        val cls = this.traverseRulePositions(ClosureItemLC1(null, G_0_0, null, LookaheadSet.UP))
        //calcAndCacheWidthInfo(listOf(G_0_0), cls)
        cacheStateInfo(listOf(G_0_0.atEnd()), listOf())
    }

    override fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> = this._stateInfo.values.toSet()

    override fun widthInto(fromStateRulePositions: List<RulePosition>): Set<WidthInfo> {
        return if (this._cacheOff) {
            val dnCls = fromStateRulePositions.flatMap { this.dnClosureLC1(it, LookaheadSet.UP) }.toSet()
            //val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
            //val bottomTerminals = filt.map { it.rulePosition.item!! }.toSet()
            val calc = calcAndCacheWidthInfo(fromStateRulePositions, dnCls)
            calc
        } else {
            this._widthInto[fromStateRulePositions]?.values?.toSet() ?: run {
                val dnCls = fromStateRulePositions.flatMap { this.dnClosureLC1(it, LookaheadSet.UP) }.toSet()
                //val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
                //val bottomTerminals = filt.map { it.rulePosition.item!! }.toSet()
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
            val upCls = prevStateRulePositions.flatMap { this.dnClosureLC1(it, LookaheadSet.UP) }.toSet()
            val calc = calcAndCacheHeightOrGraftInto(prevStateRulePositions, fromStateRuntimeRules, upCls)
            calc
        } else {
            val key = Pair(prevStateRulePositions, fromStateRuntimeRules)
            this._heightOrGraftInto[key]?.values?.toSet() ?: run {
                val upCls = prevStateRulePositions.flatMap { this.dnClosureLC1(it, LookaheadSet.UP) }.toSet()
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
            val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
            val lhs = this.createLookaheadSet(lhsc)
            WidthInfo(rp, lhs)
        }.toSet()
        //don't group them, because we need the info on the lookahead for the runtime calc of next lookaheads
        return grouped
    }

    private fun cacheWidthInfo(fromRulePositions: List<RulePosition>, wis: Set<WidthInfo>) {
        var map = this._widthInto[fromRulePositions] ?: run {
            val x = mutableMapOf<RuntimeRule, WidthInfo>()
            this._widthInto[fromRulePositions] = x
            x
        }
        for (wi in wis) {
            val existing = map[wi.to.runtimeRule]
            if (null == existing) {
                map[wi.to.runtimeRule] = wi
            } else {
                val lhs = this.createLookaheadSet(wi.lookaheadSet.content.union(existing.lookaheadSet.content))
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
            val existing = map[hg.parent]
            if (null == existing) {
                map[hg.parent] = hg
            } else {
                val lhs = this.createLookaheadSet(hg.lhs.content.union(existing.lhs.content))
                val upLhs = this.createLookaheadSet(hg.upLhs.content.union(existing.upLhs.content))
                map[hg.parent] = HeightGraftInfo(hg.parent, hg.parentNext, lhs, upLhs)
            }
        }
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(from: List<RuntimeRule>, upCls: Set<ClosureItemLC1>): Set<HeightGraftInfo> {
        // upCls is the closure down from prev
        var grouped = mutableListOf<HeightGraftInfo>()
        for (fromRp in from) {
            val upFilt = upCls.filter { fromRp == it.rulePosition.item }
            val res = upFilt.flatMap { clsItem ->
                val parent = clsItem.rulePosition
                val upLhs = clsItem.parentItem?.lookaheadSet ?: LookaheadSet.UP
                val pns = parent.next()
                pns.map { parentNext ->
                    val lhsc = this.stateSet.buildCache.firstOf(parentNext, upLhs)// this.stateSet.expectedAfter(parentNext)
                    val lhs = this.createLookaheadSet(lhsc)
                    HeightGraftInfo(listOf(parent), listOf(parentNext), lhs, upLhs)
                }
            }
            val grpd = res.groupBy { listOf(it.parent, it.parentNext) }//, it.lhs) }
                .map {
                    val parent = it.key[0] as List<RulePosition>
                    val parentNext = it.key[1] as List<RulePosition>
                    val lhs = createLookaheadSet(it.value.flatMap { it.lhs.content }.toSet())
                    val upLhs = createLookaheadSet(it.value.flatMap { it.upLhs.content }.toSet())
                    HeightGraftInfo((parent), (parentNext), lhs, upLhs)
                }
            grouped.addAll(grpd)
        }
        val grouped2 = grouped.groupBy { listOf(it.parent.first().runtimeRule.kind == RuntimeRuleKind.GOAL, it.lhs, it.upLhs) }
            .map {
                val parent = it.value.flatMap { it.parent }.toSet().toList()
                val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
                val lhs = it.key[1] as LookaheadSet
                val upLhs = it.key[2] as LookaheadSet
                HeightGraftInfo(parent, parentNext, lhs, upLhs)
            }
        //return grouped2.toSet() //TODO: returns wrong because for {A,B}-H->{C,D} maybe only A grows into C & B into D
        return grouped.toSet() //TODO: gives too many heads in some cases where can be grouped2
    }

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
            parent.hasLooped() -> emptySet() // do nothing
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
                                        val lhsc = firstOf(next, parent.lookaheadSet)
                                        val lhs = createLookaheadSet(lhsc)
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

    private fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.stateSet.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs

    private fun dnClosureLC1(rp: RulePosition, upLhs: LookaheadSet): Set<ClosureItemLC1> {
        return if (_cacheOff) {
            val lhsc = calcLookaheadDown(rp, upLhs)
            val lhs = this.stateSet.createLookaheadSet(lhsc)
            val ci = ClosureItemLC1(null, rp, null, lhs)
            calcDnClosureLC1(ci, mutableSetOf())
        } else {
            _dnClosure[rp] ?: run {
                val lhsc = calcLookaheadDown(rp, upLhs)
                val lhs = this.stateSet.createLookaheadSet(lhsc)
                val ci = ClosureItemLC1(null, rp, null, lhs)
                val v = calcDnClosureLC1(ci, mutableSetOf())
                _dnClosure[rp] = v
                v
            }
        }
    }

    private fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: LookaheadSet): Set<RuntimeRule> {
        return when {
            rulePosition.isAtEnd -> ifReachEnd.content
            else -> {
                val next = rulePosition.next()
                next.flatMap {
                    firstOf(it, ifReachEnd)
                }.toSet()
            }
        }
    }

    // does not go all the way down to terminals,
    // stops just above,
    // when item.rulePosition.items is a TERMINAL/EMBEDDED
    private fun calcDnClosureLC1(item: ClosureItemLC1, items: MutableSet<ClosureItemLC1> = mutableSetOf()): Set<ClosureItemLC1> {
        return when {
            item.rulePosition.isAtEnd -> items
            items.any {
                it.rulePosition == item.rulePosition &&
                        it.lookaheadSet == item.lookaheadSet &&
                        it.parentItem?.lookaheadSet == item.parentItem?.lookaheadSet
            } -> items
            else -> {
                items.add(item)
                //for (rr in item.rulePosition.items) {
                val rr = item.rulePosition.item
                if (null != rr) {
                    when (rr.kind) {
                        RuntimeRuleKind.TERMINAL,
                        RuntimeRuleKind.EMBEDDED -> {
                            //val chItem = ClosureItemLC1(item, RulePosition(rr, 0, RulePosition.END_OF_RULE), item.lookaheadSet)
                            //items.add(chItem)
                        }
                        RuntimeRuleKind.GOAL,
                        RuntimeRuleKind.NON_TERMINAL -> {
                            val chRps = rr.rulePositionsAt[0]
                            for (chRp in chRps) {
                                val chNext = chRp.next()
                                for (chNx in chNext) {
                                    val lh = firstOf(chNx, item.lookaheadSet)
                                    val lhs = this.stateSet.runtimeRuleSet.createLookaheadSet(lh)
                                    val ci = ClosureItemLC1(item, chRp, chNx, lhs)
                                    calcDnClosureLC1(ci, items)
                                }
                            }
                        }
                    }
                }
                items
            }
        }
    }

}