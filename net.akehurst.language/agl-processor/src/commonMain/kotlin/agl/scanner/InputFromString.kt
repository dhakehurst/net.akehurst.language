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

package net.akehurst.language.agl.scanner

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.runtime.graph.CompletedNodesStore
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.regex.RegexMatcher
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.sppt.Sentence

internal class InputFromString(
    numTerminalRules: Int,
    sentenceText: String
) : Scanner {

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

    // seems faster to match literal with regex than substring and startsWith !
    private val isLookingAt_cache = hashMapOf<Pair<Int, RuntimeRule>, Boolean>()

    override var sentence: Sentence = SentenceDefault(sentenceText); private set

    internal val leaves = CompletedNodesStore<CompleteTreeDataNode>(numTerminalRules, sentenceText.length + 1)

    override fun reset() {
        this.leaves.clear()
    }

//    internal fun isStart(position: Int): Boolean {
//        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
//        return 0 == position
//    }

    override fun isEnd(position: Int): Boolean = position >= this.sentence.text.length

    override fun isLookingAt(position: Int, terminalRule: Rule): Boolean {
        val r = isLookingAt_cache[Pair(position, terminalRule)] //TODO: make this configurable
        return if (null != r) {
            r
        } else {
            val rhs = (terminalRule as RuntimeRule).rhs as RuntimeRuleRhsTerminal
            val matched = rhs.matchable?.isLookingAt(this.sentence.text, position) ?: false
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
                matchedText
            } else {
                null
            }
        }
    }

    private fun matchRegEx2(position: Int, regex: Regex): RegexMatcher.MatchResult? {
        val matchedText = regex.matchAt(this.sentence.text, position)?.value
        return if (null == matchedText || 0 == matchedText.length)
            null
        else {
            val eolPositions = eolPositions(matchedText)
            RegexMatcher.MatchResult(matchedText, eolPositions)
        }
    }

    private fun matchRegEx3(position: Int, regex: Regex): String? {//RegexMatcher.MatchResult? {
        val stext = this.sentence.text.substring(position)
        val match = regex.find(stext)
        return match?.value
    }

    internal fun tryMatchText(position: Int, terminalRule: RuntimeRule): Int {
        val rhs = terminalRule.rhs as RuntimeRuleRhsTerminal
        val matchable = rhs.matchable
        return matchable?.matchedLength(this.sentence.text, position) ?: -1
    }

    private fun tryCreateLeaf(position: Int, terminalRuntimeRule: RuntimeRule): CompleteTreeDataNode {
        return if (terminalRuntimeRule.rhs is RuntimeRuleRhsEmpty) {
            val leaf = CompleteTreeDataNode(terminalRuntimeRule, position, position, position, 0)
            this.leaves[terminalRuntimeRule, position] = leaf
            leaf
        } else {
            val matchLength = this.tryMatchText(position, terminalRuntimeRule)
            if (-1 == matchLength) {
                this.leaves[terminalRuntimeRule, position] = LEAF_NONE
                LEAF_NONE
            } else {
                val nextInputPosition = position + matchLength
                val leaf = CompleteTreeDataNode(terminalRuntimeRule, position, nextInputPosition, nextInputPosition, 0)
                this.leaves[terminalRuntimeRule, position] = leaf
                leaf
            }
        }
    }

    override fun findOrTryCreateLeaf(position: Int, terminalRule: Rule): CompleteTreeDataNode? {
        var existing = this.leaves[terminalRule as RuntimeRule, position]
        if (null == existing) {
            existing = this.tryCreateLeaf(position, terminalRule)
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