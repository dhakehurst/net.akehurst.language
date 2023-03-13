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
import net.akehurst.language.agl.runtime.graph.TreeData
import net.akehurst.language.agl.runtime.graph.TreeDataComplete
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTException
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties - necessary for tree serialisation
/*internal */ class SPPTBranchFromTreeData internal constructor(
    private val _treeData: TreeDataComplete,
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

    override val childrenAlternatives: Set<List<SPPTNode>> by lazy {
        val alternatives = this._treeData.childrenFor(runtimeRule, startPosition, nextInputPosition)
        val r: Set<List<SPPTNode>> = alternatives.map { (prioList, alt) ->
            val chNodeList: List<SPPTNode> = alt.flatMapIndexed { childIndx, child ->
                //val possChildren = this.runtimeRule.rulePositionsAt[childIndx].filter { it.option == this.option }
                val rp = when (child.rulePositions.size) {
                    1 -> child.rulePositions[0]
                    else -> {
                        TODO()
                        //    val possChild = this.runtimeRule.rulePositionsAt[childIndx].first { prioList.contains(it.option) }
                        //    child.rulePositions.first { it.rule == possChild.item }
                    }
                }
                when {
                    this.isEmbedded -> when {
                        null != child.treeData.initialSkip -> {
                            val td = child.treeData.initialSkip!!
                            val goalChildren = child.treeData.childrenFor(
                                child.treeData.root!!.firstRule,
                                child.treeData.root!!.startPosition,
                                child.treeData.root!!.nextInputPosition
                            )
                            val userGoal = goalChildren.first().second[0]
                            val startPositionBeforeInitialSkip = td.startPosition ?: userGoal.startPosition

                            val sg = td.completeChildren[td.root]!!.values.first().get(0)
                            val skipChildren = td.completeChildren[sg]!!.values.first().map {
                                td.completeChildren[it]!!.values.first().get(0)
                            }
                            val nug =child.treeData.createCompleteNodeIndex(userGoal.state, startPositionBeforeInitialSkip, userGoal.nextInputPosition, td.nextInputPosition!!, null, null)
                            val userGoalChildren = skipChildren + child.treeData.completeChildren[userGoal]!!.values.first()
                            child.treeData.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
                            listOf(SPPTBranchFromTreeData(child.treeData, this.input, rp.rule as RuntimeRule, rp.option, nug.startPosition, nug.nextInputPosition, -1))
                        }
                        else -> {
                            val eolPositions = emptyList<Int>() //TODO calc ?
                            listOf(SPPTBranchFromTreeData(child.treeData, this.input, rp.rule as RuntimeRule, rp.option, child.startPosition, child.nextInputPosition, -1))
                        }
                    }

                    child.isEmbedded -> when {
                        child.hasSkipData -> {
                            val skipData = this._treeData.skipChildrenAfter(child)
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

                                    else -> SPPTBranchFromTreeData(
                                        skch.treeData,
                                        this.input,
                                        skch.firstRule,
                                        skch.optionList[0],
                                        skch.startPosition,
                                        skch.nextInputPosition,
                                        -1
                                    )
                                }
                            }
                            val eolPositions = emptyList<Int>() //TODO calc ?
                            listOf(
                                SPPTBranchFromTreeData(
                                    child.treeData,
                                    this.input,
                                    rp.rule as RuntimeRule,
                                    rp.option,
                                    child.startPosition,
                                    child.nextInputPosition,
                                    -1
                                )
                            ) + skipNodes
                        }
                        else -> listOf(SPPTBranchFromTreeData(child.treeData, this.input, rp.rule as RuntimeRule, rp.option, child.startPosition, child.nextInputPosition, -1))
                    }

                    rp.isTerminal -> when {
                        child.hasSkipData -> {
                            val skipData = this._treeData.skipChildrenAfter(child)
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

                                    else -> SPPTBranchFromTreeData(
                                        skch.treeData,
                                        this.input,
                                        skch.firstRule,
                                        skch.optionList[0],
                                        skch.startPosition,
                                        skch.nextInputPosition,
                                        -1
                                    )
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

                    else -> listOf(SPPTBranchFromTreeData(child.treeData, this.input, rp.rule as RuntimeRule, rp.option, child.startPosition, child.nextInputPosition, -1))
                    /*
                    else -> {
                        val possChildren = this.runtimeRule.rulePositionsAt[chIndx].filter { it.option == this.option }
                        when (possChildren.size) {
                            0 -> error("Internal error: should never happen")
                            1 -> {
                                check(ch.runtimeRulesSet.contains(possChildren[0].item))
                                listOf(
                                    SPPTBranchFromTreeData(
                                        ch.treeData,
                                        this.input,
                                        possChildren[0].item!!,
                                        ch.optionList[0],
                                        ch.startPosition,
                                        ch.nextInputPosition,
                                        -1
                                    )
                                )
                            }
                            else -> {
                                TODO()
                            }
                        }
                    }
                     */
                }

            }
            chNodeList
        }.toSet()
        r
    }

    override val children: List<SPPTNode> get() = this.childrenAlternatives.first()

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
                for (otherChildren in other.childrenAlternatives) {
                    // for each of this alternative children, find one that 'contains' otherChildren
                    var foundContainMatch = false
                    for (thisChildren in this.childrenAlternatives) {
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