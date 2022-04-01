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

    private companion object {
        enum class ReferenceFunc { FIRST_TERM, FIRST_OF }

        interface ClosureItem {
            val parent: ClosureItem
            val prev: RulePosition
            val rulePosition: RulePosition

            // val prevForChildren: RulePosition
            val nextPrev: Set<RulePosition>
            val prevPrev: RulePosition
        }

        data class ClosureItemRoot(
            override val prev: RulePosition,
            override val rulePosition: RulePosition
        ) : ClosureItem {
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
            override val prevPrev: RulePosition get() = error("ClosureItemRoot has no prevPrev")

            override val nextPrev = this.rulePosition.next()

            override fun toString(): String = "$prev<--$rulePosition"
        }

        data class ClosureItemChild(override val parent: ClosureItem, override val rulePosition: RulePosition) : ClosureItem {
            //override val prev: RulePosition = parent.prevForChildren
            override val prev: RulePosition = when {
                parent.rulePosition.isAtStart -> parent.prev
                else -> parent.rulePosition
            }

            override val nextPrev = when {
                rulePosition.isTerminal -> parent.nextPrev
                else -> rulePosition.next().flatMap {
                    when {
                        it.isAtEnd -> parent.nextPrev
                        else -> listOf(it)
                    }
                }.toSet()
            }
            override val prevPrev: RulePosition
                get() = when {
                    parent.rulePosition.isGoal -> parent.prev
                    parent.rulePosition.isAtStart -> parent.prevPrev
                    else -> parent.prev
                }

            override fun toString(): String = "$parent-->$rulePosition"
        }

        data class CalculationTask(
            val closureItem: ClosureItem,
            val needsFirstOf: Boolean, // pass as arg so we limit what is calculated
            val needsFollow: Boolean, // pass as arg so we limit what is calculated
        ) {
            override fun toString(): String = "${closureItem.nextPrev}<--${closureItem.rulePosition}"
        }
    }

    // prev/context -> ( RulePosition -> Boolean )
    private val _doneFollow = lazyMutableMapNonNull<RulePosition, MutableMap<RulePosition, Boolean>> { mutableMapOf() }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // firstOfPrev -> ( firstOfRulePosition -> Set<Pair<firstTermPrev, firstTermRP>> )
    private val _firstOfInContextAsReferenceToFunc =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<Triple<ReferenceFunc, RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> function to get value from _firstTerminal )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<firstOfPrev, firstOfRP>> )
    private val _followInContextAsReferenceToFirstOf =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RulePosition>>> { lazyMutableMapNonNull { hashSetOf() } }

    private fun containsFirstTerminal(prev: RulePosition, rulePosition: RulePosition): Boolean = this._firstTerminal[prev].containsKey(rulePosition)

    fun firstTerminal(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        check(prev.isAtEnd.not()) { "firstTerminal($prev,$rulePosition)" }
        this.calcFirstAndFollowFor(prev, rulePosition) // repetition checked for within
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

    fun followInContext(prev: RulePosition, terminalRule: RuntimeRule): Set<RuntimeRule> {
        check(prev.isAtEnd.not()) { "follow($prev,$terminalRule)" }
        // follow terminals could be added explicitly or via reference to firstof
        // check for references, if there are any resolve them first and remove
        if (this._followInContextAsReferenceToFirstOf[prev].containsKey(terminalRule)) {
            val list = this._followInContextAsReferenceToFirstOf[prev].remove(terminalRule)!!
            val firstOf = list.flatMap { (p, rp) -> this.firstOfInContext(p, rp) }.toSet()
            this._followInContext[prev][terminalRule].addAll(firstOf)
        }
        return this._followInContext[prev][terminalRule]
    }

    fun parentInContext(prev: RulePosition, completedRule: RuntimeRule): Set<RulePosition> {
        //should always already have been calculated
        return this._parentInContext[prev][completedRule]
    }

    private fun addFirstTerminalInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add firstTerm($prev,$rulePosition) = ${terminal.tag}"}
        if (Debug.CHECK)  check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(terminal)
        if (terminal.isEmptyRule) {
            //do not add to firstOf
        } else {
            this.addFirstOfInContext(prev, rulePosition, terminal)
        }
    }

    private fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add firstOf($prev,$rulePosition) = ${terminal.tag}"}
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    private fun addFirstOfInContextAsReferenceToFirstTerminal(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add firstOf($tgtPrev,$tgtRulePosition) = firstTerm($srcPrev,$srcRulePosition)"}
        if (Debug.CHECK)  check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_TERM, srcPrev, srcRulePosition))
    }

    private fun addFirstOfInContextAsReferenceToFirstOf(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add firstOf($tgtPrev,$tgtRulePosition) = firstOf($srcPrev,$srcRulePosition)"}
        if (Debug.CHECK)  check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_OF, srcPrev, srcRulePosition))
    }

    private fun addFollowInContext(prev: RulePosition, terminalRule: RuntimeRule, terminal: RuntimeRule) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add follow($prev,${terminalRule.tag}) = ${terminal.tag}"}
        if (Debug.CHECK) check(prev.isAtEnd.not())
        this._followInContext[prev][terminalRule].add(terminal)
    }

    private fun addFollowInContextAsReferenceToFirstOf(followPrev: RulePosition, followTerminalRule: RuntimeRule, firstOfPrev: RulePosition, firstOfRulePosition: RulePosition) {
        if (Debug.OUTPUT) debug(Debug.IndentDelta.NONE){"add follow($followPrev,${followTerminalRule.tag}) = firstOf($firstOfPrev,$firstOfRulePosition)"}
        if (Debug.CHECK) check(followPrev.isAtEnd.not() && firstOfPrev.isAtEnd.not())
        _followInContextAsReferenceToFirstOf[followPrev][followTerminalRule].add(Pair(firstOfPrev, firstOfRulePosition))
    }

    private fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentRulePosition: RulePosition) {
        this._parentInContext[prev][completedRule].add(parentRulePosition)
    }

    /**
     * traverse down the closure of rulePosition.
     * record the firstTerminal and the lookahead of each rulePosition in the closure
     *
     * only call this if certain need to calc FirstAndFollow.
     * can check if needed by testing _firstTerminal[rulePosition]==null
     */
    private fun calcFirstAndFollowFor(prev: RulePosition, rulePosition: RulePosition) {
        when {
            //this.containsFirstTerminal(prev, rulePosition) -> Unit //already calculated, don't repeat
            else -> {
                //handle special case for Goal
                when {
                    rulePosition.isGoal && rulePosition.isAtStart -> {
                        // val goalEndRp = this.stateSet.endState.rulePositions.first()
                        // this.addFirstTerminalInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        //this.addFirstOfInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        //this.addFollowInContext(prev, rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    }
                    else -> Unit
                }
                val closureRoot = ClosureItemRoot(prev, rulePosition)
                calcFirstAndFollowForClosureRoot(closureRoot, true)
            }
        }
    }

    private fun calcFirstAndFollowForClosureRoot(closureRoot: ClosureItemRoot, calcFollow: Boolean) {
        val doit = when (this._doneFollow[closureRoot.prev][closureRoot.rulePosition]) {
            null -> true
            false -> calcFollow == true
            true -> false
        }
        if (doit) {
            if (Debug.OUTPUT) debug(Debug.IndentDelta.INC_AFTER){"START calcFirstAndFollowForClosureRoot($closureRoot, $calcFollow)"}
            this._doneFollow[closureRoot.prev][closureRoot.rulePosition] = calcFollow
            val done = mutableSetOf<Pair<Set<RulePosition>, RulePosition>>() // Pair(prev,rulePos)

            /*
            for (rpn in closureRoot.rulePosition.next()) {
                //should be atEnd
                if (rpn.isAtEnd) {
                    for (npv in closureRoot.nextPrev) {
                        when {
                            npv.isAtEnd -> {
                                this.addFirstTerminalInContext(closureRoot.prev, rpn, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                            }
                            else -> {
                                this.addFirstOfInContextAsReferenceToFirstTerminal(closureRoot.prev, rpn, closureRoot.prev, npv)
                                if (calcFollow) this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(closureRoot.prev, npv), false)
                            }
                        }
                    }
                } else {
                    //no need to reference
                }
            }
             */

            done.add(Pair(closureRoot.nextPrev, closureRoot.rulePosition))
            val todoList = mutableStackOf<CalculationTask>()
            // handle root first as it has no parent defined
            when {
                closureRoot.rulePosition.isAtEnd -> error("Internal Error: cannot compute firstOf for root of a closure")
                closureRoot.rulePosition.item!!.isTerminal -> {
                    val childRp = closureRoot.rulePosition.item!!.asTerminalRulePosition
                    val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                    val needsFollow = childRp.isTerminal
                    todoList.push(CalculationTask(ClosureItemChild(closureRoot, childRp), needsFirstOf, needsFollow))
                }
                else -> {
                    val childRulePositions = closureRoot.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val d = Pair(closureRoot.nextPrev, childRp)
                        done.add(d)
                        val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                        val needsFollow = childRp.isTerminal //always false here
                        todoList.push(CalculationTask(ClosureItemChild(closureRoot, childRp), needsFirstOf, needsFollow))
                    }
                }
            }

            // handle rest of Tasks
            while (todoList.isNotEmpty) {
                val td = todoList.pop()
                this.addParentInContext(td.closureItem.prev, td.closureItem.rulePosition.runtimeRule, td.closureItem.parent.rulePosition)
                /*
                for (rpn in td.closureItem.rulePosition.next()) {
                    //should be atEnd
                    if (rpn.isAtEnd) {
                        for (npv in td.closureItem.nextPrev) {
                            this.addFirstOfInContextAsReferenceToFirstTerminal(td.closureItem.prev, rpn, td.closureItem.prevPrev, npv)
                            if (calcFollow) this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(td.closureItem.prevPrev, npv), false)
                        }
                    } else {
                        //no need to reference
                    }
                }
                */
                when { //maybe check if already calc'd
                    td.closureItem.rulePosition.isTerminal -> {
                        this.propagateFirstTerminalUpTheClosure(td.closureItem, calcFollow)
                        //only need follow for terminals
                        for (pnp in td.closureItem.parent.nextPrev) {
                            this.addFollowInContextAsReferenceToFirstOf(td.closureItem.prev, td.closureItem.rulePosition.runtimeRule, td.closureItem.parent.prevPrev, pnp)
                            if (calcFollow) this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(td.closureItem.prevPrev, pnp), false)
                        }
                    }
                    td.closureItem.rulePosition.item!!.isTerminal -> {
                        val childRp = td.closureItem.rulePosition.item!!.asTerminalRulePosition
                        val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                        val needsFollow = childRp.isTerminal
                        todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow))
                    }
                    td.closureItem.rulePosition.isAtEnd -> error("Internal Error: should never happen")
                    else -> {
                        val childRulePositions = td.closureItem.rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            val d = Pair(td.closureItem.nextPrev, childRp)
                            if (done.contains(d).not()) {
                                done.add(d)
                                val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                                val needsFollow = childRp.isTerminal //always false here
                                todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow))
                                if (td.needsFollow) { // only calc lookahead if needed
                                    // if childNextRp is atEnd then lookahead is firstNonEmptyTerminal of parent.next
                                    // else lookahead is firstNonEmptyTerminal of childNextRp.next
                                    when {
                                        childRp.isAtEnd -> {
                                            val next = td.closureItem.rulePosition.next()
                                            next.forEach { todoList.push(CalculationTask(ClosureItemChild(td.closureItem, it), true, false)) }
                                        }
                                        else -> {
                                            val next = childRp.next()
                                            next.forEach { todoList.push(CalculationTask(ClosureItemChild(td.closureItem, it), true, false)) }
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
            if (Debug.OUTPUT) debug(Debug.IndentDelta.DEC_BEFORE){"FINISH calcFirstAndFollowForClosureRoot($closureRoot)"}
        }
    }

    /**
     * iterate up a closure and set firstTerm,firstOf,follow as required
     */
    private fun propagateFirstTerminalUpTheClosure(closureItem: ClosureItem, calcFollow: Boolean) {
        var cls = closureItem
        val terminal = cls.rulePosition.runtimeRule
        if (Debug.CHECK)  check(terminal.isTerminal)
        var needsNextIfEmpty = terminal.isEmptyRule
        this.setFirsts(cls, terminal, needsNextIfEmpty,calcFollow)
        while (cls !is ClosureItemRoot) {
            cls = cls.parent
            this.setFirsts(cls, terminal, needsNextIfEmpty,calcFollow)
            if (cls !is ClosureItemRoot && cls.parent.rulePosition.isAtEnd.not()) {
                needsNextIfEmpty = false
            }
        }
    }

    private fun setFirsts(cls: ClosureItem, terminal: RuntimeRule, terminalEmptyAndNeedsNext: Boolean, calcFollow: Boolean) {
        // when {
        //   startState -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        //   atStart -> Nothing needed (unless startState)
        //   atEnd -> HEIGHT/GRAFT - targetState = parent.next, lookahead = firstOf(targetState)
        //   else (inMiddle) -> WIDTH - targetState = firstTerminals, lookahead = follow(targetState)
        // }
        // other than the start state, firstTerm & firstOf are never called on RPs at the start
        // also never called on a Terminal
        when {
            cls.rulePosition.isAtStart -> when {
                cls.rulePosition.isGoal -> {
                    this.addFirstTerminalInContext(cls.prev, cls.rulePosition, terminal)
                }
                else -> Unit // nothing needed
            }
            cls.rulePosition.isAtEnd -> {
                for (npv in cls.nextPrev) {
                    when {
                        npv.isAtEnd -> this.addFirstOfInContext(cls.prev, cls.rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        else -> {
                            this.addFirstOfInContextAsReferenceToFirstOf(cls.prev, cls.rulePosition, cls.prevPrev, npv)
                            if (cls.rulePosition.isEmptyRule) {
                                this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prevPrev, npv), false)
                            }
                        }
                    }
                }
            }
            else -> {
                // for targetState
                this.addFirstTerminalInContext(cls.prev, cls.rulePosition, terminal)
                //follow handled in calcFirstAndFollowForClosureRoot
            }
        }
    }

}