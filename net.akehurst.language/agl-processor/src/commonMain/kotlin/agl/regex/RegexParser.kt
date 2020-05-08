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

class RegexMatcherBuilder {

    companion object {

    }

    // List[StateNumber] -> Map<Unicode-Int, List<StateNumber>>
    val nfa = mutableListOf<State>()
    var nextStateNumber = 0
    var stack = Stack<Fragment>()
    var start:State? = null

    // return state number of new state
    private fun createState(isSplit: Boolean): State {
        val state = State(this.nextStateNumber, isSplit)
        this.nextStateNumber++
        this.nfa.add(state)
        return state
    }

    fun matchAny() {
        val state = this.createState(false)
        val trans = TransitionAny()
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun character(input: Char) {
        val state = this.createState(false)
        val trans = TransitionLiteral(input)
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun characterClass(first: Char, last: Char) {
        val state = this.createState(false)
        val trans = TransitionRange(first, last)
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun concatenate() {
        when (this.stack.size) {
            0 -> {
            }
            1 -> {
                this.start = this.stack.peek().start
            }
            else -> {
                val f2 = this.stack.pop()
                val f1 = this.stack.pop()
                f1.outgoing.forEach { it.to = f2.start }
                val frag = Fragment(f1.start, f2.outgoing)
                this.stack.push(frag)
                this.start = f1.start
            }
        }
    }

    fun choice() {
        val f2 = this.stack.pop()
        val f1 = this.stack.pop()
        val split = this.createState(true)
        val t1 = TransitionEmpty()
        t1.to = f1.start
        val t2 = TransitionEmpty()
        t2.to = f2.start
        split.outgoing.add(t1)
        split.outgoing.add(t2)
        val frag = Fragment(split, f1.outgoing + f2.outgoing)
        this.stack.push(frag)
        this.start = split
    }

    fun multi01() {
        val f1 = this.stack.pop()
        val split = this.createState(true)
        val t1 = TransitionEmpty()
        t1.to = f1.start
        val t2 = TransitionEmpty()
        split.outgoing.add(t1)
        split.outgoing.add(t2)
        val frag = Fragment(split, f1.outgoing + t2)
        this.stack.push(frag)
        this.start = split
    }

    fun multi1n() {
        val f1 = this.stack.pop()
        val split = this.createState(true)
        f1.outgoing.forEach { it.to = split }
        val t1 = TransitionEmpty()
        t1.to = f1.start
        val t2 = TransitionEmpty()
        split.outgoing.add(t1)
        split.outgoing.add(t2)
        val frag = Fragment(f1.start, listOf(t2))
        this.stack.push(frag)
        this.start = f1.start
    }

    fun multi0n() {
        val f1 = this.stack.pop()
        val split = this.createState(true)
        val t1 = TransitionEmpty()
        t1.to = f1.start
        val t2 = TransitionEmpty()
        split.outgoing.add(t1)
        split.outgoing.add(t2)
        f1.outgoing.forEach { it.to = split }
        val frag = Fragment(split, listOf(t2))
        this.stack.push(frag)
        this.start = split
    }

    fun startGroup() {
    }

    fun finishGroup() {
    }

    fun build(): RegexMatcher {
        return RegexMatcher(this.start!!, this.nfa)
    }

    fun concatenateGoal() {
        val f1 = this.stack.pop()
        f1.outgoing.forEach { it.to = RegexMatcher.MATCH_STATE }
    }
}

class RegexParserException(msg: String) : RuntimeException(msg)

class RegexParser(
        val pattern: String
) {

    companion object {
        val PREC_GROUP_CLOSE = 1
        val PREC_LITERAL = 2
        val PREC_MULTI_01 = 3
        val PREC_MULTI_0n = 4
        val PREC_MULTI_1n = 5
        val PREC_CONCAT = 6
        val PREC_CHOICE = 7
    }

    interface PatternSegment {

        class MatchAny : PatternSegment {
        }

        class MatchOneOf(options: List<Char>) : PatternSegment {
        }
    }

    var pp = 0
    val matcherBuilder = RegexMatcherBuilder()

    fun parse(): RegexMatcher {
        this.parsePattern()
        return this.matcherBuilder.build()
    }

    private fun next(): Char {
        val c = this.pattern[pp]
        this.pp++
        return c
    }

    private fun parsePattern() {
        val postfix = Stack<Pair<Int, () -> Unit>>()
        val opStack = Stack<Pair<Int, () -> Unit>>()
        while (pp < pattern.length) {
            val c = this.next()
            when (c) {
                '\\' -> this.parsePatternEscape()
                '.' -> {
                    postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.matchAny() }))
                    while (opStack.isEmpty.not() && opStack.peek().first < PREC_CONCAT) {
                        postfix.push(opStack.pop())
                    }
                    if (opStack.isEmpty)
                        opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                }
                '[' -> this.parseCharacterClass()
                '(' -> this.matcherBuilder.startGroup()
                '|' -> opStack.push(Pair(PREC_CHOICE, { this.matcherBuilder.choice() }))
                '?' -> postfix.push(Pair(PREC_MULTI_01, { this.matcherBuilder.multi01() }))
                '+' -> postfix.push(Pair(PREC_MULTI_1n, { this.matcherBuilder.multi1n() }))
                '*' -> postfix.push(Pair(PREC_MULTI_0n, { this.matcherBuilder.multi0n() }))
                ')' -> postfix.push(Pair(PREC_GROUP_CLOSE, { this.matcherBuilder.finishGroup() }))
                else -> {
                    postfix.push(Pair(PREC_LITERAL, { this.matcherBuilder.character(c) }))
                    while (opStack.isEmpty.not() && opStack.peek().first < PREC_CONCAT) {
                        postfix.push(opStack.pop())
                    }
                    if (opStack.isEmpty)
                        opStack.push(Pair(PREC_CONCAT, { this.matcherBuilder.concatenate() }))
                }
            }
        }
        while (opStack.isEmpty.not()) {
            postfix.push(opStack.pop())
        }
        postfix.elements.forEach{
            it.second.invoke()
        }
        this.matcherBuilder.concatenateGoal()
    }

    private fun parsePatternEscape() {
        val c = this.next()
        when (c) {
            '\\' -> this.matcherBuilder.character('\\')
            '(' -> this.matcherBuilder.character('(')
            '|' -> this.matcherBuilder.character('|')
            '[' -> this.matcherBuilder.character('[')
            '.' -> this.matcherBuilder.character('.')
            '?' -> this.matcherBuilder.character('?')
            '+' -> this.matcherBuilder.character('+')
            '*' -> this.matcherBuilder.character('*')
        }
    }

    private fun parseCharacterClass() {
        var c = this.next()
        var f = c
        while (c != ']') {
            when (c) {
                '\\' -> {
                    f = parseCharacterClassEscape()
                }
                '-' -> {
                    c = this.next()
                    this.matcherBuilder.characterClass(f, c)
                }
                else -> {
                    this.matcherBuilder.character(c)
                }
            }
            c = this.next()
        }
    }

    private fun parseCharacterClassEscape(): Char {
        val c = this.next()
        return when (c) {
            'u' -> {
                var unicodeChar = parseUnicode()
                unicodeChar
            }
            else -> throw RegexParserException("Unknown escape code in character class, at position ${this.pp}")
        }
    }

    private fun parseUnicode(): Char {
        var unicode = this.toInt(this.next(), 4096) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toInt(this.next(), 256) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toInt(this.next(), 16) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        unicode += this.toInt(this.next(), 1) ?: throw RegexParserException("Cannot parse Unicode, at position ${this.pp}")
        return unicode.toChar()
    }

    private fun toInt(c: Char, power: Int): Int? {
        val v = c.toString().toIntOrNull(16)
        return if (null == v) null else v * power
    }
}