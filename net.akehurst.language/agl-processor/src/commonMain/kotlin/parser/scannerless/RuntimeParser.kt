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
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.parser.sppt.SPPTBranchDefault

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
            val llg = this.lastGrown.maxWith(Comparator<GrowingNode> { a, b -> a.nextInputPosition.compareTo(b.nextInputPosition) })
            return if (null == llg) {
                return null
            } else if (llg.isLeaf) {
                this.graph.findOrTryCreateLeaf(llg.runtimeRule, llg.startPosition)
            } else {
                val cn = SPPTBranchDefault(llg.runtimeRule, llg.startPosition, llg.nextInputPosition, llg.priority)
                cn.childrenAlternatives.add(llg.children)
                cn
            }
        }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }


    fun start(userGoalRule: RuntimeRule) {
        val gState = runtimeRuleSet.startingState(userGoalRule)
        this.graph.start(gState)
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
            if (gn.runtimeRule.isGoal) {
                this.growGoalNode(gn)
            } else {
                for (prev in previous) {
                    if (gn.isSkipGrowth) {
                        this.growSkip(gn, prev)
                    } else {
                        this.growWithPrev(gn, prev)
                    }
                }
            }
        }
    }

    private fun growGoalNode(gn: GrowingNode) {
        //no previous, so gn must be the Goal node
        val rps = gn.currentState
        val transitions: Set<Transition> = this.runtimeRuleSet.transitions(this.graph.userGoalRule, rps)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, emptySet(), it)
                Transition.ParseAction.HEIGHT -> throw ParseException("Should never happen")
                Transition.ParseAction.GRAFT -> throw ParseException("Should never happen")
                Transition.ParseAction.GOAL -> doGoal(gn)
            }
        }
    }

    private fun growWithPrev(gn: GrowingNode, previous: PreviousInfo) {
        val rps = gn.currentState
        val transitions: Set<Transition> = this.runtimeRuleSet.transitions(this.graph.userGoalRule, rps)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it)
                Transition.ParseAction.GOAL -> throw ParseException("Should never happen")
            }
        }
    }

    private fun doGoal(gn: GrowingNode) {
        val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("Should never be null")
        this.graph.recordGoal(complete)
    }

    private fun doWidth(gn: GrowingNode, previousSet: Set<PreviousInfo>, transition: Transition) {
        val l = this.graph.findOrTryCreateLeaf(transition.item, gn.nextInputPosition)
        if (null != l) {
            val lh = transition.lookaheadGuard + this.runtimeRuleSet.allSkipTerminals
            val hasLh = lh.any {
                val l = this.graph.findOrTryCreateLeaf(it, l.nextInputPosition)
                null != l
            }
            if (hasLh || transition.lookaheadGuard.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                this.graph.pushToStackOf(false, transition.to, l, gn, previousSet, emptySet())
            }
        }
    }

    private fun doHeight(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        if (previous.node.currentState.rulePositionWlh != transition.prevGuard) {
            val lh = transition.lookaheadGuard
            val hasLh = lh.any {
                val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition)
                null != l
            }
            if (hasLh || lh.isEmpty()) {
                val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                        ?: throw ParseException("Should never be null")

                this.graph.createWithFirstChild(gn.isSkipGrowth, transition.to, complete, setOf(previous), gn.skipNodes)
            }
        }
    }

    private fun doGraft(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        if (previous.node.currentState.rulePositionWlh == transition.prevGuard) {
            val lh = transition.lookaheadGuard
            val hasLh = lh.any {
                val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition)
                null != l
            }
            if (hasLh || transition.lookaheadGuard.isEmpty()) { //TODO: check the empty condition it should match when shifting EOT
                val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                        ?: throw ParseException("Should never be null")
                this.graph.growNextChild(false, transition.to, previous.node, complete, previous.node.currentState.position, gn.skipNodes)
            }
        }
    }

    private fun tryGrowWidthWithSkipRules(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        if (gn.isSkipGrowth) {
            return false //dont grow more skip if currently doing a skip
        } else {
            var modified = false
            val rps = this.runtimeRuleSet.firstSkipRuleTerminalPositions //TODO: get skipStates here, probably be better/faster
            for (rp in rps) {
                for (rr in rp.runtimeRule.itemsAt[rp.position]) {
                    val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                    if (null != l) {
                        val leafRp = RulePosition(rr, 0, -1)
                        val skipStates = this.runtimeRuleSet.fetchSkipStates(leafRp)
                        for (ss in skipStates) {
                            this.graph.pushToStackOf(true, ss, l, gn, previous, emptySet())
                        }
                        modified = true
                    }
                }
            }
            return modified
        }
    }

    private fun growSkip(gn: GrowingNode, previous: PreviousInfo) {
        val rps = gn.currentState
        val transitions: Set<Transition> = this.runtimeRuleSet.skipTransitions(this.graph.userGoalRule, rps)

        for (it in transitions) {
            when (it.action) {
                Transition.ParseAction.WIDTH -> doWidth(gn, setOf(previous), it)
                Transition.ParseAction.HEIGHT -> doHeight(gn, previous, it)
                Transition.ParseAction.GRAFT -> doGraft(gn, previous, it)
                Transition.ParseAction.GOAL -> doGraftSkip(gn, previous, it)
            }
        }
    }

    private fun doGraftSkip(gn: GrowingNode, previous: PreviousInfo, transition: Transition) {
        val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("Should never be null")
        this.graph.growNextSkipChild(previous.node, complete)
    }
}