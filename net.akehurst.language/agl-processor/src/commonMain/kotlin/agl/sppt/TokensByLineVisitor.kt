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
import net.akehurst.language.api.sppt.*


internal class TokensByLineVisitor : SharedPackedParseTreeVisitor<Unit, List<String>> {

    val lines = mutableListOf<MutableList<SPPTLeaf>>()
    private lateinit var inputFromString: InputFromString

    fun MutableList<MutableList<SPPTLeaf>>.getOrCreate(index: Int): MutableList<SPPTLeaf> {
        if (index >= this.size) {
            for (i in this.size - 1 until index) {
                this.add(mutableListOf())
            }
        }
        return this[index]
    }

    override fun visitTree(target: SharedPackedParseTree, arg: List<String>) {
        return this.visitNode(target.root, arg)
    }

    override fun visitBranch(target: SPPTBranch, arg: List<String>) {
        target.children.forEach {
            val list = arg + target.name
            this.visitNode(it, list)
        }
    }

    override fun visitLeaf(target: SPPTLeaf, arg: List<String>) {
        target.setTags(arg+target.name)
        when {
            target.isEmptyMatch -> { /* do nothing */
            }
            target.eolPositions.isEmpty() -> {
                //(target.tagList as MutableList<String>).addAll(arg)
                //(target.tagList as MutableList<String>).add(target.name)
                lines.getOrCreate(target.location.line - 1).add(target)
            }
            else -> {
                if (target is SPPTLeafFromInput) {
                    val rr = target.runtimeRule
                    var line = target.location.line
                    var indexPos = 0
                    var startPos = target.location.position
                    var startLinePos = startPos
                    var column = target.location.column
                    target.eolPositions.forEach { eolPos ->
                        val lineText = target.matchedText.substring(indexPos, eolPos + 1)
                        val segmentLeaf = SPPTLeafFromInput(target.input, rr, startLinePos, startLinePos + lineText.length, target.priority)
                        segmentLeaf.setTags(arg+target.name)
                        //val segmentLeaf = SPPTLeafDefault(rr, InputLocation(startLinePos + startPos, column, line, lineText.length), false, lineText, target.priority)
                        lines.getOrCreate(line - 1).add(segmentLeaf)
                        line++
                        indexPos += lineText.length
                        startLinePos += lineText.length
                        column = 0

                    }
                    // add remaining text if there is any
                    val lineText = target.matchedText.substring(indexPos)
                    if (lineText.isNotEmpty()) {
                        //TODO: use SPPTLeafFromInput
                        val segmentLeaf = SPPTLeafFromInput(target.input, rr, startLinePos, startLinePos + lineText.length, target.priority)
                        segmentLeaf.setTags(arg+target.name)
                        //val segmentLeaf = SPPTLeafDefault(rr, InputLocation(startLinePos + startPos, column, line, lineText.length), false, lineText, target.priority)
                        lines.getOrCreate(line - 1).add(segmentLeaf)
                    }
                } else {
                    TODO()
                }
            }
        }
    }
}