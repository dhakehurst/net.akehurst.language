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

enum class RuntimeRuleChoiceKind {
    NONE,
    AMBIGUOUS,
    LONGEST_PRIORITY,
    PRIORITY_LONGEST //TODO: deprecate this
}

internal sealed class RuntimeRuleRhs(
    val rule: RuntimeRule
) {
    abstract val rhsItems: List<List<Rule>>

    abstract val rulePositionsNotAtStart: Set<RulePosition>
    abstract val rulePositionsAtStart: Set<RulePosition>

    open val asString: String get() = this.toString()

    abstract fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule>

    abstract fun nextRulePositions(current: RulePosition): Set<RulePosition>

    abstract fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs
}

internal sealed class RuntimeRuleRhsTerminal(
    rule: RuntimeRule
) : RuntimeRuleRhs(rule) {

    override val rhsItems get() = emptyList<List<Rule>>()
    override val rulePositionsNotAtStart: Set<RulePosition> get() = emptySet()
    override val rulePositionsAtStart: Set<RulePosition> get() = emptySet()
    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = emptySet()
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
    override fun toString(): String = rule.tag
}

internal class RuntimeRuleRhsPattern(
    rule: RuntimeRule,
    val patternUnescaped: String
) : RuntimeRuleRhsTerminal(rule) {
    companion object {
        fun unescape(literalEscaped: String) =
            literalEscaped
                .replace("\\\"", "\"")
    }

    val regex by lazy { Regex(this.patternUnescaped) }
    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsPattern(clonedRules[rule.tag]!!, patternUnescaped)
    override fun toString(): String = "\"$patternUnescaped\""
}

internal class RuntimeRuleRhsLiteral(
    rule: RuntimeRule,
    val literalUnescaped: String
) : RuntimeRuleRhsTerminal(rule) {

    companion object {
        fun unescape(literalEscaped: String) =
            literalEscaped
                .replace("\\'", "'")
                .replace("\\\\", "\\")
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsLiteral(clonedRules[rule.tag]!!, literalUnescaped)
    override fun toString(): String = "'$literalUnescaped'"
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

    override val asString: String get() = "EMBED ::${embeddedStartRule.tag}"

    override fun toString(): String = "EMBED ${embeddedRuntimeRuleSet.number}::${embeddedStartRule.tag}[${embeddedStartRule.ruleNumber}]"
}

internal sealed class RuntimeRuleRhsNonTerminal(
    rule: RuntimeRule
) : RuntimeRuleRhs(rule) {
}

internal class RuntimeRuleRhsGoal(
    rule: RuntimeRule,
    val userGoalRuleItem: RuntimeRule
) : RuntimeRuleRhsNonTerminal(rule) {
    override val rhsItems = listOf(listOf(userGoalRuleItem))

    override val rulePositionsNotAtStart: Set<RulePosition>
        get() = setOf(
            RulePosition(rule, 0, RulePosition.START_OF_RULE),
            RulePosition(rule, 0, RulePosition.END_OF_RULE)
        )

    override val rulePositionsAtStart: Set<RulePosition> get() = setOf(RulePosition(rule, 0, RulePosition.START_OF_RULE))

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = when (position) {
        0 -> setOf(userGoalRuleItem)
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition) = when {
        current.isAtStart -> setOf(rulePositionsNotAtStart.last())
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

    override val rhsItems get() = listOf(concatItems)

    private val _rulePositions: List<RulePosition>
        get() = this.concatItems.mapIndexedNotNull { index, _ ->
            if (RulePosition.START_OF_RULE == index) null else RulePosition(rule, 0, index)
        } + RulePosition(rule, 0, RulePosition.END_OF_RULE)

    override val rulePositionsNotAtStart: Set<RulePosition> get() = _rulePositions.toSet()

    override val rulePositionsAtStart: Set<RulePosition> get() = setOf(RulePosition(rule, 0, RulePosition.START_OF_RULE))

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = when {
        position < this.concatItems.size -> setOf(this.concatItems[position])
        else -> emptySet()
    }

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> {
        val np = current.position + 1
        return when {
            np == this.concatItems.size -> setOf(this.rulePositionsNotAtStart.last())
            np < this.concatItems.size -> setOf(this._rulePositions[np - 1])
            else -> emptySet()
        }
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsConcatenation(
        clonedRules[rule.tag]!!,
        concatItems.map { clonedRules[it.tag]!! }
    )

    override val asString: String get() = "CONCAT(${this.concatItems.joinToString(" ") { it.tag }})"

    override fun toString(): String = "CONCAT(${this.concatItems.joinToString(" ") { "${it.tag}[${it.ruleNumber}]" }})"
}

internal class RuntimeRuleRhsChoice(
    rule: RuntimeRule,
    val choiceKind: RuntimeRuleChoiceKind,
    val options: List<RuntimeRuleRhs>
) : RuntimeRuleRhsNonTerminal(rule) {

    override val rhsItems get() = options.map { it.rhsItems[0] }

    override val rulePositionsNotAtStart: Set<RulePosition>
        get() = options.flatMapIndexed { op, choiceRhs ->
            choiceRhs.rulePositionsNotAtStart.map { RulePosition(rule, op, it.position) }
        }.toSet()

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> {
        return options[option].rhsItemsAt(0, position)
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

    override val asString: String get() = "CHOICE(${this.options.joinToString(" | ") { it.asString }})"

    override fun toString(): String = "CHOICE(${this.options.joinToString(" | ") { "$it" }})"

}

internal class RuntimeRuleRhsOptional(
    rule: RuntimeRule,
    val optionalItem: RuntimeRule
) : RuntimeRuleRhsNonTerminal(rule) {

    override val rhsItems: List<List<Rule>> get() = listOf(listOf(optionalItem), listOf(RuntimeRuleSet.EMPTY))

    override val rulePositionsAtStart: Set<RulePosition>
        get() = setOf(
            RulePosition(rule, RulePosition.OPTION_OPTIONAL_ITEM, RulePosition.START_OF_RULE),
            RulePosition(rule, RulePosition.OPTION_OPTIONAL_EMPTY, RulePosition.START_OF_RULE)
        )

    override val rulePositionsNotAtStart: Set<RulePosition>
        get() = setOf(
            RulePosition(rule, RulePosition.OPTION_OPTIONAL_ITEM, RulePosition.END_OF_RULE),
            RulePosition(rule, RulePosition.OPTION_OPTIONAL_EMPTY, RulePosition.END_OF_RULE)
        )

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = when (option) {
        RulePosition.OPTION_OPTIONAL_EMPTY -> when (position) {
            RulePosition.START_OF_RULE -> setOf(RuntimeRuleSet.EMPTY)
            else -> emptySet()
        }

        RulePosition.OPTION_OPTIONAL_ITEM -> when (position) {
            RulePosition.START_OF_RULE -> setOf(optionalItem)
            RulePosition.END_OF_RULE -> emptySet()
            else -> emptySet()
        }

        else -> error("Internal Error: Invalid option value for Optional")
    }

    override fun nextRulePositions(current: RulePosition): Set<RulePosition> = when {
        current.isAtStart -> setOf(current.atEnd())
        else -> emptySet()
    }

    override fun clone(clonedRules: Map<String, RuntimeRule>): RuntimeRuleRhs = RuntimeRuleRhsOptional(
        clonedRules[rule.tag]!!,
        clonedRules[optionalItem.tag]!!
    )

    override fun toString(): String = "OPTIONAL ${optionalItem.tag}"
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

    override val rhsItems: List<List<Rule>>
        get() = when {
            min == 0 -> listOf(listOf(repeatedRhsItem), listOf(RuntimeRuleSet.EMPTY))
            else -> listOf(listOf(repeatedRhsItem))
        }

    override val rulePositionsAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            MULTIPLICITY_N != max && min > max -> error("Internal Error: max must be > min")
            max == 0 -> setOf(RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE))
            min > 0 -> setOf(RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE))
            else /* min == 0 && max > 0 */ -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE),
                RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE)
            )
        }

    override val rulePositionsNotAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            MULTIPLICITY_N != max && min > max -> error("Internal Error: max must be > min")
            max == 0 -> setOf(
                RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
            )

            min > 0 -> when (max) {
                1 -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                )

                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE)
                )
            }

            else /* min == 0 */ -> when (max) {
                1 -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE),
                    RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
                )

                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.POSITION_MULIT_ITEM),
                    RulePosition(rule, RulePosition.OPTION_MULTI_ITEM, RulePosition.END_OF_RULE),
                    RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
                )
            }
        }

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = when (option) {
        RulePosition.OPTION_MULTI_EMPTY -> when (position) {
            RulePosition.START_OF_RULE -> setOf(RuntimeRuleSet.EMPTY)
            else -> emptySet()
        }

        RulePosition.OPTION_MULTI_ITEM -> when (position) {
            RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
            RulePosition.POSITION_MULIT_ITEM -> setOf(repeatedRhsItem)
            else -> emptySet()
        }

        else -> error("Internal Error: Invalid option value for SimpleList - $option")
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

        else -> error("Internal Error: Invalid option value for SimpleList - ${current.option}")
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

    override val rhsItems: List<List<Rule>>
        get() = when {
            min == 0 -> listOf(listOf(repeatedRhsItem), listOf(separatorRhsItem), listOf(RuntimeRuleSet.EMPTY))
            else -> listOf(listOf(repeatedRhsItem), listOf(separatorRhsItem))
        }

    override val rulePositionsAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            MULTIPLICITY_N != max && min > max -> error("Internal Error: max must be > min")
            max == 0 -> setOf(RulePosition(rule, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE))
            min > 0 -> setOf(RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE))
            else /* min == 0 && max > 0 */ -> setOf(
                RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
                RulePosition(rule, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE)
            )
        }

    override val rulePositionsNotAtStart: Set<RulePosition>
        get() = when {
            min < 0 -> error("Internal Error: min must be > 0")
            MULTIPLICITY_N != max && min > max -> error("Internal Error: max must be > min")
            max == 0 -> setOf(RulePosition(rule, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE))

            min > 0 -> when (max) {
                1 -> setOf(RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE))

                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE)
                )
            }

            else /* min == 0 */ -> when (max) {
                1 -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
                    RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
                )

                else -> setOf(
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
                    RulePosition(rule, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
                    RulePosition(rule, RulePosition.OPTION_MULTI_EMPTY, RulePosition.END_OF_RULE)
                )
            }
        }

    override fun rhsItemsAt(option: Int, position: Int): Set<RuntimeRule> = when (option) {
        RulePosition.OPTION_SLIST_EMPTY -> when (position) {
            RulePosition.START_OF_RULE -> setOf(RuntimeRuleSet.EMPTY)
            else -> emptySet()
        }

        RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR -> when (position) {
            RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
            RulePosition.POSITION_SLIST_ITEM -> setOf(repeatedRhsItem)
            RulePosition.POSITION_SLIST_SEPARATOR -> setOf(separatorRhsItem)
            else -> emptySet()
        }

        else -> error("Internal Error: Invalid option value for SeparatedList - $option")
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

        else -> error("Internal Error: Invalid option value for SeparatedList - ${current.option}")
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
