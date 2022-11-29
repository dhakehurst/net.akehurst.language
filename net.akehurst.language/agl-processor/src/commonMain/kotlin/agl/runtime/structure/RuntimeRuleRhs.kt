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

enum class RuntimeRuleChoiceKind {
    NONE,
    AMBIGUOUS,
    LONGEST_PRIORITY,
    PRIORITY_LONGEST
}

/*
        val rhs = this.rhs
        return when(rhs) {
            is RuntimeRuleRhsTerminal -> when(rhs) {
                is RuntimeRuleRhsCommonTerminal-> TODO()
                is RuntimeRuleRhsLiteral-> TODO()
                is RuntimeRuleRhsPattern-> TODO()
                is RuntimeRuleRhsEmpty-> TODO()
                is RuntimeRuleRhsEmbedded-> TODO()
            }
            is RuntimeRuleRhsNonTerminal-> when(rhs) {
                is RuntimeRuleRhsGoal-> TODO()
                is RuntimeRuleRhsConcatenation-> TODO()
                is RuntimeRuleRhsChoice-> TODO()
                is RuntimeRuleRhsList-> when(rhs) {
                    is RuntimeRuleRhsListSimple-> TODO()
                    is RuntimeRuleRhsListSeparated-> TODO()
                }
            }
        }
 */

internal sealed class RuntimeRuleRhs(
    val rule: RuntimeRule
) {

    companion object {
        val EMPTY__RULE_THAT_IS_EMPTY = 0
        val MULTI__ITEM = 0
        val MULTI__EMPTY_RULE = 1
        val SLIST__ITEM = 0
        val SLIST__SEPARATOR = 1
        val SLIST__EMPTY_RULE = 2

    }

    abstract val rhsItems: Set<RuntimeRule>

    abstract val rulePositions: Set<RulePosition>
    abstract val rulePositionsAtStart: Set<RulePosition>

    abstract fun rhsItemsAt(position: Int): Set<RuntimeRule>

    /*
    fun findItemAt(n: Int): Array<out RuntimeRule> {
        return when (this.itemsKind) {
            RuntimeRuleRhsItemsKind.EMPTY -> emptyArray<RuntimeRule>()
            RuntimeRuleRhsItemsKind.CHOICE -> this.items //TODO: should maybe test n == 0
            RuntimeRuleRhsItemsKind.CONCATENATION -> if (this.items.size > n) arrayOf(this.items[n]) else emptyArray<RuntimeRule>()
            RuntimeRuleRhsItemsKind.LIST -> when (listKind) {
                RuntimeRuleListKind.NONE -> error("")
                RuntimeRuleListKind.MULTI -> {
                    if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
                        arrayOf(this.items[0])
                    } else {
                        emptyArray<RuntimeRule>()
                    }
                }

                RuntimeRuleListKind.SEPARATED_LIST -> {
                    when (n % 2) {
                        0 -> if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
                            arrayOf(this.items[0])
                        } else {
                            emptyArray<RuntimeRule>()
                        }

                        1 -> arrayOf(this.SLIST__separator)
                        else -> emptyArray<RuntimeRule>() // should never happen!
                    }
                }

                RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleListKind.UNORDERED -> TODO()
            }
        }
    }
*/

    abstract fun nextRulePositions(current: RulePosition): Set<RulePosition>

    abstract fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs
}

internal sealed class RuntimeRuleRhsTerminal(
    rule: RuntimeRule
) : RuntimeRuleRhs(rule) {
    override val rhsItems: Set<RuntimeRule> get() = emptySet()
    override val rulePositions: Set<RulePosition> get() = emptySet()
    override val rulePositionsAtStart: Set<RulePosition> get() = emptySet()
    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = emptySet()
    override fun nextRulePositions(current: RulePosition): Set<RulePosition> = emptySet()
}

internal class RuntimeRuleRhsEmpty(
    rule: RuntimeRule
) : RuntimeRuleRhsTerminal(rule) {
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsEmpty(rule) //only one EMPTY rule
    override fun toString(): String = RuntimeRuleSet.EMPTY.tag
}

internal class RuntimeRuleRhsCommonTerminal(
    rule: RuntimeRule
) : RuntimeRuleRhsTerminal(rule) {
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsCommonTerminal(clonedRules[rule.tag]!!)
    override fun toString(): String = "<Common>"
}

internal class RuntimeRuleRhsPattern(
    rule: RuntimeRule,
    val pattern: String
) : RuntimeRuleRhsTerminal(rule) {
    internal val regex by lazy { Regex(this.pattern) }
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsPattern(clonedRules[rule.tag]!!, pattern)
    override fun toString(): String = "\"$pattern\""
}

internal class RuntimeRuleRhsLiteral(
    rule: RuntimeRule,
    val value: String
) : RuntimeRuleRhsTerminal(rule) {
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsLiteral(clonedRules[rule.tag]!!, value)
    override fun toString(): String = "'$value'"
}

internal class RuntimeRuleRhsEmbedded(
    rule: RuntimeRule,
    val embeddedRuntimeRuleSet: RuntimeRuleSet,
    val embeddedStartRule: RuntimeRule
) : RuntimeRuleRhsTerminal(rule) {
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs {
        val emClone = embeddedRuntimeRuleSet.clone()
        return RuntimeRuleRhsEmbedded(
            clonedRules[rule.tag]!!,
            emClone,
            emClone.findRuntimeRule(embeddedStartRule.tag)
        )
    }
}

internal sealed class RuntimeRuleRhsNonTerminal(
    rule: RuntimeRule
) : RuntimeRuleRhs(rule) {
}

internal class RuntimeRuleRhsGoal(
    rule: RuntimeRule,
    val userGoalRuleItem: RuntimeRule
) : RuntimeRuleRhsNonTerminal(rule) {
    override val rhsItems: Set<RuntimeRule> = setOf(userGoalRuleItem)
    override val rulePositions: Set<RulePosition>
        get() = setOf(
            RulePosition(rule, 0, RulePosition.START_OF_RULE),
            RulePosition(rule, 0, RulePosition.END_OF_RULE)
        )

    override val rulePositionsAtStart: Set<RulePosition> get() = setOf(RulePosition(rule, 0, RulePosition.START_OF_RULE))

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when (position) {
        0 -> setOf(userGoalRuleItem)
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition) = when {
        current.isAtStart -> setOf(rulePositions.last())
        else -> emptySet()
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsGoal(
        clonedRules[rule.tag]!!,
        clonedRules[userGoalRuleItem.tag]!!
    )
    override fun toString(): String = "GOAL (${userGoalRuleItem.tag}[${userGoalRuleItem.ruleNumber}]"
}

internal class RuntimeRuleRhsConcatenation(
    rule: RuntimeRule,
    val concatItems: List<RuntimeRule>
) : RuntimeRuleRhsNonTerminal(rule) {

    override val rhsItems: Set<RuntimeRule> get() = concatItems.toSet()

    private val _rulePositions: List<RulePosition> by lazy {
        this.concatItems.mapIndexedNotNull { index, _ ->
            if(RulePosition.START_OF_RULE==index) null else RulePosition(rule, 0, index)
        } + RulePosition(rule, 0, RulePosition.END_OF_RULE)
    }
    override val rulePositions: Set<RulePosition> get() = _rulePositions.toSet()

    override val rulePositionsAtStart: Set<RulePosition> get() = setOf(RulePosition(rule, 0, RulePosition.START_OF_RULE))

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when {
        position < this.concatItems.size -> setOf(this.concatItems[position])
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> {
        val np = current.position + 1
        return when {
            np == this.concatItems.size -> setOf(this.rulePositions.last())
            np < this.concatItems.size -> setOf(this._rulePositions[np])
            else -> emptySet()
        }
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsConcatenation(
        clonedRules[rule.tag]!!,
        concatItems.map { clonedRules[it.tag]!! }
    )
    override fun toString(): String = "CONCAT(${this.rhsItems.map { "${it.tag}[${it.ruleNumber}]" }.joinToString(" ")})"
}

internal class RuntimeRuleRhsChoice(
    rule: RuntimeRule,
    val choiceKind: RuntimeRuleChoiceKind,
    val options: List<RuntimeRuleRhs>
) : RuntimeRuleRhsNonTerminal(rule) {

    override val rhsItems: Set<RuntimeRule> get() = options.flatMap { it.rhsItems }.toSet()
    override val rulePositions: Set<RulePosition>
        get() = options.flatMapIndexed { op, choiceRhs ->
            choiceRhs.rulePositions.map { RulePosition(rule, op, it.position) }
        }.toSet()

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> {
        return options.flatMap { choiceRhs -> choiceRhs.rhsItemsAt(position) }.toSet()
    }

    override val rulePositionsAtStart: Set<RulePosition>
        get() = options.mapIndexed { op, _ ->
            RulePosition(rule, op, RulePosition.START_OF_RULE)
        }.toSet()

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> =
        this.options[current.option].nextRulePositions(current).map {
            RulePosition(rule, current.option, it.position)
        }.toSet()

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsChoice(
        clonedRules[rule.tag]!!,
        choiceKind,
        options.map { it.clone(clonedRules) }
    )

    override fun toString(): String = "CHOICE(${this.rhsItems.map { "${it.tag}[${it.ruleNumber}]" }.joinToString(" | ")})"

}

internal sealed class RuntimeRuleRhsList(
    rule: RuntimeRule,
) : RuntimeRuleRhsNonTerminal(rule) {
    companion object {
        const val MULTIPLICITY_N = -1
    }

    abstract val min: Int
    abstract val max: Int
}

internal class RuntimeRuleRhsListSimple(
    rule: RuntimeRule,
    override val min: Int,
    override val max: Int,
    val repeatedRhsItem: RuntimeRule
) : RuntimeRuleRhsList(rule) {
    override val rhsItems: Set<RuntimeRule> = setOf(repeatedRhsItem)
    override val rulePositions: Set<RulePosition> by lazy {
        when {
            min < 0 -> error("Internal Error: min must be > 0")
            min > 0 -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
            )

            else /* min == 0 */ -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE),
                RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
            )
        }
    }

    override val rulePositionsAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            min > 0 -> setOf(RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE))
            else /* min == 0 */ -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE)
            )
        }

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when (position) {
        RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
        RulePosition.POSITION_MULIT_ITEM -> setOf(repeatedRhsItem)
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> = when (current.option) {
        RulePosition.OPTION_MULTI_EMPTY -> when {
            current.isAtStart && 0 == min -> setOf(current.atEnd())
            else -> emptySet()
        }

        RulePosition.OPTION_MULTI_ITEM -> when (current.position) {
            RulePosition.START_OF_RULE -> when {
                1 == this.max -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                )

                2 <= this.min -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM)
                )

                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                )
            }

            RulePosition.POSITION_MULIT_ITEM -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
            )

            RulePosition.END_OF_RULE -> emptySet()
            else -> emptySet()
        }

        else -> emptySet()
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsListSimple(
        clonedRules[rule.tag]!!,
        min,
        max,
        clonedRules[repeatedRhsItem.tag]!!
    )

    override fun toString(): String = "LIST ${repeatedRhsItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsListSeparated(
    rule: RuntimeRule,
    override val min: Int,
    override val max: Int,
    val repeatedRhsItem: RuntimeRule,
    val separatorRhsItem: RuntimeRule
) : RuntimeRuleRhsList(rule) {
    override val rhsItems: Set<RuntimeRule> = setOf(repeatedRhsItem, separatorRhsItem)
    override val rulePositions: Set<RulePosition> by lazy {
        setOf(
            RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
        )
    }

    override val rulePositionsAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            min > 0 -> setOf(RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE))
            else /* min == 0 */ -> setOf(
                RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                RulePosition(rule, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE)
            )
        }

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when (position) {
        RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
        RulePosition.POSITION_SLIST_ITEM -> setOf(repeatedRhsItem)
        RulePosition.POSITION_SLIST_SEPARATOR -> setOf(repeatedRhsItem)
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> = when (current.option) {
        RulePosition.OPTION_SLIST_EMPTY -> when {
            current.isAtStart && min == 0 -> setOf(current.atEnd())
            else -> emptySet()
        }

        RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR -> when (current.position) {
            RulePosition.START_OF_RULE -> when {
                1 == max -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                )

                2 <= min -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR)
                )
                //min == 0 && (max==-1 or max > 1)
                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                )
            }

            RulePosition.POSITION_SLIST_ITEM -> setOf(
                RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
            )

            RulePosition.POSITION_SLIST_SEPARATOR -> when {
                (max > 1 || -1 == max) -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM)
                )

                else -> error("This should never happen!")
            }

            RulePosition.END_OF_RULE -> emptySet()
            else -> error("This should never happen!")
        }

        else -> error("This should never happen!")
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsListSeparated(
        clonedRules[rule.tag]!!,
        min,
        max,
        clonedRules[repeatedRhsItem.tag]!!,
        clonedRules[separatorRhsItem.tag]!!
    )

    override fun toString(): String = "SLIST ${repeatedRhsItem.tag} ${separatorRhsItem.tag} {$min, $max}"
}
