/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.sppt.SPPTNode

class GrowingNode(
        val currentState: ParserState, // current rp of this node, it is growing, this changes (for new node) when children are added
        val lookahead: LookaheadSet,
        val children: GrowingChildren
) {
    /*
    class GrowingChildNode(
            val state: ParserState?, //if null then its skip children
            children: List<SPPTNode>
    ) {
        val childrenAlts = mutableListOf(children)
        var nextChild: GrowingChildNode? = null

        var nextChildMap: MutableMap<List<RuleOptionId>?, MutableList<GrowingChildNode>>? = null //TODO: use IntMap

        fun next(altNext: Int, ruleOption: RuleOptionId): GrowingChildNode? = when {
            null != nextChildMap -> nextChildMap!!.entries.first {
                val rpIds = it.key
                when {
                    null == rpIds -> true //skipNodes
                    else -> {
                        rpIds.any { it == ruleOption }
                    }
                }
            }.value[altNext]
            null == nextChild -> null
            else -> nextChild
        }

        operator fun get(alt: Int, ruleOption: RuleOptionId): List<SPPTNode> {
            val children = childrenAlts[alt]
            return when {
                null == this.state -> children //skip nodes
                else -> {
                    val i = when (ruleOption.runtimeRule.rhs.kind) {
                        RuntimeRuleRhsItemsKind.CONCATENATION -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule }
                        RuntimeRuleRhsItemsKind.CHOICE -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                        RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                        RuntimeRuleRhsItemsKind.MULTI -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                        RuntimeRuleRhsItemsKind.SEPARATED_LIST -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                        else -> TODO()
                    }
                    when {
                        -1 == i -> emptyList()
                        1 == children.size -> children
                        else -> listOf(children[i])
                    }
                }
            }
        }

        override fun toString(): String = childrenAlts.joinToString(separator = "|") {
            it.joinToString(separator = ",") {
                "${it.startPosition},${it.nextInputPosition},${it.name}"
            }
        }
    }

    class GrowingChildren() {
        companion object {
            val NONE = GrowingChildren()
        }

        var length: Int = 0; private set
        var numberNonSkip: Int = 0; private set
        var nextInputPosition: Int = -1
        var startPosition: Int = -1

        //lateinit var location: InputLocation

        var firstChild: GrowingChildNode? = null
        var lastChild: GrowingChildNode? = null

        // childIndex -> alt
        private var _alts: MutableMap<Int, Int>? = null
        private fun alt(childIndex: Int): Int {
            return when {
                null == _alts -> 0
                else -> _alts!![childIndex] ?: 0
            }
        }

        private fun incAlt(childIndex: Int) {
            if (null == _alts) _alts = mutableMapOf()
            val v = _alts!![childIndex]
            if (null == v) {
                _alts!![childIndex] = 1
            } else {
                _alts!![childIndex] = v + 1
            }
        }

        private var _nextAlts: MutableMap<Int,MutableMap<List<RuleOptionId>, Int>>? = null
        private fun nextAlt(childIndex: Int,ruleOption: RuleOptionId): Int {
            return when {
                null == _nextAlts -> 0
                else -> {
                    val m = _nextAlts!![childIndex]
                    when {
                        null == m -> 0
                        else -> m.entries.firstOrNull { it.key.contains(ruleOption) }?.value ?: 0
                    }
                }
            }
        }

        private fun incNextAlt(childIndex: Int,value: List<RuleOptionId>) {
            if (null == _nextAlts) _nextAlts = mutableMapOf()
            var m = _nextAlts!![childIndex]
            if (null==m) {
                m = mutableMapOf()
                _nextAlts!![childIndex] = m
            }
            val v = m[value]
            if (null == v) {
                m[value] = 1
            } else {
                m[value] = v + 1
            }
        }

        fun priorityFor(runtimeRule: RuntimeRule): Int = when (runtimeRule.rhs.kind) {
            RuntimeRuleRhsItemsKind.CHOICE -> {
                check(lastChild!!.state!!.runtimeRules.all { it == runtimeRule }) //TODO: remove the check
                lastChild!!.childrenAlts[this.alt(numberNonSkip)].size - 1
            }
            else -> 0
        }

        fun clone(): GrowingChildren {
            val c = GrowingChildren()
            c.numberNonSkip = this.numberNonSkip
            c.length = this.length
            c.nextInputPosition = this.nextInputPosition
            c.startPosition = this.startPosition
            c.firstChild = this.firstChild
            c.lastChild = this.lastChild
            if (null != this._alts) c._alts = this._alts!!.toMutableMap()
            if (null != this._nextAlts) c._nextAlts = this._nextAlts!!.toMutableMap()
            return c
        }

        fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren {
            val newChildren = when {
                state.isGoal -> when {
                    null == firstChild -> {
                        firstChild = GrowingChildNode(state, nextChildAlts)
                        startPosition = nextChildAlts[0].startPosition
                        lastChild = firstChild
                        this
                    }
                    null == lastChild!!.state -> { //append if last is skip node
                        val nextChild = GrowingChildNode(state, nextChildAlts)
                        this.lastChild!!.nextChild = nextChild
                        this.lastChild = nextChild
                        this
                    }
                    else -> {
                        this.toString()
                        this.incAlt(0)
                        firstChild!!.childrenAlts.add(nextChildAlts)
                        this
                    }
                }
                null == firstChild -> {
                    firstChild = GrowingChildNode(state, nextChildAlts)
                    startPosition = nextChildAlts[0].startPosition
                    lastChild = firstChild
                    this
                }
                else -> {
                    val res = this.clone()
                    val lastNext = res.lastChild!!.nextChild
                    when {
                        null != res.lastChild!!.nextChildMap -> {
                            val resLast = res.lastChild!!
                            val nextChild = GrowingChildNode(state, nextChildAlts)
                            val existing = resLast.nextChildMap!![state.rulePositionIdentity]
                            if (null == existing) {
                                resLast.nextChildMap!![state.rulePositionIdentity] = mutableListOf(nextChild)
                                res.lastChild = nextChild
                            } else {
                                res.incNextAlt(res.length,state.rulePositionIdentity)  //length-1 is index of current child we add 1 for this child we are adding
                                existing.add(nextChild)
                                res.lastChild = nextChild
                            }
                        }
                        null == lastNext -> {
                            val nextChild = GrowingChildNode(state, nextChildAlts)
                            res.lastChild!!.nextChild = nextChild
                            res.lastChild = nextChild
                        }
                        state == lastNext.state -> {
                            res.incAlt(res.length)  //length-1 is index of current child we add 1 for this child we are adding
                            lastNext.childrenAlts.add(nextChildAlts)
                            res.lastChild = lastNext
                        }
                        state.rulePositions.all { sRp -> lastNext.state!!.rulePositions.any { lRp -> sRp.identity == lRp.identity } } -> {

                            error("this is not right somehow")

                            val resLast = res.lastChild!!
                            val nextChild = GrowingChildNode(state, nextChildAlts)
                            val map =  if (null == resLast.nextChildMap) {
                                resLast.nextChildMap = mutableMapOf()
                                resLast.nextChildMap!![state.rulePositionIdentity] = mutableListOf(lastNext)
                                resLast.nextChildMap!!
                            } else {
                                resLast.nextChildMap!!
                            }
                            resLast.nextChild = null
                            res.incNextAlt(res.length,state.rulePositionIdentity) //length-1 is index of current child we add 1 for this child we are adding
                            map[state.rulePositionIdentity]!!.add(nextChild)
                            res.lastChild = nextChild
                        }
                        else -> {
                            val map = mutableMapOf<List<RuleOptionId>?, MutableList<GrowingChildNode>>()
                            val resLast = res.lastChild!!
                            resLast.nextChildMap = map
                            map[lastNext.state!!.rulePositionIdentity] = mutableListOf(lastNext)
                            resLast.nextChild = null
                            val nextChild = GrowingChildNode(state, nextChildAlts)
                            map[state.rulePositionIdentity] = mutableListOf(nextChild)
                            res.lastChild = nextChild
                        }
                    }
                    res
                }
            }
            newChildren.length++
            newChildren.numberNonSkip++
            newChildren.nextInputPosition = nextChildAlts[0].nextInputPosition //FIXME: not really correct
            return newChildren
        }

        fun appendSkipIfNotEmpty(skipChildren: List<SPPTNode>): GrowingChildren {
            if (skipChildren.isEmpty()) {
                //do nothing
            } else {
                if (null == lastChild) {
                    firstChild = GrowingChildNode(null, skipChildren)
                    startPosition = skipChildren[0].startPosition
                    lastChild = firstChild
                } else {
                    val nextChild = GrowingChildNode(null, skipChildren)
                    lastChild!!.nextChild = nextChild
                    lastChild = nextChild
                }
                nextInputPosition = skipChildren.last().nextInputPosition
            }
            this.length++
            return this
        }

        operator fun get(ruleOption: RuleOptionId): List<SPPTNode> {
            return when {
                null == firstChild -> emptyList()
                else -> {
                    val res = mutableListOf<SPPTNode>()
                    var index = 0
                    var n: GrowingChildNode? = firstChild
                    while (n != lastChild && null != n) {
                        res.addAll(n[this.alt(index), ruleOption])
                        n = n.next(this.nextAlt(index, ruleOption), ruleOption)
                        index++
                    }
                    if (null == n) {
                        //this list of children died
                        error("TODO")
                    } else {
                        res.addAll(lastChild!![this.alt(index), ruleOption])
                    }
                    res
                }
            }
        }

        override fun toString(): String = when {
            null == firstChild -> "{}"
            else -> {
                val res = mutableMapOf<RulePosition, List<String>>()
                val initialSkip = mutableListOf<String>()
                var sn = firstChild
                while (sn != null && null == sn.state) {
                    initialSkip.add(sn.childrenAlts[0].joinToString() { it.name })
                    sn = sn.nextChild
                }
                val rps = sn?.state?.rulePositions ?: emptyList()
                for (rp in rps) {
                    res[rp] = initialSkip
                    var n = sn
                    var skip = ""
                    var index = 0
                    while (lastChild != n && null != n) {
                        val state = n.state
                        when {
                            null == state -> skip += n.childrenAlts[this.alt(index)].joinToString() //skipNodes
                            else -> {
                                val hasAlts = when (n.childrenAlts.size) {
                                    1 -> ""
                                    else -> "*(${this.alt(index)})"
                                }
                                res[rp] = res[rp]!! + n[this.alt(index), rp.identity].joinToString() { it.name + hasAlts }
                            }
                        }
                        n = n.next(this.nextAlt(index, rp.identity), rp.identity)
                        index++
                    }
                    if (null == n) {
                        //this list of children died
                        res[rp] = res[rp]!! + "-X"
                    } else {
                        val state = n.state
                        when {
                            null == state -> skip += n.childrenAlts[this.alt(index)].joinToString() //skipNodes
                            else -> {
                                val hasAlts = when (n.childrenAlts.size) {
                                    1 -> ""
                                    else -> "*(${this.alt(index)})"
                                }
                                res[rp] = res[rp]!! + n[this.alt(index), rp.identity].joinToString() { it.name + hasAlts }
                            }
                        }
                    }
                }
                res.entries.map { "(${this.startPosition},${this.nextInputPosition},${it.key.runtimeRule.tag}[${it.key.option}]) -> ${it.value.joinToString()}" }.joinToString(separator = "\n")
            }
        }
    }
*/
    companion object {
        fun index(state: ParserState, lhs: LookaheadSet, growingChildren: GrowingChildren): GrowingNodeIndex {
            val listSize = listSize(state.runtimeRules.first(), growingChildren.numberNonSkip)
            return GrowingNodeIndex(state, lhs.number, growingChildren.startPosition, growingChildren.nextInputPosition, listSize)
        }

        // used for start and leaf
        fun index(state: ParserState, lhs: LookaheadSet, startPosition: Int, nextInputPosition: Int, listSize: Int): GrowingNodeIndex {
            return GrowingNodeIndex(state, lhs.number, startPosition, nextInputPosition, listSize)
        }

        // used to augment the GrowingNodeIndex (GSS node identity) for MULTI and SEPARATED_LIST
        // needed because the 'RulePosition' does not capture the 'position' in the list
        fun listSize(runtimeRule: RuntimeRule, childrenSize: Int): Int = when (runtimeRule.kind) {
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> -1
                RuntimeRuleRhsItemsKind.CONCATENATION -> -1
                RuntimeRuleRhsItemsKind.CHOICE -> -1
                RuntimeRuleRhsItemsKind.UNORDERED -> -1
                RuntimeRuleRhsItemsKind.LEFT_ASSOCIATIVE_LIST -> -1 //TODO
                RuntimeRuleRhsItemsKind.RIGHT_ASSOCIATIVE_LIST -> -1 //TODO
                RuntimeRuleRhsItemsKind.MULTI -> childrenSize
                RuntimeRuleRhsItemsKind.SEPARATED_LIST -> childrenSize
            }
            else -> -1
        }
    }

    //FIXME: shouldn't really do this, shouldn't store these in sets!!
    private val hashCode_cache = arrayOf(this.currentState, this.startPosition).contentHashCode()

    val nextInputPosition: Int get() = children.nextInputPosition
    val numNonSkipChildren: Int get() = children.numberNonSkip


    //val location: InputLocation get() = children.location
    val startPosition get() = children.startPosition
    val matchedTextLength: Int = this.nextInputPosition - this.startPosition
    val runtimeRules = currentState.runtimeRules
    val terminalRule = runtimeRules.first()

    var skipNodes: List<SPPTNode>? = null
/*
val lastLocation
    get() = when (this.runtimeRules.first().kind) {
        RuntimeRuleKind.TERMINAL -> if (this.skipNodes.isEmpty()) this.location else this.skipNodes.last().location
        RuntimeRuleKind.GOAL,
        RuntimeRuleKind.NON_TERMINAL -> if (children.isEmpty()) this.location else children.last().location
        RuntimeRuleKind.EMBEDDED -> if (children.isEmpty()) this.location else children.last().location
    }
*/

    var previous: MutableMap<GrowingNodeIndex, PreviousInfo> = mutableMapOf()
    val next: MutableSet<GrowingNode> = mutableSetOf() //TODO: do we actually need this?
    val isLeaf: Boolean
        get() {
            return this.runtimeRules.first().kind == RuntimeRuleKind.TERMINAL
        }
    val isEmptyMatch: Boolean
        get() {
            // match empty if start and next-input positions are the same and children are complete
            return this.currentState.isAtEnd && this.startPosition == this.nextInputPosition
        }

    val canGrowWidth: Boolean by lazy {
        // not sure we need the test for isEmpty, because if it is empty it should be complete or NOT!???
        if (this.isLeaf || this.isEmptyMatch) {//or this.hasCompleteChildren) {
            false
        } else {
            this.currentState.isAtEnd.not()
//            this.runtimeRule.canGrowWidth(this.currentState.position)
        }
    }

    val nextExpectedItems: Set<RuntimeRule> by lazy {
        this.currentState.rulePositions.mapNotNull { it.item }.toSet()
        //this.runtimeRule.findNextExpectedItems(this.currentState.position)
    }

    /*
    val incrementedNextItemIndex: Int
        get() {
            return this.runtimeRule.incrementNextItemIndex(this.currentState.position)
        }

    fun priorityFor(runtimeRule: RuntimeRule): Int = children.priorityFor(runtimeRule)
    */
    fun newPrevious() {
        this.previous = mutableMapOf()
    }

    fun addPrevious(info: PreviousInfo) {
        val gn = info.node
        val gi = GrowingNode.index(gn.currentState, gn.lookahead, gn.children)
        this.previous.put(gi, info)
        info.node.addNext(this)
    }

    fun addPrevious(previousNode: GrowingNode) {
        val info = PreviousInfo(previousNode)
        val gi = GrowingNode.index(previousNode.currentState, previousNode.lookahead, previousNode.children)
        this.previous.put(gi, info)
        previousNode.addNext(this)
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }

    fun removeNext(value: GrowingNode) {
        this.next.remove(value)
    }

    fun toStringTree(withChildren: Boolean, withPrevious: Boolean): String {
        var r = "$currentState,$startPosition,$nextInputPosition,"
        r += if (this.currentState.isAtEnd) "C" else this.currentState.rulePositions.first().position
        //val name = this.currentState.runtimeRules.joinToString(prefix = "[", separator = ",", postfix = "]") { "${it.tag}(${it.number})" }
        //r += ":" + name
/*
        if (withChildren) {
            if (this.isLeaf) {
                // no children
            } else {
                r += "{"
                for (c in this.children) {
                    //TODO:                r += c.accept(ParseTreeToSingleLineTreeString(), null)
                }
                if (this.hasCompleteChildren) {
                    r += "}"
                } else {
                    r += "..."
                }
            }
        }
*/
        if (withPrevious) {
            val visited = HashSet<GrowingNode>()
            r += this.toStringPrevious(visited)
        }

        return r
    }

    private fun toStringPrevious(visited: MutableSet<GrowingNode>): String {
        visited.add(this)
        var s = ""
        if (this.previous.isEmpty()) {
            //
        } else {
            val prev = this.previous.values.iterator().next().node
            if (visited.contains(prev)) {
                s = "--> ..."
            } else if (this.previous.size == 1) {
                s = " --> " + prev.toStringTree(false, false) + prev.toStringPrevious(visited)
            } else {
                val sz = this.previous.size
                s = " -" + sz + "> " + prev.toStringTree(false, false) + prev.toStringPrevious(visited)
            }

        }
        return s
    }

// --- Any ---

    override fun hashCode(): Int {
        return this.hashCode_cache
    }

    override fun equals(other: Any?): Boolean {
        // assume obj is also a GrowingNode, should never be compared otherwise
        if (other is GrowingNode) {
            return this.currentState == other.currentState
                    && this.startPosition == other.startPosition
                    && this.nextInputPosition == other.nextInputPosition
        } else {
            return false
        }
    }

    override fun toString(): String {
        return this.toStringTree(false, true)
    }
}
