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

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.graph.CompleteNodeIndex
import net.akehurst.language.agl.runtime.graph.TreeData
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree

internal class SPPTFromTreeData(
    private val _treeData: TreeData,
    private val _input: InputFromString,
    override val seasons: Int,
    override val maxNumHeads: Int
) : SharedPackedParseTree {

    override val root: SPPTNode
        get() {
            val rootOptionList = _treeData.root!!.optionList
            val goalChildren = _treeData.childrenFor(
                _treeData.root!!.firstRule,
                rootOptionList.first(), //TODO: will ther ever by more than 1 element?
                _treeData.root!!.startPosition,
                _treeData.root!!.nextInputPosition
            )
            val userGoal = goalChildren.first().first()
            val userGoalOptionList = userGoal.optionList //TODO: will ther ever by more than 1 element?
            //TODO: if goal is a leaf !

            val startPositionBeforeInitialSkip = _treeData.initialSkip?.startPosition ?: userGoal.startPosition
            //TODO: much of this code should move to TreeData I think
            val userGoalAfterSkip = _treeData.initialSkip?.let { td ->
                val sg = td.completeChildren[td.root]!!.get(0)
                val skipChildren = td.completeChildren[sg]!!.map {
                    td.completeChildren[it]!!.get(0)
                }
                val nug = CompleteNodeIndex(_treeData, userGoal.runtimeRules, startPositionBeforeInitialSkip, userGoal.nextInputPosition,userGoalOptionList,null)
                val userGoalChildren = skipChildren + _treeData.completeChildren[userGoal]!!
                _treeData.setUserGoalChildrentAfterInitialSkip(nug, userGoalChildren)
                nug
            } ?: userGoal

            return SPPTBranchFromTreeData(_treeData, _input, userGoal.firstRule, userGoalOptionList[0], startPositionBeforeInitialSkip, userGoalAfterSkip.nextInputPosition, -1)
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
        this.toStringAllWithIndent("")
    }

    override fun toStringAllWithIndent(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = visitor.visitTree(this, ToStringVisitor.Indent("", true))
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

    override fun hashCode(): Int = this.root.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (other is SharedPackedParseTree) {
            this.root == other.root
        } else {
            false
        }
    }
}