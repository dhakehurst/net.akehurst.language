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

data class ClosureItem(
    val parentItem: ClosureItem?, //needed for height/graft
    val rulePosition: RulePosition,
    val next: RulePosition?,
    val lookaheadSet: LookaheadSet
) {

    val prev: Set<RulePosition>
        get() = when {
            null == parentItem -> emptySet()
            else -> parentItem.rulePosition.next().filter { it.isAtEnd.not() }.toSet()
        }

    private fun chain(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.chain()}->"
        }
        return "$p$rulePosition"
    }

    override fun toString(): String {
        return "${chain()}$lookaheadSet"
    }
}

class BuildCacheLC1(
    val stateSet: ParserStateSet
)  : BuildCache {

    private var _cacheOff = true
    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItem>>()
    private val _upClosureForRuntimeRule = mutableMapOf<RuntimeRule, Set<ClosureItem>>()
    private val _dnClosureForRulePosition = mutableMapOf<RulePosition, Set<ClosureItem>>()

    //could use Map RuleNumber->FirstOfResult, but index with RuleNumber should be faster.
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })
    private val _parentPosition = mutableMapOf<RuntimeRule, Set<RulePosition>>()

    private val _stateInfo = mutableMapOf<List<RulePosition>,StateInfo>()

    // from-state-listOf-rule-positions -> mapOf
    //    to-state-terminal-rule -> WidthInfo
    private val _widthInto = mutableMapOf<List<RulePosition>, MutableMap<RuntimeRule, WidthInfo>>()

    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> mapOf
    //    to-state-rule-positions -> HeightGraftInfo
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>, List<RuntimeRule>>, MutableMap<List<RulePosition>,HeightGraftInfo>>()

    override fun on() {
        _cacheOff = false
    }

    override fun buildCaches() {
        val goalRule = this.stateSet.startState.runtimeRules.first()
        val G_0_0 = goalRule.rulePositions.first()
        val done = mutableMapOf<Pair<RulePosition?, LookaheadSet>, Boolean>()

        //traverse down and collect closure
        val cls = this.traverseRulePositionsForRule(ClosureItem(null, G_0_0, null, LookaheadSet.UP), mutableSetOf())
        calcAndCacheWidthInfo(listOf(G_0_0), cls)
    }

    override fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    override fun stateInfo(): Set<StateInfo> = this._stateInfo.values.toSet()

    override fun widthInto(fromStateRulePositions: List<RulePosition>): Set<WidthInfo> {
        return this._widthInto[fromStateRulePositions]?.values?.toSet() ?: run {
            val dnCls = fromStateRulePositions.flatMap { this.calcClosure(it, LookaheadSet.UP) }.toSet()
            val calc = calcAndCacheWidthInfo(fromStateRulePositions, dnCls)
            calc
        }
    }

    override fun heightGraftInto(prevStateRulePositions: List<RulePosition>, fromStateRuntimeRules: List<RuntimeRule>): Set<HeightGraftInfo> {
        val key = Pair(prevStateRulePositions, fromStateRuntimeRules)
        return this._heightOrGraftInto[key]?.values?.toSet() ?: run {
            val upCls = prevStateRulePositions.flatMap { this.closureForRulePosition(it, LookaheadSet.UP) }.toSet()
            val calc = calcAndCacheHeightOrGraftInto(prevStateRulePositions, fromStateRuntimeRules, upCls)
            calc
        }
    }


    // return the 'closure' of the parent.rulePosition
    private fun traverseRulePositionsForRule(parent: ClosureItem, upCls: Set<ClosureItem>): Set<ClosureItem> {
        return when {
            parent.rulePosition.isAtEnd -> emptySet() // do nothing
            upCls.any {
                it.rulePosition == parent.rulePosition &&
                        it.lookaheadSet == parent.lookaheadSet &&
                        it.parentItem?.lookaheadSet == parent.parentItem?.lookaheadSet
            } -> emptySet() // do nothing
            else -> {
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
                            val resultItems = mutableSetOf<ClosureItem>()
                            for (rp in runtimeRule.rulePositions) {
                                val itemRule = rp.item
                                when {
                                    null == itemRule -> null // must be atEnd, can't traverse down
                                    else -> {
                                        val next = rp.next().first() // should be only one next
                                        val lhs = parent.lookaheadSet // next will be atEnd, so use parent lookahead
                                        val clsItem = ClosureItem(parent, rp, next, lhs)
                                        val dnCls = traverseRulePositionsForRule(clsItem, upCls + parent)
                                        resultItems.addAll(dnCls)
                                    }
                                }
                            }
                            resultItems
                        }
                        RuntimeRuleRhsItemsKind.CONCATENATION -> {
                            val resultItems = mutableSetOf<ClosureItem>()
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
                                        val clsItem = ClosureItem(parent, rp, next, lhs)
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
            }
        }
    }

    private fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.stateSet.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs

    private fun cacheUpClosure(runtimeRule: RuntimeRule, cls: Set<ClosureItem>) {
        check(_upClosureForRuntimeRule[runtimeRule] == null)
        _upClosureForRuntimeRule[runtimeRule] = cls
    }

    private fun cacheDnClosure(rulePosition: RulePosition, cls: Set<ClosureItem>) {
        check(_dnClosureForRulePosition[rulePosition] == null)
        _dnClosureForRulePosition[rulePosition] = cls
    }

    private fun calcAndCacheWidthInfo(fromRulePositions: List<RulePosition>, cls: Set<ClosureItem>): Set<WidthInfo> {
        val wis = calcWidthInfo(fromRulePositions, cls)
        cacheWidthInfo(fromRulePositions, wis)
        return wis
    }

    private fun calcWidthInfo(fromRulePositions: List<RulePosition>, dnCls: Set<ClosureItem>): Set<WidthInfo> {
        // lookahead comes from closure on prev
        // upLhs can always be LookaheadSet.UP because the actual LH is carried at runtime
        // thus we don't need prevState in to compute width targets
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

    private fun calcAndCacheHeightOrGraftInto(prev:List<RulePosition>, from: List<RuntimeRule>, upCls: Set<ClosureItem>): Set<HeightGraftInfo> {
        val hgi = calcHeightOrGraftInto(from, upCls)
        cacheHeightOrGraftInto(prev, from, hgi)
        return hgi
    }

    private fun cacheHeightOrGraftInto(prev:List<RulePosition>, from: List<RuntimeRule>, hgis: Set<HeightGraftInfo>) {
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
    private fun calcHeightOrGraftInto(from: List<RuntimeRule>, upCls: Set<ClosureItem>): Set<HeightGraftInfo> {
        // have to ensure somehow that this grows into prev
        // have to do closure down from prev,
        // upCls is the closure down from prev
        val upFilt = upCls.filter { from.contains(it.rulePosition.item) }
        val res = upFilt.flatMap { clsItem ->
            val parent = clsItem.rulePosition
            val upLhs = clsItem.parentItem?.lookaheadSet ?: LookaheadSet.UP
            val pns = parent.next()
            pns.map { parentNext ->
                val lhsc = this.stateSet.buildCache.firstOf(parentNext, upLhs.content)// this.stateSet.expectedAfter(parentNext)
                val lhs = this.createLookaheadSet(lhsc)
                HeightGraftInfo(listOf(parent), listOf(parentNext), lhs, upLhs)
            }
        }
        val grouped = res.groupBy { listOf(it.parent, it.parentNext) }//, it.lhs) }
            .map {
                val parent = it.key[0] as List<RulePosition>
                val parentNext = it.key[1] as List<RulePosition>
                val lhs = createLookaheadSet(it.value.flatMap { it.lhs.content }.toSet())
                val upLhs = createLookaheadSet(it.value.flatMap { it.upLhs.content }.toSet())
                HeightGraftInfo((parent), (parentNext), lhs, upLhs)
            }
        val grouped2 = grouped.groupBy { listOf(it.lhs, it.upLhs) }
            .map {
                val parent = it.value.flatMap { it.parent }.toSet().toList()
                val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
                val lhs = it.key[0] as LookaheadSet
                val upLhs = it.key[1] as LookaheadSet
                HeightGraftInfo(parent, parentNext, lhs, upLhs)
            }
        return grouped2.toSet()
    }


    fun rulePositionsForStates(): List<List<RulePosition>> {
        val rulePositionLists = createMergedListsOfRulePositions()
        val terminalRPs = this.stateSet.usedTerminalRules.map { listOf(RulePosition(it, 0, RulePosition.END_OF_RULE)) } //TODO:EMBEDDED
        return listOf(this.stateSet.startState.rulePositions) + rulePositionLists + terminalRPs
    }

    internal fun createMergedListsOfRulePositions(): List<List<RulePosition>> {
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
                when {
                    RuntimeRuleKind.GOAL == rp.runtimeRule.kind -> {
                        // do nothing,
                    }
                    RulePosition.START_OF_RULE == rp.position -> {
                        // do nothing, SOR position RPs are not made into a state, ther than for GOAL
                    }
                    else -> {
                        val key = rp.runtimeRule.item(rp.option, RulePosition.START_OF_RULE)!!
                        val list = map[key] ?: run {
                            map[key] = mutableListOf<RulePosition>()
                            map[key]!!
                        }
                        list.add(rp)
                    }
                }
            }
        }
        return map
    }

    fun rulePositionsAreSameState(rp1: RulePosition, rp2: RulePosition): Boolean {
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
                            rp1.position == rp2.position && (0 until pIndex).all { p ->
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
                            (0 until pIndex).all { p ->
                                rp1.runtimeRule.item(rp1.option, p) == rp2.runtimeRule.item(rp2.option, p)
                            }
                        }
                        RuntimeRuleRhsItemsKind.LIST -> rp1.runtimeRule.item(rp1.option, rp1.position) == rp2.runtimeRule.item(rp2.option, rp1.position)
                    }
                    else -> error("should not happen")
                }
            }
            else -> error("should not happen")
        } //TODO: only do firstOf if true
        val rp1NextItems = firstOf(rp1, emptySet())//rp1.next().mapNotNull { it.item }
        val rp2NextItems = firstOf(rp1, emptySet())//rp2.next().mapNotNull { it.item } need closure !
        //return sameItemsUntil && rp1NextItems.isNotEmpty() && rp1NextItems == rp2NextItems
        return sameItemsUntil && rp1NextItems == rp2NextItems
    }

    private fun closureForRulePosition(rp: RulePosition, ifReachEnd: LookaheadSet): Set<ClosureItem> {
        val cls = _dnClosureForRulePosition[rp]
        return if (null == cls) {
            val cl = calcClosure(rp, ifReachEnd)
            _dnClosureForRulePosition[rp] = cl
            cl
        } else {
            cls
        }
    }

    internal fun calcClosure(rp: RulePosition, upLhs: LookaheadSet): Set<ClosureItem> {
        val lhsc = calcLookaheadDown(rp, upLhs.content)
        val lhs = this.stateSet.createLookaheadSet(lhsc)
        return calcClosure(ClosureItem(null, rp, null, lhs))
    }

    fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
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
    internal fun calcClosure(item: ClosureItem, items: MutableSet<ClosureItem> = mutableSetOf()): Set<ClosureItem> {
        return when {
            item.rulePosition.isAtEnd -> items
            items.any {
                it.rulePosition == item.rulePosition &&
                        it.lookaheadSet == item.lookaheadSet &&
                        it.parentItem?.lookaheadSet == item.parentItem?.lookaheadSet
            } -> items
            else -> {
                items.add(item)
                for (rr in item.rulePosition.items) {
                    when (rr.kind) {
                        RuntimeRuleKind.TERMINAL,
                        RuntimeRuleKind.EMBEDDED -> {
                            //val chItem = ClosureItem(item, RulePosition(rr, 0, RulePosition.END_OF_RULE), item.lookaheadSet)
                            //items.add(chItem)
                        }
                        RuntimeRuleKind.GOAL,
                        RuntimeRuleKind.NON_TERMINAL -> {
                            val chRps = rr.rulePositionsAt[0]
                            for (chRp in chRps) {
                                val chNext = chRp.next()
                                for (chNx in chNext) {
                                    val lh = firstOf(chNx, item.lookaheadSet.content)
                                    val lhs = this.stateSet.runtimeRuleSet.createLookaheadSet(lh)
                                    val ci = ClosureItem(item, chRp, chNx, lhs)
                                    calcClosure(ci, items)
                                }
                            }
                        }
                    }
                }
                items
            }
        }
    }

    /*
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     */

    override fun firstOf(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), BooleanArray(this.stateSet.runtimeRuleSet.runtimeRules.size))
                when (res.needsNext) {
                    false -> res.result
                    else -> res.result + ifReachEnd
                }
            }
        }
    }

    fun firstOfRpNotEmpty(rulePosition: RulePosition, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            val result = mutableSetOf<RuntimeRule>()

            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) {
                val nrps = mutableSetOf<RulePosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    val item = rp.item
                    when {
                        //item is null only when rp.isAtEnd
                        null == item /*rp.isAtEnd*/ -> needsNext = true
                        item.isEmptyRule -> nrps.addAll(rp.next())
                        else -> when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> result.add(item)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LC1)
                                val f = (embSS.buildCache as BuildCacheLC1).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                                result.addAll(f.result)
                                if (f.needsNext) {
                                    needsNext = true
                                }
                            }
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val f = firstOfNotEmpty(item, doneRp, done)
                                result.addAll(f.result)
                                if (f.needsNext) nrps.addAll(rp.next())
                            }
                        }
                    }
                }
                rps = nrps
            }
            existing = FirstOfResult(needsNext, result)
            doneRp[rulePosition] = existing
        }
        return existing
    }

    fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        //fun firstOfNotEmpty(rule: RuntimeRule): FirstOfResult {
        return when {
            0 > rule.number -> when {
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.number -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.USE_PARENT_LOOKAHEAD_RULE_NUMBER == rule.number -> TODO()
                else -> error("unsupported rule number $rule")
            }
            done[rule.number] -> _firstOfNotEmpty[rule.number] ?: FirstOfResult(false, emptySet())
            else -> {
                var result: FirstOfResult? = null//_firstOfNotEmpty[rule.number]
                if (null == result) {
                    done[rule.number] = true
                    result = firstOfNotEmptySafe(rule, doneRp, done)
                    _firstOfNotEmpty[rule.number] = result
                }
                result
            }
        }
    }

    fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        val result = mutableSetOf<RuntimeRule>()
        val pos = rule.rulePositionsAt[0]
        for (rp in pos) {
            val item = rp.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LC1)
                        val f = (embSS.buildCache as BuildCacheLC1).firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result.addAll(f.result)
                        if (f.needsNext) {
                            needsNext = true
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> result.add(item)
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(rp, doneRp, done)
                        result.addAll(f.result)
                        needsNext = needsNext || f.needsNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

}