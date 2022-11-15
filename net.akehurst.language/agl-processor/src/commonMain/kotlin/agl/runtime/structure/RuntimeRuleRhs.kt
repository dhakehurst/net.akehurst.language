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
    abstract val runtimeRuleSet: RuntimeRuleSet
    abstract val rulePositions: List<RulePosition>

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

    abstract fun rulePositionAt(position: Int): RulePosition?
}

internal class RuntimeRuleRhsCommonTerminal(
) : RuntimeRuleRhsTerminal() {
    override val runtimeRuleSet: RuntimeRuleSet get() = error("Internal Error: RuntimeRuleRhsCommonTerminal has no RuntimeRuleSet")
    override val rulePositions: List<RulePosition> get() = emptyList()
    override fun rulePositionAt(position: Int): RulePosition? = null
    override fun toString(): String = "<Common>"
}

internal abstract class RuntimeRuleRhsTerminal : RuntimeRuleRhs() {
}

internal class RuntimeRuleRhsPattern(
    override val runtimeRuleSet: RuntimeRuleSet,
    val pattern: String
) : RuntimeRuleRhsTerminal() {
    internal val regex by lazy { Regex(this.pattern) }
    override val rulePositions: List<RulePosition> get() = emptyList()
    override fun rulePositionAt(position: Int): RulePosition? = null
    override fun toString(): String = "\"$pattern\""
}

internal class RuntimeRuleRhsLiteral(
    override val runtimeRuleSet: RuntimeRuleSet,
    val value: String
) : RuntimeRuleRhsTerminal() {
    override val rulePositions: List<RulePosition> get() = emptyList()
    override fun rulePositionAt(position: Int): RulePosition? = null
    override fun toString(): String = "'$value'"
}

internal class RuntimeRuleRhsEmpty(
    override val runtimeRuleSet: RuntimeRuleSet,
    val ruleThatIsEmpty: RuntimeRule
) : RuntimeRuleRhsTerminal() {

    override val rulePositions: List<RulePosition> get() = emptyList()

    override fun rulePositionAt(position: Int): RulePosition? = null

    override fun toString(): String = "EMPTY(${ruleThatIsEmpty.tag})"
}

internal abstract class RuntimeRuleRhsNonTerminal : RuntimeRuleRhs() {
}

internal class RuntimeRuleRhsGoal(
    override val runtimeRuleSet: RuntimeRuleSet,
    val userGoalRule: RuntimeRule
) : RuntimeRuleRhsNonTerminal() {

    override val rulePositions: List<RulePosition> by lazy {
        listOf(
            RulePosition(runtimeRule, RulePosition.START_OF_RULE),
            RulePosition(runtimeRule, RulePosition.END_OF_RULE)
        )
    }

    override fun rulePositionAt(position: Int): RulePosition? = when {
        RulePosition.END_OF_RULE == position -> rulePositions.last()
        position < 1 -> rulePositions[0]
        else -> null
    }

    override fun toString(): String = "GOAL (${userGoalRule.tag}[${userGoalRule.ruleNumber}]"
}

internal class RuntimeRuleRhsConcatenation(
    override val runtimeRuleSet: RuntimeRuleSet,
    val items: List<RuntimeRule>
) : RuntimeRuleRhsNonTerminal() {

    override val rulePositions: List<RulePosition> by lazy {
        items.mapIndexedNotNull { index, _ ->
            if (0 == index) null else RulePosition(runtimeRule, index)
        } + RulePosition(runtimeRule, RulePosition.END_OF_RULE)
    }

    override fun rulePositionAt(position: Int): RulePosition? = when {
        RulePosition.END_OF_RULE == position -> rulePositions.last()
        position < items.size -> rulePositions[position]
        else -> null
    }

    override fun toString(): String = "CONCAT(${items.map { "${it.tag}[${it.ruleNumber}]" }.joinToString(" ")})"
}

internal class RuntimeRuleRhsListSimple(
    override val runtimeRuleSet: RuntimeRuleSet,
    val min: Int,
    val max: Int,
    val repeatedItem: RuntimeRule
) : RuntimeRuleRhsNonTerminal() {

    companion object {
        const val MULTIPLICITY_N = -1
    }

    override val rulePositions: List<RulePosition> by lazy {
        listOf(
            RulePosition(runtimeRule, RulePosition.POSITION_MULIT_ITEM),
            RulePosition(runtimeRule, RulePosition.END_OF_RULE)
        )
    }

    override fun rulePositionAt(position: Int): RulePosition? = when {
        RulePosition.END_OF_RULE == position -> rulePositions.last()
        position < max -> rulePositions[0]
        else -> null
    }

    override fun toString(): String = "List ${repeatedItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsListSeparated(
    override val runtimeRuleSet: RuntimeRuleSet,
    val min: Int,
    val max: Int,
    val repeatedItem: RuntimeRule,
    val separator: RuntimeRule
) : RuntimeRuleRhsNonTerminal() {

    companion object {
        const val MULTIPLICITY_N = -1
    }

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

    override fun toString(): String = "SList ${repeatedItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsEmbedded(
    override val runtimeRuleSet: RuntimeRuleSet,
    val embeddedRuntimeRuleSet: RuntimeRuleSet,
    val embeddedStartRule: RuntimeRule
) : RuntimeRuleRhs() {
    override val rulePositions: List<RulePosition> get() = emptyList()
    override fun rulePositionAt(position: Int): RulePosition? = null
}