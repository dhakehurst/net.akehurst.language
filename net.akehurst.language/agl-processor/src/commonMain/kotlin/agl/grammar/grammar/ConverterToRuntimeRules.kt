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
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull

/**
 * arg: String =
 */
internal class ConverterToRuntimeRules(
    val grammar: Grammar
) {

    private val _runtimeRuleSet = RuntimeRuleSet(RuntimeRuleSet.numberForGrammar[grammar])
    val runtimeRuleSet: RuntimeRuleSet by lazy {
        this.visitGrammar(grammar, "")
        _runtimeRuleSet.setRules(this.runtimeRules.values.toList())
        _runtimeRuleSet
    }

    fun originalRuleItemFor(runtimeRuleSetNumber: Int, runtimeRuleNumber: Int): RuleItem = this.originalRuleItem[Pair(runtimeRuleSetNumber, runtimeRuleNumber)]
        ?: throw LanguageProcessorException("Cannot find original item for ($runtimeRuleNumber,$runtimeRuleNumber)", null)

    // index by tag
    private val runtimeRules = mutableMapOf<String, RuntimeRule>()

    private val terminalRules = mutableMapOf<String, RuntimeRule>()
    private val embeddedRules = mutableMapOf<Pair<Grammar, String>, RuntimeRule>()
    private val originalRuleItem: MutableMap<Pair<Int, Int>, RuleItem> = mutableMapOf()
    private val embeddedConverters: LazyMutableMapNonNull<Grammar, ConverterToRuntimeRules> = lazyMutableMapNonNull { embeddedGrammar ->
        val embeddedConverter = ConverterToRuntimeRules(embeddedGrammar)
        embeddedConverter
    }

    private val _pseudoRuleNameGenerator = PseudoRuleNames(grammar)

    private fun nextRule(name: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        if (Debug.CHECK) check(this.runtimeRules.containsKey(name).not())
        val newRule = RuntimeRule(_runtimeRuleSet.number, runtimeRules.size, name, "", kind, isPattern, isSkip, null, null)
        runtimeRules[name] = newRule
        return newRule
    }

    private fun terminalRule(name: String?, value: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        val newRule = RuntimeRule(_runtimeRuleSet.number, runtimeRules.size, name, value, kind, isPattern, isSkip, null, null)
        check(this.runtimeRules.containsKey(newRule.tag).not()) { "Already got rule with tag '$name'" }
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
        val newRule = RuntimeRule(
            _runtimeRuleSet.number, runtimeRules.size, embeddedRuleName, "", RuntimeRuleKind.EMBEDDED,
            false, isSkip, embeddedRuntimeRuleSet, embeddedStartRuntimeRule
        )
        runtimeRules[newRule.tag] = newRule
        embeddedRules[Pair(embeddedGrammar, embeddedGoalRuleName)] = newRule
        return newRule
    }

    private fun createEmptyRuntimeRuleFor(tag: String): RuntimeRule {
        val ruleThatIsEmpty = this.findNamedRule(tag) ?: error("Should always exist")
        val en = "§empty.$tag"
        val emptyRuntimeRule = this.terminalRule(en, en, RuntimeRuleKind.TERMINAL, false, false)
        emptyRuntimeRule.rhsOpt = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(ruleThatIsEmpty))
        return emptyRuntimeRule
    }

    //private fun embedded(embeddedGrammar: Grammar, embeddedStartRule: GrammarRule): Pair<RuntimeRuleSet, RuntimeRule> {
    //     val embeddedConverter = embeddedConverters[embeddedGrammar]
    //     val embeddedStartRuntimeRule = embeddedConverter.runtimeRuleSet.findRuntimeRule(embeddedStartRule.name)
    //     return Pair(embeddedConverter.runtimeRuleSet, embeddedStartRuntimeRule)
    // }

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
        return target.allRule.map {
            this.visitRule(it, arg)
        }.toSet()
    }

    private fun visitRule(target: GrammarRule, arg: String): RuntimeRule {
        val rule = this.findNamedRule(target.name)
        return if (null == rule) {
            when {
                target.isLeaf -> this.buildCompressedRule(target, target.isSkip)
                target.isOneEmebedded -> {
                    val embeddedRuleName = target.name
                    val e = if(target.rhs is Embedded) {
                        target.rhs as Embedded
                    } else {
                         (target.rhs as Concatenation).items[0] as Embedded
                    }
                    val embeddedRule = this.embeddedRule(embeddedRuleName, false, e.embeddedGrammar, e.embeddedGoalName)
                    this.originalRuleItem[Pair(embeddedRule.runtimeRuleSetNumber, embeddedRule.ruleNumber)] = e
                    embeddedRule
                }
                else -> {
                    val nrule = this.nextRule(target.name, RuntimeRuleKind.NON_TERMINAL, false, target.isSkip)
                    this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target.rhs
                    val rhs = createRhs(target.rhs, target.name)
                    nrule.rhsOpt = rhs
                    nrule
                }
            }
        } else {
            rule
        }
    }

    private fun createRhs(target: RuleItem, arg: String): RuntimeRuleRhs = when (target) {
        is EmptyRule -> {
            val item = this.visitEmptyRule(target, arg)
            RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }

        is Terminal -> {
            val item = this.visitTerminal(target, arg)
            RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }

        //is Embedded -> this.createRhsForEmbedded(target, arg)

        is NonTerminal -> {
            val item = this.visitNonTerminal(target, arg)
            RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }

        is Group -> when (target.choice.alternative.size) {
            0 -> error("Should not happen")
            1 -> this.createRhs(target.choice.alternative[0], arg)
            else -> {
                val groupRuleName = this._pseudoRuleNameGenerator.nameForRuleItem(target)// this.createGroupRuleName(arg)
                this.createRhs(target.choice, groupRuleName)
            }
        }

        is Concatenation -> when (target.items.size) {
            0 -> error("Should not happen")
            1 -> this.createRhs(target.items[0], arg)
            else -> {
                val items = target.items.map { this.visitConcatenationItem(it, arg) }
                RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, items.toTypedArray())
            }
        }

        is Choice -> this.createRhsForChoice(target, arg)
        is SimpleList -> this.createRhsForSimpleList(target, arg)
        is SeparatedList -> this.createRhsForSeparatedList(target, arg)
        else -> error("Unsupported")
    }

    private fun visitConcatenationItem(target: ConcatenationItem, arg: String): RuntimeRule = when (target) {
        is SimpleList -> this.createPseudoRuleForSimpleList(target, arg)
        is SeparatedList -> this.createPseudoRuleForSeparatedList(target, arg)
        is SimpleItem -> this.visitSimpleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of ConcatenationItem")
    }

    private fun visitSimpleItem(target: SimpleItem, arg: String): RuntimeRule = when (target) {
        is Group -> this.visitGroup(target, arg)
        is TangibleItem -> this.visitTangibleItem(target, arg)
        else -> error("${target::class} is not a supported subtype of SimpleItem")
    }

    private fun visitTangibleItem(target: TangibleItem, arg: String): RuntimeRule = when (target) {
        is EmptyRule -> this.visitEmptyRule(target, arg)
        is NonTerminal -> this.visitNonTerminal(target, arg)
        is Terminal -> this.visitTerminal(target, arg)
        is Embedded -> this.visitEmbedded(target, arg)
        else -> error("${target::class} is not a supported subtype of TangibleItem")
    }

    private fun visitEmptyRule(target: EmptyRule, arg: String): RuntimeRule {
        val emptyRule = this.createEmptyRuntimeRuleFor(arg)
        this.originalRuleItem[Pair(emptyRule.runtimeRuleSetNumber, emptyRule.ruleNumber)] = target
        return emptyRule
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

    private fun visitEmbedded(target: Embedded, arg: String): RuntimeRule {
        val existing = this.findEmbedded(target.embeddedGrammar, target.embeddedGoalName)
        return if (null == existing) {
            val embeddedRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)
            val embeddedRule = this.embeddedRule(embeddedRuleName, false, target.embeddedGrammar, target.embeddedGoalName)
            this.originalRuleItem[Pair(embeddedRule.runtimeRuleSetNumber, embeddedRule.ruleNumber)] = target
            embeddedRule
        } else {
            existing
        }
    }

    private fun visitNonTerminal(target: NonTerminal, arg: String): RuntimeRule {
        val refName = target.name
        return findNamedRule(refName)
            ?: this.visitRule(target.referencedRule(this.grammar), arg)
    }

    private fun createPseudoRuleForChoice(target: Choice, psudeoRuleName: String): RuntimeRule {
        val nrule = this.nextRule(psudeoRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForChoice(target, psudeoRuleName)
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun visitChoiceAlternative(target: Concatenation, arg: String): RuntimeRule {
        return when {
            1 == target.items.size -> this.visitConcatenationItem(target.items[0], arg)
            else -> {
                val alternativeRuleName = this._pseudoRuleNameGenerator.nameForRuleItem(target)//this.createChoiceRuleName(arg)
                val items = target.items.map { this.visitConcatenationItem(it, arg) }
                val rr = this.nextRule(alternativeRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
                rr.rhsOpt = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, items.toTypedArray())
                this.originalRuleItem[Pair(rr.runtimeRuleSetNumber, rr.ruleNumber)] = target
                return rr
            }
        }
    }

    private fun visitGroup(target: Group, arg: String): RuntimeRule {
        return when (target.choice.alternative.size) {
            0 -> error("Should not happen")
            1 -> this.visitChoiceAlternative(target.choice.alternative[0], arg)
            else -> {
                val groupRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//createGroupRuleName(arg)
                this.createPseudoRuleForChoice(target.choice, groupRuleName)
            }
        }
    }

    private fun createPseudoRuleForEmbedded(target: Embedded, arg: String) {

    }

    private fun createPseudoRuleForSimpleList(target: SimpleList, arg: String): RuntimeRule {
        val multiRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(multiRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForSimpleList(target, multiRuleName)
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    private fun createPseudoRuleForSeparatedList(target: SeparatedList, arg: String): RuntimeRule {
        val listRuleName = _pseudoRuleNameGenerator.nameForRuleItem(target)//this.createSeparatedListRuleName(arg)
        val nrule = this.nextRule(listRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForSeparatedList(target, listRuleName)
        this.originalRuleItem[Pair(nrule.runtimeRuleSetNumber, nrule.ruleNumber)] = target
        return nrule
    }

    //private fun createRhsForEmbedded() {
    //    val item = this.visitEmbedded(target, arg)
    //    RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
    //}

    private fun createRhsForChoice(target: Choice, arg: String): RuntimeRuleRhs {
        return when (target.alternative.size) {
            1 -> createRhs(target.alternative[0], arg)
            else -> {
                val choiceKind = when (target) {
                    is ChoiceEqual -> RuntimeRuleChoiceKind.LONGEST_PRIORITY
                    is ChoicePriority -> RuntimeRuleChoiceKind.PRIORITY_LONGEST
                    is ChoiceAmbiguous -> RuntimeRuleChoiceKind.AMBIGUOUS
                    else -> throw RuntimeException("unsupported")
                }
                val items = target.alternative.map { this.visitChoiceAlternative(it, arg) }
                RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CHOICE, choiceKind, RuntimeRuleListKind.NONE, -1, 0, items.toTypedArray())
            }
        }
    }

    private fun createRhsForSimpleList(target: SimpleList, arg: String): RuntimeRuleRhs {
        val item = this.visitSimpleItem(target.item, arg)
        val items = when (target.min) {
            0 -> {
                val emptyRule = this.createEmptyRuntimeRuleFor(arg)
                this.originalRuleItem[Pair(emptyRule.runtimeRuleSetNumber, emptyRule.ruleNumber)] = target
                arrayOf(item, emptyRule)
            }

            else -> arrayOf(item)
        }
        return RuntimeRuleRhs(RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.MULTI, target.min, target.max, items)
    }

    private fun createRhsForSeparatedList(target: SeparatedList, arg: String): RuntimeRuleRhs {
        val item = this.visitSimpleItem(target.item, arg)
        val separator = this.visitSimpleItem(target.separator, arg)
        val kind = RuntimeRuleListKind.SEPARATED_LIST
        //val kind = when (target.associativity) {
        //     SeparatedListKind.Flat -> RuntimeRuleListKind.SEPARATED_LIST
        //    SeparatedListKind.Left -> RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST
        //     SeparatedListKind.Right -> RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST
        // }
        val items = when (target.min) {
            0 -> {
                val emptyRule = this.createEmptyRuntimeRuleFor(arg)
                this.originalRuleItem[Pair(emptyRule.runtimeRuleSetNumber, emptyRule.ruleNumber)] = target
                arrayOf(item, separator, emptyRule)
            }

            else -> arrayOf(item, separator)
        }
        return RuntimeRuleRhs(RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, kind, target.min, target.max, items)
    }
}