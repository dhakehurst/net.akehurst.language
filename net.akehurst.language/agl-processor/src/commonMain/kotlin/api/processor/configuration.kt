/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.api.processor

import net.akehurst.language.api.language.asmTransform.TransformModel
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.language.style.AglStyleModel
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.formatter.api.AglFormatterModel
import net.akehurst.language.typemodel.api.TypeModel

//typealias GrammarResolver = () -> ProcessResult<Grammar>
typealias ScannerResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<Scanner>
typealias ParserResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<Parser>
typealias AsmTransformModelResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<TransformModel>
typealias TypeModelResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<TypeModel>
typealias CrossReferenceModelResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<CrossReferenceModel>
typealias SyntaxAnalyserResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<SyntaxAnalyser<AsmType>>
typealias SemanticAnalyserResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<SemanticAnalyser<AsmType, ContextType>>
typealias FormatterResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<AglFormatterModel>
typealias StyleResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<AglStyleModel>
typealias CompletionProviderResolver<AsmType, ContextType> = (LanguageProcessor<AsmType, ContextType>) -> ProcessResult<CompletionProvider<AsmType, ContextType>>

/**
 * Configuration for the constructing a language processor
 * @param targetGrammarName name of one of the grammars in the grammarDefinitionString to generate parser for (if null use last grammar found)
 * @param goalRuleName name of the default goal rule to use, it must be one of the rules in the target grammar or its super grammars (if null use first non-skip rule found in target grammar)
 * @param syntaxAnalyser a syntax analyser (if null use SyntaxAnalyserSimple)
 * @param semanticAnalyser a semantic analyser (if null use SemanticAnalyserSimple)
 * @param formatter a formatter
 */
interface LanguageProcessorConfiguration<AsmType : Any, ContextType : Any> {
    //val grammarResolver: GrammarResolver?
    val targetGrammarName: SimpleName?
    val defaultGoalRuleName: GrammarRuleName?

    val regexEngineKind: RegexEngineKind
    val scannerKind: ScannerKind

    val scannerResolver: ScannerResolver<AsmType, ContextType>?
    val parserResolver: ParserResolver<AsmType, ContextType>?
    val asmTransformModelResolver: AsmTransformModelResolver<AsmType, ContextType>?
    val typeModelResolver: TypeModelResolver<AsmType, ContextType>?
    val crossReferenceModelResolver: CrossReferenceModelResolver<AsmType, ContextType>?
    val syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?
    val semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
    val formatterResolver: FormatterResolver<AsmType, ContextType>?
    val styleResolver: StyleResolver<AsmType, ContextType>?
    val completionProvider: CompletionProviderResolver<AsmType, ContextType>?
}

enum class ScannerKind {
    OnDemand, Classic
}

enum class RegexEngineKind {
    PLATFORM, AGL
}

enum class ParserKind {
    GLC
}