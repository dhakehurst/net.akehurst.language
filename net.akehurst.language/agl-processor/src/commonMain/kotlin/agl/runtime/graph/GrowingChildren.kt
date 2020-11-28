/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.RuleOptionId
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.api.sppt.SPPTNode


class GrowingChildren {

    var length: Int = 0; private set
    var numberNonSkip: Int = 0; private set
    var nextInputPosition: Int = -1
    var startPosition: Int = -1

    private var _firstChild: GrowingChildNode? = null
    private var _lastChild: GrowingChildNode? = null
    private var _nextChildAlts: MutableMap<Int, MutableMap<List<RuleOptionId>, Int>>? = null

    private fun clone(): GrowingChildren {
        val cl = GrowingChildren()
        cl.length = length
        cl.numberNonSkip = numberNonSkip
        cl.nextInputPosition = nextInputPosition
        cl.startPosition = startPosition
        cl._firstChild = _firstChild
        cl._lastChild = _lastChild
        if (null != _nextChildAlts) {
            cl._nextChildAlts = mutableMapOf()
            _nextChildAlts!!.entries.forEach {
                val mapCl = it.value.toMutableMap()
                cl._nextChildAlts!![it.key] = mapCl
            }
        }
        return cl
    }

    private fun nextChildAlt(childIndex: Int, ruleOption: RuleOptionId): Int {
        return when {
            null == _nextChildAlts -> 0
            else -> {
                val m = _nextChildAlts!![childIndex]
                when {
                    null == m -> 0
                    else -> m.entries.firstOrNull { it.key.contains(ruleOption) }?.value ?: 0
                }
            }
        }
    }

    internal fun incNextChildAlt(childIndex: Int, value: List<RuleOptionId>) {
        if (null == _nextChildAlts) _nextChildAlts = mutableMapOf()
        var m = _nextChildAlts!![childIndex]
        if (null == m) {
            m = mutableMapOf()
            _nextChildAlts!![childIndex] = m
        }
        val v = m[value]
        if (null == v) {
            m[value] = 1
        } else {
            m[value] = v + 1
        }
    }

    val hasSkipAtStart: Boolean get() = null == _firstChild!!.state
    val firstChild: GrowingChildNode get() = this._firstChild!!
    fun firstNonSkipChild(ruleOption: RuleOptionId): GrowingChildNode? {
        var r = this._firstChild
        var index = 0
        while(null!=r && null==r.state) {
            r = r.next(this.nextChildAlt(index, ruleOption), ruleOption)
            index++
        }
        return r
    }
    fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren {
        val newChildren: GrowingChildren = when {
            null == _firstChild -> {
                _firstChild = GrowingChildNode(state, nextChildAlts)
                startPosition = nextChildAlts[0].startPosition
                _lastChild = _firstChild
                this
            }
            state.isGoal -> {
                val lastChild = this._lastChild!!
                when {
                    null == lastChild.state -> { //append if last is skip node
                        val nextChild = lastChild.appendRealLast(state, nextChildAlts)
                        this._lastChild = nextChild
                        this
                    }
                    lastChild.isLast -> {
                        val nextChild = lastChild.appendRealLast(state, nextChildAlts)
                        this._lastChild = nextChild
                        this
                    }
                    else -> { //else alternative Goal
                        val nextChild = lastChild.appendAlternativeLast(this, this.length, state, nextChildAlts)
                        this._lastChild = nextChild
                        this
                    }
                }
            }
            else -> {
                val lastChild = this._lastChild!!
                val res = this.clone()
                when {
                    lastChild.isLast -> {
                        val nextChild = lastChild.appendRealLast(state, nextChildAlts)
                        res._lastChild = nextChild
                    }
                    else -> {
                        val nextChild = lastChild.appendAlternativeLast(res, res.length, state, nextChildAlts)
                        res._lastChild = nextChild
                    }
                }
                res
            }
        }
        newChildren.length++
        newChildren.numberNonSkip++
        newChildren.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
        return newChildren

    }

    fun appendSkipIfNotEmpty(skipChildren: List<SPPTNode>): GrowingChildren {
        if (skipChildren.isEmpty()) {
            //do nothing
        } else {
            if (null == _firstChild) {
                _firstChild = GrowingChildNode(null, skipChildren)
                startPosition = skipChildren[0].startPosition
                _lastChild = _firstChild
            } else {
                val nextChild = GrowingChildNode(null, skipChildren)
                _lastChild!!.nextChild = nextChild
                _lastChild = nextChild
            }
            nextInputPosition = skipChildren.last().nextInputPosition
        }
        this.length++
        return this
    }

    operator fun get(ruleOption: RuleOptionId): List<SPPTNode> {
        return when {
            null == _firstChild -> emptyList()
            else -> {
                val res = mutableListOf<SPPTNode>()
                var index = 1
                var n: GrowingChildNode? = _firstChild
                while (n != _lastChild && null != n) {
                    res.addAll(n[ruleOption])
                    n = n.next(this.nextChildAlt(index, ruleOption), ruleOption)
                    index++
                }
                if (null == n) {
                    //this list of children died
                    error("TODO")
                } else {
                    res.addAll(_lastChild!![ruleOption])
                }
                res
            }
        }
    }

    override fun toString(): String = when {
        null == _firstChild -> "{}"
        else -> {
            val res = mutableMapOf<RulePosition, List<String>>()
            val initialSkip = mutableListOf<String>()
            var sn = _firstChild
            while (sn != null && null == sn.state) {
                initialSkip.add(sn.children.joinToString() { it.name })
                sn = sn.nextChild
            }
            val rps = sn?.state?.rulePositions ?: emptyList()
            for (rp in rps) {
                res[rp] = initialSkip
                var n = sn
                var skip = ""
                var index = 0
                while (_lastChild != n && null != n) {
                    val state = n.state
                    when {
                        null == state -> skip += n.children.joinToString() //skipNodes
                        else -> {
                            res[rp] = res[rp]!! + n[rp.identity].joinToString() { it.name }
                        }
                    }
                    n = n.next(this.nextChildAlt(index, rp.identity), rp.identity)
                    index++
                }
                if (null == n) {
                    //this list of children died
                    res[rp] = res[rp]!! + "-X"
                } else {
                    val state = n.state
                    when {
                        null == state -> skip += n.children.joinToString() //skipNodes
                        else -> {
                            res[rp] = res[rp]!! + n[rp.identity].joinToString() { it.name }
                        }
                    }
                }
            }
            res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key.runtimeRule.tag}[${it.key.option}]) -> ${it.value.joinToString()}" }.joinToString(separator = "\n")
        }
    }

    fun concatenate(other: GrowingChildren) {
        this._lastChild!!.nextChild = other.firstChild
        this._lastChild = other._lastChild
        this.length += other.length
        this.numberNonSkip += other.numberNonSkip
        this.nextInputPosition = other.nextInputPosition
        if (null!=other._nextChildAlts) {
            TODO()
        }
    }

}