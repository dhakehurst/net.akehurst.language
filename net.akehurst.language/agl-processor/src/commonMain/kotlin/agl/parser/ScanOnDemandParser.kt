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

package net.akehurst.language.agl.parser

import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTLeafDefault
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.agl.sppt.SharedPackedParseTreeDefault
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.math.max

class ScanOnDemandParser(
        internal val runtimeRuleSet: RuntimeRuleSet
) : Parser {

    private var runtimeParser: RuntimeParser? = null

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun buildFor(goalRuleName: String) {
        this.runtimeRuleSet.buildFor(goalRuleName)
    }

    override fun scan(inputText: String, includeSkipRules: Boolean): List<SPPTLeaf> {
        val undefined = RuntimeRule(-1, -5, "undefined", "", RuntimeRuleKind.TERMINAL, false, true)
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size,inputText)
        var terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.nonSkipTerminals
        var result = mutableListOf<SPPTLeaf>()

        //eliminate tokens that are empty matches
        terminals = terminals.filter {
            it.value.isNotEmpty()
        }.toTypedArray()

        var startPosition = 0
        var nextInputPosition = 0
        while (!input.isEnd(nextInputPosition)) {
            val matches: List<SPPTLeaf> = terminals.mapNotNull {
                val match = input.tryMatchText(nextInputPosition, it.value, it.pattern)
                if (null == match) {
                    null
                } else {
                    val ni = nextInputPosition + match.length
                    val leaf = SPPTLeafFromInput(input, it, startPosition, ni, (if (it.isPattern) 0 else 1))
                    //leaf.eolPositions = match.eolPositions
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
                (null == longest || longest.matchedTextLength == 0) -> {
                    //TODO: collate unscanned, rather than make a separate token for each char
                    val text = inputText[nextInputPosition].toString()
                    nextInputPosition +=text.length
                    val unscanned = SPPTLeafFromInput(input,undefined, startPosition, nextInputPosition,0)
                    //unscanned.eolPositions = input.eolPositions(text)
                    result.add(unscanned)
                }
                else -> {
                    result.add(longest)
                    nextInputPosition +=longest.matchedTextLength
                }
            }
            startPosition = nextInputPosition
        }
        return result
    }

    override fun parse(goalRuleName: String, inputText: String): SharedPackedParseTree {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size,inputText)
        val s0 = runtimeRuleSet.startingState(goalRule)
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        val rp = RuntimeParser(s0.stateSet,skipStateSet, goalRule, LookaheadSet.EOT, input)
        this.runtimeParser = rp

        rp.start(0, LookaheadSet.EOT)
        var seasons = 1
        var maxNumHeads = rp.graph.growingHead.size
        var totalWork = maxNumHeads

        do {
            rp.grow(false)
            seasons++
            maxNumHeads = max(maxNumHeads, rp.graph.growingHead.size)
            totalWork += rp.graph.growingHead.size
        } while (rp.graph.canGrow)// && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not()))
        //TODO: when parsing an ambiguous grammar,
        // how to know we have found all goals? - keep going until cangrow is false
        // but - how to stop .. some grammars don't stop if we don't do test for a goal!
        // e.g. leftRecursive.test_aa

        val match = rp.graph.longestMatch(seasons, maxNumHeads)
        return if (match != null) {
            SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            val nextExpected = this.findNextExpectedAfterError(rp, rp.graph, input) //this possibly modifies rp and hence may change the longestLastGrown
            throwError(rp.graph, rp, nextExpected, seasons, maxNumHeads)
        }
    }

    private fun throwError(graph: ParseGraph, rp: RuntimeParser, nextExpected: Pair<InputLocation, Set<RuntimeRule>>, seasons: Int, maxNumHeads: Int): SharedPackedParseTreeDefault {
        val llg = rp.longestLastGrown ?: throw ParseFailedException("Nothing parsed", null, InputLocation(0, 0, 1, 0), emptySet())

        val lastLocation = nextExpected.first
        val exp = nextExpected.second
        //val expected = exp
        //        .filter { it.number >= 0 && it.isEmptyRule.not() }
        //        .map { this.runtimeRuleSet.firstTerminals[it.number] }
        //        .flatMap { it.map { it.value } }
        //        .toSet()
        val expected = exp.map { it.tag }.toSet()
        val errorPos = lastLocation.position + lastLocation.length
        val lastEolPos = llg.matchedText.lastIndexOf('\n')
        val errorLine = llg.location.line + llg.numberOfLines
        //val lastLocation = graph.input.locationFor(llg.lastLeaf.startPosition, llg.lastLeaf.nextInputPosition)
        val errorColumn = when {
            lastLocation.position == 0 && lastLocation.length == 0 -> errorPos + 1
            -1 == lastEolPos -> lastLocation.column + lastLocation.length
            else -> llg.matchedTextLength - lastEolPos
        }
        val errorLength = 1
        val location = InputLocation(errorPos, errorColumn, errorLine, errorLength)

        //val expected = emptySet<String>()
       // val location = nextExpected.first//InputLocation(0,0,0,0)
        throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location, expected)

    }

    private fun findNextExpectedAfterError(rp: RuntimeParser, graph: ParseGraph, input: InputFromString): Pair<InputLocation, Set<RuntimeRule>> {
        //If there is an error, it is because parsing has stopped before finding a goal.
        // parsing could have stopped because
        //  - no more input and we have not reached a goal
        //  - no new growing-head because no action/transition taken, because
        //  -- lookahead is wrong when doing an action
        //  -- other guards eliminate the transition
        //  - action causes no change (I think this should never happen)
        // if it stopped because of lookahead...it maybe that we can parse one more leaf,
        //

        rp.resetGraphToLastGrown()
        rp.tryGrowWidthOnce()
        val poss = if (graph.canGrow) {
            rp.tryGrowHeightOrGraft()
        } else {
            rp.resetGraphToLastGrown()
            rp.tryGrowHeightOrGraft()
        }

        val r = poss.map { lg ->
            // compute next expected item/RuntimeRule
            when (lg.runtimeRules.first().kind) {
                RuntimeRuleKind.GOAL -> {
                    val trs = lg.currentState.transitions(null)
                    val exp = trs.flatMap {tr ->
                        when(tr.action) {
                            Transition.ParseAction.GOAL -> emptySet<RuntimeRule>()
                            Transition.ParseAction.WIDTH -> lg.currentState.firstOf(lg.lookahead.content)
                            Transition.ParseAction.EMBED -> TODO()
                            Transition.ParseAction.HEIGHT -> tr.lookaheadGuard.createWithParent(lg.lookahead).content
                            Transition.ParseAction.GRAFT -> lg.previous.values.map { it.node.lookahead }.flatMap{ tr.lookaheadGuard.createWithParent(it).content }
                        }
                    }
                    Pair(lg, exp)
                }
                else -> {
                    val trs = lg.previous.values.flatMap { prev ->
                        lg.currentState.transitions(prev.node.currentState)//TODO: do we need to filter graft by runtimeGuard here?
                    }
                    val exp = trs.flatMap {tr ->
                        when(tr.action) {
                            Transition.ParseAction.GOAL -> emptySet<RuntimeRule>()
                            Transition.ParseAction.WIDTH -> lg.currentState.firstOf(lg.lookahead.content)
                            Transition.ParseAction.EMBED -> TODO()
                            Transition.ParseAction.HEIGHT -> tr.lookaheadGuard.createWithParent(lg.lookahead).content
                            Transition.ParseAction.GRAFT -> lg.previous.values.map { it.node.lookahead }.flatMap{ tr.lookaheadGuard.createWithParent(it).content }
                        }
                    }
                    Pair(lg, exp)
                }
            }
        }
        val maxLastLocation: InputLocation = r.map { input.locationFor(it.first.startPosition, it.first.nextInputPosition) }.maxBy { it.endPosition } ?: error("Internal error")
        val fr = r.filter { it.first.nextInputPosition == maxLastLocation.endPosition }
        val res = fr.flatMap { it.second }.toSet()
        return Pair(maxLastLocation, res)
    }

    private fun findNextExpected(rp: RuntimeParser, graph: ParseGraph, input: InputFromString, gns: List<GrowingNode>): Set<RuntimeRule> {
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf

        val matches = gns.toMutableList()
        // try grow last leaf with no lookahead
        for (gn in rp.lastGrownLinked) {
            val gnindex = GrowingNode.index(gn.currentState, gn.children)
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
        } while (rp.canGrow && graph.goals.isEmpty())

        val nextExpected = matches
                .filter { it.canGrowWidth }
                .flatMap { it.nextExpectedItems }
                .toSet()
        return nextExpected
    }

    override fun expectedAt(goalRuleName: String, inputText: String, position: Int): Set<RuntimeRule> {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val usedText = inputText.substring(0, position)
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size,usedText)
        val ss = runtimeRuleSet.fetchStateSetFor(goalRule)
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        val rp = RuntimeParser(ss,skipStateSet, goalRule, LookaheadSet.EOT, input)
        this.runtimeParser = rp

        rp.start(0, LookaheadSet.EOT)
        var seasons = 1

        val matches = mutableListOf<GrowingNode>()
        do {
            rp.grow(false)
            for (gn in rp.lastGrown) {
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
            seasons++
        } while (rp.canGrow && rp.graph.goals.isEmpty())
        val nextExpected = this.findNextExpected(rp, rp.graph, input, matches)
        return nextExpected
    }

    override fun expectedTerminalsAt(goalRuleName: String, inputText: String, position: Int): Set<RuntimeRule> {
        return this.expectedAt(goalRuleName, inputText, position)
                .flatMap {
                    when (it.kind) {
                        RuntimeRuleKind.TERMINAL -> listOf(it)
                        RuntimeRuleKind.NON_TERMINAL -> this.runtimeRuleSet.firstTerminals[it.number]
                        else -> TODO()
                    }
                }
                .toSet()
    }
}