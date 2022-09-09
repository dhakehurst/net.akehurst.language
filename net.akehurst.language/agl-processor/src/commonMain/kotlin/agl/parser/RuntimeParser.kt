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
import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.*
import net.akehurst.language.agl.runtime.structure.RuleOption
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserInterruptedException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.util.addIfNotNull
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

    var lastGrown: Set<ParseGraph.Companion.ToProcessTriple> = mutableSetOf()
    val canGrow: Boolean get() = this.graph.canGrow

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
            val endOfSkipLookaheadSet = this.stateSet.createLookaheadSet(skipLhsp)
            this.tryParseSkipUntilNone(setOf(endOfSkipLookaheadSet), startPosition, false) //TODO: think this might allow some wrong things, might be a better way
        }
        val runtimeLookahead = setOf(LookaheadSet.EOT)
        val gn = this.graph.start(gState, startPosition, runtimeLookahead, initialSkipData) //TODO: remove LH
        this.addToLastGrown(gn)
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


    fun resetGraphToLastGrown(lastGrown:Set<ParseGraph.Companion.ToProcessTriple>) {

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

    fun rememberForErrorComputation(toProcess: ParseGraph.Companion.ToProcessTriple) {
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

    fun resetLastGrown() {
        (this.lastGrown as MutableSet).clear()
    }
    fun addToLastGrown(head:GrowingNode) {
        val triples = this.graph.peekTripleFor(head)
        (this.lastGrown as MutableSet).addAll(triples)
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
    */
    // used for finding error info


    // used for finding error info
    /*
    internal fun growWidthOnly(toProcess: ParseGraph.Companion.ToProcessTriple) {
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

    fun grow3(possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Pair<Int,Set<ParseGraph.Companion.ToProcessTriple>> {
        //this.startPass()
        val lg = this.lastGrown.toMutableSet()
        this.resetLastGrown()
        var steps = 0
        val doneEmpties = mutableSetOf<ParserState>()
        val currentStartPosition = this.graph.nextHeadStartPosition
        while (this.graph.hasNextHead && this.graph.nextHeadStartPosition <= currentStartPosition) {
            checkInterrupt()
            val graph = this.graph //TODO: remove..for debug only
            if (Debug.OUTPUT_RUNTIME) {
                println("$steps --------------------------------------")
                println(graph)
            }
            val toProcess = this.graph.nextToProcess()
            if (toProcess.growingNode.isEmptyMatch && doneEmpties.contains(toProcess.growingNode.currentState)) {
                //don't do it again
                doneEmpties.add(toProcess.growingNode.currentState)
            } else {
                this.growNode(toProcess, possibleEndOfText, noLookahead)
                steps++
            }
            if (steps > 1000) { //FIXME: make this value part of configuration
                this.interrupt(Message.PARSER_WONT_STOP)
            }
        }
        return Pair(steps,lg)
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

    private fun growNode(toProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean) {
        when (toProcess.growingNode.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> when {
                toProcess.growingNode.currentState.isAtEnd -> graph.recordGoal(toProcess.growingNode)
                else -> this.growGoalNode(toProcess, possibleEndOfText, noLookahead)
            }

            RuntimeRuleKind.TERMINAL -> this.growNormal(toProcess, possibleEndOfText, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormal(toProcess, possibleEndOfText, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormal(toProcess, possibleEndOfText, noLookahead)
        }
    }

    private fun growGoalNode(nextToProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean) {
        //no previous, so gn must be the Goal node
        val toProcess = ParseGraph.Companion.ToProcessTriple(nextToProcess.growingNode, null, null)
        val transitions = toProcess.growingNode.runtimeState.transitions(stateSet.startState, stateSet.startState)
        if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Choices:\n${transitions.joinToString(separator = "\n") { "  $it" }}" }
        var grown = false
        for (it in transitions) {
            val g = when (it.action) {
                Transition.ParseAction.GOAL -> error("Should never happen")
                Transition.ParseAction.WIDTH -> doWidth(toProcess, it, possibleEndOfText, noLookahead)
                Transition.ParseAction.HEIGHT -> error("Should never happen")
                Transition.ParseAction.GRAFT -> error("Should never happen")
                Transition.ParseAction.EMBED -> TODO()
            }
            grown = grown || g
        }
        if (grown) {
            // must have grown
        } else {
            val hd = toProcess.remainingHead
            val dd = nextToProcess.growingNode.index
            hd?.let { graph.dropHead(hd) }
            graph.dropData(dd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Head: $hd" }
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Data: $dd" }
        }
    }

    private fun growNormal(nextToProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean) {
        val toDropData = mutableSetOf<GrowingNodeIndex>()
        val toDropHead = mutableSetOf<GrowingNodeIndex>()
        var grown = false
        for (toProcess in nextToProcess.triples) {
            val g = this.growWithPrev(toProcess, possibleEndOfText, noLookahead,false)
            if (g) {
                grown = true
            } else {
                //toDropHead.addIfNotNull(toProcess.remainingHead)
                //toDropData.addIfNotNull(toProcess.previous)
                //toDropData.addIfNotNull(toProcess.remainingHead)
            }
        }
        if (grown.not()) {
            //nothing was grown, so drop data for growingHead also
            toDropData.add(nextToProcess.growingNode.index)
            //TODO: maybe also drop other things!
        } else {
            // growingHead must have grown with one of the previous
        }
        for (hd in toDropHead) {
            graph.dropHead(hd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Head: $hd" }
        }
        for (dd in toDropData) {
            graph.dropData(dd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Data: $dd" }
        }
    }

    private fun growWithPrev(toProcess: ParseGraph.Companion.ToProcessTriple, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean, heightGraftOnly:Boolean): Boolean {
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
                    val b = doAction(tr, toProcess, possibleEndOfText, noLookahead,heightGraftOnly)
                    grown = grown || b
                }

                else -> {
                    val trgs = it.value.filter { it.action == Transition.ParseAction.GRAFT }
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
                    val trhs = it.value.filter { it.action == Transition.ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            val b = doGraft(toProcess, trgs[i], possibleEndOfText, noLookahead)
                            doneIt = doneIt || b
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            val b = doHeight(toProcess, trhs[i], possibleEndOfText, noLookahead)
                            doneIt = doneIt || b
                            ++i
                        }
                        grown = grown || doneIt
                    } else {
                        for (tr in it.value) {
                            val b = doAction(tr, toProcess, possibleEndOfText, noLookahead, heightGraftOnly)
                            grown = grown || b
                        }
                    }
                }
            }
        }
        return grown
    }

    private fun doAction(tr: Transition, toProcess: ParseGraph.Companion.ToProcessTriple, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean, heightGraftOnly:Boolean): Boolean {
        return when(heightGraftOnly) {
            true -> when (tr.action) {
                Transition.ParseAction.WIDTH -> false
                Transition.ParseAction.EMBED -> false
                Transition.ParseAction.HEIGHT -> doHeight(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.GRAFT -> doGraft(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.GOAL -> doGoal(toProcess, tr, possibleEndOfText, noLookahead)
            }
            false -> when (tr.action) {
                Transition.ParseAction.WIDTH -> doWidth(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.EMBED -> doEmbedded(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.HEIGHT -> doHeight(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.GRAFT -> doGraft(toProcess, tr, possibleEndOfText, noLookahead)
                Transition.ParseAction.GOAL -> doGoal(toProcess, tr, possibleEndOfText, noLookahead)
            }
        }
    }

    // Use previous runtimeLookahead
    private fun doGoal(toProcess: ParseGraph.Companion.ToProcessTriple, transition: Transition, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Boolean {
        return if (transition.runtimeGuard(transition, toProcess.previous!!, toProcess.previous.runtimeState.state)) {
            val runtimeLhs = toProcess.growingNode.runtimeState.runtimeLookaheadSet
            val lhWithMatch = transition.lookahead.filter { lh -> //TODO: should always be only one I think!!
                possibleEndOfText.any { eot ->
                    runtimeLhs.any { rt ->
                        this.graph.isLookingAt(lh.guard, eot, rt, toProcess.growingNode.nextInputPositionAfterSkip)
                    }
                }
            }
            val hasLh = lhWithMatch.isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
            if (noLookahead || hasLh) {
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Taking: $transition" }
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

    // Use current/growing runtimeLookahead
    private fun doWidth(toProcess: ParseGraph.Companion.ToProcessTriple, transition: Transition, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Boolean {
        rememberForErrorComputation(toProcess)
        val l = this.graph.input.findOrTryCreateLeaf(transition.to.firstRule, toProcess.growingNode.nextInputPosition)
        return if (null != l) {
            val lh = transition.lookahead.map { it.guard }.reduce { acc, e -> acc.union(this.stateSet, e) } //TODO:reduce to 1 in SM
            val runtimeLhs = toProcess.growingNode.runtimeState.runtimeLookaheadSet

            val skipData = if (null == skipParser) {
                //this is a skipParser, so no skipData
                null
            } else {
                val skipLh = if (lh.includesEOT || lh.includesRT) {
                    //possibleEndOfText.map { eot -> this.stateSet.createWithParent(lh, eot) }.toSet()
                    runtimeLhs.flatMap { rt -> possibleEndOfText.map { eot -> this.stateSet.createLookaheadSet(lh.resolve(eot, rt)) } }.toSet()
                } else {
                    setOf(lh)
                }
                this.tryParseSkipUntilNone(skipLh, l.nextInputPosition, noLookahead)
            }
            val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: l.nextInputPosition

            // val lhWithMatch = possibleEndOfText.filter {
            //     this.graph.isLookingAt(lh, it, nextInputPositionAfterSkip)
            // }
            val hasLh = possibleEndOfText.any { eot ->
                runtimeLhs.any { rt ->
                    this.graph.isLookingAt(lh, eot, rt, nextInputPositionAfterSkip)
                }
            }
            if (noLookahead || hasLh) {
                //val runtimeLhs = lhWithMatch
                val startPosition = l.startPosition
                val nextInputPosition = l.nextInputPosition
                //this.graph.pushToStackOf(transition.to, runtimeLhs, startPosition, nextInputPosition, curGn, previous, skipData)
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Taking: $transition" }
                val newHead = this.graph.pushToStackOf(toProcess, transition.to, setOf(LookaheadSet.EMPTY), startPosition, nextInputPosition, skipData)
                return if(newHead==null) {
                    false
                } else {
                    this.addToLastGrown(newHead)
                    true
                }
            } else {
                false
            }
        } else {
            false
        }
    }

    // Use previous runtimeLookahead
    private fun doHeight(toProcess: ParseGraph.Companion.ToProcessTriple, transition: Transition, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Boolean {
        val runtimeLhs = toProcess.previous!!.runtimeState.runtimeLookaheadSet
        val lhWithMatch = transition.lookahead.flatMap { lh ->
            possibleEndOfText.flatMap { eot ->
                runtimeLhs.mapNotNull { rt ->
                    val b = this.graph.isLookingAt(lh.guard, eot, rt, toProcess.growingNode.nextInputPositionAfterSkip) //TODO: if(transition.lookaheadGuard.includesUP) {
                    if (b) Triple(eot, rt, lh.up) else null
                }
            }
        }
        val hasLh = lhWithMatch.isNotEmpty()
        return if (noLookahead || hasLh) { //TODO: don't resolve EOT
            val newRuntimeLhs = lhWithMatch.map { t ->
                val eot = t.first
                val rt = t.second
                val up = t.third
                if (up.includesRT) {
                    //this.stateSet.createWithParent(up, rt)
                    this.stateSet.createLookaheadSet(up.resolve(eot, rt))
                } else {
                    up
                }
            }.toSet()
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Taking: $transition" }
            this.graph.createWithFirstChild(
                parentState = transition.to,
                parentRuntimeLookaheadSet = newRuntimeLhs,
                toProcess = toProcess
            )
        } else {
            false
        }
    }

    // Use previous runtimeLookahead
    private fun doGraft(toProcess: ParseGraph.Companion.ToProcessTriple, transition: Transition, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Boolean {
        return if (transition.runtimeGuard(transition, toProcess.previous!!, toProcess.previous.runtimeState.state)) {
            val runtimeLhs = toProcess.previous.runtimeState.runtimeLookaheadSet
            val lhWithMatch = transition.lookahead.filter { lh -> //TODO: should always be only one I think!!
                possibleEndOfText.any { eot ->
                    runtimeLhs.any { rt ->
                        this.graph.isLookingAt(lh.guard, eot, rt, toProcess.growingNode.nextInputPositionAfterSkip)
                    }
                }
            }
            val hasLh = lhWithMatch.isNotEmpty()//TODO: if(transition.lookaheadGuard.includesUP) {
            if (noLookahead || hasLh) {
                if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Taking: $transition" }
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

    private fun tryParseSkipUntilNone(possibleEndOfSkip: Set<LookaheadSet>, startPosition: Int, noLookahead: Boolean): TreeData? {
        val key = Pair(startPosition, possibleEndOfSkip)
        return if (_skip_cache.containsKey(key)) {
            // can cache null as a valid result
            _skip_cache[key]
        } else {
            val skipData = when (skipParser) { //TODO: raise test to outer level
                null -> null
                else -> tryParseSkip(possibleEndOfSkip, startPosition, noLookahead)
            }
            _skip_cache[key] = skipData
            skipData
        }
    }

    private fun tryParseSkip(possibleEndOfSkip: Set<LookaheadSet>, startPosition: Int, noLookahead: Boolean): TreeData? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
        skipParser!!.reset()
        skipParser.start(startPosition, possibleEndOfSkip)
        do {
            skipParser.grow3(possibleEndOfSkip, noLookahead)
        } while (skipParser.graph.canGrow && (skipParser.graph.goals.isEmpty() || skipParser.graph.goalMatchedAll.not()))
        //TODO: get longest skip match
        return when {
            skipParser.graph.goals.isEmpty() -> null
            else -> {
                skipParser.graph.treeData
            }
        }
    }

    // Use current/growing runtimeLookahead
    private fun doEmbedded(toProcess: ParseGraph.Companion.ToProcessTriple, transition: Transition, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Boolean {
        val embeddedRule = transition.to.runtimeRules.first() // should only ever be one
        val endingLookahead = transition.lookahead.first().guard //should ony ever be one
        val embeddedRuntimeRuleSet = embeddedRule.embeddedRuntimeRuleSet ?: error("Should never be null")
        val embeddedStartRule = embeddedRule.embeddedStartRule ?: error("Should never be null")
        val embeddedS0 = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedStartRule, this.stateSet.automatonKind).startState
        val embeddedSkipStateSet = embeddedRuntimeRuleSet.skipParserStateSet
        val embeddedParser = RuntimeParser(embeddedS0.stateSet, embeddedSkipStateSet, embeddedStartRule, this.input)
        val startPosition = toProcess.growingNode.nextInputPosition
        // Embedded text could end with this.skipTerms or lh from transition
        val skipTerms = this.stateSet.runtimeRuleSet.firstSkipTerminals.toSet()
        val embeddedPossibleEOT = endingLookahead.unionContent(embeddedS0.stateSet, skipTerms)
        val embeddedEOT = toProcess.growingNode.runtimeLookahead.flatMap { rt ->
            possibleEndOfText.map { eot ->
                embeddedPossibleEOT.resolve(eot, rt).lhs(embeddedS0.stateSet)
            }
        }.toSet() //FIXME: use resolve

        embeddedParser.start(startPosition, embeddedEOT)
        var seasons = 1
        var maxNumHeads = embeddedParser.graph.numberOfHeads
        do {
            embeddedParser.grow3(embeddedEOT, false)
            seasons++
            maxNumHeads = max(maxNumHeads, embeddedParser.graph.numberOfHeads)
        } while (embeddedParser.graph.canGrow && (embeddedParser.graph.goals.isEmpty() || embeddedParser.graph.goalMatchedAll.not()))
        //val match = embeddedParser.longestMatch(seasons, maxNumHeads, true) as SPPTBranch?
        val match = embeddedParser.graph.treeData
        return if (match.root != null) {
            val ni = match.nextInputPosition!! // will always have value if root not null
            //TODO: parse skipNodes
            val skipLh = toProcess.growingNode.runtimeLookahead.flatMap { rt ->
                possibleEndOfText.map { eot ->
                    endingLookahead.resolve(eot, rt).lhs(embeddedS0.stateSet)
                }
            }.toSet()
            val skipData = this.tryParseSkipUntilNone(skipLh, ni, noLookahead)//, lh) //TODO: does the result get reused?
            val nextInput = skipData?.nextInputPosition ?: ni
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Taking Transition: $transition" }
            val newHead = this.graph.pushEmbeddedToStackOf(toProcess, transition.to, toProcess.growingNode.runtimeLookahead, startPosition, nextInput, match, skipData)
            return if(newHead==null) {
                false
            } else {
                this.addToLastGrown(newHead)
                true
            }
        } else {
            //  could not parse embedded
            false
        }
    }

    fun tryGrowHeightOrGraft(possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean): Set<ParseGraph.Companion.NextToProcess> {
        val lg = mutableSetOf<ParseGraph.Companion.NextToProcess>()
        val currentStartPosition = this.graph.nextHeadStartPosition
        while (this.graph.hasNextHead && this.graph.nextHeadStartPosition <= currentStartPosition) {
            //while ((this.canGrow && this.graph.goals.isEmpty())) {
            val nextToProcess = this.graph.nextToProcess()
            checkInterrupt()
            val grown = this.growNormalHeightOrGraftOnly(nextToProcess,possibleEndOfText,noLookahead)
            if (grown.not()) {
                lg.add(nextToProcess)
            }
        }
        return lg
    }

    private fun growNormalHeightOrGraftOnly(nextToProcess: ParseGraph.Companion.NextToProcess, possibleEndOfText: Set<LookaheadSet>, noLookahead: Boolean):Boolean {
        val toDropData = mutableSetOf<GrowingNodeIndex>()
        val toDropHead = mutableSetOf<GrowingNodeIndex>()
        var grown = false
        for (toProcess in nextToProcess.triples) {
            val g = this.growWithPrev(toProcess, possibleEndOfText, noLookahead, true)
            if (g) {
                grown = true
            } else {
                //toDropHead.addIfNotNull(toProcess.remainingHead)
                //toDropData.addIfNotNull(toProcess.previous)
                //toDropData.addIfNotNull(toProcess.remainingHead)
            }
        }
        if (grown.not()) {
            //nothing was grown, so drop data for growingHead also
            toDropData.add(nextToProcess.growingNode.index)
            //TODO: maybe also drop other things!

        } else {
            // growingHead must have grown with one of the previous
        }
        for (hd in toDropHead) {
            graph.dropHead(hd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Head: $hd" }
        }
        for (dd in toDropData) {
            graph.dropData(dd)
            if (Debug.OUTPUT_RUNTIME) Debug.debug(Debug.IndentDelta.NONE) { "Dropped Data: $dd" }
        }
        return grown
    }

}