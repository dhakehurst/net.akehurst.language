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

package net.akehurst.language.api.grammar

interface RuleItem {
    val owningRule: GrammarRule
    fun setOwningRule(rule: GrammarRule, indices: List<Int>) //TODO: make internal not on interface
    val allTerminal: Set<Terminal>
    val allNonTerminal: Set<NonTerminal>
    val allEmbedded: Set<Embedded>
    fun subItem(index: Int): RuleItem
}

interface Choice : RuleItem {
    val alternative: List<RuleItem>
}

interface Concatenation : RuleItem {
    val items: List<ConcatenationItem>
}

interface ConcatenationItem : RuleItem

interface ChoiceEqual : Choice
interface ChoicePriority : Choice
interface ChoiceAmbiguous : Choice

interface OptionalItem : ConcatenationItem {
    val item: SimpleItem
}

interface SimpleItem : ConcatenationItem
interface ListOfItems : ConcatenationItem {
    val min: Int
    val max: Int
    val item: SimpleItem
}

interface SimpleList : ListOfItems
interface SeparatedList : ListOfItems {
    val separator: SimpleItem
    // val associativity: SeparatedListKind
}

enum class SeparatedListKind { Flat, Left, Right }

interface Group : SimpleItem {
    val groupedContent: RuleItem
}

interface TangibleItem : SimpleItem {
    val name: String
}

interface EmptyRule : TangibleItem
interface Terminal : TangibleItem, GrammarItem {
    val isPattern: Boolean
    val value: String
}

interface NonTerminal : TangibleItem {
    fun referencedRuleOrNull(targetGrammar: Grammar): GrammarRule?
    fun referencedRule(targetGrammar: Grammar): GrammarRule
}

interface Embedded : TangibleItem {
    /**
     *  Name of the nonTerminal to start from in the embedded Grammar
     *  (== this.name)
     **/
    val embeddedGoalName: String

    /**
     * The Grammar to embed
     */
    val embeddedGrammarReference: GrammarReference

    fun referencedRule(targetGrammar: Grammar): GrammarRule
}