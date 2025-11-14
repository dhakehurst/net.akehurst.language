/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.grammar.api

import net.akehurst.kotlinx.collections.OrderedSet
import net.akehurst.language.base.api.*
import net.akehurst.language.regex.api.EscapedPattern
import net.akehurst.language.regex.api.EscapedValue
import net.akehurst.language.regex.api.UnescapedValue

//class GrammarException(message: String, cause: Throwable?) : RuntimeException(message, cause)
//class GrammarRuleItemNotFoundException(message: String) : RuntimeException(message)


interface GrammarDomain : Domain<GrammarNamespace, Grammar> {

    val primary get() = this.namespace.lastOrNull()?.definition?.lastOrNull()
}

interface GrammarNamespace : Namespace<Grammar>

interface GrammarItem : Formatable {
    val grammar: Grammar
}

interface GrammarReference {
    val localNamespace: Namespace<Grammar>
    val nameOrQName: PossiblyQualifiedName
    val resolved: Grammar?
    fun resolveAs(resolved: Grammar)
}

/**
 *
 * The definition of a Grammar. A grammar defines a list of rules and may be defined to extend a number of other Grammars.
 *
 */
interface Grammar : Definition<Grammar> {

    val selfReference: GrammarReference

    /**
     * the List of grammar references directly extended by this one (non-transitive)
     */
    val extends: List<GrammarReference>

    val extendsResolved: List<Grammar>

    val defaultGoalRule: GrammarRule

    val grammarRule: List<GrammarRule>
    val preferenceRule: List<PreferenceRule>

    val allGrammarReferencesInRules: List<GrammarReference>

    /**
     * the OrderedSet of grammars references extended by this one or those it extends (transitive)
     */
    val allExtends: OrderedSet<GrammarReference>

    /**
     * the OrderedSet of grammars extended by this one or those it extends (transitive)
     */
    val allExtendsResolved: OrderedSet<Grammar>

    /**
     * List of all grammar rules that belong to grammars this one extends (non-transitive)
     */
    val directInheritedGrammarRule: List<GrammarRule>

    /**
     * List of all grammar rules (transitive over extended grammars), including those overridden
     * the order of the rules is the order they are defined in with the top of the grammar extension
     * hierarchy coming first (in extension order where more than one grammar is extended)
     */
    val allGrammarRule: List<GrammarRule>

    /**
     * List of all grammar rules that belong to grammars this one extends (non-transitive)
     * with best-effort to resolve repetition and override
     */
    val directInheritedResolvedGrammarRule: OrderedSet<GrammarRule>

    /**
     * List of all grammar rules that belong to grammars this one extends (transitive)
     * with best-effort to resolve repetition and override
     */
    val allInheritedResolvedGrammarRule: OrderedSet<GrammarRule>

    val resolvedGrammarRule: OrderedSet<GrammarRule>

    /**
     * the List of rules defined by this grammar and those that this grammar extends (transitive),
     * with best-effort to handle repetition and overrides
     * the order of the rules is the order they are defined in with the top of the grammar extension
     * hierarchy coming first (in extension order where more than one grammar is extended)
     */
    val allResolvedGrammarRule: OrderedSet<GrammarRule>

    val allResolvedPreferenceRuleRule: OrderedSet<PreferenceRule>

    /**
     * the Set of all non-terminal rules in this grammar and those that this grammar extends
     */
    val allResolvedNonTerminalRule: Set<GrammarRule>

    /**
     * the Set of all terminals in this grammar and those that this grammar extends
     */
    val allResolvedTerminal: Set<Terminal>

    /**
     * the Set of all terminals that are part of skip rules in this grammar and those that this grammar extends
     */
    val allResolvedSkipTerminal: Set<Terminal>

    val allResolvedEmbeddedRules: Set<Embedded>

    val allResolvedEmbeddedGrammars: Set<Grammar>

    fun findOwnedGrammarRuleOrNull(ruleName: GrammarRuleName): GrammarRule?

    /**
     * find rule with given name in all rules that this grammar extends - but not in this grammar
     */
    fun findAllSuperGrammarRule(ruleName: GrammarRuleName): List<GrammarRule>

    /**
     * find rule with given name in all rules from this grammar and ones that this grammar extends
     */
    fun findAllGrammarRuleList(ruleName: GrammarRuleName): List<GrammarRule>

    fun findAllResolvedGrammarRule(ruleName: GrammarRuleName): GrammarRule?

    fun findAllResolvedTerminalRule(terminalPattern: EscapedPattern): Terminal

    fun findAllResolvedEmbeddedGrammars(found:Set<Grammar> = emptySet()) : Set<Grammar>

}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class GrammarRuleName(override val value: String) : PublicValueType {
    override fun toString(): String = value
}

interface PreferenceRule : GrammarItem {
    val forItem: SimpleItem
    val optionList: List<PreferenceOption>
}

enum class Associativity { LEFT, RIGHT }
enum class ChoiceIndicator { NONE, EMPTY, ITEM, NUMBER }

interface PreferenceOption : Formatable {
    val spine: Spine
    val choiceIndicator: ChoiceIndicator
    val choiceNumber: Int
    val onTerminals: List<SimpleItem>
    val associativity: Associativity
}

interface Spine {
    val parts: List<NonTerminal>
}

interface GrammarRule : GrammarItem {
    val name: GrammarRuleName
    val qualifiedName: QualifiedName
    val isOverride: Boolean
    val isSkip: Boolean
    val isLeaf: Boolean
    val isOneEmbedded: Boolean
    val rhs: RuleItem

    val compressedLeaf: Terminal
}

interface NormalRule : GrammarRule {
}

enum class OverrideKind {
    /** '=' replace references to the original rule with this one */
    REPLACE,
    /** '+|=' either append this as another option or convert the original to a choice and append this option */
    APPEND_ALTERNATIVE,
    /** '==' when a rule is inherited via multiple inheritance paths (i.e. diamond), specify which one to use */
    SUBSTITUTION //TODO: document this!
}

interface OverrideRule : GrammarRule {
    val overrideKind: OverrideKind
    val overriddenRhs: RuleItem
}

interface RuleItem {
    val owningRule: GrammarRule
    val allTerminal: Set<Terminal>
    val allNonTerminal: Set<NonTerminal>
    val allEmbedded: Set<Embedded>

    /**
     * set of all terminals ar start of this item
     * non-terminals are not recursed into
     */
    val firstTerminal: Set<Terminal>

    /**
     * set of all Tangible items (Terminal, NonTerminal, Empty) ar start of this item
     * non-terminals are not recursed into
     */
    val firstTangible: Set<TangibleItem>

    /**
     * set of all first Tangible items, following non-terminals that are not leaf items
     */
    val firstTangibleRecursive: Set<TangibleItem>

    /**
     * set of all first Concatenations, following non-terminals
     */
    val firstConcatenationRecursive: Set<Concatenation>

    /**
     * The item at the index (not child number) given
     */
    fun subItem(index: Int): RuleItem

    fun itemsForChild(childNumber: Int): Set<RuleItem>

    fun setOwningRule(rule: GrammarRule, indices: List<Int>) //TODO: make internal not on interface
}

interface Choice : RuleItem {
    val alternative: List<RuleItem>
}

interface Concatenation : RuleItem {
    val items: List<RuleItem>
}

interface ConcatenationItem : RuleItem

interface ChoiceLongest : Choice
interface ChoicePriority : Choice //TODO: think we can remove this
interface ChoiceAmbiguous : Choice

interface OptionalItem : ConcatenationItem {
    val item: RuleItem
}

interface SimpleItem : ConcatenationItem
interface ListOfItems : ConcatenationItem {
    val min: Int
    val max: Int
    val item: RuleItem
}

interface SimpleList : ListOfItems
interface SeparatedList : ListOfItems {
    val separator: RuleItem
    // val associativity: SeparatedListKind
}

enum class SeparatedListKind { Flat, Left, Right }

interface Group : SimpleItem {
    val groupedContent: RuleItem
}

interface TangibleItem : SimpleItem {
    // val name: RuleName
}

interface EmptyRule : TangibleItem
interface Terminal : TangibleItem {
    /**
     * id of the terminal is its value enclosed in '' or ""
     */
    val id: String
    val isLiteral: Boolean
    val isPattern: Boolean

    /**
     * with characters special to AGL (i.e. ") having a \ before
     */
    val escapedValue: EscapedValue

    /**
     * with characters special to AGL (i.e. ") not escaped
     */
    val unescapedValue: UnescapedValue
}

interface NonTerminal : TangibleItem {
    val ruleReference: GrammarRuleName
    val targetGrammar: GrammarReference?
    fun referencedRuleOrNull(targetGrammar: Grammar): GrammarRule?
    fun referencedRule(targetGrammar: Grammar): GrammarRule
}

interface Embedded : TangibleItem {
    /**
     *  Name of the nonTerminal to start from in the embedded Grammar
     *  (== this.name)
     **/
    val embeddedGoalName: GrammarRuleName

    /**
     * The Grammar to embed
     */
    val embeddedGrammarReference: GrammarReference

    fun referencedRule(targetGrammar: Grammar): GrammarRule
}