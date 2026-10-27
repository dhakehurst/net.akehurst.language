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

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsTerminal
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.scanner.api.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.LeafData

class ScanOptionsDefault(
    override var enabled:Boolean = true,
    override var resultsByLine:Boolean = false,
    override var startAtPosition: Int = 0,
    override var offsetPosition: Int = 0
) : ScanOptions {
    override fun clone() = ScanOptionsDefault(
        enabled = enabled,
        resultsByLine = resultsByLine,
        startAtPosition = startAtPosition,
        offsetPosition = offsetPosition
    )
}


data class ScanResultDefault(
    override val tokensByLine: List<List<LeafData>>,
    override val issues: IssueCollection<LanguageIssue>
) : ScanResult {
    override val allTokens: List<LeafData> by lazy { tokensByLine.flatten() }
}

/**
 * @param literalOverLongerPattern when false (the default) the longest match wins and a literal only beats
 *   a pattern when both match the same length. When true a literal always beats a pattern, even a longer one.
 *   Only affects which token is chosen when a scanner picks one token per position, i.e. [scan] and [ScannerClassic].
 */
abstract class ScannerAbstract(
    override val regexEngine: RegexEngine,
    val literalOverLongerPattern: Boolean = false
) : Scanner {

    abstract val validTerminals: List<Rule>

    override val matchables: List<Matchable> by lazy {
        validTerminals.mapNotNull {
            ((it as RuntimeRule).rhs as RuntimeRuleRhsTerminal).matchable?.using(regexEngine)
        }.toSet().toList()
    }

    override fun isEnd(sentence: Sentence, position: Int): Boolean = position >= sentence.text.length

    override fun matchedLength(sentence: Sentence, position: Int, terminalRule: Rule):Int {
        val rhs = (terminalRule as RuntimeRule).rhs as RuntimeRuleRhsTerminal
        val len = rhs.matchable?.matchedLength(sentence, position) ?: -1
        return len
    }

    /**
     * Orders two matches at the same position; the greater one is preferred.
     */
    protected fun compareMatches(isPattern1: Boolean, length1: Int, isPattern2: Boolean, length2: Int): Int {
        val byKind = when {
            isPattern1.not() && isPattern2 -> 1
            isPattern1 && isPattern2.not() -> -1
            else -> 0
        }
        val byLength = length1.compareTo(length2)
        return when {
            literalOverLongerPattern -> if (0 != byKind) byKind else byLength
            else -> if (0 != byLength) byLength else byKind
        }
    }

    override fun scan(sentence: Sentence, options: ScanOptions?): ScanResult {
        val opts = options ?: ScanOptionsDefault()
        //TODO: improve this algorithm...it is not efficient
        this.reset()
        val issues = IssueHolder(LanguageProcessorPhase.SCAN)
        val tokens = scanTokens(sentence, opts.startAtPosition, opts.offsetPosition)
        return when {
            opts.resultsByLine -> ScanResultDefault(splitByLine(sentence, tokens, opts.offsetPosition), issues)
            else -> ScanResultDefault(listOf(tokens), issues)
        }
    }

    private fun scanTokens(sentence: Sentence, startAtPosition: Int, offsetPosition: Int): List<LeafData> {
        val inputText = sentence.text
        val result = mutableListOf<LeafData>()
        var position = startAtPosition
        var currentUndefinedStart = -1
        while (position < inputText.length) {
            val matches: List<LeafData> = this.matchables.mapNotNull {
                val matchLength = it.matchedLength(sentence, position)
                when (matchLength) {
                    -1 -> null
                    0 -> null
                    else -> LeafData(it.tag, it.kind == MatchableKind.REGEX, position + offsetPosition, matchLength, emptyList())
                }
            }
            val best = matches.maxWithOrNull { l1, l2 -> compareMatches(l1.isPattern, l1.length, l2.isPattern, l2.length) }
            when (best) {
                null -> {
                    if (-1 == currentUndefinedStart) {
                        currentUndefinedStart = position
                    }
                    position += 1
                }

                else -> {
                    if (-1 != currentUndefinedStart) {
                        val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, position - currentUndefinedStart, emptyList())
                        result.add(ud)
                        currentUndefinedStart = -1
                    }
                    result.add(best)
                    position += best.length
                }
            }
        }
        //catch undefined stuff at end
        if (-1 != currentUndefinedStart) {
            val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, position - currentUndefinedStart, emptyList())
            result.add(ud)
        }
        return result
    }

    /**
     * Puts each token on the line where it is found (one list per line of the sentence).
     * A token that passes over an EOL is split into segments, one per line,
     * and the EOL character is its own segment, the last one on its line
     * (the same convention as SharedPackedParseTree.tokensByLine).
     */
    private fun splitByLine(sentence: Sentence, tokens: List<LeafData>, offsetPosition: Int): List<List<LeafData>> {
        val text = sentence.text
        val eolPositions = SentenceDefault.eolPositions(text)
        val lines = List(eolPositions.size + 1) { mutableListOf<LeafData>() }
        var line = 0
        for (tk in tokens) {
            val start = tk.position - offsetPosition
            val end = start + tk.length
            while (line < eolPositions.size && eolPositions[line] < start) line++
            var segStart = start
            while (segStart < end) {
                val eol = text.indexOf('\n', segStart)
                if (-1 == eol || eol >= end) {
                    lines[line].add(tk.copy(position = segStart + offsetPosition, length = end - segStart))
                    segStart = end
                } else {
                    if (eol > segStart) {
                        lines[line].add(tk.copy(position = segStart + offsetPosition, length = eol - segStart))
                    }
                    lines[line].add(tk.copy(position = eol + offsetPosition, length = 1))
                    line++
                    segStart = eol + 1
                }
            }
        }
        return lines
    }

}
