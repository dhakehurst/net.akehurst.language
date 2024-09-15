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

import net.akehurst.language.agl.regex.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.agl.scanner.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.processor.*

@DslMarker
annotation class LanguageProcessorConfigurationDslMarker

@LanguageProcessorConfigurationDslMarker
class LanguageProcessorConfigurationBuilder<AsmType : Any, ContextType : Any>(
    val base: LanguageProcessorConfiguration<AsmType, ContextType>
) {

    private var _regexEngineKind: RegexEngineKind = base.regexEngineKind
    private var _scannerKind: ScannerKind = base.scannerKind
    private var _targetGrammarName: SimpleName? = base.targetGrammarName
    private var _defaultGoalRuleName: GrammarRuleName? = base.defaultGoalRuleName
    private var _scannerResolver: ScannerResolver<AsmType, ContextType>? = base.scannerResolver
    private var _parserResolver: ParserResolver<AsmType, ContextType>? = base.parserResolver
    private var _asmTransformResolver: AsmTransformModelResolver<AsmType, ContextType>? = base.asmTransformModelResolver
    private var _typeModelResolver: TypeModelResolver<AsmType, ContextType>? = base.typeModelResolver
    private var _crossReferenceModelResolver: CrossReferenceModelResolver<AsmType, ContextType>? = base.crossReferenceModelResolver
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = base.syntaxAnalyserResolver
    private var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = base.semanticAnalyserResolver
    private var _formatterResolver: FormatterResolver<AsmType, ContextType>? = base.formatterResolver
    private var _styleResolver: StyleResolver<AsmType, ContextType>? = base.styleResolver
    private var _completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? = base.completionProvider

    fun targetGrammarName(value: SimpleName?) {
        _targetGrammarName = value
    }

    fun defaultGoalRuleName(value: String?) {
        _defaultGoalRuleName = value?.let { GrammarRuleName(it) }
    }

    fun scanner(scannerKind: ScannerKind, regexEngineKind: RegexEngineKind) {
        this._scannerResolver = {
            val regexEngine = when (regexEngineKind) {
                RegexEngineKind.PLATFORM -> RegexEnginePlatform
                RegexEngineKind.AGL -> RegexEngineAgl
            }
            val scanner = when (scannerKind) {
                ScannerKind.Classic -> ScannerClassic(regexEngine, it.ruleSet.terminals)
                ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.ruleSet.terminals)
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

    fun asmTransformResolver(func: AsmTransformModelResolver<AsmType, ContextType>?) {
        this._asmTransformResolver = func
    }

    fun typeModelResolver(func: TypeModelResolver<AsmType, ContextType>?) {
        this._typeModelResolver = func
    }

    fun crossReferenceModelResolver(func: CrossReferenceModelResolver<AsmType, ContextType>?) {
        _crossReferenceModelResolver = func
    }

    fun syntaxAnalyserResolver(func: SyntaxAnalyserResolver<AsmType, ContextType>?) {
        _syntaxAnalyserResolver = func
    }

    fun semanticAnalyserResolver(value: SemanticAnalyserResolver<AsmType, ContextType>?) {
        _semanticAnalyserResolver = value
    }

    fun formatterResolver(func: FormatterResolver<AsmType, ContextType>?) {
        _formatterResolver = func
    }

    fun styleResolver(func: StyleResolver<AsmType, ContextType>?) {
        _styleResolver = func
    }

    fun completionProvider(value: CompletionProviderResolver<AsmType, ContextType>?) {
        _completionProviderResolver = value
    }

    fun build(): LanguageProcessorConfiguration<AsmType, ContextType> {
        return LanguageProcessorConfigurationEmpty<AsmType, ContextType>(
            _targetGrammarName,
            _defaultGoalRuleName,
            _regexEngineKind,
            _scannerKind,
            _scannerResolver,
            _parserResolver,
            _asmTransformResolver,
            _typeModelResolver,
            _crossReferenceModelResolver,
            _syntaxAnalyserResolver,
            _semanticAnalyserResolver,
            _formatterResolver,
            _styleResolver,
            _completionProviderResolver
        )
    }
}
