package agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.collections.MapIntTo

class CompletedNodesStore(val numRuntimeRules: Int, val inputLength:Int) {

    private val _map = MapIntTo<MapIntTo<SPPTNode>>()

    fun clear() {
        this._map.clear()
    }

    operator fun set(runtimeRule: RuntimeRule, inputPosition: Int, value: SPPTNode) {
        var m2 = _map[runtimeRule.number]
        if (null==m2) {
            m2 = MapIntTo<SPPTNode>()
            _map[runtimeRule.number] = m2
        }
        m2[inputPosition] = value
    }

    operator fun get(runtimeRule: RuntimeRule, inputPosition: Int): SPPTNode? {
        val m2 =_map[runtimeRule.number] ?: return null
        return m2[inputPosition]
    }


}