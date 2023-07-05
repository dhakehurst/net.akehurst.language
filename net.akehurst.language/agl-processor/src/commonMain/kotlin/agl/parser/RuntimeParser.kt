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

import net.akehurst.language.agl.api.messages.Message
import net.akehurst.language.agl.automaton.*
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.runtime.graph.CompleteNodeIndex
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.TreeDataComplete
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimePreferenceRule
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserTerminatedException
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.collections.lazyMutableMapNonNull
import kotlin.math.max

internal class RuntimeParser(
    val stateSet: ParserStateSet,
    val skipStateSet: ParserStateSet?, // null if this is a skipParser
    val userGoalRule: RuntimeRule,
    private val input: InputFromString,
    private val _issues: IssueHolder
) {
    companion object {
        val defaultStartLocation = InputLocation(0, 0, 1, 0)

        data class GrowArgs(
            val buildTree: Boolean,
            val noLookahead: Boolean,
            val heightGraftOnly: Boolean,
            val nonEmptyWidthOnly: Boolean,
            val reportErrors: Boolean
        )

        //val normalArgs = GrowArgs(true, false, false, false, true)
        val forPossErrors = GrowArgs(false, true, false, true, true)
        val heightGraftOnly = GrowArgs(false, true, true, false, false)
    }

    val graph = ParseGraph(input, this.stateSet.number)

    //var lastGrown: Set<ParseGraph.Companion.ToProcessTriple> = mutableSetOf()
    val canGrow: Boolean get() = this.graph.canGrow

    var lastToGrow = listOf<ParseGraph.Companion.ToProcessTriple>()

    //val lastDropped = mutableSetOf<ParseGraph.Companion.NextToProcess>()
    //val embeddedLastDropped = mutableMapOf<Transition, Set<ParseGraph.Companion.NextToProcess>>()
    //val lastToTryWidthTrans = mutableListOf<GrowingNodeIndex>()
    val failedReasons = mutableListOf<FailedParseReason>()

    private var interruptedMessage: String? = null

    private val _skip_cache = mutableMapOf<Pair<Int, Set<LookaheadSet>>, TreeDataComplete<CompleteNodeIndex>?>()

    // must use a different instance of Input, so it can be reset, reset clears cached leaves. //TODO: check this
    private val skipParser = skipStateSet?.let {
        if (this.stateSet.preBuilt) this.skipStateSet.build()
        RuntimeParser(it, null, skipStateSet.userGoalRule, InputFromString(skipStateSet.usedTerminalRules.size, this.input.text), _issues)
    }

    fun reset() {
        this.graph.reset()
    }

    fun buildSkipParser() {
        skipParser?.stateSet?.build()
    }

    fun start(startPosition: Int, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs) {
        val gState = stateSet.startState
        val initialSkipData = if (this.stateSet.isSkip) {
            null
        } else {
            //val skipLhsp = gState.rulePositions.map { this.stateSet.firstOf.expectedAt(it, LookaheadSetPart.EOT) }.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e) }
            val skipLhsp = gState.rulePositions.flatMap {
                possibleEndOfText.map { eot ->
                    this.stateSet.firstOf.expectedAt(it, eot.part)
                }
            }.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e) }
            val endOfSkipLookaheadSet = this.stateSet.createLookaheadSet(skipLhsp)
            this.tryParseSkipUntilNone(setOf(endOfSkipLookaheadSet), startPosition, parseArgs) //TODO: think this might allow some wrong things, might be a better way
        }
        val runtimeLookahead = possibleEndOfText//setOf(LookaheadSet.EOT)
        val gn = this.graph.start(gState, startPosition, runtimeLookahead, initialSkipData) //TODO: remove LH
//        this.addToLastGrown(gn)
    }

    fun interrupt(message: String) {
        this.interruptedMessage = message
    }

    fun checkForTerminationRequest() {
        val m = this.interruptedMessage
        if (null == m) {
            //do nothing
        } else {
            throw ParserTerminatedException(m)
        }
    }

    fun resetGraphToLastGrown(lastGrown: List<ParseGraph.Companion.ToProcessTriple>) {

        for (trip in lastGrown) {
            this.graph.pushback(trip)
        }

        /*
        this.graph._gss = this._lastGss
        for (r in this.graph._gss.roots) {
            this.graph._growingHeadHeap[r] = GrowingNode(this.graph, r)
        }
        */

        // this.graph.replaceHeads(this.lastGrown)
        //val maxLastGrown = this.lastGrown.maxOf { it.nextInputPosition }
        //this.lastGrown.filter { it.nextInputPosition==maxLastGrown }.forEach { gn ->
        //     val prv = this.grownInLastPassPrevious[gn]!!
        //     if (prv.isEmpty()) {
        //         this.graph._gss.root(gn.index)
        //         this.graph._growingHeadHeap[gn.index] = gn
        //     } else {
        //         prv.forEach { prev ->
        //             this.graph.addGrowingHead(prev, gn)
        //         }
        //     }
        // }
    }

    var debugCount = 0
    fun debugOutput() {
        fun GrowingNodeIndex.asString() = "(s${runtimeState.state.number.value},${startPosition}-${nextInputPosition}{${numNonSkipChildren}}${
            runtimeState.state.rulePositions.joinToString()
        }${
            runtimeState.runtimeLookaheadSet.joinToString(
                prefix = "[",
                postfix = "]",
                separator = "|"
            ) { it.toString() }
        })"

        fun GrowingNodeIndex.chains(): List<List<GrowingNodeIndex>> {
            val prevs = this@RuntimeParser.graph.previousOf(this)
            return when {
                prevs.isEmpty() -> listOf(listOf(this))
                else -> prevs.flatMap { prev ->
                    val chains = prev.chains()
                    val r: List<List<GrowingNodeIndex>> = chains.map { chain ->
                        listOf(this) + chain
                    }
                    r
                }
            }
        }
        if (true) {
            if (Debug.OUTPUT_RUNTIME) debug(Debug.IndentDelta.NONE) { "--- ${debugCount++} --------------------------------------------------------" }
            this.graph._gss.heads.forEach {
                val chains = it.chains()
                chains.forEach { chain ->
                    val str = chain.joinToString(separator = "-->") {
                        it.asString()
                    }
                    if (Debug.OUTPUT_RUNTIME) debug(Debug.IndentDelta.NONE) { str }
                }
            }
            if (Debug.OUTPUT_RUNTIME) debug(Debug.IndentDelta.NONE) { "" }
        }
    }

    fun grow3(possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Int {
        this.failedReasons.clear()
        //this.lastDropped.clear()
        this.lastToGrow = this.graph.peekAllNextToProcess()

        var steps = 0
        val progressSteps = lazyMutableMapNonNull<GrowingNodeIndex, Int> { 0 }
        val doneEmpties = mutableSetOf<Pair<ParserState, Set<GrowingNodeIndex>>>()

        val currentNextInputPosition = this.graph.nextHeadNextInputPosition
        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentNextInputPosition) {
            checkForTerminationRequest()
            val graph = this.graph //TODO: remove..for debug only
            if (Debug.OUTPUT_RUNTIME) {
                println("$steps --------------------------------------")
                println(graph)
            }

            val head = this.graph.peekNextHead

            //TODO: move empty checking stuff to doWidth
            if (head.isEmptyMatch && doneEmpties.contains(Pair(head.state, graph.previousOf(head)))) {
                //don't do it again
                val prev = this.graph.dropStackWithHead(head)
                //prev.forEach { this.graph._gss._growingHeadHeap[it] = it } //TODO: should not do this here
            } else {
                if (head.isEmptyMatch) {
                    doneEmpties.add(Pair(head.state, graph.previousOf(head)))
                }
                growHead(head, possibleEndOfText, growArgs)
                steps++
            }

            val progStep = progressSteps[head] //FIXME: maybe slow - is there a better way?
            if (progStep > 1000) { //FIXME: make part of config
                this.interrupt(Message.PARSER_WONT_STOP) //TODO: include why - which node/rule
            } else {
                progressSteps[head] = progStep + 1
            }
        }
        return steps
    }

    private fun growHead(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Boolean {
        return when {
            head.isGoal -> growGoal(head, possibleEndOfText, growArgs)
            head.isComplete -> growComplete(head, possibleEndOfText, growArgs)
            else -> growIncomplete(head, possibleEndOfText, growArgs)
        }
    }

    private fun growGoal(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Boolean {
        return if (head.isComplete) {
            graph.recordGoal(head)
            graph.dropStackWithHead(head)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $head" }
            false
        } else {
            var grown = false
            val transTaken = mutableSetOf<Transition>()
            val transitions = head.runtimeState.transitionsGoal(stateSet.startState)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${transitions.joinToString(separator = "\n") { "  $it" }}" }
            for (tr in transitions) {
                val b = when (tr.action) {
                    ParseAction.WIDTH -> doWidth(head, tr, possibleEndOfText, growArgs)
                    ParseAction.EMBED -> doEmbedded(head, tr, possibleEndOfText, growArgs)
                    else -> error("Internal Error: should only have WIDTH or EMBED transitions here")
                }
                if (b) transTaken.add(tr)
                grown = grown || b
            }
            if (growArgs.reportErrors && transTaken.size > 1) ambiguity(head, transTaken, possibleEndOfText)
            if (grown.not()) doNoTransitionsTaken(head)
            grown
        }
    }

    private fun growIncomplete(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Boolean {
        var grown = false
        val prevSet = this.graph.previousOf(head)
        val transTaken = mutableSetOf<Transition>()
        for (previous in prevSet) {
            val transitions = head.runtimeState.transitionsInComplete(previous.state)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${transitions.joinToString(separator = "\n") { "  $it" }}" }
            for (tr in transitions) {
                val b = when (tr.action) {
                    ParseAction.WIDTH -> doWidth(head, tr, possibleEndOfText, growArgs)
                    ParseAction.EMBED -> doEmbedded(head, tr, possibleEndOfText, growArgs)
                    else -> error("Internal Error: should only have WIDTH or EMBED transitions here")
                }
                if (b) transTaken.add(tr)
                grown = grown || b
            }
        }
        if (growArgs.reportErrors && transTaken.size > 1) ambiguity(head, transTaken, possibleEndOfText)
        if (grown.not()) doNoTransitionsTaken(head)
        return grown
    }

    private fun growComplete(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Boolean {
        var headGrownHeight = false
        var headGrownGraft = false
        val prevSet = this.graph.previousOf(head)
        val dropPrevs = mutableMapOf<GrowingNodeIndex, Boolean>()
        for (previous in prevSet) {
            var prevGrownHeight = false
            var prevGrownGraft = false
            val prevPrevSet = this.graph.previousOf(previous)
            if (prevPrevSet.isEmpty()) {
                val (h, g) = growComplete2(head, previous, null, possibleEndOfText, growArgs)
                prevGrownHeight = prevGrownHeight || h
                prevGrownGraft = prevGrownGraft || g
            } else {
                for (prevPrev in prevPrevSet) {
                    val (h, g) = growComplete2(head, previous, prevPrev, possibleEndOfText, growArgs)
                    prevGrownHeight = prevGrownHeight || h
                    prevGrownGraft = prevGrownGraft || g
                }
            }
            if (prevGrownHeight.not()) {
                //have to drop previous after dropping head, so record here for later drop
                // only drop previous if we did not grow with HEIGHT - HEIGHT keeps same previous
                // true indicate drop whole stack - i.e. not GRAFT or HEIGHT
                // false just drop the previous - GRAFT was done
                dropPrevs[previous] = prevGrownGraft
            }
            headGrownHeight = headGrownHeight || prevGrownHeight
            headGrownGraft = headGrownGraft || prevGrownGraft
        }

        when {
            headGrownHeight.not() && headGrownGraft.not() -> doNoTransitionsTaken(head)
            headGrownHeight && headGrownGraft.not() -> {
                graph.dropStackWithHead(head)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $head" }
            }

            headGrownHeight.not() && headGrownGraft -> {
                graph.dropStackWithHead(head)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $head" }
            }

            headGrownHeight && headGrownGraft -> {
                graph.dropStackWithHead(head)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $head" }
            }
        }

        dropPrevs.forEach {
            if (it.value) {
                graph.dropStackWithHead(it.key)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $it" }
            } else {
                doNoTransitionsTaken(it.key)
            }
        }

        return headGrownHeight || headGrownGraft
    }

    private fun matchedLookahead(position: Int, lookahead: Set<Lookahead>, possibleEndOfText: Set<LookaheadSet>, runtimeLhs: Set<LookaheadSet>) =
        when {
//            possibleEndOfText.size > 1 -> TODO()
//            runtimeLhs.size > 1 -> TODO()
            else -> {
                val lh: LookaheadSetPart = lookahead.map { it.guard.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                val eot = possibleEndOfText.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                val rt: LookaheadSetPart = runtimeLhs.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                //possibleEndOfText.flatMap { eot ->
                //    runtimeLhs.map { rt ->
                val lookingAt: Boolean = this.graph.isLookingAt(lh, eot, rt, position)
                val resolved = lh.resolve(eot, rt)
                Pair(lookingAt, resolved)
                //     }
                // }
            }
        }

    private fun growComplete2(
        head: GrowingNodeIndex,
        previous: GrowingNodeIndex,
        prevPrev: GrowingNodeIndex?,
        possibleEndOfText: Set<LookaheadSet>,
        parseArgs: GrowArgs
    ): Pair<Boolean, Boolean> {
        var grownHeight = false
        var grownGraft = false
        val transitions = head.runtimeState.transitionsComplete(previous.state, prevPrev?.state ?: stateSet.startState)
        val transWithValidLookahead = when {
            parseArgs.noLookahead -> transitions.map { Pair(it, LookaheadSetPart.ANY) }
            else -> transitions.mapNotNull {
                val lh = matchedLookahead(head.nextInputPositionAfterSkip, it.lookahead, possibleEndOfText, previous.runtimeState.runtimeLookaheadSet)
                when {
                    lh.first -> Pair(it, lh.second)
                    else -> null
                }
            }
        }
        transitions.minus(transWithValidLookahead.map { it.first }).forEach {
            when (it.action) {
                ParseAction.HEIGHT -> recordFailedHeightLh(parseArgs, head.nextInputPositionAfterSkip, it, previous.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                ParseAction.GRAFT -> recordFailedGraftLH(parseArgs, head.nextInputPositionAfterSkip, it, previous.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                ParseAction.GOAL -> recordFailedGraftLH(parseArgs, head.nextInputPositionAfterSkip, it, previous.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                else -> error("Internal Error: should never happen")
            }
        }
        //val vlhg = transWithValidLookahead.groupBy { it.first.to }
        //val ss = mutableMapOf<LookaheadSetPart, List<Pair<Transition,LookaheadSetPart>>>()
        //for (tgtGrp in vlhg) {
        //    for (x in tgtGrp.value) {
        //        for(x2 in ss.keys) {
        //            if (x2.fullContent.containsAll(x.second.fullContent)) {
        //            }
        //        }
        //    }
        //}
        val trans2 = resolvePrecedence(transWithValidLookahead, head)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${trans2.joinToString(separator = "\n") { "  $it" }}" }
        //val grouped = transitions.groupBy { it.to.runtimeRulesSet }
        if (parseArgs.reportErrors && trans2.size > 1) {
            ambiguity(head, trans2, possibleEndOfText)
        }
        val grouped = trans2.groupBy { it.to.runtimeRulesAsSet }
        for (grp in grouped) {
            when {
                1 == grp.value.size -> {
                    val tr = grp.value[0]
                    when (tr.action) {
                        ParseAction.HEIGHT -> {
                            val b = doHeight(head, previous, tr, possibleEndOfText, parseArgs)
                            grownHeight = grownHeight || b
                        }

                        ParseAction.GRAFT -> {
                            val b = doGraft(head, previous, prevPrev, tr, possibleEndOfText, parseArgs)
                            grownGraft = grownGraft || b
                        }

                        ParseAction.GOAL -> {
                            val b = doGoal(head, previous, tr, possibleEndOfText, parseArgs)
                            grownGraft = grownGraft || b
                        }

                        else -> error("Internal Error: should only have GOAL, HEIGHT or GRAFT transitions here")
                    }
                }

                else -> {
                    val trgs = grp.value.filter { it.action == ParseAction.GRAFT }
                        // if multiple GRAFT trans to same rule, prefer left most target
                        .sortedWith(Comparator { t1, t2 ->
                            val p1 = t1.to.rulePositions.first().position
                            val p2 = t2.to.rulePositions.first().position
                            when {
                                p1 == p2 -> 0
                                RulePosition.END_OF_RULE == p1 -> 1
                                RulePosition.END_OF_RULE == p2 -> -1
                                p1 > p2 -> 1
                                p1 < p2 -> -1
                                else -> 0// should never happen !
                            }
                        })
                    val trhs = grp.value.filter { it.action == ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            val b = doGraft(head, previous, prevPrev, trgs[i], possibleEndOfText, parseArgs)
                            grownGraft = grownGraft || b
                            doneIt = doneIt || b
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            val b = doHeight(head, previous, trhs[i], possibleEndOfText, parseArgs)
                            grownHeight = grownHeight || b
                            doneIt = doneIt || b
                            ++i
                        }
                    } else {
                        for (tr in grp.value) {
                            when (tr.action) {
                                ParseAction.HEIGHT -> {
                                    val b = doHeight(head, previous, tr, possibleEndOfText, parseArgs)
                                    grownHeight = grownHeight || b
                                }

                                ParseAction.GRAFT -> {
                                    val b = doGraft(head, previous, prevPrev, tr, possibleEndOfText, parseArgs)
                                    grownGraft = grownGraft || b
                                }

                                ParseAction.GOAL -> {
                                    val b = doGoal(head, previous, tr, possibleEndOfText, parseArgs)
                                    grownGraft = grownGraft || b
                                }

                                else -> error("Internal Error: should only have HEIGHT or GRAFT or GOAL transitions here")
                            }
                        }
                    }
                }
            }
        }

        return Pair(grownHeight, grownGraft)
    }

    private fun resolvePrecedence(transitions: List<Pair<Transition, LookaheadSetPart>>, head: GrowingNodeIndex): List<Transition> {
        return when {
            2 > transitions.size -> transitions.map { it.first }
            else -> {
                val precRules = this.stateSet.precedenceRulesFor(head.state)
                when {
                    null == precRules -> {
                        //if no explicit preference, prefer GRAFT over HEIGHT for same rule
                        val toGrp = transitions.groupBy { it.first.to.runtimeRulesAsSet }
                        toGrp.flatMap {
                            val actGrp = it.value.groupBy { it.first.action }
                            when {
                                2 > actGrp.size -> it.value.map { it.first }
                                else -> {
                                    val grafts = actGrp[ParseAction.GRAFT]!!
                                    // if multiple GRAFT trans to same rule, prefer left most target
                                    grafts.sortedWith(Comparator { t1, t2 ->
                                        val p1 = t1.first.to.rulePositions.first().position
                                        val p2 = t2.first.to.rulePositions.first().position
                                        when {
                                            p1 == p2 -> 0
                                            RulePosition.END_OF_RULE == p1 -> 1
                                            RulePosition.END_OF_RULE == p2 -> -1
                                            p1 > p2 -> 1
                                            p1 < p2 -> -1
                                            else -> 0// should never happen !
                                        }
                                    })
                                    listOf(grafts.last().first)
                                }
                            }
                        }
                    }

                    2 > transitions.size -> transitions.map { it.first }
                    else -> {
                        val precedence = transitions.flatMap { (tr, lh) ->
                            val lhf = lh//.map { it.second }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                            val prec = precRules.precedenceFor(tr.to.rulePositions, lhf)
                            prec.map { Pair(tr, it) }
                        }
                        val max = precedence.map { it.second.precedence }.maxOrNull() ?: 0
                        val maxPrec = precedence.filter { max == it.second.precedence }
                        val byTarget = maxPrec.groupBy { Pair(it.first.to.runtimeRules, it.second.associativity) }
                        val assoc = byTarget.flatMap {
                            when (it.key.second) {
                                RuntimePreferenceRule.Assoc.NONE -> it.value
                                RuntimePreferenceRule.Assoc.LEFT -> when {
                                    it.key.first[0].isList -> {
                                        val grafts = it.value.filter { it.first.action == ParseAction.GRAFT }
                                        val left = grafts.filter { it.first.to.rulePositions[0].isAtEnd.not() }
                                        when (left.isEmpty()) {
                                            true -> grafts
                                            false -> left
                                        }
                                    }

                                    else -> it.value.filter { it.first.action == ParseAction.GRAFT }
                                }

                                RuntimePreferenceRule.Assoc.RIGHT -> when {
                                    it.key.first[0].isList -> {
                                        val heights = it.value.filter { it.first.action == ParseAction.HEIGHT }
                                        val right = heights.filter { it.first.to.rulePositions[0].isAtEnd.not() }
                                        when (right.isEmpty()) {
                                            true -> heights
                                            false -> right
                                        }
                                    }

                                    else -> it.value.filter { it.first.action == ParseAction.HEIGHT }
                                }
                            }
                        }
                        when (assoc.size) {
                            0 -> maxPrec.map { it.first }
                            else -> assoc.map { it.first }
                        }
                    }
                }
            }
        }
    }

    fun growNonEmptyWidthForError(possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Set<GrowingNodeIndex> {
        val lg = mutableSetOf<GrowingNodeIndex>()
        val currentStartPosition = this.graph.nextHeadNextInputPosition
        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentStartPosition) {
            //while ((this.canGrow && this.graph.goals.isEmpty())) {
            //val nextToProcess = this.graph.nextToProcess()
            val head = this.graph.peekNextHead
            checkForTerminationRequest()
            // val grown = this.growNormal(nextToProcess, possibleEndOfText, growArgs)
            val grown = growHead(head, possibleEndOfText, growArgs)
            if (grown.not()) {
                lg.add(head)
                doNoTransitionsTaken(head)
            }
        }
        return lg
    }

    fun growHeightOrGraftForError(possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Set<ParseGraph.Companion.ToProcessTriple> {
        val lg = mutableSetOf<ParseGraph.Companion.ToProcessTriple>()
        val currentStartPosition = this.graph.nextHeadNextInputPosition
        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentStartPosition) {
            //while ((this.canGrow && this.graph.goals.isEmpty())) {
            //val nextToProcess = this.graph.nextToProcess()
            val head = this.graph.peekNextHead
            checkForTerminationRequest()
            //val grown = this.growNormal(nextToProcess, possibleEndOfText, growArgs)
            val grown = growHead(head, possibleEndOfText, growArgs)
            if (grown.not()) {
                lg.addAll(this.graph.peekTripleFor(head))
                doNoTransitionsTaken(head)
            }
        }
        return lg
    }

    /*
    /*
        private fun useGoal(lt: SPPTNode): SPPTNode {
            //FIXME: use GrowingChildren
            // need to re-write top of the tree so that any initial skip nodes come under the userGoal node
            val goal = lt as SPPTBranchFromInputAndGrownChildren

            val goalRpId = setOf(RuleOption(this.userGoalRule, 0))
            val goalFirstChildren = goal.grownChildrenAlternatives.values.first()
            val userGoalNode = if (goalFirstChildren.hasSkipAtStart) {
                //has skip at start
                val skipNodes = goalFirstChildren.firstChild(this.stateSet.startState.rulePositionIdentity)!!.children
                val ugn = goalFirstChildren.firstNonSkipChild(goalRpId)!!.children[0] as SPPTBranchFromInputAndGrownChildren
                val startPosition = skipNodes[0].startPosition
                val nugn = SPPTBranchFromInputAndGrownChildren(this.input, ugn.runtimeRule, ugn.option, startPosition, ugn.nextInputPosition, ugn.priority)
                ugn.grownChildrenAlternatives.values.forEach {
                    val nc = GrowingChildren().appendSkipIfNotEmpty(emptySet(), skipNodes)
                    //nc._firstChild!!.nextChild = it.firstChild
                    //nc._lastChild = it.lastChild
                    nc.concatenate(it)
                    nugn.grownChildrenAlternatives[ugn.option] = nc
                }
                nugn
            } else {

                goalFirstChildren.firstChild(goalRpId)!!.children[0]
            }
            return userGoalNode
        }
    */
    private fun growNode(toProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs) {
        val rr = toProcess.growingNode.state.firstRule//FIXME: is there only one?
        when {
            rr.isGoal -> when {
                toProcess.growingNode.state.isAtEnd -> graph.recordGoal(toProcess.growingNode)
                else -> this.growGoalNode(toProcess, possibleEndOfText, growArgs)
            }

            else -> this.growNormal(toProcess, possibleEndOfText, growArgs)
        }
    }

    private fun growGoalNode(
        nextToProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs
    ) {
        //no previous, so gn must be the Goal node
        val toProcess = ParseGraph.Companion.ToProcessTriple(nextToProcess.growingNode, null, null)
        val transitions = toProcess.growingNode.runtimeState.transitions(stateSet.startState, stateSet.startState)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${transitions.joinToString(separator = "\n") { "  $it" }}" }
        var grown = false
        for (it in transitions) {
            val g = when (it.action) {
                ParseAction.GOAL -> error("Should never happen")
                ParseAction.WIDTH -> doWidth(toProcess, it, possibleEndOfText, growArgs)
                ParseAction.HEIGHT -> error("Should never happen")
                ParseAction.GRAFT -> error("Should never happen")
                ParseAction.EMBED -> doEmbedded(toProcess, it, possibleEndOfText, growArgs)
            }
            grown = grown || g
        }
        if (grown) {
            // must have grown
        } else {
            lastDropped.add(nextToProcess)
            val hd = toProcess.remainingHead
            val dd = nextToProcess.growingNode
            hd?.let { graph.dropHead(hd) }
            graph.dropData(dd)
            if (Debug.OUTPUT_RUNTIME) {
                Debug.debug(Debug.IndentDelta.NONE) { "Dropped Head: $hd" }
                Debug.debug(Debug.IndentDelta.NONE) { "Dropped Data: $dd" }
            }
        }
    }

    private fun growNormal(
        nextToProcess: ParseGraph.Companion.NextToProcess,
        possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs
    ): Boolean {
        val toDropData = mutableSetOf<GrowingNodeIndex>()
        //val toDropHead = mutableSetOf<GrowingNodeIndex>()
        var grown = false
        for (toProcess in nextToProcess.triples) {
            val g = this.growWithPrev(toProcess, possibleEndOfText, growArgs)
            if (g) {
                grown = true
            } else {
                //toDropHead.addIfNotNull(toProcess.remainingHead)
                //toDropData.addIfNotNull(toProcess.previous)
                //toDropData.addIfNotNull(toProcess.remainingHead)
            }
        }
        if (grown) {
            // growingHead must have grown with one of the previous
        } else {
            //nothing was grown, so drop data for growingHead also
            toDropData.add(nextToProcess.growingNode)
            lastDropped.add(nextToProcess)
            //TODO: maybe also drop other things!
        }
        //for (hd in toDropHead) {
        //    graph.dropHead(hd)
        //    if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Head: $hd" }
        //}
        for (dd in toDropData) {
            graph.dropData(dd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Data: $dd" }
        }
        return grown
    }

    private fun growWithPrev(
        toProcess: ParseGraph.Companion.ToProcessTriple, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs
    ): Boolean {
        var grown = false
        val prevPrev = toProcess.remainingHead?.runtimeState?.state ?: stateSet.startState
        val prev = toProcess.previous?.runtimeState?.state ?: stateSet.startState
        val transitions = toProcess.growingNode.runtimeState.transitions(prevPrev, prev)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${transitions.joinToString(separator = "\n") { "  $it" }}" }
        //TODO: do we need to do something here? due to filtered out trans from the list
        val grouped = transitions.groupBy { it.to.runtimeRulesSet }
        for (it in grouped) {
            when {
                1 == it.value.size -> {
                    val tr = it.value[0]
                    val b = doAction(tr, toProcess, possibleEndOfText, growArgs)
                    grown = grown || b
                }

                else -> {
                    val trgs = it.value.filter { it.action == ParseAction.GRAFT }
                        // if multiple GRAFT trans to same rule, prefer left most target
                        .sortedWith(Comparator { t1, t2 ->
                            val p1 = t1.to.rulePositions.first().position
                            val p2 = t2.to.rulePositions.first().position
                            when {
                                p1 == p2 -> 0
                                RulePosition.END_OF_RULE == p1 -> 1
                                RulePosition.END_OF_RULE == p2 -> -1
                                p1 > p2 -> 1
                                p1 < p2 -> -1
                                else -> 0// should never happen !
                            }
                        })
                    val trhs = it.value.filter { it.action == ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            val b = doGraft(toProcess, trgs[i], possibleEndOfText, growArgs)
                            doneIt = doneIt || b
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            val b = doHeight(toProcess, trhs[i], possibleEndOfText, growArgs)
                            doneIt = doneIt || b
                            ++i
                        }
                        grown = grown || doneIt
                    } else {
                        for (tr in it.value) {
                            val b = doAction(tr, toProcess, possibleEndOfText, growArgs)
                            grown = grown || b
                        }
                    }
                }
            }
        }
        return grown
    }

    private fun doAction(
        tr: Transition, toProcess: ParseGraph.Companion.ToProcessTriple, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs
    ): Boolean {
        return when (tr.action) {
            ParseAction.WIDTH -> doWidth(toProcess, tr, possibleEndOfText, growArgs)
            ParseAction.EMBED -> doEmbedded(toProcess, tr, possibleEndOfText, growArgs)
            ParseAction.HEIGHT -> doHeight(toProcess, tr, possibleEndOfText, growArgs)
            ParseAction.GRAFT -> doGraft(toProcess, tr, possibleEndOfText, growArgs)
            ParseAction.GOAL -> doGoal(toProcess, tr, possibleEndOfText, growArgs)
        }
    }
*/
    private fun doNoTransitionsTaken(head: GrowingNodeIndex) {
        graph.dropStackWithHead(head)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped stack with Head: $head" }
    }

    /**
     *  GOAL
     */
    private fun doGoal(
        head: GrowingNodeIndex, previous: GrowingNodeIndex?,
        transition: Transition, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs
    ): Boolean {
        return when {
            growArgs.nonEmptyWidthOnly -> false
            // heightGraftOnly includes goal so do it
            else -> {
                if (transition.runtimeGuard.execute(previous!!.numNonSkipChildren)) {
                    val runtimeLhs = head.runtimeState.runtimeLookaheadSet //FIXME: Use previous runtimeLookahead ?
                    val lhWithMatch = transition.lookahead.filter { lh -> //TODO: should always be only one I think!!
                        possibleEndOfText.any { eot ->
                            runtimeLhs.any { rt ->
                                this.graph.isLookingAt(lh.guard, eot, rt, head.nextInputPositionAfterSkip)
                            }
                        }
                    }
                    val hasLh = lhWithMatch.isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
                    if (growArgs.noLookahead || hasLh) {
                        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head, taking: $transition" }
                        this.graph.growNextChild(
                            head = head,
                            previous = previous,
                            prevPrev = null,
                            newParentState = transition.to,
                            newParentRuntimeLookaheadSet = runtimeLhs,
                            buildSPPT = growArgs.buildTree
                        )
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    /**
     * WIDTH
     * consumes input if
     *  - 'to' can be consumed from input &
     *  - lookahead matches
     *  will consume skip after 'to' is consumed,
     *  check for lookahead after consuming skip
     * return true is grown
     */
    private fun doWidth(
        head: GrowingNodeIndex, transition: Transition, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs
    ): Boolean {
        // Use current/growing runtimeLookahead
        return when {
            parseArgs.heightGraftOnly -> false
            parseArgs.nonEmptyWidthOnly && transition.to.firstRule.isEmptyTerminal -> false
            else -> {
                val l = this.graph.input.findOrTryCreateLeaf(transition.to.firstRule, head.nextInputPosition)
                if (null != l) {
                    val lh = transition.lookahead.map { it.guard }.reduce { acc, e -> acc.union(this.stateSet, e) } //TODO:reduce to 1 in SM
                    val runtimeLhs = head.runtimeState.runtimeLookaheadSet

                    val skipData = parseSkipIfAny(l.nextInputPosition, runtimeLhs, lh, possibleEndOfText, parseArgs)
                    val nextInputPositionAfterSkip = skipData?.root?.nextInputPosition ?: l.nextInputPosition

                    val hasLh = possibleEndOfText.any { eot ->
                        runtimeLhs.any { rt ->
                            this.graph.isLookingAt(lh, eot, rt, nextInputPositionAfterSkip)
                        }
                    }
                    //if (transition.to.firstRule.isEmptyTerminal.not()) {
                    //    recordLastToTryWidthTrans(head)
                    //}
                    if (parseArgs.noLookahead || hasLh) {
                        val startPosition = l.startPosition
                        val nextInputPosition = l.nextInputPosition //TODO: should just be/pass nextInputPositionAfterSkip
                        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head, taking: $transition" }
                        this.graph.pushToStackOf(head, transition.to, setOf(LookaheadSet.EMPTY), startPosition, nextInputPosition, skipData)
                    } else {
                        val pos = if (null != skipParser && skipParser.failedReasons.isNotEmpty()) {
                            skipParser.failedReasons.maxOf { it.position }
                        } else {
                            nextInputPositionAfterSkip
                        }
                        recordFailedWidthLH(parseArgs, pos, transition, runtimeLhs, possibleEndOfText)
                        false
                    }
                } else {
                    recordFailedWidthTo(parseArgs, head.nextInputPosition, transition)
                    false
                }
            }
        }
    }

    // Use previous runtimeLookahead
    private fun doHeight(
        head: GrowingNodeIndex, previous: GrowingNodeIndex?, transition: Transition, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs
    ): Boolean {
        return when {
            parseArgs.nonEmptyWidthOnly -> false
            else -> {
                val runtimeLhs = previous!!.runtimeState.runtimeLookaheadSet
                val lhWithMatch = transition.lookahead.flatMap { lh ->
                    possibleEndOfText.flatMap { eot ->
                        runtimeLhs.mapNotNull { rt ->
                            val b = this.graph.isLookingAt(lh.guard, eot, rt, head.nextInputPositionAfterSkip) //TODO: if(transition.lookaheadGuard.includesUP) {
                            if (b) Triple(eot, rt, lh.up) else null
                        }
                    }
                }
                val hasLh = lhWithMatch.isNotEmpty()
                return if (parseArgs.noLookahead || hasLh) { //TODO: don't resolve EOT
                    val newRuntimeLhs = if (parseArgs.noLookahead) {
                        transition.lookahead.flatMap { lh ->
                            val up = lh.up
                            possibleEndOfText.flatMap { eot ->
                                runtimeLhs.map { rt ->
                                    if (up.includesRT || up.includesEOT) {
                                        //this.stateSet.createWithParent(up, rt)
                                        this.stateSet.createLookaheadSet(up.resolve(eot, rt))
                                    } else {
                                        up
                                    }
                                }
                            }
                        }.toSet()
                    } else {
                        lhWithMatch.map { t ->
                            val eot = t.first
                            val rt = t.second
                            val up = t.third
                            if (up.includesRT || up.includesEOT) {
                                //this.stateSet.createWithParent(up, rt)
                                this.stateSet.createLookaheadSet(up.resolve(eot, rt))
                            } else {
                                up
                            }
                        }.toSet()
                    }
                    if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head, taking: $transition" }
                    this.graph.createWithFirstChild(
                        head = head,
                        previous = previous,
                        parentState = transition.to,
                        parentRuntimeLookaheadSet = newRuntimeLhs,
                        buildSPPT = parseArgs.buildTree
                    )
                } else {
                    recordFailedHeightLh(parseArgs, head.nextInputPosition, transition, runtimeLhs, possibleEndOfText)
                    false
                }
            }
        }
    }

    // Use previous runtimeLookahead
    private fun doGraft(
        head: GrowingNodeIndex, previous: GrowingNodeIndex?, prevPrev: GrowingNodeIndex?,
        transition: Transition, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs
    ): Boolean {
        return when {
            parseArgs.nonEmptyWidthOnly -> false
            else -> {
                if (transition.runtimeGuard.execute(previous!!.numNonSkipChildren)) {
                    val runtimeLhs = previous.runtimeState.runtimeLookaheadSet
                    val lhWithMatch = matchedLookahead(head.nextInputPositionAfterSkip, transition.lookahead, possibleEndOfText, runtimeLhs)
                    val hasLh = lhWithMatch.first//isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
                    if (parseArgs.noLookahead || hasLh) {
                        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head, taking: $transition" }
                        this.graph.growNextChild(
                            head = head,
                            previous = previous,
                            prevPrev = prevPrev,
                            newParentState = transition.to,
                            newParentRuntimeLookaheadSet = runtimeLhs,
                            buildSPPT = parseArgs.buildTree
                        )
                    } else {
                        recordFailedGraftLH(parseArgs, head.nextInputPosition, transition, runtimeLhs, possibleEndOfText)
                        false
                    }
                } else {
                    recordFailedGraftRTG(parseArgs, head.nextInputPosition, transition, previous.numNonSkipChildren)
                    false
                }
            }
        }
    }

    fun parseSkipIfAny(
        atPosition: Int,
        runtimeLhs: Set<LookaheadSet>,
        lh: LookaheadSet,
        possibleEndOfText: Set<LookaheadSet>,
        growArgs: GrowArgs
    ): TreeDataComplete<CompleteNodeIndex>? {

        return if (null == skipParser) {
            //this is a skipParser, so no skipData
            null
        } else {
            val skipLh = if (lh.includesEOT || lh.includesRT) {
                //possibleEndOfText.map { eot -> this.stateSet.createWithParent(lh, eot) }.toSet()
                runtimeLhs.flatMap { rt -> possibleEndOfText.map { eot -> this.stateSet.createLookaheadSet(lh.resolve(eot, rt)) } }.toSet()
            } else {
                setOf(lh)
            }
            this.tryParseSkipUntilNone(skipLh, atPosition, growArgs)
        }
    }

    private fun tryParseSkipUntilNone(possibleEndOfSkip: Set<LookaheadSet>, startPosition: Int, growArgs: GrowArgs): TreeDataComplete<CompleteNodeIndex>? {
        val key = Pair(startPosition, possibleEndOfSkip)
        return if (_skip_cache.containsKey(key)) {
            // can cache null as a valid result
            _skip_cache[key]
        } else {
            val skipData = when (skipParser) { //TODO: raise test to outer level
                null -> null
                else -> tryParseSkip(possibleEndOfSkip, startPosition, growArgs)
            }
            _skip_cache[key] = skipData
            skipData
        }
    }

    private fun tryParseSkip(
        possibleEndOfSkip: Set<LookaheadSet>,
        startPosition: Int,
        growArgs: GrowArgs
    ): TreeDataComplete<CompleteNodeIndex>? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
        if (Debug.OUTPUT_RUNTIME) println("*** Start skip Parser")
        skipParser!!.reset()
        skipParser.start(startPosition, possibleEndOfSkip, growArgs)
        do {
            skipParser.grow3(possibleEndOfSkip, growArgs)
        } while (skipParser.graph.canGrow && (skipParser.graph.goals.isEmpty() || skipParser.graph.goalMatchedAll.not()))
        //TODO: get longest skip match
        if (Debug.OUTPUT_RUNTIME) println("*** End skip Parser")
        return when {
            skipParser.graph.goals.isEmpty() -> null
            else -> {
                skipParser.graph.treeData.complete
            }
        }
    }

    internal fun createEmbeddedRuntimeParser(
        possibleEndOfText: Set<LookaheadSet>,
        runtimeLookaheadSet: Set<LookaheadSet>,
        transition: Transition
    ): Pair<RuntimeParser, Set<LookaheadSet>> {
        val embeddedRhs = transition.to.runtimeRules.first().rhs as RuntimeRuleRhsEmbedded // should only ever be one
        val embeddedRuntimeRuleSet = embeddedRhs.embeddedRuntimeRuleSet
        val embeddedStartRule = embeddedRhs.embeddedStartRule
        val embeddedS0 = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedStartRule.tag, this.stateSet.automatonKind).startState
        val embeddedSkipStateSet = embeddedRuntimeRuleSet.skipParserStateSet
        val embeddedParser = RuntimeParser(embeddedS0.stateSet, embeddedSkipStateSet, embeddedStartRule, this.input, _issues)
        val skipTerms = this.skipStateSet?.firstTerminals ?: emptySet()
        val endingLookahead = transition.lookahead.first().guard //should ony ever be one
        val embeddedPossibleEOT = endingLookahead.unionContent(embeddedS0.stateSet, skipTerms)
        // Embedded text could end with this.skipTerms or lh from transition
        val embeddedEOT = runtimeLookaheadSet.flatMap { rt ->
            possibleEndOfText.map { eot ->
                embeddedPossibleEOT.resolve(eot, rt).lhs(embeddedS0.stateSet)
            }
        }.toSet()
        return Pair(embeddedParser, embeddedEOT)
    }

    // Use current/growing runtimeLookahead
    internal fun doEmbedded(
        head: GrowingNodeIndex, transition: Transition, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs
    ): Boolean {
        return when {
            parseArgs.heightGraftOnly -> false
            //TODO: what about an empty embedded ?
            else -> {
                val (embeddedParser, embeddedEOT) = createEmbeddedRuntimeParser(possibleEndOfText, head.runtimeState.runtimeLookaheadSet, transition)
                val endingLookahead = transition.lookahead.first().guard //should ony ever be one
                val startPosition = head.nextInputPositionAfterSkip
                embeddedParser.start(startPosition, embeddedEOT, parseArgs)
                var seasons = 1
                var maxNumHeads = embeddedParser.graph.numberOfHeads
                do {
                    embeddedParser.grow3(embeddedEOT, parseArgs)
                    seasons++
                    maxNumHeads = max(maxNumHeads, embeddedParser.graph.numberOfHeads)
                } while (embeddedParser.graph.canGrow && (embeddedParser.graph.goals.isEmpty() || embeddedParser.graph.goalMatchedAll.not()))
                //val match = embeddedParser.longestMatch(seasons, maxNumHeads, true) as SPPTBranch?
                val match = embeddedParser.graph.treeData.complete
                if (match.root != null) {
                    val embeddedS0 = embeddedParser.stateSet.startState
                    val ni = match.root?.nextInputPositionAfterSkip!! // will always have value if root not null
                    //TODO: parse skipNodes
                    val skipLh = head.runtimeState.runtimeLookaheadSet.flatMap { rt ->
                        possibleEndOfText.map { eot ->
                            endingLookahead.resolve(eot, rt).lhs(embeddedS0.stateSet)
                        }
                    }.toSet()
                    val skipData = this.tryParseSkipUntilNone(skipLh, ni, parseArgs)//, lh) //TODO: does the result get reused?
                    //val nextInput = skipData?.nextInputPosition ?: ni
                    if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head, taking: $transition" }
                    this.graph.pushEmbeddedToStackOf(
                        head,
                        transition.to,
                        head.runtimeState.runtimeLookaheadSet,
                        startPosition,
                        ni,
                        match,
                        skipData
                    )
                } else {
                    //  could not parse embedded
                    //this.embeddedLastDropped[transition] = embeddedParser.lastDropped
                    recordFailedEmbedded(parseArgs, head.nextInputPosition, transition, embeddedParser.failedReasons)
                    false
                }
            }
        }
    }

    private fun ambiguity(head: GrowingNodeIndex, trans: Collection<Transition>, possibleEndOfText: Set<LookaheadSet>) {
        val from = head.runtimeState.state.runtimeRules.joinToString(separator = ",") { it.tag }
        val ambigStr = trans.map { tr ->
            val lh: LookaheadSetPart = tr.lookahead.map { it.guard.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val eot = possibleEndOfText.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val rt: LookaheadSetPart = head.runtimeState.runtimeLookaheadSet.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val lhr = lh.resolve(eot, rt).fullContent
            "${tr.to.rulePositions.map { "(${it.rule.tag},${it.option})" }} on ${lhr.map { it.tag }}"
        }
        val ambigOn = trans.map { it.action }.toSet()
        val ambigOnStr = ambigOn.joinToString(separator = "/") { "$it" }
        val into = when (ambigStr.size) {
            1 -> ambigStr.first()
            else -> ambigStr.joinToString(prefix = "\n    ", separator = "\n    ", postfix = "\n") { it }
        }
        val loc = input.locationFor(head.nextInputPositionAfterSkip, 1)
        _issues.raise(LanguageIssueKind.WARNING, LanguageProcessorPhase.GRAMMAR, loc, "Ambiguity in parse (on $ambigOnStr): ($from) into $into", ambigOn)
    }

    private fun recordFailedWidthTo(parseArgs: GrowArgs, position: Int, transition: Transition) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonWidthTo(position, transition))
        }
    }

    private fun recordFailedWidthLH(parseArgs: GrowArgs, position: Int, transition: Transition, runtimeLhs: Set<LookaheadSet>, possibleEndOfText: Set<LookaheadSet>) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonLookahead(position, transition, runtimeLhs, possibleEndOfText))
        }
    }

    private fun recordFailedEmbedded(parseArgs: GrowArgs, position: Int, transition: Transition, failedEmbeddedReasons: List<FailedParseReason>) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonEmbedded(position, transition, failedEmbeddedReasons))
        }
    }

    private fun recordFailedHeightLh(parseArgs: GrowArgs, position: Int, transition: Transition, runtimeLhs: Set<LookaheadSet>, possibleEndOfText: Set<LookaheadSet>) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonLookahead(position, transition, runtimeLhs, possibleEndOfText))
        }
    }

    private fun recordFailedGraftRTG(parseArgs: GrowArgs, position: Int, transition: Transition, prevNumNonSkipChildren: Int) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonGraftRTG(position, transition, prevNumNonSkipChildren))
        }
    }

    private fun recordFailedGraftLH(parseArgs: GrowArgs, position: Int, transition: Transition, runtimeLhs: Set<LookaheadSet>, possibleEndOfText: Set<LookaheadSet>) {
        if (parseArgs.reportErrors) {
            failedReasons.add(FailedParseReasonLookahead(position, transition, runtimeLhs, possibleEndOfText))
        }
    }
}