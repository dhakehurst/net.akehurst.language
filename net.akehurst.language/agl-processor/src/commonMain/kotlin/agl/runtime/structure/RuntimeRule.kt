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

import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.lazyArray
import net.akehurst.language.collections.lazyMapNonNull
import net.akehurst.language.collections.transitiveClosure

class RuntimeRule(
        val runtimeRuleSetNumber: Int,
        val number: Int,
        val tag: String,
        val value: String,
        val kind: RuntimeRuleKind,
        val isPattern: Boolean,
        val isSkip: Boolean,
        val embeddedRuntimeRuleSet: RuntimeRuleSet? = null,
        val embeddedStartRule: RuntimeRule? = null
) {

    //TODO: get rid of this rhsOpt hack!
    var rhsOpt: RuntimeRuleItem? = null

    val rhs: RuntimeRuleItem by lazy {
        this.rhsOpt ?: throw ParserException("rhs must have a value: ${this.tag}")
    }

    val emptyRuleItem: RuntimeRule
        get() {
            return when {
                this.rhs.kind == RuntimeRuleItemKind.MULTI && 0 == this.rhs.multiMin -> this.rhs.MULTI__emptyRule
                this.rhs.kind == RuntimeRuleItemKind.SEPARATED_LIST && 0 == this.rhs.multiMin -> this.rhs.SLIST__emptyRule
                this.rhs.items[0].isEmptyRule -> this.rhs.EMPTY__ruleThatIsEmpty
                else -> throw ParserException("this rule cannot be empty and has no emptyRuleItem")
            }
        }

    val isEmptyRule: Boolean
        get() {
            return this.kind == RuntimeRuleKind.TERMINAL && null != this.rhsOpt && this.rhs.kind == RuntimeRuleItemKind.EMPTY
        }

    internal val patternAtStart = if (this.isPattern) {
        check(this.value.startsWith("^").not(),{"Must not start with ^ in a pattern"})
        check(this.value.endsWith("$").not(),{"Must not end with $ in a pattern"})
        Regex("^${this.value}")
        //regexMatcher(this.value)
    } else {
        null
    }

    val ruleThatIsEmpty: RuntimeRule
        get() {
            return this.rhs.EMPTY__ruleThatIsEmpty
        }

    val numberOfRulePositions: Int by lazy {
        when (this.kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> 0
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> 1
                RuntimeRuleItemKind.CHOICE -> 1
                RuntimeRuleItemKind.CONCATENATION -> rhs.items.size
                RuntimeRuleItemKind.MULTI -> 2
                RuntimeRuleItemKind.SEPARATED_LIST -> 3
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> 3
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> 3
                RuntimeRuleItemKind.UNORDERED -> rhs.items.size
            }
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    val rulePositions: Set<RulePosition> //TODO: make constants where possible for these sets
        get() {
            return when (kind) {
                RuntimeRuleKind.GOAL -> rhs.items.mapIndexed { index, runtimeRule ->
                    RulePosition(this, 0, index)
                }.toSet() + RulePosition(this, 0, RulePosition.END_OF_RULE)
                RuntimeRuleKind.TERMINAL -> setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> setOf(
                            RulePosition(this, 0, RulePosition.START_OF_RULE),
                            RulePosition(this, 0, RulePosition.END_OF_RULE)
                    )
                    RuntimeRuleItemKind.CHOICE -> rhs.items.mapIndexed { index, runtimeRule ->
                        setOf(
                                RulePosition(this, index, RulePosition.START_OF_RULE),
                                RulePosition(this, index, RulePosition.END_OF_RULE)
                        )
                    }.flatMap { it }.toSet()
                    RuntimeRuleItemKind.CONCATENATION -> rhs.items.mapIndexed { index, runtimeRule ->
                        RulePosition(this, 0, index)
                    }.toSet() + RulePosition(this, 0, RulePosition.END_OF_RULE)
                    RuntimeRuleItemKind.UNORDERED -> TODO()
                    RuntimeRuleItemKind.MULTI -> if (0 == rhs.multiMin) {
                        if (rhs.multiMax == 1 || rhs.multiMax == 0) {
                            setOf(
                                    RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                            )
                        } else {
                            setOf(
                                    RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                            )
                        }
                    } else {
                        setOf(
                                RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                                RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                                RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                        )
                    }
                    RuntimeRuleItemKind.SEPARATED_LIST -> if (0 == rhs.multiMin) {
                        if (rhs.multiMax <= 1) { //TODO: doesn't really make sense, make sure we cant have this
                            setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                            )
                        } else {
                            setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                            )
                        }
                    } else {
                        setOf(
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                        )
                    }
                    RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                }
                RuntimeRuleKind.EMBEDDED -> setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
            }
        }

    val rulePositionsAt = lazyMapNonNull<Int, Set<RulePosition>> { index ->
        this.calcExpectedRulePositions(index)
    }

    val itemsAt by lazy {
        lazyArray(numberOfRulePositions) { index ->
            calcItemsAt(index).toTypedArray()
        }
    }

    fun canGrowWidth(nextItemIndex: Int): Boolean {
        // nextItemIndex and numNonskip children are not always the same, especially for multi.
        //TODO: other kinds!
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> return false
            RuntimeRuleItemKind.CHOICE -> return nextItemIndex == 0
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

    fun findTerminalAt(n: Int): Set<RuntimeRule> {
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> setOf(this)
            RuntimeRuleKind.NON_TERMINAL -> {
                val firstItems = this.rhs.findItemAt(n).filter { it.kind == RuntimeRuleKind.TERMINAL }.toMutableSet()
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
//                        TODO()
                    } //TODO: L/R-Assoc and unorderd
                }
                return firstItems
            }
            RuntimeRuleKind.EMBEDDED -> setOf(this) //this.embeddedRuntimeRuleSet!!.firstTerminals[this.embeddedStartRule!!.number]
        }
    }

    fun items(option: Int, position: Int): List<RuntimeRule> { //TODO: do we need to return a set here?
        return when (kind) {
            RuntimeRuleKind.GOAL -> when (position) {
                0 -> listOf(rhs.items[position])
                else -> error("Should not happen")
            }// always a concatenation
            RuntimeRuleKind.TERMINAL -> emptyList()
            RuntimeRuleKind.EMBEDDED -> emptyList() //an embedded grammar is treated like a terminal, matches or not, so no 'items'
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> emptyList()
                RuntimeRuleItemKind.CHOICE -> listOf(rhs.items[option])
                RuntimeRuleItemKind.CONCATENATION -> listOf(rhs.items[position])
                RuntimeRuleItemKind.UNORDERED -> TODO() // will require a multiple items in the set
                RuntimeRuleItemKind.MULTI -> when (option) {
                    RuntimeRuleItem.MULTI__ITEM -> when (position) {
                        RulePosition.START_OF_RULE -> listOf(rhs.items[RuntimeRuleItem.MULTI__ITEM])
                        RulePosition.POSITION_MULIT_ITEM -> listOf(rhs.items[RuntimeRuleItem.MULTI__ITEM])
                        RulePosition.END_OF_RULE -> emptyList()
                        else -> error("Should not happen")
                    }
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when (position) {
                        RulePosition.START_OF_RULE -> listOf(rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE])
                        RulePosition.END_OF_RULE -> emptyList()
                        else -> error("Should not happen")
                    }
                    else -> error("Should not happen")
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when (option) {
                    RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR -> when (position) {
                        RulePosition.START_OF_RULE -> listOf(rhs.items[RuntimeRuleItem.SLIST__ITEM])
                        RulePosition.POSITION_SLIST_ITEM -> listOf(rhs.items[RuntimeRuleItem.SLIST__ITEM])
                        RulePosition.POSITION_SLIST_SEPARATOR -> listOf(rhs.items[RuntimeRuleItem.SLIST__SEPARATOR])
                        RulePosition.END_OF_RULE -> emptyList()
                        else -> error("Should not happen")
                    }
                    RulePosition.OPTION_SLIST_EMPTY -> when (position) {
                        RulePosition.START_OF_RULE -> listOf(rhs.items[RuntimeRuleItem.SLIST__EMPTY_RULE])
                        RulePosition.END_OF_RULE -> emptyList()
                        else -> error("Should not happen")
                    }
                    else -> error("Should not happen")

                }
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
            }
        }
    }

    fun item(option: Int, position: Int): RuntimeRule? {
        return when (kind) {
            RuntimeRuleKind.GOAL -> when (position) {
                0 -> rhs.items[position]
                else -> error("Should not happen")
            }// always a concatenation
            RuntimeRuleKind.TERMINAL -> null
            RuntimeRuleKind.EMBEDDED -> null //an embedded grammar is treated like a terminal, matches or not, so no 'items'
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> null
                RuntimeRuleItemKind.CHOICE -> rhs.items[option]
                RuntimeRuleItemKind.CONCATENATION -> rhs.items[position]
                RuntimeRuleItemKind.UNORDERED -> TODO() // will require a multiple items in the set
                RuntimeRuleItemKind.MULTI -> when (option) {
                    RuntimeRuleItem.MULTI__ITEM -> when (position) {
                        RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.MULTI__ITEM]
                        RulePosition.POSITION_MULIT_ITEM -> rhs.items[RuntimeRuleItem.MULTI__ITEM]
                        RulePosition.END_OF_RULE -> null
                        else -> error("Should not happen")
                    }
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when (position) {
                        RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
                        RulePosition.END_OF_RULE -> null
                        else -> error("Should not happen")
                    }
                    else -> error("Should not happen")
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when (option) {
                    RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR -> when (position) {
                        RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.SLIST__ITEM]
                        RulePosition.POSITION_SLIST_ITEM -> rhs.items[RuntimeRuleItem.SLIST__ITEM]
                        RulePosition.POSITION_SLIST_SEPARATOR -> rhs.items[RuntimeRuleItem.SLIST__SEPARATOR]
                        RulePosition.END_OF_RULE -> null
                        else -> error("Should not happen")
                    }
                    RulePosition.OPTION_SLIST_EMPTY -> when (position) {
                        RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.SLIST__EMPTY_RULE]
                        RulePosition.END_OF_RULE -> null
                        else -> error("Should not happen")
                    }
                    else -> error("Should not happen")

                }
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
            }
        }
    }

    private fun findAllNonTerminalAt(n: Int): Set<RuntimeRule> {
        //TODO: 'ALL' bit !
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> emptySet<RuntimeRule>()
            RuntimeRuleKind.NON_TERMINAL -> {
                when (this.rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> emptySet<RuntimeRule>()
                    RuntimeRuleItemKind.CHOICE -> {
                        if (n == 0) {
                            this.rhs.items.toSet()
                        } else {
                            emptySet<RuntimeRule>()
                        }
                    }
                    RuntimeRuleItemKind.CONCATENATION -> {
                        if (n >= this.rhs.items.size) {
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
                        when {
                            (0 == n && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0])
                            (n < this.rhs.multiMax || -1 == this.rhs.multiMax) -> setOf<RuntimeRule>(this.rhs.items[0])
                            else -> emptySet<RuntimeRule>()
                        }
                    }
                    RuntimeRuleItemKind.SEPARATED_LIST -> {
                        when {
                            (0 == n && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                            (n % 2 == 0) -> if (n < this.rhs.multiMax || -1 == this.rhs.multiMax) {
                                hashSetOf<RuntimeRule>(this.rhs.items[0])
                            } else {
                                emptySet<RuntimeRule>()
                            }
                            else -> emptySet<RuntimeRule>()
                        }
                    }
                    RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.UNORDERED -> TODO()
                }
            }
            RuntimeRuleKind.EMBEDDED -> emptySet<RuntimeRule>()
        }
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        var result = setOf(this).transitiveClosure { it.findAllNonTerminalAt(n) + it.findTerminalAt(n) }
        return result
    }

    fun findHasNextExpectedItem(nextItemIndex: Int): Boolean {
        return when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> false
            RuntimeRuleItemKind.CHOICE -> nextItemIndex == 0
            RuntimeRuleItemKind.CONCATENATION -> {
                !(-1 == nextItemIndex || nextItemIndex >= this.rhs.items.size)
            }
            RuntimeRuleItemKind.MULTI -> {
                !(-1 == nextItemIndex || (-1 != this.rhs.multiMax && nextItemIndex > this.rhs.multiMax))
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                !(-1 == nextItemIndex || (-1 != this.rhs.multiMax && (nextItemIndex / 2) > this.rhs.multiMax))
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
            RuntimeRuleItemKind.CHOICE -> {
                return if (nextItemIndex == 0) {
                    this.rhs.items.toSet()
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
        return when {
            position == RulePosition.END_OF_RULE -> emptySet()
            else -> when (kind) {
                RuntimeRuleKind.GOAL -> TODO()
                RuntimeRuleKind.TERMINAL -> emptySet() //setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.EMBEDDED -> emptySet() //setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (this.rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> {
                        emptySet()
                    }
                    RuntimeRuleItemKind.CHOICE -> {
                        return if (position == 0) {
                            this.rhs.items.mapIndexed { index, runtimeRule -> RulePosition(this, index, RulePosition.START_OF_RULE) }.toSet()
                        } else {
                            emptySet()
                        }
                    }
                    RuntimeRuleItemKind.CONCATENATION -> {
                        return if (position >= this.rhs.items.size) {
                            emptySet()
                        } else {
                            if (position == this.rhs.items.size) {
                                setOf(RulePosition(this, RulePosition.START_OF_RULE, RulePosition.END_OF_RULE))
                            } else {
                                setOf(RulePosition(this, RulePosition.START_OF_RULE, position))
                            }
                        }
                    }
                    RuntimeRuleItemKind.MULTI -> {
                        return when {
                            0 == position -> when {
                                0 == this.rhs.multiMin -> setOf(
                                        RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                                        RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE)
                                )
                                0 < this.rhs.multiMin -> setOf(
                                        RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE)
                                )
                                else -> error("should never happen")
                            }
                            (position < this.rhs.multiMax || -1 == this.rhs.multiMax) -> setOf(
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM)
                            )
                            else -> error("should never happen")// emptySet()
                        }
                    }
                    RuntimeRuleItemKind.SEPARATED_LIST -> {
                        return when {
                            (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR)
                            )
                            0 == position -> when {
                                0 == this.rhs.multiMin -> setOf(
                                        RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                        RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE)
                                )
                                0 < this.rhs.multiMin -> setOf(
                                        RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE)
                                )
                                else -> error("should never happen")
                            }
                            (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || -1 == this.rhs.multiMax)) -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM)
                            )
                            else -> error("should never happen")//emptySet()
                        }
                    }
                    RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    else -> throw RuntimeException("Internal Error: rule kind not recognised")
                }
            }
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
                RuntimeRuleItemKind.CHOICE -> {
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

    fun couldHaveChild(possibleChild: RuntimeRule, atPosition: Int): Boolean {
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> false
            RuntimeRuleKind.NON_TERMINAL -> if (possibleChild.isSkip) {
                true
            } else {
                when (this.rhs.kind) {
                    RuntimeRuleItemKind.EMPTY -> false
                    RuntimeRuleItemKind.CHOICE ->
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
                    RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleItemKind.UNORDERED -> TODO()
                }
            }
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    fun incrementNextItemIndex(currentIndex: Int): Int {
        when (this.rhs.kind) {
            RuntimeRuleItemKind.EMPTY -> return -1
            RuntimeRuleItemKind.CHOICE -> return -1
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
        return (31 * this.runtimeRuleSetNumber) + this.number
    }

    override fun equals(other: Any?): Boolean {
        if (other is RuntimeRule) {
            return this.number == other.number && this.runtimeRuleSetNumber == other.runtimeRuleSetNumber
        } else {
            return false
        }
    }

    override fun toString(): String {
        return "[$number]" + when {
            this.isEmptyRule -> " ($tag)"
            this.kind == RuntimeRuleKind.EMBEDDED -> " ($tag) = EMBEDDED"
            this.kind == RuntimeRuleKind.NON_TERMINAL -> " ($tag) = " + this.rhs
            this.isPattern -> if (this.tag == this.value) "\"${this.value}\"" else "${this.tag}(\"${this.value}\")"
            this === RuntimeRuleSet.END_OF_TEXT -> " <EOT>"
            this === RuntimeRuleSet.USE_PARENT_LOOKAHEAD -> " <UP>"
            else -> if (this.tag == this.value) "'${this.value}'" else "${this.tag}('${this.value}')"
        }
    }
}
