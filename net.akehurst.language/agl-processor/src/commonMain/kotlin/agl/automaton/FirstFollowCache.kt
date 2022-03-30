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

            //override val prevForChildren = when {
            //    rulePosition.isAtStart -> prev
            //    else -> rulePosition
            //}
            override val nextPrev = this.rulePosition.next()

            override fun toString(): String = "$prev<--$rulePosition"
        }

        data class ClosureItemChild(override val parent: ClosureItem, override val rulePosition: RulePosition) : ClosureItem {
            //override val prev: RulePosition = parent.prevForChildren
            override val prev: RulePosition = when {
                parent.rulePosition.isAtStart -> parent.prev
                else -> parent.rulePosition
            }

            //override val prevForChildren = when {
            //    rulePosition.isAtStart -> prev
            //     else -> rulePosition
            // }
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
            val postTaskActions: (futureTerminal: RuntimeRule) -> Unit = { _ -> }
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

    fun containsFirstTerminal(prev: RulePosition, rulePosition: RulePosition): Boolean = this._firstTerminal[prev].containsKey(rulePosition)

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
        println("add firstTerm($prev,$rulePosition) = ${terminal.tag}")
        check(prev.isAtEnd.not())
        this._firstTerminal[prev][rulePosition].add(terminal)
        if (terminal.isEmptyRule) {
            //do not add to firstOf
        } else {
            this.addFirstOfInContext(prev, rulePosition, terminal)
        }
    }

    private fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        println("add firstOf($prev,$rulePosition) = ${terminal.tag}")
        check(prev.isAtEnd.not())
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    private fun addFirstOfInContextAsReferenceToFirstTerminal(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        println("add firstOf($tgtPrev,$tgtRulePosition) = firstTerm($srcPrev,$srcRulePosition)")
        check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_TERM, srcPrev, srcRulePosition))
    }

    private fun addFirstOfInContextAsReferenceToFirstOf(tgtPrev: RulePosition, tgtRulePosition: RulePosition, srcPrev: RulePosition, srcRulePosition: RulePosition) {
        println("add firstOf($tgtPrev,$tgtRulePosition) = firstOf($srcPrev,$srcRulePosition)")
        check(tgtPrev.isAtEnd.not() && srcPrev.isAtEnd.not())
        _firstOfInContextAsReferenceToFunc[tgtPrev][tgtRulePosition].add(Triple(ReferenceFunc.FIRST_OF, srcPrev, srcRulePosition))
    }

    fun addFollowInContext(prev: RulePosition, terminalRule: RuntimeRule, terminal: RuntimeRule) {
        println("add follow($prev,${terminalRule.tag}) = ${terminal.tag}")
        check(prev.isAtEnd.not())
        this._followInContext[prev][terminalRule].add(terminal)
    }

    private fun addFollowInContextAsReferenceToFirstOf(followPrev: RulePosition, followTerminalRule: RuntimeRule, firstOfPrev: RulePosition, firstOfRulePosition: RulePosition) {
        println("add follow($followPrev,${followTerminalRule.tag}) = firstOf($firstOfPrev,$firstOfRulePosition)")
        check(followPrev.isAtEnd.not() && firstOfPrev.isAtEnd.not())
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
//TODO("mising followInContext - eg. G,0,0 <-- div,0,1")
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
            println("START calcFirstAndFollowForClosureRoot($closureRoot, $calcFollow)")
            this._doneFollow[closureRoot.prev][closureRoot.rulePosition] = calcFollow
            val done = mutableSetOf<Pair<Set<RulePosition>, RulePosition>>() // Pair(prev,rulePos)

            for (rpn in closureRoot.rulePosition.next()) {
                //should be atEnd
                if (rpn.isAtEnd) {
                    for (npv in closureRoot.nextPrev) {
                        when {
                            npv.isAtEnd -> this.addFirstTerminalInContext(closureRoot.prev, rpn, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
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

            done.add(Pair(closureRoot.nextPrev, closureRoot.rulePosition))
            val todoList = mutableStackOf<CalculationTask>()
            // handle root first as it has no parent defined
            when {
                closureRoot.rulePosition.isAtEnd -> Unit // do nothing (terminals are always atEnd, do nothing for them also)
                closureRoot.rulePosition.item!!.isTerminal -> {
                    val childRp = closureRoot.rulePosition.item!!.asTerminalRulePosition
                    val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                    val needsFollow = childRp.isTerminal
                    todoList.push(CalculationTask(ClosureItemChild(closureRoot, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                        this.addFirstTerminalInContext(closureRoot.prev, closureRoot.rulePosition, futureTerminal)
                    })
                }
                else -> {
                    val childRulePositions = closureRoot.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val d = Pair(closureRoot.nextPrev, childRp)
                        done.add(d)
                        val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                        val needsFollow = childRp.isTerminal //always false here
                        todoList.push(CalculationTask(ClosureItemChild(closureRoot, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                            this.addFirstTerminalInContext(closureRoot.prev, closureRoot.rulePosition, futureTerminal)
                        })
                    }
                }
            }

            // handle rest of Tasks
            while (todoList.isNotEmpty) {
                val td = todoList.pop()
                this.addParentInContext(td.closureItem.prev, td.closureItem.rulePosition.runtimeRule, td.closureItem.parent.rulePosition)
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
                when { //maybe check if already calc'd
                    td.closureItem.rulePosition.isTerminal -> {
                        this.propagateFirstTerminalUpTheClosure(td.closureItem)

                        //this.addParentInContext(td.prev, td.rulePosition.runtimeRule, td.parentRulePosition)
                        //this.addFirstTerminalInContext(td.closureItem.prev, td.closureItem.parent.rulePosition, td.closureItem.rulePosition.runtimeRule)
                        //td.postTaskActions.invoke(td.closureItem.rulePosition.runtimeRule) // triggers setting firstTerminal up the closure

                        /*
                    if (td.closureItem.rulePosition.isEmptyRule) {
                        for (npv in td.closureItem.nextPrev) {
                            this.addFirstOfInContextAsReferenceToFirstTerminal(td.closureItem.prev, td.closureItem.parent.rulePosition, td.closureItem.prevPrev, npv)
                            // have to do the calc or cannot resolve replacement for 'empty'
                            this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(td.closureItem.prevPrev, npv),false)
                        }
                    } else {
                        // firstOf(prev, rp) = firstTerms(prev, cls.nextPrev)
                        // this.addFirstOfInContext(closureRoot.prev, closureRoot.rulePosition, closureRoot.rulePosition.item!!)
                    }
                    */

                        //only need follow for terminals
                        for (nextPrev in td.closureItem.nextPrev) {
                            // follow(term) = firstOf(term.parent.next) = firstOf()
                            this.addFollowInContextAsReferenceToFirstOf(
                                td.closureItem.prev,
                                td.closureItem.rulePosition.runtimeRule,
                                td.closureItem.prevPrev,
                                nextPrev
                            ) // check target prev
                            if (calcFollow) {
                                //firstOf(parent.next) = firstTerminals(parent.next) excluding <empty>
                                // and including when empty, firstOf(parent.next.next) || firstOf(parent.next.parent)
                                val closureRoot1 = ClosureItemRoot(td.closureItem.prevPrev, nextPrev)
                                calcFirstAndFollowForClosureRoot(closureRoot1, false)
                            }
                        }
                    }
                    td.closureItem.rulePosition.item!!.isTerminal -> {
                        val childRp = td.closureItem.rulePosition.item!!.asTerminalRulePosition
                        val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                        val needsFollow = childRp.isTerminal
                        todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                            this.addFirstTerminalInContext(td.closureItem.prev, td.closureItem.rulePosition, futureTerminal)
                            td.postTaskActions.invoke(futureTerminal)
                        })
                    }
                    td.closureItem.rulePosition.isAtEnd -> TODO() //this.addParentInContext(td.prev, td.rulePosition.runtimeRule, td.parentRulePosition) //TODO: think this never happens
                    else -> {
                        val childRulePositions = td.closureItem.rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            val d = Pair(td.closureItem.nextPrev, childRp)
                            if (done.contains(d).not()) {
                                done.add(d)
                                val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                                val needsFollow = childRp.isTerminal //always false here
                                todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                                    this.addFirstTerminalInContext(td.closureItem.prev, td.closureItem.rulePosition, futureTerminal)
                                    td.postTaskActions.invoke(futureTerminal)
                                })
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
                                val i = 0
                            }
                        }
                    }
                }
            }
            println("FINISH calcFirstAndFollowForClosureRoot($closureRoot)")
        }
    }

    private fun propagateFirstTerminalUpTheClosure(closureItem: ClosureItem) {
        var cls = closureItem
        val terminal = cls.rulePosition.runtimeRule
        check(terminal.isTerminal)
        this.setFirsts(cls, terminal)
        while (cls !is ClosureItemRoot) {
            cls = cls.parent
            this.setFirsts(cls, terminal)
        }
    }

    private fun setFirsts(cls: ClosureItem, terminal: RuntimeRule) {
        if (cls.rulePosition.isTerminal.not()) {
            this.addFirstTerminalInContext(cls.prev, cls.rulePosition, terminal)
        }
        if (cls is ClosureItemRoot) {

        } else {
            if (terminal.isEmptyRule) {
                for (npv in cls.nextPrev) {
                    this.addFirstOfInContextAsReferenceToFirstOf(cls.parent.prev, cls.parent.rulePosition, cls.prevPrev, npv)
                    // have to do the calc or cannot resolve replacement for 'empty'
                    this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prevPrev, npv), false)
                }
            }
        }
        //this.addFirstTerminalInContext(cls.prev, cls.parent.rulePosition, terminal)
        //if (terminal.isEmptyRule) {
        //    for (npv in cls.nextPrev) {
        //        this.addFirstOfInContextAsReferenceToFirstTerminal(cls.prev, cls.parent.rulePosition, cls.prevPrev, npv)
        // have to do the calc or cannot resolve replacement for 'empty'
        //       this.calcFirstAndFollowForClosureRoot(ClosureItemRoot(cls.prevPrev, npv), false)
        //    }
        //} else {
        //    // no need
        //}
    }

}