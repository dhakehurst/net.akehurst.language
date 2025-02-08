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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.asm.api.*
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.formatter.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.TypeModel

class FormatterSimple<AsmType>(
    val model: AglFormatModel?,
    val typeModel: TypeModel
) : Formatter<AsmType> {

    private val _issues = IssueHolder(LanguageProcessorPhase.FORMAT)
    val objectGraph =  ObjectGraphAsmSimple(typeModel, _issues)
    val expressionInterpreter by lazy {
        ExpressionsInterpreterOverTypedObject(objectGraph, _issues)
    }

    override fun format(asm: AsmType): FormatResult {
        val sb = StringBuilder()

        for (root in (asm as Asm).root) {
            val str = root.format(model)
            sb.append(str)
        }

        return FormatResultDefault(sb.toString(), IssueHolder(LanguageProcessorPhase.FORMAT))
    }


    private fun AsmValue.format(model: AglFormatModel?): String {
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

    private fun AsmStructure.format(model: AglFormatModel?): String {
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

            else -> formatRule.formatExpression.execute(model, this)
        }
    }

    private fun formatExpression(formatExpr: FormatExpression, asm: AsmStructure) = when (formatExpr) {
        is FormatExpressionExpression -> formatFromExpression(formatExpr, asm)
        is FormatExpressionTemplate -> formatFromTemplate(formatExpr, asm)
        is FormatExpressionWhen -> formatFromWhen(formatExpr, asm)
        else -> error("Internal error: subtype of AglFormatExpression not handled: '${formatExpr::class.simpleName}'")
    }

    private fun formatFromExpression(formatExpr: FormatExpressionExpression, asm: AsmStructure): String {
        val tp = typeModel.findByQualifiedNameOrNull(asm.qualifiedTypeName)!!.type()
        val evc = EvaluationContext.ofSelf(TypedObjectAsmValue(tp,asm))
        val res = expressionInterpreter.evaluateExpression(evc, formatExpr.expression)
        return (res.self
                as AsmPrimitive).value as String
    }

    private fun formatFromTemplate(formatExpr: FormatExpressionTemplate, asm: AsmStructure): String {
        return formatExpr.content.joinToString(separator = "") {
            when (it) {
                is TemplateElementText -> TODO()
                is TemplateElementExpressionProperty -> TODO()
                is TemplateElementExpressionEmbedded -> TODO()
                else -> error("Internal error: subtype of TemplateElement not handled: '${it::class.simpleName}'")
            }
        }
    }

    private fun formatFromWhen(formatExpr: FormatExpressionWhen, asm: AsmStructure): String {
        TODO()
    }

    private fun FormatExpression.execute(model: AglFormatModel?, el: AsmStructure): String {
        return when (this) {
            is FormatExpressionExpression -> formatFromExpression(this, el)
            //(el.getProperty(PropertyValueName("literal_string")) as AsmPrimitive).value.toString()
            is FormatExpressionTemplate -> {
                this.content.joinToString(separator = model?.defaultWhiteSpace ?: "") {
                    when (it) {
                        is TemplateElementText -> ((it as AsmStructure).getProperty(PropertyValueName("raw_text")) as AsmPrimitive).value.toString()
                        is TemplateElementExpressionProperty -> {
                            val id1 = (it as AsmStructure).getProperty(PropertyValueName("dollar_identifier"))
                            val id = (id1 as AsmPrimitive).value.toString().substringAfter("\$")
                            val pv = el.getProperty(PropertyValueName(id))
                            pv.format(model)
                        }

                        is TemplateElementExpressionEmbedded -> TODO()
                        else -> error("Element type ${it::class.simpleName} not handled")
                    }
                }
            }

            is FormatExpressionWhen -> TODO()
            else -> error("Element type ${this::class.simpleName} not handled")
        }
    }
}