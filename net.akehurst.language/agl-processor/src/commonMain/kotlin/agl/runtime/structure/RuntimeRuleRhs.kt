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
import net.akehurst.language.agl.api.runtime.RuntimeRuleChoiceKind

internal sealed class RuntimeRuleRhs {

    companion object {
        val EMPTY__RULE_THAT_IS_EMPTY = 0
        val MULTI__ITEM = 0
        val MULTI__EMPTY_RULE = 1
        val SLIST__ITEM = 0
        val SLIST__SEPARATOR = 1
        val SLIST__EMPTY_RULE = 2

    }

    lateinit var runtimeRule: RuntimeRule;

    abstract val rulePositions: Set<RulePosition>
    abstract val rhsItems:Set<RuntimeRule>

    abstract fun rhsItemsAt(position: Int) : Set<RuntimeRule>

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

    abstract fun rulePositionAt(position: Int): Set<RulePosition>
}

internal sealed class RuntimeRuleRhsTerminal : RuntimeRuleRhs() {
    override val rhsItems: Set<RuntimeRule> get() = emptySet()
    override val rulePositions: Set<RulePosition> get() = emptySet()
    override fun rulePositionAt(position: Int): Set<RulePosition> = emptySet()
    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = emptySet()
}

internal class RuntimeRuleRhsCommonTerminal(
) : RuntimeRuleRhsTerminal() {
    override fun toString(): String = "<Common>"
}

internal class RuntimeRuleRhsPattern(
    val pattern: String
) : RuntimeRuleRhsTerminal() {
    internal val regex by lazy { Regex(this.pattern) }
    override fun toString(): String = "\"$pattern\""
}

internal class RuntimeRuleRhsLiteral(
    val value: String
) : RuntimeRuleRhsTerminal() {
    override fun toString(): String = "'$value'"
}

internal class RuntimeRuleRhsEmpty(
    val ruleThatIsEmpty: RuntimeRule
) : RuntimeRuleRhsTerminal() {
    override fun toString(): String = "EMPTY(${ruleThatIsEmpty.tag})"
}

internal class RuntimeRuleRhsEmbedded(
    val embeddedRuntimeRuleSet: RuntimeRuleSet,
    val embeddedStartRule: RuntimeRule
) : RuntimeRuleRhsTerminal() {
}

internal sealed class RuntimeRuleRhsNonTerminal : RuntimeRuleRhs() {
}

internal class RuntimeRuleRhsGoal(
    val userGoalRuleItem: RuntimeRule
) : RuntimeRuleRhsNonTerminal() {
    override val rhsItems: Set<RuntimeRule> = setOf(userGoalRuleItem)
    override val rulePositions: Set<RulePosition> by lazy {
        setOf(
            RulePosition(runtimeRule, RulePosition.START_OF_RULE),
            RulePosition(runtimeRule, RulePosition.END_OF_RULE)
        )
    }
    override fun rulePositionAt(position: Int): Set<RulePosition> = when {
        RulePosition.END_OF_RULE == position -> setOf(rulePositions.last())
        position < 1 -> setOf(rulePositions.first())
        else -> emptySet()
    }

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when(position) {
        0 -> setOf(userGoalRuleItem)
        else -> emptySet()
    }

    override fun toString(): String = "GOAL (${userGoalRuleItem.tag}[${userGoalRuleItem.ruleNumber}]"
}

internal class RuntimeRuleRhsConcatenation(
    val concatItems: List<RuntimeRule>
) : RuntimeRuleRhsNonTerminal() {

    override val rhsItems: Set<RuntimeRule> get() = concatItems.toSet()

    private val _rulePositions: List<RulePosition> by lazy {
        this.concatItems.mapIndexedNotNull { index, _ ->
            if (0 == index) null else RulePosition(runtimeRule, index)
        } + RulePosition(runtimeRule, RulePosition.END_OF_RULE)
    }
    override val rulePositions: Set<RulePosition> get() = _rulePositions.toSet()
    override fun rulePositionAt(position: Int): Set<RulePosition> = when {
        RulePosition.END_OF_RULE == position -> setOf(rulePositions.last())
        position < this.concatItems.size -> setOf(_rulePositions[position])
        else -> emptySet()
    }

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when {
        position < this.concatItems.size -> setOf(this.concatItems[position])
        else -> emptySet()
    }
    override fun toString(): String = "CONCAT(${this.rhsItems.map { "${it.tag}[${it.ruleNumber}]" }.joinToString(" ")})"
}

internal class RuntimeRuleRhsChoice(
    val choiceKind: RuntimeRuleChoiceKind,
    val options: List<List<RuntimeRule>>
) : RuntimeRuleRhsNonTerminal() {
    override val rhsItems: Set<RuntimeRule> get() = options.flatten().toSet()
    override val rulePositions: Set<RulePosition>
        get() = TODO("not implemented")

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> {
        TODO("not implemented")
    }

    override fun rulePositionAt(position: Int): Set<RulePosition> {
        TODO("not implemented")
    }
}

internal abstract class RuntimeRuleRhsList(
) : RuntimeRuleRhsNonTerminal() {
    abstract val min:Int
    abstract val max:Int
}

internal class RuntimeRuleRhsListSimple(
    override val min: Int,
    override val max: Int,
    val repeatedRhsItem: RuntimeRule
) : RuntimeRuleRhsList() {
    override val rhsItems: Set<RuntimeRule> = setOf(repeatedRhsItem)
    override val rulePositions: Set<RulePosition> by lazy {
        setOf(
            RulePosition(runtimeRule, RulePosition.POSITION_MULIT_ITEM),
            RulePosition(runtimeRule, RulePosition.END_OF_RULE)
        )
    }
    override fun rulePositionAt(position: Int): Set<RulePosition> = when {
        RulePosition.END_OF_RULE == position -> rulePositions.last()
        position < max -> rulePositions[0]
        else -> null
    }

    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when(position) {
        RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
        RulePosition.POSITION_MULIT_ITEM -> setOf(repeatedRhsItem)
        else -> emptySet()
    }

    override fun toString(): String = "LIST ${repeatedRhsItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsListSeparated(
    override val min: Int,
    override val max: Int,
    val repeatedRhsItem: RuntimeRule,
    val separatorRhsItem: RuntimeRule
) : RuntimeRuleRhsList() {
    override val rhsItems: List<RuntimeRule> = listOf(repeatedRhsItem, separatorRhsItem)
    override val rulePositions: List<RulePosition> by lazy {
        listOf(
            RulePosition(runtimeRule, RulePosition.POSITION_MULIT_ITEM),
            RulePosition(runtimeRule, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(runtimeRule, RulePosition.END_OF_RULE)
        )
    }
    override fun rulePositionAt(position: Int): RulePosition? {
        TODO("not implemented")
    }
    override fun rhsItemsAt(position: Int): Set<RuntimeRule> = when(position) {
        RulePosition.START_OF_RULE -> setOf(repeatedRhsItem)
        RulePosition.POSITION_SLIST_ITEM -> setOf(repeatedRhsItem)
        RulePosition.POSITION_SLIST_SEPARATOR -> setOf(repeatedRhsItem)
        else -> emptySet()
    }
    override fun toString(): String = "SLIST ${repeatedRhsItem.tag} ${separatorRhsItem.tag} {$min, $max}"
}
