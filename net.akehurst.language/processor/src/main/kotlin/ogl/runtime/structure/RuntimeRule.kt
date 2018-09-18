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

    var rhsOpt: RuntimeRuleItem? = null

    val rhs: RuntimeRuleItem by lazy {
        this.rhsOpt ?: throw ParseException("rhs must have a value")
    }

    val emptyRuleItem: RuntimeRule
        get() {
            return when {
                this.rhs.kind==RuntimeRuleItemKind.MULTI && 0==this.rhs.multiMin -> this.rhs.items[1]
                this.rhs.kind==RuntimeRuleItemKind.SEPARATED_LIST && 0==this.rhs.multiMin -> this.rhs.items[2]
                this.rhs.items[0].isEmptyRule -> this.rhs.items[0]
                else -> throw ParseException("this rule cannot be empty and has no emptyRuleItem")
            }
        }

    val isEmptyRule: Boolean
        get() {
            return this.isTerminal && null!=this.rhsOpt && this.rhs.kind == RuntimeRuleItemKind.EMPTY
        }

    val isTerminal = this.kind == RuntimeRuleKind.TERMINAL
    val isNonTerminal = this.kind == RuntimeRuleKind.NON_TERMINAL

    val ruleThatIsEmpty: RuntimeRule
        get() {
            return this.rhs.items[0]
        }

    fun isCompleteChildren(nextItemIndex: Int, numNonSkipChildren: Int, children: List<SPPTNodeDefault>): Boolean {
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

    fun canGrowWidth(nextItemIndex: Int, numNonSkipChildren: Int): Boolean {
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

    fun findAllTerminal(): Set<RuntimeRule> {
        val result = HashSet<RuntimeRule>()
        if (this.isTerminal) {
            return result
        }
        for (item in this.rhs.items) {
            if (item.isTerminal) {
                result.add(item)
            }
        }
        when (this.rhs.kind) {
            RuntimeRuleItemKind.MULTI -> {
                if (0 == this.rhs.multiMin) {
                    result.add(this.emptyRuleItem)
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                if (0 == this.rhs.multiMin) {
                    result.add(this.emptyRuleItem)
                }
            }
            else -> emptySet<RuntimeRule>()
        }
        return result
    }

    private fun findAllNonTerminal(): Set<RuntimeRule> {
        val result = HashSet<RuntimeRule>()
        if (this.isTerminal) {
            return result
        }
        for (item in this.rhs.items) {
            if (item.isNonTerminal) {
                result.add(item)
            }
        }
        return result
    }

    fun findTerminalAt(n: Int, runtimeRuleSet: RuntimeRuleSet): Set<RuntimeRule> {
        return if (this.isTerminal) {
            setOf(this);
        } else {
            val firstItems = this.rhs.findItemAt(n).filter { it.isTerminal }.toMutableSet()
            when (this.rhs.kind) {
                RuntimeRuleItemKind.MULTI -> {
                    if (0 == n && 0 == this.rhs.multiMin) {
                        firstItems.add(this.emptyRuleItem)
                    }
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    if (0 == n && 0 == this.rhs.multiMin) {
                        firstItems.add(this.emptyRuleItem)
                    }
                }
                else -> {
                } //TODO: L/R-Assoc and unorderd
            }
            return firstItems
        }
    }

    private fun findAllNonTerminalAt(n: Int): Set<RuntimeRule> {
        return if (isTerminal) {
            emptySet<RuntimeRule>()
        } else {
            this.rhs.items.filter { it.isNonTerminal }.toSet()
        }
    }

    fun findSubRules(): Set<RuntimeRule> {
        var result = this.findAllNonTerminal()
        var oldResult = mutableSetOf<RuntimeRule>()
        while (!oldResult.containsAll(result)) {
            oldResult = result.toMutableSet()
            for (nt in oldResult) {
                result += nt.findAllNonTerminal()
            }
        }
        return result
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        var result = this.findAllNonTerminalAt(n)
        var oldResult = mutableSetOf<RuntimeRule>()
        while (!oldResult.containsAll(result)) {
            oldResult = result.toMutableSet()
            for (nt in oldResult) {
                result += nt.findAllNonTerminalAt(n)
            }
        }
        return result
    }

    fun findHasNextExpectedItem(nextItemIndex: Int): Boolean {
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> return false
            RuntimeRuleItemKind.CHOICE_EQUAL -> return nextItemIndex == 0
            RuntimeRuleItemKind.CHOICE_PRIORITY -> return nextItemIndex == 0
            RuntimeRuleItemKind.CONCATENATION -> {
                return if (-1 == nextItemIndex || nextItemIndex >= this.rhs.items.size) {
                    false
                } else {
                    true
                }
            }
            RuntimeRuleItemKind.MULTI -> {
                return if (-1 == nextItemIndex) {
                    false
                } else {
                    true
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> return if (-1 == nextItemIndex) {
                false
            } else {
                true
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    fun findNextExpectedItems(nextItemIndex: Int, numNonSkipChildren: Int): Set<RuntimeRule> {
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
                    (0 == numNonSkipChildren && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                    (numNonSkipChildren < this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                return when {
                    (numNonSkipChildren % 2 == 1) -> hashSetOf<RuntimeRule>(this.rhs.listSeparator)
                    (0 == numNonSkipChildren && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                    (numNonSkipChildren < this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    fun couldHaveChild(possibleChild: RuntimeRule, atPosition: Int): Boolean {
        return if (this.isTerminal) {
            false
        } else {
            if (possibleChild.isSkip) {
                true
            } else {
                when (this.rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> false
                    RuntimeRuleItemKind.CHOICE_EQUAL ->
                        // TODO: cache this
                        0 == atPosition && this.rhs.items.contains(possibleChild)
                    RuntimeRuleItemKind.CHOICE_PRIORITY -> 0 == atPosition && this.rhs.items.contains(possibleChild)
                    RuntimeRuleItemKind.CONCATENATION -> {
                        if (-1 == atPosition || atPosition >= this.rhs.items.size)
                            false
                        else
                            this.rhs.items[atPosition].number == possibleChild.number
                    }
                    RuntimeRuleItemKind.MULTI -> this.rhs.items[0].number == possibleChild.number || (this.rhs.multiMin == 0 && atPosition == 0
                            && possibleChild.isEmptyRule && possibleChild.ruleThatIsEmpty.number == this.number)
                    RuntimeRuleItemKind.SEPARATED_LIST -> {
                        if (possibleChild.isEmptyRule) {
                            this.rhs.multiMin == 0 && possibleChild.ruleThatIsEmpty.number == this.number
                        } else {
                            if (atPosition % 2 == 0) {
                                this.rhs.items[0].number == possibleChild.number
                            } else {
                                this.rhs.listSeparator.number == possibleChild.number
                            }
                        }
                    }
                    else -> throw RuntimeException("Internal Error: rule kind not recognised " + this.rhs.kind)
                }
            }
        }
    }

    fun incrementNextItemIndex(currentIndex: Int): Int {
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> return -1
            RuntimeRuleItemKind.CHOICE_EQUAL -> return -1
            RuntimeRuleItemKind.CHOICE_PRIORITY -> return -1
            RuntimeRuleItemKind.CONCATENATION -> return if (this.rhs.items.size == currentIndex + 1) -1 else currentIndex + 1
            RuntimeRuleItemKind.MULTI -> return currentIndex
            RuntimeRuleItemKind.SEPARATED_LIST -> return if (currentIndex == 0) 1 else 0

            else -> throw RuntimeException("Internal Error: Unknown RuleKind " + this.rhs.kind)
        }
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.number
    }

    override fun equals(arg: Any?): Boolean {
        if (arg is RuntimeRule) {
            val other = arg as RuntimeRule?
            return this.number == other?.number
        } else {
            return false
        }
    }

    override fun toString(): String {
        return "[" + this.number + "]" + if (this.isNonTerminal) {
            " (" + this.name + ") : " + this.rhs
        } else if (this.isPattern) {
            " \"" + this.patternText + "\""
        } else {
            " '" + this.patternText + "'"
        }
    }

}