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

package net.akehurst.language.scanner.api

import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.regex.api.Regex
import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.LeafData
import net.akehurst.language.sppt.api.SpptDataNode

interface ScanResult {
    val tokens: List<LeafData>
    val issues: IssueCollection<LanguageIssue>
}

enum class ScannerKind {
    OnDemand, Classic
}

interface Scanner {
    val kind: ScannerKind
    val regexEngine: RegexEngine
    val matchables: List<Matchable>

    fun reset()
    fun isEnd(sentence: Sentence, position: Int): Boolean
    fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean
    fun matchedLength(sentence: Sentence, position: Int, terminalRule: Rule):Int
    fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): SpptDataNode?

    fun scan(sentence: Sentence, startAtPosition: Int = 0, offsetPosition: Int = 0): ScanResult
}

interface ScanOptions {
}

enum class MatchableKind { EOT, LITERAL, REGEX }

data class Matchable(
    val tag: String,
    /**
     * must NOT be empty/blank
     * when(kind) {
     *   EOT -> ""
     *   LITERAL -> literal text to match
     *   REGEX -> Regular Expression
     * }
     */
    val expression: String,
    val kind: MatchableKind
) {
    // create this so Regex is cached
    private var _regEx: Regex? = null

    fun using(regexEngine: RegexEngine): Matchable {
        _regEx = if (MatchableKind.REGEX == kind) regexEngine.createFor(expression) else null
        return this
    }

    /**
     * true if the expression matches the text at the given position
     */
    fun isLookingAt(sentence: Sentence, atPosition: Int): Boolean = when (kind) {
        MatchableKind.EOT -> atPosition >= sentence.text.length
        MatchableKind.LITERAL -> sentence.text.regionMatches(atPosition, expression, 0, expression.length)
        MatchableKind.REGEX -> _regEx!!.matchesAt(sentence.text, atPosition)
    }

    /**
     * length of text matched at the given position, -1 if no match
     */
    fun matchedLength(sentence: Sentence, atPosition: Int): Int = when (kind) {
        MatchableKind.EOT -> if (atPosition >= sentence.text.length) 0 else -1
        MatchableKind.LITERAL -> when {
            sentence.text.regionMatches(atPosition, expression, 0, expression.length) -> expression.length
            else -> -1
        }

        MatchableKind.REGEX -> {
            val m = _regEx!!.matchAt(sentence.text, atPosition)
            m?.let {
                //sentence.setEolPositions(m.eolPositions)
                it.matchedText.length
            } ?: -1
        }
    }
}

