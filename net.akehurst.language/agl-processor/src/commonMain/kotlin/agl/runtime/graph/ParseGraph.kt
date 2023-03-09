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

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.collections.BinaryHeap
import net.akehurst.language.agl.collections.GraphStructuredStack
import net.akehurst.language.agl.collections.binaryHeap
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.util.Debug

internal class ParseGraph(
    val input: InputFromString,
    val stateSetNumber: Int
) {

    companion object {
        data class ToProcessPrevious(
            val previous: GrowingNodeIndex?,
            val remainingHeads: Set<GrowingNodeIndex>
        )

        data class NextToProcess(
            val growingNode: GrowingNodeIndex,
            val previous: List<ToProcessPrevious>
        ) {
            //no need for overhead of a Set, previous is a Set so should be no duplicates
            val triples: List<ToProcessTriple>
                get() = when {
                    previous.isEmpty() -> listOf(ToProcessTriple(growingNode, null, null))
                    else -> previous.flatMap { prv ->
                        if (prv.remainingHeads.isEmpty()) {
                            listOf(ToProcessTriple(growingNode, prv.previous, null))
                        } else {
                            prv.remainingHeads.map { hd ->
                                ToProcessTriple(growingNode, prv.previous, hd)
                            }
                        }
                    }
                }
        }

        data class ToProcessTriple(
            val growingNode: GrowingNodeIndex,
            val previous: GrowingNodeIndex?,
            val remainingHead: GrowingNodeIndex?
        ) {
            override fun toString(): String = "$growingNode ==> $previous ==> $remainingHead"
        }
    }

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

    internal val _goals: MutableSet<GrowingNodeIndex> = mutableSetOf()

    //should be all the nodes that are in _gss
    //val _nodes = mutableMapOf<GrowingNodeIndex, GrowingNode>()

    // to keep track of the stack and get 'previous' nodes
    var _gss = GraphStructuredStack<GrowingNodeIndex>()

    // TODO: is the fifo version faster ? it might help with processing heads in a better order!
    // to order the heads efficiently so we grow them in the required order
    val _growingHeadHeap: BinaryHeap<GrowingNodeIndex, GrowingNodeIndex> = binaryHeap { parent, child ->
        //val _growingHeadHeap: BinaryHeapFifo<GrowingNodeIndex, GrowingNode> = binaryHeapFifo { parent, child ->
        // Ordering rules:
        // 1) nextInputPosition lower number first
        // 2) shift before reduce (reduce happens if state.isAtEnd)
        // 3) choice point order - how to order diff choice points?
        // 4) choice last
        // 5) high priority first if same choice point...else !!
        //TODO: how do we ensure lower choices are done first ?
        when {
            parent.startPosition==child.startPosition && parent.state.firstRule.isEmptyTerminal -> -1
            parent.startPosition==child.startPosition && child.state.firstRule.isEmptyTerminal -> 1
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
                        else -> {
                            0
                        }
                    }

                    else -> when {
                        // startPosition higher number first
                        parent.startPosition < child.startPosition -> -1
                        parent.startPosition > child.startPosition -> 1
                        else -> 0
                    }
                }

                parent.state.isAtEnd -> -1 // shift child first
                child.state.isAtEnd -> 1 // shift parent first
                else -> when {
                    // startPosition higher number first
                    parent.startPosition < child.startPosition -> -1
                    parent.startPosition > child.startPosition -> 1
                    else -> 0
                }
            }
        }
    }

    var treeData = TreeData(stateSetNumber)

    val goals: Set<GrowingNodeIndex> get() = this._goals

    var goalMatchedAll = true

    val canGrow: Boolean get() = !this._growingHeadHeap.isEmpty()

    val numberOfHeads get() = this._growingHeadHeap.size

    val hasNextHead: Boolean get() = this._growingHeadHeap.isNotEmpty()

    val nextHeadNextInputPosition: Int
        get() {
            val root = this._growingHeadHeap.peekRoot
            return when {
                null == root -> Int.MAX_VALUE
                //root.state.isGoal -> -1
                else -> root.nextInputPosition//startPosition
            }
        }

    val peekNextHead get() = this._growingHeadHeap.peekRoot!!

    fun createGrowingNodeIndex(
        state: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        nextInputPositionAfterSkip: Int,
        numNonSkipChildren: Int
    ): GrowingNodeIndex {
        val listSize = GrowingNodeIndex.listSize(state.runtimeRules.first(), numNonSkipChildren)
        return GrowingNodeIndex(this.treeData.complete, RuntimeState(state, runtimeLookaheadSet), startPosition, nextInputPosition, nextInputPositionAfterSkip, listSize)
    }

    /**
     * extract head with min nextInputPosition
     * assumes there is one - check with hasNextHead
     */
    fun nextToProcess(): NextToProcess {
        val gn = this._growingHeadHeap.extractRoot()!!
        //val previous = this._gss.peek(gn.index)
        val previous = this._gss.pop(gn)
        return if (previous.isEmpty()) {
            NextToProcess(gn, emptyList())
        } else {
            val prv = previous.map { prev ->
                val heads = this._gss.pop(prev)
                ToProcessPrevious(prev, heads)
            }
            NextToProcess(gn, prv)
        }
    }

    fun peekAllNextToProcess(): List<ToProcessTriple> = this._growingHeadHeap.entries.flatMap {
        val gn = it.value
        val previous = this._gss.peek(gn)
        if (previous.isEmpty()) {
            listOf(ToProcessTriple(gn, null, null))
        } else {
            previous.flatMap { prev ->
                val heads = this._gss.peek(prev)
                if (heads.isEmpty()) {
                    listOf(ToProcessTriple(gn, prev, null))
                } else {
                    heads.map { hd -> ToProcessTriple(gn, prev, hd) }
                }
            }
        }
    }

    fun peekTripleFor(gn: GrowingNodeIndex): List<ToProcessTriple> {
        val previous = this._gss.peek(gn)
        return if (previous.isEmpty()) {
            listOf(ToProcessTriple(gn, null, null))
        } else {
            previous.flatMap { prev ->
                val heads = this._gss.peek(prev)
                if (heads.isEmpty()) {
                    listOf(ToProcessTriple(gn, prev, null))
                } else {
                    heads.map { hd -> ToProcessTriple(gn, prev, hd) }
                }
            }
        }
    }

    fun pushback(trip: ToProcessTriple) {
        val (cur, prev, rhd) = trip
        this._growingHeadHeap[cur] = cur
        when {
            null == prev -> {
                this._gss.root(cur)
            }

            null == rhd -> {
                this._gss.push(prev, cur)
            }

            else -> {
                this._gss.push(rhd, prev)
                this._gss.push(prev, cur)
            }
        }
    }

    fun reset() {
        this.input.reset()
        //       this.completeNodes.clear()
        this._gss.clear()
        this._goals.clear()
        this._growingHeadHeap.clear()
        //TODO: don't want to create new one of these each time we parse skip
        // but currently can't reuse it as it carries the skip data
        this.treeData = TreeData(stateSetNumber)
    }

    /**
     * return next if a new head was created else oldHead
     */
    private fun addGrowingHead(oldHead: GrowingNodeIndex?, next: GrowingNodeIndex) {
        when {
            null==oldHead -> {
                this._gss.root(next)
                this._growingHeadHeap[next] = next
            }
            else -> {
                this._growingHeadHeap.remove(oldHead)
                val new = this._gss.push(oldHead, next) // true if this._gss.contains(next)==false
                if (new) {
                    this._growingHeadHeap[next] = next
                } else {
                    //
                }
            }
        }
    }

    /**
     * return true if a new head was created
     */
    private fun addGrowingHead(oldHead: Set<GrowingNodeIndex>, next: GrowingNodeIndex): Boolean {
        // this._nodes[next.index] = next
        return if (oldHead.isEmpty()) {
            this._gss.root(next)
            this._growingHeadHeap[next] = next
            true
        } else {
            var new = false
            for (oh in oldHead) {
                val r = this._gss.push(oh, next)
                new = new || r
            }
            if (new) {
                this._growingHeadHeap[next] = next
                true
            } else {
                false
            }
        }
    }

     fun dropGrowingHead(head:GrowingNodeIndex) {
        this._gss.pop(head)
        this._growingHeadHeap.remove(head)
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
    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex?) {
        this.dropGrowingHead(existing.gni!!)
        this.addGrowingHead(previous, newHead)
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndKeepExisting(newHead: GrowingNodeIndex, previous: GrowingNodeIndex?) {
        this.addGrowingHead(previous, newHead)
    }

    /**
     * return true if a new head was created
     */
    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: GrowingNodeIndex?) {
        //TODO: probably something to clean up here!
        val i = 0
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndDropExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        this.dropGrowingHead(existing.gni!!)
        this.addGrowingHead(previous, newHead)
    }

    /**
     * return true if a new head was created
     */
    private fun createNewHeadAndKeepExisting(newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        this.addGrowingHead(previous, newHead)
    }

    /**
     * return true if a new head was created
     */
    private fun dropNewHeadAndKeepExisting(existing: CompleteNodeIndex, newHead: GrowingNodeIndex, previous: Set<GrowingNodeIndex>) {
        //TODO: probably something to clean up here!
        val i = 0
    }

    /**
     * oldParent should be null if first child
     */
    private fun mergeDecision(
        existingNode: CompleteNodeIndex,
        newNode: GrowingNodeIndex,
    ): MergeOptions {
        return when {
            newNode.state.isChoice -> {
                if (Debug.CHECK) check(newNode.state.choiceKindList.size == 1) //TODO: could remove this check if always passes
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

    /**
     * START
     */
    fun start(goalState: ParserState, startPosition: Int, runtimeLookahead: Set<LookaheadSet>, initialSkipData: TreeDataComplete?): GrowingNodeIndex {
        val nextInputPositionAfterSkip = initialSkipData?.nextInputPosition ?: startPosition
        val st = this.createGrowingNodeIndex(goalState, runtimeLookahead, nextInputPositionAfterSkip, nextInputPositionAfterSkip, nextInputPositionAfterSkip, 0)
        //val goalGN = this.createGrowingNode(st)
        this._gss.root(st)
        this._growingHeadHeap[st] = st
        this.treeData.start(st, initialSkipData)
        return st
    }

    /**
     * WIDTH
     *
     * return true if grown (always grows)
     */
    fun pushToStackOf(
        head:GrowingNodeIndex,
        newState: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        skipData: TreeDataComplete?
    ): Boolean {
        //val oldHead = toProcess.growingNode
        //val previous = toProcess.previous
        //previous?.let {
        //    toProcess.remainingHead?.let { this._gss.push(toProcess.remainingHead, previous) }
        //    this._gss.push(previous, oldHead)
        //}
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        this.addGrowingHead(head,newHead)
        return true
    }

    /**
     * EMBED
     *
     * return true if grown (always grows)
     */
    // for embedded segments
    fun pushEmbeddedToStackOf(
        head:GrowingNodeIndex,
        newState: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        embeddedTreeData: TreeDataComplete,
        skipData: TreeDataComplete?
    ): Boolean {
        //val oldHead = toProcess.growingNode
        //val previous = toProcess.previous
        //previous?.let {
        //    toProcess.remainingHead?.let { this._gss.push(toProcess.remainingHead, previous) }
        //    this._gss.push(previous, oldHead)
        //}
        val nextInputPositionAfterSkip = skipData?.nextInputPosition ?: nextInputPosition
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        val embGoal = embeddedTreeData.root!!
        val children = embeddedTreeData.childrenFor(embGoal.firstRule, embeddedTreeData.startPosition!!, embeddedTreeData.nextInputPosition!!)
        val child = children.first().second[0]
        this.treeData.setEmbeddedChild(newHead, child)
        this.addGrowingHead(head,newHead)
        return true
    }

    /**
     * HEIGHT
     *
     * return true if grown
     */
    fun createWithFirstChild(
        head:GrowingNodeIndex,
        previous: GrowingNodeIndex,
        parentState: ParserState,
        parentRuntimeLookaheadSet: Set<LookaheadSet>,
        buildSPPT: Boolean //TODO:
    ): Boolean {
        //val childNode = toProcess.growingNode
        //val previous = toProcess.previous!!
        //if (null == toProcess.remainingHead) {
       //     this._gss.root(previous)
       // } else {
        //    this._gss.push(toProcess.remainingHead, previous)
        //}
        //this.dropGrowingHead(head)
        val nextInputPosition = if (head.isLeaf) head.nextInputPositionAfterSkip else head.nextInputPosition
        val parent = this.createGrowingNodeIndex(parentState, parentRuntimeLookaheadSet, head.startPosition, nextInputPosition, nextInputPosition, 1)
        //val child = childNode
        return if (parent.state.isAtEnd) {
            val existingComplete = this.treeData.preferred(parent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeDecision(existingComplete, parent)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(null, parent, head)
                        createNewHeadAndDropExisting(existingComplete, parent, previous)
                        true
                    }

                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingComplete, parent, previous)
                        false
                    }

                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingComplete, null, parent, head)
                        createNewHeadAndKeepExisting(parent, previous)
                        true
                    }

                    MergeOptions.UNDECIDABLE -> {
                        createNewHeadAndKeepExisting(parent, previous)
                        true
                    }
                }
            } else {
                val existingGrowing = this.treeData.growingChildren[parent]
                if (null != existingGrowing) {
                    // some other head has already added the first child
                    //TODO: check priority of child and replace or drop this head - or use GrowingChildren graph rather than list!
                    val existingPriority = existingGrowing[0].priorityList[0]
                    val newPriority = head.state.priorityList[0]
                    when {
                        newPriority > existingPriority -> {
                            addNewPreferredTreeData(null, parent, head)
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
                    addNewPreferredTreeData(null, parent, head)
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

            val existingGrowing = this.treeData.growingChildren[parent]
            if (null != existingGrowing) {
                // some other head has already added the first child
                val existingFirstChild = existingGrowing[0]
                val m = this.mergeDecision(existingFirstChild, head)
                when (m) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(null, parent, head)
                        createNewHeadAndDropExisting(existingFirstChild, parent, previous)
                        true
                    }

                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingFirstChild, parent, previous)
                        false
                    }

                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingFirstChild, null, parent, head)
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
                addNewPreferredTreeData(null, parent, head)
                createNewHeadAndKeepExisting(parent, previous)
                true
            }
        }
    }

    /**
     * GRAFT
     *
     * return true if grown
     */
    fun growNextChild(
        head:GrowingNodeIndex,
        previous: GrowingNodeIndex,
        prevPrev:GrowingNodeIndex?,
        newParentState: ParserState,
        newParentRuntimeLookaheadSet: Set<LookaheadSet>,
        buildSPPT: Boolean
    ): Boolean {
        //val nextChildNode = toProcess.growingNode
        //val oldParentNode = toProcess.previous!!
        //val prevPrev = toProcess.remainingHead?.let { setOf(it) } ?: emptySet<GrowingNodeIndex>()
        val newParentNumNonSkipChildren = previous.numNonSkipChildren + 1
        val nextInputPosition = if (head.isLeaf) head.nextInputPositionAfterSkip else head.nextInputPosition
        val newParent = this.createGrowingNodeIndex(
            newParentState,
            newParentRuntimeLookaheadSet,
            previous.startPosition,
            nextInputPosition,
            nextInputPosition,
            newParentNumNonSkipChildren
        )
        //val oldParent = oldParentNode
        //val child = nextChildNode
        //dropGrowingHead(head)
        //dropGrowingHead(previous) //FIXME: can't drop this here...might still be in use!

        return if (newParent.state.isAtEnd) {
            val existingComplete = this.treeData.preferred(newParent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeDecision(existingComplete, newParent)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> {
                        addNewPreferredTreeData(previous, newParent, head)
                        createNewHeadAndDropExisting(existingComplete, newParent, prevPrev)
                        true
                    }

                    MergeOptions.PREFER_EXISTING -> {
                        dropNewHeadAndKeepExisting(existingComplete, newParent, prevPrev)
                        false
                    }

                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> {
                        addAlternativeTreeData(existingComplete, previous, newParent, head)
                        createNewHeadAndKeepExisting(newParent, prevPrev)
                        true
                    }

                    MergeOptions.UNDECIDABLE -> {
                        //addNewPreferredTreeData(oldParent, newParent, child)
                        createNewHeadAndKeepExisting(newParent, prevPrev)
                        true
                    }
                }
            } else {
                val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
                if (null != existingChild) {
                    val existingPriority = existingChild.priorityList[0]
                    val newPriority = head.state.priorityList[0]
                    when {
                        newPriority > existingPriority -> {
                            if (buildSPPT) addNewPreferredTreeData(previous, newParent, head)
                            createNewHeadAndDropExisting(existingChild, newParent, prevPrev)
                            true
                        }

                        existingPriority > newPriority -> {
                            dropNewHeadAndKeepExisting(existingChild, newParent, prevPrev)
                            false
                        }

                        else -> {
                            createNewHeadAndKeepExisting(newParent, prevPrev)
                            true
                        }
                    }
                } else {
                    if (buildSPPT) addNewPreferredTreeData(previous, newParent, head)
                    createNewHeadAndKeepExisting(newParent, prevPrev)
                    true
                }
            }
        } else {
            val existingChild = this.treeData.inGrowingParentChildAt(newParent, newParentNumNonSkipChildren - 1)
            if (null != existingChild) {
                val existingPriority = existingChild.priorityList[0]
                val newPriority = head.state.priorityList[0]
                when {
                    newPriority > existingPriority -> {
                        if (buildSPPT) addNewPreferredTreeData(previous, newParent, head)
                        createNewHeadAndDropExisting(existingChild, newParent, prevPrev)
                        true
                    }

                    existingPriority > newPriority -> {
                        dropNewHeadAndKeepExisting(existingChild, newParent, prevPrev)
                        false
                    }

                    else -> {
                        createNewHeadAndKeepExisting(newParent, prevPrev)
                        true
                    }
                }
            } else {
                if (buildSPPT) addNewPreferredTreeData(previous, newParent, head)
                createNewHeadAndKeepExisting(newParent, prevPrev)
                true
            }
        }
    }

    fun dropStackWithHead(head: GrowingNodeIndex) {
        this._growingHeadHeap.remove(head)
        this._gss.removeStack(head)
    }

    fun dropData(node: GrowingNodeIndex) {
        // if still have a growing head with same complete then don't drop data
        val b = this._gss.roots.any { it.runtimeState.isAtEnd && it.complete == node.complete }
        if (b) {
            //still growing
        } else {
            this.treeData.removeTree(node) { gn -> this._gss.contains(gn) }
        }
    }

    /**
     * eotLookahead && runtimeLookahead must not include RT
     */
    fun isLookingAt(lookaheadGuard: LookaheadSet, eotLookahead: LookaheadSet, runtimeLookahead: LookaheadSet, nextInputPosition: Int): Boolean {
        return when {
            lookaheadGuard.matchANY -> true
            runtimeLookahead.includesRT -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            eotLookahead.includesRT -> error("EOT lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            else -> {
                val rtResolved = runtimeLookahead.resolve(eotLookahead, runtimeLookahead)
                val lhs = lookaheadGuard.resolve(eotLookahead, rtResolved)
                when {
                    lhs.matchANY -> true
                    lhs.includesEOT && this.input.isEnd(nextInputPosition) -> true
                    lhs.content.isEmpty() -> false
                    else -> lhs.content.any { this.input.isLookingAt(nextInputPosition, it) }
                }
            }
        }
    }

    /**
     * GOAL
     */
    fun recordGoal(goal: GrowingNodeIndex) {
        //this.treeData.setRoot(goal)
        this.treeData.complete.setRoot(goal.complete)
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
        return "[${heads.size}] "+ heads.joinToString(separator = "\n") { h ->
            val p = this.prevOfToString(h)
            "${h}$p"
        }
    }
}