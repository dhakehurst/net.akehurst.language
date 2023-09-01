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

import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.style.AglStyleModel

/**
 * Options to configure the building of a language processor
 * @param targetGrammarName name of one of the grammars in the grammarDefinitionString to generate parser for (if null use last grammar found)
 * @param goalRuleName name of the default goal rule to use, it must be one of the rules in the target grammar or its super grammars (if null use first non-skip rule found in target grammar)
 * @param syntaxAnalyser a syntax analyser (if null use SyntaxAnalyserSimple)
 * @param semanticAnalyser a semantic analyser (if null use SemanticAnalyserSimple)
 * @param formatter a formatter
 */

//typealias GrammarResolver = () -> ProcessResult<Grammar>
typealias ScopeModelResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<ScopeModel>
typealias TypeModelResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<GrammarTypeModel>
typealias SyntaxAnalyserResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<SyntaxAnalyser<AsmType>>
typealias SemanticAnalyserResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<SemanticAnalyser<AsmType, ContextType>>
typealias FormatterResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<AglFormatterModel>
typealias StyleResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<AglStyleModel>

interface LanguageProcessorConfiguration<AsmType : Any, ContextType : Any> {
    //val grammarResolver: GrammarResolver?
    val targetGrammarName: String?
    val defaultGoalRuleName: String?
    val typeModelResolver: TypeModelResolver<AsmType, ContextType>?
    val scopeModelResolver: ScopeModelResolver<AsmType, ContextType>?
    val syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?
    val semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
    val formatterResolver: FormatterResolver<AsmType, ContextType>?
    val styleResolver: StyleResolver<AsmType, ContextType>?
}

/**
 * Options to configure the parsing of a sentence
 */
interface ParseOptions {
    var goalRuleName: String?
    val automatonKind: AutomatonKind
    val reportErrors: Boolean
    val reportGrammarAmbiguities: Boolean
    val cacheSkip: Boolean
}

/**
 * Options to configure the syntax analysis of an Shared Packed Parse Tree (SPPT)
 */
interface SyntaxAnalysisOptions<AsmType : Any, ContextType : Any> {
    var active: Boolean
}

/**
 * Options to configure the semantic analysis of an Abstract Syntax Model (ASM)
 */
interface SemanticAnalysisOptions<AsmType : Any, ContextType : Any> {
    var active: Boolean
    var locationMap: Map<Any, InputLocation>
    var context: ContextType?
    val options: Map<String, Any>
}

/**
 * Options to configure the processing of a sentence
 */
interface ProcessOptions<AsmType : Any, ContextType : Any> {
    val parse: ParseOptions
    val syntaxAnalysis: SyntaxAnalysisOptions<AsmType, ContextType>
    val semanticAnalysis: SemanticAnalysisOptions<AsmType, ContextType>
}
