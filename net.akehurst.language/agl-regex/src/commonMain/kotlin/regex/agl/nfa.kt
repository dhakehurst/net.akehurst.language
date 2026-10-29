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

internal class State(
    val number: Int,
    val isSplit: Boolean
) {
    val outgoing = mutableListOf<Transition>()

    /** for the split state of a loop (x* or x+): the transition that leaves the loop */
    var loopExit: Transition? = null

    /** for the state that starts each iteration of a loop: the split state of that loop */
    var iterationStartOf: State? = null

    val isGoal get() = this == RegexMatcherImpl.MATCH_STATE

    /** a zero width assertion: one transition of kind ASSERT */
    val isAssertion get() = isSplit.not() && outgoing.size == 1 && outgoing[0].kind == TransitionKind.ASSERT

    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is State -> this.number == other.number
            else -> false
        }
    }

    override fun toString(): String = "State{$number}"
}

internal class Fragment(
    val start: State,
    val outgoing: List<Transition>
) {
}

internal enum class MatcherKind {
    EMPTY, ANY, END_OF_LINE_OR_INPUT, NEGATED, LITERAL, ONE_OF, RANGE,

    // match one character
    PREDICATE, // the character satisfies 'predicate'
    ALL_OF, // the character matches all of 'options' (class intersection)

    // zero width assertions, used on transitions of kind ASSERT
    ASSERT_START_OF_INPUT, // ^ (not multiline), \A
    ASSERT_START_OF_LINE, // ^ (multiline)
    ASSERT_END_OF_INPUT, // \z
    ASSERT_END_OF_INPUT_OR_FINAL_TERMINATOR, // $ (not multiline), \Z
    ASSERT_END_OF_LINE, // $ (multiline)
    ASSERT_WORD_BOUNDARY, // \b
    ASSERT_NOT_WORD_BOUNDARY, // \B
    ASSERT_START_OF_MATCH, // \G
    ASSERT_LOOKAHEAD, // (?=X), X starts at 'subStart'
    ASSERT_NEGATIVE_LOOKAHEAD, // (?!X)
    ASSERT_LOOKBEHIND, // (?<=X), X has at most 'maxLength' characters
    ASSERT_NEGATIVE_LOOKBEHIND // (?<!X)
}

internal class CharacterMatcher(
    val kind: MatcherKind,
    val literal: Char = '\u0000', // min
    val max: Char = '\u0000',
    val options: Array<CharacterMatcher> = arrayOf(),//CharacterMatcher.EMPTY)
    val name: String = "",
    val predicate: ((Char) -> Boolean)? = null,
    val subStart: State? = null, // for lookaround: start of the NFA of the looked-at pattern, which ends at MATCH_STATE
    val maxLength: Int = 0 // for lookbehind: maximum number of characters the looked-at pattern can match
) {
    internal companion object {
        val EMPTY = CharacterMatcher(MatcherKind.EMPTY)
        val ANY = CharacterMatcher(MatcherKind.ANY)
        val END_OF_LINE_OR_INPUT = CharacterMatcher(MatcherKind.END_OF_LINE_OR_INPUT)
    }

    val notEmpty: Boolean get() = when(kind) {
        MatcherKind.EMPTY -> false
        MatcherKind.ANY -> true
        MatcherKind.END_OF_LINE_OR_INPUT -> true
        MatcherKind.NEGATED -> true
        MatcherKind.LITERAL -> options.isEmpty() || options.all{ it.notEmpty }
        MatcherKind.ONE_OF -> true
        MatcherKind.RANGE -> true
        else -> true
    }

    // for range
    val min = literal

    // for negated
    val matcher: CharacterMatcher get() = options[0]

    //abstract fun matches(text: CharSequence, pos: Int): Boolean =

    override fun toString(): String = when (kind) {
        MatcherKind.EMPTY -> "EMPTY"
        MatcherKind.ANY -> "ANY"
        MatcherKind.END_OF_LINE_OR_INPUT -> "$"
        MatcherKind.NEGATED -> "!($matcher)"
        MatcherKind.LITERAL -> "'$literal'"
        MatcherKind.ONE_OF -> "{${options.joinToString(",")}}"
        MatcherKind.RANGE -> "[$min-$max]"
        MatcherKind.PREDICATE -> name
        MatcherKind.ALL_OF -> "{${options.joinToString("&&")}}"
        else -> "${kind.name}"
    }
}
/*
object CharacterAny : CharacterMatcher() {
    override fun matches(text: CharSequence, pos: Int): Boolean = true
    override fun toString(): String = "ANY"
}

object CharacterEndOfLineOrInput : CharacterMatcher() {
    override fun matches(text: CharSequence, pos: Int): Boolean = pos == text.length - 1 || text[pos] == '\n'
    override fun toString(): String = "$"
}

class CharacterNegated(val matcher: CharacterMatcher) : CharacterMatcher() {
    override fun matches(text: CharSequence, pos: Int): Boolean = this.matcher.matches(text, pos).not()
    override fun toString(): String = "!($matcher)"
}

class CharacterSingle(val value: Char) : CharacterMatcher() {
    override fun matches(text: CharSequence, pos: Int): Boolean = text[pos] == value
    override fun toString(): String = "'$value'"
}

class CharacterOneOf(val options: List<CharacterMatcher>) : CharacterMatcher() {
    constructor(value: String) : this(value.map { CharacterSingle(it) })

    override fun matches(text: CharSequence, pos: Int): Boolean = this.options.any { it.matches(text, pos) }
    override fun toString(): String = "{${options.joinToString("|")}}"
}

class CharacterRange(
        val min: Char, val max: Char
) : CharacterMatcher() {
    override fun matches(text: CharSequence, pos: Int): Boolean = text[pos] in min..max
    override fun toString(): String = "[$min-$max]"
}
*/

internal fun addNextStates(nextStates: MutableList<State>, next: State?): Boolean = addNextStates(nextStates, next, mutableSetOf())

/**
 * adds the non-split states reachable from 'next' by empty transitions, in priority order, each only once.
 * returns true if the goal is one of them.
 * (all branches are followed, and 'visited' stops loops over things that can match empty, e.g. (a?)* )
 */
internal fun addNextStates(nextStates: MutableList<State>, next: State?, visited: MutableSet<State>): Boolean {
    return if (null == next || visited.contains(next)) {
        false
    } else {
        visited.add(next)
        if (next.isSplit) {
            var toGoal = false
            next.outgoing.forEach { if (addNextStates(nextStates, it.to, visited)) toGoal = true }
            toGoal
        } else {
            nextStates.add(next)
            next == RegexMatcherImpl.MATCH_STATE
        }
    }
}

internal enum class TransitionKind {
    MATCHER, EMPTY, END_OF_LINE_OR_INPUT,
    ASSERT // zero width, the matcher is one of the ASSERT_ kinds
}

internal class Transition(val kind: TransitionKind, val matcher: CharacterMatcher) {

    var to: State? = null
    lateinit var nextStates: Array<State>

    // will get reassigned when nextStates is called
    var isToGoal = false

    fun init() {
        val ns = mutableListOf<State>()
        this.isToGoal = addNextStates(ns, this.to)
        this.nextStates = ns.toTypedArray()
    }

    //fun match(text: CharSequence, pos: Int): Boolean = value.matches(text, pos)
    override fun toString(): String = "-$matcher->(${to?.number})"
}