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

import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.syntaxAnalyser.isEmptyMatch
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.mutableStackOf

internal class TokensByLineVisitor(
    val sentence: Sentence
) {
    val lines = mutableListOf<MutableList<LeafData>>()
    private lateinit var inputFromString: ScannerOnDemand

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
            override fun beginTree() {}
            override fun endTree() {}
            override fun skip(startPosition: Int, nextInputPosition: Int) = Unit
            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val skipNodes = treeData.skipNodesAfter(nodeInfo.node)
                processLeaf(nodeInfo, skipNodes, nodeLabelStack.elements)
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) = nodeLabelStack.push(nodeInfo.node.rule.tag)
            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                nodeLabelStack.pop()
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                beginBranch(nodeInfo)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) = endBranch(nodeInfo)
        }
        treeData.traverseTreeDepthFirst(callback, true)
    }

    fun processLeaf(nodeInfo: SpptDataNodeInfo, skipNodes: List<SpptDataNode>, tagList: List<String>) {
        val name = nodeInfo.node.rule.tag
        val isPattern = nodeInfo.node.rule.isPattern
        val tags = tagList + name
        val location = sentence.locationForNode(nodeInfo.node)
        val matchedText = sentence.matchedTextNoSkip(nodeInfo.node)
        val eolPositions = ScannerOnDemand.eolPositions(matchedText)
        when {
            nodeInfo.node.isEmptyMatch -> Unit
            eolPositions.isEmpty() -> {
                lines.getOrCreate(location.line - 1).add(LeafData(name, isPattern, location, tags))
            }

            else -> {
                var line = location.line
                var indexPos = 0
                val startPos = nodeInfo.node.startPosition
                var startLinePos = startPos
                var column = location.column
                eolPositions.forEach { eolPos ->
                    val lineText = matchedText.substring(indexPos, eolPos + 1)
                    val loc = InputLocation(startLinePos, column, line, lineText.length)
                    val segmentLeaf = LeafData(name, isPattern, loc, tags)
                    lines.getOrCreate(line - 1).add(segmentLeaf)
                    line++
                    indexPos += lineText.length
                    startLinePos += lineText.length
                    column = 1

                }
                // add remaining text if there is any
                val lineText = matchedText.substring(indexPos)
                if (lineText.isNotEmpty()) {
                    val loc = InputLocation(startLinePos, column, line, lineText.length)
                    val segmentLeaf = LeafData(name, isPattern, loc, tags)
                    lines.getOrCreate(line - 1).add(segmentLeaf)
                }
            }
        }
    }
}