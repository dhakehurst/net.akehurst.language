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

package net.akehurst.language.grammar.processor

import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.grammar.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.parser.api.PrefRule

/**
 * arg: String =
 */
internal class ConverterToRuntimeRules(
    val grammar: Grammar
) {

    val runtimeRuleSet: RuntimeRuleSet by lazy {
        this.convertGrammar(grammar, "")
        val rules = this._runtimeRules.values.toList()
        RuntimeRuleSet(_ruleSetNumber, grammar.qualifiedName.value, rules, _precRules)
    }

    val originalRuleItemMap get() = _originalRuleItem

    fun originalRuleItemFor(runtimeRuleSetNumber: Int, runtimeRuleNumber: Int): RuleItem? =
        this._originalRuleItem[Pair(runtimeRuleSetNumber, runtimeRuleNumber)]


    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    private val _ruleSetNumber by lazy { RuntimeRuleSet.numberForGrammar[grammar.qualifiedName.value] }

    // index by tag
    private val _runtimeRules = mutableMapOf<String, RuntimeRule>()

    private var _precRules = emptyList<PrefRule>()

    private val _terminalRules = mutableMapOf<String, RuntimeRule>()
    private val _embeddedRules = mutableMapOf<Pair<Grammar, String>, RuntimeRule>()
    private val _originalRuleItem: MutableMap<Pair<Int, Int>, RuleItem> = mutableMapOf()
    private val _embeddedConverters: LazyMutableMapNonNull<Grammar, ConverterToRuntimeRules> = lazyMutableMapNonNull { embeddedGrammar ->
        val embeddedConverter = ConverterToRuntimeRules(embeddedGrammar)
        embeddedConverter
    }

    private val _pseudoRuleNameGenerator by lazy { PseudoRuleNames(grammar) }

    private fun recordOriginalRuleItem(runtimeRule: RuntimeRule, originalRuleItem: RuleItem) {
        this._originalRuleItem[Pair(runtimeRule.runtimeRuleSetNumber, runtimeRule.ruleNumber)] = originalRuleItem
    }

    private fun nextRule(name: String, isSkip: Boolean, isPseudo: Boolean): RuntimeRule {
        if (Debug.CHECK) check(this._runtimeRules.containsKey(name).not())
        val newRule = RuntimeRule(_ruleSetNumber, _runtimeRules.size, name, isSkip, isPseudo)
        _runtimeRules[name] = newRule
        return newRule
    }

    private fun terminalRule(name: String?, value: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        val newRule = RuntimeRule(_ruleSetNumber, _runtimeRules.size, name, isSkip, false).also {
            if (isPattern) {
                val unescaped = RuntimeRuleRhsPattern.unescape(value)
                it.setRhs(RuntimeRuleRhsPattern(it, unescaped))
            } else {
                val unescaped = RuntimeRuleRhsLiteral.unescape(value)
                it.setRhs(RuntimeRuleRhsLiteral(it, unescaped))
            }
        }
        if (Debug.CHECK) check(this._runtimeRules.containsKey(newRule.tag).not()) { "Already got rule with tag '$name'" }
//TODO: warn in analyser        check(this.terminalRules.containsKey(value).not()) { "Already got terminal rule with value '$value'" }
        _runtimeRules[newRule.tag] = newRule
        _terminalRules[value] = newRule
        name?.let { _terminalRules[it] = newRule } // add terminal by leaf name
        return newRule
    }

    private fun embeddedRule(embeddedRuleName: String, isSkip: Boolean, embeddedGrammar: Grammar, embeddedGoalRuleName: String): RuntimeRule {
        if (Debug.CHECK) check(this._runtimeRules.containsKey(embeddedRuleName).not())
        val embeddedConverter = _embeddedConverters[embeddedGrammar]
        val embeddedRuntimeRuleSet = embeddedConverter.runtimeRuleSet
        val embeddedStartRuntimeRule = embeddedRuntimeRuleSet.findRuntimeRule(embeddedGoalRuleName)
        val newRule = RuntimeRule(_ruleSetNumber, _runtimeRules.size, embeddedRuleName, isSkip, false).also {
            it.setRhs(RuntimeRuleRhsEmbedded(it, embeddedRuntimeRuleSet, embeddedStartRuntimeRule))
        }
        _runtimeRules[newRule.tag] = newRule
        _embeddedRules[Pair(embeddedGrammar, embeddedGoalRuleName)] = newRule
        return newRule
    }

    private fun findNamedRule(name: String): RuntimeRule? = this._runtimeRules[name]

    private fun findTerminal(value: String): RuntimeRule? = this._terminalRules[value]

    private fun findEmbedded(grammar: Grammar, goalRuleName: String): RuntimeRule? = this._embeddedRules[Pair(grammar, goalRuleName)]

    private fun buildCompressedRule(target: GrammarRule, isSkip: Boolean): RuntimeRule {
        val ci = target.compressedLeaf
        val rule = if (ci.isPattern) {
            this.terminalRule(target.name.value, ci.value, RuntimeRuleKind.TERMINAL, true, isSkip)
        } else {
            this.terminalRule(target.name.value, ci.value, RuntimeRuleKind.TERMINAL, false, isSkip)
        }
        return rule
    }

    private fun visitNamespace(target: Namespace<Grammar>, arg: String): Set<RuntimeRule> {
        return emptySet()
    }

    private fun convertGrammar(target: Grammar, arg: String): Set<RuntimeRule> {
        val rules = target.allResolvedGrammarRule.map {
            this.convertGrammarRule(it, arg)
        }.toSet()
        _precRules = target.allResolvedPreferenceRuleRule.map {
            this.convertPreferenceRule(it, arg)
        }
        return rules
    }

    private fun convertPreferenceRule(target: PreferenceRule, arg: String): RuntimePreferenceRule {
        val forItem = target.forItem
        val contextRule = when (forItem) {
            is NonTerminal -> findNamedRule(forItem.ruleReference.value)!!
            is Embedded -> TODO()
            is Terminal -> findTerminal(forItem.value)!!
            else -> error("Internal Error: subtype '${forItem::class.simpleName}' of SimpleItem not handled")
        }
        val options = target.optionList.mapIndexed { idx, it ->
            val prec = idx
            val tgt = findNamedRule(it.item.ruleReference.value)!!
            val opt = it.choiceNumber
            val terminals = it.onTerminals.map {
                when (it) {
                    is Terminal -> findTerminal(it.value) ?: error("Terminal '${it.value}' not found")
                    is NonTerminal -> {
                        val r = findNamedRule(it.ruleReference.value)
                        when {
                            null == r -> error("Rule named '${it.ruleReference}' not found")
                            r.isTerminal.not() -> error("Rule named '${it.ruleReference}' is not a terminal")
                            else -> r
                        }
                    }

                    is Embedded -> TODO()
                    else -> error("Internal Error: subtype '${forItem::class.simpleName}' of SimpleItem not handled")
                }
            }.toSet()
            val assoc = when (it.associativity) {
                Associativity.LEFT -> RuntimePreferenceRule.Assoc.LEFT
                Associativity.RIGHT -> RuntimePreferenceRule.Assoc.RIGHT
            }
            RuntimePreferenceRule.RuntimePreferenceOption(prec, tgt, opt, terminals, assoc)
        }
        return RuntimePreferenceRule(contextRule, options)
    }

    private fun convertGrammarRule(target: GrammarRule, arg: String): RuntimeRule {
        val rule = this.findNamedRule(target.name.value)
        val rhs = target.rhs
        return if (null == rule) {
            val (rrule, ruleItem) = when {
                target.isLeaf -> when {
                    rhs is Terminal -> {
                        val rrule = this.terminalRule(target.name.value, rhs.value, RuntimeRuleKind.TERMINAL, rhs.isPattern, target.isSkip)
                        Pair(rrule, rhs)
                    }

                    rhs is Concatenation && rhs.items.size == 1 && rhs.items[0] is Terminal -> {
                        val t = (rhs.items[0] as Terminal)
                        val rrule = this.terminalRule(target.name.value, t.value, RuntimeRuleKind.TERMINAL, t.isPattern, target.isSkip)
                        Pair(rrule, t)
                    }

                    rhs is Choice && rhs.alternative.size == 1 && rhs.alternative[0] is Terminal -> {
                        val t = (rhs.alternative[0] as Terminal)
                        val rrule = this.terminalRule(target.name.value, t.value, RuntimeRuleKind.TERMINAL, t.isPattern, target.isSkip)
                        Pair(rrule, t)
                    }

                    else -> {
                        val rrule = this.buildCompressedRule(target, target.isSkip)
                        Pair(rrule, rhs)
                    }
                }

                target.isOneEmbedded -> {
                    val embeddedRuleName = target.name
                    val e = if (target.rhs is Embedded) {
                        target.rhs as Embedded
                    } else {
                        (target.rhs as Concatenation).items[0] as Embedded
                    }
                    val embeddedRule = this.embeddedRule(embeddedRuleName.value, false, e.embeddedGrammarReference.resolved!!, e.embeddedGoalName.value)
                    this.recordOriginalRuleItem(embeddedRule, e)
                    Pair(embeddedRule, e)
                }

                else -> {
                    val nrule = this.nextRule(target.name.value, target.isSkip, false)
                    val rrhs = createRhs(nrule, target.rhs, target.name.value)
                    nrule.setRhs(rrhs)
                    Pair(nrule, target.rhs)
                }
            }
            recordOriginalRuleItem(rrule, ruleItem)
            rrule
        } else {
            rule
        }
    }

    private fun createRhs(rule: RuntimeRule, target: RuleItem, arg: String): RuntimeRuleRhs = when (target) {
        is EmptyRule -> createRhsForEmpty(target, rule)
        is Terminal -> createRhsForTerminal(target, rule, arg)
        is NonTerminal -> createRhsForNonTerminal(target, rule, arg)
        is Embedded -> createRhsForEmbedded(target, rule, arg)
        // need to allow r = A* to have r as a list, so that preference disambiguation is clear.
        is Concatenation -> createRhsForConcatenation(target, rule, arg)
        is Choice -> this.createRhsForChoice(target, rule, arg)
        is OptionalItem -> this.createRhsForOptional(target, rule, arg)
        is SimpleList -> this.createRhsForListSimple(target, rule, arg)
        is SeparatedList -> this.createRhsForListSeparated(target, rule, arg)
        is Group -> this.createRhsForGroup(target, rule, arg)
        else -> error("Unsupported")
    }

    private fun createRhsForEmpty(grammarRuleItem: EmptyRule, rule: RuntimeRule): RuntimeRuleRhsConcatenation {
        return RuntimeRuleRhsConcatenation(rule, listOf(RuntimeRuleSet.EMPTY))
    }

    private fun createRhsForTerminal(grammarRuleItem: Terminal, rule: RuntimeRule, arg: String): RuntimeRuleRhsConcatenation {
        return RuntimeRuleRhsConcatenation(rule, listOf(this.createRuleForRuleItemTerminal(grammarRuleItem, arg)))
    }

    private fun createRhsForNonTerminal(grammarRuleItem: NonTerminal, rule: RuntimeRule, arg: String): RuntimeRuleRhsConcatenation {
        val item = this.createRuleForRuleItemNonTerminal(grammarRuleItem, arg)
        return RuntimeRuleRhsConcatenation(rule, listOf(item))
    }

    private fun createRhsForEmbedded(grammarRuleItem: Embedded, rule: RuntimeRule, arg: String): RuntimeRuleRhsConcatenation {
        TODO()
    }

    private fun createRhsForConcatenation(grammarRuleItem: Concatenation, rule: RuntimeRule, arg: String): RuntimeRuleRhsConcatenation {
        if (Debug.CHECK) check(grammarRuleItem.items.size > 1)
        val items = grammarRuleItem.items.map { this.runtimeRuleForRuleItem(it, arg) }
        return RuntimeRuleRhsConcatenation(rule, items)
    }

    private fun createRhsForChoice(grammarRuleItem: Choice, rule: RuntimeRule, arg: String): RuntimeRuleRhsChoice {
        return when (grammarRuleItem.alternative.size) {
            1 -> error("Internal Error: choice should have more than one alternative")
            else -> {
                val items = grammarRuleItem.alternative.map { this.createRhsForChoiceAlternative(rule, it, arg) }
                val choiceKind = when (grammarRuleItem) {
                    is ChoiceLongest -> RuntimeRuleChoiceKind.LONGEST_PRIORITY
                    is ChoicePriority -> RuntimeRuleChoiceKind.PRIORITY_LONGEST
                    is ChoiceAmbiguous -> RuntimeRuleChoiceKind.AMBIGUOUS
                    else -> throw RuntimeException("unsupported")
                }
                RuntimeRuleRhsChoice(rule, choiceKind, items)
            }
        }
    }

    private fun createRhsForChoiceAlternative(rule: RuntimeRule, target: RuleItem, arg: String): RuntimeRuleRhsConcatenation {
        val items = when (target) {
            is Concatenation -> target.items.map { this.runtimeRuleForRuleItem(it, arg) }
            is ConcatenationItem -> listOf(runtimeRuleForRuleItem(target, arg))
            else -> error("Should not happen")
        }
        return RuntimeRuleRhsConcatenation(rule, items)
    }

    private fun createRhsForOptional(grammarRuleItem: OptionalItem, rule: RuntimeRule, arg: String): RuntimeRuleRhsOptional {
        val item = this.runtimeRuleForRuleItem(grammarRuleItem.item, arg)
        return RuntimeRuleRhsOptional(rule, item)
    }

    private fun createRhsForListSimple(grammarRuleItem: SimpleList, rule: RuntimeRule, arg: String): RuntimeRuleRhsListSimple {
        val item = this.runtimeRuleForRuleItem(grammarRuleItem.item, arg)
        return RuntimeRuleRhsListSimple(rule, grammarRuleItem.min, grammarRuleItem.max, item)
    }

    private fun createRhsForListSeparated(grammarRuleItem: SeparatedList, rule: RuntimeRule, arg: String): RuntimeRuleRhsListSeparated {
        val item = this.runtimeRuleForRuleItem(grammarRuleItem.item, arg)
        val separator = this.runtimeRuleForRuleItem(grammarRuleItem.separator, arg)
        return RuntimeRuleRhsListSeparated(rule, grammarRuleItem.min, grammarRuleItem.max, item, separator)
    }

    private fun createRhsForGroup(grammarRuleItem: Group, rule: RuntimeRule, arg: String): RuntimeRuleRhs {
        val content = grammarRuleItem.groupedContent
        return when (content) {
            is Choice -> this.createRhsForChoice(content, rule, arg)
            else -> {
                val groupRuleName = this._pseudoRuleNameGenerator.nameForRuleItem(grammarRuleItem)
                this.createRhs(rule, grammarRuleItem.groupedContent, groupRuleName)
            }
        }
    }

    private fun runtimeRuleForRuleItem(target: RuleItem, arg: String): RuntimeRule = when (target) {
        is EmptyRule -> RuntimeRuleSet.EMPTY
        is Terminal -> this.createRuleForRuleItemTerminal(target, arg)
        is NonTerminal -> this.createRuleForRuleItemNonTerminal(target, arg)
        is Embedded -> this.createRuleForRuleItemEmbedded(target, arg)
        is Choice -> this.createPseudoRuleForRuleItemChoice(target, arg)
        is OptionalItem -> this.createPseudoRuleForRuleItemOptional(target, arg)
        is SimpleList -> this.createPseudoRuleForRuleItemListSimple(target, arg)
        is SeparatedList -> this.createPseudoRuleForRuleItemListSeparated(target, arg)
        is Group -> this.createPseudoRuleForRuleItemGroup(target, arg)
        else -> error("${target::class} is not a supported subtype of RuleItem")
    }

    private fun createRuleForRuleItemTerminal(target: Terminal, arg: String): RuntimeRule {
        val existing = this.findTerminal(target.value)
        return if (null == existing) {
            val terminalRule = this.terminalRule(null, target.value, RuntimeRuleKind.TERMINAL, target.isPattern, false)
            this.recordOriginalRuleItem(terminalRule, target)
            terminalRule
        } else {
            existing
        }
    }

    private fun createRuleForRuleItemNonTerminal(target: NonTerminal, arg: String): RuntimeRule {
        val refName = target.ruleReference
        return findNamedRule(refName.value)
            ?: this.convertGrammarRule(target.referencedRule(this.grammar!!), arg)
    }

    private fun createRuleForRuleItemEmbedded(target: Embedded, arg: String): RuntimeRule {
        val existing = this.findEmbedded(target.embeddedGrammarReference.resolved!!, target.embeddedGoalName.value)
        return if (null == existing) {
            val embeddedRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)
            val embeddedRule = this.embeddedRule(embeddedRuleName, false, target.embeddedGrammarReference.resolved!!, target.embeddedGoalName.value)
            this.recordOriginalRuleItem(embeddedRule, target)
            embeddedRule
        } else {
            existing
        }
    }

    private fun createPseudoRuleForRuleItemChoice(target: Choice, arg: String): RuntimeRule {
        val chRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = createPseudoRule(target, chRuleName)
        val rhs = this.createRhsForChoice(target, nrule, chRuleName)
        nrule.setRhs(rhs)
        return nrule
    }

    private fun createPseudoRuleForRuleItemOptional(target: OptionalItem, arg: String): RuntimeRule {
        val optRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(optRuleName, false, true)
        nrule.setRhs(this.createRhsForOptional(target, nrule, optRuleName))
        this.recordOriginalRuleItem(nrule, target)
        return nrule
    }

    private fun createPseudoRuleForRuleItemListSimple(target: SimpleList, arg: String): RuntimeRule {
        val multiRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(multiRuleName, false, true)
        nrule.setRhs(this.createRhsForListSimple(target, nrule, multiRuleName))
        this.recordOriginalRuleItem(nrule, target)
        return nrule
    }

    private fun createPseudoRuleForRuleItemListSeparated(target: SeparatedList, arg: String): RuntimeRule {
        val sListRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSeparatedListRuleName(arg)
        val nrule = this.nextRule(sListRuleName, false, true)
        nrule.setRhs(this.createRhsForListSeparated(target, nrule, sListRuleName))
        this.recordOriginalRuleItem(nrule, target)
        return nrule
    }

    private fun createPseudoRuleForRuleItemGroup(target: Group, arg: String): RuntimeRule {
        val content = target.groupedContent
        return when (content) {
            is Choice -> createPseudoRuleForRuleItemChoice(content, arg)
            else -> {
                val grRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
                val nrule = createPseudoRule(target.groupedContent, grRuleName)
                val rhs = this.createRhs(nrule, target.groupedContent, grRuleName)
                nrule.setRhs(rhs)
                nrule
            }
        }
    }

    private fun createPseudoRule(target: RuleItem, psudeoRuleName: String): RuntimeRule {
        val nrule = this.nextRule(psudeoRuleName, false, true)
        this.recordOriginalRuleItem(nrule, target)
        return nrule
    }
}