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
            val parentInfo: Set<Pair<RulePosition,RulePosition>>
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

            override val parentInfo: Set<Pair<RulePosition,RulePosition>> = emptySet()
            override val containsEmptyRules: Boolean by lazy{ _terminals.any { it.isEmptyRule } }
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

            override val parentInfo: Set<Pair<RulePosition,RulePosition>> = parts.flatMap {
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

            override val parentInfo: Set<Pair<RulePosition,RulePosition>> = when (parentFollow) {
                is FollowDeferredLiteral -> emptySet()
                is FollowDeferredComposite -> parentFollow.parts.flatMap { it.parentInfo }.toSet()
                is FollowDeferredCalculation -> {
                    parentFollow.parentInfo + Pair(parentFollow.context,parentFollow.rulePosition)
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
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
            override val parentFollowAtEnd get() = error("ClosureItemRoot has no parent so no parentFollowAtEnd")
            override val parentOfInfo: ParentOfInContext get() = error("ClosureItemRoot has no parent so no parentOfInfo")

            override val _id = arrayOf(context, rulePosition, parentNextNotAtEndFollow)

            override fun toString(): String = "$rulePosition[$parentNextNotAtEndFollow]-|-$context"
        }

        class ClosureItemChild(
            ffc: FirstFollowCache3,
            override val parent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract(ffc) {

            override val context: RulePosition = when {
                parent.rulePosition.isAtStart -> parent.context
                else -> parent.rulePosition
            }

            override val parentFollowAtEnd: FollowDeferred get() = parent.parentNextNotAtEndFollow
            override val parentNextNotAtEndFollow: FollowDeferred get() = parent.nextNotAtEndFollow
            override val parentOfInfo: ParentOfInContext = this.let {
                val parentContext = this.parent.context
                val parentRulePosition = this.parent.rulePosition
                val parentFfc = this.parent.ffc
                val x = parentRulePosition.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FollowDeferredCalculation(parentFfc, parentContext, pn, parentFollowAtEnd)
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
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, parentFollow: FollowDeferred): Set<FirstTerminalInfo> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        processClosureFor(context, rulePosition, parentFollow)
        //processClosureFor(context, rulePosition, FollowDeferredLiteral(runtimeLookahead))
        return this._firstTerminal[context][rulePosition]
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should BE atEnd
    // entry point from calcHeightGraft
    fun parentInContext(contextContext: RulePosition, context: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        //processClosureFor(RulePosition_UNKNOWN, context, FollowDeferredLiteral.UP)
        processClosureFor(contextContext, context, FollowDeferredLiteral.RT)
        //processClosureFor(contextContext, context, FollowDeferredLiteral(runtimeLookahead))

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
        val cls = ClosureItemRoot(this, context, rulePosition, parentFollow)
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

    private fun calcFirstTermClosure(closureStart: ClosureItem) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: $closureStart" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        val done = mutableSetOf<ClosureItem>()
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(closureStart)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            when {
                cls.rulePosition.isAtEnd -> this.processClosure(cls)
                else -> {
                    val item = cls.rulePosition.item ?: error("Internal Error: should never be null")
                    when {
                        item.isTerminal -> {
                            val childRp = item.asTerminalRulePosition
                            todoList.enqueue(ClosureItemChild(cls.ffc, cls, childRp))
                        }

                        item.isEmbedded -> {
                            val childRp = item.asTerminalRulePosition
                            todoList.enqueue(ClosureItemChild(cls.ffc, cls, childRp))
                            //val embeddedRuleSet = item.embeddedRuntimeRuleSet ?: error("Internal Error: should never be null")
                            //val embeddedRule = item.embeddedStartRule ?: error("Internal Error: should never be null")
                            //val embeddedStateSet = embeddedRuleSet.fetchStateSetFor(embeddedRule, this.stateSet.automatonKind)
                            //val embeddedFfc = FirstFollowCache3(embeddedStateSet)
                            //val embeddedClosureItem = ClosureItemChild(embeddedFfc, cls, embeddedStateSet.startRulePosition)
                            //todoList.enqueue(embeddedClosureItem)
                        }

                        else -> {
                            val childRulePositions = item.rulePositionsAt[0]
                            for (childRp in childRulePositions) {
                                val childCls = ClosureItemChild(cls.ffc, cls, childRp)
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
            }
        }
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: $closureStart" }
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
                    todoList.push(ClosureItemChild(cls.ffc, cls, childRp))
                }

                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls.ffc, cls, childRp)
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
    private fun processClosure(closureItem: ClosureItem) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosure: ${closureItem.shortString}" }
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "START processClosure: $closureItem" }
        var cls = closureItem
        val firstTerminalInfo = when {
            cls.rulePosition.isTerminal -> FirstTerminalInfo(cls.rulePosition.runtimeRule,cls.rulePosition.runtimeRule, cls.parentNextNotAtEndFollow)
            cls.rulePosition.isEmbedded -> {
                val item = cls.rulePosition.runtimeRule
                val embeddedRuleSet = item.embeddedRuntimeRuleSet ?: error("Internal Error: should never be null")
                val embeddedRule = item.embeddedStartRule ?: error("Internal Error: should never be null")
                val embeddedStateSet = embeddedRuleSet.fetchStateSetFor(embeddedRule, this.stateSet.automatonKind)
                val embeddedFfc = FirstFollowCache3(embeddedStateSet)
                val s = embeddedFfc.firstTerminalInContext(cls.context, embeddedStateSet.startRulePosition, cls.parentNextNotAtEndFollow)
                val tr = s.first().terminalRule //FIXME: could be more than 1
                val fn = FollowDeferredComposite(s.map { it.followNext }.toSet())
                FirstTerminalInfo(cls.rulePosition.runtimeRule, tr, fn)
            }
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
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosure: $closureItem" }
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