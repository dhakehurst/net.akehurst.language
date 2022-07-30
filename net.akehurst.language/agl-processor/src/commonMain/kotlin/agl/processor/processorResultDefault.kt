/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SharedPackedParseTree

data class ParseResultDefault(
    override  val sppt:SharedPackedParseTree?,
    override  val issues:List<LanguageIssue>
) : ParseResult

data class SyntaxAnalysisResultDefault<AsmType : Any>(
    override  val asm:AsmType?,
    override   val issues:List<LanguageIssue>,
    override   val locationMap:Map<Any, InputLocation>
) : SyntaxAnalysisResult<AsmType>

data class SemanticAnalysisResultDefault(
    override    val issues:List<LanguageIssue>
) : SemanticAnalysisResult

data class  ProcessResultDefault<AsmType: Any>(
    override  val asm:AsmType?,
    override  val issues:List<LanguageIssue>
) : ProcessResult<AsmType>

data class FormatResultDefault(
    override  val sentence:String?,
    override  val issues:List<LanguageIssue>
) : FormatResult

data class ExpectedAtResultDefault(
    override  val items:List<CompletionItem>,
    override val issues:List<LanguageIssue>
) : ExpectedAtResult