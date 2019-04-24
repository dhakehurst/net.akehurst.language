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

data class RulePosition(
        val runtimeRule: RuntimeRule,
        val choice: Int,
        val position: Int
) {

    companion object {
        val START_OF_RULE = 0
        val END_OF_RULE = -1
        //for use in multi and separated list
        val MULIT_ITEM_POSITION = 1
        val SLIST_SEPARATOR_POSITION = 1
        val SLIST_ITEM_POSITION = 2
    }

    val isAtStart = position == START_OF_RULE
    val isAtEnd = position == END_OF_RULE

    val items: Set<RuntimeRule>
        get() {
            return if (END_OF_RULE == position) {
                emptySet()
            } else {
                runtimeRule.items(choice, position)
            }
        }


    fun next(): Set<RulePosition> {
        return this.items.flatMap { this.next(it) }.toSet()
    }

    /**
     * itemRule is the rule we use to increment rp
     */
    private fun next(itemRule: RuntimeRule): Set<RulePosition> { //TODO: cache this
        return if (RulePosition.END_OF_RULE == this.position) {
            emptySet() //TODO: use goal rule to find next position? maybe
        } else {
            when (this.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> throw ParseException("This should never happen!")
                RuntimeRuleItemKind.CHOICE_EQUAL -> when {
                    itemRule == this.runtimeRule.rhs.items[this.choice] -> setOf(RulePosition(this.runtimeRule, this.choice, END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CHOICE_PRIORITY -> when {
                    itemRule == this.runtimeRule.rhs.items[this.choice] -> setOf(RulePosition(this.runtimeRule, this.choice, END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CONCATENATION -> { //TODO: check itemRule?
                    val np = this.position + 1
                    if (np < this.runtimeRule.rhs.items.size) {
                        setOf(RulePosition(this.runtimeRule, 0, np))
                    } else {
                        setOf(RulePosition(this.runtimeRule, 0, END_OF_RULE))
                    }
                }
                RuntimeRuleItemKind.MULTI -> when (this.choice) {
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when {
                        START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.MULTI__emptyRule -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__EMPTY_RULE, END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.MULTI__ITEM -> when (this.position) {
                        START_OF_RULE -> when {
                            1 == this.runtimeRule.rhs.multiMax -> setOf(
                                    RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__ITEM, END_OF_RULE)
                            )
                            else -> setOf(
                                    RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__ITEM, MULIT_ITEM_POSITION),
                                    RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__ITEM, END_OF_RULE)
                            )
                        }
                        MULIT_ITEM_POSITION -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__ITEM, MULIT_ITEM_POSITION),
                                RulePosition(this.runtimeRule, RuntimeRuleItem.MULTI__ITEM, END_OF_RULE)
                        )
                        END_OF_RULE -> emptySet()
                        else -> emptySet()
                    }
                    else -> emptySet()
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when (this.choice) {
                    RuntimeRuleItem.SLIST__EMPTY_RULE -> when {
                        START_OF_RULE == this.position && this.runtimeRule.rhs.multiMin == 0 && itemRule == this.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                                RulePosition(this.runtimeRule, this.choice, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__ITEM -> when {
                        START_OF_RULE == this.position && (this.runtimeRule.rhs.multiMax == 1) && itemRule == this.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        START_OF_RULE == this.position && (this.runtimeRule.rhs.multiMax > 1 || -1 == this.runtimeRule.rhs.multiMax) && itemRule == this.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        SLIST_ITEM_POSITION == this.position && (this.runtimeRule.rhs.multiMax > 1 || -1 == this.runtimeRule.rhs.multiMax) && itemRule == this.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__SEPARATOR -> when {
                        SLIST_SEPARATOR_POSITION == this.position && (this.runtimeRule.rhs.multiMax > 1 || -1 == this.runtimeRule.rhs.multiMax) && itemRule == this.runtimeRule.rhs.SLIST__separator -> setOf(
                                RulePosition(this.runtimeRule, RuntimeRuleItem.SLIST__ITEM, 2)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    else -> throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.UNORDERED -> throw ParseException("Not yet supported")
            }
        }
    }


    override fun toString(): String {
        val r = when {
            runtimeRule == RuntimeRuleSet.END_OF_TEXT -> "EOT"
            runtimeRule.isTerminal -> if (runtimeRule.isPattern) "\"${runtimeRule.name}\"" else "'${runtimeRule.name}'"
            else -> runtimeRule.name
        }
        return "RP(${r},$choice,$position)"
    }

}