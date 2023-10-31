/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.collections.CollectionsTest.matches

internal object RuntimeRuleSetTest {

    fun RuntimeRuleSet.matches(other: RuntimeRuleSet) = when {
        this.runtimeRules.size != other.runtimeRules.size -> false
        else -> this.runtimeRules.sortedBy { it.tag }.matches(other.runtimeRules.sortedBy { it.tag }) { t, o -> t.matches(o) }
    }

    fun RulePosition.matches(other: RulePosition): Boolean = when {
        this.option != other.option -> false
        this.position != other.position -> false
        else -> this.rule.matches(other.rule)
    }

    fun RuntimeRule.matches(other: RuntimeRule): Boolean = when {
        this.name != other.name -> false
        this.isSkip != other.isSkip -> false
        this.rhs.matches(other.rhs).not() -> false
        else -> true
    }

    fun RuntimeRuleRhs.matches(other: RuntimeRuleRhs): Boolean = when (this) {
        is RuntimeRuleRhsTerminal -> other is RuntimeRuleRhsTerminal && this.matches(other)
        is RuntimeRuleRhsNonTerminal -> other is RuntimeRuleRhsNonTerminal && this.matches(other)
    }

    fun RuntimeRuleRhsTerminal.matches(other: RuntimeRuleRhsTerminal): Boolean = when (this) {
        is RuntimeRuleRhsCommonTerminal -> other is RuntimeRuleRhsCommonTerminal && this.matches(other)
        is RuntimeRuleRhsEmpty -> other is RuntimeRuleRhsEmpty && this.matches(other)
        is RuntimeRuleRhsLiteral -> other is RuntimeRuleRhsLiteral && this.matches(other)
        is RuntimeRuleRhsPattern -> other is RuntimeRuleRhsPattern && this.matches(other)
        is RuntimeRuleRhsEmbedded -> other is RuntimeRuleRhsEmbedded && this.matches(other)
    }

    fun RuntimeRuleRhsCommonTerminal.matches(other: RuntimeRuleRhsCommonTerminal) = this.rule == other.rule
    fun RuntimeRuleRhsEmpty.matches(other: RuntimeRuleRhsEmpty): Boolean = true
    fun RuntimeRuleRhsLiteral.matches(other: RuntimeRuleRhsLiteral): Boolean = this.literalUnescaped == other.literalUnescaped
    fun RuntimeRuleRhsPattern.matches(other: RuntimeRuleRhsPattern): Boolean = this.patternUnescaped == other.patternUnescaped
    fun RuntimeRuleRhsEmbedded.matches(other: RuntimeRuleRhsEmbedded): Boolean = when {
        this.embeddedRuntimeRuleSet.matches(other.embeddedRuntimeRuleSet).not() -> false
        this.embeddedStartRule.matches(other.embeddedStartRule).not() -> false
        else -> true
    }

    fun RuntimeRuleRhsNonTerminal.matches(other: RuntimeRuleRhsNonTerminal): Boolean = when (this) {
        is RuntimeRuleRhsGoal -> other is RuntimeRuleRhsGoal && this.matches(other)
        is RuntimeRuleRhsConcatenation -> other is RuntimeRuleRhsConcatenation && this.matches(other)
        is RuntimeRuleRhsChoice -> other is RuntimeRuleRhsChoice && this.matches(other)
        is RuntimeRuleRhsOptional -> other is RuntimeRuleRhsOptional && this.matches(other)
        is RuntimeRuleRhsList -> other is RuntimeRuleRhsList && this.matches(other)
    }

    fun RuntimeRuleRhsGoal.matches(other: RuntimeRuleRhsGoal): Boolean = when {
        this.userGoalRuleItem.tag == other.userGoalRuleItem.tag -> true
        else -> false
    }

    fun RuntimeRuleRhsConcatenation.matches(other: RuntimeRuleRhsConcatenation): Boolean {
        return this.concatItems.matches(other.concatItems) { t, o -> t.tag == o.tag }
    }

    fun RuntimeRuleRhsChoice.matches(other: RuntimeRuleRhsChoice): Boolean {
        return this.options.matches(other.options) { t, o -> t.matches(o) }
    }

    fun RuntimeRuleRhsOptional.matches(other: RuntimeRuleRhsOptional): Boolean {
        return this.optionalItem.tag == other.optionalItem.tag
    }

    fun RuntimeRuleRhsList.matches(other: RuntimeRuleRhsList): Boolean {
        return when {
            this.min != other.min -> false
            this.max != other.max -> false
            else -> when (this) {
                is RuntimeRuleRhsListSimple ->
                    other is RuntimeRuleRhsListSimple &&
                            this.repeatedRhsItem.tag == other.repeatedRhsItem.tag

                is RuntimeRuleRhsListSeparated ->
                    other is RuntimeRuleRhsListSeparated &&
                            this.repeatedRhsItem.tag == other.repeatedRhsItem.tag &&
                            this.separatorRhsItem.tag == other.separatorRhsItem.tag
            }
        }
    }
}