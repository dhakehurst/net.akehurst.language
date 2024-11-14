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
import net.akehurst.language.parser.api.PrefOption
import net.akehurst.language.parser.api.PrefRule
import net.akehurst.language.parser.api.RuleSet
import kotlin.test.assertEquals
import kotlin.test.assertTrue

//FIXME: REPEAT - because no MPP test-fixtures
internal object RuntimeRuleSetTest {

    fun assertRrsEquals(expected: RuleSet, actual: RuleSet) = assertRrsEquals(expected as RuntimeRuleSet,actual as RuntimeRuleSet)

    fun assertRrsEquals(expected: RuntimeRuleSet, actual: RuntimeRuleSet) {
        assertEquals(expected.toString(), actual.toString())
        assertTrue(expected.matches(actual))
    }

    fun RuleSet.matches(other: RuleSet) = (this as RuntimeRuleSet).matches(other as RuntimeRuleSet)

    fun RuntimeRuleSet.matches(other: RuntimeRuleSet) :Boolean{
        val rrs = when {
            this.runtimeRules.size != other.runtimeRules.size -> false
            else -> this.runtimeRules.sortedBy { it.tag }.matches(other.runtimeRules.sortedBy { it.tag }) { t, o -> t.matches(o) }
        }
        val prs = when {
            this.precedenceRules.size != other.precedenceRules.size -> false
            else -> this.precedenceRules.sortedBy { it.contextRule.tag }.matches(other.precedenceRules.sortedBy { it.contextRule.tag }) { t, o -> t.matches(o) }
        }
        return rrs && prs
    }

    fun RulePositionRuntime.matches(other: RulePositionRuntime): Boolean = when {
        this.option != other.option -> false
        this.position != other.position -> false
        else -> this.rule.matches(other.rule)
    }

    fun RuntimeRule.matches(other: RuntimeRule): Boolean = when {
        this.name != other.name -> false
        this.isSkip != other.isSkip -> false
        this.isPseudo != other.isPseudo -> false
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
        is RuntimeRuleRhsEmptyList -> other is RuntimeRuleRhsEmptyList && this.matches(other)
        is RuntimeRuleRhsLiteral -> other is RuntimeRuleRhsLiteral && this.matches(other)
        is RuntimeRuleRhsPattern -> other is RuntimeRuleRhsPattern && this.matches(other)
        is RuntimeRuleRhsEmbedded -> other is RuntimeRuleRhsEmbedded && this.matches(other)
    }

    fun RuntimeRuleRhsCommonTerminal.matches(other: RuntimeRuleRhsCommonTerminal) = this.rule == other.rule
    fun RuntimeRuleRhsEmpty.matches(other: RuntimeRuleRhsEmpty): Boolean = true
    fun RuntimeRuleRhsEmptyList.matches(other: RuntimeRuleRhsEmptyList): Boolean = true
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

    fun PrefRule.matches(other: PrefRule): Boolean {
        return when {
            this.contextRule.tag != other.contextRule.tag -> false
            this.options.size != other.options.size -> false
            else -> this.options.matches(other.options) { t,o -> t.matches(o) }
        }
    }

    fun PrefOption.matches(other: PrefOption): Boolean = when {
        this.precedence != other.precedence -> false
        this.spine.tag!=other.spine.tag -> false
        this.option!=other.option -> false
        this.associativity!=other.associativity -> false
        this.operators.size!=other.operators.size -> false
        else -> this.operators.matches(other.operators) { t, o -> t.unescapedTerminalValue == o.unescapedTerminalValue }
    }

}