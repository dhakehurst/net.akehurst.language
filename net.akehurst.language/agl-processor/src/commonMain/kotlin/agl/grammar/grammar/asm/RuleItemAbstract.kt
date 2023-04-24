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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.api.grammar.*

sealed class RuleItemAbstract : RuleItem {

    protected var _owningRule: GrammarRule? = null

    override val owningRule: GrammarRule get() = this._owningRule ?: error("Internal Error: owningRule must be set")

    var index: List<Int>? = null

    abstract override val allTerminal: Set<Terminal>

    abstract override val allNonTerminal: Set<NonTerminal>

}

class ConcatenationDefault(override val items: List<ConcatenationItem>) : RuleItemAbstract(), Concatenation {

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        var i: Int = 0
        this.items.forEach {
            val nextIndex: List<Int> = indices + (i++)
            it.setOwningRule(rule, nextIndex)
        }
    }

    override fun subItem(index: Int): RuleItem {
        return this.items.get(index)
    }

    override val allTerminal: Set<Terminal> by lazy {
        this.items.flatMap { it.allTerminal }.toSet()
    }

    override val allNonTerminal: Set<NonTerminal> by lazy {
        this.items.flatMap { it.allNonTerminal }.toSet()
    }

    override val allEmbedded: Set<Embedded> by lazy {
        this.items.flatMap { it.allEmbedded }.toSet()
    }

    override fun toString(): String = this.items.joinToString(separator = " ")

}

sealed class ChoiceAbstract(
    override val alternative: List<RuleItem>
) : RuleItemAbstract(), Choice {

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        var i: Int = 0
        this.alternative.forEach {
            val nextIndex: List<Int> = indices + (i++)
            it.setOwningRule(rule, nextIndex)
        }
    }

    override fun subItem(index: Int): RuleItem {
//		 return if (index < this.alternative.size) this.alternative.get(index) else null
        return this.alternative.get(index)
    }

    override val allTerminal: Set<Terminal> by lazy {
        this.alternative.flatMap { it.allTerminal }.toSet()
    }

    override val allNonTerminal: Set<NonTerminal> by lazy {
        this.alternative.flatMap { it.allNonTerminal }.toSet()
    }

    override val allEmbedded: Set<Embedded> by lazy {
        this.alternative.flatMap { it.allEmbedded }.toSet()
    }

    override fun toString(): String = this.alternative.joinToString(separator = " | ")

}

class ChoiceLongestDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoiceEqual {}
class ChoicePriorityDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoicePriority {}
class ChoiceAmbiguousDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoiceAmbiguous {}

sealed class ConcatenationItemAbstract : RuleItemAbstract(), ConcatenationItem {}

class OptionalItemDefault(
    override val item: SimpleItem
) : ConcatenationItemAbstract(), OptionalItem {
    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.item else throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded

    override fun toString(): String = "$item?"

}

sealed class SimpleItemAbstract : ConcatenationItemAbstract(), SimpleItem {
    abstract val name: String
}

class GroupDefault(
    override val groupedContent: RuleItem
) : SimpleItemAbstract(), Group {

    override val name: String = "${'$'}group"

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.groupedContent.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.groupedContent else throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = this.groupedContent.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.groupedContent.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.groupedContent.allEmbedded

    override fun toString(): String = "( $groupedContent )"
}

sealed class TangibleItemAbstract() : SimpleItemAbstract(), TangibleItem {}

class EmptyRuleDefault : TangibleItemAbstract(), EmptyRule {

    override val name: String = "<empty>"

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override fun toString(): String = "/* empty */"
}

class TerminalDefault(
    override val value: String,
    override val isPattern: Boolean
) : TangibleItemAbstract(), Terminal {

    override val name: String = if (isPattern) "\"$value\"" else "'$value'"

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = setOf(this) //emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override fun toString(): String = if (isPattern) "\"$value\"" else "'$value'"
}

class NonTerminalDefault(
    override val name: String
) : TangibleItemAbstract(), NonTerminal {

    override fun referencedRuleOrNull(targetGrammar: Grammar): GrammarRule? = targetGrammar.findNonTerminalRule(this.name)

    override fun referencedRule(targetGrammar: Grammar): GrammarRule {
        return referencedRuleOrNull(targetGrammar)
            ?: error("Grammar GrammarRule ($name) not found in grammar (${targetGrammar.name})")
    }

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = setOf(this)

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override fun toString(): String = name
}

class EmbeddedDefault(
    override val embeddedGoalName: String,
    override val embeddedGrammarReference: GrammarReference
) : TangibleItemAbstract(), Embedded {

    override val name: String get() = this.embeddedGoalName

    override fun referencedRule(targetGrammar: Grammar): GrammarRule {
        return targetGrammar.findNonTerminalRule(this.name) ?: error("Grammar GrammarRule '$name' not found in grammar '${targetGrammar.name}'")
    }

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = setOf(this)

    override fun toString(): String = "${embeddedGrammarReference.nameOrQName}.$embeddedGoalName"
}

sealed class ListOfItemsAbstract(
    override val min: Int,
    override val max: Int,
) : ConcatenationItemAbstract(), ListOfItems {}

class SimpleListDefault(
    min_: Int,
    max_: Int,
    override val item: SimpleItem
) : ListOfItemsAbstract(min_, max_), SimpleList {

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.item else throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded

    override fun toString(): String {
        val mult = when {
            0 == min && 1 == max -> "?"
            0 == min && -1 == max -> "*"
            1 == min && -1 == max -> "+"
            -1 == max -> " $min+"
            else -> " $min..$max"
        }
        return "$item$mult"
    }
}

class SeparatedListDefault(
    min_: Int,
    max_: Int,
    override val item: SimpleItem,
    override val separator: SimpleItem,
    //override val associativity: SeparatedListKind
) : ListOfItemsAbstract(min_, max_), SeparatedList {

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
        this.separator.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return when (index) {
            0 -> this.item
            1 -> this.separator
            else -> throw GrammarRuleItemNotFoundException("subitem ${index} not found")
        }
    }

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal + this.separator.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal + this.separator.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded + this.separator.allEmbedded

    override fun toString(): String {
        val mult = when {
            0 == min && 1 == max -> "?"
            0 == min && -1 == max -> "*"
            1 == min && -1 == max -> "+"
            -1 == max -> " $min+"
            else -> " $min..$max"
        }
        return "[$item / $separator]$mult"
    }
}