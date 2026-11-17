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

/**
 * @param source the element that holds the referring property
 * @param sourceLocation the location of 'source'
 * @param referringLocation the location of the referring value itself (e.g. the identifier), if known
 */
data class ResolvedReference(
    val source:Any?,
    val sourceLocation: InputLocation?,
    val target: Any?,
    val targetLocation: InputLocation?,
    val referringLocation: InputLocation? = null
)

/**
 * Semantic information about a part of a sentence, for styling (see the style DSL selectors 'type', 'identify' and 'refers-to').
 * Each tag is '<kind> <qualified type name>', e.g. 'refers-to example.Shapes.Circle'.
 * The tags of an element's type hold the type and every type it conforms to.
 */
data class SemanticTag(
    val location: InputLocation,
    val tags: List<String>
) {
    companion object {
        /** all tokens of an element of the type */
        const val KIND_TYPE = "type"
        /** the identifier that names (declares) an element of the type */
        const val KIND_IDENTIFY = "identify"
        /** an identifier that refers to an element of the type */
        const val KIND_REFERS_TO = "refers-to"

        fun tag(kind: String, typeQualifiedName: String) = "$kind $typeQualifiedName"
    }
}

interface SemanticAnalysisResult {
    val resolvedReferences:List<ResolvedReference>
    val issues: IssueCollection<LanguageIssue>
    /** semantic tags for styling, empty if the semantic analyser does not compute them */
    val semanticTags: List<SemanticTag> get() = emptyList()
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
    /** list of named outputs */
    val output: Map<String,String>
    /** default output */
    val sentence:String?
    val issues: IssueCollection<LanguageIssue>
}

interface ExpectedAtResult {
    /**
     * The position in the sentence that the completions were requested for.
     */
    val requestedPosition: Int

    /**
     * The position in the sentence that the completions are being offered for.
     */
    val offeredPosition: Int

    /**
     * true if the offered completions are for completing a terminal.
     */
    val isTerminalCompletion: Boolean

    /**
     * The number of characters from the requested position to the offered position.
     */
    val offset: Int

    /**
     * the completion items being offered
     */
    val items: List<CompletionItem>

    /**
     * Any issues found whilst trying to provide completions.
     */
    val issues: IssueCollection<LanguageIssue>
}