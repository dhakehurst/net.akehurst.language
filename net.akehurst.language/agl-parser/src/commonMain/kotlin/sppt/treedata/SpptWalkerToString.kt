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

import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.PathFunction
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.api.SpptWalker

 class SpptWalkerToString(
    val sentence: Sentence,
    val indentDelta: String
) : SpptWalker {
    private var currentIndent = ""
    private val sb = StringBuilder()

    val output get() = sb.toString()

    override fun beginTree() {}

    override fun endTree() {
        sb.append("\n")
    }

    override fun skip(startPosition: Int, nextInputPosition: Int) {
        val matchedText = sentence.text.substring(startPosition, nextInputPosition).replace("\n", "\u23CE").replace("\t", "\u2B72")
        sb.append("${currentIndent}<SKIP> : '$matchedText'\n")
    }

    override fun leaf(nodeInfo: SpptDataNodeInfo) {
        val chNum = nodeInfo.child.index
        val siblings = nodeInfo.child.total
        val ind = if (siblings == 1) "" else currentIndent
        val eol = if (siblings == 1) " " else "\n"
        val matchedText = sentence.matchedTextNoSkip(nodeInfo.node)
            .replace("\n", "\u23CE")
            .replace("\t", "\u2B72")
            .replace("'", "\\'")
        when {
            nodeInfo.node.rule.isEmptyTerminal -> sb.append("${ind}${nodeInfo.node.rule.tag}$eol")
            nodeInfo.node.rule.isEmptyListTerminal -> sb.append("${ind}${nodeInfo.node.rule.tag}$eol")
            nodeInfo.node.rule.isPattern -> sb.append("${ind}${nodeInfo.node.rule.tag} : '${matchedText}'$eol")
            "'${matchedText}'" == nodeInfo.node.rule.tag -> sb.append("${ind}'${matchedText}'$eol")
            else -> sb.append("${ind}${nodeInfo.node.rule.tag} : '${matchedText}'$eol")
        }
    }

    override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
        val option = nodeInfo.alt.index
        val total = nodeInfo.alt.totalMatched
        val chNum = nodeInfo.child.index
        val siblings = nodeInfo.child.total + (nodeInfo.alt.totalMatched - 1)
        val totChildren = nodeInfo.totalChildrenFromAllAlternatives + nodeInfo.numSkipChildren           //FIXME: not correct for skip !
        val eol = if (totChildren <= 1) " " else "\n"
        val tag = if (nodeInfo.alt.totalMatched <= 1) {
            nodeInfo.node.rule.tag
        } else {
            "${nodeInfo.node.rule.tag}|${nodeInfo.alt.option}"
        }
        if (siblings == 1) {
            sb.append("$tag {$eol")
        } else {
            sb.append("${currentIndent}$tag {$eol")
        }
        if (totChildren != 1) currentIndent += indentDelta
    }

    override fun endBranch(nodeInfo: SpptDataNodeInfo) {
        val chNum = nodeInfo.child.index
        val siblings = nodeInfo.child.total + (nodeInfo.alt.totalMatched - 1)
        val totChildren = nodeInfo.totalChildrenFromAllAlternatives + nodeInfo.numSkipChildren //FIXME: not correct for skip !
        val eol = if (siblings == 1) " " else "\n"
//        if (nodeInfo.alt.index + 1 == nodeInfo.alt.totalMatched)
        if (totChildren != 1) currentIndent = currentIndent.substring(indentDelta.length)
        val ind = if (totChildren == 1) "" else currentIndent
        sb.append("${ind}}$eol")
    }

    override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
        val tag = "${nodeInfo.node.rule.tag}"
        sb.append("${currentIndent}$tag : <EMBED>::")
    }

    override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {

    }

    override fun error(msg: String, path: PathFunction) {
        val p = path.invoke()
        sb.append("${currentIndent}Error at ${p.last().startPosition}: '$msg'")
        println("${currentIndent}Error at ${p.last().startPosition}: '$msg'")
    }
}