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

package net.akehurst.language.api.processor

import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.parser.api.ParseResult
import net.akehurst.language.sentence.api.InputLocation

interface SyntaxAnalysisResult<out AsmType : Any> {
    val asm: AsmType?
    val issues: IssueCollection<LanguageIssue>
    val locationMap: LocationMap
}

data class ResolvedReference(
    val source:Any?,
    val sourceLocation: InputLocation?,
    val target: Any?,
    val targetLocation: InputLocation?
)

interface SemanticAnalysisResult {
    val resolvedReferences:List<ResolvedReference>
    val issues: IssueCollection<LanguageIssue>
}

interface ProcessResult<out AsmType:Any> {
    val asm: AsmType?
    val parse: ParseResult?
    val syntaxAnalysis: SyntaxAnalysisResult<AsmType>?
    val semanticAnalysis: SemanticAnalysisResult?
    val processIssues: IssueCollection<LanguageIssue>
    val allIssues: IssueCollection<LanguageIssue>
}

interface FormatResult {
    val sentence: String?
    val issues: IssueCollection<LanguageIssue>
}

interface ExpectedAtResult {
    val offset: Int
    val items: List<CompletionItem>
    val issues: IssueCollection<LanguageIssue>
}