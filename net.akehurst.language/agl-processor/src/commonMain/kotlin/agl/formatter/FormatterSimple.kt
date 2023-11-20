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

import net.akehurst.language.agl.language.format.AglFormatExpressionDefault
import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessorPhase

class FormatterSimple<AsmType>(
    val model: AglFormatterModel?
) : Formatter<AsmType> {

    override fun format(asm: AsmType): FormatResult {
        val sb = StringBuilder()

        for (root in (asm as Asm).root) {
            val str = root.format()
            sb.append(str)
        }

        return FormatResultDefault(sb.toString(), IssueHolder(LanguageProcessorPhase.FORMAT))
    }

    private fun AsmValue.format(): String {
        val o = this
        return when (o) {
            is AsmNothing -> ""
            is AsmPrimitive -> o.value.toString()
            is AsmStructure -> o.format(model)
            is AsmListSeparated -> o.elements.joinToString(separator = "") { it.format() }
            is AsmList -> o.elements.joinToString(separator = "") { it.format() }
            else -> error("Internal Error: type '${o::class.simpleName}' not supported")
        }
    }

    fun AsmStructure.format(model: AglFormatterModel?): String {
        val formatRule = model?.rules?.get(this.typeName)
        return when (formatRule) {
            null -> {
                this.propertyOrdered.map {
                    val propValue = it.value
                    when (propValue) {
                        is AsmPrimitive -> propValue.toString() + (model?.defaultWhiteSpace ?: "")
                        else -> propValue.format()
                    }
                }.joinToString(separator = "") { it }
            }

            else -> (formatRule.formatExpression as AglFormatExpressionDefault).execute(model, this)
        }
    }

    private fun AglFormatExpressionDefault.execute(model: AglFormatterModel?, el: AsmStructure): String {
        return when (this.asm.typeName) {
            "LiteralString" -> (el.getProperty("literal_string") as AsmPrimitive).toString()
            "TemplateString" -> {
                val templateContentList = (this.asm.getProperty("templateContentList") as AsmList).elements
                templateContentList.joinToString(separator = model?.defaultWhiteSpace ?: "") {
                    when (it.typeName) {
                        "Text" -> (it as AsmStructure).getProperty("raw_text").toString()
                        "TemplateExpressionSimple" -> {
                            val id = (it as AsmStructure).getProperty("dollar_identifier").toString().substringAfter("\$")
                            val pv = el.getProperty(id)
                            when (pv) {
                                is AsmPrimitive -> pv.toString()
                                is AsmStructure -> pv.format(model)
                                else -> error("property ${pv} not handled")
                            }
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