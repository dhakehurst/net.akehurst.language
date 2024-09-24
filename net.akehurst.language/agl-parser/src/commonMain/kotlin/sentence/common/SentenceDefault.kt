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

package net.akehurst.language.sentence.common

import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNode


class SentenceDefault(
    override val text: String
) : SentenceAbstract() {

    companion object {
        val EOL_PATTERN = Regex("\n", setOf(RegexOption.MULTILINE))
        fun eolPositions(text: String): List<Int> = EOL_PATTERN.findAll(text).map { it.range.first }.toList()
    }

    // --- implementation ---
    override val eolPositions: List<Int> by lazy { eolPositions(text) }


}

abstract class SentenceAbstract() : Sentence {

    companion object {
        const val contextSize = 10
    }

    abstract override val text: String
    abstract val eolPositions: List<Int>

    // --- Sentence ---
    override fun textAt(position: Int, length: Int) =
        this.text.substring(position, position + length)

    override fun matchedTextNoSkip(node: SpptDataNode): String =
        text.substring(node.startPosition, node.nextInputNoSkip)

    override fun positionOfLine(line: Int): Int = when {
        0 > line -> error("Line number must be >= 1")
        eolPositions.size < line -> error("Number of lines in this text is ${eolPositions.size}, requested line number was $line")
        0 == line -> 0
        else -> eolPositions[line - 1] + 1
    }

    override fun locationInLine(line: Int, position: Int, length: Int): InputLocation {
        return when {
            0 > line -> error("Line number must be >= 1")
            eolPositions.size < line -> error("Number of lines in this text is ${eolPositions.size}, requested line number was $line")
            else -> {
                val col = position - positionOfLine(line) + 1
                InputLocation(position, col, line + 1, length)
            }
        }
    }

    override fun locationFor(position: Int, length: Int): InputLocation {
        return when {
            0 == position -> InputLocation(position, 1, 1, length)
            eolPositions.isEmpty() -> InputLocation(position, position + 1, 1, length)
            else -> {
                val line = eolPositions.indexOfLast { it < position }
                when (line) {
                    -1 -> InputLocation(position, position + 1, 1, length)
                    else -> locationInLine(line + 1, position, length)
                }
            }
        }
    }

    override fun locationForNode(node: SpptDataNode): InputLocation =
        locationFor(node.startPosition, node.nextInputNoSkip - node.startPosition)

    override fun contextInText(position: Int): String {
        val startIndex = maxOf(0, position - contextSize)
        val endIndex = minOf(this.text.length, position + contextSize)
        val forText = this.text.substring(startIndex, position)
        val aftText = this.text.substring(position, endIndex)
        val startOfLine = forText.lastIndexOfAny(listOf("\n", "\r"))
        val s = if (-1 == startOfLine) {
            0
        } else {
            startOfLine + 1
        }
        val forTextAfterLastEol = forText.substring(s)
        //val startOrStartOfLine = startIndex + startOfLine
        val prefix = when {
            startOfLine > 0 -> ""
            startIndex > 0 -> "..."
            else -> ""
        }
        val postFix = if (endIndex < this.text.length) "..." else ""
        return "$prefix$forTextAfterLastEol^$aftText$postFix"
    }

}