/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.formatter

import net.akehurst.language.agl.language.format.AglFormatExpressionFromAsm
import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.formatter.api.*

class FormatterSimple<AsmType>(
    val model: AglFormatterModel?
) : Formatter<AsmType> {

    override fun format(asm: AsmType): FormatResult {
        val sb = StringBuilder()

        for (root in (asm as Asm).root) {
            val str = root.format(model)
            sb.append(str)
        }

        return FormatResultDefault(sb.toString(), IssueHolder(LanguageProcessorPhase.FORMAT))
    }

    private fun AsmValue.format(model: AglFormatterModel?): String {
        val o = this
        return when (o) {
            is AsmNothing -> ""
            is AsmPrimitive -> o.value.toString()
            is AsmStructure -> o.format(model)
            is AsmListSeparated -> o.elements.joinToString(separator = "") { it.format(model) }
            is AsmList -> o.elements.joinToString(separator = "") { it.format(model) }
            else -> error("Internal Error: type '${o::class.simpleName}' not supported")
        }
    }

    private fun AsmStructure.format(model: AglFormatterModel?): String {
        val formatRule = model?.rules?.get(this.typeName)
        return when (formatRule) {
            null -> {
                this.propertyOrdered.map {
                    val propValue = it.value
                    when (propValue) {
                        is AsmPrimitive -> propValue.format(model)// + (model?.defaultWhiteSpace ?: "")
                        else -> propValue.format(model)
                    }
                }.joinToString(separator = "") { it }
            }

            else -> (formatRule.formatExpression as AglFormatExpressionFromAsm).execute(model, this)
        }
    }

    private fun formatExpression(formatExpr: FormatExpression, asm: AsmStructure) = when (formatExpr) {
        is FormatExpressionLiteral -> formatFromLiteral(formatExpr)
        is FormatExpressionTemplate -> formatFromTemplate(formatExpr, asm)
        is FormatExpressionWhen -> formatFromWhen(formatExpr, asm)
        else -> error("Internal error: subtype of AglFormatExpression not handled: '${formatExpr::class.simpleName}'")
    }

    private fun formatFromLiteral(formatExpr: FormatExpressionLiteral): String {
        return formatExpr.value
    }

    private fun formatFromTemplate(formatExpr: FormatExpressionTemplate, asm: AsmStructure): String {
        return formatExpr.content.joinToString(separator = "") {
            when (it) {
                is TemplateElementText -> TODO()
                is TemplateElementExpressionSimple -> TODO()
                is TemplateElementExpressionEmbedded -> TODO()
                else -> error("Internal error: subtype of TemplateElement not handled: '${it::class.simpleName}'")
            }
        }
    }

    private fun formatFromWhen(formatExpr: FormatExpressionWhen, asm: AsmStructure): String {
        TODO()
    }

    private fun AglFormatExpressionFromAsm.execute(model: AglFormatterModel?, el: AsmStructure): String {
        return when (this.asm.typeName) {
            "LiteralString" -> (el.getProperty("literal_string") as AsmPrimitive).value.toString()
            "TemplateString" -> {
                val templateContentList = (this.asm.getProperty("templateContentList") as AsmList).elements
                templateContentList.joinToString(separator = model?.defaultWhiteSpace ?: "") {
                    when (it.typeName) {
                        "Text" -> ((it as AsmStructure).getProperty("raw_text") as AsmPrimitive).value.toString()
                        "TemplateExpressionSimple" -> {
                            val id1 = (it as AsmStructure).getProperty("dollar_identifier")
                            val id = (id1 as AsmPrimitive).value.toString().substringAfter("\$")
                            val pv = el.getProperty(id)
                            pv.format(model)
                        }

                        "TemplateExpressionEmbedded" -> TODO()
                        else -> error("Element type ${it.typeName} not handled")
                    }
                }
            }

            "WhenExpression" -> TODO()
            else -> error("Element type ${this.asm.typeName} not handled")
        }
    }
}