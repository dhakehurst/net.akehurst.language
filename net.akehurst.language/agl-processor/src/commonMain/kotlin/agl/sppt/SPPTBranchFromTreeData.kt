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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.graph.CompleteNodeIndex
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.sppt.*

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties - necessary for tree serialisation
/*internal */ class SPPTBranchFromTreeData internal constructor(
    private val _treeData: TreeDataComplete<CompleteNodeIndex>,
    input: InputFromString,
    runtimeRule: RuntimeRule,
    option: Int,
    startPosition: Int,               // can't use children.first.startPosition, there may not be any children
    nextInputPosition: Int,          // don't use children.sumBy { it.matchedTextLength }, it requires unwanted iteration
    priority: Int
) : SPPTNodeFromInputAbstract(
    input, runtimeRule, option, startPosition, nextInputPosition, priority
), SPPTBranch {

    // --- SPPTBranch ---

    override val childrenAlternatives: Map<Int, List<SPPTNode>> by lazy {
        val alternatives = this._treeData.childrenFor(object : SpptDataNode {
            override val rule: RuntimeRule get() = runtimeRule
            override val startPosition: Int get() = startPosition
            override val nextInputPosition: Int get() = nextInputPosition
            override val nextInputNoSkip: Int get() = nextInputPosition
            override val option: Int get() = 0
        }
        )
        val r: Map<Int, List<SPPTNode>> = alternatives.map { (alt, children) ->
            val chNodeList: List<SPPTNode> = children.flatMapIndexed { childIndx, child ->
                //val possChildren = this.runtimeRule.rulePositionsAt[childIndx].filter { it.option == this.option }
                val rp = when (child.rulePositions.size) {
                    1 -> child.rulePositions[0]
                    else -> {
                        val possChild = this.runtimeRule.rulePositions.filter { it.position == childIndx }.first { alt == it.option }
                        child.rulePositions.first { possChild.items.contains(it.rule) }
                    }
                }

                when {
                    this.isEmbedded -> {
                        val childTreeData = _treeData.embeddedFor(child) ?: error("No embedded tree-data found for $child")
                        when {
                            null != childTreeData.initialSkip -> {
                                val td = childTreeData.initialSkip!!
                                val goalChildren = childTreeData.childrenFor(childTreeData.root!!)
                                val userGoal = goalChildren.first().second[0]
                                val startPositionBeforeInitialSkip = td.root!!.startPosition ?: userGoal.startPosition

                                val sg = td.completeChildren[td.root]!!.values.first().get(0)
                                val skipChildren = td.completeChildren[sg]!!.values.first().map {
                                    td.completeChildren[it]!!.values.first().get(0)
                                }
                                val nug = CompleteNodeIndex(
                                    //childTreeData,
                                    userGoal.state,
                                    startPositionBeforeInitialSkip,
                                    userGoal.nextInputPosition,
                                    td.root!!.nextInputPosition!!
                                )
                                val userGoalChildren = skipChildren + childTreeData.completeChildren[userGoal]!!.values.first()
                                childTreeData.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
                                listOf(SPPTBranchFromTreeData(childTreeData, this.input, rp.rule as RuntimeRule, rp.option, nug.startPosition, nug.nextInputPosition, -1))
                            }

                            else -> {
                                val eolPositions = emptyList<Int>() //TODO calc ?
                                listOf(SPPTBranchFromTreeData(childTreeData, this.input, rp.rule as RuntimeRule, rp.option, child.startPosition, child.nextInputPosition, -1))
                            }
                        }
                    }

                    child.isEmbedded -> {
                        val childTreeData = _treeData.embeddedFor(child) ?: error("No embedded tree-data found for $child")
                        when {
                            child.hasSkipData -> {
                                val skipData = this._treeData.skipDataAfter(child)
                                val skipChildren = skipData?.let {
                                    val sr = skipData.completeChildren[skipData.root]!!.values.first().get(0)
                                    val c = skipData.completeChildren[sr]!!.values.first().map {
                                        skipData.completeChildren[it]!!.values.first().get(0)
                                    }
                                    c
                                } ?: emptyList()
                                val skipNodes = skipChildren.map { skch ->
                                    when {
                                        skch.isLeaf -> {
                                            val eolPositions = emptyList<Int>() //TODO calc ?
                                            SPPTLeafFromInput(this.input, skch.firstRule, skch.startPosition, skch.nextInputPosition, -1)
                                        }

                                        else -> {
                                            val skchTreeData = _treeData.skipDataAfter(skch) ?: error("No skip tree-data found for $skch")
                                            SPPTBranchFromTreeData(
                                                skchTreeData,
                                                this.input,
                                                skch.firstRule,
                                                skch.option,
                                                skch.startPosition,
                                                skch.nextInputPosition,
                                                -1
                                            )
                                        }
                                    }
                                }
                                val eolPositions = emptyList<Int>() //TODO calc ?
                                listOf(
                                    SPPTBranchFromTreeData(
                                        childTreeData,
                                        this.input,
                                        rp.rule as RuntimeRule,
                                        rp.option,
                                        child.startPosition,
                                        child.nextInputPosition,
                                        -1
                                    )
                                ) + skipNodes
                            }

                            else -> {
                                val td = childTreeData.initialSkip
                                val ug = if (null == td) {
                                    val goalChildren = childTreeData.childrenFor(childTreeData.root!!)
                                    val userGoal = goalChildren.first().second[0]
                                    CompleteNodeIndex(
                                        //childTreeData,
                                        userGoal.state,
                                        userGoal.startPosition,
                                        userGoal.nextInputPosition,
                                        childTreeData.root!!.nextInputPosition!!
                                    )
                                } else {
                                    val goalChildren = childTreeData.childrenFor(childTreeData.root!!)
                                    val userGoal = goalChildren.first().second[0]
                                    val startPositionBeforeInitialSkip = td.root!!.startPosition ?: userGoal.startPosition
                                    val sg = td.completeChildren[td.root]!!.values.first().get(0)
                                    val skipChildren = td.completeChildren[sg]!!.values.first().map {
                                        td.completeChildren[it]!!.values.first().get(0)
                                    }
                                    val nug = CompleteNodeIndex(
                                        //childTreeData,
                                        userGoal.state,
                                        startPositionBeforeInitialSkip,
                                        userGoal.nextInputPosition,
                                        td.root!!.nextInputPosition!!
                                    )
                                    val userGoalChildren = skipChildren + childTreeData.completeChildren[userGoal]!!.values.first()
                                    childTreeData.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
                                    nug
                                }
                                listOf(SPPTBranchFromTreeData(childTreeData, this.input, ug.rule as RuntimeRule, ug.option, ug.startPosition, ug.nextInputPosition, -1))
                            }
                        }
                    }

                    rp.isTerminal -> when {
                        child.hasSkipData -> {
                            val skipData = this._treeData.skipDataAfter(child)
                            val skipChildren = skipData?.let {
                                val sr = skipData.completeChildren[skipData.root]!!.values.first().get(0)
                                val c = skipData.completeChildren[sr]!!.values.first().map {
                                    skipData.completeChildren[it]!!.values.first().get(0)
                                }
                                c
                            } ?: emptyList()
                            val skipNodes = skipChildren.map { skch ->
                                when {
                                    skch.isLeaf -> {
                                        val eolPositions = emptyList<Int>() //TODO calc ?
                                        SPPTLeafFromInput(this.input, skch.firstRule, skch.startPosition, skch.nextInputPosition, -1)
                                    }

                                    else -> {
                                        val skchTreeData = skipData!!//_treeData //.skipDataAfter(skch) ?: error("No skip tree-data found for $skch")
                                        SPPTBranchFromTreeData(
                                            skchTreeData,
                                            this.input,
                                            skch.firstRule,
                                            skch.option,
                                            skch.startPosition,
                                            skch.nextInputPosition,
                                            -1
                                        )
                                    }
                                }
                            }
                            val eolPositions = emptyList<Int>() //TODO calc ?
                            listOf(SPPTLeafFromInput(this.input, rp.rule as RuntimeRule, child.startPosition, child.nextInputPosition, -1)) + skipNodes
                        }

                        else -> {
                            val eolPositions = emptyList<Int>() //TODO calc ?
                            listOf(SPPTLeafFromInput(this.input, rp.rule as RuntimeRule, child.startPosition, child.nextInputPosition, -1))
                        }
                    }

                    else -> {
                        val childTreeData = _treeData
                        listOf(SPPTBranchFromTreeData(childTreeData, this.input, rp.rule as RuntimeRule, rp.option, child.startPosition, child.nextInputPosition, -1))
                    }
                }

            }
            Pair(alt, chNodeList)
        }.associate { it }
        r
    }

    override val children: List<SPPTNode> get() = this.childrenAlternatives.entries.sortedBy { it.key }.last().value

    override val nonSkipChildren: List<SPPTNode> by lazy { //TODO: maybe not use lazy
        this.children.filter { !it.isSkip }
    }

    override val branchNonSkipChildren: List<SPPTBranch> by lazy { //TODO: maybe not use lazy
        this.children.filter { it.isBranch && !it.isSkip }.filterIsInstance<SPPTBranch>()
    }

    override fun nonSkipChild(index: Int): SPPTNode = this.nonSkipChildren[index]

    override fun branchChild(index: Int): SPPTBranch = this.branchNonSkipChildren[index]

    // --- SPPTNode ---
    override val matchedText: String get() = this.input.textFromUntil(this.startPosition, this.nextInputPosition)

    override val nonSkipMatchedText: String get() = this.nonSkipChildren.map { it.nonSkipMatchedText }.joinToString("")

    override fun contains(other: SPPTNode): Boolean {
        if (other is SPPTBranch) {
            if (this.identity == other.identity) {
                // for each alternative list of other children, check there is a matching list
                // of children in this alternative children
                var allOthersAreContained = true // if no other children alternatives contain is a match
                for ((alt, otherChildren) in other.childrenAlternatives.entries.sortedBy { it.key }) {
                    // for each of this alternative children, find one that 'contains' otherChildren
                    var foundContainMatch = false
                    for ((alt, thisChildren) in this.childrenAlternatives.entries.sortedBy { it.key }) {
                        if (thisChildren.size == otherChildren.size) {
                            // for each pair of nodes, one from each of otherChildren thisChildren
                            // check thisChildrenNode contains otherChildrenNode
                            var thisMatch = true
                            for (i in 0 until thisChildren.size) {
                                val thisChildrenNode = thisChildren.get(i)
                                val otherChildrenNode = otherChildren.get(i)
                                thisMatch = thisMatch && thisChildrenNode.contains(otherChildrenNode)
                            }
                            if (thisMatch) {
                                foundContainMatch = true
                                break
                            } else {
                                // if thisChildren alternative doesn't contain, try the next one
                                continue
                            }
                        } else {
                            // if sizes don't match check next in set of this alternative children
                            continue
                        }
                    }
                    allOthersAreContained = allOthersAreContained && foundContainMatch
                }
                return allOthersAreContained
            } else {
                // if identities don't match
                return false
            }

        } else {
            // if other is not a branch
            return false
        }
    }

    override val isEmptyLeaf: Boolean get() = false

    override val isLeaf: Boolean get() = false

    override val isBranch: Boolean get() = true

    override val asLeaf: SPPTLeaf get() = throw SPPTException("Not a Leaf", null)

    override val asBranch: SPPTBranch get() = this

    //override val lastLocation get() = if (children.isEmpty()) this.location else children.last().lastLocation

    override val lastLeaf: SPPTLeaf get() = children.last().lastLeaf

//    override val location: InputLocation get() = TODO("not implemented")

    // --- Object ---
    override fun toString(): String {
        val tag = if (null == this.embeddedIn) this.runtimeRule.tag else "${embeddedIn}.${runtimeRule.tag}"
        var r = ""
        r += this.startPosition.toString() + ","
        r += this.nextInputPosition
        r += ":" + tag + "(" + this.runtimeRule.ruleNumber + ")"
        return r
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SPPTBranch) {
            return false
        } else {
            return if (this.identity != other.identity) {
                false
            } else {
                this.contains(other) && other.contains(this) //TODO: inefficient!
            }
        }
    }
}