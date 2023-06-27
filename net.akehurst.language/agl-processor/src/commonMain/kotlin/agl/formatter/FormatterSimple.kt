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

import net.akehurst.language.agl.grammar.format.AglFormatExpressionDefault
import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessorPhase

class FormatterSimple<AsmType>(
    val model: AglFormatterModel?
) : Formatter<AsmType> {

    override fun format(asm: AsmType): FormatResult {
        val sb = StringBuilder()

        for (root in (asm as AsmSimple).rootElements) {
            val str = formatAny(root)
            sb.append(str)
        }

        return FormatResultDefault(sb.toString(), IssueHolder(LanguageProcessorPhase.FORMATTER))
    }

    private fun formatAny(o: Any?): String {
        return when (o) {
            null -> ""
            is AsmElementSimple -> o.format(model)
            is List<*> -> o.joinToString(separator = "") { formatAny(it) }
            else -> error("Internal Error: type '${o::class.simpleName}' not supported")
        }
    }

    fun AsmElementSimple.format(model: AglFormatterModel?): String {
        val formatRule = model?.rules?.get(this.typeName)
        return when (formatRule) {
            null -> {
                this.propertiesOrdered.map {
                    val propValue = it.value
                    when (propValue) {
                        is String -> propValue + (model?.defaultWhiteSpace ?: "")
                        else -> formatAny(propValue)
                    }
                }.joinToString(separator = "") { it }
            }

            else -> (formatRule.formatExpression as AglFormatExpressionDefault).execute(model, this)
        }
    }

    fun AglFormatExpressionDefault.execute(model: AglFormatterModel?, el: AsmElementSimple): String {
        return when (this.asm.typeName) {
            "LiteralString" -> el.getPropertyAsString("literal_string")
            "TemplateString" -> {
                val templateContentList = this.asm.getPropertyAsListOfElement("templateContentList")
                templateContentList.joinToString(separator = model?.defaultWhiteSpace ?: "") {
                    when (it.typeName) {
                        "Text" -> it.getPropertyAsString("raw_text")
                        "TemplateExpressionSimple" -> {
                            val id = it.getPropertyAsString("dollar_identifier").substringAfter("\$")
                            val pv = el.getProperty(id)
                            when (pv) {
                                is String -> pv
                                is AsmElementSimple -> pv.format(model)
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