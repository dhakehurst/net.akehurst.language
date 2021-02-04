package agl.automaton

import net.akehurst.language.agl.automaton.ClosureItem
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition

class BuildCache {

    private var _cacheOff = true

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private val _closureItems = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItem>>()

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
        val upCls = prevRps.flatMap { thisState.stateSet.calcClosure(it, LookaheadSet.UP) }.toSet()
        val upFilt = upCls.filter { thisState.runtimeRules.contains(it.rulePosition.item) }
        return upFilt
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

}