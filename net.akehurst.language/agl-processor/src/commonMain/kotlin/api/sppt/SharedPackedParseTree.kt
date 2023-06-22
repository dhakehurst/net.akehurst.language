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

package net.akehurst.language.api.sppt

data class SpptPathNode(
    val ruleName: String,
    val startPosition: Int,
    val nextInputPosition: Int
)

interface SpptWalker {
    fun skip(startPosition: Int, nextInputPosition: Int)
    fun leaf(ruleName: String, startPosition: Int, nextInputPosition: Int)
    fun beginBranch(option: Int, ruleName: String, startPosition: Int, nextInputPosition: Int)
    fun endBranch(opt: Int, ruleName: String, startPosition: Int, nextInputPosition: Int)
    fun error(msg: String, path: () -> List<SpptPathNode>)
}

/**
 * A Shared Packed Parse Forest is a collection of parse trees which share Nodes when possible. There is a Root Node. Each Node in a tree is either a Leaf or an
 * Branch. An Branch contains a Set of Lists of child Nodes. Each list of child nodes is an alternative possible list of children for the Branch
 *
 * A traditional ParseTree would be a special case (sub type) of an SharedPackedParseForest that contains only one tree.
 */
interface SharedPackedParseTree {

    /**
     * Diagnostic info.
     */
    val seasons: Int

    /**
     * Diagnostic info. Indication of ambiguity if > 1
     */
    val maxNumHeads: Int

    /**
     * The root of the tree
     */
    val root: SPPTNode

    fun traverseTreeDepthFirst(callback: SpptWalker)


    /**
     * Determines if there is an equivalent tree in this forest for every tree in the other forest.
     *
     * @param other tree
     * @return true if this tree contains the other
     */
    fun contains(other: SharedPackedParseTree): Boolean

    fun tokensByLineAll(): List<List<SPPTLeaf>>

    fun tokensByLine(line: Int): List<SPPTLeaf>

    /**
     *  the original input text
     */
    val asString: String

    /**
     *
     *  count of the trees contained
     */
    val countTrees: Int

    /**
     *  a string representation of all contained parse trees
     */
    val toStringAll: String

    fun toStringAllWithIndent(indentIncrement: String): String
}
