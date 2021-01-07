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
import net.akehurst.language.api.sppt.SPPTNode


class GrowingChildren {

    var length: Int = 0; private set
    var numberNonSkip: Int = 0; private set
    var nextInputPosition: Int = -1
    var startPosition: Int = -1

    private var _firstChild: GrowingChildNode? = null
    private var _firstChildAlternatives: MutableMap<List<RuleOptionId>, MutableList<GrowingChildNode>>? = null
    private var _lastChild: GrowingChildNode? = null
    private var _nextChildAlts: MutableMap<Int, MutableMap<List<RuleOptionId>, Int>>? = null

    private fun clone(): GrowingChildren {
        val cl = GrowingChildren()
        cl.length = this.length
        cl.numberNonSkip = this.numberNonSkip
        cl.nextInputPosition = this.nextInputPosition
        cl.startPosition = this.startPosition
        cl._firstChild = this._firstChild
        if (null != this._firstChildAlternatives) {
            cl._firstChildAlternatives = mutableMapOf()
            this._firstChildAlternatives!!.entries.forEach {
                val l = it.value.toMutableList()
                cl._firstChildAlternatives!![it.key] = l
            }
        }
        cl._lastChild = this._lastChild
        if (null != this._nextChildAlts) {
            cl._nextChildAlts = mutableMapOf()
            this._nextChildAlts!!.entries.forEach {
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
    internal fun setNextChildAlt(childIndex: Int, value: List<RuleOptionId>, altNum:Int) {
        if (null == _nextChildAlts) _nextChildAlts = mutableMapOf()
        var m = _nextChildAlts!![childIndex]
        if (null == m) {
            m = mutableMapOf()
            _nextChildAlts!![childIndex] = m
        }
        m[value] = altNum
    }

    val isEmpty: Boolean get() = null == _firstChild && null == _firstChildAlternatives
    val hasSkipAtStart: Boolean get() = null == _firstChild!!.state

    val lastInitialSkipChild: GrowingChildNode?
        get() = when {
            isEmpty -> null
            null != this._firstChildAlternatives -> null //skips never have alternatives
            else -> {
                var n = this._firstChild
                var last: GrowingChildNode? = null
                while (null != n && null == n.state) {
                    last = n
                    n = n.nextChild
                }
                last
            }
        }

    val lastChild: GrowingChildNode? get() = this._lastChild

    fun firstChild(ruleOption: RuleOptionId?): GrowingChildNode? = when {
        null == this._firstChildAlternatives -> this._firstChild
        null == ruleOption -> null //skip will not have alternatives
        else -> this._firstChildAlternatives!!.entries.firstOrNull {
            it.key.contains(ruleOption)
        }?.value?.get(this.nextChildAlt(0, ruleOption))
    }

    fun firstNonSkipChild(ruleOption: RuleOptionId): GrowingChildNode? {
        var r = this.firstChild(ruleOption)
        var index = 1
        while (null != r && null == r.state) {
            r = r.next(this.nextChildAlt(index, ruleOption), ruleOption)
            index++
        }
        return r
    }

    fun setFirstChildAlternative() {

    }

    fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren? {
        return when {
            isEmpty -> {
                _firstChild = GrowingChildNode(state, nextChildAlts)
                startPosition = nextChildAlts[0].startPosition
                _lastChild = _firstChild
                this.length++
                this.numberNonSkip++
                //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                this.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                this
            }
            state.isGoal -> {
                val lisc = lastInitialSkipChild
                when {
                    null == lisc -> { // must be duplicate of firstNode which is goal
                        //TODO: if nextInputPosition of duplicate ??
                        val alternativeNextChild = GrowingChildNode(state, nextChildAlts)
                        val res = this.clone()
                        res._firstChildAlternatives = mutableMapOf()
                        val alts = mutableListOf(res._firstChild!!)
                        res._firstChildAlternatives!![state.rulePositionIdentity] = alts
                        res._firstChild = null
                        alts.add(alternativeNextChild)
                        res.incNextChildAlt(0, state.rulePositionIdentity)
                        res._lastChild = alternativeNextChild
                        // because its a duplicate of existing goal
                        // will not changed the length or numberNonSkip
                        res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                        res
                    }
                    else -> { // must be initial skip and duplicate of existing goal
                        val changeLength = lisc.isLast
                        val appended = lisc.appendLast(this, this.length - 1, state, nextChildAlts)
                        appended?.let {
                            this._lastChild = it
                            if (changeLength) {
                                this.length++
                                this.numberNonSkip++
                            } else {
                                // because its a duplicate of existing goal
                                // will not changed the length or numberNonSkip
                            }
                            this.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                            this
                        }
                    }
                }
            }
            else -> {
                val res = this.clone()
                val lastChild = res._lastChild!!
                val appended = lastChild.appendLast(res, res.length, state, nextChildAlts)
                appended?.let {
                    res._lastChild = it
                    res.length++
                    res.numberNonSkip++
                    //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                    res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                    res
                }
            }
        }
    }

    fun appendSkipIfNotEmpty(skipChildren: List<SPPTNode>): GrowingChildren {
        return if (skipChildren.isEmpty()) {
            //do nothing
            this
        } else {
            if (isEmpty) {
                this._firstChild = GrowingChildNode(null, skipChildren)
                this.startPosition = skipChildren[0].startPosition
                this._lastChild = _firstChild
                this.length++
                this.nextInputPosition = skipChildren.last().nextInputPosition
                this
            } else {
                val lastChild = this.lastChild!!
                when {
                    lastChild.isLast -> {
                        val res = this.clone()
                        val nextChild = GrowingChildNode(null, skipChildren)
                        res._lastChild!!.nextChild = nextChild
                        res._lastChild = nextChild
                        res.length++
                        res.nextInputPosition = skipChildren.last().nextInputPosition
                        res
                    }
                    else -> {
                        val lastNext = lastChild.nextChild
                        when {
                            null != lastNext -> when { //one alt
                                lastNext.nextInputPosition == skipChildren.last().nextInputPosition -> {
                                    //ignore it, use existing
                                    val res = this.clone()
                                    res._lastChild = lastNext
                                    res.length++
                                    res.nextInputPosition = skipChildren.last().nextInputPosition
                                    res
                                }
                                else -> {
                                    error("TODO got another skip?")
                                }
                            }
                            else -> { //multiple alts
                                error("TODO got another skip?")
                            }
                        }
                    }
                }
            }
        }
    }

    operator fun get(ruleOption: RuleOptionId): List<SPPTNode> {
        return when {
            isEmpty -> emptyList()
            else -> {
                val res = mutableListOf<SPPTNode>()
                var n: GrowingChildNode? = firstChild(ruleOption)
                var index = 1
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

    fun concatenate(other: GrowingChildren) {
        val lasChild = this._lastChild!!
        lasChild.nextChild = other._firstChild
        if (null != other._firstChildAlternatives) {
            lasChild.nextChildAlternatives = mutableMapOf()
            other._firstChildAlternatives!!.entries.forEach {
                val l = it.value.toMutableList()
                lasChild.nextChildAlternatives!![it.key] = l
            }
        }
        if (null != other._nextChildAlts) {
            other._nextChildAlts!!.forEach {
                this._nextChildAlts!![it.key + this.length] = it.value.toMutableMap()
            }
        }
        this._lastChild = other._lastChild
        this.length += other.length
        this.numberNonSkip += other.numberNonSkip
        this.nextInputPosition = other.nextInputPosition
    }

    override fun toString(): String = when {
        isEmpty -> "{}"
        else -> {
            val res = mutableMapOf<RuleOptionId?, List<String>>()
            val initialSkip = mutableListOf<String>()

            var sn = _firstChild
            var lastSkip = sn
            while (sn != null && null == sn.state) {
                initialSkip.add(sn.children.joinToString() { it.name })
                lastSkip = sn
                sn = sn.nextChild
            }
            val rpIds = when {
                // there are no skips
                null != _firstChildAlternatives -> _firstChildAlternatives!!.entries.flatMap { it.key }
                null == sn -> when {
                    //lastSkip.next has alts
                    null != lastSkip!!.nextChildAlternatives -> lastSkip.nextChildAlternatives!!.entries.flatMap { it.key }
                    //nothing after skips
                    else -> emptyList()
                }
                //sn (lastSkip.next) is the first nonSkip node
                else -> sn.state?.rulePositionIdentity ?: emptyList()
            }
            when {
                rpIds.isEmpty() -> res[null] = initialSkip
                else -> {
                    for (rpId in rpIds) {
                        res[rpId] = initialSkip
                        var n = firstNonSkipChild(rpId)
                        var skip = ""
                        var index = 1
                        while (_lastChild != n && null != n) {
                            val state = n.state
                            when {
                                null == state -> skip += n.children.joinToString { it.name } //skipNodes
                                else -> {
                                    if (skip.isNotBlank()) {
                                        res[rpId] = res[rpId]!! + skip
                                        skip = ""
                                    }
                                    res[rpId] = res[rpId]!! + n[rpId].joinToString() { it.name }
                                }
                            }
                            n = n.next(this.nextChildAlt(index, rpId), rpId)
                            index++
                        }
                        if (null == n) {
                            //this list of children died
                            res[rpId] = res[rpId]!! + "-X"
                        } else {
                            val state = n.state
                            when {
                                null == state -> {
                                    skip += n.children.joinToString { it.name } //skipNodes
                                    if (skip.isNotBlank()) {
                                        res[rpId] = res[rpId]!! + skip
                                        skip = ""
                                    }
                                }
                                else -> {
                                    if (skip.isNotBlank()) {
                                        res[rpId] = res[rpId]!! + skip
                                        skip = ""
                                    }
                                    res[rpId] = res[rpId]!! + n[rpId].joinToString() { it.name }
                                }
                            }
                        }
                    }
                }
            }

            res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key?.runtimeRule?.tag}[${it.key?.option}]) -> ${it.value.joinToString()}" }.joinToString(separator = "\n")
        }
    }


}