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

import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.parser.api.Parser
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.Scanner
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.types.api.TypesDomain

//typealias GrammarResolver = () -> ProcessResult<Grammar>
typealias ScannerResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<Scanner>
typealias ParserResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<Parser>
typealias TransformResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<AsmTransformDomain>
typealias TypesResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<TypesDomain>
//typealias AsmFactoryResolver<AsmFactoryType> = () -> AsmFactoryType
typealias CrossReferenceResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<CrossReferenceDomain>
typealias SyntaxAnalyserResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<SyntaxAnalyser<AsmType>>
typealias SemanticAnalyserResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<SemanticAnalyser<AsmType, ContextType>>
typealias FormatResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<AglFormatDomain>
typealias StyleResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<AglStyleDomain>
typealias CompletionProviderResolver<AsmType,  ContextType> = (LanguageProcessor<AsmType,  ContextType>) -> ProcessResult<CompletionProvider<AsmType, ContextType>>

/**
 * Configuration for the constructing a language processor
 * @param targetGrammarName name of one of the grammars in the grammarDefinitionString to generate parser for (if null use last grammar found)
 * @param goalRuleName name of the default goal rule to use, it must be one of the rules in the target grammar or its super grammars (if null use first non-skip rule found in target grammar)
 * @param syntaxAnalyser a syntax analyser (if null use SyntaxAnalyserSimple)
 * @param semanticAnalyser a semantic analyser (if null use SemanticAnalyserSimple)
 * @param formatter a formatter
 */
interface LanguageProcessorConfiguration<AsmType:Any, ContextType : Any> {
    //val grammarResolver: GrammarResolver?
    val targetGrammarName: SimpleName?
    val defaultGoalRuleName: GrammarRuleName?

    val regexEngineKind: RegexEngineKind
    val scannerKind: ScannerKind

    val grammarString: GrammarString?
    val typesString: TypesString?
    val asmTransformString: AsmTransformString?
    val crossReferenceString: CrossReferenceString?
    val styleString: StyleString?
    val formatString:FormatString?

    val scannerResolver: ScannerResolver<AsmType, ContextType>?
    val parserResolver: ParserResolver<AsmType,  ContextType>?
    val typesResolver: TypesResolver<AsmType,  ContextType>?
    val transformResolver: TransformResolver<AsmType,  ContextType>?
//    val asmFactoryResolver: AsmFactoryResolver<AsmFactory<AsmType,*,*>>?
    val crossReferenceResolver: CrossReferenceResolver<AsmType,  ContextType>?
    val syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType,  ContextType>?
    val semanticAnalyserResolver: SemanticAnalyserResolver<AsmType,  ContextType>?

    val styleResolver: StyleResolver<AsmType,  ContextType>?
    val formatResolver: FormatResolver<AsmType,  ContextType>?
    val completionProviderResolver: CompletionProviderResolver<AsmType,  ContextType>?
}


enum class ParserKind {
    GLC
}