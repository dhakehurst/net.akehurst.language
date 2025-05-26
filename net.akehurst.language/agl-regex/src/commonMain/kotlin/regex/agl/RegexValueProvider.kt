/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.regex.api.RegexMatcher
import kotlin.collections.removeAll

class RegexValueProvider(
    pattern: String,
    val anyChar: Char
) {

    companion object {
        val PRINTABLE_ASCII =
                    ('A'..'Z') +
                    ('a'..'z') +
                    ('0'..'9') +
                    listOf('!','"', '#','$','%','&','\'','(',')','*','+',',','-','.','/') +
                    listOf(':',';','<','=','>','?','@') +
                    listOf('[','\\',']','^','_','`') +
                    listOf('{','|','}','~') +
                    listOf(' ')
            .toMutableList()
    }

    private val _matcher: RegexMatcherImpl = regexMatcher(pattern) as RegexMatcherImpl
    val matcher: RegexMatcher = _matcher

    private val start: State = _matcher.start
    private val nfa = _matcher.nfa
    private val chars = mutableListOf<Char>()

    private val startStates: Array<State>

    init {
        val ns = mutableListOf<State>()
        addNextStates(ns, this.start)
        this.startStates = ns.toTypedArray()
        nfa.forEach {
            it.outgoing.forEach {
                it.init()
            }
        }
    }

    fun provide(): String {
        var currentStates = arrayOf<State>()
        currentStates = this.startStates
        var lastState: State? = null
        //TODO: find shorted path to goal - better algorithm required
        while (currentStates.isNotEmpty()) {
            // pick state by
            // - is Goal
            // - has a transition to goal
            // - not the last state
            // - first one
            lastState = currentStates.firstOrNull { it.isGoal}
                ?: currentStates.firstOrNull { it.outgoing.any { it.isToGoal } }
                ?: currentStates.firstOrNull { it != lastState }
                ?: currentStates.first()
            // pick the transition by
            // - goes to goal
            // - first one
            val outgoing = lastState.outgoing.firstOrNull { it.isToGoal }
                ?: lastState.outgoing.firstOrNull()
            if (null == outgoing) {
                currentStates = arrayOf()
            } else {
                val c = provideFor(outgoing.matcher)
                if (c != null) {
                    chars.add(c)
                }
                currentStates = if (outgoing.isToGoal) {
                    emptyArray()
                } else {
                    outgoing.nextStates
                }
            }
        }
        return chars.joinToString("")
    }

    internal fun provideFor(matcher: CharacterMatcher): Char? {
        return when (matcher.kind) {
            MatcherKind.EMPTY -> null
            MatcherKind.ANY -> anyChar
            MatcherKind.END_OF_LINE_OR_INPUT -> '\n'
            MatcherKind.NEGATED -> notOneOf(matcher.options)
            MatcherKind.LITERAL -> when {
                matcher.notEmpty -> matcher.literal
                else -> null
            }
            MatcherKind.ONE_OF -> provideFor(matcher.options[0])
            MatcherKind.RANGE -> matcher.min
        }
    }

    private fun notOneOf(matchers: Array<CharacterMatcher>): Char {
        val chars = PRINTABLE_ASCII.toMutableList()
        matchers.forEach {
            when (it.kind) {
                MatcherKind.EMPTY -> error("should not happen")
                MatcherKind.ANY -> anyChar
                MatcherKind.END_OF_LINE_OR_INPUT -> '\n'
                MatcherKind.NEGATED -> error("should not happen")
                MatcherKind.LITERAL -> chars.remove(it.literal)
                MatcherKind.ONE_OF -> it.options.forEach { chars.removeAll(allOf(it)) }
                MatcherKind.RANGE -> chars.removeAll(it.min .. it.max)
            }
        }
        return chars.first()
    }

    private fun allOf(cm: CharacterMatcher): List<Char> {
        return when (cm.kind) {
            MatcherKind.EMPTY -> emptyList()
            MatcherKind.ANY -> listOf(anyChar)
            MatcherKind.END_OF_LINE_OR_INPUT -> listOf('\n')
            MatcherKind.NEGATED -> error("should not happen")
            MatcherKind.LITERAL -> listOf(cm.literal)
            MatcherKind.ONE_OF -> cm.options.flatMap { allOf(it) }
            MatcherKind.RANGE -> (cm.min .. cm.max).toList()
        }
    }
}