/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.api.grammar.NodeType
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.sppt.SPPTLeafDefault
import net.akehurst.language.parser.sppt.SharedPackedParseTreeDefault
import kotlin.math.max

class ScannerlessParser(private val runtimeRuleSet: RuntimeRuleSet) : Parser {

    override val nodeTypes: Set<NodeType> by lazy {
        emptySet<NodeType>() //TODO:
    }

    override fun build() {
        this.runtimeRuleSet.buildCaches()
    }

    override fun scan(inputText: CharSequence, includeSkipRules:Boolean): List<SPPTLeaf> {
        val undefined = RuntimeRule(-5, "undefined",RuntimeRuleKind.TERMINAL, false, true)
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromCharSequence(inputText)
        val terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.allNonSkipTerminals
        var result = mutableListOf<SPPTLeaf>()

        var position = 0
        var lastLocation = InputLocation(0,1,1,0)
        while (!input.isEnd(position)) {
            val matches: List<SPPTLeaf> = terminals.mapNotNull {
                val match = input.tryMatchText(position, it.patternText, it.isPattern)
                if (null == match) {
                    null
                } else {
                    val location = input.nextLocation(lastLocation,match)
                    SPPTLeafDefault(it, location, false, match, (if (it.isPattern) 0 else 1))
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWith(Comparator<SPPTLeaf> { l1, l2 ->
                when {
                    l1.isLiteral && l2.isPattern -> 1
                    l1.isPattern && l2.isLiteral -> -1
                    else -> when {
                        l1.matchedTextLength > l2.matchedTextLength -> 1
                        l2.matchedTextLength > l1.matchedTextLength -> -1
                        else -> 0
                    }
                }
            })
            if (null == longest) {
                val text = inputText[position].toString()
                lastLocation = input.nextLocation(lastLocation,text)
                val unscanned = SPPTLeafDefault(undefined, lastLocation,false, text,0)
                result.add(unscanned)
                position++
            } else {
                result.add(longest)
                position = longest.nextInputPosition
                lastLocation = longest.location
            }
        }
        return result
    }

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromCharSequence(inputText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)

        rp.start(goalRule)
        var seasons = 1
        var maxNumHeads = graph.growingHead.size
        //       println("[$seasons] ")
        //       graph.growingHead.forEach {
        //           println("  $it")
        //       }

        do {
            rp.grow()
//            println("[$seasons] ")
//            graph.growingHead.forEach {
            //               println("  $it")
            //          }
            seasons++
            maxNumHeads = max(maxNumHeads, graph.growingHead.size)
        } while (rp.canGrow)

        val match = graph.longestMatch(rp.longestLastGrown, seasons, maxNumHeads)
        return SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
    }


    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuntimeRule> {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val usedText = inputText.subSequence(0, position)
        val input = InputFromCharSequence(usedText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)

        rp.start(goalRule)
        var seasons = 1

        // final int length = text.length();
        val matches = mutableListOf<GrowingNode>()

        do {
            rp.grow()
            seasons++
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
        } while (rp.canGrow)

        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
        // must reject the next expected

        val expected = mutableSetOf<RuntimeRule>()
        for (ep in matches) {
            var done = false
            // while (!done) {
            if (ep.canGrowWidth) {
                expected.addAll(ep.nextExpectedItems)
                done = true
                // TODO: sum from all parents
                // gn = gn.getPossibleParent().get(0).node;// .getNextExpectedItem();
            } else {
                // if has height potential?
                // gn = gn.getPossibleParent().get(0).node;

            }
            // }
        }
        // final List<RuntimeRule> expected = longest.getNextExpectedItem();
        val nextExpected = mutableListOf<RuntimeRule>()
        for (rr in expected) {
            nextExpected.add(rr)
        }
        // add skip rules at end
        for (rr in this.runtimeRuleSet.allSkipRules) {
            nextExpected.add(rr)
        }
        return nextExpected
    }
}