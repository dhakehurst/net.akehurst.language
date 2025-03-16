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

package net.akehurst.language.grammar.asm

import net.akehurst.language.grammar.api.*

sealed class RuleItemAbstract : RuleItem {

    protected var _owningRule: GrammarRule? = null

    override val owningRule: GrammarRule get() = this._owningRule ?: error("Internal Error: owningRule must be set")

    var index: List<Int>? = null

    abstract override val allTerminal: Set<Terminal>

    abstract override val allNonTerminal: Set<NonTerminal>

}

class ConcatenationDefault(override val items: List<RuleItem>) : RuleItemAbstract(), Concatenation {

    override val allTerminal: Set<Terminal> by lazy {
        this.items.flatMap { it.allTerminal }.toSet()
    }

    override val allNonTerminal: Set<NonTerminal> by lazy {
        this.items.flatMap { it.allNonTerminal }.toSet()
    }

    override val allEmbedded: Set<Embedded> by lazy {
        this.items.flatMap { it.allEmbedded }.toSet()
    }

    override val firstTerminal: Set<Terminal> get() = items[0].firstTerminal
    override val firstTangible: Set<TangibleItem> get() = items[0].firstTangible
    override val firstTangibleRecursive: Set<TangibleItem> get() = items[0].firstTangibleRecursive
    override val firstConcatenationRecursive: Set<Concatenation> get() = setOf(this)

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

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = this.items.getOrNull(childNumber)?.let { setOf(it) } ?: emptySet()

    override fun toString(): String = this.items.joinToString(separator = " ")

}

sealed class ChoiceAbstract(
    override val alternative: List<RuleItem>
) : RuleItemAbstract(), Choice {

    override val allTerminal: Set<Terminal> by lazy {
        this.alternative.flatMap { it.allTerminal }.toSet()
    }

    override val allNonTerminal: Set<NonTerminal> by lazy {
        this.alternative.flatMap { it.allNonTerminal }.toSet()
    }

    override val allEmbedded: Set<Embedded> by lazy {
        this.alternative.flatMap { it.allEmbedded }.toSet()
    }

    override val firstTerminal: Set<Terminal> get() = alternative.flatMap { it.firstTerminal }.toSet()
    override val firstTangible: Set<TangibleItem> get() = alternative.flatMap { it.firstTangible }.toSet()
    override val firstTangibleRecursive: Set<TangibleItem> get() = alternative.flatMap { it.firstTangibleRecursive }.toSet()
    override val firstConcatenationRecursive: Set<Concatenation> get() = alternative.flatMap { it.firstConcatenationRecursive }.toSet()

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
        return this.alternative.get(index)
    }

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = when(childNumber) {
        0 -> alternative.toSet()
        else -> emptySet()
    }

    override fun toString(): String = this.alternative.joinToString(separator = " | ")

}

class ChoiceLongestDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoiceLongest {}
class ChoicePriorityDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoicePriority {}
class ChoiceAmbiguousDefault(override val alternative: List<RuleItem>) : ChoiceAbstract(alternative), ChoiceAmbiguous {}

sealed class ConcatenationItemAbstract : RuleItemAbstract(), ConcatenationItem {}

class OptionalItemDefault(
    override val item: RuleItem
) : ConcatenationItemAbstract(), OptionalItem {

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded

    override val firstTerminal: Set<Terminal> get() = item.firstTerminal
    override val firstTangible: Set<TangibleItem> get() = item.firstTangible
    override val firstTangibleRecursive: Set<TangibleItem> get() = item.firstTangibleRecursive
    override val firstConcatenationRecursive: Set<Concatenation> get() = item.firstConcatenationRecursive

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.item else error("subitem ${index} not found")
    }

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = when (childNumber) {
        0 -> setOf(item)
        else -> emptySet()
    }

    override fun toString(): String = when (item) {
        is Choice -> "($item)?"
        is Concatenation -> "($item)?"
        else -> "$item?"
    }

}

sealed class SimpleItemAbstract : ConcatenationItemAbstract(), SimpleItem {
    //abstract val name: RuleName
}

class GroupDefault(
    override val groupedContent: RuleItem
) : SimpleItemAbstract(), Group {

    override val allTerminal: Set<Terminal> get() = this.groupedContent.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.groupedContent.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.groupedContent.allEmbedded

    override val firstTerminal: Set<Terminal> get() = this.groupedContent.firstTerminal
    override val firstTangible: Set<TangibleItem> get() = this.groupedContent.firstTangible
    override val firstTangibleRecursive: Set<TangibleItem> get() = this.groupedContent.firstTangibleRecursive
    override val firstConcatenationRecursive: Set<Concatenation> get() = this.groupedContent.firstConcatenationRecursive

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.groupedContent.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.groupedContent else error("subitem ${index} not found")
    }

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = when (childNumber) {
        0 -> setOf(groupedContent)
        else -> emptySet()
    }

    override fun toString(): String = "( $groupedContent )"
}

sealed class TangibleItemAbstract() : SimpleItemAbstract(), TangibleItem {
    override val firstTangible: Set<TangibleItem> get() = setOf(this)

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = emptySet()
}

class EmptyRuleDefault : TangibleItemAbstract(), EmptyRule {

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override val firstTerminal: Set<Terminal> get() = emptySet()

    override val firstTangibleRecursive: Set<Terminal> get() = emptySet()
    override val firstConcatenationRecursive: Set<Concatenation> get() = emptySet()

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        error("subitem ${index} not found")
    }

    override fun toString(): String = "/* empty */"
}

class TerminalDefault(
    override val value: String,
    override val isPattern: Boolean
) : TangibleItemAbstract(), Terminal {

    override val id: String = if (isPattern) "\"$value\"" else "'${value}'"

    override val isLiteral: Boolean get() = isPattern.not()

    override val allTerminal: Set<Terminal> get() = setOf(this) //emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override val firstTerminal: Set<Terminal> get() = setOf(this)

    override val firstTangibleRecursive: Set<Terminal> get() = setOf(this)
    override val firstConcatenationRecursive: Set<Concatenation> get() = emptySet()

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        error("subitem ${index} not found")
    }

    override fun toString(): String = if (isPattern) "\"${value.replace("\"", "\\\"")}\"" else "'${value.replace("'", "\\'")}'"
}

class NonTerminalDefault(
    override val targetGrammar: GrammarReference?,
    override val ruleReference: GrammarRuleName
) : TangibleItemAbstract(), NonTerminal {

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = setOf(this)

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override val firstTerminal: Set<Terminal> get() = emptySet()

    override val firstTangibleRecursive: Set<TangibleItem>
        get() {
            val refRule = this.referencedRule(this.owningRule.grammar)
            return when {
                refRule.isLeaf -> setOf(this)
                else -> refRule.rhs.firstTangibleRecursive
            }
        }
    override val firstConcatenationRecursive: Set<Concatenation>
        get() {
            val refRule = this.referencedRule(this.owningRule.grammar)
            return when {
                refRule.isLeaf -> emptySet()
                else -> refRule.rhs.firstConcatenationRecursive
            }
        }

    override fun referencedRuleOrNull(targetGrammar: Grammar): GrammarRule? =
        this.targetGrammar?.resolved?.findAllResolvedGrammarRule(this.ruleReference)
            ?: targetGrammar.findAllResolvedGrammarRule(this.ruleReference)

    override fun referencedRule(targetGrammar: Grammar): GrammarRule {
        return referencedRuleOrNull(targetGrammar)
            ?: error("Grammar Rule '$ruleReference' not found in grammar '${targetGrammar.qualifiedName}'")
    }

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        error("subitem ${index} not found")
    }

    override fun toString(): String = ruleReference.value
}

class EmbeddedDefault(
    override val embeddedGoalName: GrammarRuleName,
    override val embeddedGrammarReference: GrammarReference
) : TangibleItemAbstract(), Embedded {

    val resolvedGrammarRule: GrammarRule? get() = embeddedGrammarReference.resolved?.findAllResolvedGrammarRule(embeddedGoalName)

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = setOf(this)

    override val firstTerminal: Set<Terminal> get() = emptySet()
    override val firstTangibleRecursive: Set<TangibleItem>
        get() = resolvedGrammarRule?.rhs?.firstTangibleRecursive ?: error("Embedded RuleItem is not resolved!")
    override val firstConcatenationRecursive: Set<Concatenation>
        get() = resolvedGrammarRule?.rhs?.firstConcatenationRecursive ?: error("Embedded RuleItem is not resolved!")

    override fun referencedRule(targetGrammar: Grammar): GrammarRule {
        return targetGrammar.findAllResolvedGrammarRule(this.embeddedGoalName) ?: error("Grammar GrammarRule '$embeddedGoalName' not found in grammar '${targetGrammar.name}'")
    }

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        error("subitem ${index} not found")
    }

    override fun toString(): String = "${embeddedGrammarReference.nameOrQName}.$embeddedGoalName"
}

sealed class ListOfItemsAbstract(
) : ConcatenationItemAbstract(), ListOfItems {
    override val firstTerminal: Set<Terminal> get() = item.firstTerminal
    override val firstTangible: Set<TangibleItem> get() = this.item.firstTangible
    override val firstTangibleRecursive: Set<TangibleItem> get() = this.item.firstTangibleRecursive
    override val firstConcatenationRecursive: Set<Concatenation> get() = this.item.firstConcatenationRecursive
}

class SimpleListDefault(
    override val min: Int,
    override val max: Int,
    override val item: RuleItem
) : ListOfItemsAbstract(), SimpleList {

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return if (0 == index) this.item else error("subitem ${index} not found")
    }

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = when {
        -1 != max && childNumber > max -> emptySet()
        else -> setOf(item)
    }

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
    override val min: Int,
    override val max: Int,
    override val item: RuleItem,
    override val separator: RuleItem,
    //override val associativity: SeparatedListKind
) : ListOfItemsAbstract(), SeparatedList {

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal + this.separator.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal + this.separator.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded + this.separator.allEmbedded

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
            else -> error("subitem ${index} not found")
        }
    }

    override fun itemsForChild(childNumber: Int): Set<RuleItem> = when {
        -1 != max && childNumber > max -> emptySet()
        else -> when (childNumber % 2) {
            0 -> setOf(item)
            1 -> setOf(separator)
            else -> error("Internal error: should not happen!")
        }
    }

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