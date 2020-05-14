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

    fun match(text: CharSequence, startPosition: Int = 0): MatchResult? {
        var pos = startPosition
        var currentStates = mutableListOf<State>()
        currentStates.addAll(this.startStates)
        var nextStates = mutableListOf<State>()
        var reachedGoal = false
        var maxMatchedPos = -1
        var eolPositions = mutableListOf<Int>()
        while (currentStates.isNotEmpty() && pos < text.length) {
            var c = text[pos++]
            if (c=='\n') eolPositions.add(pos)
            for (state in currentStates) {
                for( trans in state.outgoing) {
                    if (trans.match(c)) {
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
        }
        return if (maxMatchedPos!=-1) {
            val matchedText = text.substring(startPosition, maxMatchedPos)
            MatchResult(matchedText, eolPositions)
        } else {
            null
        }
    }



}