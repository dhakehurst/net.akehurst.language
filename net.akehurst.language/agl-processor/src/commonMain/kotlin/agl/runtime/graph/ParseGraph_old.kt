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
import net.akehurst.language.agl.collections.GraphStructuredStack
import net.akehurst.language.agl.collections.binaryHeap
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.api.sppt.SPPTNode

internal class ParseGraph_old(
    val input: InputFromString,
    val stateSetNumber: Int,
    numTerminalRules: Int,
    numNonTerminalRules: Int
) {


    //TODO: try storing complete nodes by state rather than RuntimRule ? maybe..not sure
    //internal val completeNodes = CompletedNodesStore<SPPTBranch>(numNonTerminalRules, input.text.length + 1)
    internal val growing: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    internal val _goals: MutableSet<GrowingNode> = mutableSetOf()

    // choiceRule -> all heads that have been grown from this choice
    private val _grownChildren = mutableMapOf<CompleteChildrenIndex, GrowingChildren>()

    var treeData = TreeData(stateSetNumber)

    val gss = GraphStructuredStack<GrowingNodeIndex>()

    // to order the heads efficiently so we grow them in the required order
    val growingHeadHeap: BinaryHeap<GrowingNodeIndex, GrowingNode> = binaryHeap() { parent, child ->
        // Ordering rules:
        // 1) nextInputPosition lower number first
        // 2) shift before reduce (reduce happens if state.isAtEnd)
        // 3) choice point order - how to order diff choice points?
        // 4) choice last
        // 5) high priority first if same choice point...else !!
        //TODO: how do we ensure lower choices are done first ?
        when {
            // 1) nextInputPosition lower number first
            parent.nextInputPosition < child.nextInputPosition -> 1
            parent.nextInputPosition > child.nextInputPosition -> -1
            else -> when {
                // 2) shift before reduce (reduce happens if state.isAtEnd)
                parent.state.isAtEnd && child.state.isAtEnd -> when {
                    // 3) same choice point
                    parent.state.runtimeRules == child.state.runtimeRules -> when {
                        // 4) do choice last
                        parent.state.isChoice && child.state.isChoice -> when {
                            // 5) high priority first if same choice point...else !!
                            parent.state.priorityList[0] > child.state.priorityList[0] -> 1 //TODO: can we be sure only one priority?
                            parent.state.priorityList[0] < child.state.priorityList[0] -> -1
                            else -> 0
                        }
                        parent.state.isChoice -> -1 // do non choice child first
                        child.state.isChoice -> 1   // do non choice parent first
                        else -> 0
                    }
                    else -> 0 //TODO: how to order if not same choice point? - ideally 'lower' in the tree choice first
                }
                parent.state.isAtEnd -> -1 // shift child first
                child.state.isAtEnd -> 1 // shift parent first
                else -> 0
            }
        }
    }

    val canGrow: Boolean get() = !this.growingHeadHeap.isEmpty()

    val goals: Set<GrowingNode> get() = this._goals

    var goalMatchedAll = true

    val numberOfHeads get() = this.growingHeadHeap.size

    val hasNextHead: Boolean get() = this.growingHeadHeap.isNotEmpty()

    val nextHeadStartPosition: Int get() = this.growingHeadHeap.peekRoot?.startPosition ?: Int.MAX_VALUE
    //val growingHeadMaxNextInputPosition: Int get() = this.growingHead.toList().lastOrNull()?.nextInputPosition ?: -1 //TODO: cache rather than compute

    /**
     * extract head with min nextInputPosition
     * assumes there is one - check with hasNextHead
     */
    fun nextHead(): GrowingNode {
        val gn = this.growingHeadHeap.extractRoot()!!
        val gnindex = gn.index
        this.growing.remove(gnindex)
        return gn
    }

    fun reset() {
        this.input.reset()
        //       this.completeNodes.clear()
        this.growing.clear()
        this._goals.clear()
        this.growingHeadHeap.clear()
        //TODO: don't want to create ne one of these each time we parse skip
        // but currently can't reuse it as it carries the skip data
        this.treeData = TreeData(stateSetNumber)
    }

    fun createGrowingNode(gnindex: GrowingNodeIndex, numNonSkipChildren: Int): GrowingNode {
        TODO()//return GrowingNode(this, gnindex, numNonSkipChildren)
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
        val gnindex = gn.index
        val existing = this.growing[gnindex]
        if (null == existing) {
            this.growing[gnindex] = gn
        } else {
            // merge with existing growing previous
            for (info in gn.previous.values) {
                existing.addPrevious(info)
            }
        }
    }

    private fun addGrowing(gn: GrowingNode, previous: Set<PreviousInfo>): GrowingNode {
        //val startPosition = gn.startPosition
        //val nextInputPosition = gn.nextInputPosition
        val gnindex = gn.index
        val existing = this.growing[gnindex]
        return if (null == existing) {
            for (info in previous) {
                gn.addPrevious(info)
            }
            this.growing[gnindex] = gn
            gn
        } else {
            // merge with existing growing previous
            for (info in previous) {
                existing.addPrevious(info)
            }
            existing
        }
    }

    private fun removeGrowing(gn: GrowingNode) {
        val gnindex = gn.index
        this.growing.remove(gnindex)
    }

    fun addGrowingHead(gn: GrowingNode) {
        this.addGrowing(gn)
        this.growingHeadHeap[gn.index] = gn
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
        startPosition: Int, nextInputPosition: Int, numNonSkipChildren: Int,
        oldHead: GrowingNode,
        previous: PreviousInfo?,
        skipData: TreeData?
    ): GrowingNode {
        val oldOrExistingHead = this.addGrowing(oldHead, previous?.let { setOf(previous) } ?: emptySet())
        previous?.let { this.addGrowing(it.node) }
        val gnindex = this.treeData.createGrowingNodeIndex(newState, lookahead, startPosition, nextInputPosition, numNonSkipChildren)
        val existing = this.growing[gnindex]
        val gn = if (null == existing) {
            val nn = this.createGrowingNode(gnindex, numNonSkipChildren)
            nn.addPrevious(oldOrExistingHead)
            nn.skipData = skipData
            this.addGrowingHead(nn)
            nn
        } else {
            dropCurrentHead()
            existing.addPrevious(oldOrExistingHead)
            existing
        }


        return gn
    }

    private fun mergeOrCreateGrowingNode(newParentIndex: GrowingNodeIndex, childIndex: GrowingNodeIndex, numNonSkipChildren: Int, previous: Set<PreviousInfo>) {
        val existingGrowing = this.growing[newParentIndex] //?: findSimilarGrowing(newState)
        return if (null == existingGrowing) {
            val nn = this.createGrowingNode(newParentIndex, numNonSkipChildren)
            this.addAndRegisterGrowingPrevious(nn, previous)
            this.addGrowingHead(nn)
        } else {
            //merge
            TODO()
            //val nn = mergeOrDropWithPriority(existingGrowing, childIndex, numNonSkipChildren)
            //this.addAndRegisterGrowingPrevious(nn, previous)
            //this.addGrowingHead(nn)
        }
    }
/*
    private fun mergeOrDropWithPriority(
        existingNode: GrowingNode,
        childIndex: GrowingNodeIndex,
        numNonSkipChildren: Int
    ): GrowingNode {
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
                val newChildrenLength = nextInputPosition - startPosition
                when (choiceKind) {
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        when {
                            existingChildrenLength > newChildrenLength -> existingNode
                            newChildrenLength > existingChildrenLength -> createGrowingNode(newState, newLookahead, startPosition, nextInputPosition, numNonSkipChildren)
                            else -> when {
                                existingNodePriority > newChildrenPriority -> existingNode
                                newChildrenPriority > existingNodePriority -> createGrowingNode(newState, newLookahead, startPosition, nextInputPosition, numNonSkipChildren)
                                else -> existingNode// both the same so cannot choose, use existing - TODO don't really want this to happen!! - see "vav"
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                        when {
                            existingNodePriority > newChildrenPriority -> existingNode
                            newChildrenPriority > existingNodePriority -> createGrowingNode(newState, newLookahead, startPosition, nextInputPosition, numNonSkipChildren)
                            else -> when {
                                existingChildrenLength > newChildrenLength -> existingNode
                                newChildrenLength > existingChildrenLength -> createGrowingNode(newState, newLookahead, startPosition, nextInputPosition, numNonSkipChildren)
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
*/
    /*
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
*/
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

    fun start(goalState: ParserState, startPosition: Int, lookahead: LookaheadSet, initialSkipData: TreeData?) {
        val nextInputPositionAfterSkip = initialSkipData?.nextInputPosition ?: startPosition
        val key = this.treeData.createGrowingNodeIndex(goalState, lookahead, nextInputPositionAfterSkip, nextInputPositionAfterSkip, 0)
        val goalGN = this.createGrowingNode(
            key,
            0
        )
        this.addGrowingHead(goalGN)
        this.treeData.start(key, initialSkipData)
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

    /*
    // return if not merged null else node merged with
    fun tryMergeWithExistingHead(gn: GrowingNode, previous: Collection<PreviousInfo>): GrowingNode? {
        val gnindex = GrowingNodeIndex.indexForGrowingNode(gn)
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
*/

    private fun dropCurrentHead() {
        //TODO: probably something to clean up here!
        val i = 0
    }

    /**
     * oldParent should be null if first child
     */
    fun merge(
        existing: CompleteNodeIndex,
        oldParent: GrowingNodeIndex?,
        newParent: GrowingNodeIndex,
        newChild: GrowingNodeIndex,
        newNumNonSkipChildren: Int,
        previous: Set<PreviousInfo>,
        skipData: TreeData?
    ) {
        when {
            newParent.state.isChoice -> {
                check(newParent.state.choiceKindList.size == 1) //TODO: could remove this check if always passes
                when (newParent.state.firstRuleChoiceKind) {
                    RuntimeRuleChoiceKind.NONE -> error("should never happen")
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        val existingLength = existing.nextInputPosition - existing.startPosition
                        val newLength = newParent.nextInputPosition - newParent.startPosition
                        when {
                            newLength > existingLength -> {
                                val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                                this.addAndRegisterGrowingPrevious(nn, previous)
                                this.addGrowingHead(nn)
                                if (null == oldParent) {
                                    this.treeData.setFirstChild(newParent, newChild, skipData)
                                } else {
                                    this.treeData.appendChild(oldParent, newParent, newChild, skipData)
                                }
                                TODO("this never happens because nextInputPosition is part of the index")
                            }
                            existingLength > newLength -> {
                                //do nothing, drop current head
                                // because existing length > new length
                                // as defined by the grammar
                                TODO("this never happens because nextInputPosition is part of the index")
                            }
                            else -> {
                                val existingPriority = existing.priorityList[0] //TODO: is there definitely only 1 ?
                                val newPriority = newParent.state.priorityList[0]
                                when {
                                    newPriority > existingPriority -> {
                                        //error("should not happen if high priority heads processed first")
                                        val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                                        this.addAndRegisterGrowingPrevious(nn, previous)
                                        this.addGrowingHead(nn)
                                        if (null == oldParent) {
                                            this.treeData.setFirstChild(newParent, newChild, skipData)
                                        } else {
                                            this.treeData.appendChild(oldParent, newParent, newChild, skipData)
                                        }
                                    }
                                    existingPriority > newPriority -> {
                                        //do nothing, drop current head
                                        // because lengths are the same and existing priority is higher and already done
                                        // as defined by the grammar
                                        dropCurrentHead() //TODO: are we sure that previous is the same?
                                    }
                                    else -> {
                                        //do nothing, drop current head
                                        // because length and priority are the same
                                        //TODO: maybe raise a warning or info that this has happened
                                        //dropCurrentHead() //TODO: are we sure that previous is the same?
                                        val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                                        this.addAndRegisterGrowingPrevious(nn, previous)
                                        this.addGrowingHead(nn)
                                    }
                                }
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                        val existingPriority = existing.priorityList[0] //TODO: is there definitely only 1 ?
                        val newPriority = newParent.state.priorityList[0]
                        when {
                            newPriority > existingPriority -> {
                                //error("should not happen if high priority heads processed first")
                                val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                                this.addAndRegisterGrowingPrevious(nn, previous)
                                this.addGrowingHead(nn)
                                //TODO: new to remove all parents and up of existing !!
                                if (null == oldParent) {
                                    this.treeData.setFirstChild(newParent, newChild, skipData)
                                } else {
                                    this.treeData.appendChild(oldParent, newParent, newChild, skipData)
                                }
                            }
                            existingPriority > newPriority -> {
                                //do nothing, drop current head
                                dropCurrentHead()
                            }
                            else -> {
                                //2 diff heads parse same thing at same place same length, same priority !
                                //TODO: not sure...can we drop the second head?
                                // it possibly has different runtimeLookahead!

                                //doing nothing would mean keep existing and drop new - think we need to keep the new head, so

                                // keep new head, but don't update tree! as no new data added - TODO: maybe optionList is different?
                                val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                                this.addAndRegisterGrowingPrevious(nn, previous)
                                this.addGrowingHead(nn)
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.AMBIGUOUS -> TODO()
                }
            }
            else -> {
                //2 diff heads parse same thing at same place but it is not a choice
                //TODO: not sure...can we drop the second head?
                // it possibly has different runtimeLookahead!

                //doing nothing would mean keep existing and drop new - think we need to keep the new head, so
                if (existing.gni == newParent) {
                    //TODO: merge head
                    val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                    this.addAndRegisterGrowingPrevious(nn, previous)
                    this.addGrowingHead(nn)
                } else {
                    // keep new head, but don't update tree! as no new data added - TODO: maybe optionList is different?
                    val nn = this.createGrowingNode(newParent, newNumNonSkipChildren)
                    this.addAndRegisterGrowingPrevious(nn, previous)
                    this.addGrowingHead(nn)
                }
            }
        }
    }

    fun pushToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        startPosition: Int, nextInputPosition: Int, numNonSkipChildren: Int,
        oldHead: GrowingNode,
        previous: PreviousInfo?,
        skipData: TreeData?
    ): GrowingNode {
        val newGn = this.findOrCreateGrowingLeafOrEmbeddedNode(newState, lookahead, startPosition, nextInputPosition, numNonSkipChildren, oldHead, previous, skipData)
        return newGn
    }

    // for embedded segments
    fun pushEmbeddedToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        startPosition: Int,
        nextInputPosition: Int,
        numNonSkipChildren: Int,
        oldHead: GrowingNode,
        previous: PreviousInfo,
        skipData: TreeData?
    ) {
        //val runtimeRule = newState.runtimeRules.first()// should only ever be one
        //(embeddedNode as SPPTNodeFromInputAbstract).embeddedIn = runtimeRule.tag
//TODO: something different for embedded!
        this.findOrCreateGrowingLeafOrEmbeddedNode(newState, lookahead, startPosition, nextInputPosition, numNonSkipChildren, oldHead, previous, skipData)

        //TODO: do we need to check for longest ?
        //this.addBranchToCompetedNodes(runtimeRule,embeddedNode )//TODO: should this be here or in leaves ?
    }

    fun createWithFirstChild(
        parentState: ParserState,
        parentRuntimeLookaheadSet: LookaheadSet,
        childState: ParserState,
        childRuntimeLookaheadSet: LookaheadSet,
        startPosition: Int,
        nextInputPosition: Int,
        childNumNonSkipChildren: Int,
        previous: PreviousInfo,
        skipData: TreeData?
    ) {
        val nextInputAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val parent = this.treeData.createGrowingNodeIndex(parentState, parentRuntimeLookaheadSet, startPosition, nextInputAfterSkip, 1)
        val child = this.treeData.createGrowingNodeIndex(childState, childRuntimeLookaheadSet, startPosition, nextInputPosition, childNumNonSkipChildren)
        //this.mergeOrCreateGrowingNode(parent, child, 1, previous)

        val existingComplete = this.treeData.completedBy[parent.complete]
        if (parent.state.isAtEnd && null != existingComplete) {
            this.merge(existingComplete, null, parent, child, childNumNonSkipChildren, setOf(previous), skipData)
        } else {
            // there could be an existing head that has already set the first child using a lower priority child
            // ideally find the head and drop it...but how?
            // if heads are processed in order by:
            // - 'startPosition' in the sentence so all heads at same start position are processed before heads at later positions
            // - and heads with higher priority are done first, then when lower head is processed it can be dropped
            //       and we know that a lower one has not already been done

            val existingGrowing = this.treeData.growing[parent]
            if (null != existingGrowing) {
                // some other head has already added the first child
                //TODO: check priority of child and replace or drop this head - or use GrowingChildren graph rather than list!
                val existingPriority = existingGrowing[0].priorityList[0]
                val newPriority = child.state.priorityList[0]
                when {
                    existingPriority > newPriority -> {
                        dropCurrentHead()
                    }
                    newPriority > existingPriority -> {
                        val nn = this.createGrowingNode(parent, childNumNonSkipChildren)
                        this.addAndRegisterGrowingPrevious(nn, setOf(previous))
                        this.addGrowingHead(nn)
                        this.treeData.setFirstChild(parent, child, skipData)
                    }
                    else -> {
                        //TODO: something else here maybe
                        val nn = this.createGrowingNode(parent, childNumNonSkipChildren)
                        this.addAndRegisterGrowingPrevious(nn, setOf(previous))
                        this.addGrowingHead(nn)
                        this.treeData.setFirstChild(parent, child, skipData)
                    }
                }
            } else {
                val nn = this.createGrowingNode(parent, childNumNonSkipChildren)
                this.addAndRegisterGrowingPrevious(nn, setOf(previous))
                this.addGrowingHead(nn)
                this.treeData.setFirstChild(parent, child, skipData)
            }
        }
    }

    fun growNextChild(
        oldParentNode: GrowingNode,
        nextChildNode: GrowingNode,
        newParentState: ParserState,
        newParentRuntimeLookaheadSet: LookaheadSet,
        newParentStartPosition: Int,
        newParentNextInputPosition: Int,
        newParentNumNonSkipChildren: Int,
        skipData: TreeData?
    ) {
        val previous = oldParentNode.previous.values.toSet() //FIXME: don't convert to set
        for (pi in previous) {
            pi.node.removeNext(oldParentNode)
        }
        val nextInputAfterSkip = skipData?.nextInputPosition ?: newParentNextInputPosition
        val newParent = this.treeData.createGrowingNodeIndex(newParentState, newParentRuntimeLookaheadSet, newParentStartPosition, nextInputAfterSkip, newParentNumNonSkipChildren)
        val oldParent = oldParentNode.index
        val child = nextChildNode.index
        //this.mergeOrCreateGrowingNode(newParent, child, newNumNonSkipChildren, previous.values.toSet()) //FIXME: don't convert to set

        val existing = this.treeData.completedBy[newParent.complete]
        if (newParent.state.isAtEnd && null != existing) {
            this.merge(existing, oldParent, newParent, child, newParentNumNonSkipChildren, previous, skipData) //FIXME: don't convert to set
        } else {
            val nn = this.createGrowingNode(newParent, newParentNumNonSkipChildren)
            this.addAndRegisterGrowingPrevious(nn, previous)
            this.addGrowingHead(nn)
            this.treeData.appendChild(oldParent, newParent, child, skipData)
        }

        if (oldParentNode.next.isEmpty()) {
            this.removeGrowing(oldParentNode)
        }
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

    fun recordGoal(goal: GrowingNode) {
        this.treeData.setRoot(goal.index)
        this._goals.add(goal)
        this.goalMatchedAll = this.input.isEnd(goal.nextInputPosition)
    }

    override fun toString(): String = this.growingHeadHeap.toString()
}