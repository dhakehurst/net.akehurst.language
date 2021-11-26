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

import net.akehurst.language.agl.runtime.structure.RuleOptionId
import net.akehurst.language.api.sppt.SPPTNode

//TODO: there is a lot of code here very specific to handling
// initial skip...maybe there is a better way!
internal class GrowingChildren {

    var length: Int = 0; private set
    var numberNonSkip: Int = 0; private set
    var nextInputPosition: Int = -1
    var startPosition: Int = -1

    var containedFor = mutableSetOf<Set<RuleOptionId>>(); private set

    private var _firstChild: GrowingChildNode? = null
    private var _firstChildAlternatives: MutableMap<Set<RuleOptionId>, MutableList<GrowingChildNode>>? = null

    //private var _lastChild: GrowingChildNode? = null
    private var _lastChildAlternatives: MutableMap<Set<RuleOptionId>, GrowingChildNode> = mutableMapOf()
    private var _nextChildAlts: MutableMap<Int, MutableMap<Set<RuleOptionId>, Int>>? = null

    val isEmpty: Boolean get() = null == _firstChild && null == _firstChildAlternatives
    val hasSkipAtStart: Boolean get() = _firstChild!!.isSkip

    val lastInitialSkipChild: GrowingChildNode?
        get() = when {
            isEmpty -> null
            null != this._firstChildAlternatives -> null //skips never have alternatives
            else -> {
                var n = this._firstChild
                var last: GrowingChildNode? = null
                while (null != n && n.isSkip) {
                    last = n
                    n = n.nextChild
                }
                last
            }
        }

    val matchedTextLength get() = this.nextInputPosition - this.startPosition

    private fun clone(): GrowingChildren {
        val cl = GrowingChildren()
        cl.containedFor.addAll(this.containedFor)
        cl.length = this.length
        cl.numberNonSkip = this.numberNonSkip
        cl.nextInputPosition = this.nextInputPosition
        cl.startPosition = this.startPosition
        cl._firstChild = this._firstChild
        cl._lastChildAlternatives.putAll(this._lastChildAlternatives)
        if (null != this._firstChildAlternatives) {
            cl._firstChildAlternatives = mutableMapOf()
            this._firstChildAlternatives!!.entries.forEach {
                val l = it.value.toMutableList() // copy the list of nodes, it can get modified in the original
                cl._firstChildAlternatives!![it.key] = l
            }
        }
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

    private fun nextChildAlt(childIndex: Int, ruleOptionList: Set<RuleOptionId>): Int {
        return when {
            null == _nextChildAlts -> 0
            else -> {
                val m = _nextChildAlts!![childIndex]
                when {
                    null == m -> 0
                    else -> m[ruleOptionList] ?: 0
                }
            }
        }
    }

    internal fun incNextChildAlt(childIndex: Int, value: Set<RuleOptionId>) {
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

    internal fun setNextChildAlt(childIndex: Int, value: Set<RuleOptionId>, altNum: Int) {
        if (null == _nextChildAlts) _nextChildAlts = mutableMapOf()
        var m = _nextChildAlts!![childIndex]
        if (null == m) {
            m = mutableMapOf()
            _nextChildAlts!![childIndex] = m
        }
        m[value] = altNum
    }

    // only used in RuntimeParser.useGoal && internally in this class
    fun firstChild(ruleOptionSet: Set<RuleOptionId>): GrowingChildNode? = when {
        null == this._firstChildAlternatives -> this._firstChild
        //null == ruleOption -> null //skip will not have alternatives
        else -> this._firstChildAlternatives!!.entries.firstOrNull {
            it.key.isEmpty() || // initial skip is first child for anything, emptyList is used to mark initialSkip
            it.key==ruleOptionSet
        }?.value?.get(this.nextChildAlt(0, ruleOptionSet))
    }

    private fun firstChildWithIndexContaining(ruleOption: RuleOptionId): GrowingChildNode? = when {
        null == this._firstChildAlternatives -> this._firstChild
        //null == ruleOption -> null //skip will not have alternatives
        else -> this._firstChildAlternatives!!.entries.firstOrNull {
            it.key.isEmpty() || // initial skip is first child for anything, emptyList is used to mark initialSkip
                    it.key.contains(ruleOption)
        }?.value?.get(this.nextChildAlt(0, ruleOption))
    }

    fun lastChild(ruleOption: Set<RuleOptionId>): GrowingChildNode? =  this._lastChildAlternatives[ruleOption]

    fun lastChild2(ruleOption: Set<RuleOptionId>): GrowingChildNode? =  this._lastChildAlternatives.entries.firstOrNull {
        it.key.containsAll(ruleOption)
    }?.value

    // only used in RuntimeParser.useGoal
    fun firstNonSkipChild(ruleOption: Set<RuleOptionId>): GrowingChildNode? {
        var r = this.firstChild(ruleOption)
        var index = 1
        while (null != r && r.isSkip) {
            r = r.next(this.nextChildAlt(index, ruleOption), ruleOption)
            index++
        }
        return r
    }

    private fun setFirstChild(ruleOptionList: Set<RuleOptionId>, node: GrowingChildNode) {
        when {
            this.isEmpty -> this._firstChild = node
            else -> {
                _firstChildAlternatives = mutableMapOf()
                val alts = mutableListOf(this._firstChild!!)
                this._firstChildAlternatives!![ruleOptionList] = alts
                this._firstChild = null
                alts.add(node)
            }
        }
        this.setLastChild(ruleOptionList, node)
        if (node.isSkip.not()) this.numberNonSkip=1
        this.length = 1
        this.containedFor.add(ruleOptionList)
    }

    private fun setLastChild(ruleOption: Set<RuleOptionId>, node: GrowingChildNode) {
        this._lastChildAlternatives[ruleOption] = node
    }

    /**
     * create new GrowingChildren unless empty
     */
    //TODO: do we need to create new object or can we reuse existing because it may not be used anymore ?
    fun appendChild(ruleOptionList: Set<RuleOptionId>, nextChildAlts: List<SPPTNode>): GrowingChildren {
        val nextChild = GrowingChildNode(ruleOptionList, nextChildAlts, false)
        return when {
            isEmpty -> {
                this.setFirstChild(ruleOptionList, nextChild)
                startPosition = nextChildAlts[0].startPosition
                this.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                this
            }
            ruleOptionList.isEmpty() -> {
                TODO("I think this never happens")
                val lisc = lastInitialSkipChild
                when {
                    null == lisc -> { // must be duplicate of firstNode which is goal
                        //TODO: if nextInputPosition of duplicate ??
                        val res = this.clone()
                        res._firstChildAlternatives = mutableMapOf()
                        val alts = mutableListOf(res._firstChild!!)
                        res._firstChildAlternatives!![ruleOptionList] = alts
                        res._firstChild = null
                        alts.add(nextChild)
                        res.incNextChildAlt(0, ruleOptionList)
                        res.setLastChild(ruleOptionList, nextChild)
                        // because its a duplicate of existing goal
                        // will not changed the length or numberNonSkip
                        res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                        res.containedFor.add(ruleOptionList)
                        res
                    }
                    else -> { // must be initial skip and duplicate of existing goal
                        val changeLength = lisc.isLast
                        val appended = lisc.appendLast(this, this.length - 1, nextChild)
                        appended?.let {
                            this.setLastChild(ruleOptionList, it)
                            if (changeLength) {
                                this.length++
                                this.numberNonSkip++
                            } else {
                                // because its a duplicate of existing goal
                                // will not change the length or numberNonSkip
                            }
                            this.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                            this.containedFor.add(ruleOptionList)
                            this
                        }
                    }
                }
            }
            // only contains initialSkip
            this.containedFor.all { it.isEmpty() } -> {
                var res = GrowingChildren()
                var nc = this._firstChild
                while(null!=nc) {
                    //TODO: find a more perf way to do this if needed..but maybe this is ok as only doen for initial skip
                    res = res.appendSkipIfNotEmpty(ruleOptionList, nc.children)
                    nc = nc.nextChild
                }
                val lastChild = res.lastInitialSkipChild ?: TODO()
                val appended = lastChild.appendLast(res, this.length, nextChild)
                res.setLastChild(ruleOptionList, appended)
                res.length++
                res.numberNonSkip++
                res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                res.containedFor.add(ruleOptionList)
                res
            }
            else -> {
                val res = this.clone()
                val lastChild = res.lastChild(ruleOptionList)?: res.lastChild2(ruleOptionList)?: error("should never be null!")
                val appended = lastChild.appendLast(res, res.length, nextChild)
                res.setLastChild(ruleOptionList, appended)
                res.length++
                res.numberNonSkip++
                //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                res.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct, are all children same length?
                res.containedFor.add(ruleOptionList)
                res
            }
        }
    }

    /**
     * create new GrowingChildren unless empty
     */
    //TODO: do we need to create new object or can we reuse existing because it may not be used anymore ?
    fun appendSkipIfNotEmpty(ruleOptionList: Set<RuleOptionId>, skipChildren: List<SPPTNode>): GrowingChildren {
        val nextChild = GrowingChildNode(ruleOptionList, skipChildren,true)
        return if (skipChildren.isEmpty()) {
            //do nothing
            this
        } else {
            if (isEmpty) {
                this.setFirstChild(ruleOptionList, nextChild)
                this.startPosition = skipChildren[0].startPosition
                this.nextInputPosition = skipChildren.last().nextInputPosition
                this
            } else {
                val res = this.clone()
                res.containedFor.add(ruleOptionList)
                val lastChild = res.lastChild(ruleOptionList)?: TODO("?")
                val appended = lastChild.appendLast(res, res.length, nextChild)
                res.setLastChild(ruleOptionList, appended)
                res.length++
                //check(1 == nextChildAlts.map { it.nextInputPosition }.toSet().size)
                res.nextInputPosition = skipChildren[0].nextInputPosition //FIXME: not really correct, are all children same length?
                res
            }
        }
    }

    operator fun get(ruleOption: RuleOptionId): List<SPPTNode> {
        return when {
            this.isEmpty -> emptyList()
            else -> {
                val res = mutableListOf<SPPTNode>()
                var n: GrowingChildNode? = this.firstChildWithIndexContaining(ruleOption)
                var index = 1
                while (null != n && this._lastChildAlternatives.values.contains(n).not()) {
                    res.addAll(n[ruleOption])
                    n = n.next(this.nextChildAlt(index, ruleOption), ruleOption)
                    index++
                }
                if (null == n) {
                    //this list of children died
                    error("TODO")
                } else {
                    res.addAll(
                        n[ruleOption]
                    )
                }
                res
            }
        }
    }

    // only used in context of rewriting initial skip !
    fun concatenate(other: GrowingChildren) {
        for (ruleOptionList in other.containedFor) {
            val lastChild = this.lastChild(ruleOptionList)!!
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

    /**
     * merge other into this
     */
    //TODO: should it return a new object?
    fun mergeOrDropWithPriority(other: GrowingChildren) {
        when {
            other.isEmpty -> Unit
            this.isEmpty -> {
                this._firstChild = other._firstChild
                this._firstChildAlternatives = other._firstChildAlternatives
                this._nextChildAlts = other._nextChildAlts
                this._lastChildAlternatives = other._lastChildAlternatives
                this.length = other.length
                this.containedFor = other.containedFor
                this.startPosition = other.startPosition
                this.numberNonSkip = other.numberNonSkip
                this.nextInputPosition = other.nextInputPosition //FIXME: not really correct, are all children same length?
            }
            this.startPosition != other.startPosition -> error("Cannot merge children starting at a different position")
            else -> {
                for (ruleOption in other.containedFor) {
                    TODO()
                }
            }
        }
    }

    override fun toString(): String = when {
        this.isEmpty -> "{}"
        else -> {
            val res = mutableMapOf<RuleOptionId?, List<String>>()
            val initialSkip = mutableListOf<String>()

            for (ruleOptionList in this.containedFor) {
                var n = firstChild(ruleOptionList)
                var skip = ""
                var index = 1
                while (null != n && n!=this._lastChildAlternatives[ruleOptionList]) {
                    for(ro in ruleOptionList) {
                        when {
                            n!!.isSkip ->res[ro] = (res[ro] ?: emptyList()) + n[ro].joinToString() { it.name }
                            else -> res[ro] = (res[ro] ?: emptyList()) + n[ro].joinToString() { "${it.name}|${it.option}" }
                        }
                        n = n.next(this.nextChildAlt(index, ro), ro)
                        index++
                    }
                }
                when {
                    ruleOptionList.isEmpty() -> when { //only contains initialskip
                        null==n -> (res[null] ?: emptyList())+"-X"
                        else -> res[null] = (res[null] ?: emptyList())+ n[null].joinToString() { it.name }
                    }
                    else -> {
                        for (ro in ruleOptionList) {
                            if (null == n) {
                                //this list of children died
                                res[ro] = (res[ro] ?: emptyList()) + "-X"
                            } else {
                                when {
                                    n.isSkip -> res[ro] = (res[ro] ?: emptyList()) + n[ro].joinToString() { it.name }
                                    else -> res[ro] = (res[ro] ?: emptyList()) + n[ro].joinToString() { "${it.name}|${it.option}" }
                                }
                            }
                        }
                    }
                }
            }

            res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key?.runtimeRule?.tag}|${it.key?.option}) -> ${it.value.joinToString()}" }
                .joinToString(separator = "\n")
        }
    }
/*
     fun toString1(): String = when {
        isEmpty -> "{}"
        else -> {
            val res = mutableMapOf<RuleOptionId?, List<String>>()
            val initialSkip = mutableListOf<String>()

            var sn = _firstChild
            var lastSkip = sn
            while (sn != null && sn.isSkip) {
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
                else -> sn.ruleOptionList ?: emptyList()
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
                            when {
                                n.isSkip -> skip += n.children.joinToString { it.name } //skipNodes
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
                            when {
                                n.isSkip -> {
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

            res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key?.runtimeRule?.tag}|${it.key?.option}) -> ${it.value.joinToString()}" }
                .joinToString(separator = "\n")
        }
    }
*/
}