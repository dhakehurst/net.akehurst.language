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

fun regexMatcher(pattern:String) = RegexParser(pattern).parse()

// nfa is Array of States, each state is element in array
// Array<Int,
class RegexMatcher(
        val start:State,
        val nfa: List<State>
) {

    data class MatchResult(
            val matchedText: String,
            val eolPositions: List<Int>
    )

    companion object {
        val MATCH_STATE = State(-1, false)
        val ERROR_STATE = State(-2, false)
    }

    val startStates:Array<State>

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

    fun matches( matcher:CharacterMatcher, text:CharSequence, pos:Int): Boolean {
        return when(matcher.kind) {
            MatcherKind.EMPTY -> error("should not happen")
            MatcherKind.ANY -> true
            MatcherKind.END_OF_LINE_OR_INPUT -> {
                val nextPos = pos+1
                nextPos == text.length || text[pos] == '\n'
            }
            MatcherKind.NEGATED -> this.matches(matcher.matcher, text, pos).not()
            MatcherKind.LITERAL -> text[pos] == matcher.literal
            MatcherKind.ONE_OF -> {
                val opts = matcher.options
                for(i in 0 until opts.size) {
                    if (this.matches(opts[i], text, pos)) {
                        return true
                    }
                }
                return false
            }
            MatcherKind.RANGE -> text[pos] in matcher.min .. matcher.max
        }
    }

    // these should really be local to match, allocated here to speed up the match function
    private var currentStates = ArrayList<Array<State>>(10)
    private var nextStates = ArrayList<Array<State>>(10)
    private var eolPositions = ArrayList<Int>(10)
    fun match(text: CharSequence, startPosition: Int = 0): MatchResult? {
        var pos = startPosition
        this.currentStates.clear()
        this.nextStates.clear()
        currentStates.add(this.startStates)
        var maxMatchedPos = -1
        while (currentStates.isNotEmpty() && pos < text.length) {
            if (text[pos]=='\n') this.eolPositions.add(pos)
            for (ss in 0 until currentStates.size) {
                val states = currentStates[ss]
                for(s in 0 until states.size) {
                    val state = states[s]
                    val outgoing = state.outgoing
                    for (t in 0 until outgoing.size) { //TODO: use array and counter here
                        val trans = outgoing[t]
                        if (this.matches(trans.matcher, text, pos)) {
                            //must call trans.nextStates before trans.isToGoal
                            nextStates.add(trans.nextStates)
                            if (trans.isToGoal) {
                                maxMatchedPos = maxOf(maxMatchedPos, pos)
                            }
                        }
                    }
                }
            }
            val t = currentStates
            currentStates = nextStates
            nextStates = t
            nextStates.clear()
            pos++
        }
        return if (maxMatchedPos!=-1) {
            val matchedText = text.substring(startPosition, maxMatchedPos+1)
            MatchResult(matchedText, eolPositions)
        } else {
            null
        }
    }



}