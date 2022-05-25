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

import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.graph.StateInfoUncompressed
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.debug
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf
import net.akehurst.language.collections.mutableStackOf

internal typealias ComputeRulesFunc = () -> Set<RuntimeRule>

internal class FirstFollowCache2(val stateSet: ParserStateSet) {

    class NextNotAtEnd(
        val previousNextNotAtEnd: NextNotAtEnd?,
        val prev: RulePosition,
        val rulePosition: RulePosition
    ) {
        private val _hashCode_cache = arrayOf(previousNextNotAtEnd?.prev, previousNextNotAtEnd?.rulePosition, prev, rulePosition).contentHashCode()
        override fun hashCode(): Int = this._hashCode_cache
        override fun equals(other: Any?): Boolean = when {
            other !is NextNotAtEnd -> false
            this.prev != other.prev -> false
            this.rulePosition != other.rulePosition -> false
            this.previousNextNotAtEnd?.prev != other.previousNextNotAtEnd?.prev -> false
            this.previousNextNotAtEnd?.rulePosition != other.previousNextNotAtEnd?.rulePosition -> false
            else -> true
        }

        override fun toString(): String = "NextNotAtEnd{prev=$prev, rulePosition=$rulePosition}"
    }

    internal companion object {
        enum class ReferenceFunc { FIRST_TERM, FOLLOW }

        interface FollowDeferred {
            fun terminals(ff: FirstFollowCache2): Set<RuntimeRule>
        }

        data class FollowDeferredLiteral(
            private val _terminals: Set<RuntimeRule>
        ) : FollowDeferred {
            override fun terminals(ff: FirstFollowCache2): Set<RuntimeRule> = _terminals
            override fun toString(): String = "[${_terminals.joinToString { it.tag }}]"
        }

        data class FollowDeferredCalculation(
            val context: StateInfoDeferred,
            val next: RulePosition,
            val followAtEnd: FollowDeferred
        ) : FollowDeferred {
            override fun terminals(ff: FirstFollowCache2): Set<RuntimeRule> = ff.followInContext(context, next, followAtEnd)

            private val _id = arrayOf(context.rulePosition, next)
            override fun hashCode(): Int = _id.contentHashCode()
            override fun equals(other: Any?): Boolean =when {
                other !is FollowDeferredCalculation -> false
                else -> this._id.contentEquals(other._id)
            }
            override fun toString(): String = "follow(${context.rulePosition}, $next)"//-$followAtEnd"
        }

        data class FollowDeferredComposite(
            val parts: Set<FollowDeferred>
        ) : FollowDeferred {
            override fun terminals(ff: FirstFollowCache2): Set<RuntimeRule> = parts.flatMap { it.terminals(ff) }.toSet()
            override fun toString(): String = parts.joinToString(separator = "&") { "$it" }
        }

        data class StateInfoDeferred(
            val rulePosition: RulePosition,
            val follow:FollowDeferred
        )

        val StateInfoUncompressed.toDeferred get() = StateInfoDeferred(this.rulePosition, FollowDeferredLiteral(this.follow))

        data class ParentOfInContext(
            val parentFollowAtEnd: FollowDeferred, //parent.parentFollow
            val parent: StateInfoDeferred
        )

        /**
         * Identified by :
         *  - parent.rulePosition, used for HEIGHT/GRAFT decision
         *  - context, used for prev guard for transition
         *  - rulePosition, used for state id
         *  - follow, used for lookahead guard
         *  - parentFollow, used for lookahead up
         */
        interface ClosureItem {
            val isRoot: Boolean
            val asChild : ClosureItemChild

            val rulePosition: RulePosition
            val parentFollow: FollowDeferred
            val follow: FollowDeferred

            val context:StateInfoDeferred

            val id : Array<Any>
        }

        abstract class ClosureItemAbstract(): ClosureItem {

            override val follow: FollowDeferred by lazy {
                when {
                    rulePosition.isAtEnd -> parentFollow
                    //rulePosition.isGoal -> LookaheadSetPart.UP // follow of (G,0,0) is UP
                    else -> {
                        val parts = rulePosition.next().map { nx ->
                            when {
                                nx.isAtEnd -> parentFollow
                                //nx.isGoal -> LookaheadSetPart.UP
                                else -> FollowDeferredCalculation(context, nx, parentFollow)
                            }
                        }.toSet()
                        FollowDeferredComposite(parts)
                    }
                }
            }

            override fun hashCode(): Int = id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is ClosureItem -> false
                else -> this.id.contentDeepEquals(other.id)
            }
        }

        class ClosureItemRoot(
            override val context: StateInfoDeferred,
            override val rulePosition: RulePosition,
            override val parentFollow: FollowDeferred
        ) : ClosureItemAbstract() {
            override val isRoot: Boolean = true
            override val asChild: ClosureItemChild get() = error("Internal Error: Cannot cast ClosureItemRoot to ClosureItemChild")

            override val id = arrayOf(context, rulePosition, follow, parentFollow)
            override fun toString(): String = "$rulePosition[$follow]-|->${context.rulePosition}[${context.follow}]"
        }

        class ClosureItemChild(
            val parent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract() {
            override val isRoot: Boolean = false
            override val asChild: ClosureItemChild get() = this

            override val context: StateInfoDeferred get() = when {
                parent.rulePosition.isAtStart -> parent.context
                else -> StateInfoDeferred(parent.rulePosition, parent.follow)
            }
            override val parentFollow = this.parent.follow

            override val id = arrayOf(parent.rulePosition, context, rulePosition, follow, parentFollow)
            override fun toString(): String = "$rulePosition[$follow]-->$parent"
        }

    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<ClosureItem, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> Boolean indicates if closure does not resolve an Empty terminal )
    private val _needsNext = lazyMutableMapNonNull<RulePosition, MutableMap<ClosureItem, Boolean>> { mutableMapOf() }

    // firstOfPrev -> ( firstOfRulePosition -> Set<Pair<firstTermPrev, firstTermRP>> )
    private val _firstOfInContextAsReferenceToFunc =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<Triple<ReferenceFunc, RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RuntimeRule -> set of Terminal-RuntimeRules that could follow given RuntimeRule in given context/prev )
    private val _followAtEndInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followAtEndPrev -> ( TerminalRule -> Set<Function> )
    private val _followAtEndInContextAsFunc =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<FollowDeferred>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followAtEndPrev -> ( TerminalRule -> Set<Pair<refPrev, refRuntimeRule>> )
    private val _followAtEndInContextAsReferenceToFollowAtEnd =
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
        this._followAtEndInContext.clear()
        this._needsNext.clear()
        this._firstOfInContextAsReferenceToFunc.clear()
        this._followInContext.clear()
        this._followAtEndInContextAsFunc.clear()
        this._followAtEndInContextAsReferenceToFollowAtEnd.clear()
        this._parentInContext.clear()
    }

    // target states for WIDTH transition, rulePosition should NOT be atEnd
    fun firstTerminalInContext(context: StateInfoUncompressed, source: StateInfoUncompressed): Set<RuntimeRule> {
        if (Debug.CHECK) check(context.rulePosition.isAtEnd.not()) { "firstTerminal($context,$source)" }
        processClosureFor(context.toDeferred, source.toDeferred, true)
        return this._firstTerminal[context.rulePosition][source.rulePosition]
    }

    // target states for HEIGHT or GRAFT transition, rulePosition should BE atEnd
    // up for HEIGHT + followAtEnd
    fun parentInContext(context: StateInfoUncompressed, completedRule: RuntimeRule): Set<ParentOfInContext> =
        parentInContext(StateInfoDeferred(context.rulePosition, FollowDeferredLiteral(context.follow)), completedRule)

    fun parentInContext(context: StateInfoDeferred, completedRule: RuntimeRule): Set<ParentOfInContext> {
        if (Debug.CHECK) check(context.rulePosition.isAtEnd.not()) { "parentInContext($context,$completedRule)" }
        //processClosureFor(context, true) //could stop at completedRule
        return this._parentInContext[context.rulePosition][completedRule]
    }

    // guard for HEIGHT or GRAFT transition (target maybe atEnd)
    // also used to resolve terminals in FollowDeferredCalculation
    //fun followFromContext(context: StateInfoDeferred, rulePosition: RulePosition): Set<RuntimeRule> {
    //    processClosureFor(context, context, followAtEndOfContext, false)
   //     return this._followInContext[context][rulePosition]
   // }

    fun followInContext(context: StateInfoDeferred, rulePosition: RulePosition, follow: FollowDeferred): Set<RuntimeRule> {
        processClosureFor(context, StateInfoDeferred(rulePosition, follow), false)
        return this._followInContext[context.rulePosition][rulePosition]
    }

    // guard for WIDTH transition (target is always atEnd)
    // up for HEIGHT + parentOf
    // should already have done the closure processing
    fun followAtEndInContext(context: StateInfoUncompressed, runtimeRule: RuntimeRule) =
        this.followAtEndInContext(StateInfoDeferred(context.rulePosition, FollowDeferredLiteral(context.follow)), runtimeRule)
    fun followAtEndInContext(context: StateInfoDeferred, runtimeRule: RuntimeRule): Set<RuntimeRule> {
        if (Debug.CHECK) check(context.rulePosition.isAtEnd.not()) { "follow($context,$runtimeRule)" }
        if (_followAtEndInContextAsFunc.containsKey(context.rulePosition)) {
            if (_followAtEndInContextAsFunc[context.rulePosition].containsKey(runtimeRule)) {
                val set = _followAtEndInContextAsFunc[context.rulePosition][runtimeRule]
                for (fae in set) {
                    val rrs = fae.terminals(this)
                    this.addAllFollowAtEndInContext(context, runtimeRule, rrs)
                }
            }
        }
        return this._followAtEndInContext[context.rulePosition][runtimeRule]
    }


    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    internal fun processClosureFor(context: StateInfoDeferred, source: StateInfoDeferred, calcFollow: Boolean): Boolean {
        val cls = ClosureItemRoot(context, source.rulePosition, source.follow)
        val doit = when (this._doneFollow[context.rulePosition][cls]) {
            null -> true
            false -> calcFollow == true
            true -> false
        }
        if (doit) {
            this._doneFollow[context.rulePosition][cls] = calcFollow
            val r = this.calcFirstTermClosure(cls, calcFollow)
            this._needsNext[context.rulePosition][cls] = r
        }
        return false
    }

    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalAndFirstOfInContext(prev: StateInfoDeferred, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add firstTerm(${prev.rulePosition}[${prev.follow}],$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.rulePosition.isAtEnd.not())
        this._firstTerminal[prev.rulePosition][rulePosition].add(terminal)
        if (terminal.isEmptyRule) {
            //do not add to firstOf
        } else {
            this.addFollowInContext(prev, rulePosition, terminal)
        }
    }

    private fun addFollowInContext(prev: StateInfoDeferred, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add follow(${prev.rulePosition}[${prev.follow}],$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.rulePosition.isAtEnd.not())
        this._followInContext[prev.rulePosition][rulePosition].add(terminal)
    }

    private fun addAllFollowInContext(prev: StateInfoDeferred, rulePosition: RulePosition, terminals: Set<RuntimeRule>) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add follow(${prev.rulePosition}[${prev.follow}],$rulePosition) = ${terminals.joinToString { it.tag }}" }
        if (Debug.CHECK) check(prev.rulePosition.isAtEnd.not())
        this._followInContext[prev.rulePosition][rulePosition].addAll(terminals)
    }

    private fun addFollowInContextAsReferenceToFirstTerminal(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($tgtPrev,$tgtRulePosition) = firstTerm($srcPrev,$srcRulePosition)" }
        if (Debug.CHECK) check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_TERM, srcPrev, srcRulePosition))
    }

    private fun addFollowAtEndInContext(prev: RulePosition, runtimeRule: RuntimeRule, terminal: RuntimeRule) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add followAtEnd($prev,${runtimeRule.tag}) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followAtEndInContext[prev][runtimeRule].add(terminal)
    }

    private fun addAllFollowAtEndInContext(prev: StateInfoDeferred, runtimeRule: RuntimeRule, terminals: Set<RuntimeRule>) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add followAtEnd(${prev.rulePosition}[${prev.follow}],${runtimeRule.tag}) = ${terminals.joinToString { it.tag }}" }
        if (Debug.CHECK) check(prev.rulePosition.isAtEnd.not())
        this._followAtEndInContext[prev.rulePosition][runtimeRule].addAll(terminals)
    }

    private fun addFollowAtEndInContextAsCalcFollow(prev: StateInfoDeferred, runtimeRule: RuntimeRule, followAtEnd: FollowDeferred) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add followAtEnd(${prev.rulePosition}[${prev.follow}],${runtimeRule.tag}) = $followAtEnd" }
        if (Debug.CHECK) check(prev.rulePosition.isAtEnd.not())
        _followAtEndInContextAsFunc[prev.rulePosition][runtimeRule].add(followAtEnd)
    }

    private fun addFollowInContextAsReferenceToFollow(followPrev: RulePosition, followRuntimeRule: RuntimeRule, refPrev: RulePosition, refRuntimeRule: RuntimeRule) {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.NONE) { "add followAtEnd($followPrev,${followRuntimeRule.tag}) = follow($refPrev,$refRuntimeRule)" }
        if (Debug.CHECK) check(followPrev.isAtEnd.not() && refPrev.isAtEnd.not())
        _followAtEndInContextAsReferenceToFollowAtEnd[followPrev][followRuntimeRule].add(Pair(refPrev, refRuntimeRule))
    }

    private fun addParentInContext(prev: StateInfoDeferred, completedRule: RuntimeRule, parentRulePosition: ParentOfInContext) {
        this._parentInContext[prev.rulePosition][completedRule].add(parentRulePosition)
    }

    private fun calcFirstTermClosure(closureStart: ClosureItem, calcFollow: Boolean): Boolean {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: $closureStart, $calcFollow" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        val done = mutableSetOf<ClosureItem>()
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(closureStart)
        var needsNext = false
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            when {
                cls.rulePosition.isAtEnd -> when {
                    cls.rulePosition.isGoal -> {
                        this.addFirstTerminalAndFirstOfInContext(cls.context, cls.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    }
                    cls.rulePosition.isTerminal -> {
                        val r = this.processClosure(cls, calcFollow)
                        needsNext = r || needsNext
                    }
                    else -> error("Internal Error: should never happen")
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
                            // already done
                        }
                    }
                }
            }
        }
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: $closureStart, $calcFollow" }
        return needsNext
    }

    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required
     */
    private fun processClosure(closureItem: ClosureItem, calcFollow: Boolean): Boolean {
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosure: $closureItem" }
        var cls = closureItem
        val terminal = cls.rulePosition.runtimeRule
        if (Debug.CHECK) check(terminal.isTerminal)
        var childNeedsNext = this.setFirsts(cls, terminal, false, calcFollow)
        var goUp = true //always go up once (from the terminal)
        while (goUp && cls.isRoot.not()) {
            cls = cls.asChild.parent
            childNeedsNext = this.setFirsts(cls, terminal, childNeedsNext, calcFollow)
            goUp = cls.rulePosition.isAtStart
        }
        if (Debug.OUTPUT_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosure: $closureItem" }
        return childNeedsNext
    }

    /**
     * return if 'needsNext', ie. true IF {
     *    isTerminal && isEmpty OR
     *    next.isAtEnd && child.needsNext
     * }
     */
    private fun setFirsts(cls: ClosureItem, terminal: RuntimeRule, childNeedsNext: Boolean, calcFollow: Boolean): Boolean {
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
        when {
            cls.isRoot -> Unit
            else -> {
                val parentFollowAtEnd = cls.asChild.parent.parentFollow
                val parent = StateInfoDeferred(cls.asChild.parent.rulePosition, cls.asChild.parentFollow)
                this.addParentInContext(prev, rr, ParentOfInContext(parentFollowAtEnd, parent))
            }
        }
        // set follow for each runtime-rule - HEIGHT/GRAFT needs it
        this.addFollowAtEndInContextAsCalcFollow(prev, rr, cls.follow)

        return when {
            cls.rulePosition.isTerminal -> { // terminals only
                // if it is a terminal and isEmptyRule then needNext
                rr.isEmptyRule
            }
            cls.isRoot -> {
                // for targetState
                this.addFirstTerminalAndFirstOfInContext(prev, rp, terminal)
                if (childNeedsNext) {
                    val rrs = cls.follow.terminals(this)
                    val thisNeedsNext = rrs.any { it.isEmptyRule }
                    this.addAllFollowInContext(prev, rp, rrs)
                    childNeedsNext && thisNeedsNext
                } else {
                    false // NOT childNeedsNext => this NOT needNext
                }
            }
            cls.rulePosition.isAtStart -> {
                if (childNeedsNext) {
                    val rrs = cls.follow.terminals(this)
                    val thisNeedsNext = rrs.any { it.isEmptyRule }
                    this.addAllFollowInContext(prev, rp, rrs)
                    childNeedsNext && thisNeedsNext
                } else {
                    false // NOT childNeedsNext => this NOT needNext
                }
            }
            else -> {
                error("should not happen")
            }
        }
    }

}