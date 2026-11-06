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
import kotlin.random.Random

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

    private val startStates: Array<State>

    // the fewest chars needed to reach the goal from each state (Int.MAX_VALUE if the goal cannot be reached)
    private val distance = mutableMapOf<State, Int>()

    init {
        val ns = mutableListOf<State>()
        addNextStates(ns, this.start)
        this.startStates = ns.toTypedArray()
        nfa.forEach {
            it.outgoing.forEach {
                it.init()
            }
        }
        this.computeDistances()
    }

    private fun distanceOf(s: State?): Int = when {
        null == s -> Int.MAX_VALUE
        s.isGoal -> 0
        else -> distance[s] ?: Int.MAX_VALUE
    }

    private fun computeDistances() {
        var changed = true
        while (changed) {
            changed = false
            for (s in nfa) {
                var d = distanceOf(s)
                for (t in s.outgoing) {
                    val dt = distanceOf(t.to)
                    if (dt != Int.MAX_VALUE) {
                        val w = if (t.kind == TransitionKind.MATCHER) 1 else 0
                        if (dt + w < d) d = dt + w
                    }
                }
                if (d < distanceOf(s)) {
                    distance[s] = d
                    changed = true
                }
            }
        }
    }

    /**
     * A string that matches the pattern, always the same one: the first alternative of each choice,
     * the fewest repetitions, and the first char of each character class.
     */
    fun provide(): String {
        val first = this.walk(null, Int.MAX_VALUE)
        return when {
            _matcher.matchesEntirely(first) -> first
            // assertions (e.g. lookahead) can make the simple choice fail, try some others
            else -> (0 until 50).map { this.walk(Random(it), 20) }.firstOrNull { _matcher.matchesEntirely(it) } ?: first
        }
    }

    /**
     * A random string that matches the pattern. Repetitions stop being added once the string has about
     * 'maxLength' chars. If no matching string is found after some attempts, the result of provide() is given.
     */
    fun provideRandom(random: Random = Random.Default, maxLength: Int = 20): String {
        for (i in 0 until 50) {
            val s = this.walk(random, maxLength)
            if (_matcher.matchesEntirely(s)) return s
        }
        return this.provide()
    }

    // a walk through the NFA from the start to the goal. With no 'random', the first or shortest choices are taken
    private fun walk(random: Random?, maxLength: Int): String {
        val chars = StringBuilder()
        var state: State? = this.start
        var steps = 0
        while (null != state && state.isGoal.not() && steps < 100_000) {
            steps++
            val finishing = null == random || chars.length >= maxLength
            val outgoing = state.outgoing.filter { distanceOf(it.to) != Int.MAX_VALUE }
            if (outgoing.isEmpty()) break
            val t = when {
                state.isSplit -> when {
                    finishing -> this.shortestChoice(state, outgoing, null == random)
                    else -> outgoing[random!!.nextInt(outgoing.size)]
                }

                else -> outgoing[0]
            }
            if (t.kind == TransitionKind.MATCHER) {
                val c = if (finishing) this.firstCharFor(t.matcher) else this.randomCharFor(t.matcher, random!!)
                if (null != c) chars.append(c)
            }
            state = t.to
        }
        return chars.toString()
    }

    // provide() takes the first alternative of a choice, and leaves loops and optionals as soon as possible.
    // Finishing a random walk takes a shortest way to the goal
    private fun shortestChoice(state: State, outgoing: List<Transition>, firstAlternative: Boolean): Transition {
        val leave = state.loopExit ?: state.optionalSkip
        return when {
            null != leave && outgoing.contains(leave) -> leave
            firstAlternative -> outgoing[0]
            else -> outgoing.minBy { distanceOf(it.to) }
        }
    }

    private fun firstCharFor(matcher: CharacterMatcher): Char? {
        if (matcher.name == "." && matchesChar(matcher, anyChar)) return anyChar
        val c = this.provideFor(matcher)
        return when {
            null == c -> null
            matchesChar(matcher, c) -> c
            else -> PRINTABLE_ASCII.firstOrNull { matchesChar(matcher, it) } ?: c
        }
    }

    private fun randomCharFor(matcher: CharacterMatcher, random: Random): Char? {
        val candidates = (PRINTABLE_ASCII + charsIn(matcher)).distinct().filter { matchesChar(matcher, it) }
        return when {
            candidates.isEmpty() -> this.firstCharFor(matcher)
            else -> candidates[random.nextInt(candidates.size)]
        }
    }

    // chars named in the matcher, so that e.g. [\u00e9] or \n can be chosen
    private fun charsIn(matcher: CharacterMatcher): List<Char> = when (matcher.kind) {
        MatcherKind.LITERAL -> listOf(matcher.literal)
        MatcherKind.RANGE -> listOf(matcher.min, matcher.max)
        MatcherKind.ONE_OF, MatcherKind.NEGATED, MatcherKind.ALL_OF -> matcher.options.flatMap { charsIn(it) }
        MatcherKind.ANY -> listOf(anyChar)
        else -> emptyList()
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
            MatcherKind.PREDICATE, MatcherKind.ALL_OF -> PRINTABLE_ASCII.firstOrNull { matchesChar(matcher, it) } ?: anyChar
            else -> null // zero width assertions do not provide a char
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
                else -> chars.removeAll { c -> matchesChar(it, c) }
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
            else -> PRINTABLE_ASCII.filter { matchesChar(cm, it) }
        }
    }

    private fun matchesChar(matcher: CharacterMatcher, c: Char): Boolean = _matcher.matches(matcher, c.toString(), 0)
}