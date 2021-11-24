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


internal class GrowingChildren {

    var length: Int = 0; private set
    var numberNonSkip: Int = 0; private set
    var nextInputPosition: Int = -1
    var startPosition: Int = -1

    val containedFor = mutableSetOf<List<RuleOptionId>>()

    private var _firstChild: GrowingChildNode? = null
    private var _firstChildAlternatives: MutableMap<List<RuleOptionId>?, MutableList<GrowingChildNode>>? = null
    //private var _lastChild: GrowingChildNode? = null
    private var _lastChildAlternatives: MutableMap<List<RuleOptionId>?, GrowingChildNode> = mutableMapOf()
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
                val l = it.value.toMutableList() // copy the list of nodes, it can get modified in the original
                cl._firstChildAlternatives!![it.key] = l
            }
        }
        //cl._lastChild = this._lastChild
        cl._lastChildAlternatives.putAll(this._lastChildAlternatives)
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

    //val lastChild: GrowingChildNode? get() = this._lastChild

    val matchedTextLength get() = this.nextInputPosition - this.startPosition

    fun firstChild(ruleOption: RuleOptionId): GrowingChildNode? = when {
        null == this._firstChildAlternatives -> this._firstChild
        null == ruleOption -> null //skip will not have alternatives
        else -> this._firstChildAlternatives!!.entries.firstOrNull {
            it.key?.contains(ruleOption) ?: false
        }?.value?.get(this.nextChildAlt(0, ruleOption))
    }

    fun lastChild(ruleOption: List<RuleOptionId>): GrowingChildNode? = when {
        //null == this._lastChildAlternatives -> this._lastChild
        null == ruleOption -> null //skip will not have alternatives
        else -> this._lastChildAlternatives.get(ruleOption)
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

    fun setFirstChild(ruleOption: List<RuleOptionId>, node: GrowingChildNode) {
        when {
            this.isEmpty -> this._firstChild = node
            else -> {
                _firstChildAlternatives = mutableMapOf()
                val alts = mutableListOf(this._firstChild!!)
                this._firstChildAlternatives!![ruleOption] = alts
                this._firstChild = null
                alts.add(node)
            }
        }
    }

    private fun setLastChild(ruleOption: List<RuleOptionId>, node: GrowingChildNode) {
        this._lastChildAlternatives[ruleOption] = node
    }

    fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren? {
        val ruleOption = state.rulePositionIdentity
        containedFor.add(ruleOption)
        val alternativeNextChild = GrowingChildNode(state, nextChildAlts)
        return when {
            isEmpty -> {
               // _firstChild = GrowingChildNode(state, nextChildAlts)
                val node = GrowingChildNode(state, nextChildAlts)
                this.setFirstChild(ruleOption, node)
                startPosition = nextChildAlts[0].startPosition
               // _lastChild = _firstChild
                this.setLastChild(ruleOption, _firstChild!!)
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
                        val res = this.clone()
                        res._firstChildAlternatives = mutableMapOf()
                        val alts = mutableListOf(res._firstChild!!)
                        res._firstChildAlternatives!![ruleOption] = alts
                        res._firstChild = null
                        alts.add(alternativeNextChild)
                        res.incNextChildAlt(0, ruleOption)
                        res.setLastChild(ruleOption,alternativeNextChild)
                        // because its a duplicate of existing goal
                        // will not changed the length or numberNonSkip
                        res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                        res
                    }
                    else -> { // must be initial skip and duplicate of existing goal
                        val changeLength = lisc.isLast
                        val appended = lisc.appendLast(this, this.length - 1, alternativeNextChild)
                        appended?.let {
                            this.setLastChild(ruleOption,it)
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
                val nextChild = alternativeNextChild
                for(lastChild in res._lastChildAlternatives.values) {
                    val appended = lastChild.appendLast(res, res.length, nextChild)
                    appended?.let {
                        res.setLastChild(ruleOption, it)
                    }
                }
                res.length++
                res.numberNonSkip++
                //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                res
            }
        }
    }

    fun appendSkipIfNotEmpty(state: ParserState, skipChildren: List<SPPTNode>): GrowingChildren {
        val ruleOption = state.rulePositionIdentity
        val nextChild =  GrowingChildNode(null, skipChildren)
        return if (skipChildren.isEmpty()) {
            //do nothing
            this
        } else {
            if (isEmpty) {
                //this._firstChild = GrowingChildNode(null, skipChildren)
                this.setFirstChild(ruleOption, nextChild)
                this.startPosition = skipChildren[0].startPosition
                //this._lastChild = _firstChild
                this.setLastChild(ruleOption,nextChild)
                this.length++
                this.nextInputPosition = skipChildren.last().nextInputPosition
                this
            } else {
                val res = this.clone()
                for(lastChild in res._lastChildAlternatives.values) {
                    val appended = lastChild.appendLast(res, res.length, nextChild)
                    appended?.let {
                        res.setLastChild(lastChild.state!!.rulePositionIdentity, it)
                    }
                }
                res.length++
                //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                res.nextInputPosition = skipChildren[0].nextInputPosition //FIXME: not really correct, are all children same length?
                res
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
                while (null != n && _lastChildAlternatives.values.contains(n).not()) {
                    res.addAll(n[ruleOption])
                    n = n.next(this.nextChildAlt(index, ruleOption), ruleOption)
                    index++
                }
                if (null == n) {
                    //this list of children died
                    error("TODO")
                } else {
                   TODO()// res.addAll(_lastChild!![ruleOption])
                }
                res
            }
        }
    }

    fun concatenate(other: GrowingChildren) {
        for(ruleOption in other.containedFor) {
            val lastChild = this.lastChild(ruleOption)!!
            lastChild.nextChild = other._firstChild
            if (null != other._firstChildAlternatives) {
                lastChild.nextChildAlternatives = mutableMapOf()
                other._firstChildAlternatives!!.entries.forEach {
                    val l = it.value.toMutableList()
                    lastChild.nextChildAlternatives!![it.key] = l
                }
            }
            if (null != other._nextChildAlts) {
                other._nextChildAlts!!.forEach {
                    this._nextChildAlts!![it.key + this.length] = it.value.toMutableMap()
                }
            }
            this._lastChildAlternatives = other._lastChildAlternatives
            this.length += other.length
            this.numberNonSkip += other.numberNonSkip
            this.nextInputPosition = other.nextInputPosition
        }
    }

    fun mergeOrDropWithPriority(other: GrowingChildren) {
        when{
            other.isEmpty -> Unit
            this.isEmpty -> {
                _firstChild = other._firstChild
                startPosition = other.startPosition
                this._lastChildAlternatives = other._lastChildAlternatives
                this.length = other.length
                this.numberNonSkip = other.numberNonSkip
                this.nextInputPosition = other.nextInputPosition //FIXME: not really correct, are all children same length?
            }
            this.startPosition != other.startPosition -> error("Cannot merge children starting at a different position")
            else -> {
                for(ruleOption in other.containedFor) {
                    TODO()
                }
            }
        }
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
                null != _firstChildAlternatives -> _firstChildAlternatives!!.entries.mapNotNull { it.key }.flatten()
                null == sn -> when {
                    //lastSkip.next has alts
                    null != lastSkip!!.nextChildAlternatives -> lastSkip.nextChildAlternatives!!.entries.mapNotNull { it.key }.flatten()
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
                        while (null != n && _lastChildAlternatives.values.contains(n).not()) {
                            val state = n.state
                            when {
                                null == state -> skip += n.children.joinToString { it.name } //skipNodes
                                else -> {
                                    if (skip.isNotBlank()) {
                                        res[rpId] = res[rpId]!! + skip
                                        skip = ""
                                    }
                                    res[rpId] = res[rpId]!! + n[rpId].joinToString() { "${it.name}|${it.option}" }
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
                                    res[rpId] = res[rpId]!! + n[rpId].joinToString() { "${it.name}|${it.option}" }
                                }
                            }
                        }
                    }
                }
            }

            res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key?.runtimeRule?.tag}|${it.key?.option}) -> ${it.value.joinToString()}" }.joinToString(separator = "\n")
        }
    }

}