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

sealed class CharacterMatcher {
    abstract fun matches(input: Char): Boolean
}

class CharacterNegated(val matcher:CharacterMatcher):CharacterMatcher() {
    override fun matches(input: Char): Boolean = this.matcher.matches(input).not()
    override fun toString(): String = "!($matcher)"
}

class CharacterSingle(val value:Char) : CharacterMatcher() {
    override fun matches(input: Char): Boolean = input == value
    override fun toString(): String = "$value"
}
class CharacterOneOf(val options:List<CharacterMatcher>) : CharacterMatcher() {
    constructor(value:String) : this(value.map { CharacterSingle(it) })
    override fun matches(input: Char): Boolean = this.options.any { it.matches(input) }
    override fun toString(): String = "${options.joinToString("|")}"
}

class CharacterRange(
        val min: Char, val max: Char
) : CharacterMatcher() {
    override fun matches(input: Char): Boolean = input in min..max
    override fun toString(): String = "[$min-$max]"
}

internal fun addNextStates(nextStates:MutableList<State>, next:State?) :Boolean {
    //TODO: don't add duplicate states
    return if (null==next) {
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

sealed class Transition {
    var to: State? = null

    // will get reassigned when nextStates is called
    var isToGoal = false

    val nextStates:List<State> by lazy {
        val ns = mutableListOf<State>()
        this.isToGoal = addNextStates(ns, this.to)
        ns
    }

    abstract fun match(input: Char): Boolean
}

class TransitionLiteral(val value: Char) : Transition() {
    override fun match(input: Char): Boolean = input == value
    override fun toString(): String = "-/$value/->(${to?.number})"
}

class TransitionMatcher(val value: CharacterMatcher) : Transition() {
    override fun match(input: Char): Boolean = value.matches(input)
    override fun toString(): String = "-/$value/->(${to?.number})"
}

class TransitionAny() : Transition() {
    override fun match(input: Char): Boolean = true //TODO: maybe exclude \n
    override fun toString(): String = "-any->(${to?.number})"
}

class TransitionEmpty : Transition() {
    override fun match(input: Char): Boolean = true
    override fun toString(): String = "-empty->(${to?.number})"
}