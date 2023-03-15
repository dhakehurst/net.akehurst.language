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

/**
 * identified by: (runtimeRuleSetNumber, number, optionIndex)
 */
internal class RuntimeRule(
    val runtimeRuleSetNumber: Int,
    val ruleNumber: Int,
    val name: String?,
    val isSkip: Boolean
) : Rule {

    private lateinit var _rhs: RuntimeRuleRhs;
    fun setRhs(value: RuntimeRuleRhs) {
        this._rhs = value
    }

    val rhs get() = this._rhs

    //TODO: neeeds properties:
    // isUnnamedLiteral - so we can eliminate from AsmSimple
    // isGenerated - also w.r.t. AsmSimple so we know if we should try and get a property name from the elements
    // not sure if I really want to add the data to this class as only used for AsmSimple not runtime use?

    val isExplicitlyNamed: Boolean get() = this.name != null
    val tag: String get() = this.name ?: if (this.isTerminal) this.rhs.toString() else error("Internal Error: no tag")

    /*
    val emptyRuleItem: RuntimeRule
        get() = when {
            isEmptyTerminal -> (rhs as RuntimeRuleRhsEmpty).ruleThatIsEmpty
            else -> error("Internal Error: Not an EmptyTerminal ")
        }
*/

    val isGoal get() = this.rhs is RuntimeRuleRhsGoal
    val isEmptyTerminal get() = this.rhs is RuntimeRuleRhsEmpty
    val isEmbedded get() = this.rhs is RuntimeRuleRhsEmbedded
    val isPattern get() = this.rhs is RuntimeRuleRhsPattern

    /**
     * Empty, Literal, Pattern, Embedded
     */
    val isTerminal
        get() = when (this.rhs) {
            is RuntimeRuleRhsNonTerminal -> false
            is RuntimeRuleRhsEmpty -> true
            is RuntimeRuleRhsLiteral -> true
            is RuntimeRuleRhsPattern -> true
            is RuntimeRuleRhsEmbedded -> true
            is RuntimeRuleRhsCommonTerminal -> true
        }

    /**
     * Goal, Concatenation, ListSimple, ListSeparated
     */
    val isNonTerminal
        get() = when (this.rhs) {
            is RuntimeRuleRhsTerminal -> false
            is RuntimeRuleRhsGoal -> true
            is RuntimeRuleRhsConcatenation -> true
            is RuntimeRuleRhsChoice -> true
            is RuntimeRuleRhsList -> true
        }

    val isChoice get() = this.rhs is RuntimeRuleRhsChoice
    val isList get() = this.rhs is RuntimeRuleRhsList

    @Deprecated("use 'rhs is'")
    val kind
        get() = when {
            isEmbedded -> RuntimeRuleKind.EMBEDDED
            isGoal -> RuntimeRuleKind.GOAL
            isTerminal -> RuntimeRuleKind.TERMINAL
            isNonTerminal -> RuntimeRuleKind.NON_TERMINAL
            else -> error("Internal Error")
        }

    //val ruleThatIsEmpty: RuntimeRule get() = (this.rhs as RuntimeRuleRhsEmpty).ruleThatIsEmpty

    val asTerminalRulePosition by lazy { RulePosition(this, 0, RulePosition.END_OF_RULE) }

    //used in automaton build
    val rulePositions: Set<RulePosition> get() = rulePositionsAtStart + rulePositionsNotAtStart

    val rulePositionsNotAtStart: Set<RulePosition> get() = rhs.rulePositionsNotAtStart

    val rulePositionsAtStart get() = rhs.rulePositionsAtStart

    val rhsItems get() = this.rulePositions.flatMap { it.items }.toSet()

    /*
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
    */
    //fun rhsItemsAt(position: Int): Set<RuntimeRule> = this.rhs.rhsItemsAt(option, position)

    /*
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
    */
    /*
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
*/
    /*
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
    */
    // --- Any ---
    private val _hashCode = arrayOf(this.runtimeRuleSetNumber, this.ruleNumber).contentHashCode()
    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when {
        other !is RuntimeRule -> false
        this.runtimeRuleSetNumber != other.runtimeRuleSetNumber -> false
        this.ruleNumber != other.ruleNumber -> false
        else -> true
    }

    override fun toString(): String = "$tag = $rhs"
}
