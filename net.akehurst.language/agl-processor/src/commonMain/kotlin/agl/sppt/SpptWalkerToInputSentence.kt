/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.agl.sppt

import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.sppt.SpptWalker
import net.akehurst.language.collections.mutableStackOf

class SpptWalkerToInputSentence(
    val sentence: String
) : SpptWalker {
    val output get() = textStack.elements.joinToString(separator = "") { it.second }

    //(alt,String)
    private val textStack = mutableStackOf<Pair<Int, String>>()

    override fun beginTree() {}

    override fun endTree() {}

    override fun skip(startPosition: Int, nextInputPosition: Int) {
        val matchedText = sentence.substring(startPosition, nextInputPosition)//.replace("\n", "\u23CE").replace("\t", "\u2B72")
        when {
            textStack.isEmpty -> {
                //initial skip
                textStack.push(Pair(-1, matchedText))
            }

            else -> {
                val p = textStack.pop()
                textStack.push(Pair(p.first, p.second + matchedText))
            }
        }
    }

    override fun leaf(nodeInfo: SpptDataNodeInfo) {
        val matchedText = sentence.substring(nodeInfo.node.startPosition, nodeInfo.node.nextInputNoSkip)//.replace("\n", "\u23CE").replace("\t", "\u2B72")
        textStack.push(Pair(nodeInfo.alt.option, matchedText))
    }

    override fun beginBranch(nodeInfo: SpptDataNodeInfo) {}

    override fun endBranch(nodeInfo: SpptDataNodeInfo) {
        val numChildren = nodeInfo.numChildrenAlternatives[nodeInfo.alt.option]!!
        val chText = textStack.pop(numChildren).reversed()
        val text = chText.joinToString(separator = "") { it.second }
        textStack.push(Pair(nodeInfo.alt.option, text))
    }

    override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
        this.beginBranch(nodeInfo)
    }

    override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
        this.endBranch(nodeInfo)
    }

    override fun error(msg: String, path: () -> List<SpptDataNode>) {
        val p = path()
        val txt = "Error at ${p.last().startPosition}: '$msg'"
        textStack.push(Pair(-1, txt))
        println(txt)
    }
}