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

    //TODO: use smaller array for done, but would to map rule number!
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })

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

    internal fun calcClosure(rp: RulePosition, upLhs: LookaheadSet): Set<ClosureItem> {
        val lhsc = calcLookaheadDown(rp, upLhs.content)
        val lhs = this.stateSet.createLookaheadSet(lhsc)
        return calcClosure(ClosureItem(null, rp, null, lhs))
    }

    fun closureItems(prevState: ParserState, thisState: ParserState): List<ClosureItem> {
        return if (_cacheOff) {
            calcClosureItems(prevState, thisState)
        } else {
            val key = Pair(prevState, thisState)
            _closureItems[key] ?: run {
                val v = calcClosureItems(prevState, thisState)
                _closureItems[key] = v
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

    private fun calcClosureItems(prevState: ParserState, thisState: ParserState): List<ClosureItem> {
        val prevRps = prevState.rulePositions
        val upCls = prevRps.flatMap { this.calcClosure(it, LookaheadSet.UP) }.toSet()
        val upFilt = upCls.filter { thisState.runtimeRules.contains(it.rulePosition.item) }
        return upFilt
    }

    private fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
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

    data class FirstOfResult(
        val needsNext: Boolean,
        val result: Set<RuntimeRule>
    )

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