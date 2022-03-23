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
import net.akehurst.language.collections.LazyMapNonNull
import net.akehurst.language.collections.lazyMapNonNull
import net.akehurst.language.collections.mutableStackOf

internal class FirstFollowCache(val stateSet: ParserStateSet) {

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _followInContext = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    fun containsFirstTerminal(prev: RulePosition, rulePosition: RulePosition): Boolean = this._firstTerminal[prev].containsKey(rulePosition)

    fun firstTerminal(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return if (this._firstTerminal.containsKey(prev).not() || this._firstTerminal[prev].containsKey(rulePosition).not()) {
            this._firstTerminal[prev][rulePosition]
        } else {
            this.calcFirstAndFollowFor(prev, rulePosition)
            this._firstTerminal[prev][rulePosition]
        }
    }

    fun firstOfInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return this._firstOfInContext[prev][rulePosition]
    }

    fun followInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return this._followInContext[prev][rulePosition]
    }

    fun addFirstTerminalInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstTerminal[prev][rulePosition].add(terminal)
    }

    fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    fun addFollowInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._followInContext[prev][rulePosition].add(terminal)
    }

    /**
     * traverse down the closure of rulePosition.
     * record the firstTerminal and the lookahead of each rulePosition in the closure
     *
     * only call this if certain need to calc FirstAndFollow.
     * can check if needed by testing _firstTerminal[rulePosition]==null
     */
    private fun calcFirstAndFollowFor(prev: RulePosition, rulePosition: RulePosition) {
        data class Task(
            val parent: Task?,
            val prev: RulePosition,
            val rulePosition: RulePosition,
            val needsFirstOf: Boolean, // pass as arg so we limit what is calculated
            val needsFollow: Boolean, // pass as arg so we limit what is calculated
            val future: (futureTerminal: RuntimeRule) -> Unit = { _ -> }
        ) {
            val prevForChildren = when {
                rulePosition.isAtStart -> prev
                else -> rulePosition
            }
        }

        when {
            this.containsFirstTerminal(prev, rulePosition) -> Unit //already calculated, don't repeat
            else -> {
                //handle special case for Goal
                when {
                    rulePosition.isGoal && rulePosition.isAtStart -> {
                        val goalEndRp = this.stateSet.endState.rulePositions.first()
                        this.addFirstTerminalInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        this.addFirstOfInContext(prev, goalEndRp, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                        this.addFollowInContext(prev, rulePosition, RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    }
                    else -> Unit
                }

                val done = mutableSetOf<Pair<RulePosition, RulePosition>>() // Pair(prev,rulePos)
                val todoList = mutableStackOf<Task>()
                // handle rulePosition first as it has no parent defined
                when {
                    rulePosition.isTerminal -> Unit // do nothing
                    rulePosition.item!!.isTerminal -> this.addFirstTerminalInContext(prev, rulePosition, rulePosition.item!!)
                    else -> {
                        val childPrev = prev
                        val childRulePositions = rulePosition.item!!.rulePositionsAt[0]
                        for (childRp in childRulePositions) {
                            val d = Pair(childPrev, childRp)
                            done.add(d)
                            val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                            val needsFollow = childRp.isTerminal //always false here
                            todoList.push(Task(null, childPrev, childRp, needsFirstOf, needsFollow) { futureTerminal ->
                                this.addFirstTerminalInContext(prev, rulePosition, futureTerminal)
                            })
                        }
                    }
                }

                // handle rest of Tasks
                done.add(Pair(prev, rulePosition))
                while (todoList.isNotEmpty) {
                    val td = todoList.pop()
                    when { //maybe check if already calc'd
                        td.rulePosition.isTerminal -> {
                            this.addFirstTerminalInContext(td.prev, td.parent!!.rulePosition, td.rulePosition.runtimeRule)
                            td.future.invoke(td.rulePosition.runtimeRule) // triggers setting firstTerminal up the closure
                            // follow(X) = firstOf(X.parent.next)
                            for(pn in td.parent.parent.rulePosition.next()) {
                                this.addFollowInContextToFirstOf(td.prev, td.rulePosition, td.prev, pn) // check target prev
                            }
                        }
                        td.rulePosition.item!!.isTerminal -> {
                            this.addFirstTerminalInContext(td.prev, td.rulePosition, td.rulePosition.item!!)
                            td.future.invoke(td.rulePosition.item!!)// triggers setting firstTerminal up the closure
                            // follow(X) = firstOf(X.parent.next)
                            for(pn in td.parent.rulePosition.next()) {
                                this.addFollowInContextToFirstOf(td.prev, td.rulePosition, td.prev, pn) // check target prev
                            }
                        }
                        else -> {
                            val childPrev = td.prevForChildren
                            val childRulePositions = td.rulePosition.item!!.rulePositionsAt[0]
                            for (childRp in childRulePositions) {
                                val d = Pair(childPrev, childRp)
                                if (done.contains(d).not()) {
                                    done.add(d)
                                    val needsFirstOf = childRp.isAtStart.not() && childRp.isTerminal.not()
                                    val needsFollow = childRp.isTerminal //always false here
                                    todoList.push(Task(td, childPrev, childRp,needsFirstOf,needsFollow) { futureTerminal ->
                                        this.addFirstTerminalInContext(td.prev, td.rulePosition, futureTerminal)
                                        td.future.invoke(futureTerminal)
                                    })

                                    if (td.needsFollow) { // only calc lookahead if needed
                                        // if childNextRp is atEnd then lookahead is firstNonEmptyTerminal of parent.next
                                        // else lookahead is firstNonEmptyTerminal of childNextRp.next
                                        when {
                                            childRp.isAtEnd -> {
                                                val next = td.rulePosition.next()
                                                next.forEach { todoList.push(Task(td, childPrev,  it, true, false)) }
                                            }
                                            else -> {
                                                val next = childRp.next()
                                                next.forEach { todoList.push(Task(td, childPrev,  it, true, false)) }
                                            }
                                        }
                                    } else {
                                        //do nothing
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

}