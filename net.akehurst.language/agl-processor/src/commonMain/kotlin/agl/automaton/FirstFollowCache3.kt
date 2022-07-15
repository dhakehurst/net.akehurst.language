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

        val RulePosition_UNKNOWN = RulePosition(RuntimeRuleSet.UNKNOWN_RULE, -1, 1) // must not be 'atEnd' or atStart

        interface FollowDeferred {
            fun containsEmptyRules(ff: FirstFollowCache3): Boolean
            fun resolveTerminals(ff: FirstFollowCache3): Set<RuntimeRule>
            val parentInfo: Set<RulePosition>
        }

        data class FollowDeferredLiteral(
            private val _terminals: Set<RuntimeRule>
        ) : FollowDeferred {
            companion object {
                val UP = FollowDeferredLiteral(setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
                val EMPTY = FollowDeferredLiteral(emptySet())
            }

            override val parentInfo: Set<RulePosition> = emptySet()

            override fun containsEmptyRules(ff: FirstFollowCache3): Boolean = _terminals.any { it.isEmptyRule }

            override fun resolveTerminals(ff: FirstFollowCache3): Set<RuntimeRule> = _terminals
            override fun toString(): String = "[${_terminals.joinToString { it.tag }}]"
        }

        data class FollowDeferredComposite(
            val parts: Set<FollowDeferred>
        ) : FollowDeferred {

            override val parentInfo: Set<RulePosition> = parts.flatMap {
                when (it) {
                    is FollowDeferredLiteral -> emptySet()
                    is FollowDeferredComposite -> it.parts.flatMap { it.parentInfo }.toSet()
                    is FollowDeferredCalculation -> setOf(it.rulePosition)
                    else -> TODO()
                }
            }.toSet()

            override fun containsEmptyRules(ff: FirstFollowCache3): Boolean = parts.map { it.containsEmptyRules(ff) }.reduce { acc, b -> acc || b }
            override fun resolveTerminals(ff: FirstFollowCache3): Set<RuntimeRule> = parts.flatMap { it.resolveTerminals(ff) }.toSet()
            override fun toString(): String = parts.joinToString(separator = "+") { "$it" }
        }

        data class FollowDeferredCalculation(
            val context: RulePosition,
            val rulePosition: RulePosition,
            val parentFollow: FollowDeferred
        ) : FollowDeferred {

            private var _resolvedFollow: FollowDeferred? = null
            private var _resolvedContainsEmpty: Boolean? = null

            override val parentInfo: Set<RulePosition> = when (parentFollow) {
                is FollowDeferredLiteral -> emptySet()
                is FollowDeferredComposite -> parentFollow.parts.flatMap { it.parentInfo }.toSet()
                is FollowDeferredCalculation -> setOf(parentFollow.rulePosition)
                else -> TODO()
            }

            override fun containsEmptyRules(ff: FirstFollowCache3): Boolean {
                if (null == this._resolvedFollow) {
                    this._resolvedFollow = ff.followInContext(context, rulePosition, parentFollow)
                    this._resolvedContainsEmpty = ff.needsNext(context, rulePosition)
                }
                return _resolvedContainsEmpty!!
            }

            override fun resolveTerminals(ff: FirstFollowCache3): Set<RuntimeRule> {
                if (null == this._resolvedFollow) {
                    this._resolvedFollow = ff.followInContext(context, rulePosition, parentFollow)
                    this._resolvedContainsEmpty = ff.needsNext(context, rulePosition)
                }
                return _resolvedFollow!!.resolveTerminals(ff)
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
            val context: RulePosition
            val rulePosition: RulePosition

            val nextNotAtEndFollow: FollowDeferred

            val parent: ClosureItem

            /**
             * parent.next.follow
             */
            val parentNextNotAtEndFollow: FollowDeferred

            /**
             * parent.parentNextFollow
             */
            val parentFollowAtEnd: FollowDeferred

            val parentOfInfo: ParentOfInContext

            val _id: Array<*>

            val shortString: String
                get() = when (this) {
                    is ClosureItemRoot -> this.rulePosition.runtimeRule.tag
                    else -> "${parent.shortString}-${this.rulePosition.runtimeRule.tag}"
                }

            fun needsNext(childNeedsNext: Boolean, ff: FirstFollowCache3): Boolean
        }


        abstract class ClosureItemAbstract() : ClosureItem {

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
            override fun needsNext(childNeedsNext: Boolean, ff: FirstFollowCache3): Boolean = when {
                rulePosition.isEmptyRule -> true
                childNeedsNext -> when {
                    rulePosition.isAtEnd -> true
                    else -> {
                        val nexts = rulePosition.next()
                        nexts.any { nxt ->
                            when {
                                nxt.isAtEnd -> true
                                else -> ff.needsNext(context, nxt)
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
                                else -> FollowDeferredCalculation(context, next, parentNextNotAtEndFollow)
                            }
                        }
                        FollowDeferredComposite(allNextFollow.toSet())
                    }
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
            override val context: RulePosition,
            override val rulePosition: RulePosition,
            override val parentNextNotAtEndFollow: FollowDeferred,
        ) : ClosureItemAbstract() {
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
            override val parentFollowAtEnd get() = error("ClosureItemRoot has no parent so no parentFollowAtEnd")
            override val parentOfInfo: ParentOfInContext get() = error("ClosureItemRoot has no parent so no parentOfInfo")

            override val _id = arrayOf(context, rulePosition, parentNextNotAtEndFollow)

            override fun toString(): String = "$rulePosition[$parentNextNotAtEndFollow]-|-$context"
        }

        class ClosureItemChild(
            override val parent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract() {

            override val context: RulePosition = when {
                parent.rulePosition.isAtStart -> parent.context
                else -> parent.rulePosition
            }

            override val parentFollowAtEnd: FollowDeferred get() = parent.parentNextNotAtEndFollow
            override val parentNextNotAtEndFollow: FollowDeferred get() = parent.nextNotAtEndFollow
            override val parentOfInfo: ParentOfInContext = this.let {
                val parentContext = this.parent.context
                val parentRulePosition = this.parent.rulePosition
                val x = parentRulePosition.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FollowDeferredCalculation(parentContext, pn, parentFollowAtEnd)
                    }
                    Pair(pn, pnf)
                }.toSet()
                ParentOfInContext(parentFollowAtEnd, x, parentRulePosition)
            }
            override val _id = arrayOf(context, rulePosition, parentNextNotAtEndFollow, parentOfInfo)

            override fun toString(): String = "$rulePosition[$parentNextNotAtEndFollow]-->$parent"
        }
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
    private val _followInContextAsReferenceToFirstOf =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<refPrev, refRuntimeRule>> )
    private val _followInContextAsReferenceToFollow =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RuntimeRule>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentOfInContext>>> { lazyMutableMapNonNull { hashSetOf() } }

    init {
        // firstTerm(context=RP(G,0,SOR), rulePosition=RP(G,0,EOR) ) = UP
        //this.addFirstTerminalAndFirstOfInContext(this.stateSet.startRulePosition, this.stateSet.finishRulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
    }

    fun clear() {
        this._doneFollow.clear()
        this._firstTerminal.clear()
        this._followInContext.clear()
        this._needsNext.clear()
        this._firstOfInContext.clear()
        this._followInContextAsReferenceToFirstOf.clear()
        this._followInContextAsReferenceToFollow.clear()
        this._parentInContext.clear()
    }

    // entry point from calcWidth
    // target states for WIDTH transition, rulePosition should NOT be atEnd
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition): Set<FirstTerminalInfo> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        processClosureFor(context, rulePosition, FollowDeferredLiteral.UP)
        return this._firstTerminal[context][rulePosition]
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should BE atEnd
    // entry point from calcHeightGraft
    fun parentInContext(contextContext: RulePosition, context: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        //processClosureFor(RulePosition_UNKNOWN, context, FollowDeferredLiteral.UP)
        processClosureFor(contextContext, context, FollowDeferredLiteral.UP)

        when {
            context.item!!.isTerminal -> {
                val childRp = context.item!!.asTerminalRulePosition
                //FIXME: this special-case is about graft - we should really use prev-prev as the context, not UNKNOWN
                //val cls = ClosureItemRoot(RulePosition_UNKNOWN, context, FollowDeferredLiteral.UP)
                //val childCls = ClosureItemChild(cls, childRp)
                // if this parentOf entry is used, it must be for GRAFT and parentFollowAtEnd is irrelevant
                //val parentFollowAtEnd = childCls.parent.parentFollow
                //val parentFollow = childCls.parentFollow
                //val parent = childCls.parent.rulePosition
                //this.addParentInContext(context, context.item!!, ParentOfInContext(parentFollowAtEnd, parentFollow, parent))
                //processClosureFor(RulePosition_UNKNOWN, context, FollowDeferredLiteral.UP)
            }
            else -> {
                //for(childRp in context.item!!.rulePositionsAt[0]) {
                //   processClosureFor(context, childRp, FollowDeferredLiteral.UP)
                //}
            }
        }
        //val ctx = if (context.isAtStart) RulePosition_UNKNOWN else context
        val ctx = if (context.isAtStart) contextContext else context
        return this._parentInContext[ctx][completedRule]
    }

    fun followInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred): FollowDeferred {
        check(context.isAtEnd.not()) { "firstOf($context,$rulePosition)" }
        processClosureFor(context, rulePosition, parentFollow)
        if (this._firstOfInContext[context].containsKey(rulePosition)) {
            return FollowDeferredComposite(this._firstOfInContext[context][rulePosition])
        } else {
            error("Internal Error: followInContext not calculated for ($context,$rulePosition)")
        }
    }

    private fun needsNext(context: RulePosition, rulePosition: RulePosition) =
        this._needsNext[context][rulePosition]!!// ?: error("Internal Error: needsNext not computed for ($context,$rulePosition)")

    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    internal fun processClosureFor(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred) {
        val cls = ClosureItemRoot(context, rulePosition, parentFollow)
        val doit = when (this._doneFollow[context][cls]) {
            null -> true
            true -> false
            else -> false
        }
        if (doit) {
            this._doneFollow[context][cls] = true
            this.calcFirstTermClosure(cls)
        }
    }

    private fun setNeedsNext(context: RulePosition, rulePosition: RulePosition, needsNext: Boolean) {
        if (this._needsNext[context].containsKey(rulePosition)) {
            if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($context,$rulePosition) = $needsNext || ${this._needsNext[context][rulePosition]!!}" }
            this._needsNext[context][rulePosition] = this._needsNext[context][rulePosition]!! || needsNext
        } else {
            if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add needsNext($context,$rulePosition) = $needsNext" }
            this._needsNext[context][rulePosition] = needsNext
        }
    }


    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalAndFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, firstTerminalInfo: FirstTerminalInfo) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add firstTerm($prev,$rulePosition) = $firstTerminalInfo" }
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
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($prev,$rulePosition) = $follow" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(follow)
    }

    private fun addFollowInContext(prev: RulePosition, runtimeRule: RuntimeRule, follow: FollowDeferred) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($prev,${runtimeRule.tag}) = $follow" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followInContext[prev][runtimeRule].add(follow)
    }

    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentOf: ParentOfInContext) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add parentOf($prev,${completedRule.tag}) = $parentOf" }
        this._parentInContext[prev][completedRule].add(parentOf)
    }

    private fun calcFirstTermClosure(closureStart: ClosureItem) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: $closureStart" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        val done = mutableSetOf<ClosureItem>()
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(closureStart)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            when {
                /*cls.rulePosition.isAtEnd -> when {
                    cls.rulePosition.isGoal -> {
                        this.addFirstTerminalAndFirstOfInContext(
                            cls.context,
                            cls.rulePosition,
                            FirstTerminalInfo(RuntimeRuleSet.USE_PARENT_LOOKAHEAD, FollowDeferredLiteral(emptySet()))
                        )
                    }
                    cls.rulePosition.isTerminal -> {
                        this.processClosure(cls)
                    }
                    else -> error("Internal Error: should never happen")
                }*/
                cls.rulePosition.isAtEnd -> {
                    this.processClosure(cls)
                }
                cls.rulePosition.item!!.isTerminal -> {
                    val childRp = cls.rulePosition.item!!.asTerminalRulePosition
                    todoList.enqueue(ClosureItemChild(cls, childRp))
                }
                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls, childRp)
                        //val d = Triple(childCls.prev, childCls.rulePosition, childCls.parent.nextNotAtEnd)
                        if (done.contains(childCls).not()) {
                            done.add(childCls)
                            todoList.enqueue(childCls)
                        } else {
                            val i = childCls.shortString
                            // already done
                        }
                    }
                }
            }
        }
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: $closureStart" }
    }

    //internal so can be tested
    internal fun calcAllClosures(closureStart: ClosureItem): Set<ClosureItem> {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcClosures: $closureStart" }
        val completeClosures = mutableSetOf<ClosureItem>()
        val todoList = mutableStackOf<ClosureItem>()
        todoList.push(closureStart)
        while (todoList.isNotEmpty) {
            val cls = todoList.pop()
            when {
                cls.rulePosition.isTerminal -> completeClosures.add(cls)
                cls.rulePosition.item!!.isTerminal -> {
                    val childRp = cls.rulePosition.item!!.asTerminalRulePosition
                    todoList.push(ClosureItemChild(cls, childRp))
                }
                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls, childRp)
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
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcClosures: $closureStart" }
        return completeClosures
    }

    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required
     */
    private fun processClosure(closureItem: ClosureItem) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosure: ${closureItem.shortString}" }
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "START processClosure: $closureItem" }
        var cls = closureItem
        val firstTerminalInfo = when {
            cls.rulePosition.isTerminal -> FirstTerminalInfo(cls.rulePosition.runtimeRule, cls.parentNextNotAtEndFollow)
            else -> null
        }
        if (Debug.CHECK) firstTerminalInfo?.let { check(firstTerminalInfo.terminalRule.isTerminal) }
        var childNeedsNext = this.setFirsts(cls, firstTerminalInfo, false)
        var goUp = true //always go up once (from the terminal)
        while (goUp && cls !is ClosureItemRoot) {
            cls = cls.parent
            childNeedsNext = this.setFirsts(cls, firstTerminalInfo, childNeedsNext)
            goUp = cls.rulePosition.isAtStart
        }
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosure: $closureItem" }
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
                val parent = cls.parent.rulePosition
                val parentContext = cls.parent.context
                val x = parent.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FollowDeferredCalculation(parentContext, pn, parentFollowAtEnd)
                    }
                    Pair(pn, pnf)
                }.toSet()
                this.addParentInContext(prev, rr, ParentOfInContext(parentFollowAtEnd, x, parent))
            }
            else -> error("Internal Error: subtype of ClosureItem not handled")
        }
        // set follow for each runtime-rule - HEIGHT/GRAFT needs it
        this.addFollowInContext(prev, rr, cls.parentNextNotAtEndFollow)

        val tnn = when {
            cls.rulePosition.isTerminal -> rr.isEmptyRule
            cls is ClosureItemRoot -> {
                // for targetState
                firstTerminalInfo?.let { this.addFirstTerminalAndFirstOfInContext(prev, rp, firstTerminalInfo) }
                if (childNeedsNext) {
                    this.addFollowInContext(prev, rp, cls.nextNotAtEndFollow)
                    cls.nextNotAtEndFollow.containsEmptyRules(this)
                } else {
                    false
                }
            }
            cls.rulePosition.isAtStart -> {
                if (childNeedsNext) {
                    cls.nextNotAtEndFollow.containsEmptyRules(this)
                } else {
                    false
                }
            }
            else -> {
                error("should not happen")
            }
        }
        val nextAtEnd = rp.next().any { it.isAtEnd }
        //val needsNext = (childNeedsNext && nextAtEnd) || rr.isEmptyRule
        val needsNext = cls.needsNext(childNeedsNext, this)
//        val thisNeedsNext = childNeedsNext || cls.nextNotAtEndFollow.containsEmptyRules(this)
        when {
            rr.isTerminal -> Unit
            rp.isAtStart -> Unit
            else -> {
                this.setNeedsNext(cls.context, cls.rulePosition, needsNext)
                when (needsNext) {
                    true -> {
                        this.addFollowInContext(prev, rp, cls.nextNotAtEndFollow)
                    }
                    false -> Unit
                }
            }
        }

        return needsNext
    }

}