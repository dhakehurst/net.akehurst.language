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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.*
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuleOption
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserInterruptedException
import net.akehurst.language.api.sppt.SPPTNode
import kotlin.math.max

internal class RuntimeParser(
    internal val stateSet: ParserStateSet,
    private val skipStateSet: ParserStateSet?, // null if this is a skipParser
    val userGoalRule: RuntimeRule,
    //possibleEndOfText_: LookaheadSet,
    private val input: InputFromString
) {
    companion object {
        val defaultStartLocation = InputLocation(0, 0, 1, 0)
    }

    val graph = ParseGraph(input, this.stateSet.number, this.stateSet.usedTerminalRules.size, this.stateSet.usedNonTerminalRules.size)

    //var possibleEndOfText = possibleEndOfText_; private set

    //needs to be public so that expectedAt can use it
    val lastGrown: Collection<GrowingNode>
        get() {
            return setOf<GrowingNode>().union(this.graph.growing.values).union(this.grownInLastPass)
        }
    val lastGrownLinked: Collection<GrowingNode>
        get() {
            this.grownInLastPass.forEach { gn ->
                this.grownInLastPassPrevious[gn]!!.forEach {
                    gn.addPrevious(it)
                }
            }
            return this.grownInLastPass
        }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }


    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    private var grownInLastPass = mutableListOf<GrowingNode>()
    internal var grownInLastPassPrevious = mutableMapOf<GrowingNode, Collection<PreviousInfo>>()
    private var interruptedMessage: String? = null

    private val readyForShift = mutableListOf<GrowingNode>()
    private val readyForShiftPrevious = mutableMapOf<GrowingNode, MutableSet<PreviousInfo>>()

    fun reset() {//possibleEndOfText: LookaheadSet) {
        this.graph.reset()
        //this.possibleEndOfText = possibleEndOfText
    }

    fun start(startPosition: Int, possibleEndOfText: LookaheadSet) {
        val gState = stateSet.startState
        val initialSkipData = if (this.stateSet.isSkip) {
            null
        } else {
            val skipLhc = gState.rulePositions.flatMap { this.stateSet.buildCache.firstOf(it, LookaheadSet.EOT) }.toSet()
            val endOfSkipLookaheadSet = this.stateSet.createLookaheadSet(skipLhc)
            this.tryParseSkipUntilNone(endOfSkipLookaheadSet, startPosition,true) //TODO: think this might allow some wrong things, might be a better way
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

    fun resetGraphToLastGrown() {
        for (gn in this.lastGrownLinked) {
            this.graph.addGrowingHead(gn)
        }
    }

    fun startPass() {
        this.grownInLastPassPrevious.clear()
        this.grownInLastPass.clear()
    }

    //to find error locations
    fun tryGrowWidthOnce() {
        this.startPass()
        val currentMaxNextInputPosition = this.graph.growingHeadMaxNextInputPosition
        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentMaxNextInputPosition) {
            val gn = this.graph.nextHead()
            checkInterrupt()
            val previous = this.graph.pop(gn)
            //store gn so we can use ti to determine errors
            this.grownInLastPass.add(gn)
            this.grownInLastPassPrevious[gn] = previous
            //val merged = this.graph.tryMergeWithExistingHead(gn, previous)
            //if (null == merged)

            this.growWidthOnly(gn, previous)
        }
    }

    fun tryGrowHeightOrGraft(): Set<GrowingNode> {
        this.startPass()
        // try height or graft
        while ((this.canGrow && this.graph.goals.isEmpty())) {
            val gn = this.graph.nextHead()
            checkInterrupt()
            val previous = this.graph.pop(gn)
            //store gn so we can use ti to determine errors
            this.grownInLastPass.add(gn)
            this.grownInLastPassPrevious[gn] = previous
            //val merged = this.graph.tryMergeWithExistingHead(gn, previous)
            //if (null == merged)

            this.growHeightOrGraftOnly(gn, previous)
        }
        return this.lastGrownLinked.toSet()
    }

    internal fun growWidthOnly(gn: GrowingNode, previous: Collection<PreviousInfo>) {
        when (gn.runtimeRules.first().kind) { //FIXME
            RuntimeRuleKind.GOAL -> {
                val transitions = gn.currentState.transitions(null)
                    .filter { tr -> tr.to.runtimeRulesSet.any { it.isEmptyRule }.not() }
                for (it in transitions) {
                    when (it.action) {
                        Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it, true)
                    }
                }
            }
            else -> {
                for (prev in previous) {
                    val transitions = gn.currentState.transitions(prev.node.currentState)
                        .filter { it.to.runtimeRulesSet.any { it.isEmptyRule }.not() }
                    for (it in transitions) {
                        when (it.action) {
                            Transition.ParseAction.WIDTH -> doWidth(gn, setOf(prev), it, true)
                        }
                    }
                }
            }
        }
    }

    internal fun growHeightOrGraftOnly(gn: GrowingNode, previous: Collection<PreviousInfo>) {
        for (prev in previous) {
            val transitions = gn.currentState.transitions(prev.node.currentState)
            for (transition in transitions) {
                when (transition.action) {
                    Transition.ParseAction.HEIGHT -> doHeight(gn, prev, transition, true)
                    Transition.ParseAction.GRAFT -> {
                        if (transition.runtimeGuard(transition, prev.node, prev.node.currentState.rulePositions)) {
                            doGraft(gn, prev, transition, true)
                        }
                    }
                }
            }
        }
    }

    /*
    fun grow(noLookahead: Boolean) {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.toGrowPrevious.clear()
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            checkInterrupt()
            val previous = this.graph.pop(gn)
            this.toGrowPrevious[gn] = previous
            this.growNode(gn, previous, noLookahead, Transition.ParseAction.values().toSet())
        }
    }
    */

    fun grow3(noLookahead: Boolean): Int {
        this.startPass()
        var steps = 0
        val doneEmpties = mutableSetOf<ParserState>()
        val currentMaxNextInputPosition = this.graph.growingHeadMaxNextInputPosition
        while (this.graph.hasNextHead && this.graph.nextHeadNextInputPosition <= currentMaxNextInputPosition) {
            checkInterrupt()
            val gn = this.graph.nextHead()
            if (gn.isEmptyMatch && doneEmpties.contains(gn.currentState)) {
                //don't do it again
            } else {
                if (gn.isEmptyMatch) doneEmpties.add(gn.currentState)
                val previous = this.graph.pop(gn)
                //store gn so we can use ti to determine errors
                this.grownInLastPass.add(gn)
                this.grownInLastPassPrevious[gn] = previous
                //val merged = this.graph.tryMergeWithExistingHead(gn, previous)
                //if (null == merged)

                this.growNode(gn, previous, noLookahead)
                steps++
            }
        }
        return steps
    }
    /*
    fun grow2(noLookahead: Boolean): Int {
        var growStep = 0

        this.toGrow = this.graph.growingHead.toList() //Note: this should be a copy of the list of values
        this.toGrowPrevious.clear()
        this.graph.growingHead.clear()
        // first grow each head...should only require shift actions
        for (gn in this.toGrow) {
            checkInterrupt()
            val previous = this.graph.pop(gn)
            this.toGrowPrevious[gn] = previous
            val merged = this.graph.tryMergeWithExistingHead(gn,previous)
            if(null==merged) this.growNodeShift(gn, previous, noLookahead)
        }
        growStep++

        //TODO: to improve efficiency, get this class to store growingHeads and get ParseGraph methods to return grown node

        // graph.growingHead will become empty if node is not grown.
        // Within growNodeReduce, a node is not grown if it next wants a shift action,
        // rather it is put onto the wait or 'readyForShift' list
        while (this.graph.growingHead.isNotEmpty()) {
            // growing heads should now be ready to height or graft, just pass on any that are shift actions
            this.toGrow = this.graph.growingHead.toList() //Note: this should be a copy of the list of values
            this.toGrowPrevious.clear()
            this.graph.growingHead.clear()
            for (gn in this.toGrow) {
                checkInterrupt()
                val previous = this.graph.pop(gn)
                this.toGrowPrevious[gn] = previous
                val merged = this.graph.tryMergeWithExistingHead(gn,previous)
                if(null==merged) this.growNodeReduce(gn, previous, noLookahead)
            }
            growStep++
        }
        // restore nodes 'readyForShift' into growingHead
        for (gn in this.readyForShift) {
            this.readyForShiftPrevious[gn]!!.forEach { gn.addPrevious(it) } //TODO: not efficient
            val gnindex = GrowingNode.indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)
            graph.growingHead[gnindex] = gn
        }
        this.readyForShift.clear()
        this.readyForShiftPrevious.clear()
        return growStep
    }
     */
/*
    internal fun longestMatch(seasons: Int, maxNumHeads: Int, embedded: Boolean): SPPTNode? {
        return if (!this.graph.goals.isEmpty() && this.graph.goals.size >= 1) {
            var lt = this.graph.goals.iterator().next()
            for (gt in this.graph.goals) {
                if (gt.matchedTextLength > lt.matchedTextLength) {
                    lt = gt
                }
            }
            if (embedded) {
                useGoal(lt) // next token is from the parent grammar, no need to check anything
            } else {
                when {
                    this.input.isEnd(lt.nextInputPosition + 1).not() -> {
                        val location = this.input.locationFor(lt.nextInputPosition - 1, 1)
                        throw ParseFailedException(
                            "Goal does not match full text",
                            SPPTFromTreeData(this.graph.treeData, this.input, seasons, maxNumHeads),
                            location,
                            emptySet(),
                            this.input.contextInText(location.position)
                        )
                    }
                    else -> {
                        useGoal(lt)
                    }
                }
            }
        } else {
            null;
        }
    }
*/
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


    internal fun growNode(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        when (gn.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> this.growGoalNode(gn, noLookahead)
            RuntimeRuleKind.TERMINAL -> this.growNormal(gn, previous, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormal(gn, previous, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormal(gn, previous, noLookahead)
        }
    }

    internal fun growNodeShift(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        when (gn.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> this.growGoalNode(gn, noLookahead)
            RuntimeRuleKind.TERMINAL -> this.growNormalShift(gn, previous, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormalShift(gn, previous, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormalShift(gn, previous, noLookahead)
        }
    }

    internal fun growNodeReduce(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        when (gn.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> growGoalNodeReduce(gn, noLookahead)
            RuntimeRuleKind.TERMINAL -> this.growNormalReduce(gn, previous, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormalReduce(gn, previous, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormalReduce(gn, previous, noLookahead)
        }
    }

    private fun growGoalNode(gn: GrowingNode, noLookahead: Boolean) {
        //no previous, so gn must be the Goal node
        val transitions = gn.currentState.transitions(null)
        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.GOAL -> doGoal(gn)
                Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it, noLookahead)
                Transition.ParseAction.HEIGHT -> error("Should never happen")
                Transition.ParseAction.GRAFT -> error("Should never happen")
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun growGoalNodeReduce(gn: GrowingNode, noLookahead: Boolean) {
        //no previous, so gn must be the Goal node
        val transitions = gn.currentState.transitions(null)
        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.GOAL -> doGoal(gn)
                Transition.ParseAction.WIDTH -> error("Should never happen")
                Transition.ParseAction.HEIGHT -> error("Should never happen")
                Transition.ParseAction.GRAFT -> error("Should never happen")
                Transition.ParseAction.EMBED -> error("Should never happen")
            }
        }
    }

    private fun growNormal(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        for (prev in previous) {
            this.growWithPrev(gn, prev, noLookahead)
        }
    }

    private fun growNormalShift(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        for (prev in previous) {
            this.growWithPrevShift(gn, prev, noLookahead)
        }
    }

    private fun growNormalReduce(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        for (prev in previous) {
            this.growWithPrevReduce(gn, prev, noLookahead)
        }
    }

    private fun growWithPrev(gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        val transitions = gn.currentState.transitions(previous.node.currentState)
        //TODO: do we need to do something here? due to filtered out trans from the list
        val grouped = transitions.groupBy { it.to.runtimeRulesSet }
        for (it in grouped) {
            when {
                1 == it.value.size -> {
                    val tr = it.value[0]
                    doAction(tr, gn, previous, noLookahead)
                }
                else -> {
                    val trgs = it.value.filter { it.action == Transition.ParseAction.GRAFT }
                    val trhs = it.value.filter { it.action == Transition.ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            doneIt = doGraft(gn, previous, trgs[i], noLookahead)
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            doneIt = doHeight(gn, previous, trhs[i], noLookahead)
                            ++i
                        }
                    } else {
                        for (tr in it.value) {
                            doAction(tr, gn, previous, noLookahead)
                        }
                    }
                }
            }
        }
    }

    private fun growWithPrevShift(gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        val transitions = gn.currentState.transitions(previous.node.currentState)
        val shiftTrans = transitions.filter { it.action == Transition.ParseAction.WIDTH || it.action == Transition.ParseAction.EMBED }
        //TODO: do we need to do something here? due to filtered out trans from the list
        val grouped = shiftTrans.groupBy { it.to.runtimeRulesSet }
        for (it in grouped) {
            when {
                1 == it.value.size -> {
                    val tr = it.value[0]
                    doActionShift(tr, gn, previous, noLookahead)
                }
                else -> {
                    val trgs = it.value.filter { it.action == Transition.ParseAction.GRAFT }
                    val trhs = it.value.filter { it.action == Transition.ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            doneIt = doGraft(gn, previous, trgs[i], noLookahead)
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            doneIt = doHeight(gn, previous, trhs[i], noLookahead)
                            ++i
                        }
                    } else {
                        for (tr in it.value) {
                            doActionShift(tr, gn, previous, noLookahead)
                        }
                    }
                }
            }
        }
    }

    private fun growWithPrevReduce(gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        val transitions = gn.currentState.transitions(previous.node.currentState)
        val reduceTrans = transitions.filter { it.action == Transition.ParseAction.HEIGHT || it.action == Transition.ParseAction.GRAFT }
        // if removed some shift actions then node ready for shift
        if (reduceTrans.size < transitions.size) {
            nodeReadyForShift(gn, previous)
        }
        val grouped = reduceTrans.groupBy { it.to.runtimeRulesSet }
        for (it in grouped) {
            when {
                1 == it.value.size -> {
                    val tr = it.value[0]
                    doActionReduce(tr, gn, previous, noLookahead)
                }
                else -> {
                    val trgs = it.value.filter { it.action == Transition.ParseAction.GRAFT }
                    val trhs = it.value.filter { it.action == Transition.ParseAction.HEIGHT }
                    if (trgs.isNotEmpty() && trhs.isNotEmpty()) {
                        var doneIt = false
                        var i = 0
                        while (doneIt.not() && i < trgs.size) {
                            doneIt = doGraft(gn, previous, trgs[i], noLookahead)
                            ++i
                        }
                        i = 0
                        while (doneIt.not() && i < trhs.size) {
                            doneIt = doHeight(gn, previous, trhs[i], noLookahead)
                            ++i
                        }
                    } else {
                        for (tr in it.value) {
                            doActionReduce(tr, gn, previous, noLookahead)
                        }
                    }
                }
            }
        }

    }

    private fun doAction(tr: Transition, gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        when (tr.action) {
            Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), tr, noLookahead)
            Transition.ParseAction.EMBED -> doEmbedded(gn, setOf(previous), tr, noLookahead)
            Transition.ParseAction.HEIGHT -> doHeight(gn, previous, tr, noLookahead)
            Transition.ParseAction.GRAFT -> doGraft(gn, previous, tr, noLookahead)
            Transition.ParseAction.GOAL -> error("Should never happen")
        }
    }

    private fun doActionShift(tr: Transition, gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        when (tr.action) {
            Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), tr, noLookahead)
            Transition.ParseAction.EMBED -> doEmbedded(gn, setOf(previous), tr, noLookahead)
            Transition.ParseAction.HEIGHT -> error("should never happen") //doHeight(gn, previous, tr, noLookahead)
            Transition.ParseAction.GRAFT -> error("should never happen") //doGraft(gn, previous, tr, noLookahead)
            Transition.ParseAction.GOAL -> error("Should never happen")
        }
    }

    private fun doActionReduce(tr: Transition, gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        when (tr.action) {
            Transition.ParseAction.HEIGHT -> doHeight(gn, previous, tr, noLookahead)
            Transition.ParseAction.GRAFT -> doGraft(gn, previous, tr, noLookahead)
            Transition.ParseAction.GOAL -> error("Should never happen")
            Transition.ParseAction.WIDTH -> error("Should never happen")
            Transition.ParseAction.EMBED -> error("Should never happen")
        }
    }

    private fun nodeReadyForShift(gn: GrowingNode, previous: PreviousInfo) {
        this.readyForShift.add(gn)
        var set = this.readyForShiftPrevious[gn]
        // TODO: not efficient
        if (null == set) {
            set = mutableSetOf()
            this.readyForShiftPrevious[gn] = set
        }
        set.add(previous)
    }

    private fun doGoal(gn: GrowingNode) {
        //val complete = this.graph.findCompleteNode(gn.currentState.rulePositions.first(), gn.startPosition, gn.nextInputPosition) ?: error("Should never be null")
        this.graph.recordGoal(gn)//TODO: what about not first?
    }

    private fun doWidth(curGn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition, noLookahead: Boolean) {
        val l = this.graph.input.findOrTryCreateLeaf(transition.to.firstRule, curGn.nextInputPosition)
        if (null != l) {
//TODO: skip gets parsed multiple times
            //val skipLh = transition.lookaheadGuard.createWithParent(curGn.lookahead)
            val skipLh = this.stateSet.createWithParent(transition.lookaheadGuard, curGn.runtimeLookahead)
            val skipData = this.tryParseSkipUntilNone(skipLh, l.nextInputPosition, noLookahead)//, lh) //TODO: does the result get reused?
            val nextInput = skipData?.nextInputPosition ?: l.nextInputPosition
            //val lastLocation = skipNodes.lastOrNull()?.location ?: l.location

            val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, curGn.runtimeLookahead, nextInput)

            if (noLookahead || hasLh) {// || lh.isEmpty()) { //transition.lookaheadGuard.content.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                val runtimeLhs = curGn.runtimeLookahead//transition.lookaheadGuard.createWithParent(curGn.lookahead)
                val startPosition = l.startPosition
                val nextInputPosition = l.nextInputPosition
                val numNonSkipChildren = 0 // terminals never have children
                this.graph.pushToStackOf(transition.to, runtimeLhs, startPosition, nextInputPosition, numNonSkipChildren, curGn, previousSet, skipData)
            }
        }
    }

    private fun doHeight(curGn: GrowingNode, previous: PreviousInfo, transition: Transition, noLookahead: Boolean): Boolean {
        var doneIt = false
        val prevNode = previous.node
        val nextInputAfterSkip = curGn.skipData?.nextInputPosition ?: curGn.nextInputPosition
        val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, prevNode.runtimeLookahead, nextInputAfterSkip)
        if (noLookahead || hasLh) {// || lh.isEmpty()) {
            val startPosition = curGn.startPosition
            val nextInputPosition = curGn.nextInputPosition
            val numNonSkipChildren = 1 // first child
            val runtimeLhs = this.stateSet.createWithParent(transition.upLookahead, prevNode.runtimeLookahead)
            this.graph.createWithFirstChild(transition.to, runtimeLhs, curGn.currentState, curGn.runtimeLookahead, startPosition, nextInputPosition, numNonSkipChildren, setOf(previous), curGn.skipData)
            doneIt = true
        }
        return doneIt
    }

    private fun doGraft(curGn: GrowingNode, previous: PreviousInfo, transition: Transition, noLookahead: Boolean): Boolean {
        var doneIt = false
        val prevNode = previous.node
        if (transition.runtimeGuard(transition, prevNode, prevNode.currentState.rulePositions)) {
            val nextInputAfterSkip = curGn.skipData?.nextInputPosition ?: curGn.nextInputPosition
            val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, prevNode.runtimeLookahead, nextInputAfterSkip)
            if (noLookahead || hasLh) {
                val runtimeLhs = this.stateSet.createWithParent(transition.upLookahead, prevNode.runtimeLookahead)
                val startPosition = prevNode.startPosition
                val nextInputPosition = curGn.nextInputPosition
                val numNonSkipChildren = prevNode.numNonSkipChildren + 1
                this.graph.growNextChild(
                    oldParentNode = prevNode,
                    nextChildNode = curGn,
                    transition.to, runtimeLhs, startPosition, nextInputPosition, numNonSkipChildren,
                    curGn.skipData)
                doneIt = true
            }
        }
        return doneIt
    }

    /*
    private fun doGraftOrHeight(curGn: GrowingNode, previous: PreviousInfo, trg: Transition, trh: Transition, noLookahead: Boolean) {
        val prevNode = previous.node
        var notDoneGraft = true
        if (trg.runtimeGuard(trg, prevNode, prevNode.currentState.rulePositions)) {
            val hasLh = this.graph.isLookingAt(trg.lookaheadGuard, prevNode.runtimeLookahead, curGn.nextInputPosition)
            if (noLookahead || hasLh) {
                val runtimeLhs = this.stateSet.createWithParent(trg.upLookahead, prevNode.runtimeLookahead)
                val startPosition = curGn.startPosition
                val nextInputPosition = curGn.nextInputPosition
                val numNonSkipChildren = curGn.numNonSkipChildren + 1
                this.graph.growNextChild(prevNode, trg.to, runtimeLhs, startPosition, nextInputPosition, numNonSkipChildren, curGn.skipData)
                notDoneGraft = false
            }
        }
        if (notDoneGraft) { // then try height
            val hasLh = this.graph.isLookingAt(trh.lookaheadGuard, prevNode.runtimeLookahead, curGn.nextInputPosition)
            if (noLookahead || hasLh) {
                val startPosition = curGn.startPosition
                val nextInputPosition = curGn.nextInputPosition
                val numNonSkipChildren = 1 // first child
                val runtimeLhs = this.stateSet.createWithParent(trh.upLookahead, prevNode.runtimeLookahead)
                this.graph.createWithFirstChild(curGn.currentState, trh.to, runtimeLhs, startPosition, nextInputPosition, numNonSkipChildren, setOf(previous), curGn.skipData)
            }
        }
    }
*/

    private fun tryParseSkipUntilNone(lookaheadSet: LookaheadSet, startPosition: Int, noLookahead: Boolean): TreeData? {
        return when (skipParser) {
            null -> null
            else -> tryParseSkip(lookaheadSet, startPosition, noLookahead)
        }
    }

    // must use a different instance of Input, so it can be reset, reset clears cached leaves. //TODO: check this
    private val skipParser = skipStateSet?.let { RuntimeParser(it, null, skipStateSet.userGoalRule, InputFromString(skipStateSet.usedTerminalRules.size, this.input.text)) }

    private fun tryParseSkip(lookaheadSet: LookaheadSet, startPosition: Int, noLookahead: Boolean): TreeData? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
        skipParser!!.reset()//lookaheadSet)
        //val startLocation = InputLocation(startPosition, lastLocation.column, lastLocation.line, 0) //TODO: compute correct line and column
        skipParser.start(startPosition, lookaheadSet)
        var seasons = 1
        var maxNumHeads = skipParser.graph.growingHead.size
        do {
            skipParser.grow3(noLookahead)
            seasons++
            maxNumHeads = max(maxNumHeads, skipParser.graph.growingHead.size)
        } while (skipParser.graph.canGrow && (skipParser.graph.goals.isEmpty() || skipParser.graph.goalMatchedAll.not()))
        //val match = skipGraph.longestMatch(seasons, maxNumHeads)
        //TODO: get longest skip match
        return when {
            //null == match -> curGn
            skipParser.graph.goals.isEmpty() -> null
            else -> {
                skipParser.graph.treeData
            }
        }
    }

    private fun doEmbedded(curGn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition, noLookahead: Boolean) {
        val embeddedRule = transition.to.runtimeRules.first() // should only ever be one
        val endingLookahead = transition.lookaheadGuard.content
        val embeddedRuntimeRuleSet = embeddedRule.embeddedRuntimeRuleSet ?: error("Should never be null")
        val embeddedStartRule = embeddedRule.embeddedStartRule ?: error("Should never be null")
        val embeddedS0 = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedStartRule, this.stateSet.automatonKind).startState
        val embeddedSkipStateSet = embeddedRuntimeRuleSet.skipParserStateSet
        val embeddedParser = RuntimeParser(embeddedS0.stateSet, embeddedSkipStateSet, embeddedStartRule, this.input)
        val startPosition = curGn.nextInputPosition
        //val startLocation = InputLocation(startPosition, gn.lastLocation.column, gn.lastLocation.line, 0) //TODO: compute correct line and column
        val embeddedPossibleEOT = embeddedS0.stateSet.createLookaheadSet(
            transition.lookaheadGuard.content.union(
                this.skipStateSet?.runtimeRuleSet?.firstTerminals?.flatMap { it }?.toSet() ?: emptySet()
            )
        )

        embeddedParser.start(startPosition, embeddedPossibleEOT)
        var seasons = 1
        var maxNumHeads = embeddedParser.graph.growingHead.size
        do {
            embeddedParser.grow3(false)
            seasons++
            maxNumHeads = max(maxNumHeads, embeddedParser.graph.growingHead.size)
        } while (embeddedParser.graph.canGrow && (embeddedParser.graph.goals.isEmpty() || embeddedParser.graph.goalMatchedAll.not()))
        //val match = embeddedParser.longestMatch(seasons, maxNumHeads, true) as SPPTBranch?
        val match = embeddedParser.graph.treeData
        if (match.root != null) {
            val ni = match.nextInputPosition!! // will always have value if root not null
            //TODO: parse skipNodes
            val skipLh = this.stateSet.createWithParent(transition.lookaheadGuard, curGn.runtimeLookahead)
            val skipData = this.tryParseSkipUntilNone(skipLh, ni, noLookahead)//, lh) //TODO: does the result get reused?
            val nextInput = skipData?.nextInputPosition ?: ni
            val numNonSkipChildren = 0 // terminals never have children
            this.graph.pushEmbeddedToStackOf(transition.to, curGn.runtimeLookahead, startPosition, nextInput, numNonSkipChildren, curGn, previousSet, skipData)
            //SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            // do nothing, could not parse embedded
        }
    }

    internal fun transitionsFrom(state: ParserState, previous: Set<ParserState>?): Set<Transition> {
        return when (state.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> state.transitions(null)
            RuntimeRuleKind.TERMINAL -> previous!!.flatMap { prevState -> state.transitions(prevState) }
            RuntimeRuleKind.NON_TERMINAL -> previous!!.flatMap { prevState -> state.transitions(prevState) }
            RuntimeRuleKind.EMBEDDED -> previous!!.flatMap { prevState -> state.transitions(prevState) }
        }.toSet()
    }

    internal fun transitionsEndingInNonEmptyFrom(state: ParserState, previous: Set<ParserState>?): Set<Transition> {
        val tr = transitionsFrom(state, previous)
        val neTr = tr.flatMap {
            val toState = it.to
            if (toState.runtimeRulesSet.any { it.isEmptyRule }) {
                val prev = setOf(it.from)
                transitionsEndingInNonEmptyFrom(toState, prev)
            } else {
                listOf(it)
            }
        }
        return neTr.toSet()
    }
}