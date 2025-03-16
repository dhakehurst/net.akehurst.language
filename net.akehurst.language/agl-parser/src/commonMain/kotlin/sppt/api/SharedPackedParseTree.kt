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

package net.akehurst.language.sppt.api

import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.Rule

interface SpptDataNode {
    val rule: Rule
    val startPosition: Int
    val nextInputPosition: Int
    val nextInputNoSkip: Int
    val option: OptionNum
    val dynamicPriority:List<Int>
}

data class ChildInfo(
    val propertyIndex: Int, // property index for a list is different to child index
    val index: Int,
    val total: Int
)

data class AltInfo(
    val option: OptionNum,
    val index: Int,
    val totalMatched: Int
)

interface SpptDataNodeInfo {
    val node: SpptDataNode

    val parentAlt: AltInfo
    val alt: AltInfo
    val child: ChildInfo

    val numChildrenAlternatives: Map<OptionNum, Int>
    val totalChildrenFromAllAlternatives: Int
    val numSkipChildren: Int
}
val SpptDataNode.isEmptyMatch get() = this.startPosition == this.nextInputPosition

// can't override 'path: () -> List<SpptDataNode>' in java
// so use this fun interface instead
fun interface PathFunction {
    fun invoke() :List<SpptDataNode>
}

interface SpptWalker {
    fun beginTree()
    fun endTree()

    fun skip(startPosition: Int, nextInputPosition: Int)
    fun leaf(nodeInfo: SpptDataNodeInfo)

    /**
     * @param optionOfTotal Pair number indicating which of the parents options this branch is of how many
     * @param ruleName name/tag of the matched rule
     * @param startPosition
     * @param nextInputPosition
     * @param numChildren how many children there are
     */
    fun beginBranch(nodeInfo: SpptDataNodeInfo)

    fun endBranch(nodeInfo: SpptDataNodeInfo)

    fun beginEmbedded(nodeInfo: SpptDataNodeInfo)
    fun endEmbedded(nodeInfo: SpptDataNodeInfo)

    fun treeError(msg: String, path: PathFunction)
}

data class LeafData(
    val name: String,
    val isPattern: Boolean,
    val position: Int,
    val length: Int,
    val tagList: List<String>
) {
    val metaTags: List<String> by lazy { //TODO: make this configurable on the LanguageProcessor
        val map = mutableMapOf<String, String>(
 //TODO?           AglStyleModelDefault.KEYWORD_STYLE_ID to "'[a-zA-Z_][a-zA-Z0-9_-]*'"
        )
        map.mapNotNull {
            when {
                this.name.matches(Regex(it.value)) -> it.key
                else -> null
            }
        }
    }
}

interface TreeData {
    val root: SpptDataNode?
    val userRoot: SpptDataNode
    val initialSkip: TreeData?
    val isEmpty: Boolean
    fun skipDataAfter(node: SpptDataNode): TreeData?
    fun childrenFor(node: SpptDataNode): List<Pair<OptionNum, List<SpptDataNode>>>
    fun embeddedFor(node: SpptDataNode): TreeData?
    fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean)
    fun preferred(node: SpptDataNode): SpptDataNode?
    fun skipNodesAfter(node: SpptDataNode): List<SpptDataNode> //only used in one place - maybe not needed
    fun matches(other: TreeData): Boolean

    // Mutation
    fun start(initialSkipData: TreeData?)
    fun setRootTo(root: SpptDataNode)
    fun setUserGoalChildrenAfterInitialSkip(nug: SpptDataNode, userGoalChildren: List<SpptDataNode>)
    fun setChildren(parent: SpptDataNode, completeChildren: List<SpptDataNode>, isAlternative: Boolean)
    fun setSkipDataAfter(leafNodeIndex: SpptDataNode, skipData: TreeData)
    fun setEmbeddedTreeFor(n: SpptDataNode, treeData: TreeData)

    fun remove(node: SpptDataNode)
}

/**
 * A Shared Packed Parse Forest is a collection of parse trees which share Nodes when possible. There is a Root Node. Each Node in a tree is either a Leaf or an
 * Branch. An Branch contains a Set of Lists of child Nodes. Each list of child nodes is an alternative possible list of children for the Branch
 *
 * A traditional ParseTree would be a special case (sub type) of an SharedPackedParseForest that contains only one tree.
 */
interface SharedPackedParseTree {

    /**
     *  the original input text
     */
    val asSentence: String

    /**
     *
     *  count of the trees contained
     */
    val countTrees: Int

    /**
     *  a string representation of all contained parse trees
     */
    val toStringAll: String

    val treeData: TreeData

    /**
     * Diagnostic info.
     */
    val seasons: Int

    /**
     * Diagnostic info. Indication of ambiguity if > 1
     */
    val maxNumHeads: Int

    fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean)

    fun tokensByLineAll(): List<List<LeafData>>

    fun tokensByLine(line: Int): List<LeafData>

    fun toStringAllWithIndent(indentIncrement: String, skipDataAsTree: Boolean = false): String

}
