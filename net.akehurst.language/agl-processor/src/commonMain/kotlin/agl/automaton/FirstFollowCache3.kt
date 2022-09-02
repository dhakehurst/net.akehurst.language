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

import net.akehurst.language.agl.agl.automaton.FirstOf
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf
import net.akehurst.language.collections.mutableStackOf

internal class FirstFollowCache3(val stateSet: ParserStateSet) {

    internal companion object {

        interface FollowDeferred {
            val parentInfo: Set<Pair<RulePosition, RulePosition>>
            val containsEmptyRules: Boolean
            val resolvedTerminals: Set<RuntimeRule>

            fun process()
        }

        data class FollowDeferredLiteral(
            private val _terminals: Set<RuntimeRule>
        ) : FollowDeferred {
            companion object {
                val EOT = FollowDeferredLiteral(setOf(RuntimeRuleSet.END_OF_TEXT))
                val RT = FollowDeferredLiteral(setOf(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD))
                val EMPTY = FollowDeferredLiteral(emptySet())
            }

            override val parentInfo: Set<Pair<RulePosition, RulePosition>> = emptySet()
            override val containsEmptyRules: Boolean by lazy { _terminals.any { it.isEmptyRule } }
            override val resolvedTerminals: Set<RuntimeRule> = _terminals

            override fun process() {
                // No need to do anything
            }

            override fun toString(): String = "[${_terminals.joinToString { it.tag }}]"
        }

        class FollowDeferredComposite private constructor(
            _parts: Set<FollowDeferred>
        ) : FollowDeferred {

            companion object {
                fun constructOrDelegate(parts: Set<FollowDeferred>) = when (parts.size) {
                    1 -> parts.first()
                    else -> FollowDeferredComposite(parts)
                }
            }

            val parts: Set<FollowDeferred> = _parts.flatMap {
                when (it) {
                    is FollowDeferredLiteral -> listOf(it)
                    is FollowDeferredComposite -> it.parts
                    is FollowDeferredCalculation -> listOf(it)
                    else -> error("Internal Error: subtype '${it::class.simpleName}' of 'FollowDeferred' not handled")
                }
            }.toSet()

            override val parentInfo: Set<Pair<RulePosition, RulePosition>> = parts.flatMap {
                when (it) {
                    is FollowDeferredLiteral -> emptySet()
                    is FollowDeferredComposite -> error("Internal Error: FollowDeferredComposite should have already been flattened")
//                    is FollowDeferredComposite -> it.parts.flatMap { it.parentInfo }.toSet()
                    is FollowDeferredCalculation -> it.parentInfo
//                    is FollowDeferredCalculation -> setOf(it.rulePosition)
                    else -> error("Internal Error: subtype '${it::class.simpleName}' of 'FollowDeferred' not handled")
                }
            }.toSet()
            override val containsEmptyRules: Boolean by lazy { parts.map { it.containsEmptyRules }.reduce { acc, b -> acc || b } }
            override val resolvedTerminals: Set<RuntimeRule> by lazy { parts.flatMap { it.resolvedTerminals }.toSet() }

            override fun process() {
                parts.forEach { it.process() }
            }

            override fun hashCode(): Int = this.parts.hashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is FollowDeferredComposite -> false
                else -> this.parts == other.parts
            }

            override fun toString(): String = parts.joinToString(separator = "+") { "$it" }
        }

        class FollowDeferredCalculation(
            val ffc: FirstFollowCache3,
            val context: RulePosition,
            val rulePosition: RulePosition,
//            val nextContext: Set<RulePosition>,
            val nextContextFollow: FollowDeferred
        ) : FollowDeferred {

            // used to resolve recursion issue
            private var _resolvedFollow_resolveTerminals: Set<RuntimeRule>? = null
            private var _resolvedFollow: FollowDeferred? = null
            private var _resolvedContainsEmpty: Boolean? = null

            override val parentInfo: Set<Pair<RulePosition, RulePosition>> = when (nextContextFollow) {
                is FollowDeferredLiteral -> emptySet()
                is FollowDeferredComposite -> nextContextFollow.parts.flatMap { it.parentInfo }.toSet()
                is FollowDeferredCalculation -> {
                    nextContextFollow.parentInfo + Pair(nextContextFollow.context, nextContextFollow.rulePosition)
                }
                //is FollowDeferredCalculation -> setOf(parentFollow.rulePosition)
                else -> TODO()
            }

            override val containsEmptyRules: Boolean by lazy {
                this.process()
                _resolvedContainsEmpty!!
            }

            override val resolvedTerminals: Set<RuntimeRule> by lazy {
                this.process()
                if (null == _resolvedFollow_resolveTerminals) {
                    // resolveTerminals could be recursive, so set to empty set before calling to recursion terminates
                    _resolvedFollow_resolveTerminals = emptySet()
                    _resolvedFollow_resolveTerminals = _resolvedFollow!!.resolvedTerminals
                }
                _resolvedFollow_resolveTerminals!!
            }

            override fun process() {
                if (null == this._resolvedFollow) {
                    this._resolvedFollow = ffc.firstOfInContext(context, rulePosition, nextContextFollow)// nextContext, nextContextFollow)
                    this._resolvedContainsEmpty = ffc.needsNext(rulePosition)
                }
            }

            private val _id = arrayOf(context, rulePosition, parentInfo)
            override fun hashCode(): Int = _id.contentHashCode()
            override fun equals(other: Any?): Boolean = when {
                other !is FollowDeferredCalculation -> false
                else -> this._id.contentEquals(other._id)
            }

            override fun toString(): String = "follow(${context},$rulePosition)"//-$followAtEnd"
        }

        data class FirstTerminalInfo(
            val embeddedRule: RuntimeRule,
            val terminalRule: RuntimeRule,
            val nextContextFollow: FollowDeferred
        ) {
            override fun toString(): String = "${terminalRule.tag}[$nextContextFollow]"
        }

        data class ParentOfInContext(
            val parentNextContextFollow: FollowDeferred,
            val parentNext: Set<ParentNext>,
            val parent: RulePosition
        )
    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<ClosureItem, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FirstTerminalInfo>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FollowDeferred>>> { lazyMutableMapNonNull { hashSetOf() } }

    // context -> ( RulePosition -> Boolean indicates if closure does not resolve an Empty terminal )
    private val _needsNext = lazyMutableMapNonNull<RulePosition, Boolean> { false } //TODO: should we default to false or null ?

    // prev/context -> ( RuntimeRule -> set of Terminal-RuntimeRules that could follow given RuntimeRule in given context/prev )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<FollowDeferred>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<firstOfPrev, firstOfRP>> )
    //private val _followInContextAsReferenceToFirstOf =
    //    lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<refPrev, refRuntimeRule>> )
    //   private val _followInContextAsReferenceToFollow =
    //       lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RuntimeRule>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentNext>>> { lazyMutableMapNonNull { hashSetOf() } }

    fun clear() {
        this._doneFollow.clear()
        this._firstTerminal.clear()
        this._followInContext.clear()
        this._needsNext.clear()
        this._firstOfInContext.clear()
        //       this._followInContextAsReferenceToFirstOf.clear()
        //       this._followInContextAsReferenceToFollow.clear()
        this._parentInContext.clear()
    }

    // entry point from calcWidth
    // target states for WIDTH transition, rulePosition should NOT be atEnd
    //fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, nextContext:Set<RulePosition>, nextContextFollow: FollowDeferred): Set<FirstTerminalInfo> {
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, nextContextFollow: FollowDeferred): Set<FirstTerminalInfo> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        processClosureFor(context, rulePosition, nextContextFollow) //nextContext, nextContextFollow)
        //processClosureFor(context, rulePosition, FollowDeferredLiteral(runtimeLookahead))
        return this._firstTerminal[context][rulePosition]
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should be atEnd
    // entry point from calcHeightGraft
    fun parentInContext(contextContext: RulePosition, context: RulePosition, nextContext: Set<RulePosition>, completedRule: RuntimeRule): Set<ParentNext> {
        processClosureFor(contextContext, context, FollowDeferredLiteral.RT) //nextContext, FollowDeferredLiteral.RT)

        val ctx = if (context.isAtStart) contextContext else context
        return this._parentInContext[ctx][completedRule]
    }

    //private fun firstOfInContext(context: RulePosition, rulePosition: RulePosition, nextContext:Set<RulePosition>, nextContextFollow: FollowDeferred): FollowDeferred {
    private fun firstOfInContext(context: RulePosition, rulePosition: RulePosition, nextContextFollow: FollowDeferred): FollowDeferred {
        if (Debug.CHECK) check(context.isAtEnd.not()) { "firstOf($context,$rulePosition)" }
        val nextContextFollowTerms = nextContextFollow.resolvedTerminals
//        processClosureFor(context, rulePosition, nextContextFollow) //nextContext, nextContextFollow)
        if (this._firstOfInContext[context].containsKey(rulePosition)) {
            return FollowDeferredComposite.constructOrDelegate(this._firstOfInContext[context][rulePosition])
        } else {
            val fo = FirstOf(stateSet).expectedAt(rulePosition,LookaheadSetPart.createFromRuntimeRules(nextContextFollowTerms))
            val fd = FollowDeferredLiteral(fo.fullContent)
            //this._firstOfInContext[context][rulePosition].add(FollowDeferredLiteral(fo.fullContent))
            //error("Internal Error: followInContext not calculated for ($context,$rulePosition)")
            return fd
        }
    }

    internal fun needsNext(rulePosition: RulePosition) =
        this._needsNext[rulePosition]!!// ?: error("Internal Error: needsNext not computed for ($context,$rulePosition)")

    /**
     * when {
     *   rulePosition.isEmpty -> true
     *   childNeedsNext -> rulePosition.next().any { if it.isAtEnd then true else it.needsNext }
     *   else -> false
     * }
     */
    internal fun needsNext(rulePosition: RulePosition, childNeedsNext: Boolean): Boolean = when {
        rulePosition.isEmptyRule -> true
        childNeedsNext -> {
            if (Debug.CHECK) check(rulePosition.isAtEnd.not()) { "Internal Error: rulePosition of ClosureItem should never be at end" }
            val nexts = rulePosition.next()
            nexts.any { nxt ->
                when {
                    nxt.isAtEnd -> true
                    else -> this.needsNext(nxt)
                }
            }
        }

        else -> false
    }

    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    //internal fun processClosureFor(context: RulePosition, rulePosition: RulePosition, nextContext:Set<RulePosition>, nextContextFollow: FollowDeferred) {
    internal fun processClosureFor(context: RulePosition, rulePosition: RulePosition, nextContextFollow: FollowDeferred) {
        //val cls = ClosureItemRoot(this, context, rulePosition, parentFollow)
        val graph = ClosureGraph(this, context, rulePosition, nextContextFollow) //nextContext, nextContextFollow)
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

    private fun setNeedsNext(rulePosition: RulePosition, needsNext: Boolean) {
        if (this._needsNext.containsKey(rulePosition)) {
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($rulePosition) = $needsNext || ${this._needsNext[rulePosition]}" }
            //this._needsNext[rulePosition] = this._needsNext[rulePosition] || needsNext
            if (Debug.CHECK) check(needsNext == this._needsNext[rulePosition]) { "Internal Error: Expected that needsNext is always the same for a rule position - $rulePosition" }
        } else {
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($rulePosition) = $needsNext" }
            this._needsNext[rulePosition] = needsNext
        }
    }

    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalAndFollowInContext(prev: RulePosition, rulePosition: RulePosition, firstTerminalInfo: FirstTerminalInfo) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstTerm($prev,$rulePosition) = $firstTerminalInfo" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(firstTerminalInfo)
        if (firstTerminalInfo.terminalRule.isEmptyRule) {
            //add follow of first term as follow of rp
            this.addFollowInContext(prev, rulePosition, firstTerminalInfo.nextContextFollow)
        } else {
            // add first term as follow of rp
            this.addFollowInContext(prev, rulePosition, FollowDeferredLiteral(setOf(firstTerminalInfo.terminalRule)))
        }
    }

    private fun addFollowInContext(prev: RulePosition, rulePosition: RulePosition, follow: FollowDeferred) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($prev,$rulePosition) = $follow" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(follow)
    }

    private fun addFollowInContext(prev: RulePosition, runtimeRule: RuntimeRule, follow: FollowDeferred) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($prev,${runtimeRule.tag}) = $follow" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followInContext[prev][runtimeRule].add(follow)
    }

    //private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentOf: ParentOfInContext) {
    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentNext: Set<ParentNext>) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add parentOf($prev,${completedRule.tag}) = $parentNext" }
        this._parentInContext[prev][completedRule].addAll(parentNext)
    }

    private fun calcFirstTermClosure(graph: ClosureGraph) {

        //TODO: when we get duplicate closureItem, connect it don't discard it
        // creating a upside down tree.
        // then process closure needs to work from bottom up doing all branches.


        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: ${graph.root}" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        //val done = mutableSetOf<ClosureItem>()
        //val reachedTerminal = mutableSetOf<ClosureItem>()
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(graph.root)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            val item = cls.rulePosition.item ?: error("Internal Error: should never be null")
            when {
                item.isTerminal -> {
                    val child = cls.createAndAddChild(item.asTerminalRulePosition)
                    //cls.addChild(child)
                    //reachedTerminal.add(child)
                }

                item.isEmbedded -> {
                    val child = cls.createAndAddChild(item.asTerminalRulePosition)
                    //cls.addChild(child)
                    //reachedTerminal.add(child)
                }

                else -> {
                    val childRulePositions = item.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val child = cls.createAndAddChild(childRp)
                        ///cls.addChild(child)
                        if (null == child) {
                            // don't follow down the closure
                            //val short = child.shortString
                        } else {
                            todoList.enqueue(child)
                            //println("todo: ${childCls.shortString}")
                        }
                        //done.add(child)
                    }
                }
            }
        }
        graph.resolveAllChildParentInfo()

        //for (bottom in reachedTerminal) {
        //    this.processClosurePaths(graph, bottom)
        //println(bottom.shortString)
        //}

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
        for (dwn in graph.root.downInfo) {
            doForRoot(graph.root.rulePosition, graph.root.upInfo, dwn)
            when (graph.root.rulePosition.isAtStart) {
                true -> doForStart(graph.root.rulePosition, graph.root.upInfo, dwn)
                false -> when (graph.root.rulePosition.isTerminal) {
                    false -> doForNonTerminalAndNonStart(graph.root.rulePosition, graph.root.upInfo, dwn)
                    true -> Unit
                }
            }
        }
        for (cls in graph.nonRootClosures) {
            for (dwn in cls.downInfo) {
                when (cls.isRoot) {
                    true -> doForRoot(cls.rulePosition, cls.upInfo, dwn)
                    false -> doForNonRoot(cls.rulePosition, cls.upInfo)
                }
                when (cls.rulePosition.isAtStart) {
                    true -> doForStart(cls.rulePosition, cls.upInfo, dwn)
                    false -> when (cls.rulePosition.isTerminal) {
                        false -> doForNonTerminalAndNonStart(cls.rulePosition, cls.upInfo, dwn)
                        true -> Unit
                    }
                }
                when (cls.rulePosition.isEmbedded) {
                    true -> doForEmbedded(cls.rulePosition, cls.upInfo, dwn)
                    false -> Unit
                }
            }
        }
    }

    /**
     * return if 'needsNext', ie. true IF {
     *    isTerminal && isEmpty OR
     *    next.isAtEnd && child.needsNext
     * }
     */
    private fun setFirsts(isClosureRoot: Boolean, firstTerminalInfo: FirstTerminalInfo?, rp: RulePosition, cpInfo: RulePositionUpInfo, childNeedsNext: Boolean): Boolean {
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

        val prev = cpInfo.context
        val rr = rp.runtimeRule

        // set parentOf
        when {
            isClosureRoot -> Unit
            else -> {
                this.addParentInContext(prev, rr, cpInfo.parentNext)
            }
        }
        // set follow for each runtime-rule - HEIGHT/GRAFT needs it
        //this.addFollowInContext(prev, rr, cpInfo.childNextContextFollow)

        when {
            rp.isTerminal -> Unit
            rp.isEmbedded -> {
                firstTerminalInfo?.let { this.addFirstTerminalAndFollowInContext(prev, rp, firstTerminalInfo) }
                if (childNeedsNext) {
                    this.addFollowInContext(prev, rp, cpInfo.nextNotAtEndFollow)
                    cpInfo.nextNotAtEndFollow.containsEmptyRules
                } else {
                    // do nothing
                }
            }

            isClosureRoot -> {
                // for targetState
                firstTerminalInfo?.let { this.addFirstTerminalAndFollowInContext(prev, rp, firstTerminalInfo) }
                if (childNeedsNext) {
                    this.addFollowInContext(prev, rp, cpInfo.nextNotAtEndFollow)
                    cpInfo.nextNotAtEndFollow.containsEmptyRules
                } else {
                    // do nothing
                }
            }

            rp.isAtStart -> {
                if (childNeedsNext) {
                    // ensures stuff is cached for the follow
                    cpInfo.nextNotAtEndFollow.process()
                } else {
                    //do nothing
                }
            }

            else -> {
                error("should not happen")
            }
        }
        val needsNext = this.needsNext(rp, childNeedsNext)
        when {
            rr.isTerminal -> Unit
            rp.isAtStart -> Unit
            else -> {
                this.setNeedsNext(rp, needsNext)
                when (needsNext) {
                    true -> this.addFollowInContext(prev, rp, cpInfo.nextNotAtEndFollow)
                    false -> Unit
                }
            }
        }

        return needsNext
    }

    /**
     * RulePositionUpInfo(parent <-- child)-rulePosition-RulePositionDownInfo(parent <-- child)
     * in RulePositionUpInfo, rulePosition is the Child
     * in RulePositionDownInfo, rulePosition is the parent
     */
    private fun doForRoot(rulePosition: RulePosition, upInfo: RulePositionUpInfo, downInfo: RulePositionDownInfo) {
        this.addFirstTerminalAndFollowInContext(upInfo.context, rulePosition, downInfo.firstTerminalInfo)
        if (downInfo.childNeedsNext) {
            this.addFollowInContext(upInfo.context, rulePosition, upInfo.nextNotAtEndFollow)
            upInfo.nextNotAtEndFollow.process()
        } else {
            // do nothing
        }
    }

    private fun doForNonRoot(rulePosition: RulePosition, upInfo: RulePositionUpInfo) {
        this.addParentInContext(upInfo.context, rulePosition.runtimeRule, upInfo.parentNext)
    }

    private fun doForStart(rulePosition: RulePosition, upInfo: RulePositionUpInfo, downInfo: RulePositionDownInfo) {
        if (downInfo.childNeedsNext) {
            // ensures stuff is cached for the follow
            upInfo.nextNotAtEndFollow.process()
        } else {
            //do nothing
        }
    }

    private fun doForNonTerminalAndNonStart(rulePosition: RulePosition, upInfo: RulePositionUpInfo, downInfo: RulePositionDownInfo) {
        this.setNeedsNext(rulePosition, downInfo.childNeedsNext)
        when (downInfo.childNeedsNext) {
            true -> this.addFollowInContext(upInfo.context, rulePosition, upInfo.nextNotAtEndFollow)
            false -> Unit
        }
    }

    private fun doForEmbedded(rulePosition: RulePosition, upInfo: RulePositionUpInfo, downInfo: RulePositionDownInfo) {
        this.addFirstTerminalAndFollowInContext(upInfo.context, rulePosition, downInfo.firstTerminalInfo)
        if (downInfo.childNeedsNext) {
            this.addFollowInContext(upInfo.context, rulePosition, upInfo.nextNotAtEndFollow)
            upInfo.nextNotAtEndFollow.process()
        } else {
            // do nothing
        }
    }
}