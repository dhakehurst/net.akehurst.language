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

//import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ScanResultDefault
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ScanResult
import net.akehurst.language.api.sppt.LeafData

enum class MatchableKind { EOT, LITERAL, REGEX }

data class Matchable(
    val tag: String,
    /**
     * must NOT be empty/blank
     * when {
     *   EOT -> ""
     *   LITERAL -> literal text to match
     *   REGEX -> Regular Expression
     * }
     */
    val expression: String,
    val kind: MatchableKind
) {
    // create this so Regex is cached
    private val _regEx = if (MatchableKind.REGEX == kind) Regex(expression) else null

    /**
     * true if the expression matches the text at the given position
     */
    fun isLookingAt(text: String, atPosition: Int): Boolean = when (kind) {
        MatchableKind.EOT -> atPosition >= text.length
        MatchableKind.LITERAL -> text.regionMatches(atPosition, expression, 0, expression.length)
        MatchableKind.REGEX -> _regEx!!.matchesAt(text, atPosition)
    }

    /**
     * length of text matched at the given position, -1 if no match
     */
    fun matchedLength(text: String, atPosition: Int): Int = when (kind) {
        MatchableKind.EOT -> if (atPosition >= text.length) 0 else -1
        MatchableKind.LITERAL -> when {
            text.regionMatches(atPosition, expression, 0, expression.length) -> expression.length
            else -> -1
        }

        MatchableKind.REGEX -> _regEx!!.matchAt(text, atPosition)?.value?.length ?: -1
    }
}

class AglScanner {

    fun scan(inputText: String, nonEmptyMatchables: List<Matchable>): ScanResult {
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!

        val sentence = SentenceDefault(inputText)
        val issues = IssueHolder(LanguageProcessorPhase.SCAN)
        val undefined = RuntimeRuleSet.UNDEFINED_RULE
        val result = mutableListOf<LeafData>()
        var startPosition = 0
        var nextInputPosition = 0
        var currentUndefinedText = ""
        var currentUndefinedStart = -1
        while (nextInputPosition < inputText.length) {
            val matches: List<LeafData> = nonEmptyMatchables.mapNotNull {
                val matchLength = it.matchedLength(inputText, startPosition)
                when (matchLength) {
                    -1 -> null
                    0 -> null
                    else -> {
                        val loc = sentence.locationFor(startPosition, matchLength)
                        val matchedText = inputText.substring(startPosition, startPosition + matchLength)
                        LeafData(it.tag, it.kind == MatchableKind.REGEX, loc, matchedText, emptyList())
                    }
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWithOrNull(Comparator<LeafData> { l1, l2 ->
                when {
                    l1.isPattern.not() && l2.isPattern -> 1
                    l1.isPattern && l2.isPattern.not() -> -1
                    else -> when {
                        l1.location.length > l2.location.length -> 1
                        l2.location.length > l1.location.length -> -1
                        else -> 0
                    }
                }
            })
            when {
                (null == longest || longest.location.length == 0) -> {
                    val text = inputText[nextInputPosition].toString()
                    if (-1 == currentUndefinedStart) {
                        currentUndefinedStart = nextInputPosition
                    }
                    currentUndefinedText += text
                    nextInputPosition += 1
                }

                else -> {
                    if (-1 != currentUndefinedStart) {
                        val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
                        val ud = LeafData(undefined.tag, false, loc, currentUndefinedText, emptyList())
                        result.add(ud)
                        currentUndefinedStart = -1
                        currentUndefinedText = ""
                    }
                    result.add(longest)
                    nextInputPosition += longest.location.length
                }
            }
            startPosition = nextInputPosition
        }
        //catch undefined stuff at end
        if (-1 != currentUndefinedStart) {
            val loc = sentence.locationFor(currentUndefinedStart, currentUndefinedText.length)
            val ud = LeafData(undefined.tag, false, loc, currentUndefinedText, emptyList())
            result.add(ud)
        }
        return ScanResultDefault(result, issues)
    }

}