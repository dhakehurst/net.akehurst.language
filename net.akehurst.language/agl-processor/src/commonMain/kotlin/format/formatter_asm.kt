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

package net.akehurst.language.format.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.formatter.api.*
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.TypeModel


class AglFormatModelDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespaces: List<FormatNamespace> = emptyList()
) : AglFormatModel, ModelAbstract<FormatNamespace, FormatSet>(namespaces, options) {
    companion object {
        private fun fromRuleItem(grammar: Grammar, ruleItem: RuleItem): TemplateElement = when (ruleItem) {
            is Terminal -> when {
                ruleItem.isPattern -> TODO()
                else -> TemplateElementTextDefault(ruleItem.value)
            }

            is EmptyRule -> TemplateElementTextDefault("")
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

        fun fromGrammar(grammarModel: GrammarModel, typeModel: TypeModel): ProcessResult<AglFormatModel> {
            val issues = IssueHolder(LanguageProcessorPhase.ALL)
            val formatModel = AglFormatModelDefault(grammarModel.name)
            for (ns in typeModel.namespace) {
                when {
                    ns is GrammarTypeNamespace -> {
                        val grammar = grammarModel.allDefinitions.firstOrNull { gr -> gr.qualifiedName == ns.qualifiedName }
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

        fun fromString(context: ContextFromTypeModel, formatModelStr: FormatString): ProcessResult<AglFormatModel> {
            val proc = Agl.registry.agl.format.processor ?: error("Agl Format language not found!")
            val res = proc.process(
                sentence = formatModelStr.value,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
            return when {
                res.issues.errors.isEmpty() -> res
                else -> error(res.issues.toString())
            }
        }
    }

    override val defaultWhiteSpace: String get() = " "

    override val rules = mutableMapOf<SimpleName, AglFormatRule>()

    fun addRule(typeName: SimpleName) {
        TODO("is it needed ?")
    }
}

class AglFormatNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : FormatNamespace, NamespaceAbstract<FormatSet>(options, import) {

    override val formatSet: List<FormatSet> get() = super.definition
}

class FormatSetReferenceDefault(
    override val localNamespace: FormatNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : FormatSetReference {
    override var resolved: FormatSet? = null
    override fun resolveAs(resolved: FormatSet) {
        this.resolved = resolved
    }
}

class FormatSetDefault(
    override val namespace: FormatNamespace,
    override val name: SimpleName,
    override val extends: List<FormatSetReference>,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    override val rules: List<AglFormatRule>
) : FormatSet, DefinitionAbstract<FormatSet>() {
}

class AglFormatRuleDefault(
    override val forTypeName: TypeReference,
    override val formatExpression: FormatExpression
) : AglFormatRule {
}

class FormatExpressionWhenDefault(
    override val options: List<FormatWhenOption>
) : FormatExpressionWhen {
    override fun asString(indent: Indent, imports: List<Import>): String {
        TODO("not implemented")
    }
}

class FormatWhenOptionDefault(
    override val condition: Expression,
    override val format: FormatExpression
) : FormatWhenOption {
    override val expression: Expression = format
}

class FormatWhenOptionElseDefault(
    override val format: FormatExpression
) : FormatWhenOptionElse {
    override val expression: Expression = format
}

class FormatExpressionExpressionDefault(
    override val expression: Expression
) : FormatExpressionExpression {

    override fun asString(indent: Indent, imports: List<Import>): String {
        TODO("not implemented")
    }
}

class FormatExpressionTemplateDefault(
    override val content: List<TemplateElement>
) : FormatExpressionTemplate {
    override fun asString(indent: Indent, imports: List<Import>): String {
        TODO("not implemented")
    }
}

class TemplateElementTextDefault(
    override val text: String
) : TemplateElementText {
    override fun toString(): String = text
}

class TemplateElementExpressionPropertyDefault(
    override val propertyName: String
) : TemplateElementExpressionProperty {
    override fun toString(): String = "\$$propertyName"
}

class TemplateElementExpressionListDefault(
    override val listPropertyName: String,
    override val separator: String
) : TemplateElementExpressionList {
    override fun toString(): String = "\$[$listPropertyName / '$separator']"
}

class TemplateElementExpressionEmbeddedDefault(
    override val expression: FormatExpression
) : TemplateElementExpressionEmbedded {
    override fun toString(): String = "\${$expression}"
}