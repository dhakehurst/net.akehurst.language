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

data class ScanByLineEndState(
    val tokens:List<LeafData>,
    val eolPosition: Int,
    val leftOverText: String
)

abstract class ScannerAbstract(
    override val regexEngine: RegexEngine
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

    override fun scan(sentence: Sentence, options: ScanOptions?): ScanResult {
        val opts = options ?: ScanOptionsDefault()
        //TODO: improve this algorithm...it is not efficient
        this.reset()
        val startAtPosition = opts.startAtPosition
        val offsetPosition = opts.offsetPosition
        val inputText = sentence.text
        val issues = IssueHolder(LanguageProcessorPhase.SCAN)
        return when {
            opts.resultsByLine -> {
                val result = mutableListOf<List<LeafData>>()
                val lines = sentence.text.split("\n")
                var state = ScanByLineEndState(emptyList(), -1, "")
                for(ln in lines) {
                    val res = scanByLine(state,sentence, offsetPosition)
                    result.add(res.tokens)
                    state = res
                }
                ScanResultDefault(result, issues)
            }
            else -> {
                val result = mutableListOf<LeafData>()
                var position = startAtPosition
                var nextInputPosition = position
                var currentUndefinedText = ""
                var currentUndefinedStart = -1
                while (nextInputPosition < inputText.length) {
                    val matches: List<LeafData> = this.matchables.mapNotNull {
                        val matchLength = it.matchedLength(sentence, position)
                        when (matchLength) {
                            -1 -> null
                            0 -> null
                            else -> {
                                //val loc = sentence.locationFor(startPosition, matchLength)
                                //val matchedText = inputText.substring(startPosition, startPosition + matchLength)
                                LeafData(it.tag, it.kind == MatchableKind.REGEX, position + offsetPosition, matchLength, emptyList())
                            }
                        }
                    }
                    // prefer literals over patterns
                    val longest = matches.maxWithOrNull(Comparator<LeafData> { l1, l2 ->
                        when {
                            l1.isPattern.not() && l2.isPattern -> 1
                            l1.isPattern && l2.isPattern.not() -> -1
                            else -> when {
//                        l1.location.length > l2.location.length -> 1
//                        l2.location.length > l1.location.length -> -1
                                l1.length > l2.length -> 1
                                l2.length > l1.length -> -1
                                else -> 0
                            }
                        }
                    })
                    when {
                        //(null == longest || longest.location.length == 0) -> {
                        (null == longest || longest.length == 0) -> {
                            val text = inputText[nextInputPosition].toString()
                            if (-1 == currentUndefinedStart) {
                                currentUndefinedStart = nextInputPosition
                            }
                            currentUndefinedText += text
                            nextInputPosition += 1
                        }

                        else -> {
                            if (-1 != currentUndefinedStart) {
//                        val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
//                        val ud = LeafData(undefined.tag, false, loc, emptyList())
                                val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
                                result.add(ud)
                                currentUndefinedStart = -1
                                currentUndefinedText = ""
                            }
                            result.add(longest)
//                    nextInputPosition += longest.location.length
                            nextInputPosition += longest.length
                        }
                    }
                    position = nextInputPosition
                }
                //catch undefined stuff at end
                if (-1 != currentUndefinedStart) {
                    val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
//            val ud = LeafData(undefined.tag, false, loc, emptyList())
                    val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
                    result.add(ud)
                }
                 ScanResultDefault(listOf(result), issues)
            }
        }
    }

    /*
     * if tokens pass over an eol then we must scan that portion twice
     * once for each line
     */
    fun scanByLine(previousState:ScanByLineEndState, sentence: Sentence, offsetPosition:Int): ScanByLineEndState {
        val result = mutableListOf<LeafData>()
        // start scanning at start of leftOverText
        val startPosition = previousState.eolPosition - previousState.leftOverText.length  + 1
        var currentUndefinedText = ""
        var currentUndefinedStart = -1
        var nextInputPosition = startPosition
        while (nextInputPosition < sentence.text.length) {
            val matches: List<LeafData> = this.matchables.mapNotNull {
                val matchLength = it.matchedLength(sentence, nextInputPosition)
                when (matchLength) {
                    -1 -> null
                    0 -> null
                    else -> {
                        LeafData(it.tag, it.kind == MatchableKind.REGEX, nextInputPosition + offsetPosition, matchLength, emptyList())
                    }
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWithOrNull(Comparator<LeafData> { l1, l2 ->
                when {
                    l1.isPattern.not() && l2.isPattern -> 1
                    l1.isPattern && l2.isPattern.not() -> -1
                    else -> when {
//                        l1.location.length > l2.location.length -> 1
//                        l2.location.length > l1.location.length -> -1
                        l1.length > l2.length -> 1
                        l2.length > l1.length -> -1
                        else -> 0
                    }
                }
            })
            when {
                //(null == longest || longest.location.length == 0) -> {
                (null == longest || longest.length == 0) -> {
                    val text = sentence.text[nextInputPosition].toString()
                    if (-1 == currentUndefinedStart) {
                        currentUndefinedStart = nextInputPosition
                    }
                    currentUndefinedText += text
                    nextInputPosition += 1
                }

                else -> {
                    if (-1 != currentUndefinedStart) {
//                        val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
//                        val ud = LeafData(undefined.tag, false, loc, emptyList())
                        val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
                        result.add(ud)
                        currentUndefinedStart = -1
                        currentUndefinedText = ""
                    }
                    result.add(longest)
//                    nextInputPosition += longest.location.length
                    nextInputPosition += longest.length
                }
            }
        }
        //catch undefined stuff at end
        if (-1 != currentUndefinedStart) {
            val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
//            val ud = LeafData(undefined.tag, false, loc, emptyList())
            val ud = LeafData(RuntimeRuleSet.UNDEFINED_RULE.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
            result.add(ud)
        }
        val leftOverText = ""
        return ScanByLineEndState(result, nextInputPosition, leftOverText)
    }

}