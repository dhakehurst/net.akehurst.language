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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SPPTNodeIdentity
import net.akehurst.language.parser.scannerless.InputFromCharSequence
import net.akehurst.language.parser.sppt.*

internal class ParseGraph(
    private val userGoalRule: RuntimeRule,
    private val input: InputFromCharSequence
) {
    private val leaves: MutableMap<LeafIndex, SPPTLeafDefault> = mutableMapOf()
    private val completeNodes: MutableMap<SPPTNodeIdentity, SPPTNodeDefault> = mutableMapOf()
    internal val growing: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    private val _goals: MutableList<SPPTNodeDefault> = mutableListOf()
    val growingHead: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()

    val canGrow: Boolean
        get() {
            return !this.growingHead.isEmpty()
        }

    val goals: List<SPPTNode>
        get() {
            return this._goals
        }

    lateinit var currentUserGoalRule: RuntimeRule

    fun longestMatch(longestLastGrown: SPPTNode?, seasons: Int): SPPTNode {
        if (!this.goals.isEmpty() && this.goals.size >= 1) {
            var lt = this.goals.iterator().next()
            for (gt in this.goals) {
                if (gt.matchedTextLength > lt.matchedTextLength) {
                    lt = gt
                }
            }
            if (!this.input.isEnd(lt.nextInputPosition + 1)) {
                val llg = longestLastGrown ?: throw ParseException("Internal Error, should not happen")
                val location = this.input.calcLineAndColumn(llg.nextInputPosition)
                throw ParseFailedException("Goal does not match full text", SharedPackedParseTreeDefault(llg, seasons), location)
            } else {
                return lt
            }
        } else {
            val llg = longestLastGrown ?: throw ParseException("Nothing parsed")
            val location = this.input.calcLineAndColumn(llg.nextInputPosition)
            throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons), location)
        }
    }

    private fun tryCreateLeaf(terminalRuntimeRule: RuntimeRule, index: LeafIndex): SPPTLeafDefault? {
        // LeafIndex passed as argument because we already created it to try and find the leaf in the cache
        return if (terminalRuntimeRule.isEmptyRule) {
            val leaf = SPPTLeafDefault(terminalRuntimeRule, index.startPosition, true, "", 0)
            this.leaves[index] = leaf
            this.completeNodes[leaf.identity] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            leaf
        } else {
            val matchedText =
                this.input.tryMatchText(index.startPosition, terminalRuntimeRule.patternText, terminalRuntimeRule.isPattern)
                    ?: return null
            val leaf = SPPTLeafDefault(terminalRuntimeRule, index.startPosition, false, matchedText, 0)
            this.leaves[index] = leaf
            this.completeNodes[leaf.identity] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            return leaf
        }
    }

    fun findOrTryCreateLeaf(terminalRuntimeRule: RuntimeRule, position: Int): SPPTLeafDefault? {
        val index = LeafIndex(terminalRuntimeRule.number, position)
        return this.leaves[index] ?: this.tryCreateLeaf(terminalRuntimeRule, index)
    }

    fun createBranchNoChildren(runtimeRule: RuntimeRule, priority: Int, startPosition: Int, nextInputPosition: Int): SPPTBranchDefault {
        val cn = SPPTBranchDefault(runtimeRule, startPosition, nextInputPosition, priority)
        this.completeNodes.put(cn.identity, cn)
        return cn
    }

    fun findCompleteNode(runtimeRule: RuntimeRule, startPosition: Int, matchedTextLength: Int): SPPTNodeDefault? {
        if (runtimeRule.isTerminal) {
            return this.leaves[LeafIndex(runtimeRule.number, startPosition)]
        } else {
            val index = SPPTNodeIdentityDefault(runtimeRule.number, startPosition, matchedTextLength)
            return this.completeNodes[index]
        }
    }

    private fun addGrowing(gn: GrowingNode) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentRulePositionState, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            this.growing[gnindex] = gn
        } else {
            // merge
            for (info in gn.previous.values) {
                existing.addPrevious(info)
            }
        }
    }

    private fun addGrowing(gn: GrowingNode, previous: Set<PreviousInfo>) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentRulePositionState, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            for (info in previous) {
                gn.addPrevious(info)
            }
            this.growing[gnindex] = gn
        } else {
            // merge
            for (info in previous) {
                existing.addPrevious(info)
            }
        }
    }

    private fun removeGrowing(gn: GrowingNode) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentRulePositionState, startPosition, nextInputPosition)
        this.growing.remove(gnindex)
    }

    fun addGrowingHead(gnindex: GrowingNodeIndex, gn: GrowingNode): GrowingNode? {
        val existingGrowing = this.growing[gnindex]
        if (null != existingGrowing) {
            // don't add the head, previous should already have been merged
            return null
        } else {
            val existing = this.growingHead.get(gnindex)
            if (null == existing) {
                this.growingHead.put(gnindex, gn)
                return gn
            } else {
                // merge
                for (info in gn.previous.values) {
                    existing.addPrevious(info)
                }
                return existing
            }
        }
    }

    private fun findOrCreateGrowingLeaf(isSkipGrowth: Boolean, curRp: RulePositionState, runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int,
                                        stack: GrowingNode, previous: Set<PreviousInfo>, lookahead: Set<RuntimeRule>) {
        this.addGrowing(stack, previous)
        // TODO: remove, this is for test
        for (info in previous) {
            this.addGrowing(info.node)
        }
        val gnindex = GrowingNodeIndex(curRp, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, curRp, startPosition, nextInputPosition, 0, emptyList(), 0)
            nn.addPrevious(stack)
            this.addGrowingHead(gnindex, nn)
        } else {
            existing.addPrevious(stack)
            this.addGrowingHead(gnindex, existing)
        }
    }

    private fun findOrCreateGrowingLeafForSkip(isSkipGrowth: Boolean, runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int, previous: Set<PreviousInfo>, skipChildren: List<SPPTNode>) {
        // TODO: remove, this is for test
        for (info in previous) {
            this.addGrowing(info.node)
        }
        val curRp = RulePositionState(StateNumber(-1), RulePosition(runtimeRule, 0, RulePosition.END_OF_RULE), null, emptySet(), emptySet())//TODO: get this from the rule so we don't have to create an object here
        val gnindex = GrowingNodeIndex(curRp, startPosition, nextInputPosition) //TODO: not sure we need both tgt and cur for leaves
        val existing = this.growing[gnindex]
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, curRp, startPosition, nextInputPosition, 0, emptyList(), 0)
            previous.forEach { nn.addPrevious(it) }
            nn.skipNodes.addAll(skipChildren)
            this.addGrowingHead(gnindex, nn)
        } else {
            previous.forEach { existing.addPrevious(it) }
            existing.skipNodes.addAll(skipChildren)
            this.addGrowingHead(gnindex, existing)
        }
    }

    private fun findOrCreateGrowingNode(isSkipGrowth: Boolean, newRp: RulePositionState, startPosition: Int, nextInputPosition: Int,
                                        priority: Int, children: List<SPPTNode>, numNonSkipChildren: Int, previous: Set<PreviousInfo>): GrowingNode {
        val gnindex = GrowingNodeIndex(newRp, startPosition, nextInputPosition)
        val existing = this.growing.get(gnindex)
        var result: GrowingNode?
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, newRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren)
            for (info in previous) {
                nn.addPrevious(info)
                this.addGrowing(info.node)
            }
            this.addGrowingHead(gnindex, nn)
            if (nn.hasCompleteChildren) {
                this.complete(nn)
            }
            result = nn
        } else {
            for (info in previous) {
                existing.addPrevious(info)
                this.addGrowing(info.node)
            }
            this.addGrowingHead(gnindex, existing)
            result = existing
        }
        return result
    }

    //TODO: need to detect goal, but indicate that there is additional input, not just reject if additional input
    private fun isGoal(completeNode: SPPTNodeDefault): Boolean {
        val isStart = this.input.isStart(completeNode.startPosition)
        val isEnd = this.input.isEnd(completeNode.nextInputPosition)
        val isGoalRule = this.userGoalRule.number == completeNode.runtimeRule.number
        return isStart && isEnd && isGoalRule
    }

    private fun checkForGoal(completeNode: SPPTNodeDefault) {
        if (this.isGoal(completeNode)) {
            // TODO: maybe need to not have duplicates!
            this._goals.add(completeNode)
        }
    }

    private fun complete(gn: GrowingNode): SPPTNode? {
        if (gn.hasCompleteChildren) {
            val runtimeRule = gn.runtimeRule
            val priority = gn.priority
            val startPosition = gn.startPosition
            val matchedTextLength = gn.matchedTextLength
            var cn: SPPTNodeDefault? = this.findCompleteNode(runtimeRule, startPosition, matchedTextLength)
            if (null == cn) {
                cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    cn.childrenAlternatives.add(gn.children)
                }
            } else {
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    // final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
                    // opt.matchedLength = gn.getMatchedTextLength();
                    // opt.nodes = gn.getGrowingChildren();
                    cn = (cn as SPPTBranchDefault)
                    // TODO: don't add duplicate children
                    // TODO: somewhere resolve priorities!
                    val existingPriority = cn.priority
                    val newPriority = gn.priority
                    if (existingPriority == newPriority) {
                        // TODO: record/log ambiguity!
                        // TODO: match by length if priority the same
                        //cn.childrenAlternatives.add(gn.children)
                        if (gn.isEmptyMatch && cn.isEmptyMatch) {
                            if (cn.childrenAlternatives.isEmpty()) {
                                cn.childrenAlternatives.add(gn.children)
                            } else {
                                if (cn.childrenAlternatives.iterator().next().get(0).isEmptyLeaf) {
                                    //TODO: leave it, no need to add empty alternatives, or is there, if they are empty other things ?
                                } else {
                                    //TODO: check this!
                                    if (gn.children.get(0).isEmptyLeaf) {
                                        // use just the empty leaf
                                        cn.childrenAlternatives.clear()
                                        cn.childrenAlternatives.add(gn.children)
                                    } else {
                                        // add the alternatives
                                        cn.childrenAlternatives.add(gn.children)
                                    }
                                }
                            }

                        } else {
                            cn.childrenAlternatives.add(gn.children)

                        }
                    } else if (existingPriority > newPriority) {
                        // then existing is the lower precedence item,
                        // therefore existing node should be the higher item in the tree
                        // which it is, so change nothing
                        // do nothing, drop new one
                        //val i = 0
                    } else if (newPriority > existingPriority) {
                        // replace existing with new
                        cn.childrenAlternatives.clear()
                        cn.childrenAlternatives.add(gn.children)
                    }
                }
            }

            this.checkForGoal(cn)
            return cn
        } else {
            return null
        }
    }

    private fun growNextChildAt(isSkipGrowth: Boolean, nextRp: RulePositionState, parent: GrowingNode, priority: Int, nextChild: SPPTNodeDefault, skipChildren: List<SPPTNode>) {
        val startPosition = parent.startPosition
        val nextInputPosition = if (skipChildren.isEmpty()) {
            nextChild.nextInputPosition
        } else {
            skipChildren.last().nextInputPosition
        }
        val children = parent.children + nextChild + skipChildren
        val previous = parent.previous
        for (pi in previous.values) {
            pi.node.removeNext(parent)
        }
        val numNonSkipChildren = if (nextChild.isSkip) {
            parent.numNonSkipChildren + skipChildren.size
        } else {
            parent.numNonSkipChildren + 1 + skipChildren.size
        }
        this.findOrCreateGrowingNode(isSkipGrowth, nextRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren, previous.values.toSet()) //FIXME: don't convert to set
        if (parent.next.isEmpty()) {
            this.removeGrowing(parent)
        }
    }

    //TODO: addPrevious! goalrule growing node, maybe
    fun start(userGoalRule: RuntimeRule, goalState: RulePositionState, runtimeRuleSet: RuntimeRuleSet) {
        this.currentUserGoalRule = userGoalRule

        val goalGN = GrowingNode(false, goalState, 0, 0, 0, emptyList<SPPTNodeDefault>(), 0)

        //val startStates = runtimeRuleSet.currentPossibleRulePositionStates(this.currentGoalRule, goalState, goalState.graftLookahead)
        //val x = startStates.filter { it.items.any { it.isTerminal } }
        val cls = runtimeRuleSet.fetchOrCreateClosure(userGoalRule, goalState)
        val start = cls.filter { it.runtimeRule == userGoalRule }
        //val start = goalState.items.flatMap {
        //    it.calcExpectedRulePositions(0).map {
        //        runtimeRuleSet.fetchOrCreateRulePositionStateAndItsClosure(userGoalRule, it, goalState.rulePosition, setOf(RuntimeRuleSet.END_OF_TEXT), setOf(RuntimeRuleSet.END_OF_TEXT))
        //    }
        //}

        for (curRp in start) {
            //val curRp = RulePosition(userGoalRule, 0, 0)
            val ugoalGN = GrowingNode(false, curRp, 0, 0, 0, emptyList<SPPTNodeDefault>(), 0)
            ugoalGN.addPrevious(goalGN)
            this.addGrowingHead(GrowingNodeIndex(curRp, 0, 0), ugoalGN)
        }
    }

    fun pop(gn: GrowingNode): Set<PreviousInfo> {
        for (pi in gn.previous.values) {
            pi.node.removeNext(gn)
            this.removeGrowing(pi.node)
        }
        val previous = gn.previous
        gn.newPrevious()
        return previous.values.toSet() //FIXME: don't convert to set
    }

    fun pushToStackOf(isSkipGrowth: Boolean, curRp: RulePositionState, leafNode: SPPTLeafDefault, stack: GrowingNode, previous: Set<PreviousInfo>, lookahead: Set<RuntimeRule>) {
        this.findOrCreateGrowingLeaf(isSkipGrowth, curRp, leafNode.runtimeRule, leafNode.startPosition, leafNode.nextInputPosition, stack, previous, lookahead)
    }

    fun growNextChild(isSkipGrowth: Boolean, nextRp: RulePositionState, parent: GrowingNode, nextChild: SPPTNodeDefault, position: Int, skipChildren: List<SPPTNode>) {
        if (0 != position && parent.runtimeRule.rhs.kind == RuntimeRuleItemKind.MULTI) {
            val prev = parent.children[position - 1]
            if (prev === nextChild) {
                // dont add same child twice to a multi
                return
            }
        }
        val priority = if (0 == position && parent.runtimeRule.rhs.kind == RuntimeRuleItemKind.CHOICE_PRIORITY) {
            parent.runtimeRule.rhs.items.indexOf(nextChild.runtimeRule)
        } else {
            parent.priority
        }
        this.growNextChildAt(isSkipGrowth, nextRp, parent, priority, nextChild, skipChildren)
    }

    fun growNextSkipChild(parent: GrowingNode, skipNode: SPPTNodeDefault) {
        if (parent.runtimeRule.isNonTerminal) {
            this.growNextChildAt(
                false,
                parent.currentRulePositionState,
                parent,
                parent.priority,
                skipNode,
                emptyList()
            )
        } else {
            val nextInputPosition = parent.nextInputPosition + skipNode.matchedTextLength
            val newLeaf = this.findOrCreateGrowingLeafForSkip(
                false,
                parent.runtimeRule,
                parent.startPosition,
                nextInputPosition,
                parent.previous.values.toSet(),  //FIXME: don't convert to set
                parent.skipNodes + skipNode
            )
            if (parent.next.isEmpty()) {
                this.removeGrowing(parent)
            }
        }
    }

    fun createWithFirstChild(isSkipGrowth: Boolean, newRp: RulePositionState, firstChild: SPPTNodeDefault, previous: Set<PreviousInfo>, skipChildren: List<SPPTNode>) {
        val startPosition = firstChild.startPosition
        val nextInputPosition = if (skipChildren.isEmpty()) {
            firstChild.nextInputPosition
        } else {
            skipChildren.last().nextInputPosition
        }
        val runtimeRule = newRp.runtimeRule
        val children = listOf(firstChild) + skipChildren
        val numNonSkipChildren = skipChildren.size
        val priority = if (runtimeRule.rhs.kind == RuntimeRuleItemKind.CHOICE_PRIORITY) {
            runtimeRule.rhs.items.indexOf(firstChild.runtimeRule)
        } else {
            0
        }
        this.findOrCreateGrowingNode(isSkipGrowth, newRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren, previous)
    }
}