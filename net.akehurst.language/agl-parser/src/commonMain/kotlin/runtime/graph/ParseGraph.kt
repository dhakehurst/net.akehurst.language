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

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsChoice
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.scanner.api.Scanner
import net.akehurst.language.automaton.leftcorner.LookaheadSet
import net.akehurst.language.automaton.leftcorner.LookaheadSetPart
import net.akehurst.language.automaton.leftcorner.ParserState
import net.akehurst.language.collections.binaryHeap
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData
import net.akehurst.language.sppt.treedata.TreeDataGrowing

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

    data class MergeDecision(
        val reason:List<MergeReason>,
        val choice:MergeChoice,
        val text:String
    )

    enum class MergeReason { LENGTH, CHOICE_RULE_LENGTH, CHOICE_RULE_PRIORITY, CHOICE_RULE_AMBIGUOUS, DYNAMIC_PRIORITY }

    enum class MergeChoice {
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

    internal val _goals: MutableSet<SpptDataNode> = mutableSetOf()

    // TODO: is the fifo version faster ? it might help with processing heads in a better order!
    var _gss = GraphStructuredStack<GrowingNodeIndex>(binaryHeap { parent, child ->
        //val _growingHeadHeap: BinaryHeapFifo<GrowingNodeIndex, GrowingNode> = binaryHeapFifo { parent, child ->
        // Ordering rules:
        // 1) nextInputPosition lower number first
        // 2) shift before reduce (reduce happens if state.isAtEnd)
        // 3) choice point order - how to order diff choice points?
        // 4) choice last
        // 5) high priority first if same choice point...else !!
        //TODO: Really don't want the parse to depend on order of this
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
                        // startPosition lower number first
                        parent.startPosition < child.startPosition -> 1
                        parent.startPosition > child.startPosition -> -1
                        else -> 0
                    }
                }

                parent.state.isAtEnd -> -1 // shift child first
                child.state.isAtEnd -> 1 // shift parent first
                else -> when {
                    // startPosition lower number first
                    parent.startPosition < child.startPosition -> 1
                    parent.startPosition > child.startPosition -> -1
                    else -> 0
                }
            }
        }
    })

    var treeData = TreeDataGrowing<GrowingNodeIndex, SpptDataNode>(stateSetNumber)

    val goals: Set<SpptDataNode> get() = this._goals

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
        childrenPriorities: List<List<Int>> //maybe may a map, so we can have lists of priorities of preferred descendants!
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

    private fun preferNewCompleteParentFirstChild(oldParent: SpptDataNode, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex): Boolean {
        this.treeData.setFirstChildForComplete(newParent.complete, child, false)
        //this.dropGrowingHead(oldParent.gni!!) //TODO: do we need to do this...would one be growing ?
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun preferNewCompleteParentLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex?): Boolean {
        // addNewPreferredTreeData(null, newParent, child)
        //createNewHeadAndDropExisting(oldParent, newParent, previous)
        this.treeData.setNextChildForCompleteParent(oldParent, newParent.complete, child, false)
        this.dropGrowingHead(oldParent) //TODO: do we need to do this...would one be growing ?
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun preferExistingCompleteParentFirstChild(oldParent: SpptDataNode, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex): Boolean {
        // ( dropNewHeadAndKeepExisting )
        // don't change TreeData
        // keep oldParent - no need to do anything with it
        // newParent should not have been added to anything......what if it already has been, should we remove it ?
        // child is the head we are not growing...will be removed later if not grown by other transitions
        // previous not useful
        return false // did not 'grow' the head
    }

    private fun preferExistingCompleteParentLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex?): Boolean {
        // ( dropNewHeadAndKeepExisting )
        // don't change TreeData
        // keep oldParent - no need to do anything with it
        // newParent should not have been added to anything......what if it already has been, should we remove it ?
        // child is the head we are not growing...will be removed later if not grown by other transitions
        // previous not useful
        return false // did not 'grow' the head
    }

    private fun keepBothCompleteParentsFirstChild(oldParent: SpptDataNode, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex): Boolean {
        this.treeData.setFirstChildForComplete(newParent.complete, child, true)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun keepBothCompleteParentsLastChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex?): Boolean {
        this.treeData.setNextChildForCompleteParent(oldParent, newParent.complete, child, true)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun useExistingCompleteParentFirstChild(newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex): Boolean {
        //this.treeData.setFirstChildForComplete(oldParent.complete, child, false)
        //this.addGrowingHead(previous, oldParent)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun useExistingCompleteParentLastChild(newParent: GrowingNodeIndex, child: SpptDataNode, previous: GrowingNodeIndex?): Boolean {
        //this.treeData.setInCompleteParentChildAt(oldParent, oldParent.complete, child, false)
        //this.addGrowingHead(previous, oldParent)
        this.addGrowingHead(previous, newParent)
        return true
    }

    private fun mergeDecisionOnLength(existingParent: SpptDataNode, newParent: SpptDataNode, reasonList:List<MergeReason>,ifEqual: (reasonList:List<MergeReason>) -> MergeDecision): MergeDecision {
        val newReasonList = reasonList+MergeReason.LENGTH
        val existingLength = existingParent.nextInputPosition - existingParent.startPosition
        val newLength = newParent.nextInputPosition - newParent.startPosition
        return when {
            newLength > existingLength -> MergeDecision(newReasonList,MergeChoice.PREFER_NEW, "$newLength > $existingLength")
            newLength < existingLength -> MergeDecision(newReasonList,MergeChoice.PREFER_EXISTING, "$newLength < $existingLength")
            else -> ifEqual(newReasonList)
        }
    }

    private fun mergeDecisionOnPriority(existingParent: SpptDataNode, newParent: SpptDataNode, reasonList:List<MergeReason>, ifEqual: (reasonList:List<MergeReason>) -> MergeDecision): MergeDecision {
        if (Debug.CHECK) check(existingParent.rule.isChoice && newParent.rule.isChoice && existingParent.rule == newParent.rule) //are we comparing things with a choice
        val newReasonList = reasonList+MergeReason.CHOICE_RULE_PRIORITY
        val existingPriority = existingParent.option.asIndex
        val newPriority = newParent.option.asIndex
        return when {
            newPriority > existingPriority -> MergeDecision(newReasonList,MergeChoice.PREFER_NEW,"$newPriority > $existingPriority")
            newPriority < existingPriority -> MergeDecision(newReasonList,MergeChoice.PREFER_EXISTING,"$newPriority < $existingPriority")
            else -> ifEqual(newReasonList)
        }
    }

    private fun mergeDecisionOnDynamicPriority(existingParent: SpptDataNode, newParent: SpptDataNode, reasonList:List<MergeReason>, ifEqual: ( reasonList:List<MergeReason>) -> MergeDecision): MergeDecision {
        val newReasonList = reasonList+MergeReason.DYNAMIC_PRIORITY
        for (i in existingParent.dynamicPriority.indices) {
            val e = existingParent.dynamicPriority[i]
            val n = newParent.dynamicPriority[i]
            when {
                n > e -> return MergeDecision(newReasonList,MergeChoice.PREFER_NEW, "${newParent.dynamicPriority} > ${existingParent.dynamicPriority}")
                n < e -> return MergeDecision(newReasonList,MergeChoice.PREFER_EXISTING, "${newParent.dynamicPriority} < ${existingParent.dynamicPriority}")
                else -> Unit
            }
        }
        return ifEqual(newReasonList)
    }

    private fun mergeDecision(existingParent: SpptDataNode, newParent: SpptDataNode): MergeDecision {
        return when {
            newParent.rule.isChoice -> {
                val rhs = (newParent.rule as RuntimeRule).rhs
                val choiceKind = when (rhs) {
                    is RuntimeRuleRhsChoice -> rhs.choiceKind
                    else -> null
                }
                if (Debug.CHECK) check(null != choiceKind) //TODO: could remove this check if always passes
                when (choiceKind!!) {
                    RuntimeRuleChoiceKind.NONE -> error("should never happen")
                    RuntimeRuleChoiceKind.LONGEST_PRIORITY -> mergeDecisionOnLength(existingParent, newParent, emptyList()) { rl ->
                        mergeDecisionOnPriority(existingParent, newParent,rl) { rl2 ->
                            MergeDecision(rl2,MergeChoice.UNDECIDABLE,"")
                        }
                    }

                    RuntimeRuleChoiceKind.PRIORITY_LONGEST -> mergeDecisionOnPriority(existingParent, newParent, emptyList()) {rl ->
                        mergeDecisionOnLength(existingParent, newParent, rl) {rl2 -> //FIXME: should never happen I think !
                            MergeDecision(rl2,MergeChoice.UNDECIDABLE,"")
                        }
                    }

                    RuntimeRuleChoiceKind.AMBIGUOUS -> MergeDecision(listOf( MergeReason.CHOICE_RULE_AMBIGUOUS),MergeChoice.KEEP_BOTH_AS_ALTERNATIVES,"")
                }
            }

            else -> mergeDecisionOnLength(existingParent, newParent, emptyList()) { rl ->
                mergeDecisionOnDynamicPriority(existingParent, newParent, rl) { rl2->
                    MergeDecision(rl2,MergeChoice.UNDECIDABLE,"${newParent.dynamicPriority} == ${existingParent.dynamicPriority}")
                }
            }
        }
    }

    fun previousOf(gn: GrowingNodeIndex): Set<GrowingNodeIndex> = this._gss.peekPrevious(gn)

    /**
     * START
     */
    fun start(goalState: ParserState, startPosition: Int, runtimeLookahead: Set<LookaheadSet>, initialSkipData: TreeData?): GrowingNodeIndex {
        val nextInputPositionAfterSkip = initialSkipData?.root?.nextInputPosition ?: startPosition
        val st = this.createGrowingNodeIndex(goalState, runtimeLookahead, nextInputPositionAfterSkip, nextInputPositionAfterSkip, nextInputPositionAfterSkip, 0, emptyList())
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
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0, emptyList())
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
        val newHead = this.createGrowingNodeIndex(newState, runtimeLookaheadSet, startPosition, nextInputPosition, nextInputPositionAfterSkip, 0, emptyList())
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
//        buildSPPT: Boolean //TODO:
    ): Boolean {
        if (Debug.CHECK) check(head.isComplete)
        val child = head.complete
        val childrenPriorities = when {
            head.state.isChoice -> listOf(head.state.priorityList)
            head.state.isOptional -> listOf(head.state.priorityList)
            head.state.isList -> listOf(head.state.priorityList)
            else -> emptyList()
        }
        val parent = this.createGrowingNodeIndex(
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
                val md = this.mergeDecision(existingComplete, newParent)
                println("MergeDecision-F on ${existingComplete} vs $newParent = ${md.choice} : ${md.reason} ${md.text}")
                when (md.choice) {
                    MergeChoice.PREFER_NEW -> preferNewCompleteParentFirstChild(existingComplete, parent, child, previous)
                    MergeChoice.PREFER_EXISTING -> preferExistingCompleteParentFirstChild(existingComplete, parent, child, previous)
                    MergeChoice.KEEP_BOTH_AS_ALTERNATIVES -> keepBothCompleteParentsFirstChild(existingComplete, parent, child, previous)
                    MergeChoice.UNDECIDABLE -> {
                        //useExistingCompleteParentFirstChild(parent, child, previous)
                        keepBothCompleteParentsFirstChild(existingComplete, parent, child, previous)
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
        //buildSPPT: Boolean  //TODO:
    ): Boolean {
        if (Debug.CHECK) check(head.isComplete)
        val child = head.complete
        val newParentNumNonSkipChildren = previous.numNonSkipChildren + 1
        val childPrio = when {
            head.state.isChoice -> listOf(head.state.priorityList)
            head.state.isOptional -> listOf(head.state.priorityList)
            head.state.isList -> listOf(head.state.priorityList)
            else -> emptyList()
        }
        val childrenPriorities: List<List<Int>> = previous.childrenPriorities.plus(childPrio)
        val parent = this.createGrowingNodeIndex(
            newParentState,
            newParentRuntimeLookaheadSet,
            previous.startPosition,
            head.nextInputPositionBeforeSkip,
            head.nextInputPositionAfterSkip,
            newParentNumNonSkipChildren,
            childrenPriorities
        )

        return if (parent.isComplete) {
            val newParent = parent.complete
            val existingComplete = this.treeData.preferred(newParent)
            if (null != existingComplete) {
                val md = this.mergeDecision(existingComplete,newParent)
                println("MergeDecision-N on ${existingComplete} vs $newParent = ${md.choice} : ${md.reason} ${md.text}")
                when (md.choice) {
                    MergeChoice.PREFER_NEW -> preferNewCompleteParentLastChild(previous, parent, child, prevPrev)
                    MergeChoice.PREFER_EXISTING -> preferExistingCompleteParentLastChild(previous, parent, child, prevPrev)
                    MergeChoice.KEEP_BOTH_AS_ALTERNATIVES -> keepBothCompleteParentsLastChild(previous, parent, child, prevPrev)
                    MergeChoice.UNDECIDABLE -> {
                        //useExistingCompleteParentLastChild(parent, child, prevPrev)
                        keepBothCompleteParentsLastChild(previous, parent, child, prevPrev)
                    }
                }
            } else {
                this.treeData.setNextChildForCompleteParent(previous, newParent, child, false)
                this.addGrowingHead(prevPrev, parent)
                true
            }
        } else {
            this.treeData.setNextChildForGrowingParent(previous, parent, child)
            this.addGrowingHead(prevPrev, parent)
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

    private fun doRecordGoal(goal: SpptDataNode) {
        this.treeData.complete.setRootTo(goal)
        this._goals.add(goal)
        this.goalMatchedAll = this.scanner.isEnd(sentence, goal.nextInputPosition)
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
        return "Heads [${heads.size}] \n" + heads.joinToString(separator = "\n") { h ->
            val p = this.prevOfToString(h)
            "  ${h}$p"
        }
    }
}