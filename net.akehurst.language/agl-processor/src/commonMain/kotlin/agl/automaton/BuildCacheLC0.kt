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

internal data class ClosureItemLC0(
    val parentItem: ClosureItemLC0?, //needed for height/graft
    val rulePosition: RulePosition
) {
    val allPrev: Set<ClosureItemLC0> = if (null == parentItem) mutableSetOf() else parentItem.allPrev + parentItem

    val prev: List<RulePosition> by lazy {
        if (null == this.parentItem) {
            emptyList<RulePosition>()
        } else {
            var x: ClosureItemLC0 = this.parentItem
            while (x.rulePosition.isAtStart && x.rulePosition.runtimeRule.kind != RuntimeRuleKind.GOAL) {
                x = x.parentItem!!
            }
            listOf(x.rulePosition)
        }
    }

    fun hasLooped(): Boolean = allPrev.any { it.rulePosition == this.rulePosition }

    private fun chain(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.chain()}->"
        }
        return "$p$rulePosition"
    }

    override fun toString(): String {
        return "${chain()}"
    }

}

internal class BuildCacheLC0(
    stateSet: ParserStateSet
) : BuildCacheAbstract(stateSet) {

    private val _upClosure = mutableMapOf<RulePosition, Set<ClosureItemLC0>>()
    private val _dnClosure = mutableMapOf<RulePosition, Set<ClosureItemLC0>>()

    private val _stateInfo = mutableMapOf<List<RulePosition>, StateInfo>()

    // from-state-listOf-rule-positions -> mapOf
    //    to-state-terminal-rule -> WidthInfo
    private val _widthInto = mutableMapOf<List<RulePosition>, MutableMap<RuntimeRule, WidthInfo>>()

    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableSet<HeightGraftInfo>>()

    override fun clearAndOff() {
        _upClosure.clear()
        _dnClosure.clear()
        //_firstOfNotEmpty.clear()
        _stateInfo.clear()
        _widthInto.clear()
        _heightOrGraftInto.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> = this._stateInfo.values.toSet()

    override fun widthInto(prevState: ParserState, fromState: ParserState): Set<WidthInfo> {
        return this._widthInto[fromState.rulePositions]?.values?.toSet() ?: run {
            val dnCls = fromState.rulePositions.flatMap { this.dnClosureLR0(it) }.toSet()
            val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
            val bottomTerminals = filt.map { it.rulePosition.item!! }.toSet()
            val calc = calcAndCacheWidthInfo(fromState.rulePositions, bottomTerminals)
            calc
        }
    }

    override fun heightGraftInto(prevState: ParserState, fromStateRuntimeRules: List<RuntimeRule>): Set<HeightGraftInfo> {
        val key = Pair(prevState.rulePositions, fromStateRuntimeRules)
        return this._heightOrGraftInto[key] ?: run {
            val upCls = prevState.rulePositions.flatMap { this.dnClosureLR0(it) }.toSet()
            val calc = calcAndCacheHeightOrGraftInto(prevState.rulePositions, fromStateRuntimeRules, upCls)
            calc
        }
    }
/*
    private fun cacheStateInfo(rulePositions: List<RulePosition>, prev: List<RulePosition>) {
        val existing = this._stateInfo[rulePositions]
        if (null == existing) {
            this._stateInfo[rulePositions] = StateInfo(rulePositions, listOf(prev))
        } else {
            val pp = existing.possiblePrev.union(listOf(prev)).toList()
            this._stateInfo[rulePositions] = StateInfo(rulePositions, pp)
        }
    }
*/
    private fun calcAndCacheWidthInfo(fromRulePositions: List<RulePosition>, bottomTerminals: Set<RuntimeRule>): Set<WidthInfo> {
        val wis = calcWidthInfo(fromRulePositions, bottomTerminals)
        cacheWidthInfo(fromRulePositions, wis)
        return wis
    }

    private fun calcWidthInfo(fromRulePositions: List<RulePosition>, bottomTerminals: Set<RuntimeRule>): Set<WidthInfo> {
        // lookahead comes from closure on prev
        // upLhs can always be LookaheadSet.UP because the actual LH is carried at runtime
        // thus we don't need prevState in to compute width targets
//        val filt = dnCls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
        //       val grouped = filt.groupBy { it.rulePosition.item!! }.map {
        //           val rr = it.key
        //           val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
//            val lhs = LookaheadSet.ANY
//            WidthInfo(rp, lhs)
//        }.toSet()
        //don't group them, because we need the info on the lookahead for the runtime calc of next lookaheads
        //return grouped
        return bottomTerminals.map {
            val rp = RulePosition(it, 0, RulePosition.END_OF_RULE)
            val lhs = LookaheadSetPart.ANY
            WidthInfo(rp, lhs)
        }.toSet()
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
                map[wi.to.runtimeRule] = wi
            }
        }
    }

    private fun calcAndCacheHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>, upCls: Set<ClosureItemLC0>): Set<HeightGraftInfo> {
        val hgi = calcHeightOrGraftInto(from, upCls)
        cacheHeightOrGraftInto(prev, from, hgi)
        return hgi
    }

    private fun cacheHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>, hgis: Set<HeightGraftInfo>) {
        val key = Pair(prev, from)
        val set = this._heightOrGraftInto[key] ?: run {
            val x = mutableSetOf<HeightGraftInfo>()
            this._heightOrGraftInto[key] = x
            x
        }
        for (hg in hgis) {
            set.add(hg)
        }
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(from: List<RuntimeRule>, upCls: Set<ClosureItemLC0>): Set<HeightGraftInfo> {
        // have to ensure somehow that this grows into prev
        // have to do closure down from prev,
        // upCls is the closure down from prev
        val upFilt = upCls.filter { from.contains(it.rulePosition.item) }
        val res = upFilt.flatMap { clsItem ->
            val ancestors = clsItem.allPrev.map { it.rulePosition.runtimeRule }
            val parent = clsItem.rulePosition
            val upLhs = when (parent.runtimeRule.kind) {
                RuntimeRuleKind.GOAL -> if (parent.isAtEnd) LookaheadSetPart.UP else LookaheadSetPart.ANY
                else -> LookaheadSetPart.ANY
            }
            val pns = parent.next()
            pns.map { parentNext ->
                val lhs = when (parentNext.runtimeRule.kind) {
                    RuntimeRuleKind.GOAL -> LookaheadSetPart.UP
                    else -> LookaheadSetPart.ANY
                }
                HeightGraftInfo(ancestors,listOf(parent), listOf(parentNext), lhs, setOf(upLhs))
            }
        }
        val grouped = res.groupBy { listOf(it.ancestors, it.parentNext) }//, it.lhs) }
            .map {
                val ancestors = it.key[0] as List<RuntimeRule>
                val parentNext = it.key[1] as List<RulePosition>
                val parent = it.value[0].parent // should all be the same as ancestors are the same
                val lhs = it.value.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.lhs) }
                val upLhs = it.value.flatMap { it.upLhs }.toSet()//.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e.upLhs) }
                HeightGraftInfo(ancestors,(parent), (parentNext), lhs, upLhs)
            }
        //val grouped2 = grouped.groupBy { listOf(it.lhs, it.upLhs, it.parentNext.map { it.position }) }
        //    .map {
        //        val parent = it.value.flatMap { it.parent }.toSet().toList()
        //        val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
        //        val lhs = it.key[0] as LookaheadSet
        //        val upLhs = it.key[1] as LookaheadSet
        //        HeightGraftInfo(parent, parentNext, lhs, upLhs)
        //    }
        //return grouped2.toSet() //TODO: returns wrong because for {A,B}-H->{C,D} maybe only A grows into C & B into D
        return grouped.toSet() //TODO: gives too many heads in some cases where can be grouped2
    }

    /*
    // return the terminals at the bottom of each closure
    private fun traverseRulePositions(parent: ClosureItemLC0): Set<RuntimeRule> {
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
            parent.hasLooped() -> emptySet()
            else -> {
                val result = mutableSetOf<RuntimeRule>()
                when {
                    parent.rulePosition.isAtStart && parent.rulePosition.runtimeRule.kind != RuntimeRuleKind.GOAL -> {
                        // no need to cache for SOR positions, but need to traverse
                        val runtimeRule = parent.rulePosition.item ?: error("should never be null as position != EOR")
                        for (rp in runtimeRule.rulePositions) {
                            val ci = ClosureItemLC0(parent, rp)
                            val dnCls = traverseRulePositions(ci)
                            result.addAll(dnCls)
                        }
                    }
                    else -> {
                        // cache and traverse
                        val runtimeRule = parent.rulePosition.item ?: error("should never be null as position != EOR")
                        for (rp in runtimeRule.rulePositions) {
                            val ci = ClosureItemLC0(parent, rp)
                            val bottomTerminals = traverseRulePositions(ci)
                            if (rp.isAtEnd.not()) {
                                calcAndCacheWidthInfo(listOf(parent.rulePosition), bottomTerminals)
                            }
                            result.addAll(bottomTerminals)
                        }
                        cacheStateInfo(listOf(parent.rulePosition), parent.prev)
                    }
                }
                result
            }
        }
    }

    private fun createMergedListsOfRulePositions(): List<List<RulePosition>> {
        val map = createRulePositionsIndexByFirstItem()
        val result = mutableListOf<List<RulePosition>>()
        val allReadyMerged = mutableSetOf<RulePosition>()
        for (list in map.values) {
            when (list.size) {
                // if only one item, then must be state on its own
                1 -> result.add(list)
                else -> {
                    //multiple rps whoes rule has same first item, might be same state
                    // need to check the rest of the items
                    var head = list[0]
                    var tail = list.drop(1)
                    while (tail.isNotEmpty()) {
                        if (allReadyMerged.contains(head)) {
                            //skip it
                        } else {
                            val sl = mutableListOf(head)
                            for (rp2 in tail) {
                                when {
                                    head.runtimeRule == rp2.runtimeRule -> null // not the same if position in same rule
                                    rulePositionsAreSameState(head, rp2) -> {
                                        sl.add(rp2)
                                        allReadyMerged.add(rp2)
                                    }
                                    else -> null //do nothing
                                }
                            }
                            result.add(sl)
                        }
                        head = tail[0]
                        tail = tail.drop(1)
                    }
                    if (allReadyMerged.contains(head)) {
                        //skip it
                    } else {
                        result.add(listOf(head))
                    }
                }
            }
        }
        return result
    }

    private fun createRulePositionsIndexByFirstItem(): Map<RuntimeRule, List<RulePosition>> {
        // key is first item of RulePosition's rule
        val map = mutableMapOf<RuntimeRule, MutableList<RulePosition>>()
        for (rr in this.stateSet.usedNonTerminalRules) {
            for (rp in rr.rulePositions) {
                if (RulePosition.START_OF_RULE == rp.position) {
                    // do nothing, SOR position RPs are not made into a state, ther than for GOAL
                } else {
                    val key = rp.runtimeRule.item(rp.option, RulePosition.START_OF_RULE)!!
                    val list = map[key] ?: run {
                        map[key] = mutableListOf<RulePosition>()
                        map[key]!!
                    }
                    list.add(rp)
                }
            }
        }
        return map
    }

    private fun rulePositionsAreSameState(rp1: RulePosition, rp2: RulePosition): Boolean {
        val sameItemsUntil = when (rp1.runtimeRule.kind) {
            //RuntimeRuleKind.GOAL -> error("")
            RuntimeRuleKind.NON_TERMINAL -> when (rp1.runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> error("")
                RuntimeRuleRhsItemsKind.CHOICE -> {
                    rp1.runtimeRule.item(rp1.option, 0) == rp2.runtimeRule.item(rp2.option, 0)
                }
                RuntimeRuleRhsItemsKind.CONCATENATION -> when (rp2.runtimeRule.kind) {
                    RuntimeRuleKind.NON_TERMINAL -> when (rp1.runtimeRule.rhs.itemsKind) {
                        RuntimeRuleRhsItemsKind.EMPTY -> error("")
                        RuntimeRuleRhsItemsKind.CHOICE -> {
                            rp1.runtimeRule.item(rp1.option, 0) == rp2.runtimeRule.item(rp2.option, 0)
                        }
                        RuntimeRuleRhsItemsKind.CONCATENATION -> {
                            val pIndex = if (rp1.position == RulePosition.END_OF_RULE) rp1.runtimeRule.rhs.items.size else rp1.position
                            rp1.position == rp2.position && (1..pIndex).all { p ->
                                rp1.runtimeRule.item(rp1.option, p) == rp2.runtimeRule.item(rp2.option, p)
                            }
                        }
                        RuntimeRuleRhsItemsKind.LIST -> rp1.runtimeRule.item(rp1.option, rp1.position) == rp2.runtimeRule.item(rp2.option, rp1.position)
                    }
                    else -> error("should not happen")
                }
                RuntimeRuleRhsItemsKind.LIST -> when (rp2.runtimeRule.kind) {
                    //RuntimeRuleKind.GOAL -> error("")
                    RuntimeRuleKind.NON_TERMINAL -> when (rp1.runtimeRule.rhs.itemsKind) {
                        RuntimeRuleRhsItemsKind.EMPTY -> error("")
                        RuntimeRuleRhsItemsKind.CHOICE -> {
                            rp1.runtimeRule.item(rp1.option, 0) == rp2.runtimeRule.item(rp2.option, 0)
                        }
                        RuntimeRuleRhsItemsKind.CONCATENATION -> {
                            val pIndex = if (rp2.position == RulePosition.END_OF_RULE) rp2.runtimeRule.rhs.items.size else rp2.position
                            (1..pIndex).all { p ->
                                rp1.runtimeRule.item(rp1.option, p) == rp2.runtimeRule.item(rp2.option, p)
                            }
                        }
                        RuntimeRuleRhsItemsKind.LIST -> rp1.runtimeRule.item(rp1.option, rp1.position) == rp2.runtimeRule.item(rp2.option, rp1.position)
                    }
                    else -> error("should not happen")
                }
            }
            else -> error("should not happen")
        }
        val rp1NextItems = rp1.item//rp1.next().map { it.item }
        val rp2NextItems = rp2.item//rp2.next().map { it.item }
        return sameItemsUntil && rp1NextItems == rp2NextItems
    }
*/

    internal fun dnClosureLR0(rp: RulePosition): Set<ClosureItemLC0> {
        return if (_cacheOff) {
            val ci = ClosureItemLC0(null, rp)
            calcDnClosureLR0(ci, mutableSetOf())
        } else {
            _dnClosure[rp] ?: run {
                val ci = ClosureItemLC0(null, rp)
                val v = calcDnClosureLR0(ci, mutableSetOf())
                _dnClosure[rp] = v
                v
            }
        }
    }

    private fun calcDnClosureLR0(parent: ClosureItemLC0, items: MutableSet<ClosureItemLC0>): Set<ClosureItemLC0> {
        return when {
            parent.rulePosition.isAtEnd -> items
            items.any {
                it.rulePosition == parent.rulePosition
            } -> items
            else -> {
                items.add(parent)
                val itemRps = parent.rulePosition.items.flatMap {
                    it.rulePositionsAt[0]
                }.toSet()
                itemRps.forEach { childRp ->
                    val ci = ClosureItemLC0(parent, childRp)
                    calcDnClosureLR0(ci, items)
                }
                items
            }
        }
    }

}