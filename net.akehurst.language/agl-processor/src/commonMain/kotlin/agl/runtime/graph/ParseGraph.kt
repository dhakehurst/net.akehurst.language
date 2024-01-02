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
import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.sppt.TreeData
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.collections.binaryHeap

internal class ParseGraph(
    val sentence: Sentence,
    val scanner: Scanner,
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

    internal val _goals: MutableSet<CompleteNodeIndex> = mutableSetOf()

    // TODO: is the fifo version faster ? it might help with processing heads in a better order!
    var _gss = GraphStructuredStack<GrowingNodeIndex>(binaryHeap { parent, child ->
        //val _growingHeadHeap: BinaryHeapFifo<GrowingNodeIndex, GrowingNode> = binaryHeapFifo { parent, child ->
        // Ordering rules:
        // 1) nextInputPosition lower number first
        // 2) shift before reduce (reduce happens if state.isAtEnd)
        // 3) choice point order - how to order diff choice points?
        // 4) choice last
        // 5) high priority first if same choice point...else !!
        //TODO: how do we ensure lower choices are done first ?
        when {
//            parent.startPosition == child.startPosition && parent.state.firstRule.isEmptyTerminal -> -1
//            parent.startPosition == child.startPosition && child.state.firstRule.isEmptyTerminal -> 1
            // 1) nextInputPosition lower number first
            parent.nextInputPositionAfterSkip < child.nextInputPositionAfterSkip -> 1
            parent.nextInputPositionAfterSkip > child.nextInputPositionAfterSkip -> -1
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
    })

    var treeData = TreeDataGrowing<GrowingNodeIndex, CompleteNodeIndex>(stateSetNumber)

    val goals: Set<CompleteNodeIndex> get() = this._goals

    var goalMatchedAll = true

    val canGrow: Boolean get() = this._gss.isEmpty.not()

    val numberOfHeads get() = this._gss.numberOfHeads

    val hasNextHead: Boolean get() = this._gss.hasNextHead

    val nextHeadNextInputPosition: Int
        get() {
            val root = this._gss.peekFirstHead
            return when {
                null == root -> Int.MAX_VALUE
                //root.state.isGoal -> -1
                else -> root.nextInputPositionAfterSkip//startPosition
            }
        }

    val peekNextHead get() = this._gss.peekFirstHead!!

    val isEmpty: Boolean get() = _gss.isEmpty && treeData.isClean

    fun createGrowingNodeIndex(
        state: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPositionBeforeSkip: Int,
        nextInputPositionAfterSkip: Int,
        numNonSkipChildren: Int,
        childrenPriorities: List<List<Int>>? //maybe may a map, so we can have lists of priorities of preferred descendants!
    ): GrowingNodeIndex {
        val listSize = GrowingNodeIndex.listSize(state.runtimeRules.first(), numNonSkipChildren)
        return GrowingNodeIndex(
            runtimeState = RuntimeState(state, runtimeLookaheadSet),
            startPosition = startPosition,
            nextInputPositionBeforeSkip = nextInputPositionBeforeSkip,
            nextInputPositionAfterSkip = nextInputPositionAfterSkip,
            numNonSkipChildren = listSize,
            childrenPriorities = childrenPriorities
        )
    }

    fun peekAllNextToProcess(): List<ToProcessTriple> = this._gss.heads.flatMap {
        val gn = it
        val previous = this._gss.peekPrevious(gn)
        if (previous.isEmpty()) {
            listOf(ToProcessTriple(gn, null, null))
        } else {
            previous.flatMap { prev ->
                val heads = this._gss.peekPrevious(prev)
                if (heads.isEmpty()) {
                    listOf(ToProcessTriple(gn, prev, null))
                } else {
                    heads.map { hd -> ToProcessTriple(gn, prev, hd) }
                }
            }
        }
    }

    fun peekTripleFor(gn: GrowingNodeIndex): List<ToProcessTriple> {
        val previous = this._gss.peekPrevious(gn)
        return if (previous.isEmpty()) {
            listOf(ToProcessTriple(gn, null, null))
        } else {
            previous.flatMap { prev ->
                val heads = this._gss.peekPrevious(prev)
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
        this._gss.pushTriple(cur, prev, rhd)
    }

    fun reset() {
        this.scanner.reset()
        //       this.completeNodes.clear()
        this._gss.clear()
        this._goals.clear()
        //TODO: don't want to create new one of these each time we parse skip
        // but currently can't reuse it as it carries the skip data
        this.treeData = TreeDataGrowing(stateSetNumber)
    }

    /**
     * return next if a new head was created else oldHead
     */
    private fun addGrowingHead(oldHead: GrowingNodeIndex?, next: GrowingNodeIndex) {
        when {
            null == oldHead -> {
                this._gss.root(next)
            }

            else -> {
                //this._growingHeadHeap.remove(oldHead)
                val new = this._gss.push(oldHead, next) // true if this._gss.contains(next)==false
                //if (new) {
                //    this._growingHeadHeap[next] = next
                //} else {
                //
                //}
            }
        }
    }

    fun dropGrowingHead(head: GrowingNodeIndex): Set<GrowingNodeIndex> {
        return this._gss.pop(head)
    }

    private fun preferNewCompleteParentFirstChild(oldParent: CompleteNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex): Boolean {
        this.treeData.setFirstChildForComplete(newParent.complete, child, false)
        //this.dropGrowingHead(oldParent.gni!!) //TODO: do we need to do this...would one be growing ?
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun preferNewCompleteParentLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex?): Boolean {
        // addNewPreferredTreeData(null, newParent, child)
        //createNewHeadAndDropExisting(oldParent, newParent, previous)
        this.treeData.setNextChildForCompleteParent(oldParent, newParent.complete, child, false)
        this.dropGrowingHead(oldParent) //TODO: do we need to do this...would one be growing ?
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun preferExistingCompleteParentFirstChild(oldParent: CompleteNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex): Boolean {
        // ( dropNewHeadAndKeepExisting )
        // don't change TreeData
        // keep oldParent - no need to do anything with it
        // newParent should not have been added to anything......what if it already has been, should we remove it ?
        // child is the head we are not growing...will be removed later if not grown by other transitions
        // previous not useful
        return false // did not 'grow' the head
    }

    private fun preferExistingCompleteParentLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex?): Boolean {
        // ( dropNewHeadAndKeepExisting )
        // don't change TreeData
        // keep oldParent - no need to do anything with it
        // newParent should not have been added to anything......what if it already has been, should we remove it ?
        // child is the head we are not growing...will be removed later if not grown by other transitions
        // previous not useful
        return false // did not 'grow' the head
    }

    private fun keepBothCompleteParentsFirstChild(oldParent: CompleteNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex): Boolean {
        this.treeData.setFirstChildForComplete(newParent.complete, child, true)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun keepBothCompleteParentsLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex?): Boolean {
        this.treeData.setNextChildForCompleteParent(oldParent, newParent.complete, child, true)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun useExistingCompleteParentFirstChild(newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex): Boolean {
        //this.treeData.setFirstChildForComplete(oldParent.complete, child, false)
        //this.addGrowingHead(previous, oldParent)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun useExistingCompleteParentLastChild(newParent: GrowingNodeIndex, child: CompleteNodeIndex, previous: GrowingNodeIndex?): Boolean {
        //this.treeData.setInCompleteParentChildAt(oldParent, oldParent.complete, child, false)
        //this.addGrowingHead(previous, oldParent)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun mergeDecisionOnLength(existingParent: CompleteNodeIndex, newParent: CompleteNodeIndex, ifEqual: () -> MergeOptions): MergeOptions {
        val existingLength = existingParent.nextInputPositionAfterSkip - existingParent.startPosition
        val newLength = newParent.nextInputPositionAfterSkip - newParent.startPosition
        return when {
            newLength > existingLength -> MergeOptions.PREFER_NEW
            existingLength > newLength -> MergeOptions.PREFER_EXISTING
            else -> ifEqual()
        }
    }

    private fun mergeDecisionOnPriority(existingParent: CompleteNodeIndex, newParent: CompleteNodeIndex, ifEqual: () -> MergeOptions): MergeOptions {
        if (Debug.CHECK) check(existingParent.state.isChoice && newParent.state.isChoice && existingParent.state.runtimeRules == newParent.state.runtimeRules) //are we comparing things with a choice
        val existingPriority = existingParent.priorityList[0] //TODO: is there definitely only 1 ?
        val newPriority = newParent.priorityList[0]
        return when {
            newPriority > existingPriority -> MergeOptions.PREFER_NEW
            existingPriority > newPriority -> MergeOptions.PREFER_EXISTING
            else -> ifEqual()
        }
    }

    private fun mergeDecision(existingParent: CompleteNodeIndex, newParent: CompleteNodeIndex): MergeOptions {
        return when {
            newParent.state.isChoice -> {
                if (Debug.CHECK) check(newParent.state.choiceKindList.size == 1) //TODO: could remove this check if always passes
                when (newParent.state.firstRuleChoiceKind) {
                    RuntimeRuleChoiceKind.NONE -> error("should never happen")
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> mergeDecisionOnLength(existingParent, newParent) {
                        mergeDecisionOnPriority(existingParent, newParent) {
                            MergeOptions.UNDECIDABLE
                        }
                    }

                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> mergeDecisionOnPriority(existingParent, newParent) {
                        mergeDecisionOnLength(existingParent, newParent) {
                            MergeOptions.UNDECIDABLE
                        }
                    }

                    RuntimeRuleChoiceKind.AMBIGUOUS -> MergeOptions.KEEP_BOTH_AS_ALTERNATIVES
                }
            }

            else -> mergeDecisionOnLength(existingParent, newParent) {
                MergeOptions.UNDECIDABLE
            }
        }
    }

    fun previousOf(gn: GrowingNodeIndex): Set<GrowingNodeIndex> = this._gss.peekPrevious(gn)

    /**
     * START
     */
    fun start(goalState: ParserState, startPosition: Int, runtimeLookahead: Set<LookaheadSet>, initialSkipData: TreeData?): GrowingNodeIndex {
        val nextInputPositionAfterSkip = initialSkipData?.root?.nextInputPosition ?: startPosition
        val st = this.createGrowingNodeIndex(goalState, runtimeLookahead, nextInputPositionAfterSkip, nextInputPositionAfterSkip, nextInputPositionAfterSkip, 0, null)
        this._gss.root(st)
        this.treeData.initialise(st, initialSkipData)
        return st
    }

    /**
     * WIDTH
     *
     * return true if grown (always grows)
     */
    fun pushToStackOf(
        head: GrowingNodeIndex,
        newState: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        skipData: TreeData?
    ): Boolean {
        val nextInputPositionAfterSkip = skipData?.root?.nextInputPosition ?: nextInputPosition
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0, null)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        this.addGrowingHead(head, newHead)
        return true
    }

    /**
     * EMBED
     *
     * return true if grown (always grows)
     */
    // for embedded segments
    fun pushEmbeddedToStackOf(
        head: GrowingNodeIndex,
        newState: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        embeddedTreeData: TreeData,
        skipData: TreeData?
    ): Boolean {
        val nextInputPositionAfterSkip = skipData?.root?.nextInputPosition ?: nextInputPosition
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0, null)
        if (null != skipData) {
            this.treeData.setSkipDataAfter(newHead.complete, skipData)
        }
        val children = embeddedTreeData.childrenFor(embeddedTreeData.root!!)
        val child = children.first().second[0]
        this.treeData.setEmbeddedChild(newHead.complete, child, embeddedTreeData)

        this.addGrowingHead(head, newHead)
        return true
    }

    /**
     * HEIGHT
     *
     * return true if grown
     */
    fun createWithFirstChild(
        head: GrowingNodeIndex,
        previous: GrowingNodeIndex,
        parentState: ParserState,
        parentRuntimeLookaheadSet: Set<LookaheadSet>,
        buildSPPT: Boolean //TODO:
    ): Boolean {
        if (Debug.CHECK) check(head.isComplete)
        val child = head.complete
        //val nextInputPosition = if (head.isLeaf) head.nextInputPositionAfterSkip else head.nextInputPosition
        val childrenPriorities = listOf(head.state.priorityList)
        val parent =
            this.createGrowingNodeIndex(
                parentState,
                parentRuntimeLookaheadSet,
                head.startPosition,
                head.nextInputPositionBeforeSkip,
                head.nextInputPositionAfterSkip,
                1,
                childrenPriorities
            )
        return if (parent.isComplete) {
            val newParent = parent.complete
            val existingComplete = this.treeData.preferred(newParent)
            if (null != existingComplete) {
                // already completed this parent - should we use new or existing parent
                val shouldDrop = this.mergeDecision(existingComplete, newParent)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> preferNewCompleteParentFirstChild(existingComplete, parent, child, previous)
                    MergeOptions.PREFER_EXISTING -> preferExistingCompleteParentFirstChild(existingComplete, parent, child, previous)
                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> keepBothCompleteParentsFirstChild(existingComplete, parent, child, previous)
                    MergeOptions.UNDECIDABLE -> {
                        useExistingCompleteParentFirstChild(parent, child, previous)
                    }
                }
            } else {
                this.treeData.setFirstChildForComplete(newParent, child, false)
                this.addGrowingHead(previous, parent)
                true
            }
        } else {
            this.treeData.setFirstChildForGrowing(parent, child)
            this.addGrowingHead(previous, parent)
            true
        }
    }

    /**
     * GRAFT
     *
     * return true if grown
     */
    fun growNextChild(
        head: GrowingNodeIndex,
        previous: GrowingNodeIndex,
        prevPrev: GrowingNodeIndex?,
        newParentState: ParserState,
        newParentRuntimeLookaheadSet: Set<LookaheadSet>,
        buildSPPT: Boolean
    ): Boolean {
        if (Debug.CHECK) check(head.isComplete)
        val child = head.complete
        val newParentNumNonSkipChildren = previous.numNonSkipChildren + 1
        //val nextInputPosition = if (head.isLeaf) head.nextInputPositionAfterSkip else head.nextInputPosition
        val childPrio = listOf(head.state.priorityList)
        val childrenPriorities: List<List<Int>> = previous.childrenPriorities?.plus(childPrio)
            ?: childPrio //Goal scenario
        val newParent = this.createGrowingNodeIndex(
            newParentState,
            newParentRuntimeLookaheadSet,
            previous.startPosition,
            head.nextInputPositionBeforeSkip,
            head.nextInputPositionAfterSkip,
            newParentNumNonSkipChildren,
            childrenPriorities
        )

        return if (newParent.isComplete) {
            val existingComplete = this.treeData.preferred(newParent.complete)
            if (null != existingComplete) {
                val shouldDrop = this.mergeDecision(existingComplete, newParent.complete)
                when (shouldDrop) {
                    MergeOptions.PREFER_NEW -> preferNewCompleteParentLastChild(previous, newParent, child, prevPrev)
                    MergeOptions.PREFER_EXISTING -> preferExistingCompleteParentLastChild(previous, newParent, child, prevPrev)
                    MergeOptions.KEEP_BOTH_AS_ALTERNATIVES -> keepBothCompleteParentsLastChild(previous, newParent, child, prevPrev)
                    MergeOptions.UNDECIDABLE -> {
                        useExistingCompleteParentLastChild(newParent, child, prevPrev)
                    }
                }
            } else {
                if (buildSPPT) this.treeData.setNextChildForCompleteParent(previous, newParent.complete, child, false)
                this.addGrowingHead(prevPrev, newParent)
                true
            }
        } else {
            if (buildSPPT) this.treeData.setNextChildForGrowingParent(previous, newParent, child)
            this.addGrowingHead(prevPrev, newParent)
            true
        }
    }

    fun dropGrowingTreeData(head: GrowingNodeIndex) {
        if (head.isLeaf.not() && head.isComplete.not()) {
            when {
                //head.isComplete -> treeData.removeTreeComplete(head.complete)
                else -> treeData.removeTreeGrowing(head)
            }
        }
    }

    fun dropStackWithHead(head: GrowingNodeIndex) {
        val dropped = this._gss.dropStack(head) { n ->
            dropGrowingTreeData(n)
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
                    lhs.includesEOT && this.scanner.isEnd(sentence, nextInputPosition) -> true
                    lhs.content.isEmpty() -> false
                    else -> lhs.content.any { this.scanner.isLookingAt(sentence, nextInputPosition, it) }
                }
            }
        }
    }

    /**
     * eotLookahead && runtimeLookahead must not include RT
     */
    fun isLookingAt(lookaheadGuard: LookaheadSetPart, eotLookahead: LookaheadSetPart, runtimeLookahead: LookaheadSetPart, nextInputPosition: Int): Boolean {
        return when {
            lookaheadGuard.matchANY -> true
            runtimeLookahead.includesRT -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            eotLookahead.includesRT -> error("EOT lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            else -> {
                val rtResolved = runtimeLookahead.resolve(eotLookahead, runtimeLookahead)
                val lhs = lookaheadGuard.resolve(eotLookahead, rtResolved)
                when {
                    lhs.matchANY -> true
                    lhs.includesEOT && this.scanner.isEnd(sentence, nextInputPosition) -> true
                    lhs.content.isEmpty() -> false
                    else -> lhs.content.any { this.scanner.isLookingAt(sentence, nextInputPosition, it) }
                }
            }
        }
    }

    /**
     * GOAL
     */
    fun recordGoal(goal: GrowingNodeIndex) {
        doRecordGoal(goal.complete)
    }

    private fun doRecordGoal(goal: CompleteNodeIndex) {
        this.treeData.complete.setRoot(goal)
        this._goals.add(goal)
        this.goalMatchedAll = this.scanner.isEnd(sentence, goal.nextInputPositionAfterSkip)
    }

    private fun prevOfToString(n: GrowingNodeIndex): String {
        val prev = this._gss.peekPrevious(n).toList()
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
        val heads = this._gss.heads
        return "[${heads.size}] " + heads.joinToString(separator = "\n") { h ->
            val p = this.prevOfToString(h)
            "${h}$p"
        }
    }
}