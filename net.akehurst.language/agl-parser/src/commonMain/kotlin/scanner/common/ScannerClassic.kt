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

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsTerminal
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.treedata.CompleteTreeDataNode

/**
 * Classical scanner that does the scanning up-front and fixes the token list.
 * When there are multiple options to match,
 * Longest match takes priority.
 * Literal values take priority over patterns (i.e. Keywords will take priority over identifiers).
 */
class ScannerClassic(
    regexEngine: RegexEngine,
    terminals: List<Rule>
) : ScannerAbstract(regexEngine) {

    companion object {
        val LEAF_NONE = CompleteTreeDataNode(RuntimeRuleSet.UNDEFINED_RULE, -1, -1, -1, RulePosition.OPTION_NONE, emptyList())
    }

    //    val issues = IssueHolder(LanguageProcessorPhase.SCAN)
    override val validTerminals = terminals.filterNot { it.isEmptyTerminal || it.isEmptyListTerminal }

    init {
        this.matchables // iterate this to set the regexengine
    }

    override val kind: ScannerKind = ScannerKind.Classic

    override fun reset() {

    }

    override fun isEnd(sentence: Sentence, position: Int): Boolean = position >= sentence.text.length

    override fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean {
        val l = findOrTryCreateLeaf(sentence, position, terminalRule)
        return l?.rule?.equals(terminalRule) ?: false
    }

    override fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): CompleteTreeDataNode? {
        val l = _leaves[position]
        val res = when {
            terminalRule.isEmptyTerminal -> CompleteTreeDataNode(RuntimeRuleSet.EMPTY, position, position, position, RulePosition.OPTION_NONE,emptyList())
            terminalRule.isEmptyListTerminal -> CompleteTreeDataNode(RuntimeRuleSet.EMPTY_LIST, position, position, position, RulePosition.OPTION_NONE,emptyList())
            null == l -> {
                val lf = scanAt(sentence, position)
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

    private fun scanAt(sentence: Sentence, position: Int): CompleteTreeDataNode {
        val matches = validTerminals.mapNotNull {
            val matchLength = ((it as RuntimeRule).rhs as RuntimeRuleRhsTerminal).matchable!!.matchedLength(sentence, position)
            when (matchLength) {
                -1 -> null
                0 -> null
                else -> {
                    val nip = position + matchLength
                    CompleteTreeDataNode(it, position, nip, nip, RulePosition.OPTION_NONE,emptyList())
                }
            }
        }
        // prefer literals over patterns
        val longest = matches.maxWithOrNull { l1, l2 ->
            when {
                l1.rule.isPattern.not() && l2.rule.isPattern -> 1
                l1.rule.isPattern && l2.rule.isPattern.not() -> -1
                else -> when {
                    l1.nextInputPosition > l2.nextInputPosition -> 1
                    l2.nextInputPosition > l1.nextInputPosition -> -1
                    else -> 0
                }
            }
        }
        return when (longest) {
            null -> LEAF_NONE
            else -> longest
        }
    }

}