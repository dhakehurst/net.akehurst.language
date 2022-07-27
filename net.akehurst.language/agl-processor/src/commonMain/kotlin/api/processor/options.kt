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

import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.parser.InputLocation

/**
 * Options to configure the building of a language processor
 * @param targetGrammarName name of one of the grammars in the grammarDefinitionString to generate parser for (if null use last grammar found)
 * @param goalRuleName name of the default goal rule to use, it must be one of the rules in the target grammar or its super grammars (if null use first non-skip rule found in target grammar)
 * @param syntaxAnalyser a syntax analyser (if null use SyntaxAnalyserSimple)
 * @param semanticAnalyser a semantic analyser (if null use SemanticAnalyserSimple)
 * @param formatter a formatter
 */
interface LanguageProcessorConfiguration<AsmType : Any, ContextType : Any> {
    var targetGrammarName:String?
    var defaultGoalRuleName: String?
    var syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>?
    var semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
    var formatter: Formatter?
}

/**
 * Options to configure the parsing of a sentence
 */
interface ParseOptions {
    var goalRuleName: String?
    var automatonKind: AutomatonKind
}

/**
 * Options to configure the syntax analysis of an Shared Packed Parse Tree (SPPT)
 */
interface SyntaxAnalysisOptions<AsmType : Any, ContextType : Any> {
    var active: Boolean
    var context: ContextType?
}

/**
 * Options to configure the semantic analysis of an Abstract Syntax Model (ASM)
 */
interface SemanticAnalysisOptions<AsmType : Any, ContextType : Any> {
    var active: Boolean
    var locationMap: Map<Any, InputLocation>
}

/**
 * Options to configure the processing of a sentence
 */
interface ProcessOptions<AsmType : Any, ContextType : Any> {
    val parse: ParseOptions
    val syntaxAnalysis: SyntaxAnalysisOptions<AsmType, ContextType>
    val semanticAnalysis: SemanticAnalysisOptions<AsmType, ContextType>
}
