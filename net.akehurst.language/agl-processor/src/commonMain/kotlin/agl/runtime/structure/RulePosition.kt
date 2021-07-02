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

internal typealias RuleOptionId = RuleOption //TODO: Make this an Int

internal data class RuleOption(
        val runtimeRule: RuntimeRule,
        val option: Int
)

internal class RulePosition(
        val runtimeRule: RuntimeRule,
        val option: Int,
        val position: Int
) {

    companion object {
        val START_OF_RULE = 0
        val END_OF_RULE = -1

        val OPTION_MULTI_ITEM = 0
        val OPTION_MULTI_EMPTY = 1

        val OPTION_SLIST_ITEM_OR_SEPERATOR = 0
        val OPTION_SLIST_EMPTY = 1

        //for use in multi and separated list
        val POSITION_MULIT_ITEM = 1 //TODO: make -ve maybe
        val POSITION_SLIST_SEPARATOR = 1 //TODO: make -ve maybe
        val POSITION_SLIST_ITEM = 2 //TODO: make -ve maybe
    }

    val identity:RuleOptionId=RuleOption(runtimeRule, option) //TODO: Make this an Int

    val isAtStart = position == START_OF_RULE
    val isAtEnd = position == END_OF_RULE

    val items: List<RuntimeRule> get() = if (END_OF_RULE == position) {
        emptyList()
    } else {
        listOf(runtimeRule.item(option, position)).mapNotNull { it }
    }

    val item: RuntimeRule? get() = when {
        END_OF_RULE == this.position -> null
        else -> runtimeRule.item(option, position)
    }

    val priority get() = when (this.runtimeRule.rhs.itemsKind) {
        RuntimeRuleRhsItemsKind.CHOICE -> this.option//gn.priorityFor(runtimeRule)
        else -> 0
    }

    fun atEnd() = RulePosition(this.runtimeRule, this.option, END_OF_RULE)

    fun next(): Set<RulePosition> {
        return this.items.flatMap { this.next(it) }.toSet()
    }

    /**
     * itemRule is the rule we use to increment rp
     */
    private fun next(itemRule: RuntimeRule): Set<RulePosition> { //TODO: cache this
        return if (RulePosition.END_OF_RULE == this.position) {
            emptySet()
        } else {
            when (this.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> TODO() // possibly these never happen!
                RuntimeRuleKind.EMBEDDED -> TODO()
                RuntimeRuleKind.GOAL -> when {
                    itemRule.isSkip -> setOf(RulePosition(this.runtimeRule, 0, END_OF_RULE)) //TODO: might be wrong
                    else -> when (this.position) {
                        //0 -> setOf(RulePosition(this.runtimeRule, 0, 1))
                        0 -> setOf(RulePosition(this.runtimeRule, 0, END_OF_RULE))
                        else -> error("Should never happen")
                    }
                }
                RuntimeRuleKind.NON_TERMINAL -> when (this.runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> error("This should never happen!")
                    RuntimeRuleRhsItemsKind.CHOICE -> when {
                        itemRule == this.runtimeRule.rhs.items[this.option] -> setOf(RulePosition(this.runtimeRule, this.option, END_OF_RULE))
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleRhsItemsKind.CONCATENATION -> { //TODO: check itemRule?
                        val np = this.position + 1
                        if (np < this.runtimeRule.rhs.items.size) {
                            setOf(RulePosition(this.runtimeRule, 0, np))
                        } else {
                            setOf(RulePosition(this.runtimeRule, 0, END_OF_RULE))
                        }
                    }
                    RuntimeRuleRhsItemsKind.LIST -> when (this.runtimeRule.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("")
                        RuntimeRuleListKind.MULTI -> when (this.option) {
                            OPTION_MULTI_EMPTY -> when {
                                START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.MULTI__emptyRule -> setOf(
                                    RulePosition(this.runtimeRule, OPTION_MULTI_EMPTY, END_OF_RULE)
                                )
                                else -> emptySet() //throw ParseException("This should never happen!")
                            }
                            OPTION_MULTI_ITEM -> when (this.position) {
                                START_OF_RULE -> when {
                                    1 == this.runtimeRule.rhs.multiMax -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                    )
                                    2 <= this.runtimeRule.rhs.multiMin -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM)
                                    )
                                    else -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM),
                                        RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                    )
                                }
                                POSITION_MULIT_ITEM -> setOf(
                                    RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM),
                                    RulePosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                )
                                END_OF_RULE -> emptySet()
                                else -> emptySet()
                            }
                            else -> emptySet()
                        }
                        RuntimeRuleListKind.SEPARATED_LIST -> when (this.option) {
                            OPTION_SLIST_EMPTY -> when {
                                START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                                    RulePosition(this.runtimeRule, this.option, END_OF_RULE)
                                )
                                else -> emptySet() //throw ParseException("This should never happen!")
                            }
                            OPTION_SLIST_ITEM_OR_SEPERATOR -> when (this.position) {
                                START_OF_RULE -> when {
                                    1 == this.runtimeRule.rhs.multiMax -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                    )
                                    2 <= this.runtimeRule.rhs.multiMin -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR)
                                    )
                                    //min == 0 && (max==-1 or max > 1)
                                    else -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR),
                                        RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                    )
                                }
                                POSITION_SLIST_ITEM -> setOf(
                                    RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR),
                                    RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                )
                                POSITION_SLIST_SEPARATOR -> when {
                                    (this.runtimeRule.rhs.multiMax > 1 || -1 == this.runtimeRule.rhs.multiMax) && itemRule == this.runtimeRule.rhs.SLIST__separator -> setOf(
                                        RulePosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_ITEM)
                                    )
                                    else -> error("This should never happen!")
                                }
                                END_OF_RULE -> emptySet()
                                else -> error("This should never happen!")
                            }
                            else -> error("This should never happen!")
                        }
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO("Not yet supported")
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO("Not yet supported")
                        RuntimeRuleListKind.UNORDERED -> TODO("Not yet supported")
                    }
                }
            }
        }
    }

    private val _hashCode = listOf(this.runtimeRule, this.option, this.position).hashCode()
    override fun hashCode(): Int  = _hashCode

    override fun equals(other: Any?): Boolean = when(other) {
        is RulePosition -> this.option==other.option &&
                this.position==other.position &&
                this.runtimeRule.runtimeRuleSetNumber==other.runtimeRule.runtimeRuleSetNumber &&
                this.runtimeRule.number==other.runtimeRule.number
        else -> false
    }

    override fun toString(): String {
        val r = when {
            runtimeRule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
            //runtimeRule.isTerminal -> if (runtimeRule.isPattern) "\"${runtimeRule.name}\"" else "'${runtimeRule.name}'"
            else -> runtimeRule.tag
        }
        return "RP(${r},$option,$position)"
    }

}