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
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsTerminal
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.sppt.Sentence

/**
 * Classical scanner that does the scanning up-front and fixes the token list.
 * When there are multiple options to match,
 * Longest match takes priority.
 * Literal values take priority over patterns (i.e. Keywords will take priority over identifiers).
 */
internal class ScannerClassic(
    sentenceText: String,
    private val nonEmptyTerminals: List<RuntimeRule>
) : Scanner {

    companion object {
        val LEAF_NONE = CompleteTreeDataNode(RuntimeRuleSet.UNDEFINED_RULE, -1, -1, -1, -1)
    }

//    val issues = IssueHolder(LanguageProcessorPhase.SCAN)

    override var sentence: Sentence = SentenceDefault(sentenceText); private set

    override fun reset() {

    }

    override fun isEnd(position: Int): Boolean = position >= this.sentence.text.length

    override fun isLookingAt(position: Int, terminalRule: Rule): Boolean {
        val l = findOrTryCreateLeaf(position, terminalRule)
        return l?.rule?.equals(terminalRule) ?: false
    }

    override fun findOrTryCreateLeaf(position: Int, terminalRule: Rule): CompleteTreeDataNode? {
        val l = _leaves[position]
        val res = when {
            terminalRule.isEmptyTerminal -> CompleteTreeDataNode(RuntimeRuleSet.EMPTY, position, position, position, 0)
            null == l -> {
                val lf = scanAt(position)
                _leaves[position] = lf
                if (LEAF_NONE == lf) null else lf
            }

            LEAF_NONE == l -> null
            else -> l
        }
        return if (res?.rule == terminalRule) {
            res
        } else {
//            val len = res?.nextInputPosition?.let { it - position } ?: 1
//            issues.error(sentence.locationFor(position, len), "Expecting ${terminalRule.tag} bu got ${res?.rule?.tag}", terminalRule)
            null
        }
    }

    private val _leaves = mutableMapOf<Int, CompleteTreeDataNode>()

    private fun scanAt(position: Int): CompleteTreeDataNode {
        val matches = nonEmptyTerminals.mapNotNull {
            val matchLength = (it.rhs as RuntimeRuleRhsTerminal).matchable!!.matchedLength(sentence.text, position)
            when (matchLength) {
                -1 -> null
                0 -> null
                else -> {
                    val nip = position + matchLength
                    CompleteTreeDataNode(it, position, nip, nip, 0)
                }
            }
        }
        // prefer literals over patterns
        val longest = matches.maxWithOrNull(Comparator<CompleteTreeDataNode> { l1, l2 ->
            when {
                l1.rule.isPattern.not() && l2.rule.isPattern -> 1
                l1.rule.isPattern && l2.rule.isPattern.not() -> -1
                else -> when {
                    l1.nextInputPosition > l2.nextInputPosition -> 1
                    l2.nextInputPosition > l1.nextInputPosition -> -1
                    else -> 0
                }
            }
        })
        return when {
            null == longest -> LEAF_NONE
            else -> longest
        }
    }
}