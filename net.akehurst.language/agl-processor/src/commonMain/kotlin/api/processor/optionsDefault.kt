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

internal class LanguageProcessorConfigurationDefault<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: String? = null,
    override var defaultGoalRuleName: String? = null,
    override var typeModelResolver: TypeModelResolver<AsmType, ContextType>? = null,
    override var scopeModelResolver: ScopeModelResolver<AsmType, ContextType>? = null,
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,
    override var formatterResolver: FormatterResolver<AsmType, ContextType>? = null,
    override var styleResolver: StyleResolver<AsmType, ContextType>? = null
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class ProcessOptionsDefault<AsmType : Any, ContextType : Any>(
    override val parse: ParseOptions = ParseOptionsDefault(),
    override val syntaxAnalysis: SyntaxAnalysisOptions<AsmType, ContextType> = SyntaxAnalysisOptionsDefault(),
    override val semanticAnalysis: SemanticAnalysisOptions<AsmType, ContextType> = SemanticAnalysisOptionsDefault()
) : ProcessOptions<AsmType, ContextType>

internal class ParseOptionsDefault(
    override var goalRuleName: String? = null,
    override var automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1,
    override var reportErrors: Boolean = true
) : ParseOptions

internal class SyntaxAnalysisOptionsDefault<AsmType : Any, ContextType : Any>(
    override var active: Boolean = true
) : SyntaxAnalysisOptions<AsmType, ContextType>

internal class SemanticAnalysisOptionsDefault<AsmType : Any, ContextType : Any>(
    override var active: Boolean = true,
    override var locationMap: Map<Any, InputLocation> = emptyMap(),
    override var context: ContextType? = null,
    override val options: Map<String, Any> = mutableMapOf()
) : SemanticAnalysisOptions<AsmType, ContextType>

@DslMarker
annotation class LanguageProcessorConfigurationDslMarker

@LanguageProcessorConfigurationDslMarker
class LanguageProcessorConfigurationBuilder<AsmType : Any, ContextType : Any>(
    val base: LanguageProcessorConfiguration<AsmType, ContextType>?
) {

    private var _targetGrammarName: String? = null
    private var _defaultGoalRuleName: String? = null
    private var _typeModelResolver: TypeModelResolver<AsmType, ContextType>? = null
    private var _scopeModelResolver: ScopeModelResolver<AsmType, ContextType>? = null
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null
    private var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null
    private var _formatterResolver: FormatterResolver<AsmType, ContextType>? = null
    private var _styleResolver: StyleResolver<AsmType, ContextType>? = null

    fun targetGrammarName(value: String?) {
        _targetGrammarName = value
    }

    fun defaultGoalRuleName(value: String?) {
        _defaultGoalRuleName = value
    }

    fun typeModelResolver(func: TypeModelResolver<AsmType, ContextType>?) {
        this._typeModelResolver = func
    }

    fun scopeModelResolver(func: ScopeModelResolver<AsmType, ContextType>?) {
        _scopeModelResolver = func
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

    fun build(): LanguageProcessorConfiguration<AsmType, ContextType> {
        return when (base) {
            null -> LanguageProcessorConfigurationDefault<AsmType, ContextType>(
                _targetGrammarName,
                _defaultGoalRuleName,
                _typeModelResolver,
                _scopeModelResolver,
                _syntaxAnalyserResolver,
                _semanticAnalyserResolver,
                _formatterResolver,
                _styleResolver
            )

            is LanguageProcessorConfigurationDefault<AsmType, ContextType> -> {
                _targetGrammarName?.let { base.targetGrammarName = it }
                _defaultGoalRuleName?.let { base.defaultGoalRuleName = it }
                _typeModelResolver?.let { base.typeModelResolver = it }
                _scopeModelResolver?.let { base.scopeModelResolver = it }
                _syntaxAnalyserResolver?.let { base.syntaxAnalyserResolver = it }
                _semanticAnalyserResolver?.let { base.semanticAnalyserResolver = it }
                _formatterResolver?.let { base.formatterResolver = it }
                _styleResolver?.let { base.styleResolver = it }

                base
            }

            else -> error("Cannot override LanguageProcessorConfiguration of type ${base::class.simpleName}")
        }
    }
}

@DslMarker
annotation class ProcessOptionsDslMarker

@ProcessOptionsDslMarker
class ProcessOptionsBuilder<AsmType : Any, ContextType : Any> {

    private var _parser: ParseOptions = ParseOptionsDefault()
    private var _syntaxAnalyser: SyntaxAnalysisOptions<AsmType, ContextType> = SyntaxAnalysisOptionsDefault<AsmType, ContextType>()
    private var _semanticAnalyser: SemanticAnalysisOptions<AsmType, ContextType> = SemanticAnalysisOptionsDefault<AsmType, ContextType>()

    fun parse(init: ParseOptionsBuilder.() -> Unit) {
        val b = ParseOptionsBuilder()
        b.init()
        _parser = b.build()
    }

    fun syntaxAnalysis(init: SyntaxAnalysisOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = SyntaxAnalysisOptionsBuilder<AsmType, ContextType>()
        b.init()
        _syntaxAnalyser = b.build()
    }

    fun semanticAnalysis(init: SemanticAnalysisOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = SemanticAnalysisOptionsBuilder<AsmType, ContextType>()
        b.init()
        _semanticAnalyser = b.build()
    }

    fun build(): ProcessOptions<AsmType, ContextType> {
        return ProcessOptionsDefault<AsmType, ContextType>(_parser, _syntaxAnalyser, _semanticAnalyser)
    }
}

@ProcessOptionsDslMarker
class ParseOptionsBuilder {
    private var _goalRuleName: String? = null
    private var _automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1
    private var _reportErrors: Boolean = true

    fun goalRuleName(value: String?) {
        _goalRuleName = value
    }

    fun automatonKind(value: AutomatonKind) {
        _automatonKind = value
    }

    fun reportErrors(value: Boolean) {
        _reportErrors = value
    }

    fun build(): ParseOptions {
        return ParseOptionsDefault(_goalRuleName, _automatonKind, _reportErrors)
    }
}

@ProcessOptionsDslMarker
class SyntaxAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true

    fun active(value: Boolean) {
        _active = value
    }

    fun build(): SyntaxAnalysisOptions<AsmType, ContextType> {
        return SyntaxAnalysisOptionsDefault<AsmType, ContextType>(_active)
    }
}

@ProcessOptionsDslMarker
class SemanticAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true
    private var _locationMap = emptyMap<Any, InputLocation>()
    private var _context: ContextType? = null
    private val _options = mutableMapOf<String, Any>()

    fun active(value: Boolean) {
        _active = value
    }

    fun locationMap(value: Map<Any, InputLocation>) {
        _locationMap = value
    }

    fun context(value: ContextType?) {
        _context = value
    }

    fun option(key: String, value: Any) {
        _options[key] = value
    }

    fun build(): SemanticAnalysisOptions<AsmType, ContextType> {
        return SemanticAnalysisOptionsDefault<AsmType, ContextType>(_active, _locationMap, _context, _options)
    }
}