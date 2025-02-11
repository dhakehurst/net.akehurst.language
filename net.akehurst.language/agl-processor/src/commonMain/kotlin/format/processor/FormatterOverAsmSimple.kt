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
import net.akehurst.language.asm.simple.AsmSimple
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.formatter.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.TypeModel

class FormatterOverAsmSimple(
    val model: AglFormatModel,
    val typeModel: TypeModel,
    val issues: IssueHolder
) : Formatter<Asm> {

    private val _formatter = FormatterOverTypedObject(model, ObjectGraphAsmSimple(typeModel, issues),issues)

    override fun format(formatSetName: PossiblyQualifiedName, asm: Asm): FormatResult {
        val sb = StringBuilder()

        for (root in asm.root) {
            val str = _formatter.format(formatSetName,root).sentence
            sb.append(str)
        }

        return FormatResultDefault(sb.toString(), IssueHolder(LanguageProcessorPhase.FORMAT))
    }


}