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
    /**
     * if the scan options resultsByLine is false, the first list of LeafData contains all tokens
     * else each list represents a separate line
     */
    val tokensByLine: List<List<LeafData>>

    val allTokens: List<LeafData>

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

    fun scan(sentence: Sentence, options: ScanOptions?=null): ScanResult
}

interface ScanOptions {
    var enabled:Boolean
    var resultsByLine:Boolean
    var startAtPosition: Int
    var offsetPosition: Int
}

enum class MatchableKind { EOT, LITERAL, REGEX }

//TODO: FIXME:
// the same 'tag' could come from different RuleSets when using embedded grammars
// - it could have different rhs
// - even if the same rhs patten the matchable could be a different object
// -- in which case the _regEx is not set for one of them because of Sets
// don't want to pass in the RuntimeRule because don't want to serialise it
// could pass in the rr id ? - may end up with duplicate tags/patterns for diff rr ids?
// -- maybe that is correct!
class Matchable(
    val ruleSetNumber: Int,
    val ruleNumber:Int,
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

    override fun toString(): String = "Matchable($ruleSetNumber, $ruleNumber, $kind, $tag, $expression)"
    override fun hashCode(): Int  = Pair(ruleSetNumber, ruleNumber).hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is Matchable -> false
        else -> ruleSetNumber == other.ruleSetNumber && ruleNumber == other.ruleNumber
    }
}

