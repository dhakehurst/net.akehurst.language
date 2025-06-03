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

import net.akehurst.language.api.processor.CompletionProviderOptions
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SyntaxAnalysisOptions
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.scanner.api.ScanOptions
import net.akehurst.language.scanner.common.ScanOptionsDefault
import net.akehurst.language.sentence.api.InputLocation

@DslMarker
annotation class ProcessOptionsDslMarker

@ProcessOptionsDslMarker
class ScanOptionsBuilder(
    base: ScanOptions
) {
    private var _enabled: Boolean = true
    private var _resultsByLine: Boolean = false

    fun enabled(value: Boolean) {
        _enabled = value
    }

    fun resultsByLine(value: Boolean) {
        _resultsByLine = value
    }

    fun build(): ScanOptions {
        return ScanOptionsDefault(_enabled, _resultsByLine)
    }
}

@ProcessOptionsDslMarker
class ParseOptionsBuilder(
    base: ParseOptions
) {
    private var _enabled: Boolean = true
    private var _goalRuleName: String? = base.goalRuleName
    private  var _sentenceIdentity = base.sentenceIdentity
    private var _reportErrors: Boolean = base.reportErrors
    private var _reportGrammarAmbiguities = base.reportGrammarAmbiguities
    private var _cacheSkip: Boolean = base.cacheSkip

    fun enabled(value: Boolean) {
        _enabled = value
    }

    fun goalRuleName(value: String?) {
        _goalRuleName = value
    }

    fun sentenceIdentity(func: ()->Any?) {
        _sentenceIdentity = func
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
            _enabled, _goalRuleName, _sentenceIdentity, _reportErrors, _reportGrammarAmbiguities, _cacheSkip
        )
    }
}

@ProcessOptionsDslMarker
class ProcessOptionsBuilder<AsmType : Any, ContextType : Any>(
    base: ProcessOptions<AsmType, ContextType>
) {

    private var _scan: ScanOptions = base.scan.clone()
    private var _parse: ParseOptions = base.parse.clone()
    private var _syntaxAnalyser: SyntaxAnalysisOptions<AsmType> = base.syntaxAnalysis.clone()
    private var _semanticAnalyser: SemanticAnalysisOptions<ContextType> = base.semanticAnalysis.clone()
    private var _completionProvider: CompletionProviderOptions<ContextType> = base.completionProvider.clone()

    fun scan(init: ScanOptionsBuilder.() -> Unit) {
        val b = ScanOptionsBuilder(_scan)
        b.init()
        _scan = b.build()
    }

    fun parse(init: ParseOptionsBuilder.() -> Unit) {
        val b = ParseOptionsBuilder(_parse)
        b.init()
        _parse = b.build()
    }

    fun syntaxAnalysis(init: SyntaxAnalysisOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = SyntaxAnalysisOptionsBuilder<AsmType, ContextType>(_syntaxAnalyser)
        b.init()
        _syntaxAnalyser = b.build()
    }

    fun semanticAnalysis(init: SemanticAnalysisOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = SemanticAnalysisOptionsBuilder<AsmType, ContextType>(_semanticAnalyser)
        b.init()
        _semanticAnalyser = b.build()
    }

    fun completionProvider(init: CompletionProviderOptionsBuilder<AsmType, ContextType>.() -> Unit) {
        val b = CompletionProviderOptionsBuilder<AsmType, ContextType>(_completionProvider)
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
class SyntaxAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>(
    base: SyntaxAnalysisOptions<AsmType>
) {

    private var _enabled = base.enabled

    fun enabled(value: Boolean) {
        _enabled = value
    }

    fun build(): SyntaxAnalysisOptions<AsmType> {
        return SyntaxAnalysisOptionsDefault<AsmType>(_enabled)
    }
}

@ProcessOptionsDslMarker
class SemanticAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>(
    base: SemanticAnalysisOptions<ContextType>
) {

    private var _enabled = base.enabled
    private var _locationMap = base.locationMap
    private var _context: ContextType? = base.context
    private var _buildScope = base.buildScope
    private var _replaceIfItemAlreadyExistsInScope = base.replaceIfItemAlreadyExistsInScope
    private var _ifItemAlreadyExistsInScopeIssueKind: LanguageIssueKind? = base.ifItemAlreadyExistsInScopeIssueKind
    private var _checkReferences = base.checkReferences
    private var _resolveReferences = base.resolveReferences
    private val _options = base.other.toMutableMap()

    fun enabled(value: Boolean) {
        _enabled = value
    }

    fun locationMap(value: LocationMap) {
        _locationMap = value
    }

    fun context(value: ContextType?) {
        _context = value
    }

    fun buildScope(value: Boolean) {
        _buildScope = value
    }

    fun replaceIfItemAlreadyExistsInScope(value: Boolean) {
        _replaceIfItemAlreadyExistsInScope = value
    }

    fun ifItemAlreadyExistsInScopeIssueKind(value: LanguageIssueKind?) {
        _ifItemAlreadyExistsInScopeIssueKind = value
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

    fun build(): SemanticAnalysisOptions<ContextType> {
        return SemanticAnalysisOptionsDefault<ContextType>(
            _enabled,
            _locationMap,
            _context,
            _buildScope,
            _replaceIfItemAlreadyExistsInScope,
            _ifItemAlreadyExistsInScopeIssueKind,
            _checkReferences,
            _resolveReferences,
            _options
        )
    }
}

@ProcessOptionsDslMarker
class CompletionProviderOptionsBuilder<AsmType : Any, ContextType : Any>(
    base: CompletionProviderOptions<ContextType>
) {

    private var _context: ContextType? = base.context
    private var _depth: Int = base.depth
    private var _path: List<Pair<Int,Int>> = base.path
    private var _showOptionalItems = base.showOptionalItems
    private var _provideValuesForPatternTerminals = base.provideValuesForPatternTerminals
    private val _options = base.other.toMutableMap()

    fun context(value: ContextType?) {
        _context = value
    }

    fun depth(value: Int) {
        _depth = value
    }

    fun path(value:List<Pair<Int,Int>>) {
        _path = value
    }

    fun showOptionalItems(value:Boolean) {
        _showOptionalItems = value
    }

    fun provideValuesForPatternTerminals(value: Boolean) {
        _provideValuesForPatternTerminals = value
    }

    fun option(key: String, value: Any) {
        _options[key] = value
    }

    fun build(): CompletionProviderOptions<ContextType> {
        return CompletionProviderOptionsDefault<ContextType>(
            context = _context,
            depth = _depth,
            path = _path,
            showOptionalItems = _showOptionalItems,
            provideValuesForPatternTerminals = _provideValuesForPatternTerminals,
            other = _options
        )
    }
}