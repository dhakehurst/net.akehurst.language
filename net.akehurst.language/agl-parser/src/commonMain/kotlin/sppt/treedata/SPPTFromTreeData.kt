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

package net.akehurst.language.sppt.treedata

import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.LeafData
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.sppt.api.SpptWalker
import net.akehurst.language.sppt.api.TreeData

class SPPTFromTreeData(
    override val treeData: TreeData,
    val sentence: Sentence,
    override val seasons: Int,
    override val maxNumHeads: Int
) : SharedPackedParseTree {

    private val _tokensByLine: List<List<LeafData>> by lazy {
        val visitor = TokensByLineVisitor(sentence)
        visitor.visitTree(this, emptyList())
        visitor.lines
    }

    override val asSentence: String by lazy {
        //SPPT2InputText().visitTree(this, "")
        val walker = SpptWalkerToInputSentence(sentence)
        this.treeData.traverseTreeDepthFirst(walker, false)
        walker.output
    }

    override val countTrees: Int by lazy {
        CountTreesVisitor().visitTree(this, Unit)
    }

    override val toStringAll: String by lazy {
        this.toStringAllWithIndent("  ", true)
    }

    override fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean) {
        this.treeData.traverseTreeDepthFirst(callback, skipDataAsTree)
    }

    override fun tokensByLineAll(): List<List<LeafData>> {
        return this._tokensByLine
    }

    override fun tokensByLine(line: Int): List<LeafData> {
        val tbl = this._tokensByLine
        return if (tbl.isEmpty() || line >= tbl.size) {
            emptyList()
        } else {
            tbl[line]
        }
    }

    override fun toStringAllWithIndent(indentIncrement: String, skipDataAsTree: Boolean): String {
        val walker = SpptWalkerToString(sentence, indentIncrement)
        this.treeData.traverseTreeDepthFirst(walker, skipDataAsTree)
        return walker.output
    }

    fun matches(other: SPPTFromTreeData) = this.treeData.matches(other.treeData)

    override fun hashCode(): Int = this.treeData.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is SharedPackedParseTree -> false
        this.treeData.matches(other.treeData).not() -> false
        else -> true
    }
}