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
import net.akehurst.language.collections.transitiveClosure

class GrowingNode(
        val currentState: ParserState, // current rp of this node, it is growing, this changes (for new node) when children are added
        val lookahead: LookaheadSet,
        val priority: Int,
        val children: GrowingChildren,
        val numNonSkipChildren: Int
) {
    class GrowingChildNode(
            val state: ParserState?, //if null then its skip children
            val children: List<SPPTNode>
    ) {
        var nextChild: GrowingChildNode? = null
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

        fun appendChild(state: ParserState, nextChildAlts: List<SPPTNode>): GrowingChildren {
            if (null == lastChild) {
                firstChild = GrowingChildNode(state, nextChildAlts)
                startPosition = nextChildAlts[0].startPosition
                lastChild = firstChild
            } else {
                val nextChild = GrowingChildNode(state, nextChildAlts)
                lastChild!!.nextChild = nextChild
                lastChild = nextChild
            }
            numberNonSkip++
            nextInputPosition = nextChildAlts[0].nextInputPosition
            return this
        }

        fun appendSkip(skipChildren: List<SPPTNode>): GrowingChildren {
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

        operator fun get(runtimeRule: RuntimeRule): List<SPPTNode> {
            return when {
                null == firstChild -> emptyList()
                else -> listOf(firstChild!!)
                        .transitiveClosure { it.nextChild?.let { listOf(it)} ?: emptyList() }
                        .flatMap {
                            when {
                                null==it.state -> it.children
                                //FIXME: could cause issues if RuntimeRuleSet is different, i.e. embedded rules!
                                else -> {
                                    val i = it.state.runtimeRules.indexOf(runtimeRule)
                                    listOf(it.children[i])
                                }
                            }
                        }
            }
        }

        override fun toString(): String = when {
            null == firstChild -> "{}"
            else -> listOf(firstChild!!).transitiveClosure { it.nextChild?.let { listOf(it) } ?: emptyList() }
                    .fold(listOf<Pair<ParserState?, String>>()) { acc, ch ->
                        acc + Pair(ch.state, ch.children.joinToString(prefix = "[", separator = ",", postfix = "]"))
                    }.joinToString(prefix = "{", separator = ", ", postfix = "}") { "${it.first}->${it.second}" }
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

    //val location: InputLocation get() = children.location
    val startPosition get() = children.startPosition
    val matchedTextLength: Int = this.nextInputPosition - this.startPosition
    val runtimeRules = currentState.runtimeRules
    val terminalRule = runtimeRules.first()

    var skipNodes = mutableListOf<SPPTNode>()
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
        val name = this.currentState.runtimeRules.joinToString(prefix = "[", separator = ",", postfix = "]") { "${it.tag}(${it.number})" }
        r += ":" + name
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
