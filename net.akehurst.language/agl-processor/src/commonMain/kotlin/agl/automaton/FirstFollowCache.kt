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
import net.akehurst.language.collections.mutableStackOf

internal class FirstFollowCache(val stateSet: ParserStateSet) {
    data class ParentOfInContext(
        val context: RulePosition,
        val rulePosition: RulePosition
    )

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
        enum class ReferenceFunc { FIRST_TERM, FIRST_OF }

        /**
         * Identified by :
         *  - parent.rulePosition, used for HEIGHT/GRAFT decision
         *  - prev, used for prev guard for transition
         *  - rulePosition, used for state id
         *  - followAtEnd, used for lookahead guard
         *  - parentFollowAtEnd, used for lookahead up
         */
        interface ClosureItem {
            val parent: ClosureItem
            val prev: RulePosition
            val rulePosition: RulePosition

            val prevPrev: RulePosition
            val parentNextNotAtEnd: List<NextNotAtEnd>

            /**
             * List Of Pairs (prev,rp) that indicate the "next" state from which a 'WIDTH' is possible
             * i.e. growing up from current rp until an rp NOT atEnd
             */
            val nextNotAtEnd: List<NextNotAtEnd>

            val _id :Array<*>

        }

        abstract class ClosureItemAbstract() : ClosureItem {
            override fun hashCode(): Int = this._id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when(other) {
                !is ClosureItem -> false
                else -> this._id.contentDeepEquals(other._id)
            }
            abstract override fun toString(): String
        }

        class ClosureItemRoot(
            override val prev: RulePosition,
            override val rulePosition: RulePosition,
            override val parentNextNotAtEnd: List<NextNotAtEnd>,
        ) : ClosureItemAbstract() {
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
            override val prevPrev: RulePosition get() = error("ClosureItemRoot has no prevPrev")
            override val nextNotAtEnd: List<NextNotAtEnd>
                get() = when {
                    rulePosition.isTerminal -> parentNextNotAtEnd
                    else -> rulePosition.next().flatMap { nxRp ->
                        when {
                            nxRp.isAtEnd -> parentNextNotAtEnd
                            else -> parentNextNotAtEnd.map { NextNotAtEnd(it, prev, nxRp) }
                        }
                    }
                }

            override val _id = arrayOf(prev, null, this.rulePosition, this.nextNotAtEnd, this.parentNextNotAtEnd)

            override fun toString(): String = "$rulePosition[${nextNotAtEnd.joinToString { it.rulePosition.toString() }}]"
        }

        class ClosureItemChild(
            override val parent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract() {
            override val prev: RulePosition = when {
                parent.rulePosition.isAtStart -> parent.prev
                else -> parent.rulePosition
            }
            override val nextNotAtEnd: List<NextNotAtEnd>
                get() = when {
                    rulePosition.isTerminal -> parent.rulePosition.next().flatMap { nxRp ->
                        when {
                            nxRp.isAtEnd -> parentNextNotAtEnd
                            else -> parentNextNotAtEnd.map { NextNotAtEnd(it, parent.prev, nxRp) }
                        }
                    }
                    else -> rulePosition.next().flatMap { nxRp ->
                        when {
                            nxRp.isAtEnd -> parentNextNotAtEnd
                            else -> parentNextNotAtEnd.map { NextNotAtEnd(it, prev, nxRp) }
                        }
                    }
                }
            override val prevPrev: RulePosition = when {
                parent.rulePosition.isGoal -> parent.prev
                parent.rulePosition.isAtStart -> parent.prevPrev
                else -> parent.prev
            }
            override val parentNextNotAtEnd get() = parent.nextNotAtEnd

            override val _id = arrayOf(prev, this.parent.rulePosition, this.rulePosition, this.nextNotAtEnd, this.parentNextNotAtEnd)

            override fun toString(): String = "$parent<--$rulePosition[${nextNotAtEnd.joinToString { it.rulePosition.toString() }}]"
        }
    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<Pair<RulePosition,List<NextNotAtEnd>>, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> Boolean indicates if closure does not resolve an Empty terminal )
    private val _needsNext = lazyMutableMapNonNull<RulePosition, MutableMap<Pair<RulePosition,List<NextNotAtEnd>>, Boolean>> { mutableMapOf() }

    // firstOfPrev -> ( firstOfRulePosition -> Set<Pair<firstTermPrev, firstTermRP>> )
    private val _firstOfInContextAsReferenceToFunc =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<Triple<ReferenceFunc, RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RuntimeRule -> set of Terminal-RuntimeRules that could follow given RuntimeRule in given context/prev )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

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
        this.addFirstTerminalAndFirstOfInContext(this.stateSet.startRulePosition, this.stateSet.finishRulePosition, RuntimeRuleSet.END_OF_TEXT)
    }

    fun clear() {
        this._doneFollow.clear()
        this._firstTerminal.clear()
        this._followInContext.clear()
        this._needsNext.clear()
        this._firstOfInContextAsReferenceToFunc.clear()
        this._firstOfInContext.clear()
        this._followInContextAsReferenceToFirstOf.clear()
        this._followInContextAsReferenceToFollow.clear()
        this._parentInContext.clear()
    }

    // entry point from calcWidth
    fun firstTerminalInContext(context: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        check(context.isAtEnd.not()) { "firstTerminal($context,$rulePosition)" }
        val parentNextNotAtEnd = NextNotAtEnd(null, this.stateSet.startRulePosition, this.stateSet.finishRulePosition)
        // nextNotAtEnd of (startRp, finishRp) should always resolve to 'UP (see init).
        // the correct nextNotAtEnd, may not be (startRp, finishRp),
        // BUT it should also always resolve to 'UP'
        processClosureFor(context, rulePosition, listOf(parentNextNotAtEnd), true)
        return this._firstTerminal[context][rulePosition]
    }

    // entry point from calcHeightGraft
    fun parentInContext(context: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        val parentNextNotAtEnd = NextNotAtEnd(null, this.stateSet.startRulePosition, this.stateSet.finishRulePosition)
        // nextNotAtEnd of (startRp, finishRp) should always resolve to 'UP (see init).
        // the correct nextNotAtEnd, may not be (startRp, finishRp),
        // BUT it should also always resolve to 'UP'

        for(ch in context.item!!.rulePositionsAt[0]) {

        }

        processClosureFor(context, context, listOf(parentNextNotAtEnd), true)
        return this._parentInContext[context][completedRule]
    }

    fun followInContext(context: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        check(context.isAtEnd.not()) { "firstOf($context,$rulePosition)" }
        // firstOf terminals could be added explicitly or via reference to firstTerminal
        // check for references, if there are any resolve them first and remove
        if (this._firstOfInContextAsReferenceToFunc[context].containsKey(rulePosition)) {
            val list = this._firstOfInContextAsReferenceToFunc[context].remove(rulePosition)!!
            val firstTerm = list.flatMap { (func, p, rp) ->
                when (func) {
                    ReferenceFunc.FIRST_TERM -> this.firstTerminalInContext(p, rp)
                    ReferenceFunc.FIRST_OF -> this.followInContext(p, rp)
                }
            }.filter { it.isEmptyRule.not() }.toSet()
            this._firstOfInContext[context][rulePosition].addAll(firstTerm)
        }
        return this._firstOfInContext[context][rulePosition]
    }

    fun followAtEndInContext(context: RulePosition, runtimeRule: RuntimeRule): Set<RuntimeRule> {
        check(context.isAtEnd.not()) { "follow($context,$runtimeRule)" }
        // follow terminals could be added explicitly or via reference to firstof
        // check for references, if there are any resolve them first and remove
        if (this._followInContextAsReferenceToFirstOf[context].containsKey(runtimeRule)) {
            val list = this._followInContextAsReferenceToFirstOf[context].remove(runtimeRule)!!
            val firstOf = list.flatMap { (p, rp) -> this.followInContext(p, rp) }.toSet()
            this._followInContext[context][runtimeRule].addAll(firstOf)
        }
        if (this._followInContextAsReferenceToFollow[context].containsKey(runtimeRule)) {
            val list = this._followInContextAsReferenceToFollow[context].remove(runtimeRule)!!
            val follow = list.flatMap { (p, rr) -> this.followAtEndInContext(p, rr) }.toSet()
            this._followInContext[context][runtimeRule].addAll(follow)
        }
        return this._followInContext[context][runtimeRule]
    }


    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    internal fun processClosureFor(prev: RulePosition, rulePosition: RulePosition, nextNotAtEnd: List<NextNotAtEnd>, calcFollow: Boolean): Boolean {
        val doit = when (this._doneFollow[prev][Pair(rulePosition,nextNotAtEnd)]) {
            null -> true
            false -> calcFollow == true
            true -> false
        }
        if (doit) {
            this._doneFollow[prev][Pair(rulePosition,nextNotAtEnd)] = calcFollow
            val r = this.calcFirstTermClosure(ClosureItemRoot(prev, rulePosition, nextNotAtEnd), calcFollow)
            this._needsNext[prev][Pair(rulePosition,nextNotAtEnd)] = r
        }
        return false
    }

    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalAndFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstTerm($prev,$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(terminal)
        if (terminal.isEmptyRule) {
            //do not add to firstOf
        } else {
            this.addFirstOfInContext(prev, rulePosition, terminal)
        }
    }

    private fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstOf($prev,$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    private fun addFirstOfInContextAsReferenceToFirstTerminal(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstOf($tgtPrev,$tgtRulePosition) = firstTerm($srcPrev,$srcRulePosition)" }
        if (Debug.CHECK) check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_TERM, srcPrev, srcRulePosition))
    }

    private fun addFirstOfInContextAsReferenceToFirstOf(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add firstOf($tgtPrev,$tgtRulePosition) = firstOf($srcPrev,$srcRulePosition)" }
        if (Debug.CHECK) check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_OF, srcPrev, srcRulePosition))

    }

    private fun addFollowInContext(prev: RulePosition, runtimeRule: RuntimeRule, terminal: RuntimeRule) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($prev,${runtimeRule.tag}) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followInContext[prev][runtimeRule].add(terminal)
    }

    private fun addFollowInContextAsReferenceToFirstOf(followPrev: RulePosition, followRuntimeRule: RuntimeRule, firstOfPrev: RulePosition, firstOfRulePosition: RulePosition) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($followPrev,${followRuntimeRule.tag}) = firstOf($firstOfPrev,$firstOfRulePosition)" }
        if (Debug.CHECK) check(followPrev.isAtEnd.not() && firstOfPrev.isAtEnd.not())
        _followInContextAsReferenceToFirstOf[followPrev][followRuntimeRule].add(Pair(firstOfPrev, firstOfRulePosition))
    }

    private fun addFollowInContextAsReferenceToFollow(followPrev: RulePosition, followRuntimeRule: RuntimeRule, refPrev: RulePosition, refRuntimeRule: RuntimeRule) {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.NONE) { "add follow($followPrev,${followRuntimeRule.tag}) = follow($refPrev,$refRuntimeRule)" }
        if (Debug.CHECK) check(followPrev.isAtEnd.not() && refPrev.isAtEnd.not())
        _followInContextAsReferenceToFollow[followPrev][followRuntimeRule].add(Pair(refPrev, refRuntimeRule))
    }

    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentRulePosition: ParentOfInContext) {
        this._parentInContext[prev][completedRule].add(parentRulePosition)
    }

    private fun calcFirstTermClosure(closureStart: ClosureItem, calcFollow: Boolean): Boolean {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure: $closureStart, $calcFollow" }
        // Closures identified by (parent.rulePosition, prev, rulePosition, followAtEnd, parentFollowAtEnd)
        val done = mutableSetOf<ClosureItem>()
        val todoList = mutableStackOf<ClosureItem>()
        todoList.push(closureStart)
        var needsNext = false
        while (todoList.isNotEmpty) {
            val cls = todoList.pop()
            when {
                cls.rulePosition.isAtEnd -> when {
                    cls.rulePosition.isGoal -> {
                        this.addFirstTerminalAndFirstOfInContext(cls.prev, cls.rulePosition, RuntimeRuleSet.END_OF_TEXT)
                    }
                    cls.rulePosition.isTerminal -> {
                        val r = this.processClosure(cls, calcFollow)
                        needsNext = r || needsNext
                    }
                    else -> error("Internal Error: should never happen")
                }
                cls.rulePosition.item!!.isTerminal -> {
                    val childRp = cls.rulePosition.item!!.asTerminalRulePosition
                    todoList.push(ClosureItemChild(cls, childRp))
                }
                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls, childRp)
                        //val d = Triple(childCls.prev, childCls.rulePosition, childCls.parent.nextNotAtEnd)
                        if (done.contains(childCls).not()) {
                            done.add(childCls)
                            todoList.push(childCls)
                        } else {
                            // already done
                        }
                    }
                }
            }
        }
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure: $closureStart, $calcFollow" }
        return needsNext
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
                    todoList.push(ClosureItemChild(cls, childRp))
                }
                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls, childRp)
                        val d = Triple(childCls.prev, childCls.rulePosition, childCls.parent.nextNotAtEnd)
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
    private fun processClosure(closureItem: ClosureItem, calcFollow: Boolean): Boolean {
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.INC_AFTER) { "START processClosure: $closureItem" }
        var cls = closureItem
        val terminal = cls.rulePosition.runtimeRule
        if (Debug.CHECK) check(terminal.isTerminal)
        var childNeedsNext = this.setFirsts(cls, terminal, false, calcFollow)
        var goUp = true //always go up once (from the terminal)
        while (goUp && cls !is ClosureItemRoot) {
            cls = cls.parent
            childNeedsNext = this.setFirsts(cls, terminal, childNeedsNext, calcFollow)
            goUp = cls.rulePosition.isAtStart
        }
        if (Debug.OUTPUT_SM_BUILD) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH processClosure: $closureItem" }
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
        val prev = cls.prev
        val rp = cls.rulePosition
        val rr = cls.rulePosition.runtimeRule

        // set parentOf
        when (cls) {
            is ClosureItemRoot -> Unit
            is ClosureItemChild -> this.addParentInContext(prev, rr, ParentOfInContext(cls.parent.prev, cls.parent.rulePosition))
            else -> error("Internal Error: subtype of ClosureItem not handled")
        }
        // set follow for each runtime-rule - HEIGHT/GRAFT needs it
        if (cls.parentNextNotAtEnd.isEmpty()) {
            this.addFollowInContext(prev, rr, RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
        } else {
            for (pn in cls.parentNextNotAtEnd) {
                val nnae = pn.previousNextNotAtEnd?.let { listOf(it) } ?: emptyList()
                if (calcFollow) processClosureFor(pn.prev, pn.rulePosition, nnae, false)
                this.addFollowInContextAsReferenceToFirstOf(prev, rr, pn.prev, pn.rulePosition)
                //this.addFollowInContextAsReferenceToFollow(prev,rr,pn.prev,pn.rulePosition.runtimeRule)
            }
        }
        return when {
            cls.rulePosition.isTerminal -> { // terminals only
                // if it is a terminal and isEmptyRule then needNext
                rr.isEmptyRule
            }
            cls is ClosureItemRoot -> {
                // for targetState
                this.addFirstTerminalAndFirstOfInContext(prev, rp, terminal)
                if (childNeedsNext) {
                    var thisNeedsNext = false
                    for (rpNxt in rp.next()) {
                        if (rpNxt.isAtEnd) {
                            thisNeedsNext = true
                            if (cls.nextNotAtEnd.isEmpty()) {
                                this.addFirstOfInContext(prev, rp, RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
                                thisNeedsNext = true
                            } else {
                                for (np in cls.nextNotAtEnd) {
                                    val nnae = np.previousNextNotAtEnd?.let { listOf(it) } ?: emptyList()
                                    val r = processClosureFor(np.prev, np.rulePosition, nnae, false)
                                    this.addFirstOfInContextAsReferenceToFirstOf(prev, rp, np.prev, np.rulePosition)
                                }
                            }
                        } else {
                            //val nnae = cls.nextNotAtEnd
                            //processClosureFor(prev, rpNxt, nnae, false)
                            this.addFirstOfInContextAsReferenceToFirstOf(prev, rp, prev, rpNxt)
                        }
                    }
                    childNeedsNext && thisNeedsNext
                } else {
                    false // NOT childNeedsNext => this NOT needNext
                }
            }
            cls.rulePosition.isAtStart -> {
                if (childNeedsNext) {
                    var thisNeedsNext = false
                    for (rpNxt in rp.next()) {
                        if (rpNxt.isAtEnd) {
                            thisNeedsNext = true
                            if (cls.nextNotAtEnd.isEmpty()) {
                                //this.addFirstOfInContext(prev, rp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                                thisNeedsNext = true
                            } else {
                                for (np in cls.nextNotAtEnd) {
                                    val prevPrev = when (cls) {
                                        is ClosureItemRoot -> np.previousNextNotAtEnd?.prev ?: error("???")
                                        is ClosureItemChild -> cls.prevPrev
                                        else -> error("Internal Error: subtype of ClosureItem not handled")
                                    }
                                    val nnae = np.previousNextNotAtEnd?.let { listOf(it) } ?: emptyList()
                                    val r = processClosureFor(np.prev, np.rulePosition, nnae, false)
                                    this.addFirstOfInContextAsReferenceToFirstOf(prevPrev, prev, np.prev, np.rulePosition)
                                }
                            }
                        } else {
                            val prevPrev = when (cls) {
                                is ClosureItemRoot -> error("")
                                is ClosureItemChild -> cls.prevPrev
                                else -> error("Internal Error: subtype of ClosureItem not handled")
                            }
                            this.addFirstOfInContextAsReferenceToFirstOf(prevPrev, prev, prev, rpNxt)
                        }
                    }
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