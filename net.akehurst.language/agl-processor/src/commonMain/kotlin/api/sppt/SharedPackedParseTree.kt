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

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.api.parser.InputLocation

interface SpptDataNode {
    val rule: Rule
    val startPosition: Int
    val nextInputPosition: Int
    val nextInputNoSkip: Int
    val option: Int
}

interface Sentence {
    val text: String
    fun matchedTextNoSkip(node: SpptDataNode): String
    fun locationFor(position: Int, length: Int): InputLocation
    fun locationFor(node: SpptDataNode): InputLocation
}

data class ChildInfo(
    val propertyIndex: Int, // property index for a list is different to child index
    val index: Int,
    val total: Int
)

data class AltInfo(
    val option: Int,
    val index: Int,
    val totalMatched: Int
)

interface SpptDataNodeInfo {
    val node: SpptDataNode

    val parentAlt: AltInfo
    val alt: AltInfo
    val child: ChildInfo

    val numChildrenAlternatives: Map<Int, Int>
    val totalChildrenFromAllAlternatives: Int
    val numSkipChildren: Int
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

    fun error(msg: String, path: () -> List<SpptDataNode>)
}

data class LeafData(
    val name: String,
    val isPattern: Boolean,
    val location: InputLocation,
    val matchedText: String,
    val tagList: List<String>
) {
    val metaTags: List<String> by lazy { //TODO: make this configurable on the LanguageProcessor
        val map = mutableMapOf<String, String>(
            "\$keyword" to "'[a-zA-Z_][a-zA-Z0-9_-]*'"
        )
        map.mapNotNull {
            when {
                this.name.matches(Regex(it.value)) -> it.key
                else -> null
            }
        }
    }
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

    val treeData: TreeDataComplete<SpptDataNode>

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
