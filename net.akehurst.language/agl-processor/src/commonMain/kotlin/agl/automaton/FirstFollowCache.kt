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
        interface ClosureItem {
            val parent: ClosureItem
            val prev: RulePosition
            val rulePosition: RulePosition
            val prevForChildren: RulePosition
            val nextPrev: Set<RulePosition>
        }

        data class ClosureItemRoot(
            override val prev: RulePosition,
            override val rulePosition: RulePosition
        ) : ClosureItem {
            override val parent: ClosureItem get() = error("ClosureItemRoot has no parent")
            override val prevForChildren = when {
                rulePosition.isAtStart -> prev
                else -> rulePosition
            }
            override val nextPrev = this.rulePosition.next()

            override fun toString(): String = "$rulePosition"
        }

        data class ClosureItemChild(override val parent: ClosureItem, override val rulePosition: RulePosition) : ClosureItem {
            override val prev: RulePosition = parent.prevForChildren
            override val prevForChildren = when {
                rulePosition.isAtStart -> prev
                else -> rulePosition
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

            override fun toString(): String = "$parent-->$rulePosition"
        }

        data class CalculationTask(
            val closureItem: ClosureItem,
            val needsFirstOf: Boolean, // pass as arg so we limit what is calculated
            val needsFollow: Boolean, // pass as arg so we limit what is calculated
            val postTaskActions: (futureTerminal: RuntimeRule) -> Unit = { _ -> }
        ) {
            val prev get() = closureItem.prev
            val rulePosition get() = closureItem.rulePosition
            val parentRulePosition get() = closureItem.parent.rulePosition
            val prevForChildren get() = closureItem.prevForChildren

            override fun toString(): String = "${closureItem.nextPrev}<--$rulePosition"
        }
    }

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // firstOfPrev -> ( firstOfRulePosition -> Set<Pair<firstTermPrev, firstTermRP>> )
    private val _firstOfInContextAsReferenceToFirstTerminal =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }


    // prev/context -> ( TerminalRule -> function to get value from _firstTerminal )
    private val _followInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RuntimeRule>>> { lazyMutableMapNonNull { hashSetOf() } }

    // followPrev -> ( TerminalRule -> Set<Pair<firstOfPrev, firstOfRP>> )
    private val _followInContextAsReferenceToFirstOf =
        lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<Pair<RulePosition, RulePosition>>>> { lazyMutableMapNonNull { hashSetOf() } }

    // prev/context -> ( TerminalRule -> ParentRulePosition )
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<RulePosition>>> { lazyMutableMapNonNull { hashSetOf() } }

    fun containsFirstTerminal(prev: RulePosition, rulePosition: RulePosition): Boolean = this._firstTerminal[prev].containsKey(rulePosition)

    fun firstTerminal(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return if (this._firstTerminal.containsKey(prev) || this._firstTerminal[prev].containsKey(rulePosition)) {
            this._firstTerminal[prev][rulePosition]
        } else {
            this.calcFirstAndFollowFor(prev, rulePosition)
            this._firstTerminal[prev][rulePosition]
        }
    }

    fun firstOfInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        // firstOf terminals could be added explicitly or via reference to firstTerminal
        // check for references, if there are any resolve them first and remove
        if (this._firstOfInContextAsReferenceToFirstTerminal[prev].containsKey(rulePosition)) {
            val list = this._firstOfInContextAsReferenceToFirstTerminal[prev].remove(rulePosition)!!
            val firstTerm = list.flatMap { (p, rp) -> this.firstTerminal(p, rp) }.toSet()
            this._firstOfInContext[prev][rulePosition].addAll(firstTerm)
        }
        return this._firstOfInContext[prev][rulePosition]
    }

    fun followInContext(prev: RulePosition, terminalRule: RuntimeRule): Set<RuntimeRule> {
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

    fun addFirstTerminalInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstTerminal[prev][rulePosition].add(terminal)
    }

    fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }
    fun addFirstOfInContextAsReferenceToFirstTerminal(firstOfPrev: RulePosition, firstOfRulePosition: RulePosition, firstTermPrev: RulePosition, firstTermRulePosition: RulePosition) {
        _firstOfInContextAsReferenceToFirstTerminal[firstOfPrev][firstOfRulePosition].add(Pair(firstTermPrev, firstTermRulePosition))
    }

    fun addFollowInContext(prev: RulePosition, terminalRule: RuntimeRule, terminal: RuntimeRule) {
        this._followInContext[prev][terminalRule].add(terminal)
    }

    fun addFollowInContextAsReferenceToFirstOf(followPrev: RulePosition, followTerminalRule: RuntimeRule, firstOfPrev: RulePosition, firstOfRulePosition: RulePosition) {
        _followInContextAsReferenceToFirstOf[followPrev][followTerminalRule].add(Pair(firstOfPrev, firstOfRulePosition))
    }

    fun addParentInContext(prev: RulePosition, completedRule: RuntimeRule, parentRulePosition: RulePosition) {
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
            this.containsFirstTerminal(prev, rulePosition) -> Unit //already calculated, don't repeat
            else -> {
                //handle special case for Goal
                when {
                    rulePosition.isGoal && rulePosition.isAtStart -> {
                        val goalEndRp = this.stateSet.endState.rulePositions.first()
                        this.addFirstTerminalInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        this.addFirstOfInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
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
        val done = mutableSetOf<Pair<Set<RulePosition>, RulePosition>>() // Pair(prev,rulePos)
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
                    for(rpn in closureRoot.rulePosition.next()) {
                        //should be atEnd
                        for(npv in closureRoot.nextPrev) {
                            this.addFirstOfInContextAsReferenceToFirstTerminal(closureRoot.prev, rpn, closureRoot.prev, npv)
                        }
                    }
                    //td.postTaskActions.invoke(futureTerminal)
                })
                /*
                this.addFirstTerminalInContext(closureRoot.prev, closureRoot.rulePosition, closureRoot.rulePosition.item!!)
                if (closureRoot.rulePosition.item!!.isEmptyRule) {
                    TODO()
                } else {
                    this.addFirstOfInContext(closureRoot.prev, closureRoot.rulePosition, closureRoot.rulePosition.item!!)
                }

                for (nextPrev in closureRoot.nextPrev) {
                    // follow(X) = firstOf(parent.next)
                    this.addFollowInContextAsReferenceToFirstOf(td.prev, td.rulePosition.runtimeRule, parentClosureItem.prev, nextPrev)
                }

                this.addFollowInContext(closureRoot.prev, closureRoot.rulePosition.runtimeRule, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                 */
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
            this.addParentInContext(td.prev, td.rulePosition.runtimeRule, td.parentRulePosition)
            when { //maybe check if already calc'd
                td.rulePosition.isTerminal -> {
                    //this.addParentInContext(td.prev, td.rulePosition.runtimeRule, td.parentRulePosition)
                    this.addFirstTerminalInContext(td.prev, td.parentRulePosition, td.rulePosition.runtimeRule)
                    td.postTaskActions.invoke(td.rulePosition.runtimeRule) // triggers setting firstTerminal up the closure

                    if (td.rulePosition.isEmptyRule) {
                        TODO()
                    } else {
                        // firstOf(prev, rp) = firstTerms(prev, cls.nextPrev)
                        this.addFirstOfInContext(closureRoot.prev, closureRoot.rulePosition, closureRoot.rulePosition.item!!)
                    }

                    val parentClosureItem = td.closureItem.parent
                    for (nextPrev in parentClosureItem.nextPrev) {
                        // follow(X) = firstOf(parent.next)
                        this.addFollowInContextAsReferenceToFirstOf(parentClosureItem.prev, td.rulePosition.runtimeRule, parentClosureItem.prev, nextPrev) // check target prev
                        if (calcFollow) {
                            //firstOf(parent.next) = firstTerminals(parent.next) excluding <empty>
                            // and including when empty, firstOf(parent.next.next) || firstOf(parent.next.parent)
                            val closureRoot1 = ClosureItemRoot(parentClosureItem.prev, nextPrev)
                            calcFirstAndFollowForClosureRoot(closureRoot1, false)
                        }
                    }
                }
                td.rulePosition.item!!.isTerminal -> {
                    val childRp = td.rulePosition.item!!.asTerminalRulePosition
                    val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                    val needsFollow = childRp.isTerminal
                    todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                        this.addFirstTerminalInContext(td.prev, td.rulePosition, futureTerminal)
                        for(rpn in td.rulePosition.next()) {
                            //should be atEnd
                            for(npv in td.closureItem.nextPrev) {
                                this.addFirstOfInContextAsReferenceToFirstTerminal(td.prev, rpn, td.prev, npv)
                            }
                        }
                        td.postTaskActions.invoke(futureTerminal)
                    })
                }
                td.rulePosition.isAtEnd -> TODO() //this.addParentInContext(td.prev, td.rulePosition.runtimeRule, td.parentRulePosition) //TODO: think this never happens
                else -> {
                    val childPrev = td.prevForChildren
                    val childRulePositions = td.rulePosition.item!!.rulePositionsAt[0]
                    for (childRp in childRulePositions) {
                        val d = Pair(td.closureItem.nextPrev, childRp)
                        if (done.contains(d).not()) {
                            done.add(d)
                            val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                            val needsFollow = childRp.isTerminal //always false here
                            todoList.push(CalculationTask(ClosureItemChild(td.closureItem, childRp), needsFirstOf, needsFollow) { futureTerminal ->
                                this.addFirstTerminalInContext(td.prev, td.rulePosition, futureTerminal)
                                for(rpn in td.rulePosition.next()) {
                                    //should be atEnd
                                    for(npv in td.closureItem.nextPrev) {
                                        this.addFirstOfInContextAsReferenceToFirstTerminal(td.prev, rpn, td.prev, npv)
                                    }
                                }
                                td.postTaskActions.invoke(futureTerminal)
                            })

                            if (td.needsFollow) { // only calc lookahead if needed
                                // if childNextRp is atEnd then lookahead is firstNonEmptyTerminal of parent.next
                                // else lookahead is firstNonEmptyTerminal of childNextRp.next
                                when {
                                    childRp.isAtEnd -> {
                                        val next = td.rulePosition.next()
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
    }


}