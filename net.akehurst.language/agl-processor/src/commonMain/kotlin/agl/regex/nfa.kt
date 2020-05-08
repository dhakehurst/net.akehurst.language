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

class State(
        val number: Int,
        val isSplit:Boolean
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
)

interface Transition {
    var to: State?
    fun match(input: Char): Boolean
}

class TransitionLiteral(val value: Char) : Transition {
    override var to: State?=null
    override fun match(input: Char): Boolean = input == value
}

class TransitionRange( val min: Char, val max: Char) : Transition {
    override var to: State?=null
    override fun match(input: Char): Boolean = input >= min && input <= max
}

class TransitionAny() : Transition {
    override var to: State?=null
    override fun match(input: Char): Boolean = true //TODO: maybe exclude \n
}

class TransitionEmpty : Transition {
    override var to: State?=null
    override fun match(input: Char): Boolean = true
}