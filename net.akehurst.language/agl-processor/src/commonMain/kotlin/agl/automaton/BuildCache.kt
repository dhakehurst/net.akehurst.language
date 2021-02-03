package agl.automaton

import net.akehurst.language.agl.automaton.ClosureItem
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.LookaheadSet

class BuildCache {

    private var _cacheOff = true
    private val _cache = mutableMapOf<Pair<ParserState, ParserState>, List<ClosureItem>>()

    fun closureItems(prevState: ParserState, thisState: ParserState): List<ClosureItem> {
        return if (_cacheOff) {
            calcClosureItems(prevState, thisState)
        } else {
            val key = Pair(prevState, thisState)
            _cache[key] ?: run {
                val v = calcClosureItems(prevState, thisState)
                _cache[key] = v
                v
            }
        }
    }

    fun on() {
        _cacheOff = false
    }

    fun clearAndOff() {
        _cache.clear()
        _cacheOff = true
    }

    private fun calcClosureItems(prevState: ParserState, thisState: ParserState): List<ClosureItem> {
        val prevRps = prevState.rulePositions
        val upCls = prevRps.flatMap { thisState.stateSet.calcClosure(it, LookaheadSet.UP) }.toSet()
        val upFilt = upCls.filter { thisState.runtimeRules.contains(it.rulePosition.item) }
        return upFilt
    }

}