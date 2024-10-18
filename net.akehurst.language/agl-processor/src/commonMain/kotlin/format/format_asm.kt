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

package net.akehurst.language.format.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.FormatString
import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.formatter.api.AglFormatterModel
import net.akehurst.language.formatter.api.AglFormatterRule
import net.akehurst.language.formatter.api.FormatExpression

class AglFormatterModelFromAsm(
    val asm: Asm?
) : AglFormatterModel {

    companion object {
        fun fromString(context: SentenceContext<String>, aglFormatterModelSentence: FormatString): ProcessResult<AglFormatterModel> {
            val proc = Agl.registry.agl.format.processor ?: error("Formatter language not found!")
            return proc.process(
                sentence = aglFormatterModelSentence.value,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override val defaultWhiteSpace: String get() = " "

    override val rules: Map<SimpleName, AglFormatterRule> by lazy {
        when (asm) {
            null -> emptyMap()
            else -> ((asm.root[0] as AsmStructure).getProperty(PropertyValueName("ruleList")) as AsmList).elements.associate {
                when (it) {
                    is AsmStructure -> {
                        val rule = AglFormatterRuleFromAsm(this, it)
                        Pair(rule.forTypeName, rule)
                    }

                    else -> error("Cannot map '$it' to an AglFormatterRule")
                }
            }
        }
    }

}

class AglFormatterRuleFromAsm(
    override val model: AglFormatterModel,
    val asm: AsmStructure
) : AglFormatterRule {
    override val forTypeName: SimpleName
        get() {
            val typeRef = asm.getProperty(PropertyValueName("typeReference")) as AsmStructure
            val id = (typeRef.getProperty(PropertyValueName("identifier")) as AsmPrimitive).value.toString()
            return SimpleName(id)
        }

    override val formatExpression: FormatExpression
        get() {
            val fmAsm = asm.getProperty(PropertyValueName("formatExpression")) as AsmStructure
            return AglFormatExpressionFromAsm(fmAsm)
        }
}

class AglFormatExpressionFromAsm(
    val asm: AsmStructure
) : FormatExpression {

}