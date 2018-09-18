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
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder

/**
 * Pair<String,Boolean> : (ruleName, isSkip)
 */
class Converter(val grammar: Grammar) : GrammarVisitor<Any, Pair<String,Boolean>> {

    val builder: RuntimeRuleSetBuilder = RuntimeRuleSetBuilder()

    fun transform(): RuntimeRuleSet {
        this.visit(this.grammar, Pair("",false))
        return this.builder.ruleSet()
    }

    override fun visit(target: Namespace, arg: Pair<String,Boolean>): Set<RuntimeRule> {
        return emptySet()
    }

    override fun visit(target: Grammar, arg: Pair<String,Boolean>): Set<RuntimeRule> {
        return target.allRule.map {
            it.accept(this, arg) as RuntimeRule
        }.toSet()
    }

    override fun visit(target: Rule, arg: Pair<String,Boolean>): RuntimeRule {
        return target.rhs.accept(this, Pair(target.name,target.isSkip)) as RuntimeRule
    }

    override fun visit(target: EmptyRule, arg: Pair<String,Boolean>): Any {
        return builder.rule(arg.first).skip(arg.second).empty()
    }

    override fun visit(target: Terminal, arg: Pair<String,Boolean>): RuntimeRule {
        return if (target.isPattern) {
            builder.pattern(target.value)
        } else {
            builder.literal(target.value)
        }
    }

    override fun visit(target: NonTerminal, arg: Pair<String,Boolean>): RuntimeRule {
        return builder.findRuleByName(target.referencedRule.name, false)
                ?: target.referencedRule.accept(this, arg) as RuntimeRule
    }

    override fun visit(target: ChoiceSimple, arg: Pair<String,Boolean>): RuntimeRule {
        return if (1 == target.alternative.size) {
            target.alternative.first().accept(this, arg) as RuntimeRule
        } else {
            val items = target.alternative.map {
                it.accept(this, arg) as RuntimeRule
            }
            builder.rule(arg.first).skip(arg.second).choiceEqual(*items.toTypedArray())
        }
    }

    override fun visit(target: ChoicePriority, arg: Pair<String,Boolean>): RuntimeRule {
        val items = target.alternative.map {
            val virtualRuleName = builder.createChoiceRuleName()
            it.accept(this, Pair(virtualRuleName, false)) as RuntimeRule
        }
        return builder.rule(arg.first).skip(arg.second).choicePriority(*items.toTypedArray())
    }

    override fun visit(target: Concatenation, arg: Pair<String,Boolean>): RuntimeRule {
        val items = target.items.map {
            it.accept(this, arg) as RuntimeRule
        }
        return builder.rule(arg.first).skip(arg.second).concatenation(*items.toTypedArray())
    }

    override fun visit(target: Group, arg: Pair<String,Boolean>): RuntimeRule {
        val groupRuleName = builder.createGroupRuleName()
        val groupRule = target.choice.accept(this, Pair(groupRuleName,false)) as RuntimeRule
        return groupRule
    }

    override fun visit(target: Multi, arg: Pair<String,Boolean>): RuntimeRule {
        val item = target.item.accept(this, arg) as RuntimeRule
        return builder.rule(arg.first).skip(arg.second).multi(target.min, target.max, item)
    }

    override fun visit(target: SeparatedList, arg: Pair<String,Boolean>): RuntimeRule {
        val separator = target.separator.accept(this, arg) as RuntimeRule
        val item = target.item.accept(this, arg) as RuntimeRule
        return builder.rule(arg.first).skip(arg.second).separatedList(target.min, target.max, separator, item)
    }

}