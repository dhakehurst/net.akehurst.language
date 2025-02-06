/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.format.asm.*
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.formatter.api.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.locationForNode

internal class AglFormatSyntaxAnalyser() : SyntaxAnalyserByMethodRegistrationAbstract<AglFormatModel>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<AglFormatModel>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::format)
        super.register(this::formatRule)
        super.register(this::formatExpression)
        super.register(this::whenExpression)
        super.register(this::whenOption)
        super.register(this::templateString)
        super.register(this::templateContent)
        super.register(this::text)
        super.register(this::templateExpression)
        super.register(this::templateExpressionSimple)
        super.register(this::templateExpressionEmbedded)
        super.register(this::typeReference)
    }

    // unit = namespace format+ ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglFormatModel {
        val ns = children[0] as FormatNamespace
        val ruleBuilder = children[1] as List<((ns: FormatNamespace) -> FormatSet)>
        ruleBuilder.forEach { it.invoke(ns) }
        val su = AglFormatModelDefault(name = SimpleName("ParsedFormatUnit"), namespaces = listOf(ns))
        return su
    }

    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatNamespace {
        val qualifiedName = children[1] as PossiblyQualifiedName
        val imports = emptyList<Import>()
        return AglFormatNamespaceDefault(qualifiedName = qualifiedName.asQualifiedName(null), import = imports)
    }

    // format = 'format' IDENTIFIER extends? '{' ruleList '}' ;
    fun format(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: FormatNamespace) -> FormatSet {
        val name = SimpleName(children[1] as String)
        val extendsFunc = (children[2] as List<(ns: FormatNamespace) -> FormatSetReference>?) ?: emptyList()
        val options = OptionHolderDefault() //TODO
        val rules: List<AglFormatRule> = children[4] as List<AglFormatRule>
        return { ns ->
            val extends = extendsFunc.map { it.invoke(ns) }
            FormatSetDefault(ns, name, extends, options)
        }
    }

    // extends = ':' [possiblyQualifiedName / ',']+ ;
    fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(ns: FormatNamespace) -> FormatSetReference> {
        val extendNameList = children[1] as List<PossiblyQualifiedName>
        val sl = extendNameList.toSeparatedList<Any, PossiblyQualifiedName, String>()
        val extended = sl.items.map {
            // need to manually add the Reference as it is not seen by the super class
            { ns: FormatNamespace ->
                FormatSetReferenceDefault(ns, it).also { this.locationMap[it] = sentence.locationForNode(nodeInfo.node) }
            }
        }
        return extended
    }

    //formatRule = typeReference '->' formatExpression ;
    fun formatRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglFormatRule {
        val forTypeName = children[0] as SimpleName
        val expression = children[2] as FormatExpression
        return AglFormatRuleDefault(forTypeName, expression)
    }

    //formatExpression = expression | templateString | whenExpression    ;
    fun formatExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatExpression = when(nodeInfo.alt.option.value) {
        0 -> FormatExpressionExpressionDefault(children[0] as Expression)
        1 -> children[0] as FormatExpressionTemplate
        2 -> children[0] as FormatExpressionWhen
        else -> error("Option not handled")
    }
    // whenExpression = 'when' '{' whenOptionList '}' ;
    private fun whenExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatExpressionWhen {
        val optionList = children[2] as List<FormatWhenOption>
        return FormatExpressionWhenDefault(optionList)
    }

    // whenOption = expression '->' formatExpression ;
    private fun whenOption(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatWhenOption {
        val condition = children[0] as Expression
        val expression = children[2] as FormatExpression
        return FormatWhenOptionDefault(condition, expression)
    }


    // templateString = '"' templateContentList '"' ;
    fun templateString(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatExpressionTemplate {
        val content = children[1] as List<TemplateElement>
        return FormatExpressionTemplateDefault(content)
    }

    // templateContent = text | templateExpression ;
    fun templateContent(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElement =
        children[0] as TemplateElement

    // text = RAW_TEXT ;
    fun text(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementText =
        TemplateElementTextDefault(children[0] as String)

    // templateExpression = templateExpressionSimple | templateExpressionEmbedded ;
    fun templateExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElement =
        children[0] as TemplateElement

    // templateExpressionSimple = DOLLAR_IDENTIFIER ;
    fun templateExpressionSimple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementExpressionSimple =
        TemplateElementExpressionSimpleDefault(children[0] as String)

    // templateExpressionEmbedded = '${' formatExpression '}'
    fun templateExpressionEmbedded(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementExpressionEmbedded =
        TemplateElementExpressionEmbeddedDefault(children[1] as FormatExpression)

    // typeReference = IDENTIFIER ;
    fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): SimpleName =
        SimpleName(children[0] as String)

}
