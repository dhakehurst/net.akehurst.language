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
    //
    // isLookingAt cache: per-terminal bit-packed (seen, matched) state indexed by
    // input position. Replaces a HashMap<Pair<Int,RuntimeRule>, Boolean> to avoid
    // a Pair allocation (and two Int boxings) on every lookahead test — which is
    // the dominant O(|Q|^3 * n^4) inner-loop operation per FORMAL_DEFINITION.md
    // §12.3 / §12.4. See PERFORMANCE_OPPORTUNITIES.md item #1.
    //
    // Each entry holds two IntArrays packed as bitsets: `seen[i] bit b` set means
    // position (i*32 + b) has been tested for the terminal; `matched[i] bit b`
    // set means the test returned true. The arrays grow geometrically as larger
    // positions are queried.
    private class IsLookingAtMask {
        var seen: IntArray = IntArray(4)
        var matched: IntArray = IntArray(4)
        fun ensureSize(chunkIdx: Int) {
            if (chunkIdx >= seen.size) {
                val newSize = maxOf(seen.size * 2, chunkIdx + 1)
                seen = seen.copyOf(newSize)
                matched = matched.copyOf(newSize)
            }
        }
    }
    private val isLookingAt_cache = HashMap<RuntimeRule, IsLookingAtMask>()

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
        val rt = terminalRule as RuntimeRule
        val chunkIdx = position ushr 5
        val bit = 1 shl (position and 31)
        val existing = isLookingAt_cache[rt]
        if (existing != null && chunkIdx < existing.seen.size && (existing.seen[chunkIdx] and bit) != 0) {
            return (existing.matched[chunkIdx] and bit) != 0
        }
        val rhs = rt.rhs as RuntimeRuleRhsTerminal
        val matched = rhs.matchable?.isLookingAt(sentence, position) ?: false
        val mask = existing ?: IsLookingAtMask().also { isLookingAt_cache[rt] = it }
        mask.ensureSize(chunkIdx)
        mask.seen[chunkIdx] = mask.seen[chunkIdx] or bit
        if (matched) mask.matched[chunkIdx] = mask.matched[chunkIdx] or bit
        return matched
    }

    internal fun isLookingAtAnyOf(sentence: Sentence, lh: LookaheadSetPart, position: Int): Boolean {
        return when {
            lh.includesRT -> error("lookahead must be real lookahead values, <RT> must be resolved")
            lh.includesEOT && this.isEnd(sentence, position) -> true
            else -> lh.content.any { this.isLookingAt(sentence, position, it) }
        }
    }

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