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

    val startStates:MutableList<State> by lazy {
        val ns = mutableListOf<State>()
        addNextStates(ns, this.start)
        ns
    }

    fun matches( matcher:CharacterMatcher, text:CharSequence, pos:Int): Boolean {
        return when(matcher.kind) {
            MatcherKind.EMPTY -> error("should not happen")
            MatcherKind.ANY -> true
            MatcherKind.END_OF_LINE_OR_INPUT -> TODO()
            MatcherKind.NEGATED -> this.matches(matcher.matcher, text, pos).not()
            MatcherKind.LITERAL -> text[pos] == matcher.literal
            MatcherKind.ONE_OF -> {
                for(m in matcher.options) {
                    if (this.matches(m, text, pos)) {
                        return true
                    }
                }
                return false
            }
            MatcherKind.RANGE -> text[pos] in matcher.min .. matcher.max
        }
    }

    fun match(text: CharSequence, startPosition: Int = 0): MatchResult? {
        var pos = startPosition
        var currentStates = mutableListOf<State>()
        currentStates.addAll(this.startStates)
        var nextStates = mutableListOf<State>()
        var maxMatchedPos = -1
        var eolPositions = mutableListOf<Int>()
        while (currentStates.isNotEmpty() && pos < text.length) {
            if (text[pos]=='\n') eolPositions.add(pos)
            for (state in currentStates) {
                for( trans in state.outgoing) {
                    if (this.matches(trans.matcher,text, pos)) {
                        //must call trans.nextStates before trans.isToGoal
                        nextStates.addAll(trans.nextStates)
                        if(trans.isToGoal) {
                            maxMatchedPos = maxOf(maxMatchedPos, pos)
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