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

        class FollowDeferredComposite(
            _parts: Set<FollowDeferred>
//            val parts: Set<FollowDeferred>
        ) : FollowDeferred {

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
            val parentFollow: FollowDeferred
        ) : FollowDeferred {

            // used to resolve recursion issue
            private var _resolvedFollow_resolveTerminals: Set<RuntimeRule>? = null
            private var _resolvedFollow: FollowDeferred? = null
            private var _resolvedContainsEmpty: Boolean? = null

            override val parentInfo: Set<Pair<RulePosition, RulePosition>> = when (parentFollow) {
                is FollowDeferredLiteral -> emptySet()
                is FollowDeferredComposite -> parentFollow.parts.flatMap { it.parentInfo }.toSet()
                is FollowDeferredCalculation -> {
                    parentFollow.parentInfo + Pair(parentFollow.context, parentFollow.rulePosition)
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
                    this._resolvedFollow = ffc.followInContext(context, rulePosition, parentFollow)
                    this._resolvedContainsEmpty = ffc.needsNext(context, rulePosition)
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
            val followNext: FollowDeferred
        ) {
            override fun toString(): String = "${terminalRule.tag}[$followNext]"
        }

        data class ParentOfInContext(
            val parentFollowAtEnd: FollowDeferred,
            val parentNextInfo: Set<Pair<RulePosition, FollowDeferred>>,
            val parent: RulePosition
        )

        /*
        /**
         * Identified by :
         *  - context, used for prev guard for transition
         *  - rulePosition, target (WIDTH) if terminal
         *  - parentNextFollow, used for lookahead guard (WIDTH)
         *  - parentOfInfo (HEIGHT/GRAFT)
         *  -- parentFollowAtEnd, used for lookahead up (HEIGHT)
         *  -- parentNextInfo, target (HEIGHT/GRAFT)
         *  --- parentNext, target (HEIGHT/GRAFT)
         *  --- parentNextFollow guard (HEIGHT/GRAFT)
         *  -- parentRulePosition, used for HEIGHT/GRAFT decision
         */
        interface ClosureItem {
            val ffc: FirstFollowCache3
            val context: RulePosition
            val rulePosition: RulePosition

            val nextNotAtEndFollow: FollowDeferred

            val parents: Set<ClosureItem>

            /**
             * all parents will/must have same rulePosition
             */
            val parentRulePosition: RulePosition

            /**
             * all parents will/must have same context RulePosition
             */
            val parentContext: RulePosition

            /**
             * parent.next.follow
             */
            val parentNextNotAtEndFollow: FollowDeferred

            /**
             * parent.parentNextFollow
             */
            val parentFollowAtEnd: FollowDeferred

            val parentOfInfo: ParentOfInContext
//            val followAtEnd:FollowDeferred

            /**
             * context, rulePosition, nextNotAtEndFollow, parentFollowAtEnd, parentNextNotAtEndFollow
             */
            val _id: Array<*>

            val shortString: List<String>

            fun needsNext(childNeedsNext: Boolean): Boolean
        }

        abstract class ClosureItemAbstract(override val ffc: FirstFollowCache3) : ClosureItem {

            /**
             * when {
             *   rulePosition.isEmpty -> true
             *   childNeedsNext -> when {
             *      rulePosition.isAtEnd -> true
             *      rulePosition.next().any { it.needsNext }
             *   }
             *   else -> false
             * }
             */
            override fun needsNext(childNeedsNext: Boolean): Boolean = when {
                rulePosition.isEmptyRule -> true
                childNeedsNext -> when {
                    rulePosition.isAtEnd -> true
                    else -> {
                        val nexts = rulePosition.next()
                        nexts.any { nxt ->
                            when {
                                nxt.isAtEnd -> true
                                else -> ffc.needsNext(context, nxt)
                            }
                        }
                    }
                }

                else -> false
            }

            override val nextNotAtEndFollow: FollowDeferred by lazy {
                when {
                    rulePosition.isAtEnd -> parentNextNotAtEndFollow
                    else -> {
                        val nexts = rulePosition.next()
                        val allNextFollow = nexts.map { next ->
                            when {
                                next.isAtEnd -> parentNextNotAtEndFollow
                                else -> FollowDeferredCalculation(ffc, context, next, parentNextNotAtEndFollow)
                            }
                        }
                        FollowDeferredComposite(allNextFollow.toSet())
                    }
                }
            }

            override val shortString: List<String>
                get() {
                    val rp = this.rulePosition
                    val rr = rp.runtimeRule
                    val str = when {
                        rr.isNonTerminal && rr.rhs.listKind == RuntimeRuleListKind.SEPARATED_LIST -> when {
                            rp.isAtStart -> rr.tag + 'b'
                            rp.position == RulePosition.POSITION_SLIST_SEPARATOR -> rr.tag + 's'
                            rp.position == RulePosition.POSITION_SLIST_ITEM -> rr.tag + 'i'
                            rp.isAtEnd -> rr.tag + 'e'
                            else -> TODO()
                        }
                        rr.isNonTerminal && rr.rhs.itemsKind == RuntimeRuleRhsItemsKind.CHOICE -> "${rr.tag}${rp.option}"
                        else -> rr.tag
                    }
                    return when (this) {
                        is ClosureItemRoot -> listOf(str)
                        else -> parents.flatMap{ p -> p.shortString.map{ "$it-$str"} }
                    }
                }

            override fun hashCode(): Int = this._id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is ClosureItem -> false
                else -> this._id.contentDeepEquals(other._id)
            }

            abstract override fun toString(): String
        }

        class ClosureItemRoot(
            ffc: FirstFollowCache3,
            override val context: RulePosition,
            override val rulePosition: RulePosition,
            override val parentNextNotAtEndFollow: FollowDeferred,
        ) : ClosureItemAbstract(ffc) {
            override val parents: Set<ClosureItem> = emptySet() // get() = error("ClosureItemRoot has no parent")
            override val parentContext: RulePosition get() = error("ClosureItemRoot has no parent, so no parentContext")
            override val parentRulePosition: RulePosition get() = error("ClosureItemRoot has no parent, so no parentRulePosition")
            override val parentFollowAtEnd get() = error("ClosureItemRoot has no parent, so no parentFollowAtEnd")
            override val parentOfInfo: ParentOfInContext get() = error("ClosureItemRoot has no parent, so no parentOfInfo")

            override val _id = arrayOf(context, rulePosition, nextNotAtEndFollow, parentNextNotAtEndFollow)
            override fun toString(): String = "$rulePosition[$parentNextNotAtEndFollow]-|-$context"
        }

        class ClosureItemChild(
            ffc: FirstFollowCache3,
            initialParent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract(ffc) {

            override val parents: Set<ClosureItem> = mutableSetOf(initialParent)
            override val parentRulePosition: RulePosition = initialParent.rulePosition
            override val parentContext: RulePosition = initialParent.context

            override val context: RulePosition = when {
                parentRulePosition.isAtStart -> parentContext
                else -> parentRulePosition
            }

            override val parentFollowAtEnd: FollowDeferred = initialParent.parentNextNotAtEndFollow
            override val parentNextNotAtEndFollow: FollowDeferred = initialParent.nextNotAtEndFollow
            override val parentOfInfo: ParentOfInContext = this.let {
                val parentContext = this.parentContext
                val parentRulePosition = this.parentRulePosition
                val parentFfc = initialParent.ffc
                val x = parentRulePosition.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FollowDeferredCalculation(parentFfc, parentContext, pn, parentFollowAtEnd)
                    }
                    Pair(pn, pnf)
                }.toSet()
                ParentOfInContext(parentFollowAtEnd, x, parentRulePosition)
            }


            fun addParent(extraParent:ClosureItem) {
                if (Debug.CHECK) {
                    check(parentRulePosition == extraParent.rulePosition)
                    check(parentContext == extraParent.context)
                }
                (parents as MutableSet ).add(extraParent)
            }

            override val _id = arrayOf(context, rulePosition, nextNotAtEndFollow, parentNextNotAtEndFollow)
            override fun toString(): String = parents.joinToString("\n") { "$rulePosition[$parentNextNotAtEndFollow]-->$it" }
        }
*/

    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<ClosureItem, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FirstTerminalInfo>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FollowDeferred>>> { lazyMutableMapNonNull { hashSetOf() } }

    // context -> ( RulePosition -> Boolean indicates if closure does not resolve an Empty terminal )
    private val _needsNext = lazyMutableMapNonNull<RulePosition, MutableMap<RulePosition, Boolean>> { mutableMapOf() }

    // prev/context -> ( RuntimeRule -> set of Terminal-RuntimeRules that could follow given RuntimeRule in given context/prev )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<FollowDeferred>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<firstOfPrev, firstOfRP>> )
    //private val _followInContextAsReferenceToFirstOf =
    //    lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<refPrev, refRuntimeRule>> )
 //   private val _followInContextAsReferenceToFollow =
 //       lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RuntimeRule>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentOfInContext>>> { lazyMutableMapNonNull { hashSetOf() } }

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
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred): Set<FirstTerminalInfo> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        processClosureFor(context, rulePosition, parentFollow)
        //processClosureFor(context, rulePosition, FollowDeferredLiteral(runtimeLookahead))
        return this._firstTerminal[context][rulePosition]
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should be atEnd
    // entry point from calcHeightGraft
    fun parentInContext(contextContext: RulePosition, context: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        processClosureFor(contextContext, context, FollowDeferredLiteral.RT)

        val ctx = if (context.isAtStart) contextContext else context
        return this._parentInContext[ctx][completedRule]
    }

    private fun followInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred): FollowDeferred {
        check(context.isAtEnd.not()) { "firstOf($context,$rulePosition)" }
        processClosureFor(context, rulePosition, parentFollow)
        if (this._firstOfInContext[context].containsKey(rulePosition)) {
            return FollowDeferredComposite(this._firstOfInContext[context][rulePosition])
        } else {
            error("Internal Error: followInContext not calculated for ($context,$rulePosition)")
        }
    }

    internal fun needsNext(context: RulePosition, rulePosition: RulePosition) =
        this._needsNext[context][rulePosition]!!// ?: error("Internal Error: needsNext not computed for ($context,$rulePosition)")

    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    internal fun processClosureFor(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred) {
        //val cls = ClosureItemRoot(this, context, rulePosition, parentFollow)
        val graph = ClosureGraph(this, context, rulePosition, parentFollow)
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

    private fun setNeedsNext(context: RulePosition, rulePosition: RulePosition, needsNext: Boolean) {
        if (this._needsNext[context].containsKey(rulePosition)) {
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($context,$rulePosition) = $needsNext || ${this._needsNext[context][rulePosition]!!}" }
            this._needsNext[context][rulePosition] = this._needsNext[context][rulePosition]!! || needsNext
        } else {
            if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($context,$rulePosition) = $needsNext" }
            this._needsNext[context][rulePosition] = needsNext
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
            this.addFollowInContext(prev, rulePosition, firstTerminalInfo.followNext)
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

    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentOf: ParentOfInContext) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add parentOf($prev,${completedRule.tag}) = $parentOf" }
        this._parentInContext[prev][completedRule].add(parentOf)
    }

    private fun calcFirstTermClosure(graph: ClosureGraph) {

        //TODO: when we get duplicate closureItem, connect it don't discard it
        // creating a upside down tree.
        // then process closure needs to work from bottom up doing all branches.


        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: ${graph.root}" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        val done = mutableSetOf<ClosureItem>()
        val reachedTerminal = mutableSetOf<ClosureItem>()
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(graph.root)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            val item = cls.rulePosition.item ?: error("Internal Error: should never be null")
            when {
                item.isTerminal -> reachedTerminal.add(cls.addChild(item.asTerminalRulePosition)!!)
                item.isEmbedded -> reachedTerminal.add(cls.addChild(item.asTerminalRulePosition)!!)
                else -> {
                    val childRulePositions = item.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = cls.addChild(childRp)
                        if (done.contains(childCls)) {
                            // don't follow down the closure
                        } else {
                            todoList.enqueue(childCls)
                            //println("todo: ${childCls.shortString}")
                        }
                        done.add(childCls)
                    }
                }
            }
        }
        for (bottom in reachedTerminal) {
            this.processClosurePaths(graph, bottom)
        }
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: ${graph.root}" }
    }

    //internal so can be tested
    internal fun calcAllClosures(closureStart: ClosureItem): Set<ClosureItem> {
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

    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required
     */
    private fun processClosurePaths(graph: ClosureGraph, bottom: ClosureItem) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosurePath: ${bottom}" }
        val firstTerminalInfo = when {
            bottom.rulePosition.isTerminal -> FirstTerminalInfo(bottom.rulePosition.runtimeRule, bottom.rulePosition.runtimeRule, bottom.parentNextNotAtEndFollow)
            bottom.rulePosition.isEmbedded -> {
                val item = bottom.rulePosition.runtimeRule
                val embeddedRuleSet = item.embeddedRuntimeRuleSet ?: error("Internal Error: should never be null")
                val embeddedRule = item.embeddedStartRule ?: error("Internal Error: should never be null")
                val embeddedStateSet = embeddedRuleSet.fetchStateSetFor(embeddedRule, this.stateSet.automatonKind)
                val embeddedFfc = FirstFollowCache3(embeddedStateSet)
                val s = embeddedFfc.firstTerminalInContext(bottom.context, embeddedStateSet.startRulePosition, bottom.parentNextNotAtEndFollow)
                val tr = s.first().terminalRule //FIXME: could be more than 1
                val fn = FollowDeferredComposite(s.map { it.followNext }.toSet())
                FirstTerminalInfo(bottom.rulePosition.runtimeRule, tr, fn)
            }

            else -> null
        }
        if (Debug.CHECK) firstTerminalInfo?.let { check(firstTerminalInfo.terminalRule.isTerminal) }

        graph.traverseUpPaths(bottom) { cls, childNeedsNext ->
            this.setFirsts(cls, firstTerminalInfo, childNeedsNext)
        }

        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosurePath: $bottom" }
    }

    /**
     * return if 'needsNext', ie. true IF {
     *    isTerminal && isEmpty OR
     *    next.isAtEnd && child.needsNext
     * }
     */
    private fun setFirsts(cls: ClosureItem, firstTerminalInfo: FirstTerminalInfo?, childNeedsNext: Boolean): Boolean {
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
        val prev = cls.context
        val rp = cls.rulePosition
        val rr = cls.rulePosition.runtimeRule

        // set parentOf
        when (cls) {
            is ClosureItemRoot -> Unit
            is ClosureItemChild -> {
                val parentFollowAtEnd = cls.parentFollowAtEnd
                val parent = cls.parentRulePosition
                val parentContext = cls.parentContext
                val parentFfc = cls.ffc
                val x = parent.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FollowDeferredCalculation(parentFfc, parentContext, pn, parentFollowAtEnd)
                    }
                    Pair(pn, pnf)
                }.toSet()
                this.addParentInContext(prev, rr, ParentOfInContext(parentFollowAtEnd, x, parent))
            }

            else -> error("Internal Error: subtype of ClosureItem not handled")
        }
        // set follow for each runtime-rule - HEIGHT/GRAFT needs it
        this.addFollowInContext(prev, rr, cls.parentNextNotAtEndFollow)

        when {
            cls.rulePosition.isTerminal -> Unit
            cls.rulePosition.isEmbedded -> {
                firstTerminalInfo?.let { this.addFirstTerminalAndFollowInContext(prev, rp, firstTerminalInfo) }
                if (childNeedsNext) {
                    this.addFollowInContext(prev, rp, cls.nextNotAtEndFollow)
                    cls.nextNotAtEndFollow.containsEmptyRules
                } else {
                    // do nothing
                }
            }

            cls is ClosureItemRoot -> {
                // for targetState
                firstTerminalInfo?.let { this.addFirstTerminalAndFollowInContext(prev, rp, firstTerminalInfo) }
                if (childNeedsNext) {
                    this.addFollowInContext(prev, rp, cls.nextNotAtEndFollow)
                    cls.nextNotAtEndFollow.containsEmptyRules
                } else {
                    // do nothing
                }
            }

            cls.rulePosition.isAtStart -> {
                if (childNeedsNext) {
                    // ensures stuff is cached for the follow
                    cls.nextNotAtEndFollow.process()
                } else {
                    //do nothing
                }
            }

            else -> {
                error("should not happen")
            }
        }
        val needsNext = cls.needsNext(childNeedsNext)
        when {
            rr.isTerminal -> Unit
            rp.isAtStart -> Unit
            else -> {
                this.setNeedsNext(cls.context, cls.rulePosition, needsNext)
                when (needsNext) {
                    true -> this.addFollowInContext(prev, rp, cls.nextNotAtEndFollow)
                    false -> Unit
                }
            }
        }

        return needsNext
    }

}