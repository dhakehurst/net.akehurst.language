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

package net.akehurst.language.parser.leftcorner


import net.akehurst.language.automaton.leftcorner.*
import net.akehurst.language.automaton.leftcorner.ParserState.Companion.lhs
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.runtime.structure.RuntimePreferenceRule.RuntimePreferenceOption
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.scanner.api.Scanner
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.collections.clone
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.parser.api.Assoc
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parsermessages.IssueMessage
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData
import net.akehurst.language.sppt.api.isEmptyMatch
import kotlin.math.max

internal data class RuntimeContext(
    val head: GrowingNodeIndex,
    val prev: GrowingNodeIndex?, // null if head is goal
    val prevPrev: GrowingNodeIndex? // null if head is goal
)

internal class RuntimeParser(
    val sentence: Sentence, //FIXME: should really be a method argument
    val isSkipParser: Boolean,
    val stateSet: ParserStateSet,
    val skipStateSet: ParserStateSet?, // null if this is a skipParser
    val cacheSkip: Boolean,
    val userGoalRule: RuntimeRule,
    private val scanner: Scanner,
    private val _issues: IssueHolder
) {
    companion object {
        data class GrowArgs(
            val buildTree: Boolean, //TODO: want to not build tree is some situations, however tree is used for resolving ambiguities!
            val noLookahead: Boolean,
            val heightGraftOnly: Boolean,
            val nonEmptyWidthOnly: Boolean,
            val reportErrors: Boolean,
            val reportGrammarAmbiguities: Boolean,
            val allowEOTAfterSkip: Boolean,
            val snapshoGss: Boolean
        )

        //val normalArgs = GrowArgs(true, false, false, false, true)
        //val forPossErrors = GrowArgs(false, true, false, true, true, true)
        //val heightGraftOnly = GrowArgs(false, true, true, false, false, true)
        val forExpectedAt = GrowArgs(false, false, false, false, true, false, true, true)

    }

    val graph = ParseGraph(sentence, scanner, this.stateSet.number)

    val canGrow: Boolean get() = this.graph.canGrow

    val failedReasons = mutableMapOf<Int, MutableList<FailedParseReason>>()

    private var interruptedMessage: String? = null

    private val _skip_cache = mutableMapOf<Pair<Int, Set<LookaheadSet>>, TreeData?>()

    // must use a different instance of Input, so it can be reset, reset clears cached leaves. //TODO: check this
    private val skipParser = skipStateSet?.let {
        if (this.stateSet.preBuilt) this.skipStateSet.build()
        val skipScanner = when (scanner) {
            is ScannerOnDemand -> ScannerOnDemand(this.scanner.regexEngine, skipStateSet.usedTerminalRules.toList())
            is ScannerClassic -> scanner //ScannerClassic(this.scanner.sentence.text, skipStateSet.usedTerminalRules.toList())
            else -> error("subtype of Scanner unsupported - ${scanner::class.simpleName}")
        }
        RuntimeParser(sentence, true, it, null, false, skipStateSet.userGoalRule, skipScanner, _issues)
    }

    fun reset() {
        this._skip_cache.clear()
        this.graph.reset()
        this.failedReasons.clear()
    }

    fun buildSkipParser() {
        skipParser?.stateSet?.build()
    }

    fun start(startPosition: Int, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs): Map<Int, List<FailedParseReason>> {
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
        this.graph.start(gState, startPosition, runtimeLookahead, initialSkipData) //TODO: remove LH
        return skipParser?.failedReasons?.clone() ?: emptyMap()
    }

    fun interrupt(message: String) {
        this.interruptedMessage = message
    }

    fun checkForTerminationRequest() {
        val m = this.interruptedMessage
        if (null == m) {
            //do nothing
        } else {
            error("ParserTerminated: $m")//throw ParserTerminatedException(m)
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
        fun GrowingNodeIndex.asString() = "(s${runtimeState.state.number.value},${startPosition}-${nextInputPositionAfterSkip}{${numNonSkipChildren}}${
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
        val currentNextInputPosition = this.graph.nextHeadNextInputPosition
        this.failedReasonsClear(currentNextInputPosition)
        //this.lastDropped.clear()
        //this.lastToGrow = this.graph.peekAllNextToProcess()

        var steps = 0
        val progressSteps = lazyMutableMapNonNull<GrowingNodeIndex, Int> { 0 }
        val doneEmpties = mutableSetOf<Pair<ParserState, List<Pair<GrowingNodeIndex, Set<GrowingNodeIndex>>>>>()

        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentNextInputPosition) {
            checkForTerminationRequest()
            val graph = this.graph //TODO: remove..for debug only
            if (Debug.OUTPUT_RUNTIME) {
                println("step $steps --------------------------------------")
                println(graph)
            }

            val head = this.graph.peekNextHead

            //TODO: move empty checking stuff to doWidth
            if (head.isEmptyMatch && doneEmpties.contains(Pair(head.state, graph.previousOf(head).map { Pair(it, graph.previousOf(it)) }))) {
                //don't do it again
                this.graph.dropStackWithHead(head)
            } else {
                if (head.isEmptyMatch) {
                    doneEmpties.add(Pair(head.state, graph.previousOf(head).map { Pair(it, graph.previousOf(it)) }))
                }
                growHead(head, possibleEndOfText, growArgs)
                steps++
            }

            val progStep = progressSteps[head] //FIXME: maybe slow - is there a better way?
            if (progStep > 1000) { //FIXME: make part of config
                this.interrupt(IssueMessage.PARSER_WONT_STOP) //TODO: include why - which node/rule
            } else {
                progressSteps[head] = progStep + 1
            }
        }
        return steps
    }

    private fun growHead(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, growArgs: GrowArgs): Boolean {
        return when {
            head.isGoal -> growGoal(head, possibleEndOfText, growArgs) //GOAL
            head.isComplete -> growComplete(head, possibleEndOfText, growArgs) // HEIGHT,GRAFT
            else -> growIncomplete(head, possibleEndOfText, growArgs) // WIDTH, EMBED
        }
    }

    private fun growGoal(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs): Boolean {
        return if (head.isComplete) {
            graph.recordGoal(head)
            graph.dropStackWithHead(head)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $head" }
            false
        } else {
            val runtimeContext = RuntimeContext(head, null, null)
            var grown = false
            val transTaken = mutableSetOf<Transition>()
            val transitions = head.runtimeState.transitionsGoal(stateSet.startState)
            val trans2 = resolveTransitionAmbiguity(runtimeContext, transitions, possibleEndOfText, parseArgs)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${trans2.joinToString(separator = "\n") { "  $it" }}" }
            for (tr in trans2) {
                val b = when (tr.action) {
                    ParseAction.WIDTH -> doWidth(head, tr, possibleEndOfText, parseArgs)
                    ParseAction.EMBED -> doEmbedded(head, tr, possibleEndOfText, parseArgs)
                    else -> error("Internal Error: should only have WIDTH or EMBED transitions here")
                }
                if (b) transTaken.add(tr)
                grown = grown || b
            }
            if (grown.not()) doNoTransitionsTaken(head)
            grown
        }
    }

    private fun growIncomplete(head: GrowingNodeIndex, possibleEndOfText: Set<LookaheadSet>, parseArgs: GrowArgs): Boolean {
        var grown = false
        val prevSet = this.graph.previousOf(head)
        val transTaken = mutableSetOf<Transition>()
        for (previous in prevSet) {
            val runtimeContext = RuntimeContext(head, previous, null)
            val transitions = head.runtimeState.transitionsInComplete(previous.state)
            val trans2 = resolveTransitionAmbiguity(runtimeContext, transitions, possibleEndOfText, parseArgs)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${trans2.joinToString(separator = "\n") { "  $it" }}" }
            for (tr in trans2) {
                val b = when (tr.action) {
                    ParseAction.WIDTH -> doWidth(head, tr, possibleEndOfText, parseArgs)
                    ParseAction.EMBED -> doEmbedded(head, tr, possibleEndOfText, parseArgs)
                    else -> error("Internal Error: should only have WIDTH or EMBED transitions here")
                }
                if (b) transTaken.add(tr)
                grown = grown || b
            }
        }

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
                //graph.dropGrowingTreeData(it.key)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped: $it" }
            } else {
                doNoTransitionsTaken(it.key)
            }
        }

        return headGrownHeight || headGrownGraft
    }

    private fun matchedLookahead(position: Int, lookahead: Set<Lookahead>, possibleEndOfText: Set<LookaheadSet>, runtimeLhs: Set<LookaheadSet>, acceptEOT:Boolean) =
        when {
//            possibleEndOfText.size > 1 -> TODO()
//            runtimeLhs.size > 1 -> TODO()
            else -> {
                val lh1: LookaheadSetPart = lookahead.map { it.guard.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                val eot = possibleEndOfText.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                val rt: LookaheadSetPart = runtimeLhs.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                //possibleEndOfText.flatMap { eot ->
                //    runtimeLhs.map { rt ->
                val lh = when(acceptEOT) {
                    true -> lh1.union(LookaheadSetPart.EOT)
                    false -> lh1
                }
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
        val runtimeContext = RuntimeContext(head, previous, prevPrev)
        var grownHeight = false
        var grownGraft = false
        val transitions = head.runtimeState.transitionsComplete(previous.state, prevPrev?.state ?: stateSet.startState)
        val trans2 = resolveTransitionAmbiguity(runtimeContext, transitions, possibleEndOfText, parseArgs)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${trans2.joinToString(separator = "\n") { "  $it" }}" }
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
                    val trgs = mutableListOf<Transition>()
                    val trhs = mutableListOf<Transition>()
                    for (tr in grp.value) {
                        when (tr.action) {
                            ParseAction.GRAFT -> trgs.add(tr)
                            ParseAction.HEIGHT -> trhs.add(tr)
                            else -> error("should not happen")
                        }
                    }
                    trgs.sortWith { t1, t2 ->
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
                    }
                    /*
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
                    */
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

    private fun resolveGrafts1(grafts: List<Pair<Transition, LookaheadSetPart>>): List<Transition> {
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
        return listOf(grafts.last().first)
    }

    private fun resolveGrafts2(grafts: List<Pair<Transition, LookaheadSetPart>>): List<Transition> {
        // this approach causes issues

        // if multiple GRAFT trans to same rule with same lh, prefer left most target
        val graftsGrpByLh = grafts.groupBy { it.second }
        return graftsGrpByLh.map {
            val sorted = it.value.sortedWith(Comparator { t1, t2 ->
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
            sorted.last().first
        }
    }

    private fun resolveTransitionAmbiguity(
        runtimeContext: RuntimeContext,
        transitions: List<Transition>,
        possibleEndOfText: Set<LookaheadSet>,
        parseArgs: GrowArgs
    ): List<Transition> {
        val trans2 = when {
            runtimeContext.head.isComplete -> {
                if (null == runtimeContext.prev) {
                    error("previous must have a value if head is incomplete")
                }
                val transWithValidLookahead = when {
                    parseArgs.noLookahead -> transitions.map { Pair(it, LookaheadSetPart.ANY) }
                    else -> transitions.mapNotNull {
                        val m = matchedLookahead(runtimeContext.head.nextInputPositionAfterSkip, it.lookahead, possibleEndOfText, runtimeContext.prev.runtimeState.runtimeLookaheadSet,parseArgs.allowEOTAfterSkip)
                        when {
                            m.first -> Pair(it, m.second)
                            else -> null
                        }
                    }
                }
                transitions.minus(transWithValidLookahead.map { it.first }).forEach {
                    when (it.action) {
                        ParseAction.HEIGHT -> recordFailedHeightLh(parseArgs, runtimeContext.head, it, runtimeContext.prev.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                        ParseAction.GRAFT -> recordFailedGraftLH(parseArgs, runtimeContext.head, it, runtimeContext.prev.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                        ParseAction.GOAL -> recordFailedGraftLH(parseArgs, runtimeContext.head, it, runtimeContext.prev.runtimeState.runtimeLookaheadSet, possibleEndOfText)
                        else -> error("Internal Error: should never happen")
                    }
                }
                resolvePrecedence(transWithValidLookahead, runtimeContext)
            }

            else -> transitions
        }
        if (parseArgs.reportGrammarAmbiguities && trans2.size > 1) {
            ambiguity(runtimeContext, trans2, possibleEndOfText)
        }
        return trans2
    }

    private fun precedenceSpineMatches(prOpt: RuntimePreferenceRule.RuntimePreferenceOption, to: RulePositionRuntime, runtimeContext: RuntimeContext): Boolean {
        val optSpineRev = prOpt.spine.reversed()
        return when {
            optSpineRev.size > 2 -> error("Unsupported")
            to.option != prOpt.option -> false
            to.rule != optSpineRev.first() -> false
            optSpineRev.size > 1 && optSpineRev[1] != runtimeContext.prev!!.state.firstRule -> false
            else -> true
        }
    }

    private fun precedenceFor(precRules: RuntimePreferenceRule, to: List<RulePositionRuntime>, lh: LookaheadSetPart, runtimeContext: RuntimeContext): List<RuntimePreferenceOption> {
        val r = precRules.options.filter { prOpt ->
            val rpMatch = to.any { precedenceSpineMatches(prOpt, it, runtimeContext) }
            rpMatch && prOpt.operators.any { lh.fullContent.contains(it) }
        }
        return r
    }

    private fun resolvePrecedence(transitions: List<Pair<Transition, LookaheadSetPart>>, runtimeContext: RuntimeContext): List<Transition> {
        return when {
            2 > transitions.size -> transitions.map { it.first }
            else -> {
                val precRules = this.stateSet.precedenceRulesFor(runtimeContext.head.state)
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
                                    resolveGrafts1(grafts)
                                }
                            }
                        }
                    }

                    else -> {
                        val precedence = transitions.flatMap { (tr, lh) ->
                            val lhf = lh//.map { it.second }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                            val prec = precedenceFor(precRules, tr.to.rulePositions, lhf, runtimeContext)
                            prec.map { Pair(tr, it) }
                        }
                        when {
                            precedence.isEmpty() -> transitions.map { it.first }
                            else -> {
                                val max = precedence.map { it.second.precedence }.maxOrNull() ?: 0
                                val maxPrec = precedence.filter { max == it.second.precedence }
                                val byTarget = maxPrec.groupBy { Pair(it.first.to.runtimeRules, it.second.associativity) }
                                val assoc = byTarget.flatMap {
                                    when (it.key.second) {
                                        Assoc.NONE -> it.value
                                        Assoc.LEFT -> when {
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

                                        Assoc.RIGHT -> when {
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
                        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head\n  taking: $transition" }
                        this.graph.growNextChild(
                            head = head,
                            previous = previous,
                            prevPrev = null,
                            newParentState = transition.to,
                            newParentRuntimeLookaheadSet = runtimeLhs,
//                            buildSPPT = growArgs.buildTree
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
            parseArgs.nonEmptyWidthOnly && (transition.to.firstRule.isEmptyTerminal || transition.to.firstRule.isEmptyListTerminal) -> false
            else -> {
                val l = this.graph.scanner.findOrTryCreateLeaf(sentence, head.nextInputPositionAfterSkip, transition.to.firstRule)
                if (null != l) {
                    val lh1 = transition.lookahead.map { it.guard }.reduce { acc, e -> acc.union(this.stateSet, e) } //TODO:reduce to 1 in SM
                    val lh = when (parseArgs.allowEOTAfterSkip) {
                        true -> lh1.union(this.stateSet, LookaheadSet.EOT)
                        false -> lh1
                    }
                    val runtimeLhs = head.runtimeState.runtimeLookaheadSet

                    val (skipData, skipFailed) = parseSkipIfAny(head.nextInputPositionAfterSkip, l.nextInputPosition, runtimeLhs, lh, possibleEndOfText, parseArgs)
                    if (skipFailed.not() || l.isEmptyMatch) { //TODO: check for isEmptyMatch feels like a hack!
                        val nextInputPositionAfterSkip = skipData?.root?.nextInputNoSkip ?: l.nextInputPosition
                        val hasLh = possibleEndOfText.any { eot ->
                            runtimeLhs.any { rt ->
                                this.graph.isLookingAt(lh, eot, rt, nextInputPositionAfterSkip)
                            }
                        }
                        if (parseArgs.noLookahead || hasLh) {
                            val startPosition = l.startPosition
                            val nextInputPosition = l.nextInputPosition //TODO: should just be/pass nextInputPositionAfterSkip ?
                            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head\n  taking: $transition" }
                            this.graph.pushToStackOf(head, transition.to, setOf(LookaheadSet.EMPTY), startPosition, nextInputPosition, skipData)
                        } else {
                            val pos = if (null != skipParser && skipParser.failedReasons.isNotEmpty()) {
                                skipParser.failedReasons.keys.max()//maxOf { it.position }
                            } else {
                                nextInputPositionAfterSkip
                            }
                            recordFailedWidthLH(parseArgs, head, pos, transition, runtimeLhs, possibleEndOfText)
                            false
                        }
                    } else {
                        recordFailedExpectedSkipAfter(parseArgs, head, l, transition)
                        false
                    }
                } else {
                    recordFailedWidthTo(parseArgs, head, transition)
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
                //FIXME: we already know lookahead is matched because it was tested during resolveTransitionAmbiguity !
                val runtimeLhs = previous!!.runtimeState.runtimeLookaheadSet
                val lhWithMatch = transition.lookahead.flatMap { lh ->
                    possibleEndOfText.flatMap { eot ->
                        runtimeLhs.mapNotNull { rt ->
                            val lhg = when(parseArgs.allowEOTAfterSkip) {
                                true -> lh.guard.union(this.stateSet, LookaheadSet.EOT)
                                false -> lh.guard
                            }
                            val b = this.graph.isLookingAt(lhg, eot, rt, head.nextInputPositionAfterSkip) //TODO: if(transition.lookaheadGuard.includesUP) {
                            if (b) Triple(eot, rt, lh.up) else null
                        }
                    }
                }
               // val hasLh = lhWithMatch.isNotEmpty()
                val hasLh  =true // transitions lookahead already checked in resolveTransitionAmbiguity
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
                    if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head\n  taking: $transition" }
                    this.graph.createWithFirstChild(
                        head = head,
                        previous = previous,
                        parentState = transition.to,
                        parentRuntimeLookaheadSet = newRuntimeLhs,
//                         buildSPPT = parseArgs.buildTree
                    )
                } else {
                    recordFailedHeightLh(parseArgs, head, transition, runtimeLhs, possibleEndOfText)
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
                    val lhWithMatch = matchedLookahead(head.nextInputPositionAfterSkip, transition.lookahead, possibleEndOfText, runtimeLhs,parseArgs.allowEOTAfterSkip)
                    val hasLh = lhWithMatch.first//isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
                    if (parseArgs.noLookahead || hasLh) {
                        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head\n  taking: $transition" }
                        this.graph.growNextChild(
                            head = head,
                            previous = previous,
                            prevPrev = prevPrev,
                            newParentState = transition.to,
                            newParentRuntimeLookaheadSet = runtimeLhs,
//                            buildSPPT = parseArgs.buildTree
                        )
                    } else {
                        recordFailedGraftLH(parseArgs, head, transition, runtimeLhs, possibleEndOfText)
                        false
                    }
                } else {
                    recordFailedGraftRTG(parseArgs, head, transition, previous.numNonSkipChildren)
                    false
                }
            }
        }
    }

    /**
     * return pair containing TreeData of skip and bool indicating if skip has failed
     * sometimes skip is required, but there is none!
     */
    fun parseSkipIfAny(
        lastTokenStart: Int,
        atPosition: Int,
        runtimeLhs: Set<LookaheadSet>,
        lhs: LookaheadSet,
        possibleEndOfText: Set<LookaheadSet>,
        parseArgs: GrowArgs
    ): Pair<TreeData?, Boolean> {

        return if (null == skipParser) {
            //this is a skipParser, so no skipData
            Pair(null, false)
        } else {
            val skipLh = if (lhs.includesEOT || lhs.includesRT) {
                //possibleEndOfText.map { eot -> this.stateSet.createWithParent(lh, eot) }.toSet()
                runtimeLhs.flatMap { rt -> possibleEndOfText.map { eot -> this.stateSet.createLookaheadSet(lhs.resolve(eot, rt)) } }.toSet()
            } else {
                setOf(lhs)
            }

            //check if skip node is required - i.e. if parsing last token could overlap next token without a skip
            var needSkip = false
            for (lh in skipLh) {
                for (lhr in lh.content) {
                    val ml = this.graph.scanner.matchedLength(sentence, lastTokenStart, lhr)
                    when {
                        lastTokenStart + ml > atPosition -> needSkip = true
                        else -> Unit
                    }
                }
            }

            val skipData = this.tryParseSkipUntilNone(skipLh, atPosition, parseArgs)

            return when {
                needSkip && null == skipData -> {
                    Pair(skipData, true)
                }

                else -> Pair(skipData, false)
            }
        }
    }

    private fun tryParseSkipUntilNone(possibleEndOfSkip: Set<LookaheadSet>, startPosition: Int, growArgs: GrowArgs): TreeData? {
        val slh = when (growArgs.allowEOTAfterSkip) {
            true -> possibleEndOfSkip + LookaheadSet.EOT
            false -> possibleEndOfSkip
        }
        return if (this.cacheSkip) {
            val key = Pair(startPosition, slh)
            if (_skip_cache.containsKey(key)) {
                // can cache null as a valid result
                _skip_cache[key]
            } else {
                val skipData = when (skipParser) { //TODO: raise test to outer level
                    null -> null
                    else -> tryParseSkip(slh, startPosition, growArgs)
                }
                _skip_cache[key] = skipData
                skipData
            }
        } else {
            tryParseSkip(slh, startPosition, growArgs)
        }
    }

    private fun tryParseSkip(
        possibleEndOfSkip: Set<LookaheadSet>,
        startPosition: Int,
        growArgs: GrowArgs
    ): TreeData? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
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
        val embeddedParser = RuntimeParser(sentence, false, embeddedS0.stateSet, embeddedSkipStateSet, this.cacheSkip, embeddedStartRule, this.scanner, _issues)
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
                    val ni = match.root?.nextInputPosition!! // will always have value if root not null
                    //TODO: parse skipNodes
                    val skipLh = head.runtimeState.runtimeLookaheadSet.flatMap { rt ->
                        possibleEndOfText.map { eot ->
                            endingLookahead.resolve(eot, rt).lhs(embeddedS0.stateSet)
                        }
                    }.toSet()
                    val skipData = this.tryParseSkipUntilNone(skipLh, ni, parseArgs)//, lh) //TODO: does the result get reused?
                    //val nextInput = skipData?.nextInputPosition ?: ni
                    if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "For $head\n  taking: $transition" }
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
                    recordFailedEmbedded(parseArgs, head, transition, embeddedParser.failedReasons)
                    false
                }
            }
        }
    }

    private fun ambiguity(runtimeContext: RuntimeContext, trans: Collection<Transition>, possibleEndOfText: Set<LookaheadSet>) {
        val from = runtimeContext.head.runtimeState.state.runtimeRules.joinToString(separator = ",") { it.tag }
        val ambigStr = trans.map { tr ->
            val lh: LookaheadSetPart = tr.lookahead.map { it.guard.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val eot = possibleEndOfText.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val rt: LookaheadSetPart = runtimeContext.head.runtimeState.runtimeLookaheadSet.map { it.part }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            val lhr = lh.resolve(eot, rt).fullContent
            val spine = when (tr.action) {
                ParseAction.GOAL -> ""
                ParseAction.GRAFT -> "${runtimeContext.prevPrev!!.state.firstRule.tag} <- "
                ParseAction.HEIGHT -> "${runtimeContext.prev!!.state.firstRule.tag} <-- "
                ParseAction.WIDTH -> ""
                ParseAction.EMBED -> ""
            }

            "$spine${tr.to.rulePositions.map { "(${it.rule.tag},${it.option})" }} on ${lhr.map { it.tag }}"
        }
        val ambigOn = trans.map { it.action }.toSet()
        val ambigOnStr = ambigOn.joinToString(separator = "/") { "$it" }
        val into = when (ambigStr.size) {
            1 -> ambigStr.first()
            else -> ambigStr.joinToString(prefix = "\n    ", separator = "\n    ", postfix = "\n") { it }
        }
        val loc = sentence.locationFor(runtimeContext.head.nextInputPositionAfterSkip, 1)
        _issues.raise(LanguageIssueKind.WARNING, LanguageProcessorPhase.GRAMMAR, loc, "Ambiguity in parse (on $ambigOnStr):\n  ($from) into $into", ambigOn)
    }

    private fun failedReasonsClear(beforePosition: Int) {
        val rmv = failedReasons.keys.filter { it < beforePosition }
        rmv.forEach {
            failedReasons.remove(it)
        }
    }

    private fun failedReasonsAdd(fr: FailedParseReason) {
        val l = failedReasons[fr.failedAtPosition]
        if (null == l) {
            failedReasons[fr.failedAtPosition] = mutableListOf(fr)
        } else {
            l.add(fr)
        }
    }

    private fun recordFailedExpectedSkipAfter(parseArgs: GrowArgs, head: GrowingNodeIndex, nextLeaf: SpptDataNode, transition: Transition) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = nextLeaf.nextInputNoSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            val skipTerms = skipStateSet?.firstTerminals ?: emptySet()
            failedReasonsAdd(FailedParseExpectedSkipAfter(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, skipTerms))
        }
    }

    private fun recordFailedWidthTo(parseArgs: GrowArgs, head: GrowingNodeIndex, transition: Transition) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = head.nextInputPositionAfterSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            failedReasonsAdd(FailedParseReasonWidthTo(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot))
        }
    }

    private fun recordFailedWidthLH(
        parseArgs: GrowArgs,
        head: GrowingNodeIndex,
        failedAtPosition: Int,
        transition: Transition,
        runtimeLhs: Set<LookaheadSet>,
        possibleEndOfText: Set<LookaheadSet>
    ) {
        if (parseArgs.reportErrors) {
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            failedReasonsAdd(FailedParseReasonLookahead(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, runtimeLhs, possibleEndOfText))
        }
    }

    private fun recordFailedEmbedded(parseArgs: GrowArgs, head: GrowingNodeIndex, transition: Transition, failedEmbeddedReasons: Map<Int, MutableList<FailedParseReason>>) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = head.nextInputPositionAfterSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            val fr = failedEmbeddedReasons[failedEmbeddedReasons.keys.max()]!!
            failedReasonsAdd(FailedParseReasonEmbedded(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, fr))
        }
    }

    private fun recordFailedHeightLh(parseArgs: GrowArgs, head: GrowingNodeIndex, transition: Transition, runtimeLhs: Set<LookaheadSet>, possibleEndOfText: Set<LookaheadSet>) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = head.nextInputPositionAfterSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            failedReasonsAdd(FailedParseReasonLookahead(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, runtimeLhs, possibleEndOfText))
        }
    }

    private fun recordFailedGraftRTG(parseArgs: GrowArgs, head: GrowingNodeIndex, transition: Transition, prevNumNonSkipChildren: Int) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = head.nextInputPositionAfterSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            failedReasonsAdd(FailedParseReasonGraftRTG(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, prevNumNonSkipChildren))
        }
    }

    private fun recordFailedGraftLH(parseArgs: GrowArgs, head: GrowingNodeIndex, transition: Transition, runtimeLhs: Set<LookaheadSet>, possibleEndOfText: Set<LookaheadSet>) {
        if (parseArgs.reportErrors) {
            val failedAtPosition = head.nextInputPositionAfterSkip
            val gssSnapshot = when {
                parseArgs.snapshoGss -> this.graph._gss.snapshotFor(head) ?: emptyMap()
                else -> emptyMap()
            }
            failedReasonsAdd(FailedParseReasonLookahead(this.isSkipParser, failedAtPosition, head, transition, gssSnapshot, runtimeLhs, possibleEndOfText))
        }
    }
}