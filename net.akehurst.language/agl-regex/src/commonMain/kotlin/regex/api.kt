/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.regex.api

import kotlin.jvm.JvmInline

enum class RegexEngineKind {
    PLATFORM, AGL
}

interface RegexMatcher {

    data class MatchResultAgl(
        override val matchedText: String,
        //override val eolPositions: List<Int>
    ) : MatchResult

    fun match(text: CharSequence, startPosition: Int = 0): MatchResult?

    fun matchAt(text: String, atPosition: Int): MatchResult?

    fun matchesAt(text: String, atPosition: Int): Boolean

}

interface Regex {
    fun matchesAt(text: String, atPosition: Int): Boolean
    fun matchAt(text: String, atPosition: Int): MatchResult?
}

interface RegexEngine {
    val kind: RegexEngineKind
    fun createFor(pattern: String): Regex
}

interface MatchResult {
    val matchedText: String
    //val eolPositions: List<Int>
}

/**
 * with characters special to AGL (i.e. ") having a \ before
 */
interface EscapedValue {
    val value: String
    val unescapedFromAgl: UnescapedValue
}

/**
 * with characters special to AGL (i.e. ") not escaped
 */
interface UnescapedValue {
    val value: String
    val escapedFoAgl: EscapedValue
    val escapedForRegex: String
}

@JvmInline
value class EscapedPattern(override val value: String) : EscapedValue {
    override val unescapedFromAgl: UnescapedPattern get() = CommonRegexPatterns.unescape_PATTERN(value)
}

@JvmInline
value class UnescapedPattern(override val value: String) : UnescapedValue {
    override val escapedFoAgl: EscapedPattern get() = CommonRegexPatterns.escape_PATTERN(value)
    override val escapedForRegex: String
        get() =
            value

    fun toRegex() = escapedForRegex.toRegex()
}

@JvmInline
value class EscapedLiteral(override val value: String) : EscapedValue {
    override val unescapedFromAgl: UnescapedLiteral get() = CommonRegexPatterns.unescape_LITERAL(value)
}

@JvmInline
value class UnescapedLiteral(override val value: String) : UnescapedValue {
    override val escapedFoAgl: EscapedLiteral get() = CommonRegexPatterns.escape_LITERAL(value)
    override val escapedForRegex: String
        get() =
            kotlin.text.Regex.escape(value)
}

object CommonRegexPatterns {
    val PATTERN = UnescapedPattern("\"(\\\\\"|[^\"])+\"")  //  "(\\"|[^"])+"  escaped for java, not for AGL
    fun unescape_PATTERN(escaped: String) = UnescapedPattern(
        escaped
            .replace("\\\"", "\"")
//            .replace("\\\\", "\\")
    )

    fun escape_PATTERN(unescaped: String) = EscapedPattern(
        unescaped
//            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    )

    val LITERAL = UnescapedPattern("'(\\\\\\\\|\\\\'|[^'\\\\])+'") // '(\\\\|\\'|[^'\\])+'  escaped for java, not for AGL
    fun unescape_LITERAL(escaped: String) = UnescapedLiteral(
        escaped
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    )

    fun escape_LITERAL(unescaped: String) = EscapedLiteral(
        unescaped
            .replace("\\", "\\\\")
            .replace("'", "\\'")
    )

}