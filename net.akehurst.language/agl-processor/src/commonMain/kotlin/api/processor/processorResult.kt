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

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SharedPackedParseTree

interface IssueCollection<T : Any> : Collection<T> {
    val all: Set<T>
    val errors: List<T>
    val warnings: List<T>
    val informations: List<T>
}

interface ParseResult {
    val sppt: SharedPackedParseTree?
    val issues: IssueCollection<LanguageIssue>
}

interface SyntaxAnalysisResult<out AsmType : Any> {
    val asm: AsmType?
    val issues: IssueCollection<LanguageIssue>
    val locationMap: Map<Any, InputLocation>
}

interface SemanticAnalysisResult {
    val issues: IssueCollection<LanguageIssue>
}

interface ProcessResult<out AsmType : Any> {
    val asm: AsmType?
    val issues: IssueCollection<LanguageIssue>
}

interface FormatResult {
    val sentence: String?
    val issues: IssueCollection<LanguageIssue>
}

interface ExpectedAtResult {
    val items: List<CompletionItem>
    val issues: IssueCollection<LanguageIssue>
}