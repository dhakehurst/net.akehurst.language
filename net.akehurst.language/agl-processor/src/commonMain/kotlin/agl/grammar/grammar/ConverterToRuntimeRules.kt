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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.agl.grammar.grammar.PseudoRuleNames
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull

/**
 * arg: String =
 */
internal class ConverterToRuntimeRules(
    val grammar: Grammar
) {

    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    private val _ruleSetNumber by lazy { RuntimeRuleSet.numberForGrammar[grammar] }
    val runtimeRuleSet: RuntimeRuleSet by lazy {
        this.visitGrammar(grammar, "")
        val rules = this.runtimeRules.values.toList()
        RuntimeRuleSet(_ruleSetNumber, rules, _precRules)
    }

    fun originalRuleItemFor(runtimeRuleSetNumber: Int, runtimeRuleNumber: Int): RuleItem = this.originalRuleItem[Pair(runtimeRuleSetNumber, runtimeRuleNumber)]
        ?: throw LanguageProcessorException("Cannot find original item for ($runtimeRuleSetNumber,$runtimeRuleNumber)", null)

    // index by tag
    private val runtimeRules = mutableMapOf<String, RuntimeRule>()

    private var _precRules = emptyList<RuntimePreferenceRule>()

    private val terminalRules = mutableMapOf<String, RuntimeRule>()
    private val embeddedRules = mutableMapOf<Pair<Grammar, String>, RuntimeRule>()
    private val originalRuleItem: MutableMap<Pair<Int, Int>, RuleItem> = mutableMapOf()
    private val embeddedConverters: LazyMutableMapNonNull<Grammar, ConverterToRuntimeRules> = lazyMutableMapNonNull { embeddedGrammar ->
        val embeddedConverter = ConverterToRuntimeRules(embeddedGrammar)
        embeddedConverter
    }

    private val _pseudoRuleNameGenerator by lazy { PseudoRuleNames(grammar) }

    private fun nextRule(name: String, isSkip: Boolean): RuntimeRule {
        if (Debug.CHECK) check(this.runtimeRules.containsKey(name).not())
        val newRule = RuntimeRule(_ruleSetNumber, runtimeRules.size, name, isSkip)
        runtimeRules[name] = newRule
        return newRule
    }

    private fun terminalRule(name: String?, value: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        val newRule = RuntimeRule(_ruleSetNumber, runtimeRules.size, name, isSkip).also {
            if (isPattern) {
                it.setRhs(RuntimeRuleRhsPattern(it, value))
            } else {
                it.setRhs(RuntimeRuleRhsLiteral(it, value))
            }
        }
        if (Debug.CHECK) check(this.runtimeRules.containsKey(newRule.tag).not()) { "Already got rule with tag '$name'" }
//TODO: warn in analyser        check(this.terminalRules.containsKey(value).not()) { "Already got terminal rule with value '$value'" }
        runtimeRules[newRule.tag] = newRule
        terminalRules[value] = newRule
        return newRule
    }

    private fun embeddedRule(embeddedRuleName: String, isSkip: Boolean, embeddedGrammar: Grammar, embeddedGoalRuleName: String): RuntimeRule {
        if (Debug.CHECK) check(this.runtimeRules.containsKey(embeddedRuleName).not())
        val embeddedConverter = embeddedConverters[embeddedGrammar]
        val embeddedRuntimeRuleSet = embeddedConverter.runtimeRuleSet
        val embeddedStartRuntimeRule = embeddedRuntimeRuleSet.findRuntimeRule(embeddedGoalRuleName)
        val newRule = RuntimeRule(_ruleSetNumber, runtimeRules.size, embeddedRuleName, isSkip).also {
            it.setRhs(RuntimeRuleRhsEmbedded(it, embeddedRuntimeRuleSet, embeddedStartRuntimeRule))
        }
        runtimeRules[newRule.tag] = newRule
        embeddedRules[Pair(embeddedGrammar, embeddedGoalRuleName)] = newRule
        return newRule
    }

    private fun findNamedRule(name: String): RuntimeRule? = this.runtimeRules[name]

    private fun findTerminal(value: String): RuntimeRule? = this.terminalRules[value]

    private fun findEmbedded(grammar: Grammar, goalRuleName: String): RuntimeRule? = this.embeddedRules[Pair(grammar, goalRuleName)]

    private fun buildCompressedRule(target: GrammarRule, isSkip: Boolean): RuntimeRule {
        val ci = target.compressedLeaf
        val rule = if (ci.isPattern) {
            this.terminalRule(target.name, ci.value, RuntimeRuleKind.TERMINAL, true, isSkip)
        } else {
            this.terminalRule(target.name, ci.value, RuntimeRuleKind.TERMINAL, false, isSkip)
        }
        this.originalRuleItem[Pair(rule.runtimeRuleSetNumber, rule.ruleNumber)] = target.rhs
        return rule
    }

    private fun visitNamespace(target: Namespace, arg: String): Set<RuntimeRule> {
        return emptySet()
    }

    private fun visitGrammar(target: Grammar, arg: String): Set<RuntimeRule> {

        val rules = target.allResolvedGrammarRule.map {
            this.visitGrammarRule(it, arg)
        }.toSet()
        _precRules = target.allResolvedPreferenceRuleRule.map {
            this.visitPreferenceRule(it, arg)
        }
        return rules
    }

    private fun visitPreferenceRule(target: PreferenceRule, arg: String): RuntimePreferenceRule {
        val forItem = target.forItem
        val contextRule = when (forItem) {
            is NonTerminal -> findNamedRule(forItem.name)!!
            is Embedded -> TODO()
            is Terminal -> findTerminal(forItem.name)!!
            else -> error("Internal Error: subtype '${forItem::class.simpleName}' of SimpleItem not handled")
        }
        val options = target.optionList.mapIndexed { idx, it ->
            val prec = idx
            val tgt = findNamedRule(it.item.name)!!
            val opt = it.choiceNumber
            val terminals = it.onTerminals.map {
                when (it) {
                    is Terminal -> findTerminal(it.value) ?: error("Terminal '${it.value}' not found")
                    is NonTerminal -> {
                        val r = findNamedRule(it.name)
                        when {
                            null == r -> error("Rule named '${it.name}' not found")
                            r.isTerminal.not() -> error("Rule named '${it.name}' is not a terminal")
                            else -> r
                        }
                    }

                    is Embedded -> TODO()
                    else -> error("Internal Error: subtype '${forItem::class.simpleName}' of SimpleItem not handled")
                }
            }.toSet()
            val assoc = when (it.associativity) {
                PreferenceOption.Associativity.LEFT -> RuntimePreferenceRule.Assoc.LEFT
                PreferenceOption.Associativity.RIGHT -> RuntimePreferenceRule.Assoc.RIGHT
            }
            RuntimePreferenceRule.RuntimePreferenceOption(prec, tgt, opt, terminals, assoc)
        }
        return RuntimePreferenceRule(contextRule, options)
    }

    private fun visitGrammarRule(target: GrammarRule, arg: String): RuntimeRule {
        val rule = this.findNamedRule(target.name)
        val rhs = target.rhs
        return if (null == rule) {
            when {
                target.isLeaf -> when {
                    rhs is Terminal -> this.terminalRule(target.name, rhs.value, RuntimeRuleKind.TERMINAL, rhs.isPattern, target.isSkip)
                    rhs is Concatenation && rhs.items.size == 1 && rhs.items[0] is Terminal -> {
                        val t = (rhs.items[0] as Terminal)
                        this.terminalRule(target.name, t.value, RuntimeRuleKind.TERMINAL, t.isPattern, target.isSkip)
                    }

                    rhs is Choice && rhs.alternative.size == 1 && rhs.alternative[0] is Terminal -> {
                        val t = (rhs.alternative[0] as Terminal)
                        this.terminalRule(target.name, t.value, RuntimeRuleKind.TERMINAL, t.isPattern, target.isSkip)
                    }

                    else -> this.buildCompressedRule(target, target.isSkip)
                }

                target.isOneEmebedded -> {
                    val embeddedRuleName = target.name
                    val e = if (target.rhs is Embedded) {
                        target.rhs as Embedded
                    } else {
                        (target.rhs as Concatenation).items[0] as Embedded
                    }
                    val embeddedRule = this.embeddedRule(embeddedRuleName, false, e.embeddedGrammarReference.resolved!!, e.embeddedGoalName)
                    this.originalRuleItem[Pair(embeddedRule.runtimeRuleSetNumber, embeddedRule.ruleNumber)] = e
                    embeddedRule
                }

                else -> {
                    val nrule = this.nextRule(target.name, target.isSkip)
                    this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target.rhs
                    val rrhs = createRhs(nrule, target.rhs, target.name)
                    nrule.setRhs(rrhs)
                    nrule
                }
            }
        } else {
            rule
        }
    }

    private fun createRhs(rule: RuntimeRule, target: RuleItem, arg: String): RuntimeRuleRhs = when (target) {
        is EmptyRule -> {
            RuntimeRuleRhsConcatenation(rule, listOf(RuntimeRuleSet.EMPTY))
        }

        is Terminal -> {
            val item = this.visitTerminal(target, arg)
            RuntimeRuleRhsConcatenation(rule, listOf(item))
        }

        //is Embedded -> this.createRhsForEmbedded(target, arg)

        is NonTerminal -> {
            val item = this.visitNonTerminal(target, arg)
            RuntimeRuleRhsConcatenation(rule, listOf(item))
        }

        // need to allow r = A* to have r as a list, so that preference disambiguation is clear.
        is Concatenation -> {
            if (Debug.CHECK) check(target.items.size > 1)
            val items = target.items.map { this.visitConcatenationItem(it, arg) }
            RuntimeRuleRhsConcatenation(rule, items)
        }

        is Choice -> this.createRhsForChoice(rule, target, arg)
        is OptionalItem -> this.createRhsForOptional(rule, target, arg)
        is SimpleList -> this.createRhsForSimpleList(rule, target, arg)
        is SeparatedList -> this.createRhsForSeparatedList(rule, target, arg)

        is Group -> {
            val content = target.groupedContent
            when (content) {
                is Choice -> this.createRhsForChoice(rule, content, arg)
                else -> {
                    val groupRuleName = this._pseudoRuleNameGenerator.nameForRuleItem(target)
                    this.createRhs(rule, target.groupedContent, groupRuleName)
                }
            }
        }

        else -> error("Unsupported")
    }

    private fun visitConcatenationItem(target: ConcatenationItem, arg: String): RuntimeRule = when (target) {
        is OptionalItem -> this.createPseudoRuleForOptionalItem(target, arg)
        is SimpleList -> this.createPseudoRuleForSimpleList(target, arg)
        is SeparatedList -> this.createPseudoRuleForSeparatedList(target, arg)
        is SimpleItem -> this.runtimeRuleForSimpleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of ConcatenationItem")
    }

    private fun runtimeRuleForSimpleItem(target: SimpleItem, arg: String): RuntimeRule = when (target) {
        is Group -> this.createPseudoRuleForGroup(target, arg)
        is TangibleItem -> this.runtimeRuleForTangibleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of SimpleItem")
    }

    private fun runtimeRuleForTangibleItem(target: TangibleItem, arg: String): RuntimeRule = when (target) {
        is EmptyRule -> RuntimeRuleSet.EMPTY
        is Terminal -> this.visitTerminal(target, arg)
        is NonTerminal -> this.visitNonTerminal(target, arg)
        is Embedded -> this.visitEmbedded(target, arg)
        else -> error("${target::class} is not a supported subtype of TangibleItem")
    }

    private fun visitTerminal(target: Terminal, arg: String): RuntimeRule {
        val existing = this.findTerminal(target.value)
        return if (null == existing) {
            val terminalRule = this.terminalRule(null, target.value, RuntimeRuleKind.TERMINAL, target.isPattern, false)
            this.originalRuleItem[Pair(terminalRule.runtimeRuleSetNumber, terminalRule.ruleNumber)] = target
            terminalRule
        } else {
            existing
        }
    }

    private fun visitNonTerminal(target: NonTerminal, arg: String): RuntimeRule {
        val refName = target.name
        return findNamedRule(refName)
            ?: this.visitGrammarRule(target.referencedRule(this.grammar!!), arg)
    }

    private fun visitEmbedded(target: Embedded, arg: String): RuntimeRule {
        val existing = this.findEmbedded(target.embeddedGrammarReference.resolved!!, target.embeddedGoalName)
        return if (null == existing) {
            val embeddedRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)
            val embeddedRule = this.embeddedRule(embeddedRuleName, false, target.embeddedGrammarReference.resolved!!, target.embeddedGoalName)
            this.originalRuleItem[Pair(embeddedRule.runtimeRuleSetNumber, embeddedRule.ruleNumber)] = target
            embeddedRule
        } else {
            existing
        }
    }

    private fun createRhsForChoiceAlternative(rule: RuntimeRule, target: RuleItem, arg: String): RuntimeRuleRhsConcatenation {
        val items = when (target) {
            is Concatenation -> target.items.map { this.visitConcatenationItem(it, arg) }
            is ConcatenationItem -> listOf(visitConcatenationItem(target, arg))
            else -> TODO()
        }
        return RuntimeRuleRhsConcatenation(rule, items)
    }

    private fun createPseudoRuleForGroup(target: Group, arg: String): RuntimeRule {
        val content = target.groupedContent
        return when (content) {
            is Choice -> {
                val choiceRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)
                val cRule = this.createPseudoRule(content, choiceRuleName)
                val rhs = createRhsForChoice(cRule, content, arg)
                cRule.setRhs(rhs)
                cRule
            }

            else -> {
                val groupRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)
                val gRule = this.createPseudoRule(content, groupRuleName)
                val rhs = createRhs(gRule, content, arg)
                gRule.setRhs(rhs)
                gRule
            }
        }
    }

    private fun createPseudoRule(target: RuleItem, psudeoRuleName: String): RuntimeRule {
        val nrule = this.nextRule(psudeoRuleName, false)
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun createPseudoRuleForOptionalItem(target: OptionalItem, arg: String): RuntimeRule {
        val optRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(optRuleName, false)
        nrule.setRhs(this.createRhsForOptional(nrule, target, optRuleName))
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun createPseudoRuleForSimpleList(target: SimpleList, arg: String): RuntimeRule {
        val multiRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(multiRuleName, false)
        nrule.setRhs(this.createRhsForSimpleList(nrule, target, multiRuleName))
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun createPseudoRuleForSeparatedList(target: SeparatedList, arg: String): RuntimeRule {
        val listRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSeparatedListRuleName(arg)
        val nrule = this.nextRule(listRuleName, false)
        nrule.setRhs(this.createRhsForSeparatedList(nrule, target, listRuleName))
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun createRhsForChoice(rule: RuntimeRule, target: Choice, arg: String): RuntimeRuleRhs {
        return when (target.alternative.size) {
            1 -> error("Internal Error: choice should have more than one alternative") //createRhs(rule, target.alternative[0], arg)
            else -> {
                val choiceKind = when (target) {
                    is ChoiceEqual -> RuntimeRuleChoiceKind.LONGEST_PRIORITY
                    is ChoicePriority -> RuntimeRuleChoiceKind.PRIORITY_LONGEST
                    is ChoiceAmbiguous -> RuntimeRuleChoiceKind.AMBIGUOUS
                    else -> throw RuntimeException("unsupported")
                }
                val items = target.alternative.map { this.createRhsForChoiceAlternative(rule, it, arg) }
                RuntimeRuleRhsChoice(rule, choiceKind, items)
            }
        }
    }

    private fun createRhsForOptional(rule: RuntimeRule, target: OptionalItem, arg: String): RuntimeRuleRhs {
        val item = this.runtimeRuleForSimpleItem(target.item, arg)
        return RuntimeRuleRhsListSimple(rule, 0, 1, item)
    }

    private fun createRhsForSimpleList(rule: RuntimeRule, target: SimpleList, arg: String): RuntimeRuleRhs {
        val item = this.runtimeRuleForSimpleItem(target.item, arg)
        return RuntimeRuleRhsListSimple(rule, target.min, target.max, item)
    }

    private fun createRhsForSeparatedList(rule: RuntimeRule, target: SeparatedList, arg: String): RuntimeRuleRhs {
        val item = this.runtimeRuleForSimpleItem(target.item, arg)
        val separator = this.runtimeRuleForSimpleItem(target.separator, arg)
        return RuntimeRuleRhsListSeparated(rule, target.min, target.max, item, separator)
    }
}