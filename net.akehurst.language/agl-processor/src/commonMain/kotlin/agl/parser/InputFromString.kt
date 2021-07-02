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

package net.akehurst.language.agl.parser

import agl.runtime.graph.CompletedNodesStore
import net.akehurst.language.agl.regex.matchAtStart
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.sppt.SPPTLeafDefault
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.regex.RegexMatcher
import net.akehurst.language.api.sppt.SPPTLeaf
import kotlin.math.min

internal class InputFromString(
        numTerminalRules: Int,
        sentence: String
) {

    data class Match(
            val matchedText: String,
            val eolPositions: List<Int>
    )

    companion object {
        val END_OF_TEXT = 3.toChar().toString()
        val EOL_PATTERN = Regex("\n", setOf(RegexOption.MULTILINE))
    }

    // private var lastlocationCachePosition = -1
    // private val locationCache = mutableMapOf<Int, InputLocation>()

    var text: String = sentence; private set


    //internal val leaves: MutableMap<LeafIndex, SPPTLeafDefault?> = mutableMapOf()
    // leaves[runtimeRule, position]
    internal val leaves = CompletedNodesStore<SPPTLeaf>(numTerminalRules, text.length + 1)

    fun reset() {
        this.leaves.clear()
    }

    // used by SPPTParser to build up the sentence
    internal fun append(value: String) {
        this.text += value
    }

    operator fun get(startPosition: Int, nextInputPosition: Int): String {
        return text.substring(startPosition, nextInputPosition)
    }

    internal fun isStart(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return 0 == position
    }

    internal fun isEnd(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return position >= this.text.length
    }

    //TODO: write a scanner that counts eols as it goes, rather than scanning the text twice
    fun eolPositions(text: String): List<Int> {
        return EOL_PATTERN.findAll(text).map { it.range.first }.toList()
    }

    private fun matchLiteral(position: Int, patternText: String):RegexMatcher.MatchResult? {
        val stext = this.text.substring(position)
        val match = stext.startsWith(patternText)//regionMatches(position, patternText, 0, patternText.length, false)
        val matchedText = if (match) patternText else null
        return if (null == matchedText) {
            null
        } else {
            val eolPositions = this.eolPositions(matchedText)
            RegexMatcher.MatchResult(matchedText, eolPositions)
            //matchedText
        }
    }

    private fun matchRegEx(position: Int, regex: Regex): String? {//RegexMatcher.MatchResult? {
        val m = regex.find(this.text, position)
        return if (null == m)
            null
        else {
            val matchedText = m.value
            val x = this.text.substring(position, position + matchedText.length)
            if (x == matchedText) {
                //val eolPositions = this.eolPositions(matchedText)
                //RegexMatcher.MatchResult(matchedText, eolPositions)
                matchedText
            } else {
                null
            }
        }
    }

    private fun matchRegEx2(position: Int, regex: Regex): RegexMatcher.MatchResult? {
        val stext = this.text.substring(position)
        val matchedText = regex.matchAtStart(stext)
        return if (null == matchedText)
            null
        else {
            val eolPositions = this.eolPositions(matchedText)
            RegexMatcher.MatchResult(matchedText, eolPositions)
            //matchedText
        }
    }
    private fun matchRegEx3(position: Int, regex: Regex): String? {//RegexMatcher.MatchResult? {
        val stext = this.text.substring(position)
        val match = regex.find(stext)
        return if (null == match)
            null
        else {
            //val eolPositions = this.eolPositions(matchedText)
            //RegexMatcher.MatchResult(matchedText, eolPositions)
            match.value
        }
    }
    internal fun tryMatchText(position: Int, terminalRule: RuntimeRule): RegexMatcher.MatchResult? {
        val matched = when {
            (position >= this.text.length) -> if (terminalRule.value == END_OF_TEXT) RegexMatcher.MatchResult(END_OF_TEXT, emptyList()) else null// TODO: should we need to do this?
            terminalRule.isPattern -> this.matchRegEx2(position, terminalRule.patternAtStart!!)
            else -> this.matchLiteral(position, terminalRule.value)
            //else ->pattern.match(this.text, position)
        }
        return matched
    }

    fun nextLocation(lastLocation: InputLocation, newLength: Int): InputLocation {
        val endIndex = min(this.text.length, lastLocation.position + lastLocation.length)
        val lastText = this.text.substring(lastLocation.position, endIndex)
        var linesInText = 0
        var lastEolInText = -1
        lastText.forEachIndexed { index, ch -> //FIXME: inefficient having to parse text again
            if (ch == '\n') {
                linesInText++
                lastEolInText = index
            }
        }

        val position = lastLocation.position + lastLocation.length
        val line = lastLocation.line + linesInText
        val column = when {
            0 == lastLocation.position && 0 == lastLocation.length -> 1
            -1 == lastEolInText -> lastLocation.column + lastLocation.length
            else -> lastLocation.length - lastEolInText
        }
        return InputLocation(position, column, line, newLength)
    }

    private fun tryCreateLeaf(terminalRuntimeRule: RuntimeRule, atInputPosition: Int): SPPTLeaf {
        // LeafIndex passed as argument because we already created it to try and find the leaf in the cache
        return if (terminalRuntimeRule.isEmptyRule) {
            //val location = this.nextLocation(lastLocation, 0)
            //val leaf = SPPTLeafDefault(terminalRuntimeRule, location, true, "", 0)
            val leaf = SPPTLeafFromInput(this, terminalRuntimeRule, atInputPosition, atInputPosition,0)
            this.leaves[terminalRuntimeRule, atInputPosition] = leaf
            //val cindex = CompleteNodeIndex(terminalRuntimeRule.number, inputPosition)//0, index.startPosition)
            //this.completeNodes[cindex] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            leaf
        } else {
            val match = this.tryMatchText(atInputPosition, terminalRuntimeRule)
            if (null == match) {
                this.leaves[terminalRuntimeRule, atInputPosition] = SPPTLeafDefault.NONE
                SPPTLeafDefault.NONE
            } else {
                //val location = this.nextLocation(lastLocation, match.length)//match.matchedText.length)
                val nextInputPosition = atInputPosition + match.matchedText.length
                val leaf = SPPTLeafFromInput(this, terminalRuntimeRule, atInputPosition, nextInputPosition,0)//.matchedText, 0)
                leaf.eolPositions = match.eolPositions
                this.leaves[terminalRuntimeRule, atInputPosition] = leaf
                //val cindex = CompleteNodeIndex(terminalRuntimeRule.number, inputPosition)//0, index.startPosition)
                //this.completeNodes[cindex] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
                leaf
            }
        }
    }

    fun findOrTryCreateLeaf(terminalRuntimeRule: RuntimeRule, inputPosition: Int): SPPTLeaf? {
        //val index = LeafIndex(terminalRuntimeRule.number, inputPosition)
        var existing = this.leaves[terminalRuntimeRule, inputPosition]
        if (null == existing) {
            existing = this.tryCreateLeaf(terminalRuntimeRule, inputPosition)
            //this.leaves[index] = l
            //this.completeNodes[terminalRuntimeRule.number, inputPosition] = existing
        }
        return if (SPPTLeafDefault.NONE === existing) {
            null
        } else {
            existing as SPPTLeaf
        }
    }

    /**
     * startPosition - 0 index position in input text
     * nextInputPosition - 0 index position of next 'token', so we can calculate length
     */
    fun locationFor(startPosition: Int, length: Int): InputLocation {
        val before = this.text.substring(0,startPosition)
        val line = before.count { it=='\n' } +1
        val column = startPosition - before.lastIndexOf('\n')
        return InputLocation(startPosition,column,line,length)
    }

}