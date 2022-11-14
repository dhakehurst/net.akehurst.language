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

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.api.runtime.RuleKind
import net.akehurst.language.agl.regex.asRegexLiteral
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.lazyArray
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.transitiveClosure

/**
 * identified by: (runtimeRuleSetNumber, number, optionIndex)
 */
internal class RuntimeRule(
    val ruleNumber: Int,
    val optionIndex: Int,
    val name: String?,
    val isSkip: Boolean,
    val rhs: RuntimeRuleRhs
) : Rule {

    val runtimeRuleSet = this.rhs.runtimeRuleSet
    val runtimeRuleSetNumber: Int = runtimeRuleSet.number

    //TODO: neeeds properties:
    // isUnnamedLiteral - so we can eliminate from AsmSimple
    // isGenerated - also w.r.t. AsmSimple so we know if we should try and get a property name from the elements
    // not sure if I really want to add the data to this class as only used for AsmSimple not runtime use?

    val isExplicitlyNamed: Boolean get() = this.name != null
    val tag: String get() = this.name ?: if (this.isTerminal) this.rhs.toString() else error("Internal Error: no tag")

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

    val isGoal = this.rhs is RuntimeRuleRhsGoal
    val isEmptyTerminal = this.rhs is RuntimeRuleRhsEmpty
    val isEmbedded = when (this.rhs) {
        is RuntimeRuleRhsEmbedded -> true
        else -> false
    }
    val isTerminal = when (this.rhs) {
        is RuntimeRuleRhsEmpty -> true
        is RuntimeRuleRhsLiteral -> true
        is RuntimeRuleRhsPattern -> true
        is RuntimeRuleRhsEmbedded -> true
        else -> false
    }
    val isNonTerminal = when (this.rhs) {
        is RuntimeRuleRhsConcatenation -> true
        is RuntimeRuleRhsListSimple -> true
        is RuntimeRuleRhsListSeparated -> true
        else -> false
    }

    val ruleThatIsEmpty: RuntimeRule get() = (this.rhs as RuntimeRuleRhsEmpty).ruleThatIsEmpty

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

    val asTerminalRulePosition by lazy { RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE) }

    val rulePositions: Set<RuleOptionPosition> //TODO: make constants where possible for these sets
        get() {
            return when (kind) {
                RuntimeRuleKind.GOAL -> rhs.items.mapIndexed { index, _ ->
                    RuleOptionPosition(this, 0, index)
                }.toSet() + RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE)

                RuntimeRuleKind.TERMINAL -> setOf(RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> setOf(
                        RuleOptionPosition(this, 0, RuleOptionPosition.START_OF_RULE),
                        RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE)
                    )

                    RuntimeRuleRhsItemsKind.CHOICE -> rhs.items.mapIndexed { index, _ ->
                        setOf(
                            RuleOptionPosition(this, index, RuleOptionPosition.START_OF_RULE),
                            RuleOptionPosition(this, index, RuleOptionPosition.END_OF_RULE)
                        )
                    }.flatMap { it }.toSet()

                    RuntimeRuleRhsItemsKind.CONCATENATION -> rhs.items.mapIndexed { index, _ ->
                        RuleOptionPosition(this, 0, index)
                    }.toSet() + RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE)

                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> if (0 == rhs.multiMin) {
                            if (rhs.multiMax == 1 || rhs.multiMax == 0) {
                                setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.END_OF_RULE)
                                )
                            } else {
                                setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.POSITION_MULIT_ITEM),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.END_OF_RULE)
                                )
                            }
                        } else {
                            setOf(
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.START_OF_RULE),
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.POSITION_MULIT_ITEM),
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.END_OF_RULE)
                            )
                        }

                        RuntimeRuleListKind.SEPARATED_LIST -> when (this.rhs.multiMin) {
                            0 -> when (this.rhs.multiMax) {
                                MULTIPLICITY_N -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_ITEM),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                0 -> error("Multiplicity of 0..0 is meaningless")
                                1 -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                2 -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                else -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.END_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_ITEM),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )
                            }

                            else -> when (this.rhs.multiMax) {
                                MULTIPLICITY_N -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_ITEM),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                0 -> error("Multiplicity of X..0 is meaningless")
                                1 -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                2 -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )

                                else -> setOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_ITEM),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.END_OF_RULE)
                                )
                            }
                        }

                        RuntimeRuleListKind.UNORDERED -> TODO()
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                    }
                }

                RuntimeRuleKind.EMBEDDED -> setOf(RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE))
            }
        }

    val rulePositionsAt = lazyMutableMapNonNull<Int, List<RuleOptionPosition>> { index ->
        this.calcExpectedRulePositions(index)
    }

    val itemsAt by lazy {
        lazyArray(numberOfRulePositions) { index ->
            calcItemsAt(index).toTypedArray()
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
                    RuntimeRuleRhsItemsKind.EMPTY -> Unit // already in firstItems
                    RuntimeRuleRhsItemsKind.CONCATENATION -> Unit // already in firstItems
                    RuntimeRuleRhsItemsKind.CHOICE -> Unit // already in firstItems
                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
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

                        else -> TODO()//TODO: L/R-Assoc and unorderd
                    }
                }
                return firstItems
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
            RuntimeRuleKind.NON_TERMINAL -> when (rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> null
                RuntimeRuleRhsItemsKind.CHOICE -> rhs.items[option]
                RuntimeRuleRhsItemsKind.CONCATENATION -> when {
                    RuleOptionPosition.END_OF_RULE == position -> null
                    rhs.items.size > position -> rhs.items[position]
                    else -> null
                }

                RuntimeRuleRhsItemsKind.LIST -> when (rhs.listKind) {
                    RuntimeRuleListKind.NONE -> error("")
                    RuntimeRuleListKind.UNORDERED -> TODO() // will require a multiple items in the set
                    RuntimeRuleListKind.MULTI -> when (option) {
                        RuntimeRuleRhs.MULTI__ITEM -> when (position) {
                            RuleOptionPosition.END_OF_RULE -> null
                            RuleOptionPosition.START_OF_RULE -> rhs.items[RuntimeRuleRhs.MULTI__ITEM]
                            RuleOptionPosition.POSITION_MULIT_ITEM -> rhs.items[RuntimeRuleRhs.MULTI__ITEM]
                            else -> when {
                                position >= rhs.multiMin && position <= rhs.multiMax -> rhs.items[RuntimeRuleRhs.MULTI__ITEM]
                                else -> null
                            }
                        }

                        RuntimeRuleRhs.MULTI__EMPTY_RULE -> when (position) {
                            RuleOptionPosition.START_OF_RULE -> rhs.items[RuntimeRuleRhs.MULTI__EMPTY_RULE]
                            RuleOptionPosition.END_OF_RULE -> null
                            else -> error("Should not happen")
                        }

                        else -> error("Should not happen")
                    }

                    RuntimeRuleListKind.SEPARATED_LIST -> when (option) {
                        RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR -> when (position) {
                            RuleOptionPosition.START_OF_RULE -> rhs.items[RuntimeRuleRhs.SLIST__ITEM]
                            RuleOptionPosition.POSITION_SLIST_ITEM -> rhs.items[RuntimeRuleRhs.SLIST__ITEM]
                            RuleOptionPosition.POSITION_SLIST_SEPARATOR -> rhs.items[RuntimeRuleRhs.SLIST__SEPARATOR]
                            RuleOptionPosition.END_OF_RULE -> null
                            else -> error("Should not happen")
                        }

                        RuleOptionPosition.OPTION_SLIST_EMPTY -> when (position) {
                            RuleOptionPosition.START_OF_RULE -> rhs.items[RuntimeRuleRhs.SLIST__EMPTY_RULE]
                            RuleOptionPosition.END_OF_RULE -> null
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
            RuntimeRuleKind.TERMINAL -> emptySet()
            RuntimeRuleKind.NON_TERMINAL -> {
                when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> emptySet()
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        if (n == 0) {
                            this.rhs.items.toSet()
                        } else {
                            emptySet()
                        }
                    }

                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        if (n >= this.rhs.items.size) {
                            throw RuntimeException("Internal Error: No NextExpectedItem")
                        } else {
                            if (MULTIPLICITY_N == n) {
                                emptySet()
                            } else {
                                val nextItem = this.rhs.items[n]
                                val res = mutableSetOf(nextItem)
                                res
                            }
                        }
                    }

                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> {
                            when {
                                (0 == n && 0 == this.rhs.multiMin) -> setOf(this.rhs.items[0])
                                (n < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> setOf(this.rhs.items[0])
                                else -> emptySet()
                            }
                        }

                        RuntimeRuleListKind.SEPARATED_LIST -> {
                            when {
                                (0 == n && 0 == this.rhs.multiMin) -> hashSetOf(this.rhs.items[0])
                                (n % 2 == 0) -> if (n < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) {
                                    hashSetOf(this.rhs.items[0])
                                } else {
                                    emptySet()
                                }

                                else -> emptySet()
                            }
                        }

                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                        RuntimeRuleListKind.UNORDERED -> TODO()
                    }
                }
            }

            RuntimeRuleKind.EMBEDDED -> emptySet()
        }
    }

    fun findSubRulesAt(n: Int): Set<RuntimeRule> {
        val result = setOf(this).transitiveClosure { it.findAllNonTerminalAt(n) + it.findTerminalAt(n) }
        return result
    }

    internal fun calcExpectedRulePositions(position: Int): List<RuleOptionPosition> {
        return when {
            position == RuleOptionPosition.END_OF_RULE -> emptyList()
            else -> when (kind) {
                RuntimeRuleKind.GOAL -> TODO()
                RuntimeRuleKind.TERMINAL -> emptyList() //setOf(RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE))
                RuntimeRuleKind.EMBEDDED -> emptyList() //setOf(RuleOptionPosition(this, 0, RuleOptionPosition.END_OF_RULE))
                RuntimeRuleKind.NON_TERMINAL -> when (this.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> {
                        emptyList()
                    }

                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        return if (position == 0) {
                            this.rhs.items.mapIndexed { index, _ -> RuleOptionPosition(this, index, RuleOptionPosition.START_OF_RULE) }
                        } else {
                            emptyList()
                        }
                    }

                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        return if (position >= this.rhs.items.size) {
                            emptyList()
                        } else {
                            if (position == this.rhs.items.size) {
                                listOf(RuleOptionPosition(this, RuleOptionPosition.START_OF_RULE, RuleOptionPosition.END_OF_RULE))
                            } else {
                                listOf(RuleOptionPosition(this, RuleOptionPosition.START_OF_RULE, position))
                            }
                        }
                    }

                    RuntimeRuleRhsItemsKind.LIST -> when (this.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("should not happen")
                        RuntimeRuleListKind.MULTI -> when {
                            0 == position -> when {
                                0 == this.rhs.multiMin -> listOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.START_OF_RULE),
                                )

                                0 < this.rhs.multiMin -> listOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.START_OF_RULE)
                                )

                                else -> error("should never happen")
                            }

                            (position < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax) -> listOf(
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_MULTI_ITEM, RuleOptionPosition.POSITION_MULIT_ITEM)
                            )

                            else -> error("should never happen")// emptySet()
                        }

                        RuntimeRuleListKind.SEPARATED_LIST -> when {
                            (position % 2 == 1 && (((position + 1) / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> listOf(
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_SEPARATOR)
                            )

                            0 == position -> when {
                                0 == this.rhs.multiMin -> listOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_EMPTY, RuleOptionPosition.START_OF_RULE),
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE),
                                )

                                0 < this.rhs.multiMin -> listOf(
                                    RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.START_OF_RULE)
                                )

                                else -> error("should never happen")
                            }

                            (position % 2 == 0 && ((position / 2) < this.rhs.multiMax || MULTIPLICITY_N == this.rhs.multiMax)) -> listOf(
                                RuleOptionPosition(this, RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RuleOptionPosition.POSITION_SLIST_ITEM)
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
        return if (RuleOptionPosition.END_OF_RULE == position) {
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
                        val nextItem = this.rhs.items[position]
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

    // --- Any ---
    private val _hashCode = arrayOf(this.runtimeRuleSetNumber, this.ruleNumber, this.optionIndex).contentHashCode()
    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when {
        other !is RuntimeRule -> false
        this.runtimeRuleSetNumber != other.runtimeRuleSetNumber -> false
        this.ruleNumber != other.ruleNumber -> false
        this.optionIndex != other.optionIndex -> false
        else -> true
    }

    override fun toString(): String {
        return "[$ruleNumber]" + when {
            this.isEmptyRule -> " ($tag)"
            this.kind == RuntimeRuleKind.EMBEDDED -> " ($tag) = EMBEDDED"
            this.kind == RuntimeRuleKind.NON_TERMINAL -> " ($tag) = " + this.rhs
            this.isPattern -> if (this.tag == this.value) "\"${this.value}\"" else "${this.tag}(\"${this.value}\")"
            this === RuntimeRuleSet.END_OF_TEXT -> " <EOT>"
            this === RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD -> " <RT>"
            else -> if (this.tag == this.value) "'${this.value}'" else "${this.tag}('${this.value}')"
        }
    }
}
