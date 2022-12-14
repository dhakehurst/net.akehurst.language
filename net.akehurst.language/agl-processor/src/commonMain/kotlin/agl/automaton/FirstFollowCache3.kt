/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf

/**
 * RP.firstOf(ifAtEnd)
 *   isAtEnd -> ifAtEnd
 *   else -> RP.symbols.rulePositionsAtStart.firstOf(ifAtEnd)
 *
 * R.followIn(ctx)
 *   R.parentOfIn(ctx).next.isAtEnd -> R.parentOfIn(ctx).followIn(R.parentOfIn(ctx).context)
 *   else -> R.parentOfIn(ctx).next.firstOf(
 */

internal class FirstFollowCache3 {

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<ClosureItem, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FirstTerminalInfo>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentNext>>> { lazyMutableMapNonNull { hashSetOf() } }

    private val _possibleContexts = lazyMutableMapNonNull<RulePosition, MutableSet<RulePosition>> { hashSetOf() }

    fun clear() {
        this._doneFollow.clear()
        this._firstTerminal.clear()
        this._parentInContext.clear()
        this._possibleContexts.clear()
    }

    val firstTerminalContexts
        get() = _firstTerminal.flatMap { (ctx, values) ->
            values.map { (rp, ft) ->
                Pair(ctx, rp)
            }
        }

    fun possibleContextsFor(rp: RulePosition): Set<RulePosition> = _possibleContexts[rp]

    // entry point from calcWidth
    // target states for WIDTH transition, rulePosition should NOT be atEnd
    //fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, nextContext:Set<RulePosition>, nextContextFollow: FollowDeferred): Set<FirstTerminalInfo> {
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: LookaheadSetPart): Set<FirstTerminalInfo> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        return  if (this._firstTerminal.containsKey(context) && this._firstTerminal[context].containsKey(rulePosition)) {
            this._firstTerminal[context][rulePosition]
        } else {
            processClosureFor(context, rulePosition, parentFollow)
            this._firstTerminal[context][rulePosition]
        }
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should be atEnd
    // entry point from calcHeightGraft
    fun parentInContext(contextContext: RulePosition, context: RulePosition, completedRule: RuntimeRule): Set<ParentNext> {
        val ctx = if (context.isAtStart) contextContext else context
        return if (this._parentInContext.containsKey(ctx) && this._parentInContext[ctx].containsKey(completedRule)) {
            this._parentInContext[ctx][completedRule]
        } else {
            //error("Internal Error: a call to firstTerminalInContext should set all needed entries in _parentInContext")
            processClosureFor(contextContext, context, LookaheadSetPart.RT)
            this._parentInContext[ctx][completedRule]
        }
    }

    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    //internal fun processClosureFor(context: RulePosition, rulePosition: RulePosition, nextContext:Set<RulePosition>, nextContextFollow: FollowDeferred) {
    private fun processClosureFor(context: RulePosition, rulePosition: RulePosition, parentFollow: LookaheadSetPart) {
        //val cls = ClosureItemRoot(this, context, rulePosition, parentFollow)
        val graph = ClosureGraph(context, rulePosition, parentFollow)
        val cls = graph.root
        val doit = when (this._doneFollow[context][cls]) {
            null -> true
            true -> false
            else -> false
        }
        if (doit) {
            this._doneFollow[context][cls] = true
            this.calcFirstTermClosure(graph)
        }
    }

    fun processAllClosures(context: RulePosition, rulePosition: RulePosition, parentFollow: LookaheadSetPart, parentParentNextFollow: LookaheadSetPart) {
        this.clear()
        val graph = ClosureGraph(context, rulePosition, parentFollow)
        calcAllClosure(graph)
    }

    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalInContext(prev: RulePosition, rulePosition: RulePosition, firstTerminalInfo: FirstTerminalInfo) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstTerm($prev,$rulePosition) = $firstTerminalInfo" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(firstTerminalInfo)
    }

    //private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentOf: ParentOfInContext) {
    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentNext: Set<ParentNext>) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add parentOf($prev,${completedRule.tag}) = $parentNext" }
        this._parentInContext[prev][completedRule].addAll(parentNext)
    }

    private fun addPossibleContext(rp: RulePosition, ctx: RulePosition) {
        this._possibleContexts[rp].add(ctx)
    }

    //internal not private so we can test it
    internal fun calcFirstTermClosure(graph: ClosureGraph) {
        //TODO: when we get duplicate closureItem, connect it don't discard it
        // creating a upside down tree.
        // then process closure needs to work from bottom up doing all branches.

        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: ${graph.root}" }

        if (graph.root.rulePosition.isGoal && graph.root.rulePosition.isAtEnd) {
            return
        } //TODO: else

        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(graph.root)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            for (item in cls.rulePosition.items) {
                when {
                    item.isTerminal -> graph.addChild(cls,item.asTerminalRulePosition)
                    item.isNonTerminal -> {
                        val childRps = item.rulePositionsAtStart
                        for (childRp in childRps) {
                            val child = graph.addChild(cls,childRp)
                            if (null == child) {
                                // don't follow down the closure
                                //val short = child.shortString
                            } else {
                                todoList.enqueue(child)
                                //println("todo: ${childCls.shortString}")
                            }
                        }
                    }

                    else -> error("Internal Error: should never happen")
                }
            }
        }
        graph.resolveAllChildParentInfo()

        this.cacheStuff(graph)

        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: ${graph.root}" }
    }

    //internal not private so we can test it
    internal fun calcAllClosure(graph: ClosureGraph) {
        //TODO: when we get duplicate closureItem, connect it don't discard it
        // creating a upside down tree.
        // then process closure needs to work from bottom up doing all branches.

        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: ${graph.root}" }

        if (graph.root.rulePosition.isGoal && graph.root.rulePosition.isAtEnd) {
            return
        } //TODO: else

        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(graph.root)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            for (item in cls.rulePosition.items) {
                when {
                    item.isTerminal -> graph.addChild(cls,item.asTerminalRulePosition)
                    item.isNonTerminal -> {
                        val childRps = item.rulePositions
                        for (childRp in childRps) {
                            val child = graph.addChild(cls,childRp)
                            if (null == child) {
                                // don't follow down the closure
                                //val short = child.shortString
                            } else {
                                todoList.enqueue(child)
                                //println("todo: ${childCls.shortString}")
                            }
                        }
                    }

                    else -> error("Internal Error: should never happen")
                }
            }
        }
        graph.resolveAllChildParentInfo()

        this.cacheStuff(graph)

        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: ${graph.root}" }
    }

    //internal so can be tested
    /*    internal fun calcAllClosures(closureStart: ClosureItem): Set<ClosureItem> {
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcClosures: $closureStart" }
            val completeClosures = mutableSetOf<ClosureItem>()
            val todoList = mutableStackOf<ClosureItem>()
            todoList.push(closureStart)
            while (todoList.isNotEmpty) {
                val cls = todoList.pop()
                when {
                    cls.rulePosition.isTerminal -> completeClosures.add(cls)
                    cls.rulePosition.item!!.isTerminal -> {
                        val childRp = cls.rulePosition.item!!.asTerminalRulePosition
                        todoList.push(cls.createChild(childRp))
                    }

                    else -> {
                        val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            val childCls = cls.createChild(childRp)
                            if (completeClosures.contains(childCls).not()) {
                                completeClosures.add(childCls)
                                todoList.push(childCls)
                            } else {
                                // already done
                            }
                        }
                    }
                }
            }
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcClosures: $closureStart" }
            return completeClosures
        }
    */
    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required

    private fun processClosurePaths(graph: ClosureGraph, bottom: ClosureItem) {
    if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosurePath: ${bottom}" }
    for (bottomParentRel in bottom.parentRels) {
    for (bottomParentInfo in bottomParentRel.upInfo) {
    val firstTerminalInfo = when {
    bottom.rulePosition.isTerminal -> FirstTerminalInfo(bottom.rulePosition.runtimeRule, bottom.rulePosition.runtimeRule, bottomParentInfo.nextContextFollow)
    bottom.rulePosition.isEmbedded -> {
    val item = bottom.rulePosition.runtimeRule
    val embeddedRuleSet = item.embeddedRuntimeRuleSet ?: error("Internal Error: should never be null")
    val embeddedRule = item.embeddedStartRule ?: error("Internal Error: should never be null")
    val embeddedStateSet = embeddedRuleSet.fetchStateSetFor(embeddedRule, this.stateSet.automatonKind)
    val embeddedFfc = FirstFollowCache3(embeddedStateSet)
    val s = embeddedFfc.firstTerminalInContext(
    bottomParentInfo.context,
    embeddedStateSet.startRulePosition,
    bottomParentInfo.nextContextFollow
    ) //bottomParentInfo.childNextContext, bottomParentInfo.childNextContextFollow)
    val tr = s.first().terminalRule //FIXME: could be more than 1
    val fn = FollowDeferredComposite.constructOrDelegate(s.map { it.nextContextFollow }.toSet())
    FirstTerminalInfo(bottom.rulePosition.runtimeRule, tr, fn)
    }

    else -> null
    }
    if (Debug.CHECK) firstTerminalInfo?.let { check(firstTerminalInfo.terminalRule.isTerminal) }

    //TODO: no need to start with terminal, start with next one up
    graph.traverseUpPaths(bottom) { isClosureRoot, rp, cpInfo, childNeedsNext ->
    this.setFirsts(isClosureRoot, firstTerminalInfo, rp, cpInfo, childNeedsNext)
    }
    }
    }
    if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosurePath: $bottom" }
    }
     */

    private fun cacheStuff(graph: ClosureGraph) {
        // End/Bottom of closure is always a terminal
        // Root/Start/Top of closure might be notAtEnd
        // every closure-item in between will always be atStart
        // when X we need to calculate {
        //   startState -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        //   atStart -> Nothing needed (unless startState)
        //   atEnd -> HEIGHT/GRAFT - targetState = parent.next, lookahead = firstOf(targetState)
        //   else (inMiddle) -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        // }
        // therefore
        //   parentOf is always set if not root of closure
        //   when {
        //      isTerminal (always atEnd) -> set follow
        //      aEnd -> set firstOf to firstOf nextClosure - first parent with next not atEnd
        //
        //   }
        // other than the start state, firstTerm & firstOf are never called on RPs at the start
        // also never called on a Terminal
        for (dwn in graph.root.downInfo) {
            doForRoot(graph.root.rulePosition, graph.root.context, dwn)
        }
        for (cls in graph.nonRootClosures) {
            if (cls.downInfo.isEmpty()) {
                this.addPossibleContext(cls.rulePosition, cls.context)
            } else {
                when {
                    cls.rulePosition.isAtStart -> doForNonRoot(cls)
                    cls.rulePosition.isTerminal -> doForNonRoot(cls)
                    else -> cls.downInfo.forEach { dwn -> doForRoot(cls.rulePosition, cls.context, dwn) }
                }
                //TODO handle closure not at start!
                //for (dwn in cls.downInfo) {
                //    when (cls.isRoot) {
                //        true -> doForRoot(cls.rulePosition, cls.upInfo, dwn)
                //        false -> doForNonRoot(cls.upInfo.context, cls.rulePosition, cls.parentNext)
                //    }
                //    when (cls.rulePosition.isEmbedded) {
                //        true -> doForEmbedded(cls.rulePosition, cls.upInfo, dwn)
                //        false -> Unit
                //    }
                //}
            }
        }
    }

    /**
     * RulePositionUpInfo(parent <-- child)-rulePosition-RulePositionDownInfo(parent <-- child)
     * in RulePositionUpInfo, rulePosition is the Child
     * in RulePositionDownInfo, rulePosition is the parent
     */
    private fun doForRoot(rulePosition: RulePosition, context: RulePosition, downInfo: FirstTerminalInfo) {
        this.addFirstTerminalInContext(context, rulePosition, downInfo)
        this.addPossibleContext(rulePosition, context)
    }

    private fun doForNonRoot(cls:ClosureItem) {
        val context = cls.context
        val rulePosition = cls.rulePosition
        val parentNext = cls.parentNext
        val completedRule = rulePosition.rule
        this.addParentInContext(context, completedRule, parentNext)
        this.addPossibleContext(rulePosition, context)
    }

    private fun doForEmbedded(rulePosition: RulePosition, upInfo: RulePositionUpInfo, downInfo: FirstTerminalInfo) {
        this.addFirstTerminalInContext(upInfo.context, rulePosition, downInfo)
        this.addPossibleContext(rulePosition, upInfo.context)
    }
}