/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.syntaxAnalyser.isEmptyMatch
import net.akehurst.language.agl.syntaxAnalyser.locationIn
import net.akehurst.language.agl.syntaxAnalyser.matchedTextNoSkip
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.mutableStackOf

internal class TokensByLineVisitor2(
    val sentence: String
) {

    val lines = mutableListOf<MutableList<LeafData>>()
    private lateinit var inputFromString: InputFromString

    fun MutableList<MutableList<LeafData>>.getOrCreate(index: Int): MutableList<LeafData> {
        if (index >= this.size) {
            for (i in this.size - 1 until index) {
                this.add(mutableListOf())
            }
        }
        return this[index]
    }

    fun visitTree(target: SharedPackedParseTree, arg: List<String>) {
        val treeData = (target as SPPTFromTreeData).treeData
        val nodeLabelStack = mutableStackOf<String>()
        val callback = object : SpptWalker {
            override fun error(msg: String, path: () -> List<SpptDataNode>) = Unit
            override fun skip(startPosition: Int, nextInputPosition: Int) = Unit
            override fun leaf(nodeInfo: SpptDataNodeInfo) = processLeaf(nodeInfo, nodeLabelStack.elements)
            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                nodeLabelStack.push(nodeInfo.node.rule.tag)
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                nodeLabelStack.pop()
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) = beginBranch(nodeInfo)
            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) = endBranch(nodeInfo)
        }
        treeData.traverseTreeDepthFirst(callback, true)
    }

    fun processLeaf(nodeInfo: SpptDataNodeInfo, tagList: List<String>) {
        val name = nodeInfo.node.rule.tag
        val tags = tagList + name
        val location = nodeInfo.node.locationIn(sentence)
        val matchedText = nodeInfo.node.matchedTextNoSkip(sentence)
        val eolPositions = InputFromString.eolPositions(matchedText)
        when {
            nodeInfo.node.isEmptyMatch -> Unit
            eolPositions.isEmpty() -> {
                lines.getOrCreate(location.line - 1).add(LeafData(name, location, matchedText, tags))
            }

            else -> {
                var line = location.line
                var indexPos = 0
                val startPos = nodeInfo.node.startPosition
                var startLinePos = startPos
                var column = location.column
                eolPositions.forEach { eolPos ->
                    val lineText = matchedText.substring(indexPos, eolPos + 1)
                    val segmentLeaf = LeafData(name, location, lineText, tags)
                    lines.getOrCreate(line - 1).add(segmentLeaf)
                    line++
                    indexPos += lineText.length
                    startLinePos += lineText.length
                    column = 0

                }
                // add remaining text if there is any
                val lineText = matchedText.substring(indexPos)
                if (lineText.isNotEmpty()) {
                    val segmentLeaf = LeafData(name, location, lineText, tags)
                    lines.getOrCreate(line - 1).add(segmentLeaf)
                }
            }
        }
    }
}