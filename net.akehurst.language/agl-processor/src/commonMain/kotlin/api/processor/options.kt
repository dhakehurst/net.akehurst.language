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

import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.scanner.api.ScanOptions
import net.akehurst.language.sentence.api.InputLocation

/**
 * Options to configure the syntax analysis of a Shared Packed Parse Tree (SPPT)
 */
interface SyntaxAnalysisOptions<AsmType : Any> {
    var enabled: Boolean

    fun clone(): SyntaxAnalysisOptions<AsmType>
}

/**
 * Options to configure the semantic analysis of an Abstract Syntax Model (ASM)
 */
interface SemanticAnalysisOptions<ContextType : Any> {
    var enabled: Boolean
    var locationMap: Map<Any, InputLocation>
    var context: ContextType?
    var buildScope: Boolean

    /**
     * whether or not to replace items that already exist in the scope
     * true -> replace with new item
     * false -> do not replace, leave old item
     */
    var replaceIfItemAlreadyExistsInScope: Boolean

    /**
     * Report this kind of LanguageIssue if item already exists in scope.
     * If null, do not report replacements as an issue.
     */
    var ifItemAlreadyExistsInScopeIssueKind: LanguageIssueKind?

    var checkReferences: Boolean
    var resolveReferences: Boolean
    val other: Map<String, Any>

    fun clone(): SemanticAnalysisOptions<ContextType>
}

interface CompletionProviderOptions<ContextType : Any> {
    var context: ContextType?

    /**
     * depth of nested rules to search when constructing possible completions
     **/
    var depth: Int

    var path: List<Pair<Int,Int>>

    var showOptionalItems:Boolean

    var provideValuesForPatternTerminals:Boolean

    val other: Map<String, Any>

    fun clone(): CompletionProviderOptions<ContextType>
}

/**
 * Options to configure the processing of a sentence
 */
interface ProcessOptions<AsmType : Any, ContextType : Any> {
    val scan: ScanOptions
    val parse: ParseOptions
    val syntaxAnalysis: SyntaxAnalysisOptions<AsmType>
    val semanticAnalysis: SemanticAnalysisOptions<ContextType>
    val completionProvider: CompletionProviderOptions<ContextType>

    fun clone(): ProcessOptions<AsmType, ContextType>
}
