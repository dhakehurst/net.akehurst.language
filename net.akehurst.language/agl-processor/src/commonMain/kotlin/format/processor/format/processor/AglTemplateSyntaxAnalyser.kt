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
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.format.asm.*
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.formatter.api.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.locationForNode

internal class AglTemplateSyntaxAnalyser() : SyntaxAnalyserByMethodRegistrationAbstract<AglFormatModel>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()
    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mutableMapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::templateString)
        super.register(this::templateContent)
        super.register(this::text)
        super.register(this::templateExpression)
        super.register(this::templateExpressionProperty)
        super.register(this::templateExpressionList)
        super.register(this::templateExpressionEmbedded)
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
    fun text(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementText {
        val parsedText = children[0] as String
        var lines = parsedText.split('\n')
        var joined = when {
            1==lines.size -> lines[0]
            else -> {
                lines = when {
                    lines.first().isBlank() -> lines.drop(1)
                    else -> lines
                }
                lines = when {
                    lines.isEmpty() -> lines
                    lines.last().isBlank() -> lines.dropLast(1)
                    else -> lines
                }

                val prefixMatch = Regex("\\s+").matchAt(lines.first(), 0)
                val prefix = prefixMatch?.value?.length ?: 0
                lines.map { ln -> ln.drop(prefix) }
                lines.joinToString("\n")
            }
        }
        val unescaped = joined.replace("\\", "")
        return TemplateElementTextDefault(unescaped)
    }

    // templateExpression = templateExpressionSimple | templateExpressionEmbedded ;
    fun templateExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElement =
        children[0] as TemplateElement

    // templateExpressionProperty = DOLLAR_IDENTIFIER ;
    fun templateExpressionProperty(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementExpressionProperty =
        TemplateElementExpressionPropertyDefault((children[0] as String).drop(1))

    // templateExpressionList = '$' '[' IDENTIFIER '/' STRING ']' ;
    fun templateExpressionList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementExpressionList {
        val expression = children[2] as String
        val separator = children[4] as String
        return TemplateElementExpressionListDefault(expression, separator)
    }

    // templateExpressionEmbedded = '${' formatExpression '}'
    fun templateExpressionEmbedded(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TemplateElementExpressionEmbedded =
        TemplateElementExpressionEmbeddedDefault(children[1] as FormatExpression)

    // typeReference = IDENTIFIER ;
    // fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): SimpleName =
    //    SimpleName(children[0] as String)

}
