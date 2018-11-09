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

package net.akehurst.language.ogl.grammar.runtime

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.ogl.runtime.structure.*

/**
 * arg: String =
 */
class Converter(val grammar: Grammar) : GrammarVisitor<Any, String> {

    val builder = RuntimeRuleSetBuilder()

    private fun findRule(name: String): RuntimeRule? {
        return this.builder.findRuleByName(name, false)
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
            val nrule = this.builder.rule(target.name).skip(target.isSkip).build()

            //need to get back RuntimeRuleItems here,
            // then set the rhs accordingly

            val rhs = createRhs(target.rhs, target.name)
            //val rhs =  target.rhs.accept(this, target.name) as RuntimeRule
            nrule.rhsOpt = rhs
            nrule
        } else {
            rule
        }
    }

    private fun createRhs(target: RuleItem, arg: String): RuntimeRuleItem {
        return when {
            (target is Choice && target.alternative.size == 1) -> {
                // only one choice, so can create a concatination
                val rhsItem = target.alternative[0]
                val items = rhsItem.items.map { it.accept(this, "") as RuntimeRule }
                RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, items.toTypedArray())
            }
            (target is Choice) -> {
                val items = target.alternative.map {
                    if (it.items.size == 1) {
                        it.items[0].accept(this, "") as RuntimeRule
                    } else {
                        val thisChoiceName = builder.createChoiceRuleName(arg)
                        val thisChoiceItems = it.items.map { it.accept(this, thisChoiceName) as RuntimeRule }
                        builder.rule(thisChoiceName).concatenation(*thisChoiceItems.toTypedArray())
                    }
                }
                val kind = if (target is ChoiceEqual) RuntimeRuleItemKind.CHOICE_EQUAL else RuntimeRuleItemKind.CHOICE_PRIORITY
                RuntimeRuleItem(kind, -1, 0, items.toTypedArray())
            }
            else -> {
                throw ParseException("Not supported (yet)!")
            }
        }
    }

    override fun visit(target: EmptyRule, arg: String): RuntimeRule {
        val ruleThatIsEmpty = this.findRule(arg) ?: throw ParseException("Internal Error: should not happen")
        val e = this.builder.empty(ruleThatIsEmpty)
        return e
    }

    override fun visit(target: Terminal, arg: String): RuntimeRule {
        val terminalRule = if (target.isPattern) {
            builder.pattern(target.value)
        } else {
            builder.literal(target.value)
        }
        return terminalRule
    }

    override fun visit(target: NonTerminal, arg: String): RuntimeRule {
        val nonTerminalRule = this.findRule(target.referencedRule.name)
                ?: target.referencedRule.accept(this, arg) as RuntimeRule
        return nonTerminalRule
    }

    override fun visit(target: ChoiceEqual, arg: String): RuntimeRule {
        return if (1 == target.alternative.size) {
            target.alternative[0].accept(this, arg) as RuntimeRule
        } else {
            val choiceRuleName = builder.createChoiceRuleName(arg);
            val items = target.alternative.map {
                it.accept(this, choiceRuleName) as RuntimeRule
            }
            builder.rule(choiceRuleName).choiceEqual(*items.toTypedArray())
        }
    }

    override fun visit(target: ChoicePriority, arg: String): RuntimeRule {
        return if (1 == target.alternative.size) {
            target.alternative[0].accept(this, arg) as RuntimeRule
        } else {
            val choiceRuleName = builder.createChoiceRuleName(arg);
            val items = target.alternative.map {
                it.accept(this, choiceRuleName) as RuntimeRule
            }
            builder.rule(choiceRuleName).choicePriority(*items.toTypedArray())
        }
    }

    override fun visit(target: Concatenation, arg: String): RuntimeRule {
        val items = target.items.map { it.accept(this, arg) as RuntimeRule }
        return builder.rule(arg).concatenation(*items.toTypedArray())
    }

    override fun visit(target: Group, arg: String): RuntimeRule {
        val groupRuleName = builder.createGroupRuleName(arg)
        val groupRuleItem = target.choice.accept(this, groupRuleName) as RuntimeRule
        return builder.rule(groupRuleName).concatenation(groupRuleItem)
    }

    override fun visit(target: Multi, arg: String): RuntimeRule {
        val multiRuleName = builder.createMultiRuleName(arg)
        val multiRuleItem = target.item.accept(this, arg) as RuntimeRule
        return builder.rule(multiRuleName).multi(target.min, target.max, multiRuleItem)
    }

    override fun visit(target: SeparatedList, arg: String): RuntimeRule {
        val listRuleName = builder.createListRuleName(arg)
        val listRuleItem = target.item.accept(this, arg) as RuntimeRule
        val sepRule = target.separator.accept(this, arg) as RuntimeRule
        return builder.rule(listRuleName).separatedList(target.min, target.max, sepRule, listRuleItem)
    }

}