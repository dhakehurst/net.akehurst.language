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
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

internal class ParseGraph(
    val input: InputFromString,
    val stateSetNumber: Int,
    numTerminalRules: Int,
    numNonTerminalRules: Int
) {

    internal val _goals: MutableSet<GrowingNode> = mutableSetOf()

    //should be all the nodes that are in _gss
    //val _nodes = mutableMapOf<GrowingNodeIndex, GrowingNode>()

    // to keep track of the stack and get 'previous' nodes
    val _gss = GraphStructuredStack<GrowingNodeIndex>()

    //TODO: is the fifo version faster ? it might help with processing heads in a better order!
    // to order the heads efficiently so we grow them in the required order
    val _growingHeadHeap: BinaryHeap<GrowingNodeIndex, GrowingNode> = binaryHeap { parent, child ->
        //val _growingHeadHeap: BinaryHeapFifo<GrowingNodeIndex, GrowingNode> = binaryHeapFifo { parent, child ->
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

    var treeData = TreeData(stateSetNumber)

    val canGrow: Boolean get() = !this._growingHeadHeap.isEmpty()

    val goals: Set<GrowingNode> get() = this._goals

    var goalMatchedAll = true

    val numberOfHeads get() = this._growingHeadHeap.size

    val hasNextHead: Boolean get() = this._growingHeadHeap.isNotEmpty()

    val nextHeadStartPosition: Int get() = this._growingHeadHeap.peekRoot?.startPosition ?: Int.MAX_VALUE
    //val growingHeadMaxNextInputPosition: Int get() = this.growingHead.toList().lastOrNull()?.nextInputPosition ?: -1 //TODO: cache rather than compute

    /**
     * extract head with min nextInputPosition
     * assumes there is one - check with hasNextHead
     */
    fun nextHead(): Pair<GrowingNode, Set<GrowingNodeIndex>> {
        val gn = this._growingHeadHeap.extractRoot()!!
        //val previous = this._gss.peek(gn.index)
        val previous = this._gss.pop(gn.index)
        return Pair(gn, previous)
    }

    fun reset() {
        this.input.reset()
        //       this.completeNodes.clear()
        this._gss.clear()
        this._goals.clear()
        this._growingHeadHeap.clear()
        //TODO: don't want to create ne one of these each time we parse skip
        // but currently can't reuse it as it carries the skip data
        this.treeData = TreeData(stateSetNumber)
    }

    private fun createGrowingNode(gnindex: GrowingNodeIndex): GrowingNode {
        return GrowingNode(this, gnindex)
    }

    internal fun addGrowingHead(oldHead: GrowingNodeIndex, next: GrowingNode) {
        //this._nodes[next.index] = next
        val new = this._gss.push(oldHead, next.index)
        if (new) {
            this._growingHeadHeap[next.index] = next
        }
    }

    private fun addGrowingHead(oldHead: Set<GrowingNodeIndex>, next: GrowingNode) {
        // this._nodes[next.index] = next
        if (oldHead.isEmpty()) {
            this._gss.root(next.index)
            this._growingHeadHeap[next.index] = next
        } else {
            var new = false
            for (oh in oldHead) {
                val r = this._gss.push(oh, next.index)
                new = new || r
            }
            if (new) {
                this._growingHeadHeap[next.index] = next
            }
        }
    }

    private fun addNewPreferredTreeData(existing: CompleteNodeIndex, oldParent: GrowingNodeIndex?, newParent: GrowingNodeIndex, newChild: GrowingNodeIndex) {
        if (null == oldParent) {
            // should not remove existing, because it can be part of newParent
            // setting the child will also reset the preferred
            this.treeData.setFirstChild(newParent, newChild)
        } else {
            this.treeData.setInGrowingParentChildAt(oldParent, newParent, newChild)
        }
    }

    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        this._gss.pop(existing.gni!!)
        //TODO: maybe this._growingHeadHeap.remove(existing.gni)
        val nn = this.createGrowingNode(newHead)
        this.addGrowingHead(previous, nn)
    }

    private fun createNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        val nn = this.createGrowingNode(newHead)
        this.addGrowingHead(previous, nn)
    }

    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        //TODO: probably something to clean up here!
        val i = 0
    }

    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        this._gss.pop(existing.gni!!)
        //TODO: maybe this._growingHeadHeap.remove(existing.gni)
        val nn = this.createGrowingNode(newHead)
        this.addGrowingHead(previous, nn)
    }

    private fun createNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        val nn = this.createGrowingNode(newHead)
        this.addGrowingHead(previous, nn)
    }

    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        //TODO: probably something to clean up here!
        val i = 0
    }

    private fun findOrCreateGrowingLeafOrEmbeddedNode(
        newHead: GrowingNodeIndex,
        oldHead: GrowingNode,
        previous: GrowingNodeIndex?
    ) {
        if (this._gss.contains(newHead)) {
            // preserve the relationship to previous, but no need for a new head
            if (null != previous) this._gss.push(oldHead.index, newHead)
        } else {
            val nn = this.createGrowingNode(newHead)
            this.addGrowingHead(oldHead.index, nn)
        }
    }

    /**
     * oldParent should be null if first child
     * return when {
     *    1 -> keep new, drop Existing
     *    0 -> keep both
     *    -1 -> keep existing, drop new
     * }
     */
    private fun mergeWithExistingComplete(
        existing: CompleteNodeIndex,
        oldParent: GrowingNodeIndex?,
        newParent: GrowingNodeIndex,
        newChild: GrowingNodeIndex
    ): Int {
        return when {
            newParent.state.isChoice -> {
                check(newParent.state.choiceKindList.size == 1) //TODO: could remove this check if always passes
                when (newParent.state.firstRuleChoiceKind) {
                    RuntimeRuleChoiceKind.NONE -> error("should never happen")
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        val existingLength = existing.nextInputPosition - existing.startPosition
                        val newLength = newParent.nextInputPosition - newParent.startPosition
                        when {
                            newLength > existingLength -> {
                                1
                            }
                            existingLength > newLength -> {
                                //do nothing, drop current head
                                // because existing length > new length
                                // as defined by the grammar
                                //TODO("this never happens because nextInputPosition is part of the index")
                                -1
                            }
                            else -> {
                                val existingPriority = existing.priorityList[0] //TODO: is there definitely only 1 ?
                                val newPriority = newParent.state.priorityList[0]
                                when {
                                    newPriority > existingPriority -> {
                                        1
                                    }
                                    existingPriority > newPriority -> {
                                        //do nothing, drop current head
                                        // because lengths are the same and existing priority is higher and already done
                                        // as defined by the grammar
                                        //TODO: are we sure that previous is the same?
                                        -1
                                    }
                                    else -> {
                                        //do nothing, drop current head
                                        // because length and priority are the same
                                        //TODO: maybe raise a warning or info that this has happened
                                        //dropCurrentHead() //TODO: are we sure that previous is the same?
                                        0
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
                                //TODO: new to remove all parents and up of existing !!
                                1
                            }
                            existingPriority > newPriority -> {
                                //do nothing, drop current head
                                -1
                            }
                            else -> {
                                //2 diff heads parse same thing at same place same length, same priority !
                                //TODO: not sure...can we drop the second head?
                                // it possibly has different runtimeLookahead!

                                //doing nothing would mean keep existing and drop new - think we need to keep the new head, so

                                // keep new head, but don't update tree! as no new data added - TODO: maybe optionList is different?
                                0
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

                val existingLength = existing.nextInputPosition - existing.startPosition
                val newLength = newParent.nextInputPosition - newParent.startPosition
                when {
                    newLength > existingLength -> {
                        1
                    }
                    existingLength > newLength -> {
                        //do nothing, drop current head
                        // because existing length > new length
                        // as defined by the grammar
                        //TODO("this never happens because nextInputPosition is part of the index")
                        -1
                    }
                    else -> {
                        //TODO:should we keep both ?
                        0
                    }
                }
            }
        }
    }

    fun xxx(newParent: GrowingNodeIndex) {
        if (newParent.state.isAtEnd) {
            // if newParent has already been completed
            // and existing is preferred due to priority or length then
            //   - remove newParent from GSS
            //   - do not create new head
            //   - not create new tree data
            // else
            //  - add to GSS
            //  - create new head if at top of GSS
            //  - create new tree data
        } else { // not at end

        }
    }

    fun previousOf(gn: GrowingNodeIndex): Set<GrowingNodeIndex> = this._gss.peek(gn)

    fun start(goalState: ParserState, startPosition: Int, lookahead: LookaheadSet, initialSkipData: TreeData?) {
        val nextInputPositionAfterSkip = initialSkipData?.nextInputPosition ?: startPosition
        val st = this.treeData.createGrowingNodeIndex(goalState, lookahead, nextInputPositionAfterSkip, nextInputPositionAfterSkip, nextInputPositionAfterSkip, 0)
        val goalGN = this.createGrowingNode(st)
        this._gss.root(st)
        this._growingHeadHeap[st] = goalGN
        this.treeData.start(st, initialSkipData)
    }

    fun pushToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        startPosition: Int,
        nextInputPosition: Int,
        oldHead: GrowingNode,
        previous: GrowingNodeIndex?,
        skipData: TreeData?
    ) {
        if (null != previous) this._gss.push(previous, oldHead.index)
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.treeData.createGrowingNodeIndex(newState, lookahead, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        this.findOrCreateGrowingLeafOrEmbeddedNode(newHead, oldHead, previous)
    }

    // for embedded segments
    fun pushEmbeddedToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        startPosition: Int,
        nextInputPosition: Int,
        oldHead: GrowingNode,
        previous: GrowingNodeIndex,
        embeddedTreeData: TreeData,
        skipData: TreeData?
    ) {
        if (null != previous) this._gss.push(previous, oldHead.index)
        //TODO: something different for embedded ?
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.treeData.createGrowingNodeIndex(newState, lookahead, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        this.findOrCreateGrowingLeafOrEmbeddedNode(newHead, oldHead, previous)
        val embGoal = embeddedTreeData.root!!
        val child = embeddedTreeData.childrenFor(embGoal.firstRule, startPosition, nextInputPosition)
        this.treeData.setEmbeddedChild(newHead, child.first().first())
        //TODO: do we need to check for longest ?
    }

    fun createWithFirstChild(
        parentState: ParserState,
        parentRuntimeLookaheadSet: LookaheadSet,
        childNode: GrowingNode,
        previous: GrowingNodeIndex
    ) {
        val nextInputPosition = if (childNode.isLeaf) childNode.nextInputPositionAfterSkip else childNode.nextInputPosition
        val parent = this.treeData.createGrowingNodeIndex(parentState, parentRuntimeLookaheadSet, childNode.startPosition, nextInputPosition, nextInputPosition, 1)
        val child = childNode.index

        if (parent.state.isAtEnd) {
            val existingComplete = this.treeData.completedBy(parent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeWithExistingComplete(existingComplete, null, parent, child)
                when (shouldDrop) {
                    1 -> {
                        createNewHeadAndDropExisting(existingComplete, parent, previous)
                        addNewPreferredTreeData(existingComplete, null, parent, child)
                    }
                    0 -> createNewHeadAndKeepExisting(existingComplete, parent, previous)
                    -1 -> dropNewHeadAndKeepExisting(existingComplete, parent, previous)
                }
            } else {
                val existingGrowing = this.treeData.growing[parent]
                if (null != existingGrowing) {
                    // some other head has already added the first child
                    //TODO: check priority of child and replace or drop this head - or use GrowingChildren graph rather than list!
                    val existingPriority = existingGrowing[0].priorityList[0]
                    val newPriority = child.state.priorityList[0]
                    when {
                        existingPriority > newPriority -> {
                            //dropNewHeadAndKeepExisting()
                        }
                        newPriority > existingPriority -> {
                            //val nn = this.createGrowingNode(parent)
                            //this.addGrowingHead(previous, nn)
                            this.treeData.setFirstChild(parent, child)
                        }
                        else -> {
                            //TODO: something else here maybe
                            //val nn = this.createGrowingNode(parent)
                            //this.addGrowingHead(previous, nn)
                            this.treeData.setFirstChild(parent, child)
                        }
                    }
                } else {
                    //val nn = this.createGrowingNode(parent)
                    // this.addGrowingHead(previous, nn)
                    this.treeData.setFirstChild(parent, child)
                }
                val nn = this.createGrowingNode(parent)
                this.addGrowingHead(previous, nn)
            }
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
                        //dropNewHeadAndKeepExisting(ex)
                    }
                    newPriority > existingPriority -> {
                        //val nn = this.createGrowingNode(parent)
                        //this.addGrowingHead(previous, nn)
                        this.treeData.setFirstChild(parent, child)
                    }
                    else -> {
                        //TODO: something else here maybe
                        //val nn = this.createGrowingNode(parent)
                        //this.addGrowingHead(previous, nn)
                        this.treeData.setFirstChild(parent, child)
                    }
                }
            } else {
                //val nn = this.createGrowingNode(parent)
                // this.addGrowingHead(previous, nn)
                this.treeData.setFirstChild(parent, child)
            }
            val nn = this.createGrowingNode(parent)
            this.addGrowingHead(previous, nn)
        }

    }

    fun growNextChild(
        oldParentNode: GrowingNodeIndex,
        nextChildNode: GrowingNode,
        newParentState: ParserState,
        newParentRuntimeLookaheadSet: LookaheadSet
    ) {
        val newParentNumNonSkipChildren = oldParentNode.numNonSkipChildren + 1
        val previous = this.previousOf(oldParentNode)
        val nextInputPosition = if (nextChildNode.isLeaf) nextChildNode.nextInputPositionAfterSkip else nextChildNode.nextInputPosition
        val newParent = this.treeData.createGrowingNodeIndex(
            newParentState,
            newParentRuntimeLookaheadSet,
            oldParentNode.startPosition,
            nextInputPosition,
            nextInputPosition,
            newParentNumNonSkipChildren
        )
        val oldParent = oldParentNode
        val child = nextChildNode.index

        if (newParent.state.isAtEnd) {
            val existingComplete = this.treeData.completedBy(newParent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeWithExistingComplete(existingComplete, oldParent, newParent, child) //FIXME: don't convert to set
                when (shouldDrop) {
                    1 -> {
                        createNewHeadAndDropExisting(existingComplete, newParent, previous)
                        addNewPreferredTreeData(existingComplete, oldParent, newParent, child)
                    }
                    0 -> createNewHeadAndKeepExisting(existingComplete, newParent, previous)
                    -1 -> dropNewHeadAndKeepExisting(existingComplete, newParent, previous)
                }
            } else {
                val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
                when {
                    null == existingChild -> this.treeData.setInGrowingParentChildAt(oldParent, newParent, child)
                    else -> {
                        TODO()
                    }
                }
                //this.treeData.appendChild(oldParent, newParent, child)
                val nn = this.createGrowingNode(newParent)
                this.addGrowingHead(previous, nn)
            }
        } else {
            val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
            when {
                null == existingChild -> this.treeData.setInGrowingParentChildAt(oldParent, newParent, child)
                else -> {
                    this.treeData.setInGrowingParentChildAt(oldParent, newParent, child) //TODO()
                }
            }
            //this.treeData.appendChild(oldParent, newParent, child)
            val nn = this.createGrowingNode(newParent)
            this.addGrowingHead(previous, nn)
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

    private fun prevOfToString(n: GrowingNodeIndex): String {
        val prev = this._gss.peek(n).toList()
        return when {
            prev.isEmpty() -> ""
            1 == prev.size -> {
                val p = prev.first()
                " --> $p${this.prevOfToString(p)}"
            }
            else -> {
                val p = prev.first()
                " -${prev.size}-> $p${this.prevOfToString(p)}"
            }
        }
    }

    override fun toString(): String {
        val heads = this._growingHeadHeap.toList()
        return heads.joinToString(separator = "\n") { h ->
            val p = this.prevOfToString(h.index)
            "$h$p"
        }
    }
}