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

import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.PreviousInfo
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SharedPackedParseTreeDefault
import kotlin.math.max

internal class RuntimeParser(
        private val runtimeRuleSet: RuntimeRuleSet,
        private val graph: ParseGraph
) {
    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    private var toGrow: List<GrowingNode> = listOf()

    //needs to be public so that expectedAt can use it
    val lastGrown: Collection<GrowingNode>
        get() {
            return setOf<GrowingNode>().union(this.graph.growing.values).union(this.toGrow)
        }

    val longestLastGrown: SPPTNode?
        get() {
            //TODO: handle the fact that we can parse next token, but if the lookahead is wrong then it fails

            //val llg = this.graph.longestCompleteNodeFromStart
            //return llg
            val llg = this.lastGrown.maxWith(Comparator<GrowingNode> { a, b -> a.nextInputPosition.compareTo(b.nextInputPosition) })

            return if (null == llg) {
                return null
            } else if (llg.isLeaf) {
                val leaf = this.graph.findOrTryCreateLeaf(llg.runtimeRule, llg.startPosition, llg.location)!!
                if (llg.skipNodes.isEmpty()) {
                    leaf
                } else {
                    val children = listOf(leaf) + llg.skipNodes
                    val firstChild = children.first()
                    val lastChild = children.last()
                    val length = (lastChild.location.position - firstChild.location.position) + lastChild.location.length
                    val location = InputLocation(firstChild.location.position, firstChild.location.column, firstChild.location.line, length)
                    val branch = SPPTBranchDefault(llg.runtimeRule, location, llg.skipNodes.last().nextInputPosition, llg.priority)
                    branch.childrenAlternatives.add(children)
                    branch
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
                val cn = SPPTBranchDefault(llg.runtimeRule, llg.location, llg.nextInputPosition, llg.priority)
                cn.childrenAlternatives.add(llg.children)
                cn
            }
        }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }

    fun start(userGoalRule: RuntimeRule) {
        this.runtimeRuleSet.createAllSkipStates()
        val gState = runtimeRuleSet.startingState(userGoalRule, listOf(RuntimeRuleSet.END_OF_TEXT))
        this.graph.start(gState)
    }

    fun start(userGoalRule: RuntimeRule, startLocation: InputLocation, possibleEndOfText: List<RuntimeRule>) {
        val gState = runtimeRuleSet.startingState(userGoalRule, possibleEndOfText)
        this.graph.start(gState, startLocation)
    }


    fun grow() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            val previous = this.graph.pop(gn)
            this.growNode(gn, previous)
        }
    }

    private fun growNode(gn: GrowingNode, previous: Set<PreviousInfo>) {
        val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        if (didSkipNode) {
            return
        } else {
            when (gn.runtimeRule.kind) {
                RuntimeRuleKind.GOAL -> this.growGoalNode(gn)
                RuntimeRuleKind.TERMINAL -> this.growNormal(gn, previous)
                RuntimeRuleKind.NON_TERMINAL -> this.growNormal(gn, previous)
                RuntimeRuleKind.EMBEDDED -> this.growNormal(gn, previous)
            }
        }
    }

    private fun growGoalNode(gn: GrowingNode) {
        //no previous, so gn must be the Goal node
        val rps = gn.currentState
        val transitions: Set<Transition> = rps.transitions(this.runtimeRuleSet, null)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it)
                Transition.ParseAction.HEIGHT -> throw ParserException("Should never happen")
                Transition.ParseAction.GRAFT -> throw ParserException("Should never happen")
                Transition.ParseAction.GOAL -> doGoal(gn)
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun growNormal(gn: GrowingNode, previous: Set<PreviousInfo>) {
        for (prev in previous) {
            if (gn.isSkipGrowth) {
                this.growSkip(gn, prev)
            } else {
                this.growWithPrev(gn, prev)
            }
        }
    }

    private fun growWithPrev(gn: GrowingNode, previous: PreviousInfo) {
        val rps = gn.currentState
        val transitions: Set<Transition> = rps.transitions(this.runtimeRuleSet, previous.node.currentState.rulePosition)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it)
                Transition.ParseAction.GOAL -> throw ParserException("Should never happen")
                Transition.ParseAction.EMBED -> doEmbedded(gn, setOf(previous), it)
            }
        }
    }

    private fun doGoal(gn: GrowingNode) {
        val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParserException("Should never be null")
        this.graph.recordGoal(complete)
    }

    private fun doWidth(gn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition) {
        val l = this.graph.findOrTryCreateLeaf(transition.to.runtimeRule, gn.nextInputPosition, gn.lastLocation)
        if (null != l) {
            //TODO: find a better way to look past skip terminals, this means wrong matches can be made...though they will be dropped on H or G!
            val lh = transition.lookaheadGuard + this.runtimeRuleSet.firstSkipTerminals
            val hasLh = lh.any {
                val lhLeaf = this.graph.findOrTryCreateLeaf(it, l.nextInputPosition, l.location)
                null != lhLeaf
            }
            if (hasLh || transition.lookaheadGuard.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                this.graph.pushToStackOf(false, transition.to, l, gn, previousSet, emptySet())
 //               println(transition)
            }
        }
    }

    private fun doHeight(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        if (previous.node.currentState.rulePosition != transition.prevGuard) {
            val lh = transition.lookaheadGuard //TODO: do we actually need to lookahead here ? Add an explanation if so
            //TODO("check old and new LH here")
            val hasLh = lh.any {
                val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition, gn.lastLocation)
                null != l
            }
            if (hasLh || lh.isEmpty()) {
                val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                        ?: throw ParserException("Should never be null")
                this.graph.createWithFirstChild(gn.isSkipGrowth, transition.to, complete, setOf(previous), gn.skipNodes)
 //               println(transition)
            }
        }
    }

    private fun doGraft(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        if (previous.node.currentState.rulePosition == transition.prevGuard) {
            if (transition.runtimeGuard(transition, previous.node, previous.node.currentState.rulePosition)) {
                val lh = transition.lookaheadGuard //TODO: do we actually need to lookahead here ? Add an explanation if so
                val hasLh = lh.any {
                    val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition, gn.lastLocation)
                    null != l
                }
                if (hasLh || transition.lookaheadGuard.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                    val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                            ?: throw ParserException("Should never be null")
                    this.graph.growNextChild(false, transition.to, previous.node, complete, previous.node.currentState.position, gn.skipNodes)
 //                   println(transition)
                }
            }
        }
    }

    private fun tryGrowWidthWithSkipRules(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        //TODO: make skip rule parsing essentially be a separate parser, with root rule $skip = all | marked | skip | rules
        // so we always get the longest possible skip match
        if (gn.isSkipGrowth) {
            return false //dont grow more skip if currently doing a skip
        } else {
            var modified = false
            val rps = this.runtimeRuleSet.firstSkipRuleTerminalPositions //TODO: get skipStates here, probably be better/faster
            //for (rp in rps) {
            //for (rr in rp.runtimeRule.itemsAt[rp.position]) {
            for (rr in rps) {
                val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition, gn.lastLocation)
                if (null != l) {
                    val leafRp = RulePosition(rr, 0, -1)
                    val skipState = this.runtimeRuleSet.fetchSkipStates(leafRp)
                    this.graph.pushToStackOf(true, skipState, l, gn, previous, emptySet())
                    modified = true
                }
            }
            //}
            return modified
        }
    }

    private fun growSkip(gn: GrowingNode, previous: PreviousInfo) {
        val rps = gn.currentState
        //val trs = rps.transitions(this.runtimeRuleSet, previous.node.currentState.rulePosition)
        val transitions: Set<Transition> = this.runtimeRuleSet.skipTransitions(this.graph.userGoalRule, rps, previous.node.currentState.rulePosition)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it)
                Transition.ParseAction.GOAL -> doGraftSkip(gn, previous, it)
                Transition.ParseAction.EMBED -> TODO()
            }
        }
    }

    private fun doGraftSkip(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParserException("Should never be null")
        this.graph.growNextSkipChild(previous.node, complete)
 //       println(transition)
    }

    private fun doEmbedded(gn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition) {
        val embeddedRule = transition.to.runtimeRule

        val embeddedRuntimeRuleSet = embeddedRule.embeddedRuntimeRuleSet ?: error("Should never be null")
        val embeddedStartRule = embeddedRule.embeddedStartRule ?: error("Should never be null")
        val graph = ParseGraph(embeddedStartRule, this.graph.input)
        val rp = RuntimeParser(embeddedRuntimeRuleSet, graph)
        val startPosition = gn.lastLocation.position + gn.lastLocation.length
        val startLocation = InputLocation(startPosition, gn.lastLocation.column, gn.lastLocation.line, 0) //TODO: compute correct line and column
        val endingLookahead = transition.lookaheadGuard.toList()
        rp.start(embeddedStartRule, startLocation, endingLookahead)
        var seasons = 1
        var maxNumHeads = graph.growingHead.size
        do {
            rp.grow()
            seasons++
            maxNumHeads = max(maxNumHeads, graph.growingHead.size)
        } while (rp.canGrow)
        val match = graph.longestMatch(seasons, maxNumHeads)
        if (match != null) {
            this.graph.pushToStackOf(false, transition.to, match, gn, previousSet, emptySet())
            //SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            TODO()
//            throwError(graph, rp, seasons, maxNumHeads)
        }
//        println(transition)
    }
}