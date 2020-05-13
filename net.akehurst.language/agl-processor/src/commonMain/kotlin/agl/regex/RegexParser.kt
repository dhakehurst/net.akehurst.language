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

package net.akehurst.language.agl.regex

import net.akehurst.language.collections.Stack

class RegexParserException(msg: String) : RuntimeException(msg)

class RegexParser(
        val pattern: String
) {

    enum class EscapeKind { SINGLE, OPTIONS, LITERAL }

    companion object {
        val PREC_GROUP_OPEN = 1
        val PREC_GROUP_CLOSE = 1
        val PREC_LITERAL = 2
        val PREC_MULTI_01 = 3
        val PREC_MULTI_0n = 4
        val PREC_MULTI_1n = 5
        val PREC_REP = 6
        val PREC_CONCAT = 7
        val PREC_CHOICE = 8

        val PREDEFINED_DIGIT = Pair(EscapeKind.SINGLE, (CharacterRange('0', '9')))
        val PREDEFINED_DIGIT_NEGATED = Pair(EscapeKind.SINGLE, (CharacterNegated(CharacterRange('0', '9'))))
        val PREDEFINED_WHITESPACE = Pair(EscapeKind.SINGLE, (CharacterOneOf(listOf(
                CharacterRange('a', 'z'),
                CharacterRange('A', 'Z'),
                CharacterSingle('_'),
                CharacterRange('0', '9')))
                ))
        val PREDEFINED_WHITESPACE_NEGATED = Pair(EscapeKind.SINGLE, (CharacterNegated(CharacterOneOf(listOf(
                CharacterRange('a', 'z'),
                CharacterRange('A', 'Z'),
                CharacterSingle('_'),
                CharacterRange('0', '9'))))
                ))
    }

    val patternX = pattern + 0.toChar().toString() // add something to the end of the string, saves doing an if (> length) in fun next()
    var pp = 0
    val matcherBuilder = RegexMatcherBuilder()

    fun parse(): RegexMatcher {
        this.parsePattern()
        return this.matcherBuilder.build()
    }

    private fun next(): Char {
        val c = this.patternX[pp]
        this.pp++
        return c
    }

    private fun parsePattern() {
        //this.matcherBuilder.start()
        val postfix = Stack<Pair<Int, () -> Unit>>()
        val opStack = Stack<Pair<Int, () -> Unit>>()
        var needConcat = Stack<Boolean>()
        needConcat.push(false)
        if (pattern.length > 0) {
            var c = this.next()
            while (pp <= pattern.length) {
                when (c) {
                    '\\' -> {
                        val escaped_matchers = this.parsePatternEscape()
                        when (escaped_matchers.first) {
                            EscapeKind.SINGLE -> {
                                val matcher = escaped_matchers.second as CharacterMatcher
                                postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.characterClass(matcher) }))
                                if (needConcat.pop()) {
                                    while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                        postfix.push(opStack.pop())
                                    }
                                    opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                                }
                                needConcat.push(true)
                            }
                            EscapeKind.LITERAL -> {
                                val literal = escaped_matchers.second as CharSequence
                                if (literal.length > 0) {
                                    val cc = literal[0]
                                    postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.character(cc) }))
                                    if (needConcat.pop()) {
                                        while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                            postfix.push(opStack.pop())
                                        }
                                        opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                                    }
                                    var i = 1
                                    while (i < literal.length) {
                                        val d = literal[i]
                                        postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.character(d) }))
                                        opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                                        i++
                                    }
                                    needConcat.push(true)
                                }
                            }
                        }
                        c = this.next()
                    }
                    '.' -> {
                        postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.matchAny() }))
                        if (needConcat.pop()) {
                            while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                postfix.push(opStack.pop())
                            }
                            opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                        }
                        needConcat.push(true)
                        c = this.next()
                    }
                    '[' -> {
                        val options = this.parseCharacterClass()
                        postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.characterClass(options) }))
                        if (needConcat.pop()) {
                            while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                postfix.push(opStack.pop())
                            }
                            opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                        }
                        needConcat.push(true)
                        c = this.next()
                    }
                    '(' -> {
                        needConcat.push(false)
                        opStack.push(Pair(PREC_GROUP_OPEN, { }))
                        c = this.next()
                        when (c) {
                            '?' -> {
                                TODO(pattern)
                                //ignore 'non capturing group' indicator, as this doesn't handle groups anyhow
                                c = this.next()
                                when (c) {
                                    ':' -> {
                                        c = this.next()
                                    }
                                    '=' -> {
                                        c = this.next()
                                    }
                                    '!' -> {
                                        c = this.next()
                                    }
                                    '>' -> {
                                        c = this.next()
                                    }
                                    '<' -> {
                                        TODO(pattern)
                                    }
                                    //TODO: idmsuxU !
                                    //TODO: idmsux !
                                    else -> { /* continue */
                                    }
                                }
                            }
                            else -> { /* continue */
                            }
                        }
                    }
                    '|' -> {
                        while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CHOICE) {
                            postfix.push(opStack.pop())
                        }
                        opStack.push(Pair(PREC_CHOICE, { this.matcherBuilder.choice() }))
                        needConcat.pop()
                        needConcat.push(false)
                        c = this.next()
                    }
                    '?' -> {
                        postfix.push(Pair(PREC_MULTI_01, { this.matcherBuilder.multi01() }))
                        c = this.next()
                        when (c) {
                            '?' -> {
                                TODO()
                            }
                            '+' -> {
                                TODO()
                            }
                            else -> { /* continue */
                            }
                        }
                    }
                    '+' -> {
                        postfix.push(Pair(PREC_MULTI_1n, { this.matcherBuilder.multi1n() }))
                        c = this.next()
                        when (c) {
                            '?' -> {
                                TODO(pattern)
                            }
                            '+' -> {
                                TODO(pattern)
                            }
                            else -> { /* continue */
                            }
                        }
                    }
                    '*' -> {
                        postfix.push(Pair(PREC_MULTI_0n, { this.matcherBuilder.multi0n() }))
                        c = this.next()
                        when (c) {
                            '?' -> {
                                TODO(pattern)
                            }
                            '+' -> {
                                TODO(pattern)
                            }
                            else -> { /* continue */
                            }
                        }
                    }
                    '{' -> {
                        val nb = StringBuilder()
                        c = this.next()
                        while (c in '0'..'9') {
                            nb.append(c)
                            c = this.next()
                        }
                        val n = nb.toString().toIntOrNull(10)
                                ?: throw RegexParserException("Counted repetition must be one of the forms {n} | {n,} | {n,m} where n and m are numbers")
                        when (c) {
                            '}' -> {
                                postfix.push(Pair(PREC_REP, { this.matcherBuilder.repetition(n, n) }))
                                c = this.next()
                            }
                            ',' -> {
                                c = this.next()
                                when (c) {
                                    '}' -> {
                                        postfix.push(Pair(PREC_REP, { this.matcherBuilder.repetition(n, -1) }))
                                        c = this.next()
                                    }
                                    else -> {
                                        val mb = StringBuilder()
                                        mb.append(c)
                                        c = this.next()
                                        while (c in '0'..'9') {
                                            mb.append(c)
                                            c = this.next()
                                        }
                                        val m = mb.toString().toIntOrNull(10)
                                                ?: throw RegexParserException("Counted repetition must be one of the forms {n} | {n,} | {n,m} where n and m are numbers")
                                        when (c) {
                                            '}' -> {
                                                postfix.push(Pair(PREC_REP, { this.matcherBuilder.repetition(n, m) }))
                                                c = this.next()
                                            }
                                            else -> throw RegexParserException("Counted repetition must be one of the forms {n} | {n,} | {n,m} where n and m are numbers")
                                        }
                                    }
                                }
                            }
                            else -> RegexParserException("Counted repetition must be one of the forms {n} | {n,} | {n,m} where n and m are numbers")
                        }
                    }
                    ')' -> {
                        while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN) {
                            postfix.push(opStack.pop())
                        }
                        if (opStack.isEmpty.not() && opStack.peek().first == PREC_GROUP_OPEN) opStack.pop()
                        needConcat.pop()
                        if (needConcat.pop()) {
                            while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                postfix.push(opStack.pop())
                            }
                            opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                        }
                        needConcat.push(true)
                        //postfix.push(Pair(PREC_GROUP_CLOSE, { this.matcherBuilder.finishGroup() }))
                        c = this.next()
                    }
                    else -> {
                        val cc = c
                        postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.character(cc) }))
                        if (needConcat.pop()) {
                            while (opStack.isEmpty.not() && opStack.peek().first != PREC_GROUP_OPEN && opStack.peek().first < PREC_CONCAT) {
                                postfix.push(opStack.pop())
                            }
                            opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                        }
                        needConcat.push(true)
                        c = this.next()
                    }
                }
            }
            while (opStack.isEmpty.not()) {
                postfix.push(opStack.pop())
            }
            postfix.elements.forEach {
                it.second.invoke()
            }
        }
        this.matcherBuilder.concatenateGoal()
    }

    private fun parsePatternEscape(): Pair<EscapeKind, Any> {
        var c = this.next()
        return when (c) {
            '\\' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '(' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '|' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '[' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '.' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '?' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '+' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            '*' -> Pair(EscapeKind.SINGLE, (CharacterSingle(c)))
            't' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\t')))
            'n' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\n')))
            'r' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\r')))
            'f' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\u000C')))
            'a' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\u0007')))
            'e' -> Pair(EscapeKind.SINGLE, (CharacterSingle('\u001B')))
            'c' -> TODO("Control char")
            'd' -> PREDEFINED_DIGIT
            'D' -> PREDEFINED_DIGIT_NEGATED
            's' -> Pair(EscapeKind.SINGLE, (CharacterOneOf(" \t\n\u000B\u000C\r")))
            'S' -> Pair(EscapeKind.SINGLE, (CharacterNegated(CharacterOneOf(" \t\n\u000B\u000C\r"))))
            'w' -> PREDEFINED_WHITESPACE
            'W' -> PREDEFINED_WHITESPACE_NEGATED
            '0' -> {
                TODO("Octal value")
            }
            'x' -> {
                TODO("hex value")
            }
            'u' -> {
                TODO("unicode hex value")
            }
            'Q' -> { // quote, literal value until \E
                val sb = StringBuilder()
                var end = false
                c = this.next()
                while (!end) {
                    when (c) {
                        '\\' -> {
                            c = this.next()
                            if ('E' == c) {
                                end = true
                            } else {
                                sb.append('\\')
                                sb.append(c)
                                c = this.next()
                            }
                        }
                        else -> {
                            sb.append(c)
                            c = this.next()
                        }
                    }
                }
                Pair(EscapeKind.LITERAL, sb.toString())
            }
            else -> TODO("$c at $pp in $pattern")
        }
    }

    private fun parseCharacterClass(): CharacterMatcher {
        val options = mutableListOf<CharacterMatcher>()
        var c = this.parseNextCharOrEscape()
        val negated = if ('^' == c) {
            c = this.parseNextCharOrEscape()
            true
        } else {
            false
        }
        var f = c
        while (c != ']') {
            c = this.parseNextCharOrEscape()
            when (c) {
                ']' -> {
                    options.add(CharacterSingle(f))
                }
                '-' -> {
                    c = this.parseNextCharOrEscape()
                    if (c == ']') {
                        options.add(CharacterSingle('-'))
                    } else {
                        options.add(CharacterRange(f, c))
                        c = this.parseNextCharOrEscape()
                        f = c
                    }
                }
                else -> {
                    options.add(CharacterSingle(f))
                    f = c
                }
            }
        }
        return if (negated) {
            CharacterNegated(CharacterOneOf(options))
        } else {
            CharacterOneOf(options)
        }
    }

    private fun parseNextCharOrEscape(): Char {
        val c = this.next()
        return when (c) {
            '\\' -> parseCharacterClassEscape()
            else -> c
        }
    }

    private fun parseCharacterClassEscape(): Char {
        var c = this.next()
        return when (c) {
            '\\' -> '\\'
            't' -> '\t'
            'n' -> '\n'
            'r' -> '\r'
            'f' -> '\u000C'
            'a' -> '\u0007'
            'e' -> '\u001B'
            'c' -> TODO("Control char")
            'u' -> {
                var unicodeChar = parseUnicode()
                unicodeChar
            }
            'x' -> {
                this.parseHex()
            }
            else -> throw RegexParserException("Unknown escape code '$c' in character class, at position ${this.pp} in ${pattern}, ")
        }
    }

    private fun parseHex(): Char {
        var hex = this.toHexInt(this.next(), 16) ?: throw RegexParserException("Cannot parse Hex, at position ${this.pp}")
        hex += this.toHexInt(this.next(), 1) ?: throw RegexParserException("Cannot parse Hex, at position ${this.pp}")
        return hex.toChar()
    }

    private fun parseUnicode(): Char {
        var unicode = this.toHexInt(this.next(), 4096) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toHexInt(this.next(), 256) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toHexInt(this.next(), 16) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toHexInt(this.next(), 1) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        return unicode.toChar()
    }

    private fun toHexInt(c: Char, power: Int): Int? {
        val v = c.toString().toIntOrNull(16)
        return if (null == v) null else v * power
    }
}