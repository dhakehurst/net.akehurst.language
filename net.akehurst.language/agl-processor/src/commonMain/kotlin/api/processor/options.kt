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
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.scanner.api.ScanOptions

/**
 * Options to configure the syntax analysis of a Shared Packed Parse Tree (SPPT)
 */
interface SyntaxAnalysisOptions<AsmType : Any> {
    /** false: `process` stops after parsing */
    var enabled: Boolean

    /**
     * true: `process` builds the ASM while parsing (no parse tree is kept), see `SyntaxAnalysisStream`.
     * Only for a syntax analyser that uses asm transform rules (SyntaxAnalyserFromAsmTransformAbstract);
     * for any other syntax analyser, `process` parses to a tree as usual and gives a warning.
     * An ambiguity is an error when streaming, and the parse stops.
     * The parse result has no tree (sppt is null).
     * false (default): parse to a tree, then walk it.
     */
    var streaming: Boolean

    fun clone(): SyntaxAnalysisOptions<AsmType>
}

/**
 * Options to configure the semantic analysis of an Abstract Syntax Model (ASM)
 */
interface SemanticAnalysisOptions<ContextType : Any> {
    /** false: `process` stops after syntax analysis */
    var enabled: Boolean
    /** the location in the sentence of each ASM object, set by syntax analysis */
    var locationMap: LocationMap
    /** the context the sentence is analysed in (e.g. `contextAsmSimple()`); if null, references are not built, checked or resolved */
    var sentenceContext: ContextType?
    /** build the scopes defined by the references DSL */
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

    /** report references that cannot be resolved */
    var checkReferences: Boolean
    /** set resolved references in the ASM */
    var resolveReferences: Boolean
    /** options for a specific semantic analyser */
    val other: Map<String, Any>

    fun clone(): SemanticAnalysisOptions<ContextType>
}

/**
 * Options for `expectedItemsAt` (code completion)
 */
interface CompletionProviderOptions<ContextType : Any> {
    var sentenceContext: ContextType?

    /**
     * depth of nested rules to search when constructing possible completions
     **/
    var depth: Int

    /**
     * list of (depth, ruleName) pairs
     */
    var path: List<Pair<Int, Int>>

    /**
     * true (default): optional items, and lists with min 0, are expanded both with and without the item(s).
     * false: they are left out, only the expansion without them is given.
     * TODO: future enhancement, more flexibility in configuring the output (e.g. show them as a placeholder)
     */
    var showOptionalItems: Boolean

    var provideValuesForPatternTerminals: Boolean

    /**
     * depth for named rules (rule name -> depth), used instead of the current depth when the rule is expanded.
     * Set by a completion definition (AGL completion language, 'depth' action).
     */
    var ruleDepth: Map<String, Int>

    val other: Map<String, Any>

    fun clone(): CompletionProviderOptions<ContextType>
}

/**
 * Options for formatting an ASM as text
 */
interface FormatOptions<SelfType : Any> {
    /** locationMap for the FormatString */
    var locationMap: LocationMap?

    /** named values available in format expressions */
    val environment: Map<String, SelfType>

    fun clone(): FormatOptions<SelfType>
}

/**
 * Options to configure the processing of a sentence, one group per phase.
 * Build them with `Agl.options { parse { ... } syntaxAnalysis { ... } semanticAnalysis { ... } completionProvider { ... } }`
 */
interface ProcessOptions<AsmType : Any, ContextType : Any> {
    val scan: ScanOptions
    val parse: ParseOptions
    val syntaxAnalysis: SyntaxAnalysisOptions<AsmType>
    val semanticAnalysis: SemanticAnalysisOptions<ContextType>
    val completionProvider: CompletionProviderOptions<ContextType>
    val format: FormatOptions<AsmType>

    fun clone(): ProcessOptions<AsmType, ContextType>
}
