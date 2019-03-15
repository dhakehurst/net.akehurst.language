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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.collections.transitveClosure
import net.akehurst.language.parser.scannerless.InputFromCharSequence
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
        this.rhsOpt ?: throw ParseException("rhs must have a value: ${this}")
    }

    val emptyRuleItem: RuntimeRule
        get() {
            return when {
                this.rhs.kind == RuntimeRuleItemKind.MULTI && 0 == this.rhs.multiMin -> this.rhs.MULTI__emptyRule
                this.rhs.kind == RuntimeRuleItemKind.SEPARATED_LIST && 0 == this.rhs.multiMin -> this.rhs.SLIST__emptyRule
                this.rhs.items[0].isEmptyRule -> this.rhs.EMPTY__ruleThatIsEmpty
                else -> throw ParseException("this rule cannot be empty and has no emptyRuleItem")
            }
        }

    val isEmptyRule: Boolean
        get() {
            return this.isTerminal && null != this.rhsOpt && this.rhs.kind == RuntimeRuleItemKind.EMPTY
        }

    val isTerminal = this.kind == RuntimeRuleKind.TERMINAL
    val isNonTerminal = this.kind == RuntimeRuleKind.NON_TERMINAL
    val isGoal = this.kind == RuntimeRuleKind.GOAL

    val ruleThatIsEmpty: RuntimeRule
        get() {
            return this.rhs.EMPTY__ruleThatIsEmpty
        }

    val numberOfRulePositions: Int by lazy {
        if (isTerminal) {
            0
        } else {
            when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> 1
                RuntimeRuleItemKind.CHOICE_EQUAL -> 1
                RuntimeRuleItemKind.CHOICE_PRIORITY -> 1
                RuntimeRuleItemKind.CONCATENATION -> rhs.items.size
                RuntimeRuleItemKind.MULTI -> 2
                RuntimeRuleItemKind.SEPARATED_LIST -> 3
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> 3
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> 3
                RuntimeRuleItemKind.UNORDERED -> rhs.items.size
            }
        }
    }

    val rulePositions: Set<RulePosition>
        get() {
            return if (isTerminal) {
                setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
            } else {
                when (rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> setOf(
                        RulePosition(this, 0, 0),
                        RulePosition(this, 0, RulePosition.END_OF_RULE)
                    )
                    RuntimeRuleItemKind.CHOICE_EQUAL -> rhs.items.mapIndexed { index, runtimeRule ->
                        setOf(
                            RulePosition(this, index, 0),
                            RulePosition(this, index, RulePosition.END_OF_RULE)
                        )
                    }.flatMap { it }.toSet()
                    RuntimeRuleItemKind.CHOICE_PRIORITY -> rhs.items.mapIndexed { index, runtimeRule ->
                        setOf(
                            RulePosition(this, index, 0),
                            RulePosition(this, index, RulePosition.END_OF_RULE)
                        )
                    }.flatMap { it }.toSet()
                    RuntimeRuleItemKind.CONCATENATION -> rhs.items.mapIndexed { index, runtimeRule ->
                        RulePosition(this, 0, index)
                    }.toSet() + RulePosition(this, 0, RulePosition.END_OF_RULE)
                    RuntimeRuleItemKind.UNORDERED -> TODO()
                    RuntimeRuleItemKind.MULTI -> if (0 == rhs.multiMin) {
                        setOf(
                            RulePosition(this, RuntimeRuleItem.MULTI__EMPTY_RULE, 0),
                            RulePosition(this, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE),
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 0),
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 1),
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
                        )
                    } else {
                        setOf(
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 0),
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 1),
                            RulePosition(this, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
                        )
                    }
                    RuntimeRuleItemKind.SEPARATED_LIST -> if (0 == rhs.multiMin) {
                        setOf(
                            RulePosition(this, RuntimeRuleItem.SLIST__EMPTY_RULE, 0),
                            RulePosition(this, RuntimeRuleItem.SLIST__EMPTY_RULE, RulePosition.END_OF_RULE),
                            RulePosition(this, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 0),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 2),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                    } else {
                        setOf(
                            RulePosition(this, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 0),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 2),
                            RulePosition(this, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                    }
                    RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                }
            }
        }

//    val nextExpectedItems by lazy { lazyArray(rhs.items.size,{
//        this.findNextExpectedItems(it)
//    })}

    val itemsAt by lazy {
        lazyArray(numberOfRulePositions) { index ->
            calcItemsAt(index).toTypedArray()
        }
    }

    /*
        fun isCompleteChildren(nextItemIndex: Int, numNonSkipChildren: Int, children: List<SPPTNode>): Boolean {
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
                        var res = false
                        if (0 == rhs.multiMin && numNonSkipChildren == 1) {
                            // complete if we have an empty node as child
                            res = if (children.isEmpty()) false else children[0].runtimeRule.isEmptyRule
                        }
                        val max = this.rhs.multiMax
                        val x = (nextItemIndex + 1) / 2
                        val inRange = (0 != nextItemIndex && (x >= this.rhs.multiMin && (-1 == max || x <= max)))
                        res || -1 == nextItemIndex || inRange
                        //nextItemIndex % 2 == 1 || nextItemIndex == -1 // the -1 is used when creating dummy branch...should really need the test here!
                    }
                    else -> throw RuntimeException("Internal Error: rule kind not recognised")
                }
            }
        }
    */
    fun canGrowWidth(nextItemIndex: Int): Boolean {
        // nextItemIndex and numNonskip children are not always the same, especially for multi.
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
                val max = this.rhs.multiMax
                return -1 != nextItemIndex && (-1 == max || nextItemIndex < max)
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                val max = this.rhs.multiMax
                val x = nextItemIndex / 2
                return -1 != nextItemIndex && (-1 == max || x < max)
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

    fun findTerminalAt(n: Int): Set<RuntimeRule> {
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

    fun items(choice: Int, position: Int): Set<RuntimeRule> { //TODO: do we need to return a set here?
        return when (rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> emptySet()
            RuntimeRuleItemKind.CHOICE_EQUAL -> setOf(rhs.items[choice])
            RuntimeRuleItemKind.CHOICE_PRIORITY -> setOf(rhs.items[choice])
            RuntimeRuleItemKind.CONCATENATION -> setOf(rhs.items[position])
            RuntimeRuleItemKind.UNORDERED -> TODO() // will require a multiple items in the set
            RuntimeRuleItemKind.MULTI -> setOf(rhs.items[choice])
            RuntimeRuleItemKind.SEPARATED_LIST -> setOf(rhs.items[choice])
            RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
            RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
        }
    }

    private fun findAllNonTerminalAt(n: Int): Set<RuntimeRule> {
        //TODO: 'ALL' bit !
        return if (this.isTerminal) {
            emptySet<RuntimeRule>()
        } else {
            when (this.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> {
                    return emptySet<RuntimeRule>()
                }
                RuntimeRuleItemKind.CHOICE_EQUAL -> {
                    return if (n == 0) {
                        this.rhs.items.toHashSet()
                    } else {
                        emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleItemKind.CHOICE_PRIORITY -> {
                    return if (n == 0) {
                        this.rhs.items.toHashSet()
                    } else {
                        emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleItemKind.CONCATENATION -> {
                    return if (n >= this.rhs.items.size) {
                        throw RuntimeException("Internal Error: No NextExpectedItem")
                    } else {
                        if (-1 == n) {
                            emptySet<RuntimeRule>()
                        } else {
                            var nextItem = this.rhs.items[n]
                            val res = mutableSetOf(nextItem)
                            res
                        }
                    }
                }
                RuntimeRuleItemKind.MULTI -> {
                    return when {
                        (0 == n && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0])
                        (n < this.rhs.multiMax || -1 == this.rhs.multiMax) -> setOf<RuntimeRule>(this.rhs.items[0])
                        else -> emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    return when {
                        (0 == n && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                        (n % 2 == 0) -> if (n < this.rhs.multiMax || -1 == this.rhs.multiMax) {
                            hashSetOf<RuntimeRule>(this.rhs.items[0])
                        } else {
                            emptySet<RuntimeRule>()
                        }
                        else -> emptySet<RuntimeRule>()
                    }
                }
                else -> throw RuntimeException("Internal Error: rule kind not recognised")
            }

        }
    }

    fun findSubRules(): Set<RuntimeRule> {
        /*
        var result = this.findAllNonTerminal()
        var oldResult = mutableSetOf<RuntimeRule>()
        while (!oldResult.containsAll(result)) {
            oldResult = result.toMutableSet()
            for (nt in oldResult) {
                result += nt.findAllNonTerminal()
            }
        }
        return result
        */
        var result = setOf(this).transitveClosure { it.findAllNonTerminal() }
        return result;
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        /*
        var result = this.findAllNonTerminalAt(n)
        var oldResult = mutableSetOf<RuntimeRule>()
        while (!oldResult.containsAll(result)) {
            oldResult = result.toMutableSet()
            for (nt in oldResult) {
                result += nt.findAllNonTerminalAt(n)
            }
        }
        */
        var result = setOf(this).transitveClosure { it.findAllNonTerminalAt(n) + it.findTerminalAt(n) }
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
                return if (-1 == nextItemIndex || (-1 != this.rhs.multiMax && nextItemIndex > this.rhs.multiMax)) {
                    false
                } else {
                    true
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                return if (-1 == nextItemIndex || (-1 != this.rhs.multiMax && (nextItemIndex / 2) > this.rhs.multiMax)) {
                    false
                } else {
                    true
                }
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    //TODO: this should be able to be removed
    fun findNextExpectedItems(nextItemIndex: Int): Set<RuntimeRule> {
        //TODO: would it be faster to return an array here?
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
                return if (nextItemIndex >= this.rhs.items.size) {
                    throw RuntimeException("Internal Error: No NextExpectedItem")
                } else {
                    if (-1 == nextItemIndex) {
                        emptySet<RuntimeRule>()
                    } else {
                        var nextItem = this.rhs.items[nextItemIndex]
                        val res = mutableSetOf(nextItem)
                        //TODO: I don't think we need this....or do we?
                        //var i = nextItemIndex+1
                        //while (nextItem.canBeEmpty(setOf()) && i < this.rhs.items.size) {
                        //    nextItem = this.rhs.items[i]
                        //   res.add(nextItem)
                        //    ++i
                        //}
                        res
                    }
                }
            }
            RuntimeRuleItemKind.MULTI -> {
                return when {
                    (0 == nextItemIndex && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                    (nextItemIndex < this.rhs.multiMax || -1 == this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                return when {
                    (nextItemIndex % 2 == 1 && (((nextItemIndex + 1) / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf<RuntimeRule>(this.rhs.SLIST__separator)
                    (0 == nextItemIndex && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                    (nextItemIndex % 2 == 0 && ((nextItemIndex / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf<RuntimeRule>(this.rhs.items[0])
                    else -> emptySet<RuntimeRule>()
                }
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    internal fun calcExpectedRulePositions(position: Int): Set<RulePosition> {
        return if (position == RulePosition.END_OF_RULE || this.isTerminal) {
            emptySet()
        } else when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> {
                emptySet()
            }
            RuntimeRuleItemKind.CHOICE_EQUAL -> {
                return if (position == 0) {
                    this.rhs.items.mapIndexed { index, runtimeRule -> RulePosition(this, index, 0) }.toSet()
                } else {
                    emptySet()
                }
            }
            RuntimeRuleItemKind.CHOICE_PRIORITY -> {
                return if (position == 0) {
                    this.rhs.items.mapIndexed { index, runtimeRule -> RulePosition(this, index, 0) }.toSet()
                } else {
                    emptySet<RulePosition>()
                }
            }
            RuntimeRuleItemKind.CONCATENATION -> {
                return if (position >= this.rhs.items.size) {
                    throw RuntimeException("Internal Error: No NextExpectedItem")
                } else {
                    if (position == this.rhs.items.size) {
                        setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                    } else {
                        setOf(RulePosition(this, 0, position))
                    }
                }
            }
            RuntimeRuleItemKind.MULTI -> {
                return when {
                    (0 == position && 0 == this.rhs.multiMin) -> setOf(RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 0), RulePosition(this, RuntimeRuleItem.MULTI__EMPTY_RULE, 0))
                    (position < this.rhs.multiMax || -1 == this.rhs.multiMax) -> setOf(RulePosition(this, RuntimeRuleItem.MULTI__ITEM, 0))
                    else -> emptySet<RulePosition>()
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                return when {
                    (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf<RulePosition>(RulePosition(this, RuntimeRuleItem.SLIST__SEPARATOR, 1))
                    (0 == position && 0 == this.rhs.multiMin) -> setOf<RulePosition>(RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 0), RulePosition(this, RuntimeRuleItem.SLIST__EMPTY_RULE, 0))
                    (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf<RulePosition>(RulePosition(this, RuntimeRuleItem.SLIST__ITEM, 0))
                    else -> emptySet<RulePosition>()
                }
            }
            else -> throw RuntimeException("Internal Error: rule kind not recognised")
        }
    }

    internal fun calcItemsAt(position: Int): Set<RuntimeRule> {
        //TODO: would it be faster to return an array here?
        return if (RulePosition.END_OF_RULE == position) {
            emptySet()
        } else {
            when (this.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> {
                    emptySet()
                }
                RuntimeRuleItemKind.CHOICE_EQUAL -> {
                    if (position == 0) {
                        this.rhs.items.toSet()
                    } else {
                        emptySet()
                    }
                }
                RuntimeRuleItemKind.CHOICE_PRIORITY -> {
                    if (position == 0) {
                        this.rhs.items.toSet()
                    } else {
                        emptySet()
                    }
                }
                RuntimeRuleItemKind.CONCATENATION -> {
                    if (position >= this.rhs.items.size) {
                        throw RuntimeException("Internal Error: No NextExpectedItem")
                    } else {
                        var nextItem = this.rhs.items[position]
                        val res = setOf(nextItem)
                        //TODO: I don't think we need this....or do we?
                        //var i = nextItemIndex+1
                        //while (nextItem.canBeEmpty(setOf()) && i < this.rhs.items.size) {
                        //    nextItem = this.rhs.items[i]
                        //   res.add(nextItem)
                        //    ++i
                        //}
                        res
                    }
                }
                RuntimeRuleItemKind.MULTI -> {
                    when {
                        (0 == position && 0 == this.rhs.multiMin) -> setOf(this.rhs.items[0], this.emptyRuleItem)
                        (position < this.rhs.multiMax || -1 == this.rhs.multiMax) -> setOf(this.rhs.items[0])
                        else -> emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    when {
                        (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf(this.rhs.SLIST__separator)
                        (0 == position && 0 == this.rhs.multiMin) -> setOf(this.rhs.items[0], this.emptyRuleItem)
                        (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf(this.rhs.items[0])
                        else -> emptySet()
                    }
                }
                else -> throw RuntimeException("Internal Error: rule kind not recognised")
            }
        }
    }

    private fun canBeEmpty(checked: Set<RuntimeRule>): Boolean {
        if (this.isTerminal) {
            return this.isEmptyRule;
        } else {
            val newchecked = checked.toMutableSet()
            newchecked.add(this)
            when (this.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> return true
                RuntimeRuleItemKind.CHOICE_EQUAL -> return this.rhs.items.any { newchecked.contains(it) || it.canBeEmpty(newchecked) }
                RuntimeRuleItemKind.CHOICE_PRIORITY -> return this.rhs.items.any { newchecked.contains(it) || it.canBeEmpty(newchecked) }
                RuntimeRuleItemKind.CONCATENATION -> return this.rhs.items.all { newchecked.contains(it) || it.canBeEmpty(newchecked) }
                RuntimeRuleItemKind.MULTI -> return 0 == this.rhs.multiMin
                RuntimeRuleItemKind.SEPARATED_LIST -> return 0 == this.rhs.multiMin

                else -> throw RuntimeException("Internal Error: Unknown RuleKind " + this.rhs.kind)
            }
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
                    RuntimeRuleItemKind.CHOICE_PRIORITY ->
                        // TODO: cache this
                        0 == atPosition && this.rhs.items.contains(possibleChild)
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
                                this.rhs.SLIST__separator.number == possibleChild.number
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
//            RuntimeRuleItemKind.MULTI -> return  currentIndex
            RuntimeRuleItemKind.MULTI -> return if (-1 == this.rhs.multiMax || this.rhs.multiMax > currentIndex + 1) currentIndex + 1 else -1
//            RuntimeRuleItemKind.SEPARATED_LIST -> return if (currentIndex == 0) 1 else 0
            RuntimeRuleItemKind.SEPARATED_LIST -> return if (-1 == this.rhs.multiMax || this.rhs.multiMax > (currentIndex / 2)) currentIndex + 1 else -1

            else -> throw RuntimeException("Internal Error: Unknown RuleKind " + this.rhs.kind)
        }
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.number
    }

    override fun equals(other: Any?): Boolean {
        if (other is RuntimeRule) {
            return this.number == other.number
        } else {
            return false
        }
    }

    override fun toString(): String {
        return "[$number]" + when {
            this.isEmptyRule -> " ($name)"
            this.isNonTerminal -> " ($name) = " + this.rhs
            this.isPattern -> " \"" + this.patternText + "\""
            this.name == InputFromCharSequence.END_OF_TEXT -> " <EOT>"
            else -> " '" + this.patternText + "'"
        }
    }
}
