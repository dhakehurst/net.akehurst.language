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

import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.RulePosition
import kotlin.jvm.JvmInline

internal typealias RuleOptionId = RuleOption //TODO: Make this an Int

data class RuleOption(
    val runtimeRule: RuntimeRule,
    val option: OptionNum
) {
    val isGoal = this.runtimeRule.isGoal

    override fun toString(): String = "RuleOption{${runtimeRule.tag},$option}"
}

class RulePositionRuntime(
    override val rule: RuntimeRule,
    override val option: OptionNum,
    override val position: Int
) : RulePosition {

    val identity: RuleOptionId = RuleOption(rule, option) //TODO: Make this an Int

    val isAtStart get() = RulePosition.START_OF_RULE == position
    val isAtEnd get() = RulePosition.END_OF_RULE == position
    val isGoal: Boolean get() = this.rule.isGoal
    val isTerminal get() = this.rule.isTerminal
    val isEmbedded get() = this.rule.isEmbedded

    val items: Set<RuntimeRule>
        get() = if (this.isAtEnd) {
            emptySet()
        } else {
            this.rule.rhs.rhsItemsAt(option, position)
        }

    fun atEnd() = RulePositionRuntime(this.rule, this.option, RulePosition.END_OF_RULE)
    fun next(): Set<RulePositionRuntime> = when {
        isAtEnd -> emptySet()
        else -> rule.rhs.nextRulePositions(this)
    }

    private val _hashCode = arrayOf(this.rule, this.option, this.position).contentHashCode()
    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when {
        other !is RulePositionRuntime -> false
        this.position != other.position -> false
        this.option != other.option -> false
        this.rule != other.rule -> false
        else -> true
    }

    override fun toString(): String {
        val r = when {
            rule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
            //runtimeRule.isTerminal -> if (runtimeRule.isPattern) "\"${runtimeRule.name}\"" else "'${runtimeRule.name}'"
            else -> rule.tag
        }
        val rhs = rule.rhs
        /*
        val o = when (rhs) {
            is RuntimeRuleRhsTerminal -> option
            is RuntimeRuleRhsNonTerminal -> when (rhs) {
                is RuntimeRuleRhsGoal -> option
                is RuntimeRuleRhsConcatenation -> option
                is RuntimeRuleRhsChoice -> option
                is RuntimeRuleRhsOptional -> when (option) {
                    RulePosition.OPTION_MULTI_EMPTY -> "E"
                    RulePosition.OPTION_MULTI_ITEM -> "I"
                    else -> option
                }

                is RuntimeRuleRhsList -> when (option) {
                    RulePosition.OPTION_MULTI_EMPTY -> "E"
                    RulePosition.OPTION_MULTI_ITEM -> "I"
                    else -> option
                }
            }
        }
        */
        val o = option.toString()
        val pos = when (position) {
            0 -> "SR"
            -1 -> "ER"
            else -> position.toString()
        }
        val p = when (rhs) {
            is RuntimeRuleRhsTerminal -> pos
            is RuntimeRuleRhsNonTerminal -> when (rhs) {
                is RuntimeRuleRhsGoal -> pos
                is RuntimeRuleRhsConcatenation -> pos
                is RuntimeRuleRhsChoice -> pos
                is RuntimeRuleRhsOptional -> when (position) {
                    RulePosition.START_OF_RULE -> "BR"
                    RulePosition. END_OF_RULE -> "ER"
                    else -> pos
                }

                is RuntimeRuleRhsList -> when (rhs) {
                    is RuntimeRuleRhsListSimple -> when (position) {
                        RulePosition.START_OF_RULE -> "BR"
                        RulePosition.POSITION_MULIT_ITEM -> "MI"
                        RulePosition.END_OF_RULE -> "ER"
                        else -> pos
                    }

                    is RuntimeRuleRhsListSeparated -> when (position) {
                        RulePosition.START_OF_RULE -> "BR"
                        RulePosition.POSITION_SLIST_SEPARATOR -> "LS"
                        RulePosition.POSITION_SLIST_ITEM -> "LI"
                        RulePosition. END_OF_RULE -> "ER"
                        else -> pos
                    }
                }
            }
        }
        return "RP(${rule.runtimeRuleSetNumber}/${r},$o,$p)"
    }
}
/*
internal val RulePosition.item: RuntimeRule?
    get() = when {
        this.isAtEnd -> null
        else -> (this.rule as RuntimeRule).item(this.position)
    }


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



}
 */