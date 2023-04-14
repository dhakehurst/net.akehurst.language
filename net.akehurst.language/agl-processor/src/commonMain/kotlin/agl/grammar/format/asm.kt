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
package net.akehurst.language.agl.grammar.format

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.formatter.AglFormatExpression
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.formatter.AglFormatterRule
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.processor.SentenceContext

class AglFormatterModelDefault(
    val asm: AsmSimple?
) : AsmSimple(), AglFormatterModel {

    companion object {
        fun fromString(context: SentenceContext<String>, aglFormatterModelSentence: String): ProcessResult<AglFormatterModel> {
            val proc = Agl.registry.agl.formatter.processor ?: error("Formatter language not found!")
            return proc.process(
                sentence = aglFormatterModelSentence,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override val defaultWhiteSpace: String
        get() = " "

    override val rules: Map<String, AglFormatterRule> by lazy {
        when (asm) {
            null -> emptyMap()
            else -> (asm.rootElements[0] as AsmElementSimple).getPropertyAsList("ruleList").associate {
                when (it) {
                    is AsmElementSimple -> {
                        val rule = AglFormatterRuleDefault(this,it)
                        Pair(rule.forTypeName, rule)
                    }

                    else -> error("Cannot map '$it' to an AglFormatterRule")
                }
            }
        }
    }

}

class AglFormatterRuleDefault(
    override val model: AglFormatterModel,
    val asm: AsmElementSimple
) : AglFormatterRule {
    override val forTypeName: String
        get() = asm.getPropertyAsAsmElement("typeReference").getPropertyAsString("identifier")

    override val formatExpression: AglFormatExpression
        get() {
            val fmAsm = asm.getPropertyAsAsmElement("formatExpression")
            return AglFormatExpressionDefault(fmAsm)
        }
}

class AglFormatExpressionDefault(
    val asm: AsmElementSimple
) : AglFormatExpression {

}