/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.formatter

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.grammar.*
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.formatter.api.*
import net.akehurst.language.typemodel.api.TypeModel


internal class AglFormatterModelSimple : AglFormatterModel {
    companion object {
        private fun fromRuleItem(grammar: Grammar, ruleItem: RuleItem): TemplateElement = when (ruleItem) {
            is Terminal -> when {
                ruleItem.isPattern -> TODO()
                else -> TemplateElementTextSimple(ruleItem.value)
            }

            is EmptyRule -> TemplateElementTextSimple("")
            is NonTerminal -> fromRuleItem(grammar, ruleItem.referencedRule(grammar).rhs)
            is Embedded -> TODO()
            is Choice -> TODO()
            is Concatenation -> TODO()
            is Group -> TODO()
            is OptionalItem -> TODO()
            is SimpleList -> TODO()
            is SeparatedList -> TODO()
            else -> error("Internal error: subtype of RuleItem not handled: '${ruleItem::class.simpleName}'")
        }

        fun fromGrammar(grammarList: List<Grammar>, typeModel: TypeModel): ProcessResult<AglFormatterModel> {
            val issues = IssueHolder(LanguageProcessorPhase.ALL)
            val formatModel = AglFormatterModelSimple()
            for (ns in typeModel.allNamespace) {
                when {
                    ns is GrammarTypeNamespace -> {
                        val grammar = grammarList.firstOrNull { it.qualifiedName == ns.qualifiedName }
                        when {
                            null != grammar -> {
                                for ((rn, ty) in ns.allRuleNameToType) {
                                    val grule = grammar.findOwnedGrammarRuleOrNull(rn)
                                    when {
                                        null != grule -> fromRuleItem(grammar, grule.rhs)
                                        else -> TODO()
                                    }

                                    formatModel.addRule(ty.typeName)
                                }
                            }

                            else -> Unit
                        }
                    }

                    else -> Unit
                }
            }
            return ProcessResultDefault(formatModel, issues)
        }
    }

    override val defaultWhiteSpace: String get() = " "

    override val rules = mutableMapOf<String, AglFormatterRule>()

    fun addRule(typeName: String) {

    }
}

class AglFormatterRuleSimple(
    override val model: AglFormatterModel,
    override val forTypeName: String
) : AglFormatterRule {

    override val formatExpression: FormatExpression
        get() = TODO("not implemented")
}

class AglFormatExpressionSimple() : FormatExpression {

}

class TemplateElementTextSimple(
    override val text: String
) : TemplateElementText