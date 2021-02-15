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

class BuildCache(
    val stateSet: ParserStateSet
) {

    private var _cacheOff = true
    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItem>>()
    private val _closureForRulePosition = mutableMapOf<RulePosition, Set<ClosureItem>>()
    //could use Map RuleNumber->FirstOfResult, but index with RuleNumber should be faster.
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })
    private val _parentPosition = mutableMapOf<RuntimeRule, Set<RulePosition>>()

    private val _widthInto = mutableMapOf<List<RulePosition>, Set<WidthIntoInfo>>()
    // Pair( listOf(RulePositions-of-previous-state), listOf(RuntimeRules-of-fromState) ) -> setOf(possible-HeightGraftInfo)
    private val _heightOrGraftInto = mutableMapOf<Pair<List<RulePosition>,List<RuntimeRule>>, Set<HeightGraftInfo>>()

    fun closureLR0(rp: RulePosition): Set<RulePosition> {
        return if (_cacheOff) {
            calcClosureLR0(rp)
        } else {
            _calcClosureLR0[rp] ?: run {
                val v = calcClosureLR0(rp)
                _calcClosureLR0[rp] = v
                v
            }
        }
    }

    fun on() {
        _cacheOff = false
    }

    fun clearAndOff() {
        _calcClosureLR0.clear()
        _closureItems.clear()
        _cacheOff = true
    }

    fun traverseRulePositions() {
        val goalRule = this.stateSet.startState.runtimeRules.first()
        this.traverseRulePositionsForRule(goalRule, listOf(Pair(null, LookaheadSet.UP)), mutableMapOf())
    }

    private fun traverseRulePositionsForRule(runtimeRule: RuntimeRule, prevPositions:List<Pair<RulePosition?,LookaheadSet>>, done:MutableMap<Pair<RulePosition?,LookaheadSet>,Boolean>) {
        val parent = prevPositions.last()
        when {
            done.containsKey(parent) -> TODO()
            else ->{
                done[prevPositions.last()] = true
                when(runtimeRule.kind) {
                    RuntimeRuleKind.GOAL -> {
                        for (rp in runtimeRule.rulePositions) {
                            val itemRule = rp.item
                            when {
                                null==itemRule -> null // can't traverse down
                                else -> {
                                    val next = rp.next().first()
                                    val nextFstOf = firstOf(next,parent.second.content)
                                    val nextFstOfLhs = this.stateSet.createLookaheadSet(nextFstOf)

                                    // cache width info
                                    this._widthInto[listOf(rp)] = setOf(WidthIntoInfo(itemRule.rulePositions.last(),nextFstOfLhs))

                                    //traverse down
                                    val newPP = Pair(rp, parent.second)
                                    traverseRulePositionsForRule(itemRule, prevPositions + newPP, done)
                                }
                            }
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> TODO()
                    RuntimeRuleKind.EMBEDDED -> TODO()
                    RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind){
                        RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                        RuntimeRuleRhsItemsKind.CHOICE -> {
                            // state = merge of all choices atEnd
                            // transitions are H/G for any prev
                            for (rp in runtimeRule.rulePositions) {
                                val itemRule = rp.item
                                when {
                                    null==itemRule -> null // can't traverse down
                                    else -> {
                                        val newPP = Pair(rp, parent.second)
                                        traverseRulePositionsForRule(itemRule, prevPositions + newPP, done)
                                    }
                                }
                            }
                        }
                        RuntimeRuleRhsItemsKind.CONCATENATION -> {
                            for (rp in runtimeRule.rulePositions) {
                                // rp becomes a state  TODO: merging
                                // possible Prevs = (prev, ifAtEnd)
                                val itemRule = rp.item
                                when {
                                    null==itemRule -> null // must be atEnd, can't traverse down
                                    else -> {
                                        val next = rp.next().first()
                                        val nextFstOf = firstOf(next,parent.second.content)
                                        val nextFstOfLhs = this.stateSet.createLookaheadSet(nextFstOf)

                                        // cache width info
                                        this._widthInto[listOf(rp)] = setOf(WidthIntoInfo(itemRule.rulePositions.last(),nextFstOfLhs))

                                        //traverse down
                                        val newPP = Pair(rp, nextFstOfLhs)
                                        traverseRulePositionsForRule(itemRule, prevPositions + newPP, done)
                                    }
                                }
                            }
                        }
                        RuntimeRuleRhsItemsKind.LIST -> TODO()
                    }
                }
            }
        }
    }

    fun widthInto(fromState: ParserState): Set<WidthIntoInfo> {
        return this._widthInto[fromState.rulePositions] ?: run {
            val calc = calcWidthInto(fromState.rulePositions)
            this._widthInto[fromState.rulePositions] = calc
            calc
        }
    }

    fun heightOrGraftInto(prevState: ParserState, fromState: ParserState): Set<HeightGraftInfo> {
        val key = Pair(prevState.rulePositions,fromState.runtimeRules)
        return this._heightOrGraftInto[key] ?: run {
            val calc = calcHeightOrGraftInto(prevState.rulePositions,fromState.runtimeRules)
            this._heightOrGraftInto[key] = calc
            calc
        }
    }

    private fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.stateSet.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs

    private fun calcWidthInto(from: List<RulePosition>): Set<WidthIntoInfo> {
        // get lh by closure on prev
        // upLhs can always be LookaheadSet.UP because the actual LH is carried at runtime
        // thus we don't need prevState in to compute width targets
/*
        val prevRps = prevState?.rulePositions
        val upLhs = when (prevRps) {
            null -> LookaheadSet.UP
            else -> {
                val upFilt = this.stateSet.buildCache.closureItems(prevState, this)
                val lhsc = upFilt.flatMap { it.lookaheadSet.content }.toSet() //TODO: should we combine these or keep sepraate?
                this.createLookaheadSet(lhsc)
            }
        }
*/
        val cls = from.flatMap { this.stateSet.buildCache.calcClosure(it, LookaheadSet.UP)}
        val filt = cls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
        val grouped = filt.groupBy { it.rulePosition.item!! }.map {
            val rr = it.key
            val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
            val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
            val lhs = this.createLookaheadSet(lhsc)
            WidthIntoInfo(rp, lhs)
        }.toSet()
        //don't group them, because we need the info on the lookahead for the runtime calc of next lookaheads
        return grouped
    }

    //for graft, previous must match prevGuard, for height must not match
    private fun calcHeightOrGraftInto(prev: List<RulePosition>, from: List<RuntimeRule>): Set<HeightGraftInfo> {
        // have to ensure somehow that this grows into prev
        //have to do closure down from prev,
        val upFilt = this.calcClosureItems(prev, from)
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
                    if (allReadyMerged.contains(head)){
                        //skip it
                    } else {
                        result.add(listOf(head))
                    }
                }
            }
        }
        return result
    }

    private fun createRulePositionsIndexByFirstItem() : Map<RuntimeRule, List<RulePosition>> {
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

    private fun calcClosureItems(prevRps: List<RulePosition>, from: List<RuntimeRule>): List<ClosureItem> {
        val upCls = prevRps.flatMap { this.closureForRulePosition(it, LookaheadSet.UP) }.toSet()
        val upFilt = upCls.filter { from.contains(it.rulePosition.item) }
        return upFilt
    }

    private fun closureForRulePosition(rp:RulePosition,ifReachEnd: LookaheadSet) : Set<ClosureItem> {
        val cls = _closureForRulePosition[rp]
        return if (null==cls) {
            val cl = calcClosure(rp,ifReachEnd)
            _closureForRulePosition[rp] = cl
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

    private fun calcClosureLR0(rp: RulePosition): Set<RulePosition> {
        var cl = _calcClosureLR0[rp]
        if (null == cl) {
            cl = calcClosureLR0(rp, mutableSetOf())
            _calcClosureLR0[rp] = cl
        }
        return cl
    }

    private fun calcClosureLR0(rp: RulePosition, items: MutableSet<RulePosition>): Set<RulePosition> {
        return when {
            items.contains(rp) -> {
                items
            }
            else -> {
                items.add(rp)
                val itemRps = rp.items.flatMap {
                    it.rulePositionsAt[0]
                }.toSet()
                itemRps.forEach { childRp ->
                    calcClosureLR0(childRp, items)
                }
                items
            }
        }
    }

    /*
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     */

    fun firstOf(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
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
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!)
                                val f = embSS.buildCache.firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
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
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!)
                        val f = embSS.buildCache.firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
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