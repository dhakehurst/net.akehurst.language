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

import net.akehurst.language.agl.regex.asRegexLiteral
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.lazyArray
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.transitiveClosure

/**
 * identified by: (runtimeRuleSetNumber, number)
 */
internal class RuntimeRule(
    val runtimeRuleSetNumber: Int,
    val number: Int,
    val name: String?,
    val value: String,
    val kind: RuntimeRuleKind,
    val isPattern: Boolean,
    val isSkip: Boolean,
    val embeddedRuntimeRuleSet: RuntimeRuleSet? = null,
    val embeddedStartRule: RuntimeRule? = null
) {
    companion object {
        const val MULTIPLICITY_N = -1
    }

    //TODO: neeeds properties:
    // isUnnamedLiteral - so we can eliminate from AsmSimple
    // isGenerated - also w.r.t. AsmSimple so we know if we should try and get a property name from the elements
    // not sure if I really want to add the data to this class as only used for AsmSimple not runtime use?

    val isExplicitlyNamed: Boolean get() = this.name!=null
    val tag:String = this.name?:if (this.isPattern) "\"$value\"" else "'$value'"

    //TODO: get rid of this rhsOpt hack!
    var rhsOpt: RuntimeRuleItem? = null

    val rhs: RuntimeRuleItem by lazy {
        this.rhsOpt ?: throw ParserException("rhs must have a value: ${this.tag}")
    }

    val emptyRuleItem: RuntimeRule
        get() {
            return when {
                this.rhs.itemsKind == RuntimeRuleRhsItemsKind.LIST -> when {
                    this.rhs.listKind == RuntimeRuleListKind.MULTI && 0 == this.rhs.multiMin -> this.rhs.MULTI__emptyRule
                    this.rhs.listKind == RuntimeRuleListKind.SEPARATED_LIST && 0 == this.rhs.multiMin -> this.rhs.SLIST__emptyRule
                    else -> error("unsupported")
                }
                this.rhs.items[0].isEmptyRule -> this.rhs.EMPTY__ruleThatIsEmpty
                else -> throw ParserException("this rule cannot be empty and has no emptyRuleItem")
            }
        }

    val isGoal get() = this.kind == RuntimeRuleKind.GOAL
    val isEmptyRule get() = this.kind == RuntimeRuleKind.TERMINAL && null != this.rhsOpt && this.rhs.itemsKind == RuntimeRuleRhsItemsKind.EMPTY
    val isTerminal get() = this.kind == RuntimeRuleKind.TERMINAL
    val isEmbedded get() = this.kind == RuntimeRuleKind.EMBEDDED
    val isTerminalOrEmbedded get() = this.isTerminal || this.isEmbedded
    val isNonTerminal get() = this.kind == RuntimeRuleKind.NON_TERMINAL && null != this.rhsOpt
    val isChoice get()  = isNonTerminal && this.rhs.itemsKind == RuntimeRuleRhsItemsKind.CHOICE
    val isChoiceLongest get() = this.isChoice && this.rhs.choiceKind == RuntimeRuleChoiceKind.LONGEST_PRIORITY
    val isChoicePriority get() = this.isChoice && this.rhs.choiceKind == RuntimeRuleChoiceKind.PRIORITY_LONGEST
    val isChoiceAmbiguous get() = this.isChoice && this.rhs.choiceKind == RuntimeRuleChoiceKind.AMBIGUOUS

    internal val regex by lazy {
        if (this.isPattern) {
            Regex(this.value)
        } else {
            this.value.asRegexLiteral()
        }
    }

    val ruleThatIsEmpty: RuntimeRule
        get() {
            return this.rhs.EMPTY__ruleThatIsEmpty
        }

    val numberOfRulePositions: Int by lazy {
        when (this.kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> 0
            RuntimeRuleKind.NON_TERMINAL -> when (this.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> 1
                RuntimeRuleRhsItemsKind.CHOICE -> 1
                RuntimeRuleRhsItemsKind.CONCATENATION -> rhs.items.size
                RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                    RuntimeRuleListKind.NONE -> error("should not happen")
                    RuntimeRuleListKind.MULTI -> 2
                    RuntimeRuleListKind.SEPARATED_LIST -> 3
                    RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> 3
                    RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> 3
                    RuntimeRuleListKind.UNORDERED -> rhs.items.size
                }
            }
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    val asTerminalRulePosition by lazy{ RulePosition(this, 0, RulePosition.END_OF_RULE) }

    val rulePositions: Set<RulePosition> //TODO: make constants where possible for these sets
        get() {
            return when (kind) {
                RuntimeRuleKind.GOAL -> rhs.items.mapIndexed { index, _ ->
                    RulePosition(this, 0, index)
                }.toSet() + RulePosition(this, 0, RulePosition.END_OF_RULE)
                RuntimeRuleKind.TERMINAL -> setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> setOf(
                        RulePosition(this, 0, RulePosition.START_OF_RULE),
                        RulePosition(this, 0, RulePosition.END_OF_RULE)
                    )
                    RuntimeRuleRhsItemsKind.CHOICE -> rhs.items.mapIndexed { index, _ ->
                        setOf(
                            RulePosition(this, index, RulePosition.START_OF_RULE),
                            RulePosition(this, index, RulePosition.END_OF_RULE)
                        )
                    }.flatMap { it }.toSet()
                    RuntimeRuleRhsItemsKind.CONCATENATION -> rhs.items.mapIndexed { index, _ ->
                        RulePosition(this, 0, index)
                    }.toSet() + RulePosition(this, 0, RulePosition.END_OF_RULE)
                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> if (0 == rhs.multiMin) {
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
                        RuntimeRuleListKind.SEPARATED_LIST -> when(this.rhs.multiMin) {
                            0 -> when(this.rhs.multiMax) {
                                MULTIPLICITY_N -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                0 -> error("Multiplicity of 0..0 is meaningless")
                                1 -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                2 -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                else -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                            }
                            else -> when(this.rhs.multiMax) {
                                MULTIPLICITY_N -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                0 -> error("Multiplicity of X..0 is meaningless")
                                1 -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                2 -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                                else -> setOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                                )
                            }
                        }
                        RuntimeRuleListKind.UNORDERED -> TODO()
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    }
                }
                RuntimeRuleKind.EMBEDDED -> setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
            }
        }

    val rulePositionsAt = lazyMutableMapNonNull<Int, List<RulePosition>> { index ->
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
        when (this.rhs.itemsKind) {
            RuntimeRuleRhsItemsKind.EMPTY -> return false
            RuntimeRuleRhsItemsKind.CHOICE -> return nextItemIndex == 0
            RuntimeRuleRhsItemsKind.CONCATENATION -> {
                if (nextItemIndex != MULTIPLICITY_N && nextItemIndex < this.rhs.items.size) {
                    return true
                } else {
                    return false // !reachedEnd;
                }
            }
            RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                RuntimeRuleListKind.MULTI -> {
                    val max = this.rhs.multiMax
                    return MULTIPLICITY_N != nextItemIndex && (MULTIPLICITY_N == max || nextItemIndex < max)
                }
                RuntimeRuleListKind.SEPARATED_LIST -> {
                    val max = this.rhs.multiMax
                    val x = nextItemIndex / 2
                    return MULTIPLICITY_N != nextItemIndex && (MULTIPLICITY_N == max || x < max)
                }
                else -> TODO()
            }
        }
    }

    fun findTerminalAt(n: Int): Set<RuntimeRule> {
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> setOf(this)
            RuntimeRuleKind.EMBEDDED -> setOf(this) //this.embeddedRuntimeRuleSet!!.firstTerminals[this.embeddedStartRule!!.number]
            RuntimeRuleKind.NON_TERMINAL -> {
                val firstItems = this.rhs.findItemAt(n).filter { it.kind == RuntimeRuleKind.TERMINAL }.toMutableSet()
                when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.LIST -> when(this.rhs.listKind) {
                        RuntimeRuleListKind.MULTI -> {
                            if (0 == n && 0 == this.rhs.multiMin) {
                                firstItems.add(this.emptyRuleItem)
                            }
                        }
                        RuntimeRuleListKind.SEPARATED_LIST -> {
                            if (0 == n && 0 == this.rhs.multiMin) {
                                firstItems.add(this.emptyRuleItem)
                            }
                        }
                        else ->  TODO()//TODO: L/R-Assoc and unorderd
                    }
                    else -> TODO()
                }
                return firstItems
            }
        }
    }
/*
    fun items(option: Int, position: Int): List<RuntimeRule> { //TODO: do we need to return a set here?
        return when (kind) {
            RuntimeRuleKind.GOAL -> when (position) {
                0 -> listOf(rhs.items[position])
                else -> error("Should not happen")
            }// always a concatenation
            RuntimeRuleKind.TERMINAL -> emptyList()
            RuntimeRuleKind.EMBEDDED -> emptyList() //an embedded grammar is treated like a terminal, matches or not, so no 'items'
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> emptyList()
                RuntimeRuleRhsItemsKind.CHOICE -> listOf(rhs.items[option])
                RuntimeRuleRhsItemsKind.CONCATENATION -> listOf(rhs.items[position])
                RuntimeRuleRhsItemsKind.UNORDERED -> TODO() // will require a multiple items in the set
                RuntimeRuleRhsItemsKind.MULTI -> when (option) {
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
                RuntimeRuleRhsItemsKind.SEPARATED_LIST -> when (option) {
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
                RuntimeRuleRhsItemsKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleRhsItemsKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
            }
        }
    }
    */

    fun item(option: Int, position: Int): RuntimeRule? {
        return when (kind) {
            RuntimeRuleKind.GOAL -> when (position) {
                0 -> rhs.items[position]
                else -> error("Should not happen")
            }// always a concatenation
            RuntimeRuleKind.TERMINAL -> null
            RuntimeRuleKind.EMBEDDED -> null //an embedded grammar is treated like a terminal, matches or not, so no 'items'
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> null
                RuntimeRuleRhsItemsKind.CHOICE -> rhs.items[option]
                RuntimeRuleRhsItemsKind.CONCATENATION -> when {
                    RulePosition.END_OF_RULE==position -> null
                    rhs.items.size > position -> rhs.items[position]
                    else -> null
                }
                RuntimeRuleRhsItemsKind.LIST -> when (rhs.listKind) {
                    RuntimeRuleListKind.NONE -> error("")
                    RuntimeRuleListKind.UNORDERED -> TODO() // will require a multiple items in the set
                    RuntimeRuleListKind.MULTI -> when (option) {
                        RuntimeRuleItem.MULTI__ITEM -> when (position) {
                            RulePosition.END_OF_RULE -> null
                            RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.MULTI__ITEM]
                            RulePosition.POSITION_MULIT_ITEM -> rhs.items[RuntimeRuleItem.MULTI__ITEM]
                            else -> when {
                                position >= rhs.multiMin && position <= rhs.multiMax -> rhs.items[RuntimeRuleItem.MULTI__ITEM]
                                else -> null
                            }
                        }
                        RuntimeRuleItem.MULTI__EMPTY_RULE -> when (position) {
                            RulePosition.START_OF_RULE -> rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
                            RulePosition.END_OF_RULE -> null
                            else -> error("Should not happen")
                        }
                        else -> error("Should not happen")
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> when (option) {
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
                    RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                }
            }
        }
    }

    private fun findAllNonTerminalAt(n: Int): Set<RuntimeRule> {
        //TODO: 'ALL' bit !
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> emptySet<RuntimeRule>()
            RuntimeRuleKind.NON_TERMINAL -> {
                when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> emptySet<RuntimeRule>()
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        if (n == 0) {
                            this.rhs.items.toSet()
                        } else {
                            emptySet<RuntimeRule>()
                        }
                    }
                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        if (n >= this.rhs.items.size) {
                            throw RuntimeException("Internal Error: No NextExpectedItem")
                        } else {
                            if (MULTIPLICITY_N == n) {
                                emptySet<RuntimeRule>()
                            } else {
                                var nextItem = this.rhs.items[n]
                                val res = mutableSetOf(nextItem)
                                res
                            }
                        }
                    }
                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> {
                            when {
                                (0 == n && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0])
                                (n < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> setOf<RuntimeRule>(this.rhs.items[0])
                                else -> emptySet<RuntimeRule>()
                            }
                        }
                        RuntimeRuleListKind.SEPARATED_LIST -> {
                            when {
                                (0 == n && 0 == this.rhs.multiMin) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                                (n % 2 == 0) -> if (n < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) {
                                    hashSetOf<RuntimeRule>(this.rhs.items[0])
                                } else {
                                    emptySet<RuntimeRule>()
                                }
                                else -> emptySet<RuntimeRule>()
                            }
                        }
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.UNORDERED -> TODO()
                    }
                }
            }
            RuntimeRuleKind.EMBEDDED -> emptySet<RuntimeRule>()
        }
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        var result = setOf(this).transitiveClosure { it.findAllNonTerminalAt(n) + it.findTerminalAt(n) }
        return result
    }

    /*
        fun findHasNextExpectedItem(nextItemIndex: Int): Boolean {
            return when (this.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> false
                RuntimeRuleRhsItemsKind.CHOICE -> nextItemIndex == 0
                RuntimeRuleRhsItemsKind.CONCATENATION -> {
                    !(MULTIPLICITY_N == nextItemIndex || nextItemIndex >= this.rhs.items.size)
                }
                RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                    RuntimeRuleListKind.NONE -> error("should not happen")
                    RuntimeRuleListKind.MULTI -> {
                        !(MULTIPLICITY_N == nextItemIndex || (MULTIPLICITY_N != this.rhs.multiMax && nextItemIndex > this.rhs.multiMax))
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> {
                        !(MULTIPLICITY_N == nextItemIndex || (MULTIPLICITY_N != this.rhs.multiMax && (nextItemIndex / 2) > this.rhs.multiMax))
                    }
                    else -> TODO()
                }
            }
        }

        //TODO: this should be able to be removed
        fun findNextExpectedItems(nextItemIndex: Int): Set<RuntimeRule> {
            //TODO: would it be faster to return an array here?
            when (this.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> {
                    return emptySet<RuntimeRule>()
                }
                RuntimeRuleRhsItemsKind.CHOICE -> {
                    return if (nextItemIndex == 0) {
                        this.rhs.items.toSet()
                    } else {
                        emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleRhsItemsKind.CONCATENATION -> {
                    return if (nextItemIndex >= this.rhs.items.size) {
                        throw RuntimeException("Internal Error: No NextExpectedItem")
                    } else {
                        if (MULTIPLICITY_N == nextItemIndex) {
                            emptySet<RuntimeRule>()
                        } else {
                            var nextItem = this.rhs.items[nextItemIndex]
                            val res = mutableSetOf(nextItem)
                            res
                        }
                    }
                }
                RuntimeRuleRhsItemsKind.MULTI -> {
                    return when {
                        (0 == nextItemIndex && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                        (nextItemIndex < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> hashSetOf<RuntimeRule>(this.rhs.items[0])
                        else -> emptySet<RuntimeRule>()
                    }
                }
                RuntimeRuleRhsItemsKind.SEPARATED_LIST -> {
                    return when {
                        (nextItemIndex % 2 == 1 && (((nextItemIndex + 1) / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> setOf<RuntimeRule>(this.rhs.SLIST__separator)
                        (0 == nextItemIndex && 0 == this.rhs.multiMin) -> setOf<RuntimeRule>(this.rhs.items[0], this.emptyRuleItem)
                        (nextItemIndex % 2 == 0 && ((nextItemIndex / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> setOf<RuntimeRule>(this.rhs.items[0])
                        else -> emptySet<RuntimeRule>()
                    }
                }
                else -> throw RuntimeException("Internal Error: rule kind not recognised")
            }
        }
    */
    internal fun calcExpectedRulePositions(position: Int): List<RulePosition> {
        return when {
            position == RulePosition.END_OF_RULE -> emptyList()
            else -> when (kind) {
                RuntimeRuleKind.GOAL -> TODO()
                RuntimeRuleKind.TERMINAL -> emptyList() //setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.EMBEDDED -> emptyList() //setOf(RulePosition(this, 0, RulePosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> {
                        emptyList()
                    }
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        return if (position == 0) {
                            this.rhs.items.mapIndexed { index, _ -> RulePosition(this, index, RulePosition.START_OF_RULE) }
                        } else {
                            emptyList()
                        }
                    }
                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        return if (position >= this.rhs.items.size) {
                            emptyList()
                        } else {
                            if (position == this.rhs.items.size) {
                                listOf(RulePosition(this, RulePosition.START_OF_RULE, RulePosition.END_OF_RULE))
                            } else {
                                listOf(RulePosition(this, RulePosition.START_OF_RULE, position))
                            }
                        }
                    }
                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> when {
                            0 == position -> when {
                                0 == this.rhs.multiMin -> listOf(
                                    RulePosition(this, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                                )
                                0 < this.rhs.multiMin -> listOf(
                                    RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE)
                                )
                                else -> error("should never happen")
                            }
                            (position < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> listOf(
                                RulePosition(this, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM)
                            )
                            else -> error("should never happen")// emptySet()
                        }
                        RuntimeRuleListKind.SEPARATED_LIST -> when {
                            (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> listOf(
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR)
                            )
                            0 == position -> when {
                                0 == this.rhs.multiMin -> listOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                                )
                                0 < this.rhs.multiMin -> listOf(
                                    RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE)
                                )
                                else -> error("should never happen")
                            }
                            (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> listOf(
                                RulePosition(this, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM)
                            )
                            else -> error("should never happen")//emptySet()
                        }
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                        else -> throw RuntimeException("Internal Error: rule kind not recognised")
                    }
                }
            }
        }
    }

    internal fun calcItemsAt(position: Int): Set<RuntimeRule> {
        //TODO: would it be faster to return an array here?
        return if (RulePosition.END_OF_RULE == position) {
            emptySet()
        } else {
            when (this.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> {
                    emptySet()
                }
                RuntimeRuleRhsItemsKind.CHOICE -> {
                    if (position == 0) {
                        this.rhs.items.toSet()
                    } else {
                        emptySet()
                    }
                }
                RuntimeRuleRhsItemsKind.CONCATENATION -> {
                    if (position >= this.rhs.items.size) {
                        throw RuntimeException("Internal Error: No NextExpectedItem")
                    } else {
                        var nextItem = this.rhs.items[position]
                        val res = setOf(nextItem)
                        res
                    }
                }
                RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> when {
                        (0 == position && 0 == this.rhs.multiMin) -> setOf(this.rhs.items[0], this.emptyRuleItem)
                        (position < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> setOf(this.rhs.items[0])
                        else -> emptySet<RuntimeRule>()
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> when {
                        (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> setOf(this.rhs.SLIST__separator)
                        (0 == position && 0 == this.rhs.multiMin) -> setOf(this.rhs.items[0], this.emptyRuleItem)
                        (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> setOf(this.rhs.items[0])
                        else -> emptySet()
                    }
                    else -> TODO()
                }
            }
        }
    }
/*
    fun couldHaveChild(possibleChild: RuntimeRule, atPosition: Int): Boolean {
        return when (kind) {
            RuntimeRuleKind.GOAL -> TODO()
            RuntimeRuleKind.TERMINAL -> false
            RuntimeRuleKind.NON_TERMINAL -> if (possibleChild.isSkip) {
                true
            } else {
                when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> false
                    RuntimeRuleRhsItemsKind.CHOICE ->
                        // TODO: cache this
                        0 == atPosition && this.rhs.items.contains(possibleChild)
                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        if (MULTIPLICITY_N == atPosition || atPosition >= this.rhs.items.size)
                            false
                        else
                            this.rhs.items[atPosition].number == possibleChild.number
                    }
                    RuntimeRuleRhsItemsKind.MULTI -> this.rhs.items[0].number == possibleChild.number || (this.rhs.multiMin == 0 && atPosition == 0
                            && possibleChild.isEmptyRule && possibleChild.ruleThatIsEmpty.number == this.number)
                    RuntimeRuleRhsItemsKind.SEPARATED_LIST -> {
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
                    RuntimeRuleRhsItemsKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleRhsItemsKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    RuntimeRuleRhsItemsKind.UNORDERED -> TODO()
                }
            }
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    fun incrementNextItemIndex(currentIndex: Int): Int {
        when (this.rhs.itemsKind) {
            RuntimeRuleRhsItemsKind.EMPTY -> return MULTIPLICITY_N
            RuntimeRuleRhsItemsKind.CHOICE -> return MULTIPLICITY_N
            RuntimeRuleRhsItemsKind.CONCATENATION -> return if (this.rhs.items.size == currentIndex + 1) MULTIPLICITY_N else currentIndex + 1
//            RuntimeRuleRhsItemsKind.MULTI -> return  currentIndex
            RuntimeRuleRhsItemsKind.MULTI -> return if (MULTIPLICITY_N == this.rhs.multiMax || this.rhs.multiMax > currentIndex + 1) currentIndex + 1 else MULTIPLICITY_N
//            RuntimeRuleRhsItemsKind.SEPARATED_LIST -> return if (currentIndex == 0) 1 else 0
            RuntimeRuleRhsItemsKind.SEPARATED_LIST -> return if (MULTIPLICITY_N == this.rhs.multiMax || this.rhs.multiMax > (currentIndex / 2)) currentIndex + 1 else MULTIPLICITY_N

            else -> throw RuntimeException("Internal Error: Unknown RuleKind " + this.rhs.itemsKind)
        }
    }
*/

    // --- Any ---

    private val _hashCode = listOf(this.runtimeRuleSetNumber, this.number).hashCode()
    override fun hashCode(): Int = _hashCode

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
