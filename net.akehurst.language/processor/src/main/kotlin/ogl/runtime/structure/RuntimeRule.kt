/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.parser.sppt.SPPTNodeDefault

class RuntimeRule(
        val number: Int,
        val name: String,
        val kind: RuntimeRuleKind,
        val isPattern: Boolean,
        val isSkip: Boolean
) {
    // alias for name to use when this is a pattern rule
    val patternText: String = name

    var rhsOpt : RuntimeRuleItem? = null

    val rhs : RuntimeRuleItem = lazy {
        this.rhsOpt ?: throw ParseException("rhs must have a value")
    }.value

    val emptyRule : RuntimeRule get() {
        val er = this.rhs?.items?.get(0) ?: throw ParseException("rhs does not contain any rules")
        if (er.isEmptyRule) {
            return er
        } else {
            throw ParseException("this is not an empty rule")
        }
    }

    val isEmptyRule: Boolean get() {
        return rhs?.kind == RuntimeRuleItemKind.EMPTY
    }

    val isTerminal = this.kind == RuntimeRuleKind.TERMINAL
    val isNonTerminal = this.kind == RuntimeRuleKind.NON_TERMINAL

    val ruleThatIsEmpty: RuntimeRule get() {
        return this.rhs.items[0]
    }

    fun isCompleteChildren(nextItemIndex: Int, numNonSkipChildren:Int, children:List<SPPTNodeDefault>): Boolean {
        return if (RuntimeRuleKind.TERMINAL == this.kind) {
            true
        } else {
            val rhs: RuntimeRuleItem = this.rhs
            when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> true
                RuntimeRuleItemKind.CHOICE_EQUAL ->
                    // a choice can only have one child
                    // TODO: should never be 1, should always be -1 if we create nodes correctly
                    nextItemIndex == 1 || nextItemIndex == -1
                RuntimeRuleItemKind.CHOICE_PRIORITY ->
                    // a choice can only have one child
                    // TODO: should never be 1, should always be -1 if we create nodes correctly
                    nextItemIndex == 1 || nextItemIndex == -1
                RuntimeRuleItemKind.CONCATENATION -> {
                    // the -1 is used when creating dummy ?
                    // test here!
                    rhs.items.size <= nextItemIndex || nextItemIndex == -1
                }
                RuntimeRuleItemKind.MULTI -> {
                    var res = false
                    if (0 == rhs.multiMin && numNonSkipChildren == 1) {
                        // complete if we have an empty node as child
                        res = if (children.isEmpty()) false else children[0].runtimeRule.isEmptyRule
                    }
                    val size = numNonSkipChildren
                    res || size > 0 && size >= rhs.multiMin || nextItemIndex == -1 // the -1 is used when
                    // creating
                    // dummy branch...should
                    // really need the test here!
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    val size = numNonSkipChildren
                    size % 2 == 1 || nextItemIndex == -1 // the -1 is used when creating dummy branch...should really need the test here!
                }
                else -> throw RuntimeException("Internal Error: rule kind not recognised")
            }
        }
    }

    fun canGrowWidth(nextItemIndex: Int, numNonSkipChildren:Int) :Boolean {
        //TODO: other kinds!
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> return false
            RuntimeRuleItemKind.CHOICE_EQUAL -> return nextItemIndex == 0
            RuntimeRuleItemKind.CHOICE_PRIORITY -> return nextItemIndex == 0
            RuntimeRuleItemKind.CONCATENATION -> {
                if (nextItemIndex != -1 && nextItemIndex < this.rhs.items.size) {
                    return true
                } else {
                    return false // !reachedEnd;
                }
            }
            RuntimeRuleItemKind.MULTI -> {
                val size = numNonSkipChildren
                val max = this.rhs.multiMax
                return -1 != size && (-1 == max || size < max)
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                val size = numNonSkipChildren
                val max = this.rhs.multiMax
                val x = size / 2
                return -1 != size && (-1 == max || x < max)
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    fun findTerminalAt(n: Int, runtimeRuleSet:RuntimeRuleSet): Set<RuntimeRule> {
        return if (this.isTerminal) {
            setOf(this);
        } else {
            val firstItems = this.rhs.findItemAt(n).filter { it.isTerminal }.toMutableSet()
            when (this.rhs.kind) {
                RuntimeRuleItemKind.MULTI -> {
                    if (0 == n && 0 == this.rhs.multiMin) {
                        firstItems.add(runtimeRuleSet.findEmptyRule(this))
                    }
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    if (0 == n && 0 == this.rhs.multiMin) {
                        firstItems.add(runtimeRuleSet.findEmptyRule(this))
                    }
                }
                else  -> {} //TODO: L/R-Assoc and unorderd
            }
            return firstItems
        }
    }

    fun findAllNonTerminalAt(n: Int): Set<RuntimeRule> {
        return if (isTerminal) {
            emptySet<RuntimeRule>()
        } else {
            this.rhs.items.filter { it.isTerminal }.toSet()
        }
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        var result = this.findAllNonTerminalAt(n)
        var oldResult= mutableSetOf<RuntimeRule>()
        while (!oldResult.containsAll(result)) {
            oldResult = result.toMutableSet()
            for (nt in oldResult) {
                result += nt.findAllNonTerminalAt(n)
            }
        }
        return result
    }

    fun findNextExpectedItems(nextItemIndex: Int, numNonSkipChildren:Int, runtimeRuleSet: RuntimeRuleSet) : Set<RuntimeRule> {
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> {
                return emptySet<RuntimeRule>()
            }
            RuntimeRuleItemKind.CHOICE_EQUAL -> {
                return if (nextItemIndex == 0) {
                    this.rhs.items.toHashSet()
                } else {
                    emptySet<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.CHOICE_PRIORITY -> {
                return if (nextItemIndex == 0) {
                    this.rhs.items.toHashSet()
                } else {
                    emptySet<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.CONCATENATION -> {
                return if (numNonSkipChildren >= this.rhs.items.size) {
                    throw RuntimeException("Internal Error: No NextExpectedItem")
                } else {
                    if (-1 == nextItemIndex) {
                        emptySet<RuntimeRule>()
                    } else {
                        hashSetOf<RuntimeRule>(this.rhs.items[nextItemIndex])
                    }
                }
            }
            RuntimeRuleItemKind.MULTI -> {
                return when {
                    (0 == numNonSkipChildren && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0], runtimeRuleSet.findEmptyRule(this))
                    (numNonSkipChildren < this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                return when {
                    (numNonSkipChildren % 2 == 1) -> hashSetOf<RuntimeRule>(this.rhs.listSeparator)
                    (0 == numNonSkipChildren && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0], runtimeRuleSet.findEmptyRule(this))
                    (numNonSkipChildren < this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }
}