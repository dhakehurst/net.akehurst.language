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

import net.akehurst.language.regex.api.MatchResult
import net.akehurst.language.regex.api.RegexMatcher

fun regexMatcher(pattern: String): RegexMatcher = RegexParser().parse(pattern)

// nfa is Array of States, each state is element in array
// Array<Int,
internal class RegexMatcherImpl(
    val pattern: String,
    val start: State,
    val nfa: List<State>
) : RegexMatcher {

    internal companion object {
        val MATCH_STATE = State(-1, false)
        val ERROR_STATE = State(-2, false)

        // as in Kotlin/Java (without UNIX_LINES)
        fun isLineTerminator(ch: Char) = when (ch) {
            '\n', '\r', '\u0085', '\u2028', '\u2029' -> true
            else -> false
        }

        // as \w, used by \b (as in Kotlin/Java on JVM 19 onwards)
        fun isWordChar(ch: Char) = ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '_'
    }

    override fun matchAt(text: String, atPosition: Int): MatchResult? = this.match(text, atPosition)

    override fun matchesAt(text: String, atPosition: Int): Boolean = null != this.match(text, atPosition)

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

    internal fun matches(matcher: CharacterMatcher, text: CharSequence, pos: Int): Boolean {
        return when (matcher.kind) {
            MatcherKind.EMPTY -> error("should not happen")
            MatcherKind.ANY -> true
            MatcherKind.END_OF_LINE_OR_INPUT -> {
                val nextPos = pos + 1
                nextPos == text.length || text[pos] == '\n'
            }

            MatcherKind.NEGATED -> this.matches(matcher.matcher, text, pos).not()
            MatcherKind.LITERAL -> text[pos] == matcher.literal
            MatcherKind.ONE_OF -> {
                val opts = matcher.options
                for (i in 0 until opts.size) {
                    if (this.matches(opts[i], text, pos)) {
                        return true
                    }
                }
                return false
            }

            MatcherKind.RANGE -> text[pos] in matcher.min..matcher.max
            MatcherKind.PREDICATE -> matcher.predicate!!.invoke(text[pos])
            MatcherKind.ALL_OF -> {
                val opts = matcher.options
                for (i in 0 until opts.size) {
                    if (this.matches(opts[i], text, pos).not()) {
                        return false
                    }
                }
                return true
            }

            else -> error("Assertion '${matcher.kind}' does not match a character")
        }
    }

    override fun match(text: CharSequence, startPosition: Int): RegexMatcher.MatchResultAgl? {
        if (startPosition < 0 || startPosition > text.length) throw IndexOutOfBoundsException("startPosition $startPosition is out of bounds of the text of length ${text.length}")
        val end = Simulation(text, startPosition).longestFirst(this.start, startPosition)
        return when (end) {
            -1 -> null
            else -> RegexMatcher.MatchResultAgl(text.substring(startPosition, end))
        }
    }

    // the loops whose iteration started without a char being matched since
    private class LoopsStarted(val loop: State, val next: LoopsStarted?) {
        fun contains(s: State): Boolean = this.loop == s || (this.next?.contains(s) ?: false)
        fun without(s: State): LoopsStarted? = when {
            this.loop == s -> this.next
            else -> LoopsStarted(this.loop, this.next?.without(s))
        }

        fun toSet(): Set<Int> = generateSequence(this) { it.next }.map { it.loop.number }.toSet()
    }

    /** true if all of 'text' matches the pattern (as Kotlin's Regex.matches) */
    fun matchesEntirely(text: CharSequence): Boolean = Simulation(text, 0).matchesExactly(this.start, 0, text.length)

    /**
     * Runs the NFA on 'text', following all choices in parallel (one 'thread' per state), so the time taken is
     * linear in the length of the text times the number of states.
     * The threads are kept in priority order (first alternative, greedy or lazy repetition), and the match found is the one
     * a backtracking matcher (Kotlin/Java) finds first: when a thread reaches the goal, the lower priority threads are dropped.
     */
    private inner class Simulation(
        val text: CharSequence,
        val matchStart: Int // for \G
    ) {
        private val size = nfa.size + 1 // last index is for MATCH_STATE
        private val mark = IntArray(size)
        private var stamp = 0

        private fun index(s: State): Int = if (s.number < 0) size - 1 else s.number

        /** the end position of the first match (by priority) starting at 'from', or -1 */
        fun longestFirst(startState: State, from: Int): Int {
            if (startState == ERROR_STATE) return -1
            var current = ArrayList<State>()
            stamp++
            addThreads(current, startState, from)
            var matchEnd = -1
            var pos = from
            while (true) {
                val next = ArrayList<State>()
                stamp++
                for (s in current) {
                    if (s.isGoal) {
                        matchEnd = pos
                        break // lower priority threads are not needed
                    }
                    if (pos < text.length) {
                        val t = s.outgoing[0]
                        if (matches(t.matcher, text, pos)) addThreads(next, t.to, pos + 1)
                    }
                }
                if (next.isEmpty()) break
                current = next
                pos++
            }
            return matchEnd
        }

        /** true if some way through the NFA from 'startState', starting at 'from', ends exactly at 'target' */
        fun matchesExactly(startState: State, from: Int, target: Int): Boolean {
            if (startState == ERROR_STATE) return false
            var current = ArrayList<State>()
            stamp++
            addThreads(current, startState, from)
            var pos = from
            while (true) {
                if (pos == target) return current.any { it.isGoal }
                val next = ArrayList<State>()
                stamp++
                for (s in current) {
                    if (s.isGoal.not()) {
                        val t = s.outgoing[0]
                        if (matches(t.matcher, text, pos)) addThreads(next, t.to, pos + 1)
                    }
                }
                if (next.isEmpty()) return false
                current = next
                pos++
            }
        }

        // states already reached in the current position (stamp), for states reached in a loop iteration that started at the current position
        private val reachedInIteration = mutableSetOf<Pair<Int, Set<Int>>>()
        private var reachedStamp = -1

        // adds the states reachable from 'from' by empty transitions and assertions that hold at 'pos', in priority order, each once per position
        private fun addThreads(list: MutableList<State>, from: State?, pos: Int) {
            if (reachedStamp != stamp) {
                reachedInIteration.clear()
                reachedStamp = stamp
            }
            val todo = ArrayList<Pair<State?, LoopsStarted?>>()
            todo.add(Pair(from, null))
            while (todo.isNotEmpty()) {
                val (s, loops) = todo.removeAt(todo.size - 1)
                if (null == s) continue
                val loopExit = s.loopExit
                if (null != loopExit && null != loops && loops.contains(s)) {
                    // the end of an iteration that matched nothing, the loop is left (as Kotlin/Java do)
                    todo.add(Pair(loopExit.to, loops.without(s)))
                    continue
                }
                val i = index(s)
                if (null == loops || s.isSplit.not() && s.isAssertion.not()) {
                    if (mark[i] == stamp) continue
                    mark[i] = stamp
                } else {
                    // the same state in a different loop iteration can lead to a different result
                    if (reachedInIteration.add(Pair(i, loops.toSet())).not()) continue
                }
                when {
                    s.isSplit -> {
                        val ls = s.iterationStartOf?.let { LoopsStarted(it, loops) } ?: loops
                        for (t in s.outgoing.size - 1 downTo 0) todo.add(Pair(s.outgoing[t].to, ls))
                    }

                    s.isAssertion -> if (holds(s.outgoing[0].matcher, pos)) todo.add(Pair(s.outgoing[0].to, loops))
                    else -> list.add(s)
                }
            }
        }

        private fun holds(assertion: CharacterMatcher, pos: Int): Boolean {
            val len = text.length
            return when (assertion.kind) {
                MatcherKind.ASSERT_START_OF_INPUT -> 0 == pos
                MatcherKind.ASSERT_END_OF_INPUT -> len == pos
                MatcherKind.ASSERT_START_OF_MATCH -> matchStart == pos
                MatcherKind.ASSERT_START_OF_LINE -> when {
                    pos == len -> false
                    pos > 0 -> {
                        val ch = text[pos - 1]
                        isLineTerminator(ch) && (ch == '\r' && text[pos] == '\n').not()
                    }

                    else -> true
                }

                MatcherKind.ASSERT_END_OF_LINE -> when {
                    pos < len -> isLineTerminatorAt(pos)
                    else -> true
                }

                MatcherKind.ASSERT_END_OF_INPUT_OR_FINAL_TERMINATOR -> when {
                    pos < len - 2 -> false
                    pos == len - 2 -> text[pos] == '\r' && text[pos + 1] == '\n'
                    pos == len - 1 -> isLineTerminatorAt(pos)
                    else -> true
                }

                MatcherKind.ASSERT_WORD_BOUNDARY -> isWordBoundary(pos)
                MatcherKind.ASSERT_NOT_WORD_BOUNDARY -> isWordBoundary(pos).not()
                MatcherKind.ASSERT_LOOKAHEAD -> lookahead(assertion, pos)
                MatcherKind.ASSERT_NEGATIVE_LOOKAHEAD -> lookahead(assertion, pos).not()
                MatcherKind.ASSERT_LOOKBEHIND -> lookbehind(assertion, pos)
                MatcherKind.ASSERT_NEGATIVE_LOOKBEHIND -> lookbehind(assertion, pos).not()
                else -> error("'${assertion.kind}' is not an assertion")
            }
        }

        // a line terminator at pos, but not the \n of \r\n
        private fun isLineTerminatorAt(pos: Int): Boolean {
            val ch = text[pos]
            return when (ch) {
                '\n' -> (pos > 0 && text[pos - 1] == '\r').not()
                else -> isLineTerminator(ch)
            }
        }

        private fun isWordBoundary(pos: Int): Boolean {
            val left = pos > 0 && isWordChar(text[pos - 1])
            val right = pos < text.length && isWordChar(text[pos])
            return left != right
        }

        private fun lookahead(assertion: CharacterMatcher, pos: Int): Boolean =
            -1 != Simulation(text, matchStart).longestFirst(assertion.subStart!!, pos)

        private fun lookbehind(assertion: CharacterMatcher, pos: Int): Boolean {
            val earliest = if (-1 == assertion.maxLength) 0 else maxOf(0, pos - assertion.maxLength)
            val sim = Simulation(text, matchStart)
            for (from in pos downTo earliest) {
                if (sim.matchesExactly(assertion.subStart!!, from, pos)) return true
            }
            return false
        }
    }

    override fun toString(): String = "Regex{$pattern}"
}