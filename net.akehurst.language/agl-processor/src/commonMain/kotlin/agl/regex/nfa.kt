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

import kotlin.properties.Delegates

class State(
        val number: Int,
        val isSplit: Boolean
) {
    val outgoing = mutableListOf<Transition>()

    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is State -> this.number == other.number
            else -> false
        }
    }

    override fun toString(): String = "State{$number}"
}

class Fragment(
        val start: State,
        val outgoing: List<Transition>
) {
}

enum class MatcherKind {
  EMPTY, ANY, END_OF_LINE_OR_INPUT, NEGATED, LITERAL, ONE_OF, RANGE
}

class CharacterMatcher(
        val kind:MatcherKind,
        val literal:Char = '\u0000', // min
        val max:Char = '\u0000',
        val options: Array<CharacterMatcher> = arrayOf(CharacterMatcher.EMPTY)
) {
    companion object {
        val EMPTY = CharacterMatcher(MatcherKind.EMPTY)
        val ANY = CharacterMatcher(MatcherKind.ANY)
        val END_OF_LINE_OR_INPUT = CharacterMatcher(MatcherKind.END_OF_LINE_OR_INPUT)
    }

    // for range
    val min = literal

    // for negated
    val matcher: CharacterMatcher = options[0]

    //abstract fun matches(text: CharSequence, pos: Int): Boolean =

    override fun toString(): String = when(kind) {
        MatcherKind.EMPTY -> error("should not happen")
        MatcherKind.ANY -> "ANY"
        MatcherKind.END_OF_LINE_OR_INPUT -> "$"
        MatcherKind.NEGATED -> "!()"
        MatcherKind.LITERAL -> "''"
        MatcherKind.ONE_OF -> "{}"
        MatcherKind.RANGE -> "[]"
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
internal fun addNextStates(nextStates: MutableList<State>, next: State?): Boolean {
    //TODO: don't add duplicate states
    return if (null == next) {
        false
    } else {
        if (next.isSplit) {
            next.outgoing.any { addNextStates(nextStates, it.to) }
        } else {
            nextStates.add(next)
            next == RegexMatcher.MATCH_STATE
        }
    }
}

enum class TransitionKind {
    MATCHER, EMPTY, END_OF_LINE_OR_INPUT
}

class Transition(val kind: TransitionKind, val matcher: CharacterMatcher) {

    var to: State? = null

    // will get reassigned when nextStates is called
    var isToGoal = false

    val nextStates: List<State> by lazy {
        val ns = mutableListOf<State>()
        this.isToGoal = addNextStates(ns, this.to)
        ns
    }

    //fun match(text: CharSequence, pos: Int): Boolean = value.matches(text, pos)
    override fun toString(): String = "-$matcher->(${to?.number})"
}