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

    data class NextNotAtEnd(
        val previousNextNotAtEnd: NextNotAtEnd?,
        val prev: RulePosition,
        val rulePosition: RulePosition
    )

    private companion object {
        enum class ReferenceFunc { FIRST_TERM, FIRST_OF }

        interface ClosureItem {
            val parent: ClosureItem
            val prev: RulePosition
            val rulePosition: RulePosition

            /**
             * List Of Pairs (prev,rp) that indicate the "next" state from which a 'WIDTH' is possible
             * i.e. growing up from current rp until an rp NOT atEnd
             */
            val nextNotAtEnd: List<NextNotAtEnd>
        }

        data class ClosureItemRoot(
            override val prev: RulePosition,
            override val rulePosition: RulePosition,
            val parentNextNotAtEnd: List<NextNotAtEnd>,
        ) : ClosureItem {
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
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

            override fun toString(): String = "$prev<--$rulePosition"
        }

        data class ClosureItemChild(
            override val parent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItem {
            val parentNextNotAtEnd = parent.nextNotAtEnd
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

            override fun toString(): String = "$parent-->$rulePosition"
        }
    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<RulePosition, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> Boolean indicates if closure does not resolve an Empty terminal )
    private val _needsNext = lazyMutableMapNonNull<RulePosition, MutableMap<RulePosition, Boolean>> { mutableMapOf() }

    // firstOfPrev -> ( firstOfRulePosition -> Set<Pair<firstTermPrev, firstTermRP>> )
    private val _firstOfInContextAsReferenceToFunc =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<Triple<ReferenceFunc, RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RuntimeRule -> set of Terminal-RuntimeRules that could follow given RuntimeRule in given context/prev )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<firstOfPrev, firstOfRP>> )
    private val _followInContextAsReferenceToFirstOf =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentOfInContext>>> { lazyMutableMapNonNull { hashSetOf() } }

    fun firstTerminal(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        check(prev.isAtEnd.not()) { "firstTerminal($prev,$rulePosition)" }
        //val nextNotAtEnd = Pair(this.stateSet.startRulePosition, this.stateSet.finishRulePosition)
        //calcFirstAndFollowForClosureRoot(ClosureItemRoot(prev, rulePosition, listOf(nextNotAtEnd)), true) // repetition checked for within
        val nextNotAtEnd = NextNotAtEnd(null, this.stateSet.startRulePosition, this.stateSet.finishRulePosition)
        // nextNotAtEnd of (startRp, finishRp) should always resolve to 'UP.
        // the correct nextNotAtEnd, may not be (startRp, finishRp),
        // BUT it should also always resolve to 'UP'
        processClosureFor(prev, rulePosition, listOf(nextNotAtEnd), true)
        return this._firstTerminal[prev][rulePosition]
    }

    fun firstOfInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        check(prev.isAtEnd.not()) { "firstOf($prev,$rulePosition)" }
        // firstOf terminals could be added explicitly or via reference to firstTerminal
        // check for references, if there are any resolve them first and remove
        if (this._firstOfInContextAsReferenceToFunc[prev].containsKey(rulePosition)) {
            val list = this._firstOfInContextAsReferenceToFunc[prev].remove(rulePosition)!!
            val firstTerm = list.flatMap { (func, p, rp) ->
                when (func) {
                    ReferenceFunc.FIRST_TERM -> this.firstTerminal(p, rp)
                    ReferenceFunc.FIRST_OF -> this.firstOfInContext(p, rp)
                }
            }.filter { it.isEmptyRule.not() }.toSet()
            this._firstOfInContext[prev][rulePosition].addAll(firstTerm)
        }
        return this._firstOfInContext[prev][rulePosition]
    }

    fun followInContext(prev: RulePosition, runtimeRule: RuntimeRule): Set<RuntimeRule> {
        check(prev.isAtEnd.not()) { "follow($prev,$runtimeRule)" }
        // follow terminals could be added explicitly or via reference to firstof
        // check for references, if there are any resolve them first and remove
        if (this._followInContextAsReferenceToFirstOf[prev].containsKey(runtimeRule)) {
            val list = this._followInContextAsReferenceToFirstOf[prev].remove(runtimeRule)!!
            val firstOf = list.flatMap { (p, rp) -> this.firstOfInContext(p, rp) }.toSet()
            this._followInContext[prev][runtimeRule].addAll(firstOf)
        }
        return this._followInContext[prev][runtimeRule]
    }

    fun parentInContext(prev: RulePosition, completedRule: RuntimeRule): Set<ParentOfInContext> {
        //should always already have been calculated
        return this._parentInContext[prev][completedRule]
    }

    /**
     * Calculate the first position closures for the given (context,rulePosition)
     * and record the firstTerminals, firstOf and follow information.
     * return true if the next position is needed to complete the 'first/follow' information
     * typically true if there is an 'empty' terminal involved or the 'end' of a rule is reached
     */
    // internal so we can use in testing
    internal fun processClosureFor(prev: RulePosition, rulePosition: RulePosition, nextNotAtEnd: List<NextNotAtEnd>, calcFollow: Boolean): Boolean {
        return if (this._needsNext[prev].containsKey(rulePosition)) {
            val doit = when (this._doneFollow[prev][rulePosition]) {
                null -> true
                false -> calcFollow == true
                true -> false
            }
            if (doit) {
                this._doneFollow[prev][rulePosition] = calcFollow
                val r = this.calcFirstTermClosure(ClosureItemRoot(prev, rulePosition, nextNotAtEnd), calcFollow)
                this._needsNext[prev][rulePosition] = r
            }
            this._needsNext[prev][rulePosition]!!
        } else {
            val r = this.calcFirstTermClosure(ClosureItemRoot(prev, rulePosition, nextNotAtEnd), calcFollow)
            this._needsNext[prev][rulePosition] = r
            r
        }
    }

    /**
     * only add firstOf if not empty
     */
    private fun addFirstTerminalAndFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add firstTerm($prev,$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(terminal)
        if (terminal.isEmptyRule) {
            //do not add to firstOf
        } else {
            this.addFirstOfInContext(prev, rulePosition, terminal)
        }
    }

    private fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add firstOf($prev,$rulePosition) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    private fun addFirstOfInContextAsReferenceToFirstTerminal(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add firstOf($tgtPrev,$tgtRulePosition) = firstTerm($srcPrev,$srcRulePosition)" }
        if (Debug.CHECK) check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_TERM, srcPrev, srcRulePosition))
    }

    private fun addFirstOfInContextAsReferenceToFirstOf(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add firstOf($tgtPrev,$tgtRulePosition) = firstOf($srcPrev,$srcRulePosition)" }
        if (Debug.CHECK) check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_OF, srcPrev, srcRulePosition))
    }

    private fun addFollowInContext(prev: RulePosition, runtimeRule: RuntimeRule, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add follow($prev,${runtimeRule.tag}) = ${terminal.tag}" }
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followInContext[prev][runtimeRule].add(terminal)
    }

    private fun addFollowInContextAsReferenceToFirstOf(followPrev: RulePosition, followRuntimeRule: RuntimeRule, firstOfPrev: RulePosition, firstOfRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "add follow($followPrev,${followRuntimeRule.tag}) = firstOf($firstOfPrev,$firstOfRulePosition)" }
        if (Debug.CHECK) check(followPrev.isAtEnd.not() && firstOfPrev.isAtEnd.not())
        _followInContextAsReferenceToFirstOf[followPrev][followRuntimeRule].add(Pair(firstOfPrev, firstOfRulePosition))
    }

    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentRulePosition: ParentOfInContext) {
        this._parentInContext[prev][completedRule].add(parentRulePosition)
    }

    private fun calcFirstTermClosure(closureStart: ClosureItem, calcFollow: Boolean): Boolean {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstTermClosure($closureStart, $calcFollow)" }
        val done = mutableSetOf<Triple<RulePosition, RulePosition, List<NextNotAtEnd>>>()
        val todoList = mutableStackOf<ClosureItem>()
        todoList.push(closureStart)
        var needsNext = false
        while (todoList.isNotEmpty) {
            val cls = todoList.pop()
            when (cls) {
                is ClosureItemRoot -> {
                    val prev = cls.prev
                    val rr = cls.rulePosition.runtimeRule
                    this.addFollowInContext(prev, rr, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                }
                is ClosureItemChild -> {
                    val prev = cls.prev
                    val rr = cls.rulePosition.runtimeRule
                    val parentRp = cls.parent.rulePosition
                    this.addParentInContext(prev, rr, ParentOfInContext(cls.parent.prev, parentRp))
                    if (cls.parent.nextNotAtEnd.isEmpty()) {
                        this.addFollowInContext(prev, rr, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    } else {
                        for (pn in cls.parent.nextNotAtEnd) {
                            this.addFollowInContextAsReferenceToFirstOf(prev, rr, pn.prev, pn.rulePosition)
                        }
                    }
                }
                else -> error("Internal Error: Should not happen")
            }
            when {
                cls.rulePosition.isTerminal -> {
                    if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE) { "process closure: $cls" }
                    val r = this.propagateFirstTerminalUpTheClosure(cls, calcFollow)
                    needsNext = r || needsNext
                }
                cls.rulePosition.item!!.isTerminal -> {
                    val childRp = cls.rulePosition.item!!.asTerminalRulePosition
                    todoList.push(ClosureItemChild(cls, childRp))
                }
                cls.rulePosition.isAtEnd -> error("Internal Error: should never happen")
                else -> {
                    val childRulePositions = cls.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val childCls = ClosureItemChild(cls, childRp)
                        val d = Triple(childCls.prev, childCls.rulePosition, childCls.parent.nextNotAtEnd)
                        if (done.contains(d).not()) {
                            done.add(d)
                            todoList.push(childCls)
                        } else {
                            // already done
                        }
                    }
                }
            }
        }
        if (Debug.OUTPUT) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstTermClosure($closureStart)" }
        return needsNext
    }

    // may be called with closure that is not the 'Root' of the closure, rather a starting point in a closure,
    // processing above the start would already be in progress in that case
    /**
     * return true if needsNext (i.e. could be empty)
     */
    /*
    private fun calcFirstAndFollowForClosureRoot(closureStart: ClosureItem, calcFollow: Boolean) {
        val doit = when (this._doneFollow[closureStart.prev][closureStart.rulePosition]) {
            null -> true
            false -> calcFollow == true
            true -> false
        }
        if (doit) {
            if (Debug.OUTPUT) debug(Debug.IndentDelta.INC_AFTER) { "START calcFirstAndFollowForClosureRoot($closureStart, $calcFollow)" }
            this._doneFollow[closureStart.prev][closureStart.rulePosition] = calcFollow
            val done = mutableSetOf<Pair<Set<RulePosition>, RulePosition>>()
            done.add(Pair(closureStart.nextPrev, closureStart.rulePosition))
            val todoList = mutableStackOf<CalculationTask>()
            // handle root first as it has no parent defined
            when {
                closureStart.rulePosition.isAtEnd -> this.addFirstTerminalAndFirstOfInContext(closureStart.prev, closureStart.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                closureStart.rulePosition.item!!.isTerminal -> {
                    val childRp = closureStart.rulePosition.item!!.asTerminalRulePosition
                    val needsFollow = childRp.isTerminal
                    todoList.push(CalculationTask(ClosureItemChild(closureStart, childRp), needsFollow))
                }
                else -> {
                    val childRulePositions = closureStart.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val d = Pair(closureStart.nextPrev, childRp)
                        done.add(d)
                        val needsFollow = childRp.isTerminal //always false here
                        todoList.push(CalculationTask(ClosureItemChild(closureStart, childRp), needsFollow))
                    }
                }
            }

            // handle rest of Tasks
            while (todoList.isNotEmpty) {
                val td = todoList.pop()
                this.addParentInContext(td.closureItem.prev, td.closureItem.rulePosition.runtimeRule, td.closureItem.parent.rulePosition)
                when { //maybe check if already calc'd
                    td.closureItem.rulePosition.isTerminal -> {
                        this.propagateFirstTerminalUpTheClosure(td.closureItem, calcFollow)
                    }
                    td.closureItem.rulePosition.item!!.isTerminal -> {
                        val childRp = td.closureItem.rulePosition.item!!.asTerminalRulePosition
                        val needsFollow = childRp.isTerminal
                        todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFollow))
                    }
                    td.closureItem.rulePosition.isAtEnd -> error("Internal Error: should never happen")
                    else -> {
                        val childRulePositions = td.closureItem.rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            val d = Pair(td.closureItem.nextPrev, childRp)
                            if (done.contains(d).not()) {
                                done.add(d)
                                val needsFollow = childRp.isTerminal //always false here
                                todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFollow))
                                if (td.needsFollow) { // only calc lookahead if needed
                                    // if childNextRp is atEnd then lookahead is firstNonEmptyTerminal of parent.next
                                    // else lookahead is firstNonEmptyTerminal of childNextRp.next
                                    when {
                                        childRp.isAtEnd -> {
                                            val next = td.closureItem.rulePosition.next()
                                            next.forEach { todoList.push(CalculationTask(ClosureItemChild(td.closureItem, it), false)) }
                                        }
                                        else -> {
                                            val next = childRp.next()
                                            next.forEach { todoList.push(CalculationTask(ClosureItemChild(td.closureItem, it), false)) }
                                        }
                                    }
                                } else {
                                    //do nothing
                                }
                            } else {
                                // already done
                            }
                        }
                    }
                }
            }
            if (Debug.OUTPUT) debug(Debug.IndentDelta.DEC_BEFORE) { "FINISH calcFirstAndFollowForClosureRoot($closureStart)" }
        }
    }
    */

    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required
     */
    private fun propagateFirstTerminalUpTheClosure(closureItem: ClosureItem, calcFollow: Boolean): Boolean {
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
        return childNeedsNext
    }

    /**
     * return if 'needsNext', ie. true IF {
     *    isTerminal && isEmpty OR
     *    next.isAtEnd && child.needsNext
     * }
     */
    private fun setFirsts(cls: ClosureItem, terminal: RuntimeRule, childNeedsNext: Boolean, calcFollow: Boolean): Boolean {
        // when X we need to calculate {
        //   startState -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        //   atStart -> Nothing needed (unless startState)
        //   atEnd -> HEIGHT/GRAFT - targetState = parent.next, lookahead = firstOf(targetState)
        //   else (inMiddle) -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        // }
        // therefore
        //   parentOf is calculated on the way down the tree - in calcFirstAndFollowForClosureRoot
        //   when {
        //      isTerminal -> set follow
        //      aEnd -> set firstOf to firstOf nextClosure - first parent with next not atEnd
        //
        //   }
        // other than the start state, firstTerm & firstOf are never called on RPs at the start
        // also never called on a Terminal
        return when {
            cls.rulePosition.isTerminal -> { // terminals only
                if (cls.nextNotAtEnd.isEmpty()) {
                    this.addFollowInContext(cls.prev, cls.rulePosition.runtimeRule, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                } else {
                    for (np in cls.nextNotAtEnd) {
                        //this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(np.first, np.second, emptyList()), false)
                        // perhaps should be nextNotAtEnd.previous rather than parent.nextNotAtEnd
                        val nnae = np.previousNextNotAtEnd?.let{listOf(it)} ?: emptyList()
                        processClosureFor(np.prev, np.rulePosition, nnae, false)
                        this.addFollowInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition.runtimeRule, np.prev, np.rulePosition)
                    }
                }
                /*
                for (npvCls in cls.nextClosure) {
                    val npv = npvCls.rulePosition
                    when {
                        npv.isAtEnd -> {
                            // this only happens if the Root of the closure's next is atEnd, in which case we use UP to get the LHS at runtime
                            val ifRootAtEnd = cls.ifRootAtEnd
                            when (ifRootAtEnd) {
                                is RuntimeRule -> this.addFollowInContext(cls.prev, cls.rulePosition.runtimeRule, ifRootAtEnd)
                                is List<*> -> ifRootAtEnd.forEach {
                                    val ifRAE = it as Pair<*, *>
                                    val foPrev = ifRAE.first as RulePosition
                                    val foRp = ifRAE.second as RulePosition
                                    val ifRootAtEnd2 = when (npvCls) {
                                        is ClosureItemRoot -> {
                                            npvCls.ifRootAtEnd
                                        }
                                        else -> npvCls.parent.nextPrev.map { Pair(npvCls.parent.prev, it) }
                                    }
                                    this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(foPrev, foRp, ifRootAtEnd2), false)
                                    //this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(foPrev, foRp,ifRootAtEnd2), false)
                                    this.addFollowInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition.runtimeRule, foPrev, foRp)
                                }
                                else -> error("Internal Error: should not happen")
                            }
                        }
                        else -> {
                            if (calcFollow || terminal.isEmptyRule) {
                                val ifRootAtEnd = when (npvCls) {
                                    is ClosureItemRoot -> {
                                        npvCls.ifRootAtEnd
                                    }
                                    else -> npvCls.parent.nextPrev.map { Pair(npvCls.prevPrev, it) }
                                }
                                this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prevPrev, npv, ifRootAtEnd), false)
                                //this.calcFirstAndFollowForClosureRoot(npvCls, false)
                            }
                            this.addFollowInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition.runtimeRule, npvCls.prevPrev, npv)
                        }
                    }
                }
                 */
                // if it is a terminal and isEmptyRule then needNext
                cls.rulePosition.runtimeRule.isEmptyRule
            }
            cls is ClosureItemRoot -> {
                // for targetState
                this.addFirstTerminalAndFirstOfInContext(cls.prev, cls.rulePosition, terminal)
                if (childNeedsNext) {
                    var thisNeedsNext = false
                    for (pn in cls.rulePosition.next()) {
                        if (pn.isAtEnd) {
                            if (cls.nextNotAtEnd.isEmpty()) {
                                this.addFirstOfInContext(cls.prev, cls.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                                thisNeedsNext = true
                            } else {
                                for (np in cls.nextNotAtEnd) {
                                    //this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(np.first, np.second, listOf()), false)
                                    // perhaps should be nextNotAtEnd.previous rather than parent.nextNotAtEnd
                                    val nnae = np.previousNextNotAtEnd?.let{listOf(it)} ?: emptyList()
                                    val r = processClosureFor(np.prev, np.rulePosition, nnae, false)
                                    thisNeedsNext = r || thisNeedsNext
                                    this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, np.prev, np.rulePosition)
                                }
                            }
                            /*
                            for (npvCls in cls.nextClosure) {
                                val npv = npvCls.rulePosition
                                when {
                                    npv.isAtEnd -> {
                                        //maybe do no need to do this!
                                        // this only happens if the Root of the closure's next is atEnd, in which case we use UP to get the LHS at runtime
                                        val ifRootAtEnd = cls.ifRootAtEnd
                                        when (ifRootAtEnd) {
                                            //is RuntimeRule -> this.addFirstOfInContext(cls.prev, cls.rulePosition, ifRootAtEnd)
                                            is List<*> -> ifRootAtEnd.forEach {
                                                val ifRAE = it as Pair<*, *>
                                                val foPrev = ifRAE.first as RulePosition
                                                val foRp = ifRAE.second as RulePosition
                                                val ifRootAtEnd2 = when (npvCls) {
                                                    is ClosureItemRoot -> {
                                                        npvCls.ifRootAtEnd
                                                    }
                                                    else -> npvCls.parent.nextPrev.map { Pair(npvCls.parent.prev, it) }
                                                }
                                                this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(foPrev, foRp, ifRootAtEnd2), false)
                                                this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, foPrev, foRp)
                                            }
                                            else -> error("Internal Error: should not happen")
                                        }
                                    }
                                    else -> {
                                        val ifRootAtEnd = when (npvCls) {
                                            is ClosureItemRoot -> {
                                                npvCls.ifRootAtEnd
                                            }
                                            else -> npvCls.parent.nextPrev.map { Pair(npvCls.parent.prev, it) }
                                        }
                                        this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prev, npv, ifRootAtEnd), false)
                                        //this.calcFirstAndFollowForClosureRoot(npvCls, false)
                                        this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, cls.prev, npv)
                                    }
                                }
                            }
                            */
                        } else {
                            if (cls.nextNotAtEnd.isEmpty()) {
                                this.addFirstOfInContext(cls.prev, cls.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                            } else {
                                for (np in cls.nextNotAtEnd) {
                                    //this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(np.first, np.second, listOf()), false)
                                    // perhaps should be nextNotAtEnd.previous rather than parent.nextNotAtEnd
                                    val nnae = np.previousNextNotAtEnd?.let{listOf(it)} ?: emptyList()
                                    val r = processClosureFor(np.prev, np.rulePosition, nnae, false)
                                    thisNeedsNext = r || thisNeedsNext
                                    this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, np.prev, np.rulePosition)
                                }
                            }
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
                    if (cls.nextNotAtEnd.isEmpty()) {
                        this.addFirstOfInContext(cls.prev, cls.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        thisNeedsNext = cls.rulePosition.next().any { it.isAtEnd }
                    } else {
                        for (np in cls.nextNotAtEnd) {
                            //val r = this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(np.first,np.second, listOf()), false)
                            // perhaps should be nextNotAtEnd.previous rather than parent.nextNotAtEnd
                            val nnae = np.previousNextNotAtEnd?.let{listOf(it)} ?: emptyList()
                            val r = processClosureFor(np.prev, np.rulePosition, nnae, false)
                            thisNeedsNext = r || thisNeedsNext
                            this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, np.prev, np.rulePosition)
                        }
                    }
                    /*
                    for (pn in cls.rulePosition.next()) {
                        if (pn.isAtEnd) {
                            for (npvCls in cls.nextClosure) {
                                val npv = npvCls.rulePosition
                                when {
                                    npv.isAtEnd -> {
                                        // this only happens if the Root of the closure's next is atEnd, in which case we use UP to get the LHS at runtime
                                        val ifRootAtEnd = cls.ifRootAtEnd
                                        when (ifRootAtEnd) {
                                            is RuntimeRule -> this.addFirstOfInContext(cls.prev, cls.rulePosition, ifRootAtEnd)
                                            is List<*> -> ifRootAtEnd.forEach {
                                                val ifRAE = it as Pair<*, *>
                                                val foPrev = ifRAE.first as RulePosition
                                                val foRp = ifRAE.second as RulePosition
                                                val ifRootAtEnd2 = when (npvCls) {
                                                    is ClosureItemRoot -> {
                                                        npvCls.ifRootAtEnd
                                                    }
                                                    else -> npvCls.parent.nextPrev.map { Pair(npvCls.parent.prev, it) }
                                                }
                                                this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(foPrev, foRp, ifRootAtEnd2), false)
                                                this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, pn, foPrev, foRp)
                                            }
                                            else -> error("Internal Error: should not happen")
                                        }
                                    }
                                    else -> {
                                        val ifRootAtEnd = when (npvCls) {
                                            is ClosureItemRoot -> {
                                                npvCls.ifRootAtEnd
                                            }
                                            else -> npvCls.parent.nextPrev.map { Pair(npvCls.parent.prev, it) }
                                        }
                                        this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prevPrev, npv, ifRootAtEnd), false)
                                        //this.calcFirstAndFollowForClosureRoot(npvCls, false)
                                        this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, pn, cls.prevPrev, npv)
                                    }
                                }
                            }
                            thisNeedsNext = true
                        } else {
                            //next is not atEnd, nothing to do
                        }
                    }
                     */
                    childNeedsNext && thisNeedsNext
                } else {
                    false // NOT childNeedsNext => this NOT needNext
                }
            }
            else -> {
                error("should not happen")
                //this.addFirstTerminalAndFirstOfInContext(cls.prev, cls.rulePosition, terminal)
                if (childNeedsNext) {
                }
                false
            }
        }
    }

}