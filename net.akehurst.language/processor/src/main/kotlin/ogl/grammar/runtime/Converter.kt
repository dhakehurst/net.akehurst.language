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
        return if (null==rule) {
            val nrule = this.builder.rule(target.name).skip(target.isSkip).build()
            nrule.rhsOpt = target.rhs.accept(this, target.name) as RuntimeRuleItem
            nrule
        } else {
            rule
        }
    }

    override fun visit(target: EmptyRule, arg: String): RuntimeRuleItem {
        val ruleThatIsEmpty = this.findRule(arg) ?: throw ParseException("Internal Error: should not happen")
        val e = this.builder.empty(ruleThatIsEmpty)
        return RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(e))
    }

    override fun visit(target: Terminal, arg: String): RuntimeRuleItem {
        val terminalRule = if (target.isPattern) {
            builder.pattern(target.value)
        } else {
            builder.literal(target.value)
        }
        return RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(terminalRule))
    }

    override fun visit(target: NonTerminal, arg: String): RuntimeRuleItem {
        val nonTerminalRule = this.findRule(target.referencedRule.name)
                ?: target.referencedRule.accept(this, arg) as RuntimeRule
        return RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(nonTerminalRule))
    }

    override fun visit(target: ChoiceEqual, arg: String): RuntimeRuleItem {
        return if (1 == target.alternative.size) {
            target.alternative[0].accept(this, arg) as RuntimeRuleItem
        } else {
            val items = target.alternative.map {
                it.accept(this, arg) as RuntimeRuleItem
            }.map {
                if (1 == it.items.size) {
                    it.items.first()
                } else {
                    val groupRuleName = builder.createGroupRuleName(arg)
                    builder.rule(groupRuleName).concatenation(*it.items)
                }
            }
            RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, items.toTypedArray())
        }
    }

    override fun visit(target: ChoicePriority, arg: String): RuntimeRuleItem {
        return if (1 == target.alternative.size) {
            target.alternative[0].accept(this, arg) as RuntimeRuleItem
        } else {
            val items = target.alternative.map {
                 it.accept(this, arg) as RuntimeRuleItem
            }.map {
                if (1 == it.items.size) {
                    it.items.first()
                } else {
                    val groupRuleName = builder.createGroupRuleName(arg)
                    builder.rule(groupRuleName).concatenation(*it.items)
                }
            }
            RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_PRIORITY, -1, 0, items.toTypedArray())
        }
    }

    override fun visit(target: Concatenation, arg: String): RuntimeRuleItem {
        return if (1 == target.items.size) {
            target.items[0].accept(this, arg) as RuntimeRuleItem
        } else {
            val items = target.items.map {
                it.accept(this, arg) as RuntimeRuleItem
            }.map {
                if (RuntimeRuleItemKind.CONCATENATION==it.kind && 1 == it.items.size) {
                    it.items.first()
                } else {
                    val groupRuleName = builder.createGroupRuleName(arg)
                    builder.rule(groupRuleName).withRhs(it)
                }
            }

            RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, items.toTypedArray())
        }
    }

    override fun visit(target: Group, arg: String): RuntimeRuleItem {
        val groupRuleName = builder.createGroupRuleName(arg)
        val groupRuleItem = target.choice.accept(this, groupRuleName) as RuntimeRuleItem
        val groupRule = builder.rule(groupRuleName).withRhs(groupRuleItem)
        return RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(groupRule))
    }

    override fun visit(target: Multi, arg: String): RuntimeRuleItem {
        //val multiRuleName = builder.createMultiRuleName(arg)
        val multiRuleItem = target.item.accept(this, arg) as RuntimeRuleItem
        // all ruleitems should only have one rhs.item (Terminal,ot Group), so can use this directly
        val ruleToBeMultied = multiRuleItem.items[0]
        if (0==target.min) {
            val ruleThatIsEmpty = this.findRule(arg) ?: throw ParseException("Internal Error: should not happen")
            val emptyRule = builder.empty(ruleThatIsEmpty)
            return RuntimeRuleItem(RuntimeRuleItemKind.MULTI, target.min, target.max, arrayOf(ruleToBeMultied, emptyRule))
        } else {
            return RuntimeRuleItem(RuntimeRuleItemKind.MULTI, target.min, target.max, arrayOf(ruleToBeMultied))
        }
    }

    override fun visit(target: SeparatedList, arg: String): RuntimeRuleItem {
        //val listRuleName = builder.createListRuleName(arg)
        val listRuleItem = target.item.accept(this, arg) as RuntimeRuleItem
        // all ruleitems should only have one rhs.item (Terminal,ot Group), so can use this directly
        //val listRule = builder.rule(listRuleName).withRhs(listRuleItem)
        val ruleToBeListed = listRuleItem.items[0]
        val sepRule = (target.separator.accept(this,arg) as RuntimeRuleItem).items.first()
        if (0==target.min) {
            val ruleThatIsEmpty = this.findRule(arg) ?: throw ParseException("Internal Error: should not happen")
            val emptyRule = builder.empty(ruleThatIsEmpty)
            return RuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST, target.min, target.max, arrayOf(ruleToBeListed, sepRule, emptyRule))
        } else {
            return RuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST, target.min, target.max, arrayOf(ruleToBeListed, sepRule))
        }
    }

}