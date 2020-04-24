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

package net.akehurst.language.agl.grammar.runtime

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.analyser.GrammarExeception

/**
 * arg: String =
 */
class ConverterToRuntimeRules(val grammar: Grammar) : GrammarVisitor<Any, String> {

    class CompressedItem(val value: String, val isPattern: Boolean)

    private val originalRule: MutableMap<RuntimeRule, RuleItem> = mutableMapOf()
    val builder = RuntimeRuleSetBuilder()

    private fun findRule(name: String): RuntimeRule? {
        return this.builder.findRuleByName(name, false)
    }

    private fun findTerminal(value: String): RuntimeRule? {
        return this.builder.findRuleByName(value, true)
    }

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
                    val pattern = items.joinToString(separator = "") { it.value }
                    CompressedItem(pattern, true)
                    //throw GrammarExeception("Rule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
                }
            }
            rhs is Choice -> {
                val ct = rhs.alternative.map { this.compressRhs(it) }
                val pattern = ct.joinToString(separator = "|") { it.value }
                CompressedItem(pattern, true)
            }
            rhs is Multi -> {
                val ct = this.compressRhs(rhs.item)
                val min = rhs.min
                val max = if (-1==rhs.max) "" else rhs.max
                val pattern = "(${ct.value}){${min},${max}}"
                CompressedItem(pattern, true)
            }
            rhs is Group -> {
                val ct = this.compressRhs(rhs.choice)
                val pattern = "(${ct.value})"
                CompressedItem(pattern, true)
            }
            rhs is NonTerminal -> this.compressRhs(rhs.referencedRule.rhs) //TODO: need to catch the recursion before this
            else -> throw GrammarExeception("Rule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
        }
    }

    private fun buildCompressedRule(target: Rule, isSkip: Boolean): RuntimeRule {
        val ci = this.compressRhs(target.rhs)
        val rule = if (ci.isPattern) {
            this.builder.pattern(target.name, ci.value, isSkip)
        } else {
            this.builder.literal(target.name, ci.value, isSkip)
        }
        this.originalRule.put(rule, target.rhs)
        return rule
    }

    fun originalRuleItemFor(rr: RuntimeRule): RuleItem {
        return this.originalRule.get(rr)
                ?: throw LanguageProcessorException("cannot find original item for " + rr, null)
        /*
        val name = rr.name
        if (name.startsWith("§")) {
            // decode it (see Converter) and RuleItem.setOwningRule
            val split = name.split("[.]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val ruleName = split[0].substring(1)
            val rhs = grammar.findAllRule(ruleName).rhs
            val type = split[1]
            val index = IntArray(split.size - 3)
            for (i in 3 until split.size) {
                val ix = split[i].toInt()
                index[i - 3] = ix
            }
            var item = rhs
            for (i in index) {
                item = item.subItem(i)
            }

            return item
        } else {
            // find grammar rule
            return NonTerminalRuleReference(grammar, name)
        }
        */
    }

    fun transform(): RuntimeRuleSet {
        this.visit(this.grammar, "")
        return this.builder.ruleSet()
    }

    override fun visit(target: Namespace, arg: String): Set<RuntimeRule> {
        return emptySet()
    }

    override fun visit(target: Grammar, arg: String): Set<RuntimeRule> {
        return target.allRule.map {
            it.accept(this, arg) as RuntimeRule
        }.toSet()
    }

    override fun visit(target: Rule, arg: String): RuntimeRule {
        val rule = this.findRule(target.name)
        return if (null == rule) {
            when {
                target.isLeaf -> this.buildCompressedRule(target, target.isSkip)
                else -> {
                    val nrule = this.builder.rule(target.name).skip(target.isSkip).build()
                    this.originalRule.put(nrule, target.rhs)
                    //need to get back RuntimeRuleItems here,
                    // then set the rhs accordingly

                    val rhs = createRhs(target.rhs, target.name)
                    //val rhs =  target.rhs.accept(this, target.name) as RuntimeRule
                    nrule.rhsOpt = rhs
                    nrule
                }
            }
        } else {
            rule
        }
    }

    private fun createRhs(target: RuleItem, arg: String): RuntimeRuleItem {
        return when {
            (target is Choice && target.alternative.size == 1) -> {
                // only one choice, so can create a concatination
                val rhsItem = target.alternative[0]
                val items = rhsItem.items.map { it.accept(this, arg) as RuntimeRule }
                RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, -1, 0, items.toTypedArray())
            }
            (target is Choice) -> {
                val items = target.alternative.map {
                    if (it.items.size == 1) {
                        it.items[0].accept(this, arg) as RuntimeRule
                    } else {
                        val thisChoiceName = builder.createChoiceRuleName(arg)
                        val thisChoiceItems = it.items.map { it.accept(this, thisChoiceName) as RuntimeRule }
                        builder.rule(thisChoiceName).concatenation(*thisChoiceItems.toTypedArray())
                    }
                }
                val kind = RuntimeRuleItemKind.CHOICE
                val choiceKind = when (target) {
                    is ChoiceEqual -> RuntimeRuleChoiceKind.LONGEST_PRIORITY
                    is ChoicePriority -> RuntimeRuleChoiceKind.PRIORITY_LONGEST
                    else -> throw RuntimeException("unsupported")
                }
                RuntimeRuleItem(kind, choiceKind, -1, 0, items.toTypedArray())
            }
            (target is EmptyRule) -> {
                val item = target.accept(this, arg) as RuntimeRule
                RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, -1, 0, arrayOf(item))
            }
            else -> {
                throw ParserException("Not supported (yet)!")
            }
        }
    }

    override fun visit(target: EmptyRule, arg: String): RuntimeRule {
        val ruleThatIsEmpty = this.findRule(arg) ?: throw ParserException("Internal Error: should not happen")
        val e = this.builder.empty(ruleThatIsEmpty)
        this.originalRule.put(e, target)
        return e
    }

    override fun visit(target: Terminal, arg: String): RuntimeRule {
        val existing = this.findTerminal("'${target.value}'")
        if (null == existing) {
            val terminalRule = if (target.isPattern) {
                builder.pattern(target.value)
            } else {
                builder.literal(target.value)
            }
            this.originalRule.put(terminalRule, target)
            return terminalRule
        } else {
            return existing
        }
    }

    override fun visit(target: NonTerminal, arg: String): RuntimeRule {
        val nonTerminalRule = this.findRule(target.referencedRule.name)
                ?: target.referencedRule.accept(this, arg) as RuntimeRule
        return nonTerminalRule
    }

    override fun visit(target: ChoiceEqual, arg: String): RuntimeRule {
        if (1 == target.alternative.size) {
            return target.alternative[0].accept(this, arg) as RuntimeRule
        } else {
            val choiceRuleName = builder.createChoiceRuleName(arg);
            val items = target.alternative.map {
                it.accept(this, choiceRuleName) as RuntimeRule
            }
            val rr = builder.rule(choiceRuleName).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, *items.toTypedArray())
            this.originalRule.put(rr, target)
            return rr
        }
    }

    override fun visit(target: ChoicePriority, arg: String): RuntimeRule {
        if (1 == target.alternative.size) {
            return target.alternative[0].accept(this, arg) as RuntimeRule
        } else {
            val choiceRuleName = builder.createChoiceRuleName(arg);
            val items = target.alternative.map {
                it.accept(this, choiceRuleName) as RuntimeRule
            }
            val rr = builder.rule(choiceRuleName).choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, *items.toTypedArray())
            this.originalRule.put(rr, target)
            return rr
        }
    }

    override fun visit(target: ChoiceAmbiguous, arg: String): RuntimeRule {
        TODO()
    }

    override fun visit(target: Concatenation, arg: String): RuntimeRule {
        val items = target.items.map { it.accept(this, arg) as RuntimeRule }
        val rr = builder.rule(arg).concatenation(*items.toTypedArray())
        this.originalRule.put(rr, target)
        return rr
    }

    override fun visit(target: Group, arg: String): RuntimeRule {
        val groupRuleName = builder.createGroupRuleName(arg)
        val groupRuleItem = target.choice.accept(this, groupRuleName) as RuntimeRule
        val rr = builder.rule(groupRuleName).concatenation(groupRuleItem)
        this.originalRule.put(rr, target)
        return rr
    }

    override fun visit(target: Multi, arg: String): RuntimeRule {
        val multiRuleName = builder.createMultiRuleName(arg)
        val multiRuleItem = target.item.accept(this, arg) as RuntimeRule
        val rr = builder.rule(multiRuleName).multi(target.min, target.max, multiRuleItem)
        this.originalRule.put(rr, target)
        return rr
    }

    override fun visit(target: SeparatedList, arg: String): RuntimeRule {
        val listRuleName = builder.createListRuleName(arg)
        val listRuleItem = target.item.accept(this, arg) as RuntimeRule
        val sepRule = target.separator.accept(this, arg) as RuntimeRule
        val rr = builder.rule(listRuleName).separatedList(target.min, target.max, sepRule, listRuleItem)
        this.originalRule.put(rr, target)
        return rr
    }

}