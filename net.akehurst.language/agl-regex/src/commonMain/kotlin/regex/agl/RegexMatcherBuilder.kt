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

internal class RegexMatcherBuilder(val pattern: String) {

    // List[StateNumber] -> Map<Unicode-Int, List<StateNumber>>
    val nfa = mutableListOf<State>()
    var nextStateNumber = 0
    var stack = mutableStackOf<Fragment>()
    var startState: State = RegexMatcherImpl.ERROR_STATE

    // return new state
    private fun createState(isSplit: Boolean): State {
        val state = State(this.nextStateNumber, isSplit)
        this.nextStateNumber++
        this.nfa.add(state)
        return state
    }

    private fun cloneState(orig: State?, nullTransitions: MutableList<Transition>, map: MutableMap<State, State>): State? {
        return if (null == orig) {
            null
        } else {
            val clone = map[orig]
            if (null == clone) {
                val c = this.createState(orig.isSplit)
                map[orig] = c
                val ctrans = orig.outgoing.map { this.cloneTransition(it, nullTransitions, map) }
                c.outgoing.addAll(ctrans)
                orig.loopExit?.let {
                    val idx = orig.outgoing.indexOf(it)
                    c.loopExit = if (idx >= 0) c.outgoing[idx] else this.cloneTransition(it, nullTransitions, map)
                }
                orig.iterationStartOf?.let { c.iterationStartOf = this.cloneState(it, nullTransitions, map) }
                orig.optionalSkip?.let { c.optionalSkip = c.outgoing[orig.outgoing.indexOf(it)] }
                c
            } else {
                clone
            }
        }
    }

    fun cloneTransition(orig: Transition, nullTransitions: MutableList<Transition>, map: MutableMap<State, State>): Transition {
        val clone = Transition(orig.kind, orig.matcher)
        clone.to = this.cloneState(orig.to, nullTransitions, map)
        if (null == clone.to) {
            nullTransitions.add(clone)
        }
        return clone
    }

    fun clone(orig: Fragment): Fragment {
        val coutgoing = mutableListOf<Transition>()
        val cstart = this.cloneState(orig.start, coutgoing, mutableMapOf())!!
        return Fragment(cstart, coutgoing)
    }

    fun start() {
        val state = this.createState(true)
        state.outgoing.add(Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY))
        this.stack.push(Fragment(state, state.outgoing))
        this.startState = state
    }

    fun matchAny() {
        val state = this.createState(false)
        val trans = Transition(TransitionKind.MATCHER, CharacterMatcher.ANY)
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun matchEndOfLineOrInput() {
        val state = this.createState(false)
        val trans = Transition(TransitionKind.MATCHER, CharacterMatcher.END_OF_LINE_OR_INPUT)
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun character(value: Char) {
        val state = this.createState(false)
        val trans = Transition(TransitionKind.MATCHER, CharacterMatcher(MatcherKind.LITERAL, value))
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun characterClass(matcher: CharacterMatcher) {
        val state = this.createState(false)
        val trans = Transition(TransitionKind.MATCHER, matcher)
        state.outgoing.add(trans)
        val frag = Fragment(state, state.outgoing)
        this.stack.push(frag)
    }

    fun concatenate() {
        when (this.stack.size) {
            0 -> {
            }

            1 -> {
                //this.start = this.stack.peek().start
            }

            else -> {
                val f2 = this.stack.pop()
                val f1 = this.stack.pop()
                f1.outgoing.forEach { it.to = f2.start }
                val frag = Fragment(f1.start, f2.outgoing)
                this.stack.push(frag)
                //this.start = f1.start
            }
        }
    }

    fun choice() {
        val f2 = this.stack.pop()
        val f1 = this.stack.pop()
        val split = this.createState(true)
        val t1 = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        t1.to = f1.start
        val t2 = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        t2.to = f2.start
        split.outgoing.add(t1)
        split.outgoing.add(t2)
        val frag = Fragment(split, f1.outgoing + f2.outgoing)
        this.stack.push(frag)
        //this.start = split
    }

    fun multi01(lazy: Boolean = false) {
        val f1 = this.stack.pop()
        this.stack.push(this.optionalFragment(f1, lazy))
    }

    fun multi1n(lazy: Boolean = false) {
        val f1 = this.stack.pop()
        this.stack.push(this.oneOrMoreFragment(f1, lazy))
    }

    fun multi0n(lazy: Boolean = false) {
        val f1 = this.stack.pop()
        this.stack.push(this.zeroOrMoreFragment(f1, lazy))
    }

    /**
     * {n}, {n,} (m == -1) or {n,m}.
     * {n,m} is built as n copies followed by nested optionals, x{1,3} = x(x(x)?)?, so that the priority of the choices
     * is the same as in Kotlin/Java: as many as possible first (greedy) or as few as possible first (lazy).
     * As in Kotlin/Java, a repetition that matches nothing ends the whole repetition, even if fewer than n were matched,
     * e.g. (|b){2} matches "" in "b".
     */
    fun repetition(n: Int, m: Int, lazy: Boolean = false) {
        val repFrag = this.stack.pop()
        val endState = this.createState(true)
        val endTransition = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        endState.outgoing.add(endTransition)
        fun iteration(): Fragment {
            val body = this.clone(repFrag)
            val iterationStart = this.iterationStart(body)
            val checkpoint = this.createState(true)
            val next = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
            checkpoint.outgoing.add(next)
            val exit = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
            exit.to = endState
            checkpoint.loopExit = exit
            iterationStart.iterationStartOf = checkpoint
            body.outgoing.forEach { it.to = checkpoint }
            return Fragment(iterationStart, listOf(next))
        }

        var result: Fragment? = null
        for (r in 0 until n) {
            val c = iteration()
            result = if (null == result) c else this.concatenateFragments(result, c)
        }
        val rest = when {
            -1 == m -> this.zeroOrMoreFragment(this.clone(repFrag), lazy)
            m > n -> {
                var opt: Fragment? = null
                for (r in n until m) {
                    val c = iteration()
                    val body = if (null == opt) c else this.concatenateFragments(c, opt)
                    opt = this.optionalFragment(body, lazy)
                }
                opt
            }

            else -> null
        }
        result = when {
            null == result -> rest
            null == rest -> result
            else -> this.concatenateFragments(result, rest)
        } ?: this.emptyFragment()
        result.outgoing.forEach { it.to = endState }
        this.stack.push(Fragment(result.start, listOf(endTransition)))
    }

    /** matches the empty string */
    fun empty() {
        this.stack.push(this.emptyFragment())
    }

    /** a zero width assertion, e.g. ^, $, \b */
    fun assertion(matcher: CharacterMatcher) {
        val state = this.createState(false)
        val trans = Transition(TransitionKind.ASSERT, matcher)
        state.outgoing.add(trans)
        this.stack.push(Fragment(state, state.outgoing))
    }

    /** the fragment on the stack becomes the pattern looked at by a lookahead or lookbehind assertion */
    fun lookaround(kind: MatcherKind) {
        val sub = this.stack.pop()
        sub.outgoing.forEach { it.to = RegexMatcherImpl.MATCH_STATE }
        val maxLength = when (kind) {
            MatcherKind.ASSERT_LOOKBEHIND, MatcherKind.ASSERT_NEGATIVE_LOOKBEHIND -> maxLength(sub.start)
            else -> 0
        }
        this.assertion(CharacterMatcher(kind, subStart = sub.start, maxLength = maxLength))
    }

    private fun emptyFragment(): Fragment {
        val state = this.createState(true)
        state.outgoing.add(Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY))
        return Fragment(state, state.outgoing)
    }

    private fun concatenateFragments(f1: Fragment, f2: Fragment): Fragment {
        f1.outgoing.forEach { it.to = f2.start }
        return Fragment(f1.start, f2.outgoing)
    }

    // the order of the transitions out of a split state gives the priority, first is preferred
    private fun split(first: Transition, second: Transition): State {
        val split = this.createState(true)
        split.outgoing.add(first)
        split.outgoing.add(second)
        return split
    }

    private fun optionalFragment(f1: Fragment, lazy: Boolean): Fragment {
        val take = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        take.to = f1.start
        val skip = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        val split = if (lazy) this.split(skip, take) else this.split(take, skip)
        split.optionalSkip = skip
        return Fragment(split, f1.outgoing + skip)
    }

    private fun oneOrMoreFragment(f1: Fragment, lazy: Boolean): Fragment {
        val iterationStart = this.iterationStart(f1)
        val again = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        again.to = iterationStart
        val exit = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        val split = this.loopSplit(again, exit, lazy, iterationStart)
        f1.outgoing.forEach { it.to = split }
        return Fragment(iterationStart, listOf(exit))
    }

    private fun zeroOrMoreFragment(f1: Fragment, lazy: Boolean): Fragment {
        val iterationStart = this.iterationStart(f1)
        val again = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        again.to = iterationStart
        val exit = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        val split = this.loopSplit(again, exit, lazy, iterationStart)
        f1.outgoing.forEach { it.to = split }
        return Fragment(split, listOf(exit))
    }

    // each iteration of a loop passes this state, so that an iteration that matches nothing can be recognised:
    // as in Kotlin/Java, after such an iteration the loop is left
    private fun iterationStart(body: Fragment): State {
        val state = this.createState(true)
        val t = Transition(TransitionKind.EMPTY, CharacterMatcher.EMPTY)
        t.to = body.start
        state.outgoing.add(t)
        return state
    }

    private fun loopSplit(again: Transition, exit: Transition, lazy: Boolean, iterationStart: State): State {
        val split = if (lazy) this.split(exit, again) else this.split(again, exit)
        split.loopExit = exit
        iterationStart.iterationStartOf = split
        return split
    }

    /** the maximum number of characters matched from 'start' to the goal, -1 if there is no maximum (a loop) */
    private fun maxLength(start: State): Int {
        val done = mutableMapOf<State, Int>()
        val onPath = mutableSetOf<State>()
        fun longest(s: State?): Int {
            if (null == s || s.isGoal) return 0
            done[s]?.let { return it }
            if (onPath.contains(s)) return -1
            onPath.add(s)
            var max = 0
            for (t in s.outgoing) {
                val l = longest(t.to)
                if (-1 == l) {
                    max = -1
                    break
                }
                val step = if (t.kind == TransitionKind.MATCHER) 1 else 0
                max = maxOf(max, l + step)
            }
            onPath.remove(s)
            done[s] = max
            return max
        }
        return longest(start)
    }

    fun startGroup() {
    }

    fun finishGroup() {
    }

    fun build(): RegexMatcherImpl {
        return RegexMatcherImpl(pattern, this.startState, this.nfa)
    }

    fun concatenateGoal() {
        if (this.stack.isEmpty) {
            //don't add goal
        } else {
            val f1 = this.stack.pop()
            f1.outgoing.forEach { it.to = RegexMatcherImpl.MATCH_STATE }
            this.startState = f1.start
        }
    }
}
