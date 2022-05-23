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

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.math.max

internal class ScanOnDemandParser(
    internal val runtimeRuleSet: RuntimeRuleSet
) : Parser {

    // cached only so it can be interrupted
    private var runtimeParser: RuntimeParser? = null

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun buildFor(goalRuleName: String, automatonKind: AutomatonKind) {
        this.runtimeRuleSet.buildFor(goalRuleName, automatonKind)
    }

    override fun parseForGoal(goalRuleName: String, inputText: String, automatonKind: AutomatonKind): Pair<SharedPackedParseTree?, List<LanguageIssue>> {
        //FIXME: currently only works  if build is called
        //this.runtimeRuleSet.buildFor(goalRuleName,automatonKind)

        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, inputText)
        val s0 = runtimeRuleSet.fetchStateSetFor(goalRule, automatonKind).startState
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        val rp = RuntimeParser(s0.stateSet, skipStateSet, goalRule, input)
        this.runtimeParser = rp

        rp.start(0, setOf(LookaheadSet.EOT))
        var seasons = 1
        var maxNumHeads = rp.graph.numberOfHeads
        var totalWork = maxNumHeads

        while (rp.graph.canGrow && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            if (Debug.OUTPUT_RUNTIME) println("$seasons ===================================")
            val steps = rp.grow3(false)
            seasons += steps
            maxNumHeads = max(maxNumHeads, rp.graph.numberOfHeads)
            totalWork += rp.graph.numberOfHeads
        }

        //TODO: when parsing an ambiguous grammar,
        // how to know we have found all goals? - keep going until cangrow is false
        // but - how to stop .. some grammars don't stop if we don't do test for a goal!
        // e.g. leftRecursive.test_aa
        // e.g. test_a1bOa2.ambiguous_a

        //val match = rp.longestMatch(seasons, maxNumHeads, false)
        val match = rp.graph.treeData
        return if (match.root != null) {
            //val sppt = SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
            val sppt = SPPTFromTreeData(match, input, seasons, maxNumHeads)
            Pair(sppt, emptyList())
        } else {
            val nextExpected = this.findNextExpectedAfterError2(rp, rp.graph, input) //this possibly modifies rp and hence may change the longestLastGrown
            val issue = throwError(input, rp, nextExpected, seasons, maxNumHeads)
            val sppt = null//rp.longestLastGrown?.let{ SharedPackedParseTreeDefault(it, seasons, maxNumHeads) }
            Pair(sppt, listOf(issue))
        }
    }

    private fun throwError(
        input: InputFromString,
        rp: RuntimeParser,
        nextExpected: Pair<InputLocation, Set<RuntimeRule>>,
        seasons: Int,
        maxNumHeads: Int
    ): LanguageIssue {
        val lastLocation = nextExpected.first
        val exp = nextExpected.second
        val expected = exp.map { it.tag }.toSet()
        val errorPos = lastLocation.position + lastLocation.length
        val errorLength = 1 //TODO: determine a better length
        val location = input.locationFor(errorPos, errorLength)//InputLocation(errorPos, errorColumn, errorLine, errorLength)

        val contextInText = rp.graph.input.contextInText(location.position)
        return LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, location, contextInText, expected)
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
        val grown = rp.tryGrowWidthOnce()
        val poss = if (graph.canGrow) {
            rp.tryGrowHeightOrGraft()
        } else {
            rp.resetGraphToLastGrown()
            rp.tryGrowHeightOrGraft()
        }
        val r = poss.map { (lg, prevs) ->
            //val prevs = rp._lastGss.peek(lg)
            // compute next expected item/RuntimeRule
            when (lg.runtimeState.state.runtimeRules.first().kind) {
                RuntimeRuleKind.GOAL -> {
                    val trs = lg.runtimeState.transitions(RuntimeState(rp.stateSet.startState, setOf(LookaheadSet.EMPTY)))
                    val exp: Set<RuntimeRule> = trs.flatMap { tr ->
                        when (tr.action) {
                            Transition.ParseAction.GOAL -> emptySet<RuntimeRule>()
                            Transition.ParseAction.WIDTH -> lg.runtimeState.runtimeLookaheadSet.flatMap { lg.runtimeState.state.firstOf(it).fullContent }.toSet() //TODO: needs prev as arg to firstOf
                            Transition.ParseAction.EMBED -> TODO()
                            Transition.ParseAction.HEIGHT -> lg.runtimeState.runtimeLookaheadSet.flatMap { rt -> tr.lookahead.flatMap { it.guard.resolveUP(rt).fullContent } }.toSet()
                            Transition.ParseAction.GRAFT -> prevs.flatMap { it.runtimeState.runtimeLookaheadSet.flatMap { rt -> tr.lookahead.flatMap { it.guard.resolveUP(rt).fullContent } } }
                                .toSet()
                        }
                    }.toSet()
                    Pair(lg, exp)
                }
                else -> {
                    val x: Set<RuntimeRule> = prevs.flatMap { prev ->
                        val trs = lg.runtimeState.transitions(prev.runtimeState).filter { it.runtimeGuard(it, prev, prev.runtimeState.state.rulePositions) }
                        val exp = trs.flatMap { tr ->
                            when (tr.action) {
                                Transition.ParseAction.GOAL -> emptySet<RuntimeRule>()
                                Transition.ParseAction.WIDTH -> lg.runtimeState.runtimeLookaheadSet.flatMap { lg.runtimeState.state.firstOf(it).fullContent }.toSet()
                                Transition.ParseAction.EMBED -> lg.runtimeState.runtimeLookaheadSet.flatMap { lg.runtimeState.state.firstOf(it).fullContent }.toSet()
                                Transition.ParseAction.HEIGHT -> lg.runtimeState.runtimeLookaheadSet.flatMap { rt -> tr.lookahead.flatMap { it.guard.resolveUP(rt).fullContent } }.toSet()
                                Transition.ParseAction.GRAFT -> prev.runtimeState.runtimeLookaheadSet.flatMap { rt -> tr.lookahead.flatMap { it.guard.resolveUP(rt).fullContent } }.toSet()
                            }
                        }.toSet()
                        exp
                    }.toSet()
                    Pair(lg, x)
                }
            }
        }
        val maxLastLocation: InputLocation = r.map { input.locationFor(it.first.startPosition, it.first.nextInputPosition - it.first.startPosition) }
            .maxByOrNull { it.endPosition }
            ?: error("Internal error")
        val fr = r.filter { it.first.nextInputPosition == maxLastLocation.endPosition }
        val res = fr.flatMap { it.second }.toSet()
        return Pair(maxLastLocation, res)
    }

    private fun findNextExpectedAfterError2(rp: RuntimeParser, graph: ParseGraph, input: InputFromString): Pair<InputLocation, Set<RuntimeRule>> {
        rp.resetGraphToLastGrown()
        val poss =  rp.tryGrowHeightOrGraft()//graph.peekAllHeads()
        val r = poss.map { (lg, prevs) ->
            when {
                lg.runtimeState.state.isGoal -> {
                    val trs = lg.runtimeState.transitions(RuntimeState(rp.stateSet.startState, setOf(LookaheadSet.EMPTY)))
                    val errors: Set<Pair<Int, Set<RuntimeRule>>> = trs.mapNotNull { tr ->
                        when (tr.action) {
                            Transition.ParseAction.GOAL -> null
                            Transition.ParseAction.WIDTH,
                            Transition.ParseAction.EMBED -> {
                                // try grab 'to' token, if nothing then that is error else lookahead is error
                                val l = graph.input.findOrTryCreateLeaf(tr.to.firstRule, lg.nextInputPosition)
                                when (l) {
                                    null -> Pair(lg.nextInputPosition, tr.to.runtimeRules.toSet())
                                    else -> {
                                        val expected = tr.lookahead.flatMap { lh -> lg.runtimeState.runtimeLookaheadSet.flatMap { lh.guard.resolveUP(it).fullContent } }.toSet()
                                        val pos = l.nextInputPosition
                                        Pair(pos, expected)
                                    }
                                }
                            }
                            else -> TODO("")
                        }
                    }.toSet()
                    Pair(lg, errors)
                }
                else -> {
                    val errors: Set<Pair<Int, Set<RuntimeRule>>> = prevs.flatMap { prev ->
                        val trs = lg.runtimeState.transitions(prev.runtimeState).filter { it.runtimeGuard(it, prev, prev.runtimeState.state.rulePositions) }
                        val pairs: Set<Pair<Int, Set<RuntimeRule>>> = trs.mapNotNull { tr ->
                            when (tr.action) {
                                Transition.ParseAction.GOAL -> null
                                Transition.ParseAction.WIDTH,
                                Transition.ParseAction.EMBED -> {
                                    // try grab 'to' token, if nothing then that is error else lookahead is error
                                    val l = graph.input.findOrTryCreateLeaf(tr.to.firstRule, lg.nextInputPosition)
                                    when (l) {
                                        null -> Pair(lg.nextInputPosition, tr.to.runtimeRules.toSet())
                                        else -> {
                                            val expected = tr.lookahead.flatMap { lh -> lg.runtimeState.runtimeLookaheadSet.flatMap { lh.guard.resolveUP(it).fullContent } }.toSet()
                                            val pos = l.nextInputPosition
                                            Pair(pos, expected)
                                        }
                                    }
                                }
                                else -> {
                                    val expected = tr.lookahead.flatMap { lh -> lg.runtimeState.runtimeLookaheadSet.flatMap { lh.guard.resolveUP(it).fullContent } }.toSet()
                                    val pos = lg.nextInputPosition
                                    Pair(pos, expected)
                                }
                            }
                        }.toSet()
                        pairs
                    }.toSet()
                    Pair(lg, errors)
                }
            }
        }
        val errorLocations = r.flatMap { (lg, errors) ->
            errors.map { (pos, expected) -> Pair(input.locationFor(pos, 0), expected) } //FIXME: length maybe not correct
        }
        val maxLastLocation = errorLocations.maxByOrNull { it.first.endPosition } ?: error("Internal error")
        val fr = errorLocations.filter { it.first.position == maxLastLocation.first.position }
        val res = fr.flatMap { it.second }.toSet()
        return Pair(maxLastLocation.first, res)
    }

    private fun findNextExpected(rp: RuntimeParser, graph: ParseGraph, input: InputFromString, gns: List<Pair<GrowingNode, Set<GrowingNodeIndex>>>): Set<RuntimeRule> {
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf

        val matches = gns.toMutableList()
        // try grow last leaf with no lookahead
        rp.resetGraphToLastGrown()
        do {
            rp.grow3(true)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    // lastGrown is combination of growing and toGrow
                    //  the previous of groowing is on the node, of toGrow is in the Map
                    val prev = rp.grownInLastPassPrevious[gn] ?: emptySet()
                    matches.add(Pair(gn, prev))
                }
            }
        } while (rp.canGrow && graph.goals.isEmpty())

        //val nextExpected = matches
        //    .filter { it.canGrowWidth }
        //    .flatMap { it.nextExpectedItems }
        //    .toSet()
        //return nextExpected

        val trans_lh_pairs = matches.flatMap { gn_prev ->
            val gn = gn_prev.first
            val prev = gn_prev.second.map { it.runtimeState }
            val trans = prev.flatMap { pr -> gn.runtimeState.transitions(pr) }.toSet()
            trans.flatMap { tr ->
                val pairs = gn.runtimeLookahead.map { rt -> Pair(tr, tr.lookahead.flatMap { it.guard.resolveUP(rt).content }) }.toSet()
                pairs
            }
        }.toSet()

        val result = trans_lh_pairs.flatMap { tr_lh ->
            val tr = tr_lh.first
            val lh = tr_lh.second
            when (tr.action) {
                Transition.ParseAction.GOAL -> lh
                Transition.ParseAction.WIDTH -> tr.to.runtimeRulesSet
                Transition.ParseAction.GRAFT -> lh
                //         Transition.ParseAction.GRAFT_OR_HEIGHT -> lh
                Transition.ParseAction.HEIGHT -> lh
                Transition.ParseAction.EMBED -> TODO()
            }
        }.toSet()
        return result
    }

    override fun expectedAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule> {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val usedText = inputText.substring(0, position)
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, usedText)
        val ss = runtimeRuleSet.fetchStateSetFor(goalRule, automatonKind)
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        val rp = RuntimeParser(ss, skipStateSet, goalRule, input)
        this.runtimeParser = rp

        rp.start(0, setOf(LookaheadSet.EOT))
        var seasons = 1

        val matches = mutableListOf<Pair<GrowingNode, Set<GrowingNodeIndex>>>()
        do {
            rp.grow3(false)
            for (gn in rp.lastGrown) {
                if (input.isEnd(gn.nextInputPosition)) {
                    val prev = rp.grownInLastPassPrevious[gn] ?: emptySet()
                    if (gn.currentState.isGoal.not()) {
                        //don't include it TODO: why does this happen?
                    } else {
                        matches.add(Pair(gn, prev))
                    }
                }
            }
            seasons++
        } while (rp.canGrow && rp.graph.goals.isEmpty())
        val nextExpected = this.findNextExpected(rp, rp.graph, input, matches)
        return nextExpected
    }

    override fun expectedTerminalsAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule> {
        return this.expectedAt(goalRuleName, inputText, position, automatonKind)
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