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

interface ScanOptions {
}

/**
 * Options to configure the parsing of a sentence
 * there is no separate scanner, so scanner options are passed to the parser
 */
interface ParseOptions {
    var goalRuleName: String?
    var reportErrors: Boolean
    var reportGrammarAmbiguities: Boolean
    var cacheSkip: Boolean
}

/**
 * Options to configure the syntax analysis of a Shared Packed Parse Tree (SPPT)
 */
interface SyntaxAnalysisOptions<AsmType : Any> {
    var active: Boolean
}

/**
 * Options to configure the semantic analysis of an Abstract Syntax Model (ASM)
 */
interface SemanticAnalysisOptions<AsmType : Any, ContextType : Any> {
    var active: Boolean
    var locationMap: Map<Any, InputLocation>
    var context: ContextType?
    var checkReferences: Boolean
    var resolveReferences: Boolean
    val other: Map<String, Any>
}

interface CompletionProviderOptions<AsmType : Any, ContextType : Any> {
    var context: ContextType?
    val other: Map<String, Any>
}

/**
 * Options to configure the processing of a sentence
 */
interface ProcessOptions<AsmType : Any, ContextType : Any> {
    val scan: ScanOptions
    val parse: ParseOptions
    val syntaxAnalysis: SyntaxAnalysisOptions<AsmType>
    val semanticAnalysis: SemanticAnalysisOptions<AsmType, ContextType>
    val completionProvider: CompletionProviderOptions<AsmType, ContextType>
}
