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

import net.akehurst.language.agl.api.runtime.RulePosition

internal typealias RuleOptionId = RuleOption //TODO: Make this an Int

internal data class RuleOption(
    val runtimeRule: RuntimeRule,
    val option: Int
) {
    val isGoal = this.runtimeRule.kind == RuntimeRuleKind.GOAL

    override fun toString(): String = "RuleOption{${runtimeRule.tag},$option}"
}

val RulePosition.isGoal get() = this.rule.isGoal

internal class RuleOptionPosition(
    val runtimeRule: RuntimeRule,
    val option: Int,
    val position: Int
) {

    val identity: RuleOptionId = RuleOption(runtimeRule, option) //TODO: Make this an Int

    val isGoal get() = this.runtimeRule.isGoal
    val isAtStart get() = position == START_OF_RULE
    val isAtEnd get() = position == END_OF_RULE
    val isTerminal get() = this.runtimeRule.isTerminal
    val isEmptyRule get() = this.runtimeRule.isEmptyRule
    val isNonTerminal get() = this.runtimeRule.isNonTerminal
    val isEmbedded get() = this.runtimeRule.isEmbedded
    val isTerminalOrEmbedded get() = this.isTerminal || this.isEmbedded

    val items: List<RuntimeRule>
        get() = if (END_OF_RULE == position) {
            emptyList()
        } else {
            listOf(runtimeRule.item(option, position)).mapNotNull { it }
        }

    val item: RuntimeRule?
        get() = when {
            END_OF_RULE == this.position -> null
            else -> runtimeRule.item(option, position)
        }

    val priority //TODO: I think that priority is always == option !
        get() = when (this.runtimeRule.kind) {
            RuntimeRuleKind.EMBEDDED,
            RuntimeRuleKind.TERMINAL -> 0
            RuntimeRuleKind.NON_TERMINAL,
            RuntimeRuleKind.GOAL ->
                when (this.runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.CHOICE -> this.option//gn.priorityFor(runtimeRule)
                    else -> 0
                }
        }

    fun atEnd() = RuleOptionPosition(this.runtimeRule, this.option, END_OF_RULE)

    fun next(): Set<RuleOptionPosition> = this.items.flatMap { this.next(it) }.toSet()

    /**
     * itemRule is the rule we use to increment rp
     */
    //FIXME: I think the parameter 'itemRule' here is not needed
    private fun next(itemRule: RuntimeRule): Set<RuleOptionPosition> { //TODO: cache this
        return if (RuleOptionPosition.END_OF_RULE == this.position) {
            emptySet()
        } else {
            when (this.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> TODO() // possibly these never happen!
                RuntimeRuleKind.EMBEDDED -> TODO()
                RuntimeRuleKind.GOAL -> when {
                    itemRule.isSkip -> setOf(RuleOptionPosition(this.runtimeRule, 0, END_OF_RULE)) //TODO: might be wrong
                    else -> when (this.position) {
                        //0 -> setOf(RuleOptionPosition(this.runtimeRule, 0, 1))
                        0 -> setOf(RuleOptionPosition(this.runtimeRule, 0, END_OF_RULE))
                        else -> error("Should never happen")
                    }
                }
                RuntimeRuleKind.NON_TERMINAL -> when (this.runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> error("This should never happen!")
                    RuntimeRuleRhsItemsKind.CHOICE -> when {
                        itemRule == this.runtimeRule.rhs.items[this.option] -> setOf(this.atEnd())
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleRhsItemsKind.CONCATENATION -> { //TODO: check itemRule?
                        val np = this.position + 1
                        if (np < this.runtimeRule.rhs.items.size) {
                            setOf(RuleOptionPosition(this.runtimeRule, 0, np))
                        } else {
                            setOf(RuleOptionPosition(this.runtimeRule, 0, END_OF_RULE))
                        }
                    }
                    RuntimeRuleRhsItemsKind.LIST -> when (this.runtimeRule.rhs.listKind) {
                        RuntimeRuleListKind.NONE -> error("")
                        RuntimeRuleListKind.MULTI -> when (this.option) {
                            OPTION_MULTI_EMPTY -> when {
                                START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.MULTI__emptyRule -> setOf(
                                    RuleOptionPosition(this.runtimeRule, OPTION_MULTI_EMPTY, END_OF_RULE)
                                )
                                else -> emptySet() //throw ParseException("This should never happen!")
                            }
                            OPTION_MULTI_ITEM -> when (this.position) {
                                START_OF_RULE -> when {
                                    1 == this.runtimeRule.rhs.multiMax -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                    )
                                    2 <= this.runtimeRule.rhs.multiMin -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM)
                                    )
                                    else -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM),
                                        RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                    )
                                }
                                POSITION_MULIT_ITEM -> setOf(
                                    RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, POSITION_MULIT_ITEM),
                                    RuleOptionPosition(this.runtimeRule, OPTION_MULTI_ITEM, END_OF_RULE)
                                )
                                END_OF_RULE -> emptySet()
                                else -> emptySet()
                            }
                            else -> emptySet()
                        }
                        RuntimeRuleListKind.SEPARATED_LIST -> when (this.option) {
                            OPTION_SLIST_EMPTY -> when {
                                START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                                    RuleOptionPosition(this.runtimeRule, this.option, END_OF_RULE)
                                )
                                else -> emptySet() //throw ParseException("This should never happen!")
                            }
                            OPTION_SLIST_ITEM_OR_SEPERATOR -> when (this.position) {
                                START_OF_RULE -> when {
                                    1 == this.runtimeRule.rhs.multiMax -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                    )
                                    2 <= this.runtimeRule.rhs.multiMin -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR)
                                    )
                                    //min == 0 && (max==-1 or max > 1)
                                    else -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR),
                                        RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                    )
                                }
                                POSITION_SLIST_ITEM -> setOf(
                                    RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_SEPARATOR),
                                    RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, END_OF_RULE)
                                )
                                POSITION_SLIST_SEPARATOR -> when {
                                    (this.runtimeRule.rhs.multiMax > 1 || -1 == this.runtimeRule.rhs.multiMax) && itemRule == this.runtimeRule.rhs.SLIST__separator -> setOf(
                                        RuleOptionPosition(this.runtimeRule, OPTION_SLIST_ITEM_OR_SEPERATOR, POSITION_SLIST_ITEM)
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

    private val _hashCode get()= arrayOf(this.runtimeRule, this.option, this.position).contentHashCode()
    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when (other) {
        is RuleOptionPosition -> this.option == other.option &&
                this.position == other.position &&
                this.runtimeRule.runtimeRuleSetNumber == other.runtimeRule.runtimeRuleSetNumber &&
                this.runtimeRule.ruleNumber == other.runtimeRule.ruleNumber
        else -> false
    }

    override fun toString(): String {
        val r = when {
            runtimeRule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
            //runtimeRule.isTerminal -> if (runtimeRule.isPattern) "\"${runtimeRule.name}\"" else "'${runtimeRule.name}'"
            else -> runtimeRule.tag
        }
        val o = when(runtimeRule.kind) {
            RuntimeRuleKind.GOAL -> option
            RuntimeRuleKind.TERMINAL -> option
            RuntimeRuleKind.EMBEDDED -> option
            RuntimeRuleKind.NON_TERMINAL -> when(runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> option
                RuntimeRuleRhsItemsKind.CHOICE -> option
                RuntimeRuleRhsItemsKind.CONCATENATION -> option
                RuntimeRuleRhsItemsKind.LIST -> when(runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.NONE -> option
                    RuntimeRuleListKind.MULTI -> when(option) {
                        OPTION_MULTI_EMPTY -> "ME"
                        OPTION_MULTI_ITEM -> "MI"
                        else -> option
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> when(option) {
                        OPTION_SLIST_EMPTY -> "LE"
                        OPTION_SLIST_ITEM_OR_SEPERATOR -> "LI"
                        else -> option
                    }
                    else -> option //TODO other list types
                }
            }
        }
        val p = when(runtimeRule.kind) {
            RuntimeRuleKind.GOAL -> position
            RuntimeRuleKind.TERMINAL -> position
            RuntimeRuleKind.EMBEDDED -> position
            RuntimeRuleKind.NON_TERMINAL -> when(runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> position
                RuntimeRuleRhsItemsKind.CHOICE -> position
                RuntimeRuleRhsItemsKind.CONCATENATION -> position
                RuntimeRuleRhsItemsKind.LIST -> when(runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.NONE -> position
                    RuntimeRuleListKind.MULTI -> when(position) {
                        START_OF_RULE -> "BR"
                        POSITION_MULIT_ITEM -> "MI"
                        END_OF_RULE -> "ER"
                        else -> position
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> when(position) {
                        START_OF_RULE -> "BR"
                        POSITION_SLIST_SEPARATOR -> "LS"
                        POSITION_SLIST_ITEM -> "LI"
                        END_OF_RULE -> "ER"
                        else -> position
                    }
                    else -> position //TODO other list types
                }
            }
        }
        return "${runtimeRule.runtimeRuleSetNumber}.RP(${r},$o,$p)"
    }

}