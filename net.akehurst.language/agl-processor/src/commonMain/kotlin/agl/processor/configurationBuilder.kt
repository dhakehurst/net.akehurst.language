/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.regex.agl.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand

@DslMarker
annotation class LanguageProcessorConfigurationDslMarker

@LanguageProcessorConfigurationDslMarker
class LanguageProcessorConfigurationBuilder<AsmType : Any, ContextType : Any>(
    val base: LanguageProcessorConfiguration<AsmType, ContextType>
) {

    private var _grammarString: GrammarString? = null
    private var _typesString: TypesString? = null
    private var _transformString: TransformString? = null
    private var _crossReferenceString: CrossReferenceString? = null
    private var _styleString: StyleString? = null
    private var _formatString: FormatString? = null

    private var _regexEngineKind: RegexEngineKind = base.regexEngineKind
    private var _scannerKind: ScannerKind = base.scannerKind
    private var _targetGrammarName: SimpleName? = base.targetGrammarName
    private var _defaultGoalRuleName: GrammarRuleName? = base.defaultGoalRuleName
    private var _scannerResolver: ScannerResolver<AsmType, ContextType>? = base.scannerResolver
    private var _parserResolver: ParserResolver<AsmType, ContextType>? = base.parserResolver
    private var _asmTransformResolver: TransformResolver<AsmType, ContextType>? = base.transformResolver
    private var _typeModelResolver: TypesResolver<AsmType, ContextType>? = base.typesResolver

    //    private var _asmFactoryResolver: AsmFactoryResolver<AsmFactory<AsmType,*,*>>? = base.asmFactoryResolver
    private var _crossReferenceModelResolver: CrossReferenceResolver<AsmType, ContextType>? = base.crossReferenceResolver
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = base.syntaxAnalyserResolver
    private var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = base.semanticAnalyserResolver
    private var _formatModelResolver: FormatResolver<AsmType, ContextType>? = base.formatResolver
    private var _styleResolver: StyleResolver<AsmType, ContextType>? = base.styleResolver
    private var _completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? = base.completionProviderResolver

    fun targetGrammarName(value: String?) {
        _targetGrammarName = value?.let { SimpleName(it) }
    }

    fun defaultGoalRuleName(value: String?) {
        _defaultGoalRuleName = value?.let { GrammarRuleName(it) }
    }

    fun grammarString(value: GrammarString?) {
        _grammarString = value
    }

    fun typesString(value: TypesString?) {
        _typesString = value
    }

    fun transformString(value: TransformString?) {
        _transformString = value
    }

    fun crossReferenceString(value: CrossReferenceString?) {
        _crossReferenceString = value
    }

    fun styleString(value: StyleString?) {
        _styleString = value
    }

    fun formatString(value: FormatString?) {
        _formatString = value
    }

    fun scanner(scannerKind: ScannerKind, regexEngineKind: RegexEngineKind) {
        this._scannerResolver = {
            val regexEngine = when (regexEngineKind) {
                RegexEngineKind.PLATFORM -> RegexEnginePlatform
                RegexEngineKind.AGL -> RegexEngineAgl
            }
            val scanner = when (scannerKind) {
                ScannerKind.Classic -> ScannerClassic(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
                ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
            }
            ProcessResultDefault(scanner, IssueHolder(LanguageProcessorPhase.ALL))
        }
    }

    fun scannerResolver(func: ScannerResolver<AsmType, ContextType>?) {
        this._scannerResolver = func
    }

    fun parserResolver(func: ParserResolver<AsmType, ContextType>?) {
        this._parserResolver = func
    }

    fun transformResolver(func: TransformResolver<AsmType, ContextType>?) {
        this._asmTransformResolver = func
    }

    fun typesResolver(func: TypesResolver<AsmType, ContextType>?) {
        this._typeModelResolver = func
    }

//    fun asmFactoryResolver(func: AsmFactoryResolver<AsmFactory<AsmType,*,*>>) {
//        this._asmFactoryResolver = func
//   }

    fun crossReferenceResolver(func: CrossReferenceResolver<AsmType, ContextType>?) {
        _crossReferenceModelResolver = func
    }

    fun syntaxAnalyserResolver(func: SyntaxAnalyserResolver<AsmType, ContextType>?) {
        _syntaxAnalyserResolver = func
    }

    fun syntaxAnalyserResolverResult(func: () -> SyntaxAnalyser<AsmType>) { //TODO: others need this
        _syntaxAnalyserResolver = { ProcessResultDefault(func.invoke(), IssueHolder(LanguageProcessorPhase.ALL)) }
    }

    fun semanticAnalyserResolver(value: SemanticAnalyserResolver<AsmType, ContextType>?) {
        _semanticAnalyserResolver = value
    }

    fun semanticAnalyserResolverResult(func: () -> SemanticAnalyser<AsmType, ContextType>) { //TODO: others need this
        _semanticAnalyserResolver = { ProcessResultDefault(func.invoke(), IssueHolder(LanguageProcessorPhase.ALL)) }
    }

    fun formatResolver(func: FormatResolver<AsmType, ContextType>?) {
        _formatModelResolver = func
    }

    fun styleResolver(func: StyleResolver<AsmType, ContextType>?) {
        _styleResolver = func
    }

    fun completionProvider(value: CompletionProviderResolver<AsmType, ContextType>?) {
        _completionProviderResolver = value
    }

    fun build(): LanguageProcessorConfiguration<AsmType, ContextType> {
        return LanguageProcessorConfigurationEmpty<AsmType, ContextType>(
            targetGrammarName = _targetGrammarName,
            defaultGoalRuleName = _defaultGoalRuleName,
            grammarString = _grammarString,
            typesString = _typesString,
            transformString =  _transformString,
            crossReferenceString = _crossReferenceString,
            styleString = _styleString,
            formatString = _formatString,

            regexEngineKind = _regexEngineKind,
            scannerKind = _scannerKind,
            scannerResolver = _scannerResolver,
            parserResolver = _parserResolver,
            typesResolver = _typeModelResolver,
            transformResolver = _asmTransformResolver,
            //        _asmFactoryResolver,
            crossReferenceResolver = _crossReferenceModelResolver,
            syntaxAnalyserResolver = _syntaxAnalyserResolver,
            semanticAnalyserResolver = _semanticAnalyserResolver,
            formatResolver = _formatModelResolver,
            styleResolver = _styleResolver,
            completionProviderResolver = _completionProviderResolver
        )
    }
}
