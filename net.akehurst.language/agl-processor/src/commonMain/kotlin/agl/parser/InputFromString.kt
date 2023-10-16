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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.runtime.graph.CompletedNodesStore
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.regex.RegexMatcher
import net.akehurst.language.api.sppt.Sentence

internal class InputFromString(
    numTerminalRules: Int,
    sentenceText: String
) {

    companion object {
        const val contextSize = 10
        val END_OF_TEXT = 3.toChar().toString()
        val EOL_PATTERN = Regex("\n", setOf(RegexOption.MULTILINE))
        val LEAF_NONE = CompleteTreeDataNode(RuntimeRuleSet.UNDEFINED_RULE, -1, -1, -1, -1)

        //TODO: write a scanner that counts eols as it goes, rather than scanning the text twice
        fun eolPositions(text: String): List<Int> = EOL_PATTERN.findAll(text).map { it.range.first }.toList()

        /*
        fun locationFor(sentence: String, startPosition: Int, length: Int): InputLocation {
            val before = sentence.substring(0, startPosition)
            val line = before.count { it == '\n' } + 1
            val column = startPosition - before.lastIndexOf('\n')
            return InputLocation(startPosition, column, line, length)
        }
         */
    }

    // private var lastlocationCachePosition = -1
    // private val locationCache = mutableMapOf<Int, InputLocation>()

    // seems faster to match literal with regex than substring and startsWith
    private val isLookingAt_cache = hashMapOf<Pair<Int, RuntimeRule>, Boolean>()

    var sentence: Sentence = SentenceDefault(sentenceText); private set

    fun contextInText(position: Int): String {
        val startIndex = maxOf(0, position - contextSize)
        val endIndex = minOf(this.sentence.text.length, position + contextSize)
        val forText = this.sentence.text.substring(startIndex, position)
        val aftText = this.sentence.text.substring(position, endIndex)
        val startOfLine = forText.lastIndexOfAny(listOf("\n", "\r"))
        val s = if (-1 == startOfLine) {
            0
        } else {
            startOfLine + 1
        }
        val forTextAfterLastEol = forText.substring(s)
        val startOrStartOfLine = startIndex + startOfLine
        val prefix = when {
            startOfLine > 0 -> ""
            startIndex > 0 -> "..."
            else -> ""
        }
        val postFix = if (endIndex < this.sentence.text.length) "..." else ""
        return "$prefix$forTextAfterLastEol^$aftText$postFix"
    }

    //internal val leaves: MutableMap<LeafIndex, SPPTLeafDefault?> = mutableMapOf()
    // leaves[runtimeRule, position]
    internal val leaves = CompletedNodesStore<CompleteTreeDataNode>(numTerminalRules, sentenceText.length + 1)

    fun reset() {
        this.leaves.clear()
    }

    // used by SPPTParserDefault to build up the sentence
//    internal fun append(value: String) {
//        this.text += value
//    }

//    operator fun get(startPosition: Int, nextInputPosition: Int): String {
//        return sentence.text.substring(startPosition, nextInputPosition)
//    }

    internal fun isStart(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return 0 == position
    }

    internal fun isEnd(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return position >= this.sentence.text.length
    }

    fun isLookingAt(position: Int, terminalRule: RuntimeRule): Boolean {
        val r = isLookingAt_cache[Pair(position, terminalRule)] //TODO: make this configurable
        return if (null != r) {
            r
        } else {
            val rhs = terminalRule.rhs
            val matched = when {
                this.isEnd(position) -> if (terminalRule == RuntimeRuleSet.END_OF_TEXT) true else false //TODO: do we need this
                rhs is RuntimeRuleRhsPattern -> rhs.regex.matchesAt(this.sentence.text, position)
                rhs is RuntimeRuleRhsLiteral -> this.sentence.text.regionMatches(position, rhs.literalUnescaped, 0, rhs.literalUnescaped.length)
                else -> error("Internal Error: not handled")
            }
            isLookingAt_cache[Pair(position, terminalRule)] = matched
            matched
        }
    }

    fun isLookingAtAnyOf(lh: LookaheadSetPart, position: Int): Boolean {
        return when {
            lh.includesRT -> error("lookahead must be real lookahead values, <RT> must be resolved")
            lh.includesEOT && this.isEnd(position) -> true
            else -> lh.content.any { this.isLookingAt(position, it) }
        }
    }

    private fun matchLiteral(position: Int, terminalRule: RuntimeRule): RegexMatcher.MatchResult? {
        val rhs = terminalRule.rhs
        return when (rhs) {
            is RuntimeRuleRhsLiteral -> when {
                rhs.literalUnescaped.isEmpty() -> error("Zero length literals are not permitted.")
                else -> {
                    val match = this.isLookingAt(position, terminalRule)
                    when {
                        match -> {
                            val text = (terminalRule.rhs as RuntimeRuleRhsLiteral).literalUnescaped
                            val eolPositions = emptyList<Int>() //this.eolPositions(text)
                            RegexMatcher.MatchResult(text, eolPositions)
                        }

                        else -> null
                    }
                }
            }

            else -> error("Should not happen")
        }
    }

    private fun matchRegEx(position: Int, regex: Regex): String? {//RegexMatcher.MatchResult? {
        val m = regex.find(this.sentence.text, position)
        return if (null == m)
            null
        else {
            val matchedText = m.value
            val x = this.sentence.text.substring(position, position + matchedText.length)
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
        //val stext = this.text.substring(position)
        //val matchedText = regex.matchAtStart(stext)
        val matchedText = regex.matchAt(this.sentence.text, position)?.value
        return if (null == matchedText || 0 == matchedText.length)
            null
        else {
            val eolPositions = eolPositions(matchedText)
            RegexMatcher.MatchResult(matchedText, eolPositions)
            //matchedText
        }
    }

    private fun matchRegEx3(position: Int, regex: Regex): String? {//RegexMatcher.MatchResult? {
        val stext = this.sentence.text.substring(position)
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
        val rhs = terminalRule.rhs
        val matched = when {
            this.isEnd(position) -> if (terminalRule == RuntimeRuleSet.END_OF_TEXT) RegexMatcher.MatchResult(END_OF_TEXT, emptyList()) else null// TODO: should we need to do this?
            rhs is RuntimeRuleRhsPattern -> this.matchRegEx2(position, rhs.regex)
            rhs is RuntimeRuleRhsLiteral -> this.matchLiteral(position, terminalRule)
            else -> error("Internal Error: not handled")
        }
        return matched
    }

    /*
        fun nextLocation(lastLocation: InputLocation, newLength: Int): InputLocation {
            val endIndex = min(this.text.sentence.length, lastLocation.position + lastLocation.length)
            val lastText = this.sentence.text.substring(lastLocation.position, endIndex)
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
    */
    private fun tryCreateLeaf(terminalRuntimeRule: RuntimeRule, atInputPosition: Int): CompleteTreeDataNode {
        // LeafIndex passed as argument because we already created it to try and find the leaf in the cache
        return if (terminalRuntimeRule.rhs is RuntimeRuleRhsEmpty) {
            //val location = this.nextLocation(lastLocation, 0)
            //val leaf = SPPTLeafDefault(terminalRuntimeRule, location, true, "", 0)
            val leaf = CompleteTreeDataNode(terminalRuntimeRule, atInputPosition, atInputPosition, atInputPosition, 0)
            this.leaves[terminalRuntimeRule, atInputPosition] = leaf
            //val cindex = CompleteNodeIndex(terminalRuntimeRule.number, inputPosition)//0, index.startPosition)
            //this.completeNodes[cindex] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            leaf
        } else {
            val match = this.tryMatchText(atInputPosition, terminalRuntimeRule)
            if (null == match) {
                this.leaves[terminalRuntimeRule, atInputPosition] = LEAF_NONE
                LEAF_NONE
            } else {
                //val location = this.nextLocation(lastLocation, match.length)//match.matchedText.length)
                val nextInputPosition = atInputPosition + match.matchedText.length
                val leaf = CompleteTreeDataNode(terminalRuntimeRule, atInputPosition, nextInputPosition, nextInputPosition, 0)
                this.leaves[terminalRuntimeRule, atInputPosition] = leaf
                //val cindex = CompleteNodeIndex(terminalRuntimeRule.number, inputPosition)//0, index.startPosition)
                //this.completeNodes[cindex] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
                leaf
            }
        }
    }

    fun findOrTryCreateLeaf(terminalRuntimeRule: RuntimeRule, inputPosition: Int): CompleteTreeDataNode? {
        //val index = LeafIndex(terminalRuntimeRule.number, inputPosition)
        var existing = this.leaves[terminalRuntimeRule, inputPosition]
        if (null == existing) {
            existing = this.tryCreateLeaf(terminalRuntimeRule, inputPosition)
            //this.leaves[index] = l
            //this.completeNodes[terminalRuntimeRule.number, inputPosition] = existing
        }
        return if (LEAF_NONE === existing) {
            null
        } else {
            existing
        }
    }

    /**
     * startPosition - 0 index position in input text
     * nextInputPosition - 0 index position of next 'token', so we can calculate length
     */
    fun locationFor(startPosition: Int, length: Int): InputLocation = this.sentence.locationFor(startPosition, length)

}