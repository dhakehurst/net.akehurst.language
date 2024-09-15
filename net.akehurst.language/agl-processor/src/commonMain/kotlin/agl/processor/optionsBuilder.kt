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
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.api.ParseOptions

@DslMarker
annotation class ProcessOptionsDslMarker

@ProcessOptionsDslMarker
class ScanOptionsBuilder(
    base: ScanOptions
) {

    fun build(): ScanOptions {
        return ScanOptionsDefault()
    }
}

@ProcessOptionsDslMarker
class ParseOptionsBuilder(
    base: ParseOptions
) {
    private var _goalRuleName: String? = base.goalRuleName
    private var _reportErrors: Boolean = base.reportErrors
    private var _reportGrammarAmbiguities = base.reportGrammarAmbiguities
    private var _cacheSkip: Boolean = base.cacheSkip

    fun goalRuleName(value: String?) {
        _goalRuleName = value
    }

    fun reportErrors(value: Boolean) {
        _reportErrors = value
    }

    fun reportGrammarAmbiguities(value: Boolean) {
        _reportGrammarAmbiguities = value
    }

    fun cacheSkip(value: Boolean) {
        _cacheSkip = value
    }

    fun build(): ParseOptions {
        return ParseOptionsDefault(
            _goalRuleName, _reportErrors, _reportGrammarAmbiguities, _cacheSkip
        )
    }
}

@ProcessOptionsDslMarker
class ProcessOptionsBuilder<AsmType : Any, ContextType : Any>(
    base: ProcessOptions<AsmType, ContextType>
) {

    private var _scan: ScanOptions = base.scan
    private var _parse: ParseOptions = base.parse
    private var _syntaxAnalyser: SyntaxAnalysisOptions<AsmType> = base.syntaxAnalysis
    private var _semanticAnalyser: SemanticAnalysisOptions<AsmType, ContextType> = base.semanticAnalysis
    private var _completionProvider: CompletionProviderOptions<AsmType, ContextType> = base.completionProvider

    fun scan(base: ScanOptions = ScanOptionsDefault(), init: ScanOptionsBuilder.() -> Unit) {
        val b = ScanOptionsBuilder(base)
        b.init()
        _scan = b.build()
    }

    fun parse(base: ParseOptions = ParseOptionsDefault(), init: ParseOptionsBuilder.() -> Unit) {
        val b = ParseOptionsBuilder(base)
        b.init()
        _parse = b.build()
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

    fun completionProvider(init: CompletionProviderOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = CompletionProviderOptionsBuilder<AsmType, ContextType>()
        b.init()
        _completionProvider = b.build()
    }

    fun build(): ProcessOptions<AsmType, ContextType> {
        return ProcessOptionsDefault<AsmType, ContextType>(
            _scan, _parse,
            _syntaxAnalyser, _semanticAnalyser, _completionProvider
        )
    }
}

@ProcessOptionsDslMarker
class SyntaxAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true

    fun active(value: Boolean) {
        _active = value
    }

    fun build(): SyntaxAnalysisOptions<AsmType> {
        return SyntaxAnalysisOptionsDefault<AsmType>(_active)
    }
}

@ProcessOptionsDslMarker
class SemanticAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true
    private var _locationMap = emptyMap<Any, InputLocation>()
    private var _context: ContextType? = null
    private var _checkReferences = true
    private var _resolveReferences = true
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

    fun checkReferences(value: Boolean) {
        _checkReferences = value
    }

    fun resolveReferences(value: Boolean) {
        _resolveReferences = value
    }

    fun option(key: String, value: Any) {
        _options[key] = value
    }

    fun build(): SemanticAnalysisOptions<AsmType, ContextType> {
        return SemanticAnalysisOptionsDefault<AsmType, ContextType>(
            _active,
            _locationMap,
            _context,
            _checkReferences,
            _resolveReferences,
            _options
        )
    }
}

@ProcessOptionsDslMarker
class CompletionProviderOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _context: ContextType? = null
    private val _options = mutableMapOf<String, Any>()

    fun context(value: ContextType?) {
        _context = value
    }

    fun option(key: String, value: Any) {
        _options[key] = value
    }

    fun build(): CompletionProviderOptions<AsmType, ContextType> {
        return CompletionProviderOptionsDefault<AsmType, ContextType>(_context, _options)
    }
}