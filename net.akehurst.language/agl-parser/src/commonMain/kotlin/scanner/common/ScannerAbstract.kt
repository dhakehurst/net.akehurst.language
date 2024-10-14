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
) : ScanOptions


data class ScanResultDefault(
    override val tokens: List<LeafData>,
    override val issues: IssueCollection<LanguageIssue>
) : ScanResult

abstract class ScannerAbstract(
    override val regexEngine: RegexEngine
) : Scanner {

    abstract val validTerminals: List<Rule>

    override val matchables: List<Matchable> by lazy {
        validTerminals.mapNotNull {
            ((it as RuntimeRule).rhs as RuntimeRuleRhsTerminal).matchable?.using(regexEngine)
        }
    }

    override fun isEnd(sentence: Sentence, position: Int): Boolean = position >= sentence.text.length

    override fun scan(sentence: Sentence, startAtPosition: Int, offsetPosition: Int): ScanResult {
        //TODO: improve this algorithm...it is not efficient
        this.reset()
        val inputText = sentence.text
        val issues = IssueHolder(LanguageProcessorPhase.SCAN)
        val undefined = RuntimeRuleSet.UNDEFINED_RULE
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
                        val ud = LeafData(undefined.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
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
            val ud = LeafData(undefined.tag, false, currentUndefinedStart + offsetPosition, currentUndefinedText.length, emptyList())
            result.add(ud)
        }
        return ScanResultDefault(result, issues)
    }

}