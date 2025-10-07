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
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.LiteralExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.format.asm.*
import net.akehurst.language.formatter.api.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.types.asm.StdLibDefault

internal class AglFormatSyntaxAnalyser() : SyntaxAnalyserByMethodRegistrationAbstract<AglFormatDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    private val templateAnalyser = AglTemplateSyntaxAnalyser()
        .also { it.setEmbeddedSyntaxAnalyser(QualifiedName("net.akehurst.language.Format"), this) }
    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("net.akehurst.language.Template") to templateAnalyser
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::definition)
        super.register(this::format)
        super.register(this::formatRule)
        super.register(this::formatExpression)
        super.register(this::whenExpression)
        super.register(this::whenOption)
        super.register(this::whenOptionElse)
        super.register(this::separatedList)
        super.register(this::separator)
    }

    // override unit from BaseSyntaxAnalyser
    // unit = option* namespace* ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglFormatDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespace = children[1] as List<FormatNamespace>
        val optHolder = OptionHolderDefault(null, options.associate { it })
        namespace.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        val result = AglFormatDomainDefault(SimpleName("Unit"), optHolder, namespace)
        return result
    }

    // override namespace from BaseSyntaxAnalyser
    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, String>>
        val import = children[3] as List<Import>
        val definition = children[4] as List<(ns: FormatNamespace) -> FormatSet>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val ns = AglFormatNamespaceDefault(pqn.asQualifiedName(null), optHolder, import)
        definition.forEach {
            val def = it.invoke(ns)
            ns.addDefinition(def)
        }
        return ns
    }

    fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: FormatNamespace) -> FormatSet =
        children[0] as ((ns: FormatNamespace) -> FormatSet)

    // format = 'format' IDENTIFIER extends? '{' ruleList '}' ;
    fun format(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: FormatNamespace) -> FormatSet {
        val name = SimpleName(children[1] as String)
        val extendsFunc = (children[2] as List<(ns: FormatNamespace) -> FormatSetReference>?) ?: emptyList()
        val options = OptionHolderDefault() //TODO
        val rules: List<AglFormatRule> = children[4] as List<AglFormatRule>
        return { ns ->
            val extends = extendsFunc.map { it.invoke(ns) }
            FormatSetDefault(ns, name, extends, options, rules)
        }
    }

    // extends = ':' [possiblyQualifiedName / ',']+ ;
    fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(ns: FormatNamespace) -> FormatSetReference> {
        val extendNameList = children[1] as List<PossiblyQualifiedName>
        val sl = extendNameList.toSeparatedList<Any, PossiblyQualifiedName, String>()
        val extended = sl.items.map {
            // need to manually add the Reference as it is not seen by the super class
            { ns: FormatNamespace ->
                FormatSetReferenceDefault(ns, it).also { setLocationFor(it, nodeInfo, sentence) }
            }
        }
        return extended
    }

    //formatRule = typeReference '->' formatExpression ;
    fun formatRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglFormatRule {
        val forTypeName = children[0] as TypeReference
        val expression = children[2] as FormatExpression
        return AglFormatRuleDefault(forTypeName, expression)
    }

    //formatExpression = expression | templateString | whenExpression    ;
    fun formatExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatExpression = when (nodeInfo.alt.option.value) {
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

    // whenOptionElse = else '->' formatExpression ;
    private fun whenOptionElse(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FormatWhenOptionElse {
        val expression = children[2] as FormatExpression
        return FormatWhenOptionElseDefault(expression)
    }

    // separatedList = rootExpression 'sep' separator ;
    private fun separatedList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Expression,Expression> {
        val propertyReference = children[0] as Expression
        val separator = children[2] as Expression
        return Pair(propertyReference,separator)
    }

    // separator = STRING | rootExpression ;
    private fun separator(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression {
        return when (nodeInfo.alt.option.value) {
            0 -> {
                val value = children[0] as String
                val value2 = value
                    .removeSurrounding("'")
                    .replace("\\\\n", "\n")
                LiteralExpressionDefault(StdLibDefault.String.qualifiedTypeName,value2)
            }
            1 -> children[0] as Expression
            else -> error("Option not supported ${nodeInfo.alt.option.value}")
        }
    }

}
