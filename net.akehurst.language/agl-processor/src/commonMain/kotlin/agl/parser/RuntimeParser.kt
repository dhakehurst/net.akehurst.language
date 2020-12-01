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

import agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.PreviousInfo
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserInterruptedException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTNode
import kotlin.math.max

internal class RuntimeParser(
        private val stateSet: ParserStateSet,
        private val skipStateSet: ParserStateSet?, // null if this is a skipParser
        goalRule: RuntimeRule,
        possibleEndOfText_: LookaheadSet,
        private val input: InputFromString
) {
    companion object {
        val defaultStartLocation = InputLocation(0, 0, 1, 0)
    }

    val graph = ParseGraph(goalRule, input, this.stateSet.usedTerminalRules.size, this.stateSet.usedNonTerminalRules.size)

    var possibleEndOfText = possibleEndOfText_; private set

    //needs to be public so that expectedAt can use it
    val lastGrown: Collection<GrowingNode>
        get() {
            return setOf<GrowingNode>().union(this.graph.growing.values).union(this.toGrow)
        }
    val lastGrownLinked: Collection<GrowingNode>
        get() {
            this.toGrow.forEach { gn ->
                this.toGrowPrevious[gn]!!.forEach {
                    gn.addPrevious(it)
                }
            }
            return this.toGrow
        }
    val longestLastGrown: SPPTNode?
        get() {
            //TODO: handle the fact that we can parse next token, but if the lookahead is wrong then it fails

            //val llg = this.graph.longestCompleteNodeFromStart
            //return llg
            val llg = this.lastGrown.maxWith(Comparator { a, b -> a.nextInputPosition.compareTo(b.nextInputPosition) })

            return if (null == llg) {
                return null
            } else if (llg.isLeaf) {
                val leaf = this.graph.input.findOrTryCreateLeaf(llg.terminalRule, llg.startPosition)!!
                if (null == llg.skipNodes) {
                    leaf
                } else {
                    val children = listOf(leaf) + llg.skipNodes!!
                    val firstChild = children.first()
                    val lastChild = children.last()
                    val length = (lastChild.nextInputPosition - firstChild.startPosition)
                    //val location = InputLocation(firstChild.location.position, firstChild.location.column, firstChild.location.line, length)
                    TODO()
                    //val branch = SPPTBranchFromInput(llg.runtimeRule, llg.currentState.rulePosition.option, location, llg.skipNodes.last().nextInputPosition, llg.priority)
                    //branch.childrenAlternatives.add(children)
                    //branch
                }
            } else {
                //try grow width
                /*
                val rps = llg.currentState
                val transitions: Set<Transition> = rps.transitions(this.runtimeRuleSet, llg.previous.values.first().node.currentState.rulePosition)
                val transition = transitions.firstOrNull { it -> it.action==Transition.ParseAction.WIDTH }
                if (null!=transition) {
                    val l = this.graph.findOrTryCreateLeaf(transition.to.runtimeRule, llg.nextInputPosition, llg.location)
                }

                 */
                //TODO()
                //val cn = SPPTBranchDefault(llg.runtimeRule, llg.currentState.rulePosition.option, llg.location, llg.nextInputPosition, llg.priority)
                val fst = llg.currentState.rulePositions.first()
                val cn = SPPTBranchFromInputAndGrownChildren(this.input, fst.runtimeRule, fst.option, llg.startPosition, llg.nextInputPosition, fst.priority)
                cn.grownChildrenAlternatives[fst.option] = llg.children
                cn
            }
        }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }


    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    private var toGrow: List<GrowingNode> = listOf()
    private var toGrowPrevious = mutableMapOf<GrowingNode, Collection<PreviousInfo>>()
    private var interruptedMessage: String? = null


    fun reset(possibleEndOfText: LookaheadSet) {
        this.graph.reset()
        this.possibleEndOfText = possibleEndOfText
    }

    fun start(startPosition: Int, possibleEndOfText: LookaheadSet) {
        val gState = stateSet.startState
        this.graph.start(gState, startPosition, possibleEndOfText) //TODO: remove LH
        if (this.stateSet.isSkip) {
        } else {
            this.tryGrowInitialSkip()
        }
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
            val gnindex = GrowingNode.index(gn.currentState, gn.children)//, gn.nextInputPosition, gn.priority)
            this.graph.growingHead[gnindex] = gn
        }
    }

    //to find error locations
    fun tryGrowWidthOnce() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.toGrowPrevious.clear()
        this.graph.growingHead.clear()
        // try grow width
        for (gn in this.toGrow) {
            checkInterrupt()
            val previous = this.graph.pop(gn)
            this.toGrowPrevious[gn] = previous
            this.growWidthOnly(gn, previous)
        }
    }

    fun tryGrowHeightOrGraft(): Set<GrowingNode> {
        val poss = mutableSetOf<GrowingNode>()
        // try height or graft
        while ((this.canGrow && this.graph.goals.isEmpty())) {
            this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
            this.toGrowPrevious.clear()
            this.graph.growingHead.clear()
            // try grow height or graft
            for (gn in this.toGrow) {
                checkInterrupt()
                val previous = this.graph.pop(gn)
                this.toGrowPrevious[gn] = previous
                this.growHeightOrGraftOnly(gn, previous)
            }
            poss.addAll(this.lastGrownLinked)
        }
        return poss
    }


    internal fun growWidthOnly(gn: GrowingNode, previous: Collection<PreviousInfo>) {
        when (gn.runtimeRules.first().kind) { //FIXME
            RuntimeRuleKind.GOAL -> {
                val transitions = gn.currentState.transitions(null)
                        .filter { tr -> tr.to.runtimeRules.any { it.isEmptyRule }.not() }
                for (it in transitions) {
                    when (it.action) {
                        Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it, true)
                    }
                }
            }
            else -> {
                for (prev in previous) {
                    val transitions = gn.currentState.transitions(prev.node.currentState)
                            .filter { it.to.runtimeRules.any { it.isEmptyRule }.not() }
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
        //should never be a GOAL
        //val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        //if (didSkipNode) {
        //    return
        //} else {

        for (prev in previous) {
            //if (gn.isSkipGrowth) {
            //    this.growSkip(gn, prev)
            //} else {
            //val rps = gn.currentState
            //val transitions: Set<Transition> = rps.transitions(this.runtimeRuleSet, prev.node.currentState)
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
            //}
        }
        //}


    }

    fun tryGrowInitialSkip() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.toGrowPrevious.clear()
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            checkInterrupt()
            val skipLhc = gn.currentState.rulePositions.flatMap { this.stateSet.firstOf(it, setOf(RuntimeRuleSet.END_OF_TEXT)) }.toSet()
            val skipLh = this.stateSet.createLookaheadSet(skipLhc)
            val skipNodes = this.tryParseSkipUntilNone(skipLh, gn.startPosition)
            gn.children.appendSkipIfNotEmpty(skipNodes) //ok because gn.children should be empty

            //} else {
            val gi = GrowingNode.index(gn.currentState, gn.children)
            this.graph.addGrowingHead(gi, gn)
            // }
        }
    }

    fun grow(noLookahead: Boolean) {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.toGrowPrevious.clear()
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            checkInterrupt()
            val previous = this.graph.pop(gn)
            this.toGrowPrevious[gn] = previous
            this.growNode(gn, previous, noLookahead)
        }
    }

    internal fun growNode(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        //val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        //if (didSkipNode) {
        //    return
        //} else {
        when (gn.runtimeRules.first().kind) {//FIXME
            RuntimeRuleKind.GOAL -> this.growGoalNode(gn, noLookahead)
            RuntimeRuleKind.TERMINAL -> this.growNormal(gn, previous, noLookahead)
            RuntimeRuleKind.NON_TERMINAL -> this.growNormal(gn, previous, noLookahead)
            RuntimeRuleKind.EMBEDDED -> this.growNormal(gn, previous, noLookahead)
        }
        // }
    }

    private fun growGoalNode(gn: GrowingNode, noLookahead: Boolean) {
        //no previous, so gn must be the Goal node
        //val rps = gn.currentState
        //val transitions: Set<Transition> = rps.transitions(this.runtimeRuleSet, null)
        val transitions = gn.currentState.transitions(null)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it, noLookahead)
                Transition.ParseAction.HEIGHT -> error("Should never happen")
                Transition.ParseAction.GRAFT -> error("Should never happen")
                Transition.ParseAction.GOAL -> doGoal(gn)
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun growNormal(gn: GrowingNode, previous: Collection<PreviousInfo>, noLookahead: Boolean) {
        for (prev in previous) {
            //if (gn.isSkipGrowth) {
            //    this.growSkip(gn, prev)
            //} else {
            this.growWithPrev(gn, prev, noLookahead)
            //}
        }
    }

    private fun growWithPrev(gn: GrowingNode, previous: PreviousInfo, noLookahead: Boolean) {
        val transitions = gn.currentState.transitions(previous.node.currentState)
        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it, noLookahead)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it, noLookahead)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it, noLookahead)
                Transition.ParseAction.GOAL -> error("Should never happen")
                Transition.ParseAction.EMBED -> doEmbedded(gn, setOf(previous), it)
            }
        }
    }

    private fun doGoal(gn: GrowingNode) {
        val complete = this.graph.findCompleteNode(gn.currentState.rulePositions.first(), gn.startPosition) ?: error("Should never be null")
        this.graph.recordGoal(complete)
    }

    private fun doWidth(curGn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition, noLookahead: Boolean) {
        val l = this.graph.input.findOrTryCreateLeaf(transition.to.terminalRule, curGn.nextInputPosition)
        if (null != l) {
//TODO: skip gets parsed multiple times
            val skipLh = transition.lookaheadGuard.createWithParent(curGn.lookahead)
            val skipNodes = this.tryParseSkipUntilNone(skipLh, l.nextInputPosition)//, lh) //TODO: does the result get reused?
            val nextInput = skipNodes.lastOrNull()?.nextInputPosition ?: l.nextInputPosition
            //val lastLocation = skipNodes.lastOrNull()?.location ?: l.location

            val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, curGn.lookahead, nextInput)

            if (noLookahead || hasLh) {// || lh.isEmpty()) { //transition.lookaheadGuard.content.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                val lhs = curGn.lookahead//transition.lookaheadGuard.createWithParent(curGn.lookahead)
                this.graph.pushToStackOf(transition.to, lhs, l, curGn, previousSet, skipNodes)
            }
        }
    }

    private fun doHeight(curGn: GrowingNode, previous: PreviousInfo, transition: Transition, noLookahead: Boolean) {
        val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, previous.node.lookahead, curGn.nextInputPosition)
        //val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, curGn.lookahead, curGn.nextInputPosition)
        if (noLookahead || hasLh) {// || lh.isEmpty()) {

            val firstChildAlts = curGn.currentState.rulePositions.map {
                this.graph.findCompleteNode(it, curGn.startPosition) ?: error("Should never be null")
            }
            val lhs = transition.upLookahead.createWithParent(previous.node.lookahead)
            this.graph.createWithFirstChild(transition.to, lhs, firstChildAlts, setOf(previous), curGn.skipNodes)
            //this.graph.createWithFirstChild(transition.to, lhs, curGn.children, setOf(previous))//, curGn.skipNodes)
        }
    }

    private fun doGraft(curGn: GrowingNode, previous: PreviousInfo, transition: Transition, noLookahead: Boolean) {
        val prev = previous.node
        if (transition.runtimeGuard(transition, prev, prev.currentState.rulePositions)) {

            val hasLh = this.graph.isLookingAt(transition.lookaheadGuard, prev.lookahead, curGn.nextInputPosition)

            if (noLookahead || hasLh) {// || lh.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                val nextChildAlts = curGn.currentState.rulePositions.map {
                    this.graph.findCompleteNode(it, curGn.startPosition) ?: error("Should never be null")
                }
                val lhs = transition.upLookahead.createWithParent(previous.node.lookahead)
                this.graph.growNextChild(transition.to, lhs, previous.node, nextChildAlts, curGn.skipNodes)
            }
        }
    }

    private fun tryParseSkipUntilNone(lookaheadSet: LookaheadSet, startPosition: Int): List<SPPTNode> {
        return when {
            null == skipParser -> emptyList<SPPTNode>()
            else -> {
                val skipNode = tryParseSkip(lookaheadSet, startPosition)
                when (skipNode) {
                    null -> emptyList<SPPTNode>()
                    else -> skipNode.asBranch.children[0].asBranch.children.flatMap { it.asBranch.children }
                }
            }
        }
    }

    // must use a different instance of Input, so it can be reset, reset clears cached leaves. //TODO: check this
    private val skipParser = skipStateSet?.let { RuntimeParser(it, null, skipStateSet.userGoalRule, LookaheadSet.EMPTY, InputFromString(skipStateSet.usedTerminalRules.size, this.input.text)) }
    private fun tryParseSkip(lookaheadSet: LookaheadSet, startPosition: Int): SPPTNode? {//, lh:Set<RuntimeRule>): List<SPPTNode> {
        skipParser!!.reset(lookaheadSet)
        //val startLocation = InputLocation(startPosition, lastLocation.column, lastLocation.line, 0) //TODO: compute correct line and column
        skipParser.start(startPosition, lookaheadSet)
        var seasons = 1
        var maxNumHeads = skipParser.graph.growingHead.size
        do {
            skipParser.grow(false)
            seasons++
            maxNumHeads = max(maxNumHeads, skipParser.graph.growingHead.size)
        } while (skipParser.graph.canGrow && (skipParser.graph.goals.isEmpty() || skipParser.graph.goalMatchedAll.not()))
        //val match = skipGraph.longestMatch(seasons, maxNumHeads)
        //TODO: get longest skip match
        return when {
            //null == match -> curGn
            skipParser.graph.goals.isEmpty() -> null
            else -> {
                val match = skipParser.graph.goals.sortedBy { it.matchedTextLength }.last()
                val skipNode = match
                skipNode
            }
        }
    }

    /*
    private fun growSkip(gn: GrowingNode, previous: PreviousInfo) {
        val rps = gn.currentState
        //val transitions = rps.transitions(this.runtimeRuleSet, null)//previous.node.currentState)
        // val transitions: Set<Transition> = this.runtimeRuleSet.skipTransitions(this.graph.userGoalRule, rps, previous.node.currentState.rulePosition)
        val transitions = gn.currentState.transitions(previous.node.currentState)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it, false)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it, false)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it, false)
                Transition.ParseAction.GOAL -> doGraftSkip(gn, previous, it)
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun doGraftSkip(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        val complete = this.graph.findCompleteNode(gn.currentState.rulePosition, gn.startPosition, gn.matchedTextLength) ?: error("Should never be null")
        this.graph.growNextSkipChild(previous.node, complete)
        //       println(transition)
    }
*/
    private fun doEmbedded(gn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition) {
        val embeddedRule = transition.to.runtimeRules.first() // should only ever be one
        val endingLookahead = transition.lookaheadGuard.content
        val embeddedRuntimeRuleSet = embeddedRule.embeddedRuntimeRuleSet ?: error("Should never be null")
        val embeddedStartRule = embeddedRule.embeddedStartRule ?: error("Should never be null")
        val embeddedS0 = embeddedRuntimeRuleSet.startingState(embeddedStartRule)
        val embeddedSkipStateSet = embeddedRuntimeRuleSet.skipParserStateSet
        val embeddedParser = RuntimeParser(embeddedS0.stateSet, embeddedSkipStateSet, embeddedStartRule, transition.lookaheadGuard, this.input)
        val startPosition = gn.nextInputPosition
        //val startLocation = InputLocation(startPosition, gn.lastLocation.column, gn.lastLocation.line, 0) //TODO: compute correct line and column

        embeddedParser.start(startPosition, transition.lookaheadGuard)
        var seasons = 1
        var maxNumHeads = embeddedParser.graph.growingHead.size
        do {
            embeddedParser.grow(false)
            seasons++
            maxNumHeads = max(maxNumHeads, embeddedParser.graph.growingHead.size)
        } while (embeddedParser.graph.canGrow && (embeddedParser.graph.goals.isEmpty() || embeddedParser.graph.goalMatchedAll.not()))
        val match = embeddedParser.graph.longestMatch(seasons, maxNumHeads) as SPPTBranch
        if (match != null) {
            //TODO: parse skipNodes

            this.graph.pushToStackOf(transition.to, gn.lookahead, match, gn, previousSet, emptyList())
            //SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            // do nothing, could not parse embedded
        }
    }
}