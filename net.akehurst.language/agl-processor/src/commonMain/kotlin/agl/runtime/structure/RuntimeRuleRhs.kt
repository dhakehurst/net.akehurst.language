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

import net.akehurst.language.agl.regex.asRegexLiteral
import net.akehurst.language.api.parser.ParserException


/**
 * when (kind) {
 *   EMPTY                   -> items[0] == the rule that is empty
 *   CHOICE                  -> items == what to chose between
 *   PRIORITY_CHOICE         -> items == what to chose between, 0 is lowest priority
 *   CONCATENATION           -> items == what to concatenate, in order
 *   UNORDERED               -> items == what to concatenate, any order
 *   MULTI                   -> items[0] == the item to repeat, items[1] == empty rule if min==0
 *   SEPARATED_LIST          -> items[0] == the item to repeat, items[1] == separator, items[2] == empty rule if min==0
 *   LEFT_ASSOCIATIVE_LIST   -> items[0] == the item to repeat, items[1] == separator
 *   RIGHT_ASSOCIATIVE_LIST  -> items[0] == the item to repeat, items[1] == separator
 * }
 */
internal sealed class RuntimeRuleRhs {

    companion object {
        val EMPTY__RULE_THAT_IS_EMPTY = 0
        val MULTI__ITEM = 0
        val MULTI__EMPTY_RULE = 1
        val SLIST__ITEM = 0
        val SLIST__SEPARATOR = 1
        val SLIST__EMPTY_RULE = 2

    }

    abstract val runtimeRuleSet: RuntimeRuleSet

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
}

internal class RuntimeRuleRhsCommonTerminal(
) : RuntimeRuleRhs() {
    override val runtimeRuleSet: RuntimeRuleSet get() = error("Internal Error: RuntimeRuleRhsCommonTerminal has no RuntimeRuleSet")
    override fun toString(): String = "<Common>"
}

internal class RuntimeRuleRhsPattern(
    override val runtimeRuleSet: RuntimeRuleSet,
    val pattern: String
) : RuntimeRuleRhs() {
    internal val regex by lazy { Regex(this.pattern) }
    override fun toString(): String = "\"$pattern\""
}

internal class RuntimeRuleRhsLiteral(
    override val runtimeRuleSet: RuntimeRuleSet,
    val value: String
) : RuntimeRuleRhs() {
    override fun toString(): String = "'$value'"
}

internal class RuntimeRuleRhsEmpty(
    override val runtimeRuleSet: RuntimeRuleSet,
    val ruleNumberThatIsEmpty: Int
) : RuntimeRuleRhs() {
    val ruleThatIsEmpty: RuntimeRule by lazy { runtimeRuleSet.runtimeRules[ruleNumberThatIsEmpty] }
    override fun toString(): String = "EMPTY(${ruleThatIsEmpty.tag})"
}

internal class RuntimeRuleRhsGoal(
    override val runtimeRuleSet: RuntimeRuleSet,
    ruleNumber: Int
) : RuntimeRuleRhs() {
    val item: RuntimeRule by lazy { runtimeRuleSet.runtimeRules[ruleNumber] }
    override fun toString(): String = "GOAL (${item.tag}[${item.ruleNumber}]"
}

internal class RuntimeRuleRhsConcatenation(
    override val runtimeRuleSet: RuntimeRuleSet,
    ruleReferences: List<Int>
) : RuntimeRuleRhs() {
    val items: List<RuntimeRule> by lazy { ruleReferences.map { runtimeRuleSet.runtimeRules[it] } }
    override fun toString(): String = "CONCAT(${items.map { "${it.tag}[${it.ruleNumber}]" }.joinToString(" ")})"
}

internal class RuntimeRuleRhsListSimple(
    override val runtimeRuleSet: RuntimeRuleSet,
    val min: Int,
    val max: Int,
    val repeatedItemNumber: Int
) : RuntimeRuleRhs() {

    companion object {
        const val MULTIPLICITY_N = -1
    }

    val repeatedItem by lazy { runtimeRuleSet.runtimeRules[repeatedItemNumber] }

    override fun toString(): String = "List ${repeatedItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsListSeparated(
    override val runtimeRuleSet: RuntimeRuleSet,
    val min: Int,
    val max: Int,
    val repeatedItemNumber: Int,
    val separatorNumber: Int
) : RuntimeRuleRhs() {

    companion object {
        const val MULTIPLICITY_N = -1
    }

    val repeatedItem by lazy { runtimeRuleSet.runtimeRules[repeatedItemNumber] }

    val separator by lazy { runtimeRuleSet.runtimeRules[separatorNumber] }

    override fun toString(): String = "SList ${repeatedItem.tag} {$min, $max}"
}

internal class RuntimeRuleRhsEmbedded(
    override val runtimeRuleSet: RuntimeRuleSet,
    val embeddedRuntimeRuleSet: RuntimeRuleSet,
    val embeddedStartRule: RuntimeRule
) : RuntimeRuleRhs() {

}