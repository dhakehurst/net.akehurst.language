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

import net.akehurst.language.agl.automaton.*
import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.*
import net.akehurst.language.agl.runtime.structure.RuleOption
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserInterruptedException
import net.akehurst.language.api.sppt.SPPTNode
import kotlin.math.max

internal class RuntimeParser(
    internal val stateSet: ParserStateSet,
    private val skipStateSet: ParserStateSet?, // null if this is a skipParser
    val userGoalRule: RuntimeRule,
    private val input: InputFromString
) {
    companion object {
        val defaultStartLocation = InputLocation(0, 0, 1, 0)
    }

    val graph = ParseGraph(input, this.stateSet.number, this.stateSet.usedTerminalRules.size, this.stateSet.usedNonTerminalRules.size)

    //needs to be public so that expectedAt can use it
    //val lastGrown: Collection<GrowingNode> get() = this.grownInLastPass.toSet()
    var lastGrown: List<ParseGraph.Companion.NextToProcess> = emptyList()
    val canGrow: Boolean get() = this.graph.canGrow

    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    //private var grownInThisPass = mutableListOf<GrowingNode>()
    //internal var grownInThisPassPrevious = mutableMapOf<GrowingNode, MutableSet<GrowingNodeIndex>>()
    //private var grownInLastPass = mutableListOf<GrowingNode>()
    //internal var grownInLastPassPrevious = mutableMapOf<GrowingNode, MutableSet<GrowingNodeIndex>>()
    //internal lateinit var _lastGss: GraphStructuredStack<GrowingNodeIndex>
    private var interruptedMessage: String? = null

    private val readyForShift = mutableListOf<GrowingNode>()
    private val readyForShiftPrevious = mutableMapOf<GrowingNode, MutableSet<GrowingNodeIndex>>()
    private val _skip_cache = mutableMapOf<Pair<Int, Set<LookaheadSet>>, TreeData?>()

    // must use a different instance of Input, so it can be reset, reset clears cached leaves. //TODO: check this
    private val skipParser = skipStateSet?.let {
        if (this.stateSet.preBuilt) this.skipStateSet.build()
        RuntimeParser(it, null, skipStateSet.userGoalRule, InputFromString(skipStateSet.usedTerminalRules.size, this.input.text))
    }

    fun reset() {
        this.graph.reset()
    }

    fun buildSkipParser() {
        skipParser?.stateSet?.build()
    }

    fun start(startPosition: Int, possibleEndOfText: Set<LookaheadSet>) {
        val gState = stateSet.startState
        val initialSkipData = if (this.stateSet.isSkip) {
            null
        } else {
            val skipLhsp = gState.rulePositions.map { this.stateSet.buildCache.expectedAt(it, LookaheadSetPart.EOT) }.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e) }
            val endOfSkipLookaheadSet = this.stateSet.createLookaheadSet(skipLhsp.includesUP, skipLhsp.includesEOT, skipLhsp.matchANY, skipLhsp.content)
            this.tryParseSkipUntilNone(setOf(endOfSkipLookaheadSet), startPosition, false) //TODO: think this might allow some wrong things, might be a better way
        }
        this.graph.start(gState, startPosition, possibleEndOfText, initialSkipData) //TODO: remove LH
    }

    fun interrupt(message: String) {
        this.interruptedMessage = message
    }

    fun checkInterrupt() {
        val m = this.interruptedMessage
        if (null == m) {
            //do nothing
        } else {
            throw ParserInterruptedException(m)
        }
    }

    /*
    fun resetGraphToLastGrown() {
        this.graph._gss = this._lastGss
        for (r in this.graph._gss.roots) {
            this.graph._growingHeadHeap[r] = GrowingNode(this.graph, r)
        }
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
*/
    fun rememberForErrorComputation(toProcess: ParseGraph.Companion.NextToProcess) {
        //this.grownInThisPass.add(gn)
        //var set = this.grownInThisPassPrevious[gn]
        // if (null==set) {
        //    set = mutableSetOf<GrowingNodeIndex>()
        //    this.grownInThisPassPrevious[gn] = set
        //}
        //if (null!=previous){
        //    set.add(previous)
        //}
    }

    fun cacheLastGrown() {
        this.lastGrown = this.graph.peekNextToProcess()
    }
/*
    // used for finding error info
    fun tryGrowWidthOnce() {
        this.grow3(true)
    }

    fun tryGrowWidthOnce1() {
        //this.startPass()
        val currentStartPosition = this.graph.nextHeadStartPosition
        while (this.graph.hasNextHead && this.graph.nextHeadStartPosition <= currentStartPosition) {
            val (gn, previous) = this.graph.nextHead()
            checkInterrupt()
            this.growWidthOnly(gn, previous)
        }
        //this.endPass()
    }

    // used for finding error info
    fun tryGrowHeightOrGraft(): Set<Pair<GrowingNodeIndex, Set<GrowingNodeIndex>>> {
        val lg = mutableSetOf<Pair<GrowingNodeIndex, Set<GrowingNodeIndex>>>()
        //this.startPass()
        // try height or graft
        val currentStartPosition = this.graph.nextHeadStartPosition
        while (this.graph.hasNextHead && this.graph.nextHeadStartPosition <= currentStartPosition) {
            //while ((this.canGrow && this.graph.goals.isEmpty())) {
            val nextToProcess = this.graph.nextToProcess()
            checkInterrupt()
            lg.add(Pair(gn.index, previous))
            this.growHeightOrGraftOnly(gn, previous)
        }
        //this.endPass()
        //return this.lastGrown.toSet()
        return lg
    }
*/
    // used for finding error info
    /*
    internal fun growWidthOnly(toProcess: ParseGraph.Companion.NextToProcess) {
        when (toProcess.growingNode.runtimeRules.first().kind) { //FIXME
            RuntimeRuleKind.GOAL -> {
                val transitions = toProcess.growingNode.runtimeState.transitions(RuntimeState(stateSet.startState, setOf(LookaheadSet.EMPTY)))
                    .filter { tr -> tr.to.runtimeRulesSet.any { it.isEmptyRule }.not() }
                if (transitions.isEmpty()) {
                    this.rememberForErrorComputation(toProcess) // remember for finding error info, think this never happens
                } else {
                    for (it in transitions) {
                        when (it.action) {
                            Transition.ParseAction.WIDTH -> doWidth(toProcess, it, true)
                            else -> this.rememberForErrorComputation(toProcess) // remember for finding error info, think this never happens
                        }
                    }
                }
            }
            else -> {
                    val transitions = toProcess.growingNode.runtimeState.transitions(toProcess.previous!!.runtimeState)
                        .filter { it.to.runtimeRulesSet.any { it.isEmptyRule }.not() }
                    if (transitions.isEmpty()) {
                        this.rememberForErrorComputation(toProcess) // remember for finding error info, think this never happens
                    } else {
                        for (it in transitions) {
                            when (it.action) {
                                Transition.ParseAction.WIDTH -> doWidth(toProcess, it, true)
                                else -> this.rememberForErrorComputation(toProcess) // remember for finding error info
                            }
                        }
                    }
            }
        }
    }
     */
/*
    internal fun growHeightOrGraftOnly(toProcess: ParseGraph.Companion.NextToProcess) {
            val transitions = toProcess.growingNode.runtimeState.transitions(toProcess.previous!!.runtimeState)
            for (transition in transitions) {
                when (transition.action) {
                    Transition.ParseAction.HEIGHT -> doHeight(toProcess, transition, true)
                    Transition.ParseAction.GRAFT -> {
                        if (transition.runtimeGuard(transition, toProcess.previous, toProcess.previous.runtimeState.state.rulePositions)) {
                            doGraft(toProcess, transition, true)
                        }
                    }
                }
            }
    }
*/
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
            this.graph._growingHeadHeap.forEach {
                val chains = it.index.chains()
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

    fun grow3(noLookahead: Boolean): Int {
        //this.startPass()
        var steps = 0
        val doneEmpties = mutableSetOf<ParserState>()
        val currentStartPosition = this.graph.nextHeadStartPosition
        while (this.graph.hasNextHead && this.graph.nextHeadStartPosition <= currentStartPosition) {
            this.cacheLastGrown()
            checkInterrupt()
            val graph = this.graph //TODO: remove..for debug only
            if (Debug.OUTPUT_RUNTIME) {
                println("$steps --------------------------------------")
                println(graph)
            }
            val nextToProcess = this.graph.nextToProcess()
            for(toProcess in nextToProcess) {
                if (toProcess.growingNode.isEmptyMatch && doneEmpties.contains(toProcess.growingNode.currentState)) {
                    //don't do it again
                    doneEmpties.add(toProcess.growingNode.currentState)
                } else {
                    // GRAFT drops previous so can't record it for error here
                    this.growNode(toProcess, noLookahead)
                    steps++
                }
            }
        }
        return steps
    }

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

    internal fun growNode(toProcess: ParseGraph.Companion.NextToProcess, noLookahead: Boolean) {
        when (toProcess.growingNode.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> when {
                toProcess.growingNode.currentState.isAtEnd -> graph.recordGoal(toProcess.growingNode)
                else -> this.growGoalNode(toProcess, noLookahead)
            }
            RuntimeRuleKind.TERMINAL -> this.growNormal(toProcess, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormal(toProcess, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormal(toProcess, noLookahead)
        }
    }

    private fun growGoalNode(toProcess: ParseGraph.Companion.NextToProcess, noLookahead: Boolean) {
        //no previous, so gn must be the Goal node
        val transitions = toProcess.growingNode.runtimeState.transitions(RuntimeState(stateSet.startState, setOf(LookaheadSet.EMPTY)), RuntimeState(stateSet.startState, setOf(LookaheadSet.EMPTY)))
        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.GOAL -> error("Should never happen")
                Transition.ParseAction.WIDTH -> doWidth(toProcess, it, noLookahead)
                Transition.ParseAction.HEIGHT -> error("Should never happen")
                Transition.ParseAction.GRAFT -> error("Should never happen")
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun growNormal(toProcess: ParseGraph.Companion.NextToProcess, noLookahead: Boolean) {
            val grown = this.growWithPrev(toProcess, noLookahead)
            if (grown) {
                //do nothing
            } else {
                graph.drop(toProcess)
            }
    }

    private fun growWithPrev(toProcess: ParseGraph.Companion.NextToProcess, noLookahead: Boolean): Boolean {
        var grown = false
        val prevPrev = toProcess.remainingHead?.runtimeState ?: RuntimeState(stateSet.startState, setOf(LookaheadSet.EMPTY))
        val transitions = toProcess.growingNode.runtimeState.transitions(prevPrev, toProcess.previous!!.runtimeState)
        //TODO: do we need to do something here? due to filtered out trans from the list
        val grouped = transitions.groupBy { it.to.runtimeRulesSet }
        for (it in grouped) {
            when {
                1 == it.value.size -> {
                    val tr = it.value[0]
                    val b = doAction(tr, toProcess, noLookahead)
                    grown = grown || b
                }
                else -> {
                    val trgs = it.value.filter { it.action == Transition.ParseAction.GRAFT }
                    val trhs = it.value.filter { it.action == Transition.ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            val b = doGraft(toProcess, trgs[i], noLookahead)
                            doneIt = doneIt || b
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            val b = doHeight(toProcess, trhs[i], noLookahead)
                            doneIt = doneIt || b
                            ++i
                        }
                        grown = grown || doneIt
                    } else {
                        for (tr in it.value) {
                            val b = doAction(tr, toProcess, noLookahead)
                            grown = grown || b
                        }
                    }
                }
            }
        }
        return grown
    }

    private fun doAction(tr: Transition, toProcess: ParseGraph.Companion.NextToProcess, noLookahead: Boolean): Boolean {
        return when (tr.action) {
            Transition.ParseAction.WIDTH -> doWidth(toProcess, tr, noLookahead)
            Transition.ParseAction.EMBED -> doEmbedded(toProcess, tr, noLookahead)
            Transition.ParseAction.HEIGHT -> doHeight(toProcess, tr, noLookahead)
            Transition.ParseAction.GRAFT -> doGraft(toProcess, tr, noLookahead)
            Transition.ParseAction.GOAL -> doGoal(toProcess, tr, noLookahead)
        }
    }

    private fun doGoal(toProcess: ParseGraph.Companion.NextToProcess, transition: Transition, noLookahead: Boolean):Boolean {
        return if (transition.runtimeGuard(transition, toProcess.previous!!, toProcess.previous.runtimeState.state)) {
            val lhWithMatch = toProcess.previous.runtimeState.runtimeLookaheadSet.filter {
                transition.lookahead.any { lh ->
                    this.graph.isLookingAt(lh.guard, it, toProcess.growingNode.nextInputPositionAfterSkip)
                }
            }
            val hasLh = lhWithMatch.isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
            if (noLookahead || hasLh) {
                val runtimeLhs = toProcess.previous.runtimeState.runtimeLookaheadSet
                this.graph.growNextChild(
                    toProcess,
                    newParentState = transition.to,
                    newParentRuntimeLookaheadSet = runtimeLhs
                )
            } else {
                false
            }
        } else {
            false
        }

    }

    private fun doWidth(toProcess: ParseGraph.Companion.NextToProcess, transition: Transition, noLookahead: Boolean): Boolean {
        rememberForErrorComputation(toProcess)
        val l = this.graph.input.findOrTryCreateLeaf(transition.to.firstRule, toProcess.growingNode.nextInputPosition)
        return if (null != l) {
                val lh = transition.lookahead.map { it.guard }.reduce { acc, e->acc.union(this.stateSet,e)} //TODO:reduce to 1 in SM
                val skipLh = if (lh.includesUP) {
                    toProcess.growingNode.runtimeLookahead.map { this.stateSet.createWithParent(lh, it) }.toSet()
                } else {
                    setOf(lh)
                }
                val skipData = this.tryParseSkipUntilNone(skipLh, l.nextInputPosition, noLookahead)
                val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: l.nextInputPosition

                //val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, curGn.runtimeLookahead, nextInput)
                val lhWithMatch = toProcess.growingNode.runtimeLookahead.filter {
                    this.graph.isLookingAt(lh, it, nextInputPositionAfterSkip)
                }
            val hasLh = lhWithMatch.isNotEmpty()
            if (noLookahead || hasLh) {
                //val runtimeLhs = lhWithMatch
                val startPosition = l.startPosition
                val nextInputPosition = l.nextInputPosition
                //this.graph.pushToStackOf(transition.to, runtimeLhs, startPosition, nextInputPosition, curGn, previous, skipData)
                this.graph.pushToStackOf(toProcess, transition.to, setOf(LookaheadSet.EMPTY), startPosition, nextInputPosition,  skipData)
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun doHeight(toProcess: ParseGraph.Companion.NextToProcess, transition: Transition, noLookahead: Boolean): Boolean {
        val lhWithMatch =  transition.lookahead.flatMap{ lh ->
            toProcess.previous!!.runtimeState.runtimeLookaheadSet.mapNotNull { rtLh ->
                val b = this.graph.isLookingAt(lh.guard, rtLh, toProcess.growingNode.nextInputPositionAfterSkip) //TODO: if(transition.lookaheadGuard.includesUP) {
                if(b) Pair(rtLh,lh.up) else null
            }
       }
        val hasLh = lhWithMatch.isNotEmpty()
        return if (noLookahead || hasLh) {
            val runtimeLhs = lhWithMatch.map { p ->
                val rtLh = p.first
                val up = p.second
                if (up.includesUP) {
                    this.stateSet.createWithParent(up,rtLh)
                } else {
                    up
                }
            }.toSet()
            this.graph.createWithFirstChild(
                parentState = transition.to,
                parentRuntimeLookaheadSet = runtimeLhs,
                toProcess = toProcess
            )
        } else {
            false
        }
    }

    private fun doGraft(toProcess: ParseGraph.Companion.NextToProcess, transition: Transition, noLookahead: Boolean): Boolean {
        return if (transition.runtimeGuard(transition, toProcess.previous!!, toProcess.previous.runtimeState.state)) {
            //val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, previous.runtimeLookaheadSet, curGn.nextInputPositionAfterSkip)
            val lhWithMatch = toProcess.previous.runtimeState.runtimeLookaheadSet.filter {
                transition.lookahead.any { lh ->
                    this.graph.isLookingAt(lh.guard, it, toProcess.growingNode.nextInputPositionAfterSkip)
                }
            }
            val hasLh = lhWithMatch.isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
            if (noLookahead || hasLh) {
                val runtimeLhs = toProcess.previous.runtimeState.runtimeLookaheadSet
                this.graph.growNextChild(
                    toProcess = toProcess,
                    newParentState = transition.to,
                    newParentRuntimeLookaheadSet = runtimeLhs
                )
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun tryParseSkipUntilNone(lookaheadSet: Set<LookaheadSet>, startPosition: Int, noLookahead: Boolean): TreeData? {
        val key = Pair(startPosition, lookaheadSet)
        return if (_skip_cache.containsKey(key)) {
            // can cache null as a valid result
            _skip_cache[key]
        } else {
            val skipData = when (skipParser) {
                null -> null
                else -> tryParseSkip(lookaheadSet, startPosition, noLookahead)
            }
            _skip_cache[key] = skipData
            skipData
        }
    }

    private fun tryParseSkip(lookaheadSet: Set<LookaheadSet>, startPosition: Int, noLookahead: Boolean): TreeData? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
        skipParser!!.reset()
        skipParser.start(startPosition, lookaheadSet)
        do {
            skipParser.grow3(noLookahead)
        } while (skipParser.graph.canGrow && (skipParser.graph.goals.isEmpty() || skipParser.graph.goalMatchedAll.not()))
        //TODO: get longest skip match
        return when {
            skipParser.graph.goals.isEmpty() -> null
            else -> {
                skipParser.graph.treeData
            }
        }
    }

    private fun doEmbedded(toProcess: ParseGraph.Companion.NextToProcess, transition: Transition, noLookahead: Boolean): Boolean {
        val embeddedRule = transition.to.runtimeRules.first() // should only ever be one
        val endingLookahead = transition.lookahead.first().guard //should ony ever be one
        val embeddedRuntimeRuleSet = embeddedRule.embeddedRuntimeRuleSet ?: error("Should never be null")
        val embeddedStartRule = embeddedRule.embeddedStartRule ?: error("Should never be null")
        val embeddedS0 = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedStartRule, this.stateSet.automatonKind).startState
        val embeddedSkipStateSet = embeddedRuntimeRuleSet.skipParserStateSet
        val embeddedParser = RuntimeParser(embeddedS0.stateSet, embeddedSkipStateSet, embeddedStartRule, this.input)
        val startPosition = toProcess.growingNode.nextInputPosition
        val skipTerms = this.stateSet.runtimeRuleSet.firstSkipTerminals.toSet()
        val embeddedPossibleEOT = endingLookahead.unionContent(embeddedS0.stateSet,skipTerms)
        val embeddedEOT = toProcess.growingNode.runtimeLookahead.map { this.stateSet.createWithParent(embeddedPossibleEOT, it) }.toSet()
        embeddedParser.start(startPosition, embeddedEOT)
        var seasons = 1
        var maxNumHeads = embeddedParser.graph.numberOfHeads
        do {
            embeddedParser.grow3(false)
            seasons++
            maxNumHeads = max(maxNumHeads, embeddedParser.graph.numberOfHeads)
        } while (embeddedParser.graph.canGrow && (embeddedParser.graph.goals.isEmpty() || embeddedParser.graph.goalMatchedAll.not()))
        //val match = embeddedParser.longestMatch(seasons, maxNumHeads, true) as SPPTBranch?
        val match = embeddedParser.graph.treeData
        return if (match.root != null) {
            val ni = match.nextInputPosition!! // will always have value if root not null
            //TODO: parse skipNodes
            val skipLh = toProcess.growingNode.runtimeLookahead.map { this.stateSet.createWithParent(endingLookahead, it) }.toSet()
            val skipData = this.tryParseSkipUntilNone(skipLh, ni, noLookahead)//, lh) //TODO: does the result get reused?
            val nextInput = skipData?.nextInputPosition ?: ni
            this.graph.pushEmbeddedToStackOf(toProcess,transition.to, toProcess.growingNode.runtimeLookahead, startPosition, nextInput, match, skipData)
        } else {
            //  could not parse embedded
            false
        }
    }

}