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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.analyser.GrammarExeception
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.collections.LazyMapNonNull
import net.akehurst.language.collections.lazyMapNonNull

/**
 * arg: String =
 */
internal class ConverterToRuntimeRules(
    val grammar: Grammar
) {

    class CompressedItem(val value: String, val isPattern: Boolean)

    private val _runtimeRuleSet= RuntimeRuleSet()
    val runtimeRuleSet: RuntimeRuleSet by lazy {
        this.visitGrammar(grammar,"")
        _runtimeRuleSet.setRules(this.runtimeRules.values.toList())
        _runtimeRuleSet
    }

    fun originalRuleItemFor(rr: RuntimeRule): RuleItem = this.originalRuleItem.get(rr.tag)
        ?: throw LanguageProcessorException("Cannot find original item for $rr", null)

    // index by tag
    private val runtimeRules = mutableMapOf<String, RuntimeRule>()
    // index by value
    private val terminalRules = mutableMapOf<String, RuntimeRule>()
    private val originalRuleItem: MutableMap<String, RuleItem> = mutableMapOf()
    private val embeddedConverters: LazyMapNonNull<Grammar, ConverterToRuntimeRules> = lazyMapNonNull { embeddedGrammar ->
        val embeddedConverter = ConverterToRuntimeRules(embeddedGrammar)
        embeddedConverter
    }
    private var nextGroupNumber = 0
    private var nextChoiceNumber = 0
    private var nextSimpleListNumber = 0
    private var nextSeparatedListNumber = 0

    private fun createGroupRuleName(parentRuleName: String): String = "§${parentRuleName}§group" + this.nextGroupNumber++ //TODO: include original rule name fo easier debug

    private fun createChoiceRuleName(parentRuleName: String): String = "§${parentRuleName}§choice" + this.nextChoiceNumber++ //TODO: include original rule name fo easier debug

    private fun createSimpleListRuleName(parentRuleName: String): String = "§${parentRuleName}§multi" + this.nextSimpleListNumber++ //TODO: include original rule name fo easier debug

    private fun createSeparatedListRuleName(parentRuleName: String): String = "§${parentRuleName}§sList" + this.nextSeparatedListNumber++ //TODO: include original rule name fo easier debug

    private fun nextRule(tag: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        check(this.runtimeRules.containsKey(tag).not())
        val newRule = RuntimeRule(_runtimeRuleSet.number, runtimeRules.size, tag, "", kind, isPattern, isSkip, null, null)
        runtimeRules[tag] = newRule
        return newRule
    }
    private fun terminalRule(tag: String, value: String, kind: RuntimeRuleKind, isPattern: Boolean, isSkip: Boolean): RuntimeRule {
        check(this.terminalRules.containsKey(value).not())
        val newRule = RuntimeRule(_runtimeRuleSet.number, runtimeRules.size, tag, value, kind, isPattern, isSkip, null, null)
        runtimeRules[tag] = newRule
        terminalRules[value] = newRule
        return newRule
    }
    private fun embeddedRule(tag: String,isSkip: Boolean,embeddedRuntimeRuleSet:RuntimeRuleSet, embeddedStartRule:RuntimeRule): RuntimeRule {
        check(this.runtimeRules.containsKey(tag).not())
        val newRule = RuntimeRule(_runtimeRuleSet.number, runtimeRules.size, tag, "", RuntimeRuleKind.EMBEDDED, false, isSkip, embeddedRuntimeRuleSet, embeddedStartRule)
        runtimeRules[tag] = newRule
        return newRule
    }

    private fun createEmptyRuntimeRuleFor(tag: String): RuntimeRule {
        val ruleThatIsEmpty = this.findRule(tag) ?: error("Should always exist")
        val en = "§empty.$tag"
        val emptyRuntimeRule = this.terminalRule(en, en, RuntimeRuleKind.TERMINAL, false, false)
        emptyRuntimeRule.rhsOpt = RuntimeRuleItem(RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, 0, 0, arrayOf(ruleThatIsEmpty))
        return emptyRuntimeRule
    }

    private fun embedded(embeddedGrammar: Grammar, embeddedStartRule: Rule): Pair<RuntimeRuleSet, RuntimeRule> {
        val embeddedConverter = embeddedConverters[embeddedGrammar]
        val embeddedStartRuntimeRule = embeddedConverter.runtimeRuleSet.findRuntimeRule(embeddedStartRule.name)
        return Pair(embeddedConverter.runtimeRuleSet, embeddedStartRuntimeRule)
    }

    private fun findRule(tag: String): RuntimeRule? =this.runtimeRules[tag]

    private fun findTerminal(value: String): RuntimeRule? =this.terminalRules[value]

    private fun toRegEx(value: String): String {
        return Regex.escape(value)
    }

    private fun compressRhs(rhs: RuleItem): CompressedItem {
        return when {
            rhs is Terminal -> when {
                rhs.isPattern -> CompressedItem("(${rhs.value})", true)
                else -> CompressedItem("(${toRegEx(rhs.value)})", true)
            }
            rhs is Concatenation -> {
                if (1 == rhs.items.size) {
                    this.compressRhs(rhs.items[0])
                } else {
                    val items = rhs.items.map { this.compressRhs(it) }
                    val pattern = items.joinToString(separator = "") { "(${it.value})" }
                    CompressedItem(pattern, true)
                    //throw GrammarExeception("Rule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
                }
            }
            rhs is Choice -> {
                val ct = rhs.alternative.map { this.compressRhs(it) }
                val pattern = ct.joinToString(separator = "|") { "${it.value}" }
                CompressedItem(pattern, true)
            }
            rhs is SimpleList -> {
                val ct = this.compressRhs(rhs.item)
                val min = rhs.min
                val max = if (-1 == rhs.max) "" else rhs.max
                val pattern = "(${ct.value}){${min},${max}}"
                CompressedItem(pattern, true)
            }
            rhs is Group -> {
                val ct = this.compressRhs(rhs.choice)
                val pattern = "(${ct.value})"
                CompressedItem(pattern, true)
            }
            rhs is NonTerminal -> {
                //TODO: handle overridden vs embedded rules!
                //TODO: need to catch the recursion before this
                this.compressRhs(rhs.referencedRule.rhs)
            }
            else -> throw GrammarExeception("Rule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
        }
    }

    private fun buildCompressedRule(target: Rule, isSkip: Boolean): RuntimeRule {
        val ci = this.compressRhs(target.rhs)
        val rule = if (ci.isPattern) {
            this.terminalRule(target.name, ci.value, RuntimeRuleKind.TERMINAL, true, isSkip)
        } else {
            this.terminalRule(target.name, ci.value, RuntimeRuleKind.TERMINAL, false, isSkip)
        }
        this.originalRuleItem.put(target.name, target.rhs)
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

    private fun visitRule(target: Rule, arg: String): RuntimeRule {
        val rule = this.findRule(target.name)
        return if (null == rule) {
            when {
                target.isLeaf -> this.buildCompressedRule(target, target.isSkip)
                else -> {
                    val nrule = this.nextRule(target.name, RuntimeRuleKind.NON_TERMINAL, false, target.isSkip)
                    this.originalRuleItem.put(nrule.tag, target.rhs)
                    val rhs = createRhs(target.rhs, target.name)
                    nrule.rhsOpt = rhs
                    nrule
                }
            }
        } else {
            rule
        }
    }

    private fun createRhs(target: RuleItem, arg: String): RuntimeRuleItem = when (target) {
        is EmptyRule -> {
            val item = this.visitEmptyRule(target, arg)
            RuntimeRuleItem(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }
        is Terminal -> {
            val item = this.visitTerminal(target, arg)
            RuntimeRuleItem(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }
        is NonTerminal -> {
            val item = this.visitNonTerminal(target, arg)
            RuntimeRuleItem(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(item))
        }
        is Group -> when (target.choice.alternative.size) {
            0 -> error("Should not happen")
            1 -> this.createRhs(target.choice.alternative[0], arg)
            else -> {
                val groupRuleName = this.createGroupRuleName(arg)
                this.createRhs(target.choice, groupRuleName)
            }
        }

        is Concatenation -> {
            val items = target.items.map { this.visitConcatenationItem(it, arg) }
            RuntimeRuleItem(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, items.toTypedArray())
        }

        is Choice -> this.createRhsForChoice(target,arg)
        is SimpleList -> this.createRhsForSimpleList(target,arg)
        is SeparatedList -> this.createRhsForSeparatedList(target,arg)
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
        else -> error("${target::class} is not a supported subtype of TangibleItem")
    }

    private fun visitEmptyRule(target: EmptyRule, arg: String): RuntimeRule {
        val emptyRuleItem = this.createEmptyRuntimeRuleFor(arg)
        this.originalRuleItem.put(emptyRuleItem.tag, target)
        return emptyRuleItem
    }

    private fun visitTerminal(target: Terminal, arg: String): RuntimeRule {
        val existing = this.findTerminal(target.value)
        if (null == existing) {
            val terminalRule = if (target.isPattern) {
                this.terminalRule(target.name, target.value, RuntimeRuleKind.TERMINAL, true, false)
            } else {
                this.terminalRule(target.name, target.value, RuntimeRuleKind.TERMINAL, false, false)
            }
            this.originalRuleItem.put(terminalRule.tag, target)
            return terminalRule
        } else {
            return existing
        }
    }

    private fun visitNonTerminal(target: NonTerminal, arg: String): RuntimeRule {
        val refName = target.name
        return findRule(refName)
            ?: if (target.embedded) {
                val embeddedGrammar = target.referencedRule.grammar
                val (embeddedRuleSet, embeddedStartRule) = this.embedded(embeddedGrammar, target.referencedRule)
                this.embeddedRule(target.name, false,embeddedRuleSet, embeddedStartRule)
            } else {
                val r = this.grammar.findAllRule(refName)
                this.visitRule(r, arg)
            }
    }

    private fun createPseudoRuleForChoice(target: Choice, psudeoRuleName: String): RuntimeRule {
        val nrule = this.nextRule(psudeoRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForChoice(target,psudeoRuleName)
        this.originalRuleItem[nrule.tag] = target
        return nrule
    }

    private fun visitConcatenation(target: Concatenation, arg: String): RuntimeRule {
        return when {
            1 == target.items.size -> this.visitConcatenationItem(target.items[0], arg)
            else -> {
                val alternativeRuleName = this.createChoiceRuleName(arg)
                val items = target.items.map { this.visitConcatenationItem(it, arg) }
                val rr = this.nextRule(alternativeRuleName,RuntimeRuleKind.NON_TERMINAL,false,false)
                rr.rhsOpt = RuntimeRuleItem(RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE,-1,0,items.toTypedArray())
                this.originalRuleItem.put(rr.tag, target)
                return rr
            }
        }
    }

    private fun visitGroup(target: Group, arg: String): RuntimeRule {
        return when (target.choice.alternative.size) {
            0 -> error("Should not happen")
            1 -> this.visitConcatenation(target.choice.alternative[0], arg)
            else -> {
                val groupRuleName = this.createGroupRuleName(arg)
                this.createPseudoRuleForChoice(target.choice, groupRuleName)
            }
        }
    }

    private fun createPseudoRuleForSimpleList(target: SimpleList, arg: String): RuntimeRule {
        val multiRuleName = this.createSimpleListRuleName(arg)
        val nrule = this.nextRule(multiRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForSimpleList(target, multiRuleName)
        this.originalRuleItem[nrule.tag] = target
        return nrule
    }

    private fun createPseudoRuleForSeparatedList(target: SeparatedList, arg: String): RuntimeRule {
        val listRuleName = this.createSeparatedListRuleName(arg)
        val nrule = this.nextRule(listRuleName, RuntimeRuleKind.NON_TERMINAL, false, false)
        nrule.rhsOpt = this.createRhsForSeparatedList(target, listRuleName)
        this.originalRuleItem[nrule.tag] = target
        return nrule
    }

    private fun createRhsForChoice(target: Choice, arg: String): RuntimeRuleItem {
        val choiceKind = when (target) {
            is ChoiceEqual -> RuntimeRuleChoiceKind.LONGEST_PRIORITY
            is ChoicePriority -> RuntimeRuleChoiceKind.PRIORITY_LONGEST
            is ChoiceAmbiguous -> RuntimeRuleChoiceKind.AMBIGUOUS
            else -> throw RuntimeException("unsupported")
        }
        val items = target.alternative.map { this.visitConcatenation(it, arg) }
        return RuntimeRuleItem(RuntimeRuleRhsItemsKind.CHOICE, choiceKind, RuntimeRuleListKind.NONE, -1, 0, items.toTypedArray())
    }

    private fun createRhsForSimpleList(target: SimpleList, arg: String): RuntimeRuleItem {
        val item = this.visitSimpleItem(target.item, arg)
        val items = when (target.min) {
            0 -> arrayOf(item, createEmptyRuntimeRuleFor(arg))
            else -> arrayOf(item)
        }
        return RuntimeRuleItem(RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.MULTI, target.min, target.max, items)
    }

    private fun createRhsForSeparatedList(target: SeparatedList, arg: String): RuntimeRuleItem {
        val item = this.visitSimpleItem(target.item, arg)
        val separator = this.visitSimpleItem(target.separator, arg)
        val kind = when(target.associativity) {
            SeparatedListKind.Flat -> RuntimeRuleListKind.SEPARATED_LIST
            SeparatedListKind.Left -> RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST
            SeparatedListKind.Right -> RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST
        }
        val items = when (target.min) {
            0 -> arrayOf(item, separator, createEmptyRuntimeRuleFor(arg))
            else -> arrayOf(item, separator)
        }
        return RuntimeRuleItem(RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, kind, target.min, target.max, items)
    }
}