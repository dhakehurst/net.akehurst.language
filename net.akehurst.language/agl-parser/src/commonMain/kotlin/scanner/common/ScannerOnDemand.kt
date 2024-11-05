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

package net.akehurst.language.scanner.common

import net.akehurst.language.agl.runtime.graph.CompletedNodesStore
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.automaton.leftcorner.LookaheadSetPart
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.treedata.CompleteTreeDataNode

class ScannerOnDemand(
    regexEngine: RegexEngine,
    terminals: List<Rule>
) : ScannerAbstract(regexEngine) {

    companion object {
        val END_OF_TEXT = 3.toChar().toString()
        val LEAF_NONE = CompleteTreeDataNode(RuntimeRuleSet.UNDEFINED_RULE, -1, -1, -1, RulePosition.OPTION_NONE,emptyList())
    }

    // seems faster to match literal with regex than substring and startsWith !
    private val isLookingAt_cache = hashMapOf<Pair<Int, RuntimeRule>, Boolean>()

    //override var sentence: Sentence = SentenceDefault(sentenceText); private set

    internal val leaves = CompletedNodesStore<CompleteTreeDataNode>(terminals.size)

    override val validTerminals: List<Rule> = terminals.filterNot { it.isEmptyTerminal || it.isEmptyListTerminal }

    init {
        this.matchables // iterate this to set the regexengine
    }

    override val kind: ScannerKind = ScannerKind.OnDemand

    override fun reset() {
        this.leaves.clear()
        this.isLookingAt_cache.clear()
    }

    override fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean {
        val r = isLookingAt_cache[Pair(position, terminalRule)] //TODO: make this configurable
        return if (null != r) {
            r
        } else {
            val rhs = (terminalRule as RuntimeRule).rhs as RuntimeRuleRhsTerminal
            val matched = rhs.matchable?.isLookingAt(sentence.text, position) ?: false
            isLookingAt_cache[Pair(position, terminalRule)] = matched
            matched
        }
    }

    internal fun isLookingAtAnyOf(sentence: Sentence, lh: LookaheadSetPart, position: Int): Boolean {
        return when {
            lh.includesRT -> error("lookahead must be real lookahead values, <RT> must be resolved")
            lh.includesEOT && this.isEnd(sentence, position) -> true
            else -> lh.content.any { this.isLookingAt(sentence, position, it) }
        }
    }
    /*
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
    */

    internal fun tryMatchText(sentence: Sentence, position: Int, terminalRule: RuntimeRule): Int {
        val rhs = terminalRule.rhs as RuntimeRuleRhsTerminal
        val matchable = rhs.matchable
        return matchable?.matchedLength(sentence, position) ?: -1
    }

    private fun tryCreateLeaf(sentence: Sentence, position: Int, terminalRuntimeRule: RuntimeRule): CompleteTreeDataNode {
        return when (terminalRuntimeRule.rhs) {
            is RuntimeRuleRhsEmpty -> {
                val leaf = CompleteTreeDataNode(terminalRuntimeRule, position, position, position, RulePosition.OPTION_NONE,emptyList())
                this.leaves[terminalRuntimeRule, position] = leaf
                leaf
            }

            is RuntimeRuleRhsEmptyList -> {
                val leaf = CompleteTreeDataNode(terminalRuntimeRule, position, position, position, RulePosition.OPTION_NONE,emptyList())
                this.leaves[terminalRuntimeRule, position] = leaf
                leaf
            }

            else -> {
                val matchLength = this.tryMatchText(sentence, position, terminalRuntimeRule)
                if (-1 == matchLength) {
                    this.leaves[terminalRuntimeRule, position] = LEAF_NONE
                    LEAF_NONE
                } else {
                    val nextInputPosition = position + matchLength
                    val leaf = CompleteTreeDataNode(terminalRuntimeRule, position, nextInputPosition, nextInputPosition, RulePosition.OPTION_NONE,emptyList())
                    this.leaves[terminalRuntimeRule, position] = leaf
                    leaf
                }
            }
        }
    }

    override fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): CompleteTreeDataNode? {
        var existing = this.leaves[terminalRule as RuntimeRule, position]
        if (null == existing) {
            existing = this.tryCreateLeaf(sentence, position, terminalRule)
        }
        return if (LEAF_NONE === existing) {
            null
        } else {
            existing
        }
    }

}