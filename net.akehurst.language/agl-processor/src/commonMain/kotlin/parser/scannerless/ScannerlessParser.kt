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

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.sppt.SPPTLeafDefault
import net.akehurst.language.parser.sppt.SharedPackedParseTreeDefault
import kotlin.math.max

class ScannerlessParser(
        private val runtimeRuleSet: RuntimeRuleSet
) : Parser {

    private var runtimeParser: RuntimeParser? = null

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun build() {
        this.runtimeRuleSet.buildCaches()
    }

    override fun scan(inputText: CharSequence, includeSkipRules: Boolean): List<SPPTLeaf> {
        val undefined = RuntimeRule(-5, "undefined", "", RuntimeRuleKind.TERMINAL, false, true)
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromCharSequence(inputText)
        var terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.nonSkipTerminals
        var result = mutableListOf<SPPTLeaf>()

        //eliminate tokens that are empty matches
        terminals = terminals.filter {
            it.value.isNotEmpty()
        }.toTypedArray()

        var position = 0
        var lastLocation = InputLocation(0, 1, 1, 0)
        while (!input.isEnd(position)) {
            val matches: List<SPPTLeaf> = terminals.mapNotNull {
                val match = input.tryMatchText(position, it.value, it.isPattern)
                if (null == match) {
                    null
                } else {
                    val location = input.nextLocation(lastLocation, match.matchedText.length)
                    val leaf = SPPTLeafDefault(it, location, false, match.matchedText, (if (it.isPattern) 0 else 1))
                    leaf.eolPositions = match.eolPositions
                    leaf
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
            when {
                (null == longest || longest.matchedTextLength==0) -> {
                    //TODO: collate unscanned, rather than make a separate token for each char
                    val text = inputText[position].toString()
                    lastLocation = input.nextLocation(lastLocation, text.length)
                    val unscanned = SPPTLeafDefault(undefined, lastLocation, false, text, 0)
                    unscanned.eolPositions = input.eolPositions(text)
                    result.add(unscanned)
                    position++
                }
                else -> {
                    result.add(longest)
                    position = longest.nextInputPosition
                    lastLocation = longest.location
                }
            }
        }
        return result
    }

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromCharSequence(inputText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)
        this.runtimeParser = rp

        rp.start(goalRule)
        var seasons = 1
        var maxNumHeads = graph.growingHead.size
        //       println("[$seasons] ")
        //       graph.growingHead.forEach {
        //           println("  $it")
        //       }

        do {
            rp.grow(false)
//            println("[$seasons] ")
//            graph.growingHead.forEach {
            //               println("  $it")
            //          }
            seasons++
            maxNumHeads = max(maxNumHeads, graph.growingHead.size)
//        } while (rp.canGrow)
        } while (rp.canGrow && graph.goals.isEmpty())
        //TODO: when parsing an ambiguous grammar,
        // how to know we have found all goals? - keep going until cangrow is false
        // but - stop .. some grammars don't stop if we don't do test for a goal!
        // e.g. leftRecursive.test_aa

        val match = graph.longestMatch(seasons, maxNumHeads)
        return if (match != null) {
            SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            throwError(graph, rp, seasons, maxNumHeads)
        }
    }

    private fun throwError(graph: ParseGraph, rp: RuntimeParser, seasons: Int, maxNumHeads: Int): SharedPackedParseTreeDefault {
        val llg = rp.longestLastGrown ?: throw ParseFailedException("Nothing parsed", null, InputLocation(0, 0, 1, 0))
        val possibleNextLeaf: SPPTLeafDefault? = null//graph.leaves.values.filter { it.startPosition == llg.nextInputPosition }.firstOrNull()
        if (null != possibleNextLeaf) {
            val errorPos = possibleNextLeaf.location.position + possibleNextLeaf.location.length
            val lastEolPos = possibleNextLeaf.matchedText.lastIndexOf('\n')
            val errorLine = possibleNextLeaf.location.line + possibleNextLeaf.numberOfLines
            val errorColumn = when {
                possibleNextLeaf.lastLocation.position == 0 && possibleNextLeaf.lastLocation.length == 0 -> errorPos + 1
                -1 == lastEolPos -> errorPos + 1
                else -> possibleNextLeaf.matchedTextLength - lastEolPos
            }
            val errorLength = 1
            val location = InputLocation(errorPos, errorColumn, errorLine, errorLength)
            throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location)
        } else {
            val errorPos = llg.lastLocation.position + llg.lastLocation.length
            val lastEolPos = llg.matchedText.lastIndexOf('\n')
            val errorLine = llg.location.line + llg.numberOfLines
            val errorColumn = when {
                llg.lastLocation.position == 0 && llg.lastLocation.length == 0 -> errorPos + 1
                -1 == lastEolPos -> llg.lastLocation.column + llg.lastLocation.length
                else -> llg.matchedTextLength - lastEolPos
            }
            val errorLength = 1
            val location = InputLocation(errorPos, errorColumn, errorLine, errorLength)//this.input.calcLineAndColumn(llg.nextInputPosition)
            throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location)
        }

    }

    /*
        private fun findNextExpected() {
            // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
            // try grow last leaf with no lookahead
            for (gn in rp.lastGrownLinked) {
                val gnindex = GrowingNodeIndex(gn.currentState, gn.startPosition, gn.nextInputPosition, gn.priority)
                graph.growingHead[gnindex] = gn
            }
            do {
                rp.grow(true)
                for (gn in rp.lastGrown) {
                    // may need to change this to finalInputPos!
                    if (input.isEnd(gn.nextInputPosition)) {
                        matches.add(gn)
                    }
                }
                seasons++
            } while (rp.canGrow)


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
            //for (rr in this.runtimeRuleSet.skipRules) {
            //    nextExpected.add(rr)
            //}
            return nextExpected
        }
    */
    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuntimeRule> {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val usedText = inputText.subSequence(0, position)
        val input = InputFromCharSequence(usedText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)
        this.runtimeParser = rp

        rp.start(goalRule)
        var seasons = 1

        // final int length = text.length();
        val matches = mutableListOf<GrowingNode>()

        do {
            rp.grow(false)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
            seasons++
        } while (rp.canGrow)

        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
        // try grow last leaf with no lookahead
        for (gn in rp.lastGrownLinked) {
            val gnindex = GrowingNodeIndex(gn.currentState, gn.startPosition, gn.nextInputPosition, gn.priority)
            graph.growingHead[gnindex] = gn
        }
        do {
            rp.grow(true)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
            seasons++
        } while (rp.canGrow)


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
        //for (rr in this.runtimeRuleSet.skipRules) {
        //    nextExpected.add(rr)
        //}
        return nextExpected
    }

    override fun expectedTerminalsAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuntimeRule> {
        return this.expectedAt(goalRuleName, inputText, position).flatMap { it.itemsAt[0].toList() }
    }
}