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

import net.akehurst.language.api.processor.*
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.ParseResult

class LanguageProcessorResult<AsmType:Any, ContextType : Any>(
    val processor: LanguageProcessor<AsmType, ContextType>?,
    val issues: IssueCollection<LanguageIssue>
)

data class SyntaxAnalysisResultDefault<AsmType:Any>(
    override val asm: AsmType?,
    override val issues: IssueCollection<LanguageIssue>,
    override val locationMap: LocationMap
) : SyntaxAnalysisResult<AsmType>

data class SemanticAnalysisResultDefault(
    override val resolvedReferences: List<ResolvedReference>,
    override val issues: IssueCollection<LanguageIssue>
) : SemanticAnalysisResult

data class ProcessResultDefault<AsmType:Any>(
    override val asm: AsmType?,
    override val parse: ParseResult? = null,
    override val syntaxAnalysis: SyntaxAnalysisResult<AsmType>? = null,
    override val semanticAnalysis: SemanticAnalysisResult? = null,
    override val processIssues: IssueCollection<LanguageIssue> = IssueHolder(),
) : ProcessResult<AsmType> {
    override val allIssues: IssueCollection<LanguageIssue>
        get() {
            val parseIssues = parse?.issues?.all ?: emptySet()
            val synIssues = syntaxAnalysis?.issues?.all ?: emptySet()
            val semIssues = semanticAnalysis?.issues?.all ?: emptySet()
            return IssueHolder(issues = parseIssues + synIssues + semIssues + processIssues.all)
        }
}

data class FormatResultDefault(
    override val sentence: String?,
    override val issues: IssueCollection<LanguageIssue>
) : FormatResult

data class ExpectedAtResultDefault(
    override val offset: Int,
    override val items: List<CompletionItem>,
    override val issues: IssueCollection<LanguageIssue>
) : ExpectedAtResult