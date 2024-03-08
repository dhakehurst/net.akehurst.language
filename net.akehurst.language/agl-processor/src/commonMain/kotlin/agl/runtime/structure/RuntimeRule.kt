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

/**
 * identified by: (runtimeRuleSetNumber, number, optionIndex)
 */
class RuntimeRule(
    val runtimeRuleSetNumber: Int,
    val ruleNumber: Int,
    val name: String?,
    override val isSkip: Boolean,
    override val isPseudo: Boolean
) : Rule {

    private lateinit var _rhs: RuntimeRuleRhs
    internal fun setRhs(value: RuntimeRuleRhs) {
        this._rhs = value
    }

    internal val rhs get() = this._rhs

    //TODO: needs properties- maybe:
    // isUnnamedLiteral - so we can eliminate from AsmSimple
    // isGenerated - also w.r.t. AsmSimple so we know if we should try and get a property name from the elements
    // not sure if I really want to add the data to this class as only used for AsmSimple not runtime use?

    val isExplicitlyNamed: Boolean get() = this.name != null
    override val tag: String get() = this.name ?: if (this.isTerminal) this.rhs.toString() else error("Internal Error: no tag")

    val isGoal get() = this.rhs is RuntimeRuleRhsGoal
    override val isEmptyTerminal get() = this.rhs is RuntimeRuleRhsEmpty
    override val isEmptyListTerminal get() = this.rhs is RuntimeRuleRhsEmptyList
    override val isEmbedded get() = this.rhs is RuntimeRuleRhsEmbedded
    override val isPattern get() = this.rhs is RuntimeRuleRhsPattern
    override val isLiteral get() = this.rhs is RuntimeRuleRhsLiteral

    /**
     * Empty, Literal, Pattern, Embedded
     */
    override val isTerminal
        get() = when (this.rhs) {
            is RuntimeRuleRhsNonTerminal -> false
            is RuntimeRuleRhsEmpty -> true
            is RuntimeRuleRhsEmptyList -> true
            is RuntimeRuleRhsLiteral -> true
            is RuntimeRuleRhsPattern -> true
            is RuntimeRuleRhsEmbedded -> true
            is RuntimeRuleRhsCommonTerminal -> true
        }

    /**
     * Goal, Concatenation, ListSimple, ListSeparated
     */
    val isNonTerminal
        get() = when (this.rhs) {
            is RuntimeRuleRhsTerminal -> false
            is RuntimeRuleRhsGoal -> true
            is RuntimeRuleRhsConcatenation -> true
            is RuntimeRuleRhsChoice -> true
            is RuntimeRuleRhsOptional -> true
            is RuntimeRuleRhsList -> true
        }

    override val isChoice get() = this.rhs is RuntimeRuleRhsChoice
    val isChoiceAmbiguous get() = this.isChoice && (this.rhs as RuntimeRuleRhsChoice).choiceKind == RuntimeRuleChoiceKind.AMBIGUOUS
    override val isOptional: Boolean get() = this.rhs is RuntimeRuleRhsOptional
    override val isListOptional: Boolean get() = this.rhs is RuntimeRuleRhsList && 0 == (this.rhs as RuntimeRuleRhsList).min
    override val isList get() = this.rhs is RuntimeRuleRhsList
    override val isListSimple: Boolean get() = this.rhs is RuntimeRuleRhsListSimple
    override val isListSeparated: Boolean get() = this.rhs is RuntimeRuleRhsListSeparated

    @Deprecated("use 'rhs is'")
//    val kind
//        get() = when {
//            isEmbedded -> RuntimeRuleKind.EMBEDDED
//            isGoal -> RuntimeRuleKind.GOAL
//            isTerminal -> RuntimeRuleKind.TERMINAL
//            isNonTerminal -> RuntimeRuleKind.NON_TERMINAL
//            else -> error("Internal Error")
//        }

    //val ruleThatIsEmpty: RuntimeRule get() = (this.rhs as RuntimeRuleRhsEmpty).ruleThatIsEmpty

    internal val asTerminalRulePosition by lazy { RulePosition(this, 0, RulePosition.END_OF_RULE) }

    //used in automaton build
    internal val rulePositions: Set<RulePosition> get() = rulePositionsAtStart + rulePositionsNotAtStart

    internal val rulePositionsNotAtStart: Set<RulePosition> get() = rhs.rulePositionsNotAtStart

    internal val rulePositionsAtStart get() = rhs.rulePositionsAtStart

    override val rhsItems get() = this.rhs.rhsItems

    override val unescapedTerminalValue: String
        get() = when (rhs) {
            is RuntimeRuleRhsLiteral -> (rhs as RuntimeRuleRhsLiteral).literalUnescaped
            is RuntimeRuleRhsPattern -> (rhs as RuntimeRuleRhsPattern).patternUnescaped
            else -> error("'unescapedTerminalValue' is only valid for Literals and Patterns")
        }

    //val rhsItems get() = this.rulePositions.flatMap { it.items }.toSet()

    val asString: String
        get() = when {
            isTerminal -> if (tag == rhs.asString) tag else "$tag(${rhs.asString})"
            isNonTerminal -> "$tag = ${rhs.asString}"
            else -> error("All rules should be either Terminal or NonTerminal")
        }

    // --- Any ---
    private val _hashCode = arrayOf(this.runtimeRuleSetNumber, this.ruleNumber).contentHashCode()
    override fun hashCode(): Int = _hashCode
    override fun equals(other: Any?): Boolean = when {
        other !is RuntimeRule -> false
        this.runtimeRuleSetNumber != other.runtimeRuleSetNumber -> false
        this.ruleNumber != other.ruleNumber -> false
        else -> true
    }

    override fun toString(): String = when {
        isTerminal -> if (tag == rhs.toString()) tag else "$tag($rhs)"
        isNonTerminal -> "$tag = $rhs"
        else -> error("All rules should be either Terminal or NonTerminal")
    }
}
