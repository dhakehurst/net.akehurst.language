/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.regex.agl

import net.akehurst.kotlinx.collections.mutableStackOf
import net.akehurst.language.regex.api.RegexMatcher

class RegexParser {

    internal enum class EscapeKind { SINGLE, OPTIONS, LITERAL }

    private companion object {
        val PREC_GROUP_OPEN = 1
        val PREC_GROUP_CLOSE = 1
        val PREC_LITERAL = 2
        val PREC_MULTI_01 = 3
        val PREC_MULTI_0n = 4
        val PREC_MULTI_1n = 5
        val PREC_REP = 6
        val PREC_CONCAT = 7
        val PREC_CHOICE = 8

        val PREDEFINED_DIGIT = Pair(EscapeKind.SINGLE, (CharacterMatcher(MatcherKind.RANGE, '0', '9')))
        val PREDEFINED_DIGIT_NEGATED = Pair(EscapeKind.SINGLE, (CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(CharacterMatcher(MatcherKind.RANGE, '0', '9')))))
        val PREDEFINED_UNICODE_LINEBREAK = Pair(
            EscapeKind.SINGLE, (CharacterMatcher(
                MatcherKind.ONE_OF, options = arrayOf(
                    CharacterMatcher(MatcherKind.LITERAL, '\u000A'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u000B'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u000C'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u000D'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u0085'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u2029')
//TODO:                CharacterSequence(listOf('\u000D','\u000A'))
                )
            ))
        )
        val PREDEFINED_WHITESPACE = Pair(
            EscapeKind.SINGLE, (CharacterMatcher(
                MatcherKind.ONE_OF, options = arrayOf(
                    CharacterMatcher(MatcherKind.LITERAL, ' '),
                    CharacterMatcher(MatcherKind.LITERAL, '\t'),
                    CharacterMatcher(MatcherKind.LITERAL, '\n'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u000B'),
                    CharacterMatcher(MatcherKind.LITERAL, '\u000C'),
                    CharacterMatcher(MatcherKind.LITERAL, '\r')
                )
            ))
        )
        val PREDEFINED_WHITESPACE_NEGATED = Pair(
            EscapeKind.SINGLE, CharacterMatcher(
                MatcherKind.NEGATED, options = arrayOf(
                    CharacterMatcher(
                        MatcherKind.ONE_OF, options = arrayOf(
                            CharacterMatcher(MatcherKind.LITERAL, ' '),
                            CharacterMatcher(MatcherKind.LITERAL, '\t'),
                            CharacterMatcher(MatcherKind.LITERAL, '\n'),
                            CharacterMatcher(MatcherKind.LITERAL, '\u000B'),
                            CharacterMatcher(MatcherKind.LITERAL, '\u000C'),
                            CharacterMatcher(MatcherKind.LITERAL, '\r')
                        )
                    )
                )
            )
        )
        val PREDEFINED_WORD = Pair(
            EscapeKind.SINGLE, CharacterMatcher(
                MatcherKind.ONE_OF, options = arrayOf(
                    CharacterMatcher(MatcherKind.RANGE, 'a', 'z'),
                    CharacterMatcher(MatcherKind.RANGE, 'A', 'Z'),
                    CharacterMatcher(MatcherKind.LITERAL, '_'),
                    CharacterMatcher(MatcherKind.RANGE, '0', '9')
                )
            )
        )
        val PREDEFINED_WORD_NEGATED = Pair(
            EscapeKind.SINGLE, CharacterMatcher(
                MatcherKind.NEGATED, options = arrayOf(
                    CharacterMatcher(
                        MatcherKind.ONE_OF, options = arrayOf(
                            CharacterMatcher(MatcherKind.RANGE, 'a', 'z'),
                            CharacterMatcher(MatcherKind.RANGE, 'A', 'Z'),
                            CharacterMatcher(MatcherKind.LITERAL, '_'),
                            CharacterMatcher(MatcherKind.RANGE, '0', '9')
                        )
                    )
                )
            )
        )
    }

    private class Flags(
        var caseInsensitive: Boolean = false, // (?i), US-ASCII only, as Kotlin/Java without UNICODE_CASE
        var multiline: Boolean = false, // (?m)
        var dotAll: Boolean = false, // (?s)
        var comments: Boolean = false // (?x)
    ) {
        fun copy() = Flags(caseInsensitive, multiline, dotAll, comments)
    }

    private lateinit var patternX: String
    private lateinit var pattern: String
    private var pp = 0
    private lateinit var matcherBuilder: RegexMatcherBuilder
    private var flags = Flags()

    /**
     * Parses a regular expression with the syntax and meaning of Kotlin's Regex on the JVM (java.util.regex.Pattern).
     * Not supported (an error is reported): back references, atomic groups, possessive quantifiers,
     * the flags d, u, U, c, and Unicode scripts, blocks and binary properties in \p{..}.
     */
    fun parse(pattern: String): RegexMatcher {
        this.pattern = pattern
        patternX = pattern + 0.toChar().toString()
        pp = 0
        flags = Flags()
        matcherBuilder = RegexMatcherBuilder(pattern)
        this.parseAlternation()
        if (this.more) fail("Unmatched closing ')'")
        this.matcherBuilder.concatenateGoal()
        return this.matcherBuilder.build()
    }

    private val more: Boolean get() = pp < pattern.length

    private fun peek(offset: Int = 0): Char = this.patternX.getOrElse(pp + offset) { 0.toChar() }

    private fun next(): Char {
        if (this.more.not()) fail("Unexpected end of pattern")
        val c = this.patternX[pp]
        this.pp++
        return c
    }

    private fun fail(message: String): Nothing = error("Failed to parse regex \"$pattern\" at position $pp: $message")

    // alternation = sequence ('|' sequence)*
    private fun parseAlternation() {
        this.parseSequence()
        while (this.more && peek() == '|') {
            pp++
            this.parseSequence()
            this.matcherBuilder.choice()
        }
    }

    // sequence = quantifiedAtom*
    private fun parseSequence() {
        var count = 0
        while (true) {
            this.skipComments()
            if (this.more.not() || peek() == '|' || peek() == ')') break
            this.parseAtom()
            this.parseQuantifier()
            count++
            if (count > 1) this.matcherBuilder.concatenate()
        }
        if (0 == count) this.matcherBuilder.empty()
    }

    // with (?x), whitespace and comments from # to the end of the line are ignored
    private fun skipComments() {
        if (flags.comments) {
            while (this.more) {
                when (peek()) {
                    ' ', '\t', '\n', '\u000B', '\u000C', '\r' -> pp++
                    '#' -> while (this.more && RegexMatcherImpl.isLineTerminator(peek()).not()) pp++
                    else -> return
                }
            }
        }
    }

    private fun parseQuantifier() {
        this.skipComments()
        when (peek()) {
            '*' -> {
                pp++
                this.matcherBuilder.multi0n(this.parseQuantifierKind())
            }

            '+' -> {
                pp++
                this.matcherBuilder.multi1n(this.parseQuantifierKind())
            }

            '?' -> {
                pp++
                this.matcherBuilder.multi01(this.parseQuantifierKind())
            }

            '{' -> {
                pp++
                val n = this.parseNumber() ?: fail("Illegal repetition")
                val m = when (peek()) {
                    '}' -> n
                    ',' -> {
                        pp++
                        this.parseNumber() ?: -1
                    }

                    else -> fail("Unclosed counted closure")
                }
                if (peek() != '}') fail("Unclosed counted closure")
                pp++
                if (-1 != m && m < n) fail("Illegal repetition range")
                this.matcherBuilder.repetition(n, m, this.parseQuantifierKind())
            }

            else -> return
        }
        when (peek()) {
            '*', '+', '?' -> fail("Dangling meta character '${peek()}'")
            else -> Unit
        }
    }

    // returns true if lazy
    private fun parseQuantifierKind(): Boolean = when (peek()) {
        '?' -> {
            pp++
            true
        }

        '+' -> fail("Possessive quantifiers are not supported")
        else -> false
    }

    private fun parseNumber(): Int? {
        val sb = StringBuilder()
        while (peek() in '0'..'9') sb.append(next())
        return if (sb.isEmpty()) null else sb.toString().toIntOrNull() ?: fail("Number too large")
    }

    // pushes one fragment
    private fun parseAtom() {
        val c = peek()
        when (c) {
            '(' -> this.parseGroup()
            '[' -> {
                pp++
                this.matcherBuilder.characterClass(this.parseCharacterClass())
            }

            '.' -> {
                pp++
                when {
                    flags.dotAll -> this.matcherBuilder.matchAny()
                    else -> this.matcherBuilder.characterClass(NOT_LINE_TERMINATOR)
                }
            }

            '^' -> {
                pp++
                this.matcherBuilder.assertion(CharacterMatcher(if (flags.multiline) MatcherKind.ASSERT_START_OF_LINE else MatcherKind.ASSERT_START_OF_INPUT))
            }

            '$' -> {
                pp++
                this.matcherBuilder.assertion(CharacterMatcher(if (flags.multiline) MatcherKind.ASSERT_END_OF_LINE else MatcherKind.ASSERT_END_OF_INPUT_OR_FINAL_TERMINATOR))
            }

            '\\' -> {
                pp++
                this.parseEscape()
            }

            '*', '+', '?' -> fail("Dangling meta character '$c'")
            '{' -> fail("Illegal repetition")
            else -> {
                pp++
                if (c.isHighSurrogate() && peek().isLowSurrogate()) {
                    // one code point, so that a quantifier applies to both chars
                    this.literal(c)
                    this.literal(next())
                    this.matcherBuilder.concatenate()
                } else {
                    this.literal(c)
                }
            }
        }
    }

    private fun literal(c: Char) {
        if (flags.caseInsensitive && c.isAsciiLetter()) {
            this.matcherBuilder.characterClass(this.literalLeaf(c))
        } else {
            this.matcherBuilder.character(c)
        }
    }

    private fun literals(chars: CharSequence) {
        if (chars.isEmpty()) {
            this.matcherBuilder.empty()
        } else {
            chars.forEachIndexed { i, ch ->
                this.literal(ch)
                if (i > 0) this.matcherBuilder.concatenate()
            }
        }
    }

    private fun parseGroup() {
        pp++ // (
        val outerFlags = flags.copy()
        if (peek() == '?') {
            pp++
            when (val k = next()) {
                ':' -> this.parseGroupContent()
                '=' -> this.parseLookaround(MatcherKind.ASSERT_LOOKAHEAD)
                '!' -> this.parseLookaround(MatcherKind.ASSERT_NEGATIVE_LOOKAHEAD)
                '>' -> fail("Atomic groups (?>X) are not supported")
                '<' -> when (peek()) {
                    '=' -> {
                        pp++
                        this.parseLookaround(MatcherKind.ASSERT_LOOKBEHIND)
                    }

                    '!' -> {
                        pp++
                        this.parseLookaround(MatcherKind.ASSERT_NEGATIVE_LOOKBEHIND)
                    }

                    else -> { // named group, the name is not used as groups are not captured
                        if (peek().isAsciiLetter().not()) fail("Capturing group name does not start with a Latin letter")
                        while (peek().isAsciiLetter() || peek() in '0'..'9') pp++
                        if (next() != '>') fail("Named capturing group is missing trailing '>'")
                        this.parseGroupContent()
                    }
                }

                else -> {
                    pp--
                    this.parseFlags()
                    when (next()) {
                        ')' -> { // (?flags) changes the flags until the end of the enclosing group
                            this.matcherBuilder.empty()
                            return
                        }

                        ':' -> this.parseGroupContent() // (?flags:X)
                        else -> fail("Unknown inline modifier")
                    }
                }
            }
        } else {
            this.parseGroupContent()
        }
        flags = outerFlags
    }

    private fun parseGroupContent() {
        this.parseAlternation()
        if (peek() != ')' || this.more.not()) fail("Unclosed group")
        pp++
    }

    private fun parseLookaround(kind: MatcherKind) {
        this.parseGroupContent()
        this.matcherBuilder.lookaround(kind)
    }

    private fun parseFlags() {
        var on = true
        while (true) {
            when (val f = peek()) {
                '-' -> on = false
                'i' -> flags.caseInsensitive = on
                'm' -> flags.multiline = on
                's' -> flags.dotAll = on
                'x' -> flags.comments = on
                'd', 'u', 'U', 'c' -> fail("The flag '$f' is not supported")
                else -> return
            }
            pp++
        }
    }

    // after the \, pushes one fragment
    private fun parseEscape() {
        val c = next()
        when (c) {
            'd', 'D', 's', 'S', 'w', 'W', 'h', 'H', 'v', 'V' -> this.matcherBuilder.characterClass(this.predefinedClass(c))
            'p', 'P' -> this.matcherBuilder.characterClass(this.parseProperty(c == 'P'))
            'R' -> this.linebreak()
            'b' -> {
                if (peek() == '{') fail("\\b{g} is not supported")
                this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_WORD_BOUNDARY))
            }

            'B' -> this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_NOT_WORD_BOUNDARY))
            'A' -> this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_START_OF_INPUT))
            'z' -> this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_END_OF_INPUT))
            'Z' -> this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_END_OF_INPUT_OR_FINAL_TERMINATOR))
            'G' -> this.matcherBuilder.assertion(CharacterMatcher(MatcherKind.ASSERT_START_OF_MATCH))
            'Q' -> this.literals(this.parseQuoted())
            'E' -> this.matcherBuilder.empty() // \E without \Q is ignored
            'k', in '1'..'9' -> fail("Back references are not supported")
            'X', 'N' -> fail("\\$c is not supported")
            else -> this.literals(this.escapedChar(c))
        }
    }

    // \Q...\E, the chars are literal, the end of the pattern also ends it
    private fun parseQuoted(): String {
        val sb = StringBuilder()
        while (this.more) {
            if (peek() == '\\' && peek(1) == 'E') {
                pp += 2
                return sb.toString()
            }
            sb.append(next())
        }
        return sb.toString()
    }

    // the char(s) of an escape that is a single character (a code point above 0xFFFF is two chars)
    private fun escapedChar(c: Char): String = when (c) {
        't' -> "\t"
        'n' -> "\n"
        'r' -> "\r"
        'f' -> "\u000C"
        'a' -> "\u0007"
        'e' -> "\u001B"
        'c' -> (next().code xor 64).toChar().toString()
        '0' -> {
            val digits = StringBuilder()
            while (digits.length < 3 && peek() in '0'..'7') {
                if (digits.length == 2 && digits[0] > '3') break
                digits.append(next())
            }
            if (digits.isEmpty()) fail("Illegal octal escape sequence")
            digits.toString().toInt(8).toChar().toString()
        }

        'x' -> {
            val codePoint = if (peek() == '{') {
                pp++
                val sb = StringBuilder()
                while (peek() != '}') {
                    if (this.more.not()) fail("Unclosed hexadecimal escape sequence")
                    sb.append(next())
                }
                pp++
                sb.toString().toIntOrNull(16)?.takeIf { it <= 0x10FFFF } ?: fail("Illegal hexadecimal escape sequence")
            } else {
                this.parseHexDigits(2)
            }
            codePointToString(codePoint)
        }

        'u' -> this.parseHexDigits(4).toChar().toString()
        else -> when {
            c.isAsciiLetter() || c in '0'..'9' -> fail("Illegal/unsupported escape sequence '\\$c'")
            else -> c.toString() // any other char is itself
        }
    }

    private fun parseHexDigits(count: Int): Int {
        var v = 0
        for (i in 0 until count) {
            val d = next().digitToIntOrNull(16) ?: fail("Illegal hexadecimal escape sequence")
            v = v * 16 + d
        }
        return v
    }

    private fun codePointToString(cp: Int): String = when {
        cp < 0x10000 -> cp.toChar().toString()
        else -> {
            val v = cp - 0x10000
            charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
        }
    }

    // \R, as Kotlin/Java: \r\n or one of [\n\x0B\f\r\u0085\u2028\u2029].
    // On its own it can give back the \n of \r\n, e.g. \R\n matches "\r\n",
    // but with a quantifier (Java's Curly) each repetition is \r\n if it can be, e.g. \R{2} does not match "\r\n"
    private fun linebreak() {
        this.skipComments()
        val quantified = peek() in "*+?{"
        this.matcherBuilder.character('\r')
        this.matcherBuilder.character('\n')
        this.matcherBuilder.concatenate()
        if (quantified) {
            this.matcherBuilder.character('\r')
            this.matcherBuilder.character('\n')
            this.matcherBuilder.lookaround(MatcherKind.ASSERT_NEGATIVE_LOOKAHEAD)
            this.matcherBuilder.concatenate()
            this.matcherBuilder.choice()
            this.matcherBuilder.characterClass(CharacterMatcher(MatcherKind.ONE_OF, options = "\n\u000B\u000C\u0085\u2028\u2029".map { CharacterMatcher(MatcherKind.LITERAL, it) }.toTypedArray()))
        } else {
            this.matcherBuilder.characterClass(CharacterMatcher(MatcherKind.ONE_OF, options = "\n\u000B\u000C\r\u0085\u2028\u2029".map { CharacterMatcher(MatcherKind.LITERAL, it) }.toTypedArray()))
        }
        this.matcherBuilder.choice()
    }

    private fun predefinedClass(c: Char): CharacterMatcher = when (c) {
        'd' -> PREDEFINED_DIGIT.second
        'D' -> PREDEFINED_DIGIT_NEGATED.second
        's' -> PREDEFINED_WHITESPACE.second
        'S' -> PREDEFINED_WHITESPACE_NEGATED.second
        'w' -> PREDEFINED_WORD.second
        'W' -> PREDEFINED_WORD_NEGATED.second
        'h' -> HORIZONTAL_WHITESPACE
        'H' -> CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(HORIZONTAL_WHITESPACE))
        'v' -> VERTICAL_WHITESPACE
        'V' -> CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(VERTICAL_WHITESPACE))
        else -> error("should not happen")
    }

    // after \p or \P: \pL or \p{name}
    private fun parseProperty(negated: Boolean): CharacterMatcher {
        val name = if (peek() == '{') {
            pp++
            val sb = StringBuilder()
            while (peek() != '}') {
                if (this.more.not()) fail("Unclosed character family")
                sb.append(next())
            }
            pp++
            sb.toString()
        } else {
            next().toString()
        }
        val m = propertyMatcher(name) ?: fail("Unknown or unsupported character property name {$name}")
        return if (negated) CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(m)) else m
    }

    private fun propertyMatcher(name: String): CharacterMatcher? {
        POSIX_CLASSES[name]?.let { return it }
        val category = when {
            name.startsWith("general_category=") -> name.substring(17)
            name.startsWith("gc=") -> name.substring(3)
            name.startsWith("Is") -> name.substring(2)
            else -> name
        }
        return when {
            category == "LC" -> predicate("\\p{LC}") { it.category.code in setOf("Lu", "Ll", "Lt") }
            category.length == 1 && CharCategory.entries.any { it.code.startsWith(category) } -> predicate("\\p{$category}") { it.category.code.startsWith(category) }
            category.length == 2 && CharCategory.entries.any { it.code == category } -> predicate("\\p{$category}") { it.category.code == category }
            else -> null
        }
    }

    // after the [
    private fun parseCharacterClass(): CharacterMatcher {
        val negated = if (peek() == '^') {
            pp++
            true
        } else {
            false
        }
        var intersection: CharacterMatcher? = null
        var union = mutableListOf<CharacterMatcher>()
        var first = true
        while (true) {
            if (flags.comments) {
                while (peek() in " \t\n\u000B\u000C\r" && this.more) pp++
            }
            if (this.more.not()) fail("Unclosed character class")
            when {
                peek() == ']' && first.not() -> {
                    pp++
                    break
                }

                peek() == '[' -> {
                    pp++
                    union.add(this.parseCharacterClass())
                }

                peek() == '&' && peek(1) == '&' -> {
                    pp += 2
                    intersection = intersect(intersection, unionOf(union))
                    union = mutableListOf()
                }

                else -> this.parseCharacterClassItem(union)
            }
            first = false
        }
        val all = intersect(intersection, unionOf(union)) ?: CharacterMatcher(MatcherKind.ONE_OF)
        return if (negated) CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(all)) else all
    }

    // a char, an escape, or a range
    private fun parseCharacterClassItem(union: MutableList<CharacterMatcher>) {
        val from = when (peek()) {
            '\\' -> {
                pp++
                when (val c = next()) {
                    'd', 'D', 's', 'S', 'w', 'W', 'h', 'H', 'v', 'V' -> {
                        union.add(this.predefinedClass(c))
                        return
                    }

                    'p', 'P' -> {
                        union.add(this.parseProperty(c == 'P'))
                        return
                    }

                    'Q' -> {
                        this.parseQuoted().forEach { union.add(this.literalLeaf(it)) }
                        return
                    }

                    'E' -> return
                    else -> this.classEscapedChar(c)
                }
            }

            else -> next()
        }
        if (peek() == '-' && peek(1) != ']' && peek(1) != '[') {
            pp++ // -
            val to = when (peek()) {
                '\\' -> {
                    pp++
                    when (val c = next()) {
                        'd', 'D', 's', 'S', 'w', 'W', 'h', 'H', 'v', 'V', 'p', 'P', 'Q' -> fail("Illegal character range")
                        else -> this.classEscapedChar(c)
                    }
                }

                else -> next()
            }
            if (to < from) fail("Illegal character range")
            union.add(this.rangeLeaf(from, to))
        } else {
            union.add(this.literalLeaf(from))
        }
    }

    private fun classEscapedChar(c: Char): Char {
        if (c == 'b' || c == 'B') fail("Illegal/unsupported escape sequence '\\$c'")
        val s = this.escapedChar(c)
        if (s.length != 1) fail("Characters above \\uFFFF are not supported in a character class")
        return s[0]
    }

    private fun literalLeaf(c: Char): CharacterMatcher = when {
        flags.caseInsensitive && c.isAsciiLetter() -> CharacterMatcher(
            MatcherKind.ONE_OF, options = arrayOf(
                CharacterMatcher(MatcherKind.LITERAL, c.lowercaseChar()),
                CharacterMatcher(MatcherKind.LITERAL, c.uppercaseChar())
            )
        )

        else -> CharacterMatcher(MatcherKind.LITERAL, c)
    }

    private fun rangeLeaf(from: Char, to: Char): CharacterMatcher = when {
        flags.caseInsensitive -> predicate("[$from-$to]") { ch ->
            ch in from..to || (ch.isAsciiLetter() && (ch.uppercaseChar() in from..to || ch.lowercaseChar() in from..to))
        }

        else -> CharacterMatcher(MatcherKind.RANGE, from, to)
    }

    private fun unionOf(list: List<CharacterMatcher>): CharacterMatcher? = when (list.size) {
        0 -> null
        1 -> list[0]
        else -> CharacterMatcher(MatcherKind.ONE_OF, options = list.toTypedArray())
    }

    // an empty operand is ignored, as in Kotlin/Java
    private fun intersect(a: CharacterMatcher?, b: CharacterMatcher?): CharacterMatcher? = when {
        null == a -> b
        null == b -> a
        else -> CharacterMatcher(MatcherKind.ALL_OF, options = arrayOf(a, b))
    }
}

private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

private fun predicate(name: String, p: (Char) -> Boolean) = CharacterMatcher(MatcherKind.PREDICATE, name = name, predicate = p)

private fun charsOf(name: String, chars: String) = CharacterMatcher(MatcherKind.ONE_OF, name = name, options = chars.map { CharacterMatcher(MatcherKind.LITERAL, it) }.toTypedArray())

private val NOT_LINE_TERMINATOR = CharacterMatcher(MatcherKind.NEGATED, options = arrayOf(charsOf("line terminator", "\n\r\u0085  ")))

private val HORIZONTAL_WHITESPACE = CharacterMatcher(
    MatcherKind.ONE_OF, options = arrayOf(
        charsOf("", " \t  ᠎  　"),
        CharacterMatcher(MatcherKind.RANGE, ' ', ' ')
    )
)

private val VERTICAL_WHITESPACE = charsOf("\\v", "\n\u000B\u000C\r\u0085  ")

// as in Kotlin/Java, US-ASCII only
private val POSIX_CLASSES: Map<String, CharacterMatcher> by lazy {
    val punct = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
    val lower = CharacterMatcher(MatcherKind.RANGE, 'a', 'z')
    val upper = CharacterMatcher(MatcherKind.RANGE, 'A', 'Z')
    val digit = CharacterMatcher(MatcherKind.RANGE, '0', '9')
    val alpha = CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(lower, upper))
    val alnum = CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(lower, upper, digit))
    val graph = CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(alnum, charsOf("", punct)))
    mapOf(
        "Lower" to lower,
        "Upper" to upper,
        "ASCII" to CharacterMatcher(MatcherKind.RANGE, '\u0000', '\u007F'),
        "Alpha" to alpha,
        "Digit" to digit,
        "Alnum" to alnum,
        "Punct" to charsOf("\\p{Punct}", punct),
        "Graph" to graph,
        "Print" to CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(graph, CharacterMatcher(MatcherKind.LITERAL, ' '))),
        "Blank" to charsOf("\\p{Blank}", " \t"),
        "Cntrl" to CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(CharacterMatcher(MatcherKind.RANGE, '\u0000', '\u001F'), CharacterMatcher(MatcherKind.LITERAL, '\u007F'))),
        "XDigit" to CharacterMatcher(MatcherKind.ONE_OF, options = arrayOf(digit, CharacterMatcher(MatcherKind.RANGE, 'a', 'f'), CharacterMatcher(MatcherKind.RANGE, 'A', 'F'))),
        "Space" to charsOf("\\p{Space}", " \t\n\u000B\u000C\r"),
    )
}
