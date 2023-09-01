/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.agl.parser

//import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.sppt.LeafData

internal class Scanner(
    internal val runtimeRuleSet: RuntimeRuleSet
) {

    fun scan(inputText: String, includeSkipRules: Boolean): List<LeafData> {
        val undefined = RuntimeRuleSet.UNDEFINED_RULE
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, inputText)
        var terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.nonSkipTerminals
        val result = mutableListOf<LeafData>()

        //eliminate tokens that are empty matches
        terminals = terminals.filter { it.isEmptyTerminal.not() }

        var startPosition = 0
        var nextInputPosition = 0
        while (!input.isEnd(nextInputPosition)) {
            val matches: List<LeafData> = terminals.mapNotNull {
                val match = input.tryMatchText(nextInputPosition, it)
                if (null == match) {
                    null
                } else {
                    val ni = nextInputPosition + match.matchedText.length
                    val loc = input.locationFor(startPosition, ni - startPosition)
                    val matchedText = inputText.substring(loc.position, loc.length)
                    LeafData(it.tag, it.isPattern, loc, matchedText, emptyList())
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWithOrNull(Comparator<LeafData> { l1, l2 ->
                when {
                    l1.isPattern.not() && l2.isPattern -> 1
                    l1.isPattern && l2.isPattern.not() -> -1
                    else -> when {
                        l1.location.length > l2.location.length -> 1
                        l2.location.length > l1.location.length -> -1
                        else -> 0
                    }
                }
            })
            when {
                (null == longest || longest.location.length == 0) -> {
                    //TODO: collate unscanned, rather than make a separate token for each char
                    val text = inputText[nextInputPosition].toString()
                    nextInputPosition += text.length
                    val eolPositions = emptyList<Int>()//TODO calculate
                    val loc = input.locationFor(startPosition, nextInputPosition - startPosition)
                    val matchedText = inputText.substring(loc.position, loc.length)
                    val ld = LeafData(undefined.tag, false, loc, matchedText, emptyList())
                    //unscanned.eolPositions = input.eolPositions(text)
                    result.add(ld)
                }

                else -> {
                    result.add(longest)
                    nextInputPosition += longest.location.length
                }
            }
            startPosition = nextInputPosition
        }
        return result
    }

}