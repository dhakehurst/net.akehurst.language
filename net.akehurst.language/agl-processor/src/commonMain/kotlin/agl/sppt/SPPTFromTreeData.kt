/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.agl.sppt.SpptWalkerToString
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.graph.CompleteNodeIndex
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SpptWalker

internal class SPPTFromTreeData(
    val treeData: TreeDataComplete<CompleteNodeIndex>,
    val input: InputFromString,
    override val seasons: Int,
    override val maxNumHeads: Int
) : SharedPackedParseTree {

    override fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean) {
        this.treeData.traverseTreeDepthFirst(callback, skipDataAsTree)
    }

    override val root: SPPTNode
        get() {
            val goalChildren = treeData.childrenFor(treeData.root!!)
            val userGoal = goalChildren.first().second[0]
            val userGoalOption = userGoal.option //TODO: will ther ever by more than 1 element?
            //TODO: if goal is a leaf !

            val startPositionBeforeInitialSkip = treeData.initialSkip?.root?.startPosition ?: userGoal.startPosition
            //TODO: much of this code should move to TreeData I think
            val uags = treeData.initialSkip?.let { td ->
                val sg = td.completeChildren[td.root]!!.values.first().get(0)
                val skipChildren = td.completeChildren[sg]!!.values.first().map {
                    td.completeChildren[it]!!.values.first().get(0)
                }
                val nug = CompleteNodeIndex(userGoal.state, startPositionBeforeInitialSkip, userGoal.nextInputPosition, td.root!!.nextInputPosition!!)
                val userGoalChildren = skipChildren + treeData.completeChildren[userGoal]!!.values.first()
                treeData.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
                nug
            } ?: userGoal

            return when {
                uags.isEmbedded -> {
                    val rp = when (uags.rulePositions.size) {
                        1 -> uags.rulePositions[0]
                        else -> {
                            TODO()
                            //    val possChild = this.runtimeRule.rulePositionsAt[childIndx].first { prioList.contains(it.option) }
                            //    child.rulePositions.first { it.rule == possChild.item }
                        }
                    }
                    val uagsTreeData = this.treeData.embeddedFor(uags) ?: error("No tree-data found for $uags")
                    SPPTBranchFromTreeData(uagsTreeData, input, rp.rule as RuntimeRule, rp.option, uags.startPosition, uags.nextInputPosition, -1)
                }

                uags.isLeaf -> {
                    val eolPositions = emptyList<Int>() //TODO calc ?
                    SPPTLeafFromInput(input, uags.firstRule, uags.startPosition, uags.nextInputPosition, -1)
                }

                else -> SPPTBranchFromTreeData(treeData, input, userGoal.highestPriorityRule, userGoalOption, startPositionBeforeInitialSkip, uags.nextInputPosition, -1)
            }
        }

    override fun contains(other: SharedPackedParseTree): Boolean {
        return this.root.contains(other.root)
    }

    private val _tokensByLine: List<List<SPPTLeaf>> by lazy {
        val visitor = TokensByLineVisitor()
        visitor.visitTree(this, emptyList())
        visitor.lines
    }

    override fun tokensByLineAll(): List<List<SPPTLeaf>> {
        return this._tokensByLine
    }

    override fun tokensByLine(line: Int): List<SPPTLeaf> {
        val tbl = this._tokensByLine
        return if (tbl.isEmpty() || line >= tbl.size) {
            emptyList()
        } else {
            tbl[line]
        }
    }

    override val asString: String by lazy {
        SPPT2InputText().visitTree(this, "")
    }

    override val countTrees: Int by lazy {
        CountTreesVisitor().visitTree(this, Unit)
    }

    override val toStringAll: String by lazy {
        this.toStringAllWithIndent("  ", true)
    }

    fun toStringAllWithIndent1(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = visitor.visitTree(this, "  ")
        val total = all.size
        val sep = "\n"
        var cur = 0
        var res = ""
        for (pt in all) {
            cur++
            res += "Tree ${cur} of ${total}\n"
            res += pt
            res += "\n"
        }
        return all.joinToString(sep)
    }

    override fun toStringAllWithIndent(indentIncrement: String, skipDataAsTree: Boolean): String {
        val walker = SpptWalkerToString(input.text, indentIncrement)
        this.treeData.traverseTreeDepthFirst(walker, skipDataAsTree)
        return walker.output
    }

    override fun hashCode(): Int = this.root.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (other is SharedPackedParseTree) {
            this.root == other.root
        } else {
            false
        }
    }
}