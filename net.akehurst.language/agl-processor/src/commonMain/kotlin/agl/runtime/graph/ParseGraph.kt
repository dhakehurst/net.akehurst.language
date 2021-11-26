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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.collections.BinaryHeap
import net.akehurst.language.agl.collections.binaryHeapMin
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.sppt.SPPTNodeFromInputAbstract
import net.akehurst.language.agl.sppt.SharedPackedParseTreeDefault
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode

internal class ParseGraph(
    val input: InputFromString,
    numTerminalRules: Int,
    numNonTerminalRules: Int
) {
    data class CompleteNodeIndex(
        val runtimeRuleNumber: Int,
        //val option: Int,
        val startPosition: Int
    )


    //TODO: try storing complete nodes by state rather than RuntimRule ? maybe..not sure
    //internal val completeNodes = CompletedNodesStore<SPPTBranch>(numNonTerminalRules, input.text.length + 1)
    internal val growing: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    internal val _goals: MutableSet<SPPTNode> = mutableSetOf()
    // choiceRule -> all heads that have been grown from this choice
    private val _grownChildren  = mutableMapOf<CompleteChildrenIndex, GrowingChildren>()

    //val growingHead: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    val growingHead: BinaryHeap<Int, GrowingNode> = binaryHeapMin()

    val canGrow: Boolean get() = !this.growingHead.isEmpty()

    val goals: Set<SPPTNode> get() = this._goals

    var goalMatchedAll = true

    val hasNextHead:Boolean get() = this.growingHead.isNotEmpty()

    val nextHeadNextInputPosition:Int get() = this.growingHead.peekRoot?.nextInputPosition ?: Int.MAX_VALUE
    val growingHeadMaxNextInputPosition:Int get() = this.growingHead.toList().lastOrNull()?.nextInputPosition ?: -1 //TODO: cache rather than compute

    /**
     * extract head with min nextInputPosition
     * assumes there is one - check with hasNextHead
     */
    fun nextHead():GrowingNode  {
        val gn = this.growingHead.extractRoot()!!
        val gnindex = GrowingNode.indexForGrowingNode(gn)
        this.growing.remove(gnindex)
        return gn
    }

    fun reset() {
        this.input.reset()
        //       this.completeNodes.clear()
        this.growing.clear()
        this._goals.clear()
        this.growingHead.clear()
    }

    fun createGrowingNode(state: ParserState, lookahead: LookaheadSet, growingChildren: GrowingChildren): GrowingNode {
        if (state.isAtEnd) {
            state.runtimeRulesSet.forEach {
                val ci = CompleteChildrenIndex(it, growingChildren.startPosition)
                val existing=this._grownChildren[ci]
                if (null==existing) {
                    this._grownChildren[ci] = growingChildren
                } else {
                    //TODO: existing.mergeOrDropWithPriority(growingChildren)

                }
            }
        }
        return GrowingNode(this, state, lookahead, growingChildren)
    }
/*
    fun findCompleteNode(rulePosition: RulePosition, startPosition: Int, nextInputPosition: Int): SPPTNode? {
        val rr = rulePosition.runtimeRule
        // val option = rulePosition.option
        return when (rulePosition.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> this.input.leaves[rr, startPosition]
            RuntimeRuleKind.GOAL,
            RuntimeRuleKind.NON_TERMINAL -> {
                //val index = CompleteNodeIndex(rr.number, startPosition)//option, startPosition)
                this.completeNodes[rr, startPosition]
            }
            RuntimeRuleKind.EMBEDDED -> {
                //val index = CompleteNodeIndex(rr.number, startPosition)//option, startPosition)
                this.completeNodes[rr, startPosition]
            }
        }
    }

    fun findLongestCompleteNode(rulePosition: RulePosition, startPosition: Int): SPPTNode? {
        val rr = rulePosition.runtimeRule
        // val option = rulePosition.option
        return when (rulePosition.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> this.input.leaves[rr, startPosition]
            RuntimeRuleKind.GOAL,
            RuntimeRuleKind.NON_TERMINAL -> {
                //val index = CompleteNodeIndex(rr.number, startPosition)//option, startPosition)
                this.completeNodes[rr, startPosition]
            }
            RuntimeRuleKind.EMBEDDED -> {
                //val index = CompleteNodeIndex(rr.number, startPosition)//option, startPosition)
                this.completeNodes[rr, startPosition]
            }
        }
    }

    private fun addBranchToCompetedNodes(runtimeRule:RuntimeRule, cn:SPPTBranch) {
        this.completeNodes[runtimeRule, cn.startPosition] = cn
    }*/

    private fun addGrowing(gn: GrowingNode) {
        val gnindex = GrowingNode.indexForGrowingNode(gn)//, nextInputPosition, gn.priority)
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

    private fun addGrowing(gnindex: GrowingNodeIndex, gn: GrowingNode): GrowingNode {
        val existing = this.growing[gnindex]
        return if (null == existing) {
            this.growing[gnindex] = gn
            gn
        } else {
            // merge
            for (info in gn.previous.values) {
                existing.addPrevious(info)
            }
            existing
        }
    }

    private fun addGrowing(gn: GrowingNode, previous: Set<PreviousInfo>): GrowingNode {
        //val startPosition = gn.startPosition
        //val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNode.indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)//, nextInputPosition, gn.priority)
        val existing = this.growing[gnindex]
        return if (null == existing) {
            for (info in previous) {
                gn.addPrevious(info)
            }
            this.growing[gnindex] = gn
            gn
        } else {
            // merge
            for (info in previous) {
                existing.addPrevious(info)
            }
            existing
        }
    }

    private fun removeGrowing(gn: GrowingNode) {
        //val startPosition = gn.startPosition
        //val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNode.indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)
        this.growing.remove(gnindex)
    }

    fun addGrowingHead(gn: GrowingNode) {
        this.addGrowing(gn)
        this.growingHead[gn.nextInputPosition] = gn
    }

    fun addGrowingHead(gnindex: GrowingNodeIndex, gn: GrowingNode) {
        this.addGrowing(gnindex, gn)
        this.growingHead[gn.nextInputPosition] = gn
    }

    private fun addAndRegisterGrowingPrevious(gn: GrowingNode, previous: Iterable<PreviousInfo>) {
        for (info in previous) {
            gn.addPrevious(info)
            this.addGrowing(info.node)
        }
    }

    //TODO: combine next 3 methods!
    private fun findOrCreateGrowingLeafOrEmbeddedNode(
        newState: ParserState,
        lookahead: LookaheadSet,
        growingChildren: GrowingChildren,
        oldHead: GrowingNode,
        previous: Set<PreviousInfo>,
        skipNodes: List<SPPTNode>
    ): GrowingNode {
        val oldOrExistingHead = this.addGrowing(oldHead, previous)
        for (info in previous) {
            this.addGrowing(info.node)
        }
        val gnindex = GrowingNode.indexFromGrowingChildren(newState, lookahead, growingChildren)
        val existing = this.growing[gnindex]
        val gn = if (null == existing) {
            val nn = this.createGrowingNode(newState, lookahead, growingChildren)
            nn.addPrevious(oldOrExistingHead)
            nn
        } else {
            existing.addPrevious(oldOrExistingHead)
            existing
        }
        if (skipNodes.isNotEmpty()) {
            gn.skipNodes = skipNodes
        }
        this.addGrowingHead(gnindex, gn)
        return gn
    }

    private fun findOrCreateGrowingNode(newState: ParserState, newRuntimeLookahead: LookaheadSet, newChildren: GrowingChildren, previous: Set<PreviousInfo>): GrowingNode {
        val gnindex = GrowingNode.indexFromGrowingChildren(newState, newRuntimeLookahead, newChildren)//, nextInputPosition, priority)
        val existingGrowing = this.growing[gnindex] //?: findSimilarGrowing(newState)

        return if (null == existingGrowing) {
            val nn = this.createGrowingNode(newState, newRuntimeLookahead, newChildren)
            this.addAndRegisterGrowingPrevious(nn, previous)
            this.addGrowingHead(nn)
            nn
        } else {
            //merge
            val nn = mergeOrDropWithPriority(existingGrowing, newState, newRuntimeLookahead, newChildren)
            if (nn!=existingGrowing) {
                // could be a child of a head that has already reduced
                //TODO("Invalidate/remove head corresponding to the node")
            }
            this.addAndRegisterGrowingPrevious(nn, previous)
            this.addGrowingHead(nn)
            nn
        }
    }

    private fun mergeOrDropWithPriority(existingNode: GrowingNode, newState: ParserState, newLookahead: LookaheadSet, newChildren: GrowingChildren): GrowingNode {
        // the Rule for the existingNode will be same as Rule for newState,
        // but the position & priority of newState will/could be different
        val newChoiceKindList = newState.choiceKind
        val existingChoiceKindList = existingNode.currentState.choiceKind
        check(newChoiceKindList == existingChoiceKindList) //TODO: could remove for performance
        return when (existingChoiceKindList.size) {
            0 -> existingNode // TODO: could be a LIST which encodes choice between middle and end of the list?
            1 -> {
                val choiceKind = existingChoiceKindList[0]
                val existingNodePriority = existingNode.currentState.priority[0]
                val newChildrenPriority = newState.priority[0]
                val existingChildrenLength = existingNode.matchedTextLength
                val newChildrenLength = newChildren.matchedTextLength
                when (choiceKind) {
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        when {
                            existingChildrenLength > newChildrenLength -> existingNode
                            newChildrenLength > existingChildrenLength -> createGrowingNode(newState, newLookahead, newChildren)
                            else -> when {
                                existingNodePriority > newChildrenPriority -> existingNode
                                newChildrenPriority > existingNodePriority -> createGrowingNode(newState, newLookahead, newChildren)
                                else -> existingNode// both the same so cannot choose, use existing - TODO don't really want this to happen!! - see "vav"
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                        when {
                            existingNodePriority > newChildrenPriority -> existingNode
                            newChildrenPriority > existingNodePriority -> createGrowingNode(newState, newLookahead, newChildren)
                            else -> when {
                                existingChildrenLength > newChildrenLength -> existingNode
                                newChildrenLength > existingChildrenLength -> createGrowingNode(newState, newLookahead, newChildren)
                                else -> existingNode// both the same so cannot choose, use existing - TODO don't really want this to happen!! - see "vav"
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.AMBIGUOUS -> {
                        TODO()
                    }//existingNode.children.join(newChildren); existingNode }
                    RuntimeRuleChoiceKind.NONE -> error("Should never happen")
                }
            }
            else -> {
                TODO()
            }
        }
    }

    private fun mergeOrDropWithPriority(existingNode: GrowingNode, newNode: GrowingNode): GrowingNode {
        // the Rule for the existingNode will be same as Rule for newState,
        // but the position & priority of newState will/could be different
        val newChoiceKindList = newNode.currentState.choiceKind
        val existingChoiceKindList = existingNode.currentState.choiceKind
        check(newChoiceKindList == existingChoiceKindList) //TODO: could remove for performance
        return when (existingChoiceKindList.size) {
            0 -> existingNode // TODO: could be a LIST which encodes choice between middle and end of the list?
            1 -> {
                val choiceKind = existingChoiceKindList[0]
                val existingNodePriority = existingNode.currentState.priority[0]
                val newChildrenPriority = newNode.currentState.priority[0]
                val existingChildrenLength = existingNode.matchedTextLength
                val newChildrenLength = newNode.matchedTextLength
                when (choiceKind) {
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        when {
                            existingChildrenLength > newChildrenLength -> existingNode
                            newChildrenLength > existingChildrenLength -> newNode
                            else -> when {
                                existingNodePriority > newChildrenPriority -> existingNode
                                newChildrenPriority > existingNodePriority -> newNode
                                else -> TODO() //existingNode// both the same so cannot choose, use existing - TODO don't really want this to happen!! - see "vav"
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                        when {
                            existingNodePriority > newChildrenPriority -> existingNode
                            newChildrenPriority > existingNodePriority -> newNode
                            else -> when {
                                existingChildrenLength > newChildrenLength -> existingNode
                                newChildrenLength > existingChildrenLength -> newNode
                                else -> TODO() //existingNode// both the same so cannot choose, use existing - TODO don't really want this to happen!! - see "vav"
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.AMBIGUOUS -> {
                        TODO()
                    }//existingNode.children.join(newChildren); existingNode }
                    RuntimeRuleChoiceKind.NONE -> error("Should never happen")
                }
            }
            else -> {
                TODO()
            }
        }
    }

    /**
     * this will overwrite an existing node with same runtimeRule and startPosition
     */
    private fun createBranchNoChildren(runtimeRule: RuntimeRule, option: Int, priority: Int, startPosition: Int, nextInputPosition: Int): SPPTBranchFromInputAndGrownChildren {
        val cn = SPPTBranchFromInputAndGrownChildren(this.input, runtimeRule, option, startPosition, nextInputPosition, priority)
        //this.addBranchToCompetedNodes(runtimeRule, cn)
        return cn
    }

    /*
        //TODO: need to detect goal, but indicate that there is additional input, not just reject if additional input
        private fun isGoal(completeNode: SPPTNode): Boolean {
            val isStart = this.input.isStart(completeNode.startPosition)
            val isEnd = this.input.isEnd(completeNode.nextInputPosition)
            val isGoalRule = this.userGoalRule.number == completeNode.asBranch.children[0].runtimeRuleNumber
            return isStart && isEnd && isGoalRule
        }

        fun checkForGoal(completeNode: SPPTNode) {
            if (this.isGoal(completeNode)) {
                // TODO: maybe need to not have duplicates!
                this._goals.add(completeNode)
            }
        }

        private fun complete1(gn: GrowingNode): SPPTNode? {
            if (gn.hasCompleteChildren) {
                val runtimeRule = gn.runtimeRule
                val priority = gn.priority
                val location = gn.location
                val matchedTextLength = gn.matchedTextLength
                var cn: SPPTNode? = this.findCompleteNode(runtimeRule, location.position, matchedTextLength)
                if (null == cn) {
                    cn = this.createBranchNoChildren(runtimeRule, priority, location, gn.nextInputPosition)
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

                        val gnLength = gn.matchedTextLength
                        val existingLength = cn.matchedTextLength
                        when {
                            (gnLength > existingLength) -> {
                                //replace existing with this
                                //cn.childrenAlternatives.clear()
                                cn = this.createBranchNoChildren(runtimeRule, priority, location, gn.nextInputPosition)
                                cn.childrenAlternatives.add(gn.children)
                            }
                            (gnLength < existingLength) -> {
                                //keep existing drop this
                            }
                            (gnLength == existingLength) -> {
                                val existingPriority = cn.priority
                                val newPriority = gn.priority
                                when {
                                    (newPriority > existingPriority) -> {
                                        // replace existing with new
                                        //cn.childrenAlternatives.clear()
                                        cn = this.createBranchNoChildren(runtimeRule, priority, location, gn.nextInputPosition)
                                        cn.childrenAlternatives.add(gn.children)
                                    }
                                    (existingPriority > newPriority) -> {
                                        // then existing is the lower precedence item,
                                        // therefore existing node should be the higher item in the tree
                                        // which it is, so change nothing
                                        // do nothing, drop new one
                                        val i = 0
                                    }
                                    (existingPriority == newPriority) -> {
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
                                                        cn = this.createBranchNoChildren(runtimeRule, priority, location, gn.nextInputPosition)
                                                        //cn.childrenAlternatives.clear()
                                                        cn.childrenAlternatives.add(gn.children)
                                                    } else {
                                                        // add the alternatives
                                                        cn.childrenAlternatives.add(gn.children)
                                                    }
                                                }
                                            }
                                        } else {
                                            //TODO: record ambiguity
                                            cn.childrenAlternatives.add(gn.children)

                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                //this.checkForGoal(cn)
                return cn
            } else {
                return null
            }
        }
    */
/*    private fun completeIfReachedEnd(gn: GrowingNode): GrowingNode {
        val used = mutableMapOf<RulePosition, GrowingNode>()
        if (gn.currentState.isAtEnd) {
            gn.currentState.rulePositions.forEachIndexed { index, rp ->
                val runtimeRule = rp.runtimeRule
                val children = gn.children//[runtimeRule] //TODO: can we separate up the children later ?
                val option = rp.option
                val priority = rp.priority
                var cn: SPPTNode? = this.findLongestCompleteNode(rp, gn.startPosition)
                if (null == cn || SPPTLeafFromInput.NONE === cn) {
                    cn = this.createBranchNoChildren(runtimeRule, option, priority, gn.startPosition, gn.nextInputPosition)
                    if (gn.isLeaf) {
                        // dont try and add children...can't for a leaf
                        gn.skipNodes
                    } else {
                        cn.grownChildrenAlternatives[option] = children
                    }
                    gn.addCompleted(runtimeRule, cn)
                    used[rp] = gn
                } else {
                    if (gn.isLeaf) {
                        TODO()
                        // dont try and add children...can't for a leaf
                        gn.skipNodes
                        gn.addCompleted(runtimeRule, cn)
                        used[rp] = gn
                    } else {
                        cn = (cn as SPPTBranchFromInputAndGrownChildren)

                        //TODO: when there is ambiguity, sometimes a complete node is replaced after it has been used in the completions of another node
                        // this give unexpected (wrong!) results
                        val chosen = when (runtimeRule.rhs.itemsKind) {
                            RuntimeRuleRhsItemsKind.CONCATENATION -> {
                                val choice = pickLongest(gn, rp, children, cn)
                                //?:pickHighestPriority(gn, rp, children, cn)
                                    ?: pickByLongestChildren(gn, rp, children, cn)
                                if (null == choice) {
                                    //ambiguous, keep existing TODO is this an error??
                                    cn
                                } else {
                                    choice
                                }
                            }
                            RuntimeRuleRhsItemsKind.CHOICE -> {
                                when (runtimeRule.rhs.choiceKind) {
                                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                                        val choice = pickLongest(gn, rp, children, cn)
                                            ?: pickHighestPriority(gn, rp, children, cn)
                                            ?: cn //ambiguous, keep existing
                                        choice
                                    }
                                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                                        val choice = pickHighestPriority(gn, rp, children, cn)
                                            ?: pickLongest(gn, rp, children, cn)
                                            ?: cn //ambiguous, keep existing
                                        choice
                                    }
                                    RuntimeRuleChoiceKind.AMBIGUOUS -> {
                                        val choice = pickLongest(gn, rp, children, cn)
                                        if (null == choice) {
                                            // same length, so ambiguous
                                            cn.grownChildrenAlternatives[option] = children
                                            cn
                                        } else {
                                            choice
                                        }
                                    }
                                    else -> {
                                        TODO()
                                    }
                                }
                            }
                            RuntimeRuleRhsItemsKind.EMPTY -> {
                                TODO()
                            }
                            RuntimeRuleRhsItemsKind.LIST -> {
                                val choice = pickLongest(gn, rp, children, cn)
                                    ?: cn //ambiguous, keep existing
                                choice
                            }
                        }

                        if (cn === chosen) {
                            //used existing so return that as the new gn
                            //if (null != used) {
                            //    error("TODO")
                            //    //used.children.addFirstChild()
                            //}
                            //used = GrowingNode(gn.currentState, gn.lookahead, cn.grownChildrenAlternatives.values.first())
                            used[rp] = this.createGrowingNode(gn.currentState, gn.lookahead, cn.grownChildrenAlternatives.values.first())
                            used[rp]?.addCompleted(runtimeRule, chosen)
                        } else {
                            //used new stuff
                            gn.addCompleted(runtimeRule, chosen)
                            used[rp] = gn
                        }
                    }
                }
            }
        } else {
            //do nothing

        }
        return when {
            used.isEmpty() -> error("should not happen")
            1 == used.size -> used.values.first()
            used.size == gn.currentState.rulePositions.size -> gn
            else -> TODO()
        }
    }
*/
    // return null if length is the same
    private fun pickLongest(newNode: GrowingNode, newRp: RulePosition, newChildren: GrowingChildren, existingNode: SPPTBranchFromInputAndGrownChildren): SPPTNode? {
        val gnLength = newNode.matchedTextLength
        val existingLength = existingNode.matchedTextLength
        return when {
            (gnLength > existingLength) -> {
                //replace existing with new node
                val longest = this.createBranchNoChildren(newRp.runtimeRule, newRp.option, newRp.priority, newNode.startPosition, newNode.nextInputPosition)
                longest.grownChildrenAlternatives[newRp.option] = newChildren
                longest
            }
            (gnLength < existingLength) -> {
                //keep existing drop this
                existingNode
            }
            else -> null
        }
    }

    // return null if priority is the same
    private fun pickHighestPriority(newNode: GrowingNode, newRp: RulePosition, newChildren: GrowingChildren, existingNode: SPPTBranchFromInputAndGrownChildren): SPPTNode? {
        val newPriority = newRp.priority
        val existingPriority = existingNode.priority
        return when {
            (newPriority > existingPriority) -> {
                // replace existing with new
                //cn.childrenAlternatives.clear()
                val highest = this.createBranchNoChildren(newRp.runtimeRule, newRp.option, newPriority, newNode.startPosition, newNode.nextInputPosition)
                highest.grownChildrenAlternatives[newRp.option] = newChildren
                highest
            }
            (existingPriority > newPriority) -> {
                // then existing is the higher precedence item,
                // therefore existing node should be the higher item in the tree
                // which it is, so change nothing
                // do nothing, drop new one
                existingNode
            }
            else -> null
        }
    }

    private fun pickByLongestChildren(newNode: GrowingNode, newRp: RulePosition, newChildren: GrowingChildren, existingNode: SPPTBranchFromInputAndGrownChildren): SPPTNode? {
        val newChildrenList = newChildren[newRp.identity]
        val existingChildren = existingNode.grownChildrenAlternatives[newRp.option]!![newRp.identity]
        var i = 0
        var chosen: SPPTNode? = null
        while (i < newChildrenList.size && chosen == null) {
            val newChildNP = newChildrenList[i].nextInputPosition
            val existingChildNP = existingChildren[i].nextInputPosition
            chosen = when {
                newChildNP > existingChildNP -> {
                    val lngst = this.createBranchNoChildren(newRp.runtimeRule, newRp.option, newRp.priority, newNode.startPosition, newNode.nextInputPosition)
                    lngst.grownChildrenAlternatives[newRp.option] = newChildren
                    lngst
                }
                existingChildNP > newChildNP -> {
                    existingNode
                }
                else -> null
            }
            i++
        }
        return chosen
    }


    fun start(goalState: ParserState, startPosition: Int, lookahead: LookaheadSet) {
        val growingChildren = GrowingChildren()
        growingChildren.nextInputPosition = startPosition
        growingChildren.startPosition = startPosition
        val goalGN = this.createGrowingNode(
            goalState,
            lookahead,
            growingChildren
        )
        this.addGrowingHead(goalGN)
    }

    fun pop(gn: GrowingNode): Collection<PreviousInfo> {
        for (pi in gn.previous.values) {
            pi.node.removeNext(gn)
            this.removeGrowing(pi.node)
        }
        val previous = gn.previous
        gn.newPrevious()
        return previous.values
    }

    // return if not merged null else node merged with
    fun tryMergeWithExistingHead(gn: GrowingNode, previous: Collection<PreviousInfo>): GrowingNode? {
        val gnindex = GrowingNode.indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)
        val existing = this.growing[gnindex]
        return if (null == existing) {
            null
        } else {
            val nn = mergeOrDropWithPriority(existing, gn)
            this.addAndRegisterGrowingPrevious(nn, previous)
            this.addGrowingHead(nn)
            nn
        }
    }

    fun pushToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        leafNode: SPPTLeaf,
        oldHead: GrowingNode,
        previous: Set<PreviousInfo>,
        skipNodes: List<SPPTNode>
    ): GrowingNode? {
        val growingChildren = GrowingChildren()
            .appendChild(newState.rulePositionIdentity, listOf(leafNode))
            ?.appendSkipIfNotEmpty(newState.rulePositionIdentity,skipNodes)
        //check(growingChildren.nextInputPosition == growingChildren.lastChild?.nextInputPosition)
        val newGn = growingChildren?.let {
            this.findOrCreateGrowingLeafOrEmbeddedNode(newState, lookahead, it, oldHead, previous, skipNodes)
        }
        return newGn
    }

    // for embedded segments
    fun pushToStackOf(newState: ParserState, lookahead: LookaheadSet, embeddedNode: SPPTBranch, oldHead: GrowingNode, previous: Set<PreviousInfo>, skipNodes: List<SPPTNode>) {
        val runtimeRule = newState.runtimeRules.first()// should only ever be one
        (embeddedNode as SPPTNodeFromInputAbstract).embeddedIn = runtimeRule.tag
        val growingChildren = GrowingChildren()
            .appendChild(newState.rulePositionIdentity, listOf(embeddedNode))
            .appendSkipIfNotEmpty(newState.rulePositionIdentity,skipNodes)
        this.findOrCreateGrowingLeafOrEmbeddedNode(newState, lookahead, growingChildren, oldHead, previous, skipNodes)

        //TODO: do we need to check for longest ?
        //this.addBranchToCompetedNodes(runtimeRule,embeddedNode )//TODO: should this be here or in leaves ?
    }

    fun growNextChild(nextState: ParserState, runtimeLookahead: LookaheadSet, parent: GrowingNode, nextChildAlts: List<SPPTNode>, skipChildren: List<SPPTNode>?) {
        var growingChildren = parent.children.appendChild(nextState.rulePositionIdentity, nextChildAlts)
        growingChildren = when {
            null == skipChildren -> growingChildren
            else -> growingChildren.appendSkipIfNotEmpty(nextState.rulePositionIdentity,skipChildren)
        }

        //check(growingChildren.nextInputPosition == growingChildren.lastChild?.nextInputPosition)
        val previous = parent.previous
        for (pi in previous.values) {
            pi.node.removeNext(parent)
        }
        this.findOrCreateGrowingNode(nextState, runtimeLookahead, growingChildren, previous.values.toSet()) //FIXME: don't convert to set
        if (parent.next.isEmpty()) {
            this.removeGrowing(parent)
        }
    }

    fun createWithFirstChild(newState: ParserState, runtimeLookahead: LookaheadSet, firstChildAlts: List<SPPTNode>, previous: Set<PreviousInfo>, skipChildren: List<SPPTNode>?) {
        var growingChildren = GrowingChildren().appendChild(newState.rulePositionIdentity, firstChildAlts)
        growingChildren = when {
            null == skipChildren -> growingChildren
            else -> growingChildren.appendSkipIfNotEmpty(newState.rulePositionIdentity, skipChildren)
        }
        //check(growingChildren.nextInputPosition == growingChildren.lastChild?.nextInputPosition)
        this.findOrCreateGrowingNode(newState, runtimeLookahead, growingChildren, previous)
    }

    fun isLookingAt(lookaheadGuard: LookaheadSet, runtimeLookahead: LookaheadSet?, nextInputPosition: Int): Boolean {
        //TODO: use regex.lookingAt
        return when {
            LookaheadSet.UP == runtimeLookahead -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            LookaheadSet.ANY == lookaheadGuard -> true
            null == runtimeLookahead -> {
                var result = false
                for (rr in lookaheadGuard.content) {
                    val l = this.input.findOrTryCreateLeaf(rr, nextInputPosition)
                    if (null != l) {
                        result = true
                        break
                    }
                }
                result
            }
            LookaheadSet.UP == lookaheadGuard -> {
                var result = false
                for (rr in runtimeLookahead.content) {
                    val l = this.input.findOrTryCreateLeaf(rr, nextInputPosition)
                    if (null != l) {
                        result = true
                        break
                    }
                }
                result
            }
            else -> {
                var result = false
                for (rr in lookaheadGuard.content) {
                    if (RuntimeRuleSet.USE_PARENT_LOOKAHEAD == rr) {
                        if (isLookingAt(runtimeLookahead, null, nextInputPosition)) {
                            result = true
                            break
                        }
                    } else {
                        val l = this.input.findOrTryCreateLeaf(rr, nextInputPosition)
                        if (null != l) {
                            result = true
                            break
                        }
                    }
                }
                result
            }
        }
    }

    fun recordGoal(completeNode: SPPTNode) {
        this._goals.add(completeNode)
        this.goalMatchedAll = this.input.isEnd(completeNode.nextInputPosition)
    }

    override fun toString(): String = this.growingHead.toString()
}