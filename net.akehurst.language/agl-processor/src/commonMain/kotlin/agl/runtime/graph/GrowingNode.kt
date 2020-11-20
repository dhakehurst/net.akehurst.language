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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.sppt.SPPTNode

class GrowingNode(
        val currentState: ParserState, // current rp of this node, it is growing, this changes (for new node) when children are added
        val lookahead: LookaheadSet,
        priority_deprecated: Int,
        val children: GrowingChildren
) {
    class GrowingChildNode(
            val state: ParserState?, //if null then its skip children
            children: List<SPPTNode>
    ) {
        val childrenAlts = mutableListOf(children)
        var nextChild: GrowingChildNode? = null
        //Pair<Alt,State> -> Node
        var nextChildMap: MutableMap<ParserState?, GrowingChildNode>? = null //TODO: use IntMap
        //var alts = 0

        fun next(runtimeRule: RuntimeRule, option: Int): GrowingChildNode? = when {
            null != nextChildMap -> nextChildMap!!.entries.first {
                val state = it.value.state
                when {
                    null == state -> true //skipNodes
                    else -> when (runtimeRule.rhs.kind) {
                        RuntimeRuleItemKind.CHOICE -> state.rulePositions.any { it.runtimeRule == runtimeRule && it.option == option }
                        else -> state.rulePositions.any { it.runtimeRule == runtimeRule }
                    }
                }
            }.value
            null == nextChild -> null
            else -> nextChild
        }

        operator fun get(alt:Int,runtimeRule: RuntimeRule, option: Int): List<SPPTNode> {
            val children = childrenAlts[alt]
            return when {
                null == this.state -> children //skip nodes
                else -> {
                    val i = when (runtimeRule.rhs.kind) {
                        RuntimeRuleItemKind.CHOICE -> state.rulePositions.indexOfFirst { it.runtimeRule == runtimeRule && it.option == option }
                        else -> state.rulePositions.indexOfFirst { it.runtimeRule == runtimeRule }
                    }
                    when {
                        -1 == i -> emptyList()
                        1 == children.size -> children
                        else -> listOf(children[i])
                    }
                }
            }
        }

        override fun toString(): String = childrenAlts.joinToString(separator = "|")
    }

    class GrowingChildren() {
        companion object {
            val NONE = GrowingChildren()
        }

        var numberNonSkip: Int = 0; private set
        var nextInputPosition: Int = -1
        var startPosition: Int = -1

        //lateinit var location: InputLocation

        var firstChild: GrowingChildNode? = null
        var lastChild: GrowingChildNode? = null

        private var _alts = mutableMapOf<ParserState?,Int>()
        private fun alt(state: ParserState?) :Int{
            val v = _alts[state]
            return if (null==v) {
                _alts[state] = 0
                0
            } else {
                v
            }
        }
        private fun incAlt(state: ParserState) {
            val v = _alts[state]
            return if (null==v) {
                _alts[state] = 0
            } else {
                _alts[state] = v+1
            }
        }

        fun priorityFor(runtimeRule: RuntimeRule): Int = when (runtimeRule.rhs.kind) {
            RuntimeRuleItemKind.CHOICE -> {
                check(lastChild!!.state!!.runtimeRules.all { it == runtimeRule }) //TODO: remove the check
                lastChild!!.childrenAlts[this.alt(lastChild!!.state)].size - 1
            }
            else -> 0
        }

        fun clone(): GrowingChildren {
            val c = GrowingChildren()
            c.numberNonSkip = this.numberNonSkip
            c.nextInputPosition = this.nextInputPosition
            c.startPosition = this.startPosition
            c.firstChild = this.firstChild
            c.lastChild = this.lastChild
            c._alts = this._alts.toMutableMap()
            return c
        }

        fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren {
            return if (null == lastChild) {
                firstChild = GrowingChildNode(state, nextChildAlts)
                startPosition = nextChildAlts[0].startPosition
                lastChild = firstChild
                numberNonSkip++
                nextInputPosition = nextChildAlts[0].nextInputPosition
                this
            } else {
                val res = this.clone()
                val nextChild = GrowingChildNode(state, nextChildAlts)
                val lastNext = res.lastChild!!.nextChild
                when {
                    null != res.lastChild!!.nextChildMap -> {
                        val resLast = res.lastChild!!
                        resLast.nextChildMap!![state] = nextChild
                        res.lastChild = nextChild
                    }
                    null == lastNext -> {
                        res.lastChild!!.nextChild = nextChild
                        res.lastChild = nextChild
                    }
                    state == lastNext.state -> {
                        res.incAlt(state)
                        lastNext.childrenAlts.add(nextChildAlts)
                        res.lastChild = lastNext
                    }
                    else -> {
                        val map = mutableMapOf<ParserState?, GrowingChildNode>()
                        val resLast = res.lastChild!!
                        resLast.nextChildMap = map
                        map[lastNext.state] = lastNext
                        resLast.nextChild = null
                        map[state] = nextChild
                        res.lastChild = nextChild
                    }
                }
                res.numberNonSkip++
                res.nextInputPosition = nextChildAlts[0].nextInputPosition
                res
            }

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
            return this
        }

        operator fun get(runtimeRule: RuntimeRule, option: Int): List<SPPTNode> {
            return when {
                null == firstChild -> emptyList()
                else -> {
                    val res = mutableListOf<SPPTNode>()
                    var n: GrowingChildNode? = firstChild
                    while (n != lastChild) {
                        res.addAll(n!![this.alt(n.state),runtimeRule, option])
                        n = n.next(runtimeRule, option)
                    }
                    res.addAll(lastChild!![this.alt(n!!.state),runtimeRule, option])
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
                while (null == sn!!.state) {
                    initialSkip.add(sn.childrenAlts[0].joinToString() { it.name })
                    sn = sn.nextChild
                }
                val rps = sn.state!!.rulePositions
                rps.forEach { rp ->
                    res[rp] = initialSkip
                    var n = sn
                    var skip = ""
                    while (null != n) {
                        val state = n.state
                        when {
                            null == state -> skip += n.childrenAlts[this.alt(n.state)].joinToString() //skipNodes
                            else -> {
                                val hasAlts = when (n.childrenAlts.size) {
                                     1 -> ""
                                    else -> "*(${this.alt(n.state)})"
                                }
                                res[rp] = res[rp]!! + n[this.alt(n.state), rp.runtimeRule, rp.option].joinToString() { it.name + hasAlts }
                            }
                        }
                        n = n.next( rp.runtimeRule, rp.option)
                    }
                }
                res.entries.map { "(${this.startPosition},${this.nextInputPosition-1}${it.key.runtimeRule.tag}|opt=${it.key.option}) -> ${it.value.joinToString()}" }.joinToString(separator = "\n")
            }
        }
    }

    companion object {
        fun index(state: ParserState, growingChildren: GrowingChildren): GrowingNodeIndex {
            val listSize = listSize(state.runtimeRules.first(), growingChildren.numberNonSkip)
            return GrowingNodeIndex(state, growingChildren.startPosition, growingChildren.nextInputPosition, listSize)
        }

        // used for start and leaf
        fun index(state: ParserState, startPosition: Int, nextInputPosition: Int, listSize: Int): GrowingNodeIndex {
            return GrowingNodeIndex(state, startPosition, nextInputPosition, listSize)
        }

        // used to augment the GrowingNodeIndex (GSS node identity) for MULTI and SEPARATED_LIST
        // needed because the 'RulePosition' does not capture the 'position' in the list
        fun listSize(runtimeRule: RuntimeRule, childrenSize: Int): Int = when (runtimeRule.kind) {
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> -1
                RuntimeRuleItemKind.CONCATENATION -> -1
                RuntimeRuleItemKind.CHOICE -> -1
                RuntimeRuleItemKind.UNORDERED -> -1
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> -1 //TODO
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> -1 //TODO
                RuntimeRuleItemKind.MULTI -> childrenSize
                RuntimeRuleItemKind.SEPARATED_LIST -> childrenSize
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
*/
    fun priorityFor(runtimeRule: RuntimeRule): Int = children.priorityFor(runtimeRule)

    fun newPrevious() {
        this.previous = mutableMapOf()
    }

    fun addPrevious(info: PreviousInfo) {
        val gn = info.node
        val gi = GrowingNode.index(gn.currentState, gn.children)
        this.previous.put(gi, info)
        info.node.addNext(this)
    }

    fun addPrevious(previousNode: GrowingNode) {
        val info = PreviousInfo(previousNode)
        val gi = GrowingNode.index(previousNode.currentState, previousNode.children)
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
