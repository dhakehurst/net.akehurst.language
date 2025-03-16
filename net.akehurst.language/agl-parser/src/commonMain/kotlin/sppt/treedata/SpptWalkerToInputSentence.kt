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

package net.akehurst.language.sppt.treedata

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.PathFunction
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.api.SpptWalker

internal class SpptWalkerToInputSentence(
    val sentence: Sentence
) : SpptWalker {
    companion object {
        val OPTION_SKIP = null
        val OPTION_ERROR = null
    }

    val output
        get() = when {
            OPTION_SKIP == textStack.elements[0].first -> {
                //skip at start
                val startSkip = textStack.elements[0].second
                textStack.elements.drop(1).map { startSkip + it.second }
            }

            else -> textStack.elements.map { it.second }
        }

    //(alt,String)
    private val textStack = mutableStackOf<Pair<SpptDataNodeInfo?, String>>()

    private fun pushText(nodeInfo: SpptDataNodeInfo?, text: String) {
        when {
            // for skip
            null == nodeInfo -> textStack.push(Pair(nodeInfo, text))
            textStack.isEmpty -> textStack.push(Pair(nodeInfo, text))
            //ambiguous choices mean multiple options for text
            nodeInfo.node.rule.isChoiceAmbiguous -> {
                val top = textStack.peek().first ?: error("Should not happen")
                when {
                    top.node.rule == nodeInfo.node.rule -> {
                        //ambiguous choice...just pick last one for converting to string TODO: indicate multiple choices
                        textStack.pop()
                        textStack.push(Pair(nodeInfo, text))
                    }

                    else -> textStack.push(Pair(nodeInfo, text))
                }
            }

            else -> textStack.push(Pair(nodeInfo, text))
        }
    }

    override fun beginTree() {}

    override fun endTree() {
    }

    override fun skip(startPosition: Int, nextInputPosition: Int) {
        val matchedText = sentence.text.substring(startPosition, nextInputPosition)//.replace("\n", "\u23CE").replace("\t", "\u2B72")
        when {
            textStack.isEmpty -> {
                //initial skip
                pushText(OPTION_SKIP, matchedText)
            }

            else -> {
                val p = textStack.pop()
                pushText(p.first, p.second + matchedText)
            }
        }
    }

    override fun leaf(nodeInfo: SpptDataNodeInfo) {
        val matchedText = sentence.matchedTextNoSkip(nodeInfo.node)//.replace("\n", "\u23CE").replace("\t", "\u2B72")
        pushText(nodeInfo, matchedText)
    }

    override fun beginBranch(nodeInfo: SpptDataNodeInfo) {}

    override fun endBranch(nodeInfo: SpptDataNodeInfo) {
        val numChildren = nodeInfo.numChildrenAlternatives[nodeInfo.alt.option]!!
        val chText = textStack.pop(numChildren).reversed()
        val text = chText.joinToString(separator = "") { it.second }
        pushText(nodeInfo, text)
    }

    override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
        this.beginBranch(nodeInfo)
    }

    override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
        val numChildren = nodeInfo.numChildrenAlternatives[RulePosition.OPTION_NONE]!!
        val chText = textStack.pop(numChildren).reversed()
        val text = chText.joinToString(separator = "") { it.second }
        pushText(nodeInfo, text)
    }

    override fun treeError(msg: String, path: PathFunction) {
        val p = path.invoke()
        val txt = "Error at ${p.last().startPosition}: '$msg'"
        pushText(OPTION_ERROR, txt)
        println(txt)
    }
}