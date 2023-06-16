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
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ParseResultDefault
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ParseResult
import kotlin.math.max

internal class ScanOnDemandParser(
    internal val runtimeRuleSet: RuntimeRuleSet
) : Parser {

    // cached only so it can be interrupted
    private var runtimeParser: RuntimeParser? = null

    private val _issues = IssueHolder(LanguageProcessorPhase.PARSE)

    val runtimeDataIsEmpty: Boolean get() = runtimeParser?.graph?.isEmpty ?: true

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun buildFor(goalRuleName: String, automatonKind: AutomatonKind) {
        this.runtimeRuleSet.buildFor(goalRuleName, automatonKind)
    }

    override fun parseForGoal(goalRuleName: String, inputText: String, automatonKind: AutomatonKind): ParseResult { //Pair<SharedPackedParseTree?, List<LanguageIssue>> {
        _issues.clear()
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, inputText)
        val rp = createRuntimeParser(goalRuleName, input, automatonKind)
        this.runtimeParser = rp

        val possibleEndOfText = setOf(LookaheadSet.EOT)
        rp.start(0, possibleEndOfText)
        var seasons = 1
        var maxNumHeads = rp.graph.numberOfHeads
        var totalWork = maxNumHeads

        var lastToTryWidth = emptyList<GrowingNodeIndex>()
        while (rp.graph.hasNextHead && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            if (Debug.OUTPUT_RUNTIME) println("$seasons ===================================")
            val steps = rp.grow3(possibleEndOfText, RuntimeParser.normalArgs)
            seasons += steps
            maxNumHeads = max(maxNumHeads, rp.graph.numberOfHeads)
            totalWork += rp.graph.numberOfHeads
            lastToTryWidth = rp.lastToTryWidthTrans
        }

        val match = rp.graph.treeData.complete
        return if (match.root != null) {
            //val sppt = SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
            val sppt = SPPTFromTreeData(match, input, seasons, maxNumHeads)
            ParseResultDefault(sppt, this._issues)
        } else {
            //val nextExpected = this.findNextExpectedAfterError2(rp, lastToTryWidth, input, possibleEndOfText) //this possibly modifies rp and hence may change the longestLastGrown
            val nextExpected = this.findNextExpectedAfterError3(input, rp.failedReasons, rp.stateSet.automatonKind, rp.stateSet)
            addParseIssue(input, rp, nextExpected, seasons, maxNumHeads)
            val sppt = null//rp.longestLastGrown?.let{ SharedPackedParseTreeDefault(it, seasons, maxNumHeads) }
            ParseResultDefault(sppt, this._issues)
        }
    }

    private fun createRuntimeParser(goalRuleName: String, input: InputFromString, automatonKind: AutomatonKind): RuntimeParser {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val s0 = runtimeRuleSet.fetchStateSetFor(goalRuleName, automatonKind).startState
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        return RuntimeParser(s0.stateSet, skipStateSet, goalRule, input, _issues)
    }

    private fun addParseIssue(
        input: InputFromString,
        rp: RuntimeParser,
        nextExpected: Pair<InputLocation, Set<RuntimeRule>>,
        seasons: Int,
        maxNumHeads: Int
    ) {
        val lastLocation = nextExpected.first
        val exp = nextExpected.second
        val expected = exp.map { it.tag }.toSet()
        val errorPos = lastLocation.position + lastLocation.length
        val errorLength = 1 //TODO: determine a better length
        val location = input.locationFor(errorPos, errorLength)//InputLocation(errorPos, errorColumn, errorLine, errorLength)

        val contextInText = rp.graph.input.contextInText(location.position)
        this._issues.error(location, contextInText, expected)
        //return LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, location, contextInText, expected)
    }

    /*
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
    */
    private fun findNextExpectedAfterError2(
        rp: RuntimeParser,
        lastToTryWidth: List<GrowingNodeIndex>,
        input: InputFromString,
        possibleEndOfText: Set<LookaheadSet>
    ): Pair<InputLocation, Set<RuntimeRule>> {
        //val trips = lastToTryWidth//rp.lastToTryWidthTrans//lastToGrow //rp.lastDropped.flatMap { it.triples }
        val trips = lastToTryWidth.flatMap { rp.graph.peekTripleFor(it) }
        // for anything not transitioning to empty do the width then try H or G
        rp.resetGraphToLastGrown(trips)//lastGrown)
        //val lgs1 =rp.growNonEmptyWidthForError(possibleEndOfText, RuntimeParser.Companion.GrowArgs(false, true, false, true))
        val lgs2 = rp.growHeightOrGraftForError(possibleEndOfText, RuntimeParser.Companion.GrowArgs(false, true, true, false))
        val triples = when {
            lgs2.isNotEmpty() -> lgs2 //.flatMap { it.triples }.toSet()
            //    lgs1.isNotEmpty() -> lgs1.flatMap { it.triples }.toSet()
            else -> trips.toSet()
        }
        val r = possErrors(triples, rp, input, possibleEndOfText)
        val errorLocations = r.map { (pos, expected) ->
            Pair(input.locationFor(pos, 0), expected) //FIXME: length maybe not correct
        }
        return if (errorLocations.isEmpty()) {
            Pair(InputLocation(0, 1, 0, 0), setOf(RuntimeRuleSet.END_OF_TEXT))
        } else {
            val maxLastLocation = errorLocations.maxBy { it.first.endPosition }
            val fr = errorLocations.filter { it.first.position == maxLastLocation.first.position }
            val res = fr.flatMap { it.second }.toSet()
            Pair(maxLastLocation.first, res)
        }
    }

    private fun possErrors(
        triples: Set<ParseGraph.Companion.ToProcessTriple>,
        rp: RuntimeParser,
        input: InputFromString,
        possibleEndOfText: Set<LookaheadSet>
    ): Set<Pair<Int, Set<RuntimeRule>>> {
        val graph = rp.graph
        val r: Set<Pair<Int, Set<RuntimeRule>>> = triples.flatMap { (lg, prev, remainingHead) ->
            when {
                lg.runtimeState.state.isGoal -> {
                    if (lg.runtimeState.isAtEnd) {
                        val runtimeLookahead = setOf(LookaheadSet.EOT)
                        val skipData = rp.parseSkipIfAny(lg.nextInputPosition, runtimeLookahead, LookaheadSet.ANY, possibleEndOfText, RuntimeParser.normalArgs)
                        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: lg.nextInputPosition
                        setOf(Pair(nextInputPositionAfterSkip, setOf(RuntimeRuleSet.END_OF_TEXT)))
                    } else {
                        val trs = lg.runtimeState.transitions(rp.stateSet.startState, rp.stateSet.startState)
                        if (trs.isEmpty()) {
                            setOf(Pair(lg.nextInputPosition, emptySet()))
                        } else {
                            val errors: Set<Pair<Int, Set<RuntimeRule>>> = trs.mapNotNull { tr ->
                                when (tr.action) {
                                    ParseAction.GOAL -> null
                                    ParseAction.WIDTH,
                                    ParseAction.EMBED -> {
                                        //TODO: find failure in embedded parser!
                                        val embLastDropped = rp.embeddedLastDropped[tr]?.flatMap { it.triples } //TODO: change this to lastGrown
                                        if (null == embLastDropped) {
                                            // try grab 'to' token, if nothing then that is error else lookahead is error
                                            val l = input.findOrTryCreateLeaf(tr.to.firstRule, lg.nextInputPosition)
                                            when (l) {
                                                null -> {
                                                    val expected = tr.to.runtimeRules.filter { it.isEmptyTerminal.not() }
                                                    if (expected.isEmpty()) {
                                                        null
                                                    } else {
                                                        listOf(Pair(lg.nextInputPosition, expected.toSet()))
                                                    }
                                                }

                                                else -> {
                                                    val rtLh = when (tr.action) {
                                                        ParseAction.WIDTH -> lg.runtimeState.runtimeLookaheadSet
                                                        ParseAction.EMBED -> lg.runtimeState.runtimeLookaheadSet
                                                        ParseAction.HEIGHT -> prev!!.runtimeState.runtimeLookaheadSet
                                                        ParseAction.GRAFT -> prev!!.runtimeState.runtimeLookaheadSet
                                                        ParseAction.GOAL -> prev!!.runtimeState.runtimeLookaheadSet
                                                    }
                                                    val expected = tr.lookahead
                                                        .flatMap { lh -> possibleEndOfText.flatMap { eot -> rtLh.flatMap { rt -> lh.guard.resolve(eot, rt).fullContent } } }
                                                        .filter { it.isEmptyTerminal.not() }
                                                    if (expected.isEmpty()) {
                                                        null
                                                    } else {
                                                        listOf(Pair(l.nextInputPosition, expected.toSet()))
                                                    }
                                                }
                                            }
                                        } else {
                                            val (embeddedParser, embeddedEOT) = rp.createEmbeddedRuntimeParser(possibleEndOfText, lg.runtimeState.runtimeLookaheadSet, tr)
                                            embeddedParser.resetGraphToLastGrown(embLastDropped)
                                            val embLgs = embeddedParser.growHeightOrGraftForError(embeddedEOT, RuntimeParser.normalArgs) //TODO: maybe diff args
                                            val embTriples = embLgs //.flatMap { it.triples }.toSet()
                                            possErrors(embTriples, embeddedParser, input, embeddedEOT)
                                        }
                                    }

                                    else -> TODO("")
                                }
                            }.flatten().toSet()
                            errors
                        }
                    }
                }

                else -> {
                    //FIXME: error option may not be correct, need to find the original
                    val prevPrev = remainingHead
                        ?: graph.createGrowingNodeIndex(rp.stateSet.startState, setOf(LookaheadSet.EMPTY), 0, 0, 0, 0, null)
                    val beforeRuntimeCheck = lg.runtimeState.transitions(prevPrev.runtimeState.state, prev!!.runtimeState.state)
                    if (beforeRuntimeCheck.isNotEmpty()) {
                        val pairs: Set<Pair<Int, Set<RuntimeRule>>> = beforeRuntimeCheck.mapNotNull { tr ->
                            val rtLh = when (tr.action) {
                                ParseAction.WIDTH -> lg.runtimeState.runtimeLookaheadSet
                                ParseAction.EMBED -> lg.runtimeState.runtimeLookaheadSet
                                ParseAction.HEIGHT -> prev.runtimeState.runtimeLookaheadSet
                                ParseAction.GRAFT -> prev.runtimeState.runtimeLookaheadSet
                                ParseAction.GOAL -> prev.runtimeState.runtimeLookaheadSet
                            }
                            val runtimeGuardPassed = tr.runtimeGuard.execute(prev.numNonSkipChildren)
                            when (tr.action) {
                                ParseAction.EMBED -> {
                                    //TODO: find failure in embedded parser!
                                    rp.resetGraphToLastGrown(listOf(ParseGraph.Companion.ToProcessTriple(lg, prev, prevPrev)))
                                    val ld = rp.growNonEmptyWidthForError(possibleEndOfText, RuntimeParser.Companion.GrowArgs(false, true, false, true))
                                    val nip = if (ld.isEmpty()) {
                                        rp.lastToGrow.maxOf { it.growingNode.nextInputPosition }
                                    } else {
                                        ld.maxOf { it.nextInputPosition }
                                    }
                                    val embLastDropped = rp.embeddedLastDropped[tr]?.flatMap { it.triples } //TODO: change this to lastGrown
                                    if (null == embLastDropped) {

                                        val expected = tr.lookahead
                                            .flatMap { lh -> possibleEndOfText.flatMap { eot -> rtLh.flatMap { rt -> lh.guard.resolve(eot, rt).fullContent } } }
                                            .filter { it.isEmptyTerminal.not() }
                                        if (expected.isEmpty()) {
                                            null
                                        } else {
                                            listOf(Pair(nip, expected.toSet()))
                                        }

                                    } else {
                                        val (embeddedParser, embeddedEOT) = rp.createEmbeddedRuntimeParser(possibleEndOfText, lg.runtimeState.runtimeLookaheadSet, tr)
                                        embeddedParser.resetGraphToLastGrown(embLastDropped)
                                        val embLgs = embeddedParser.growHeightOrGraftForError(
                                            embeddedEOT,
                                            RuntimeParser.Companion.GrowArgs(false, true, false, true)
                                        ) //TODO: maybe diff args
                                        val embTriples = embLgs //.flatMap { it.triples }.toSet()
                                        possErrors(embTriples, embeddedParser, input, embeddedEOT)
                                    }
                                }

                                else -> errorPairs(rp, input, lg, tr, possibleEndOfText, rtLh, runtimeGuardPassed)
                            }

                        }.flatten().toSet()
                        pairs
                    } else {
                        //no transitions so have to assume we parsed something we didn't want.
                        val ntp = graph.peekAllNextToProcess()
                        val lg2 = prev
                        val prev2 = prevPrev
                        ntp.flatMap { tpt ->
                            val prevPrev2 = tpt.previous?.runtimeState ?: RuntimeState(rp.stateSet.startState, setOf(LookaheadSet.EMPTY))
                            val trs2 = lg2.runtimeState.transitions(prevPrev2.state, prev2.runtimeState.state)
                            trs2.mapNotNull { tr2 ->
                                val rtLh = when (tr2.action) {
                                    ParseAction.WIDTH -> lg2.runtimeState.runtimeLookaheadSet
                                    ParseAction.EMBED -> lg2.runtimeState.runtimeLookaheadSet
                                    ParseAction.HEIGHT -> prev2.runtimeState.runtimeLookaheadSet
                                    ParseAction.GRAFT -> prev2.runtimeState.runtimeLookaheadSet
                                    ParseAction.GOAL -> prev2.runtimeState.runtimeLookaheadSet
                                }
                                val runtimeGuardPassed = tr2.runtimeGuard.execute(prev2.numNonSkipChildren)
                                errorPairs(rp, input, lg, tr2, possibleEndOfText, rtLh, runtimeGuardPassed)
                            }.flatten()
                        }
                    }
                }
            }
        }.toSet()
        return r
    }

    private fun errorPairs(
        rp: RuntimeParser,
        input: InputFromString,
        lg: GrowingNodeIndex,
        transition: Transition,
        possibleEndOfText: Set<LookaheadSet>,
        runtimeLookahead: Set<LookaheadSet>,
        runtimeGuardPassed: Boolean
    ): Set<Pair<Int, Set<RuntimeRule>>>? {
        return when (transition.action) {
            ParseAction.EMBED -> error("Internal Error: should never happen")
            ParseAction.GOAL -> null
            ParseAction.WIDTH -> {
                // try grab 'to' token, if nothing then that is error else lookahead is error
                val l = input.findOrTryCreateLeaf(transition.to.firstRule, lg.nextInputPosition)
                when (l) {
                    null -> {
                        val expected = transition.to.runtimeRules.filter { it.isEmptyTerminal.not() }
                        if (expected.isEmpty()) {
                            null
                        } else {
                            setOf(Pair(lg.nextInputPosition, expected.toSet()))
                        }
                    }

                    else -> {
                        val trlh = transition.lookahead.map { it.guard }.reduce { acc, e -> acc.union(rp.stateSet, e) } //TODO:reduce to 1 in SM
                        val skipData = rp.parseSkipIfAny(l.nextInputPosition, runtimeLookahead, trlh, possibleEndOfText, RuntimeParser.normalArgs)
                        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: l.nextInputPosition

                        val expected = transition.lookahead
                            .flatMap { lh -> possibleEndOfText.flatMap { eot -> runtimeLookahead.flatMap { rt -> lh.guard.resolve(eot, rt).fullContent } } }
                            .filter { it.isEmptyTerminal.not() }
                        if (expected.isEmpty()) {
                            null
                        } else {
                            setOf(Pair(nextInputPositionAfterSkip, expected.toSet()))
                        }
                    }
                }
            }

            else -> when {
                runtimeGuardPassed -> {
                    val expected =
                        transition.lookahead.flatMap { lh -> possibleEndOfText.flatMap { eot -> runtimeLookahead.flatMap { rt -> lh.guard.resolve(eot, rt).fullContent } } }.toSet()
                    val pos = lg.nextInputPosition
                    setOf(Pair(pos, expected))
                }

                else -> {
                    val expected =
                        transition.lookahead.flatMap { lh -> possibleEndOfText.flatMap { eot -> runtimeLookahead.flatMap { rt -> lh.guard.resolve(eot, rt).fullContent } } }.toSet()
                    val pos = lg.startPosition
                    setOf(Pair(pos, expected))
                }
            }
        }
    }

    private fun findNextExpectedAfterError3(
        input: InputFromString,
        failedParseReasons: List<FailedParseReason>,
        automatonKind: AutomatonKind,
        stateSet: ParserStateSet
    ): Pair<InputLocation, Set<RuntimeRule>> {
        val max = failedParseReasons.maxOf { it.position }
        val maxReasons = failedParseReasons.filter { it.position == max }
        val x = maxReasons.map { fr ->
            when (fr) {
                is FailedParseReasonWidthTo -> Pair(input.locationFor(fr.position, 0), setOf(fr.transition.to.firstRule))
                is FailedParseReasonGraftRTG -> {
                    val exp = fr.transition.runtimeGuard.expectedWhenFailed(fr.prevNumNonSkipChildren)
                    Pair(input.locationFor(fr.position, 0), exp)
                }

                is FailedParseReasonLookahead -> {
                    val expected: Set<RuntimeRule> = fr.possibleEndOfText.flatMap { eot ->
                        fr.runtimeLhs.flatMap { rt ->
                            fr.transition.lookahead.flatMap { lh ->
                                lh.guard.resolve(eot, rt).fullContent
                            }
                        }
                    }.toSet()
                    val terms = stateSet.usedTerminalRules
                    val embeddedSkipTerms = stateSet.embeddedRuntimeRuleSet.flatMap { it.skipTerminals }.toSet()
                    val exp = expected.minus(embeddedSkipTerms.minus(terms))
                    Pair(input.locationFor(fr.position, 0), exp)
                }

                is FailedParseReasonEmbedded -> {
                    // Outer skip terms are part of the 'possibleEndOfText' and thus could be in the expected terms
                    // if these skip terms are not part of the embedded 'normal' terms...remove them
                    val embeddedRhs = fr.transition.to.runtimeRules.first().rhs as RuntimeRuleRhsEmbedded // should only ever be one
                    val embeddedStateSet = embeddedRhs.embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, automatonKind)
                    val x = findNextExpectedAfterError3(input, fr.embededFailedParseReasons, automatonKind, embeddedStateSet)
                    val embeddedRuntimeRuleSet = embeddedRhs.embeddedRuntimeRuleSet
                    val embeddedTerms = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, automatonKind).usedTerminalRules
                    val skipTerms = runtimeRuleSet.skipParserStateSet?.usedTerminalRules ?: emptySet()
                    val exp = x.second.minus(skipTerms.minus(embeddedTerms))
                    Pair(x.first, exp)
                }
            }
        }
        val y = x.groupBy { it.first.position }
        val m = y.map {
            val m = it.value.flatMap { it.second }.toSet()
            Pair(input.locationFor(it.key, 0), m)
        }
        return m.first()
    }

    /*
    private fun findNextExpected(
        rp: RuntimeParser,
        gns: List<ParseGraph.Companion.ToProcessTriple>,
        possibleEndOfText: Set<LookaheadSet>
    ): Set<RuntimeRule> {
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf

        val matches = gns.toMutableList()
        // try grow last leaf with no lookahead
        /*
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
*/
        //val nextExpected = matches
        //    .filter { it.canGrowWidth }
        //    .flatMap { it.nextExpectedItems }
        //    .toSet()
        //return nextExpected
        val graph = rp.graph
        val lastGrown = rp.lastDropped.flatMap { it.triples }
        rp.resetGraphToLastGrown(lastGrown)
        rp.grow3(possibleEndOfText, RuntimeParser.normalArgs)
        matches.addAll(graph.peekAllNextToProcess())
        val trans_lh_pairs = matches.flatMap { (gn, previous, remainingHead) ->
            when {
                gn.state.isAtEnd && gn.state.isGoal -> emptySet()
                else -> {
                    val prevPrev = remainingHead?.runtimeState ?: RuntimeState(rp.stateSet.startState, setOf(LookaheadSet.EMPTY))
                    val prev = previous?.runtimeState ?: RuntimeState(rp.stateSet.startState, setOf(LookaheadSet.EMPTY))
                    val trans = gn.runtimeState.transitions(prevPrev.state, prev.state).toSet()
                    val rtLh = gn.runtimeState.runtimeLookaheadSet
                    trans.flatMap { tr ->
                        val pairs = possibleEndOfText.flatMap { eot ->
                            rtLh.map { rt ->
                                Pair(tr, tr.lookahead.flatMap { it.guard.resolve(eot, rt).content })
                            }
                        }.toSet()
                        pairs
                    }
                }
            }
        }.toSet()

        val result = trans_lh_pairs.flatMap { tr_lh ->
            val tr = tr_lh.first
            val lh = tr_lh.second
            when (tr.action) {
                ParseAction.GOAL -> lh
                ParseAction.WIDTH -> tr.to.runtimeRulesSet
                ParseAction.GRAFT -> lh
                ParseAction.HEIGHT -> lh
                ParseAction.EMBED -> TODO()
            }
        }.toSet()
        return result
    }
*/
    override fun expectedAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule> {
        val usedText = inputText.substring(0, position)
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, usedText)
        val rp = createRuntimeParser(goalRuleName, input, automatonKind)
        this.runtimeParser = rp

        val possibleEndOfText = setOf(LookaheadSet.EOT)
        rp.start(0, possibleEndOfText)
        var seasons = 1

        var lastToTryWidth = emptyList<GrowingNodeIndex>()
        while (rp.graph.canGrow && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            rp.grow3(possibleEndOfText, RuntimeParser.normalArgs) //TODO: maybe no need to build tree!
            lastToTryWidth = rp.lastToTryWidthTrans
            seasons++
        }
        /*
        return rp.nextExpected.flatMap {
            when(it) {
                is RuntimeRule -> listOf(it)
                is Set<*> -> it as Set<RuntimeRule>
                is List<*> -> it as List<RuntimeRule>
                is LookaheadSet -> it.fullContent
                else -> error("Internl Error: Not handled - $it")
            }
        }.toSet()
         */
        //val nextExpected = this.findNextExpected(rp, matches, possibleEndOfText)
        return if (rp.failedReasons.isEmpty()) {
            emptySet()
        } else {
            //val nextExpected = this.findNextExpectedAfterError2(rp, lastToTryWidth, input, possibleEndOfText) //this possibly modifies rp and hence may change the longestLastGrown
            val nextExpected = this.findNextExpectedAfterError3(input, rp.failedReasons, rp.stateSet.automatonKind, rp.stateSet)
            nextExpected.second
        }
    }

    override fun expectedTerminalsAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule> {
        return this.expectedAt(goalRuleName, inputText, position, automatonKind)
            .flatMap {
                when {
                    it.isTerminal -> listOf(it)
                    //RuntimeRuleKind.NON_TERMINAL -> this.runtimeRuleSet.firstTerminals[it.ruleNumber]
                    else -> TODO()
                }
            }
            .toSet()
    }
}