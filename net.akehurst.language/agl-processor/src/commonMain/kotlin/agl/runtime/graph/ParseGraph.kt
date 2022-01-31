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

import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.collections.BinaryHeap
import net.akehurst.language.agl.collections.GraphStructuredStack
import net.akehurst.language.agl.collections.binaryHeap
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind

internal class ParseGraph(
    val input: InputFromString,
    val stateSetNumber: Int,
    numTerminalRules: Int,
    numNonTerminalRules: Int
) {

    enum class MergeOptions {
        /**
         * New head provides NON preferable parse due to higher priority or longer match
         */
        PREFER_EXISTING,

        /**
         * New head provides preferable parse due to higher priority or longer match
         */
        PREFER_NEW,

        /**
         * Cannot decide between new head and existing based on priority and length
         */
        UNDECIDABLE,

        /**
         * Ambiguous choice defined in grammar so keep both
         */
        KEEP_BOTH_AS_ALTERNATIVES,
    }

    internal val _goals: MutableSet<GrowingNode> = mutableSetOf()

    //should be all the nodes that are in _gss
    //val _nodes = mutableMapOf<GrowingNodeIndex, GrowingNode>()

    // to keep track of the stack and get 'previous' nodes
    var _gss = GraphStructuredStack<GrowingNodeIndex>()

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

    val nextHeadStartPosition: Int get() {
        val root = this._growingHeadHeap.peekRoot
        return when {
            null==root -> Int.MAX_VALUE
            root.index.state.isGoal -> -1
            else -> root.startPosition
        }
    }
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

    /**
     * return true if a new head was created
     */
    internal fun addGrowingHead(oldHead: GrowingNodeIndex, next: GrowingNode):Boolean {
        //this._nodes[next.index] = next
        val new = this._gss.push(oldHead, next.index)
        return if (new) {
            this._growingHeadHeap[next.index] = next
            true
        } else {
            false
        }
    }

    /**
     * return true if a new head was created
     */
    private fun addGrowingHead(oldHead: Set<GrowingNodeIndex>, next: GrowingNode):Boolean {
        // this._nodes[next.index] = next
        return if (oldHead.isEmpty()) {
            this._gss.root(next.index)
            this._growingHeadHeap[next.index] = next
            true
        } else {
            var new = false
            for (oh in oldHead) {
                val r = this._gss.push(oh, next.index)
                new = new || r
            }
            if (new) {
                this._growingHeadHeap[next.index] = next
                true
            } else {
                false
            }
        }
    }

    private fun addNewPreferredTreeData(oldParent: GrowingNodeIndex?, newParent: GrowingNodeIndex, newChild: GrowingNodeIndex) {
        if (null == oldParent) {
            // should not remove existing, because it can be part of newParent sub tree
            // setting the child will also reset the preferred
            this.treeData.setFirstChild(newParent, newChild, false)
        } else {
            this.treeData.setInGrowingParentChildAt(oldParent, newParent, newChild, false)
        }
    }

    private fun addAlternativeTreeData(existing: CompleteNodeIndex, oldParent: GrowingNodeIndex?, newParent: GrowingNodeIndex, newChild: GrowingNodeIndex) {
        if (null == oldParent) {
            // should not remove existing, because it can be part of newParent
            // setting the child will also reset the preferred
            this.treeData.setFirstChild(newParent, newChild, true)
        } else {
            this.treeData.setInGrowingParentChildAt(oldParent, newParent, newChild, true)
        }
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        this._gss.pop(existing.gni!!)
        //TODO: maybe this._growingHeadHeap.remove(existing.gni)
        val nn = this.createGrowingNode(newHead)
        this.addGrowingHead(previous, nn)
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndKeepExisting(newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        val nn = this.createGrowingNode(newHead)
         this.addGrowingHead(previous, nn)
    }

    /**
     * return true if a new head was created
     */
    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex) {
        //TODO: probably something to clean up here!
        val i = 0

    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        this._gss.pop(existing.gni!!)
        //TODO: maybe this._growingHeadHeap.remove(existing.gni)
        val nn = this.createGrowingNode(newHead)
         this.addGrowingHead(previous, nn)
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndKeepExisting(newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        val nn = this.createGrowingNode(newHead)
         this.addGrowingHead(previous, nn)
    }

    /**
     * return true if a new head was created
     */
    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        //TODO: probably something to clean up here!
        val i = 0
    }

    /**
     * return true if a new head was created
     */
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
    private fun mergeDecision(
        existingNode: CompleteNodeIndex,
        newNode: GrowingNodeIndex,
    ): MergeOptions {
        return when {
            newNode.state.isChoice -> {
                check(newNode.state.choiceKindList.size == 1) //TODO: could remove this check if always passes
                when (newNode.state.firstRuleChoiceKind) {
                    RuntimeRuleChoiceKind.NONE -> error("should never happen")
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> {
                        val existingLength = existingNode.nextInputPosition - existingNode.startPosition
                        val newLength = newNode.nextInputPosition - newNode.startPosition
                        when {
                            newLength > existingLength -> MergeOptions.PREFER_NEW
                            existingLength > newLength -> MergeOptions.PREFER_EXISTING
                            else -> {
                                val existingPriority = existingNode.priorityList[0] //TODO: is there definitely only 1 ?
                                val newPriority = newNode.state.priorityList[0]
                                when {
                                    newPriority > existingPriority -> MergeOptions.PREFER_NEW
                                    existingPriority > newPriority -> MergeOptions.PREFER_EXISTING
                                    //TODO: are we sure that previous is the same?
                                    else -> {
                                        //do nothing, drop current head
                                        // because length and priority are the same
                                        //TODO: maybe raise a warning or info that this has happened
                                        //TODO: are we sure that previous is the same?
                                        MergeOptions.UNDECIDABLE
                                    }
                                }
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> {
                        val existingPriority = existingNode.priorityList[0] //TODO: is there definitely only 1 ?
                        val newPriority = newNode.state.priorityList[0]
                        when {
                            newPriority > existingPriority -> MergeOptions.PREFER_NEW
                            //TODO: new to remove all parents and up of existing !!
                            existingPriority > newPriority -> MergeOptions.PREFER_EXISTING
                            else -> {
                                val existingLength = existingNode.nextInputPosition - existingNode.startPosition
                                val newLength = newNode.nextInputPosition - newNode.startPosition
                                //TODO: not sure...can we drop the second head?
                                // it possibly has different runtimeLookahead!
                                when {
                                    newLength > existingLength -> MergeOptions.PREFER_NEW
                                    existingLength > newLength -> MergeOptions.PREFER_EXISTING
                                    else -> MergeOptions.UNDECIDABLE
                                }
                            }
                        }
                    }
                    RuntimeRuleChoiceKind.AMBIGUOUS -> MergeOptions.KEEP_BOTH_AS_ALTERNATIVES
                }
            }
            else -> {
                //2 diff heads parse same thing at same place but it is not a choice
                //TODO: not sure...can we drop the second head?
                // it possibly has different runtimeLookahead!

                val existingLength = existingNode.nextInputPosition - existingNode.startPosition
                val newLength = newNode.nextInputPosition - newNode.startPosition
                when {
                    newLength > existingLength -> MergeOptions.PREFER_NEW
                    existingLength > newLength -> MergeOptions.PREFER_EXISTING
                    else -> MergeOptions.UNDECIDABLE
                }
            }
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

    /**
     * return true if grown
     */
    fun pushToStackOf(
        newState: ParserState,
        lookahead: LookaheadSet,
        startPosition: Int,
        nextInputPosition: Int,
        oldHead: GrowingNode,
        previous: GrowingNodeIndex?,
        skipData: TreeData?
    ) :Boolean {
        if (null != previous) this._gss.push(previous, oldHead.index)
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.treeData.createGrowingNodeIndex(newState, lookahead, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
         this.findOrCreateGrowingLeafOrEmbeddedNode(newHead, oldHead, previous)
        return true
    }

    /**
     * return true if grown
     */
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
    ):Boolean {
        this._gss.push(previous, oldHead.index)
        //TODO: something different for embedded ?
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.treeData.createGrowingNodeIndex(newState, lookahead, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        val embGoal = embeddedTreeData.root!!
        val children = embeddedTreeData.childrenFor(embGoal.firstRule, embeddedTreeData.startPosition!!, embeddedTreeData.nextInputPosition!!)
        val child = children.first().second[0]
        this.treeData.setEmbeddedChild(newHead, child)
         this.findOrCreateGrowingLeafOrEmbeddedNode(newHead, oldHead, previous)
        //TODO: do we need to check for longest ?
        return true
    }

    /**
     * return true if grown
     */
    fun createWithFirstChild(
        parentState: ParserState,
        parentRuntimeLookaheadSet: LookaheadSet,
        childNode: GrowingNode,
        previous: GrowingNodeIndex
    ):Boolean {
        val nextInputPosition = if (childNode.isLeaf) childNode.nextInputPositionAfterSkip else childNode.nextInputPosition
        val parent = this.treeData.createGrowingNodeIndex(parentState, parentRuntimeLookaheadSet, childNode.startPosition, nextInputPosition, nextInputPosition, 1)
        val child = childNode.index

        return if (parent.state.isAtEnd) {
            val existingComplete = this.treeData.preferred(parent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeDecision(existingComplete, parent)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(null, parent, child)
                        createNewHeadAndDropExisting(existingComplete, parent, previous)
                        true
                    }
                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingComplete, parent, previous)
                        false
                    }
                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingComplete, null, parent, child)
                        createNewHeadAndKeepExisting(parent, previous)
                        true
                    }
                    MergeOptions.UNDECIDABLE -> {
                        createNewHeadAndKeepExisting(parent, previous)
                        true
                    }
                }
            } else {
                val existingGrowing = this.treeData.growing[parent]
                if (null != existingGrowing) {
                    // some other head has already added the first child
                    //TODO: check priority of child and replace or drop this head - or use GrowingChildren graph rather than list!
                    val existingPriority = existingGrowing[0].priorityList[0]
                    val newPriority = child.state.priorityList[0]
                    when {
                        newPriority > existingPriority -> {
                            addNewPreferredTreeData(null, parent, child)
                            createNewHeadAndDropExisting(existingGrowing[0], parent, previous)
                            true
                        }
                        existingPriority > newPriority -> {
                            dropNewHeadAndKeepExisting(existingGrowing[0], parent, previous)
                            false
                        }
                        else -> {
                            createNewHeadAndKeepExisting(parent, previous)
                            true
                        }
                    }
                } else {
                    addNewPreferredTreeData(null, parent, child)
                    createNewHeadAndKeepExisting(parent, previous)
                    true
                }
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
                val existingFirstChild = existingGrowing[0]
                val m = this.mergeDecision(existingFirstChild, child)
                when (m) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(null, parent, child)
                        createNewHeadAndDropExisting(existingFirstChild, parent, previous)
                        true
                    }
                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingFirstChild, parent, previous)
                        false
                    }
                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingFirstChild, null, parent, child)
                        createNewHeadAndKeepExisting(parent, previous)
                        true
                    }
                    MergeOptions.UNDECIDABLE -> {
                        createNewHeadAndKeepExisting(parent, previous)
                        //addNewPreferredTreeData(existingComplete, null, parent, child)
                        true
                    }
                }
            } else {
                addNewPreferredTreeData(null, parent, child)
                createNewHeadAndKeepExisting(parent, previous)
                true
            }
        }
    }

    /**
     * return true if grown
     */
    fun growNextChild(
        oldParentNode: GrowingNodeIndex,
        nextChildNode: GrowingNode,
        newParentState: ParserState,
        newParentRuntimeLookaheadSet: LookaheadSet
    ):Boolean {
        val newParentNumNonSkipChildren = oldParentNode.numNonSkipChildren + 1
        // pop oldParentNode here, because its previous will be linked to the newParentNode
        //val previous = this.previousOf(oldParentNode)
        val previous = this._gss.pop(oldParentNode)
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

        return if (newParent.state.isAtEnd) {
            val existingComplete = this.treeData.preferred(newParent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeDecision(existingComplete, newParent)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(oldParent, newParent, child)
                        createNewHeadAndDropExisting(existingComplete, newParent, previous)
                        true
                    }
                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingComplete, newParent, previous)
                        false
                    }
                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingComplete, oldParent, newParent, child)
                        createNewHeadAndKeepExisting(newParent, previous)
                        true
                    }
                    MergeOptions.UNDECIDABLE -> {
                        //addNewPreferredTreeData(existingComplete, oldParent, newParent, child)
                        createNewHeadAndKeepExisting(newParent, previous)
                        true
                    }
                }
            } else {
                val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
                if (null != existingChild) {
                    val existingPriority = existingChild.priorityList[0]
                    val newPriority = child.state.priorityList[0]
                    when {
                        newPriority > existingPriority -> {
                            addNewPreferredTreeData(null, newParent, child)
                            createNewHeadAndDropExisting(existingChild, newParent, previous)
                            true
                        }
                        existingPriority > newPriority -> {
                            dropNewHeadAndKeepExisting(existingChild, newParent, previous)
                            false
                        }
                        else -> {
                            createNewHeadAndKeepExisting(newParent, previous)
                            true
                        }
                    }
                } else {
                    addNewPreferredTreeData(oldParent, newParent, child)
                    createNewHeadAndKeepExisting(newParent, previous)
                    true
                }
            }
        } else {
            val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
            if (null != existingChild) {
                val existingPriority = existingChild.priorityList[0]
                val newPriority = child.state.priorityList[0]
                when {
                    newPriority > existingPriority -> {
                        addNewPreferredTreeData(null, newParent, child)
                        createNewHeadAndDropExisting(existingChild, newParent, previous)
                        true
                    }
                    existingPriority > newPriority -> {
                        dropNewHeadAndKeepExisting(existingChild, newParent, previous)
                        false
                    }
                    else -> {
                        createNewHeadAndKeepExisting(newParent, previous)
                        true
                    }
                }
            } else {
                addNewPreferredTreeData(oldParent, newParent, child)
                createNewHeadAndKeepExisting(newParent, previous)
                true
            }
        }
    }

    fun drop(previous: GrowingNodeIndex?) {
        if (null!=previous) {
            this._gss.removeStack(previous)
        }
    }

    fun isLookingAt(lookaheadGuard: LookaheadSet, runtimeLookahead: LookaheadSet?, nextInputPosition: Int): Boolean {
        //runtimeLookahead should never includeUP
        return when {
            null != runtimeLookahead && runtimeLookahead.includesUP -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            lookaheadGuard.matchANY -> true
            null == runtimeLookahead -> lookaheadGuard.content.any { this.input.isLookingAt(nextInputPosition,it) } //this.input.isLookingAt(nextInputPosition, lookaheadGuard.regex)
            LookaheadSet.UP == lookaheadGuard -> when {
                runtimeLookahead.matchANY -> true
                LookaheadSet.EOT == runtimeLookahead -> this.input.isEnd(nextInputPosition)
                else -> when {
                    runtimeLookahead.includesEOT && this.input.isEnd(nextInputPosition)->true
                    runtimeLookahead.content.isEmpty() -> false
                    else -> runtimeLookahead.content.any { this.input.isLookingAt(nextInputPosition,it) }
                }
            }
            else -> {
                val lhs = lookaheadGuard.resolveUP(runtimeLookahead)
                when {
                    lhs.matchANY -> true
                    LookaheadSetPart.EOT == lhs -> this.input.isEnd(nextInputPosition)
                    else -> when {
                        lhs.includesEOT && this.input.isEnd(nextInputPosition)->true
                        lhs.content.isEmpty() -> false
                        else -> lhs.content.any { this.input.isLookingAt(nextInputPosition,it) }
                    }
                }

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
            "${h.index}$p"
        }
    }
}