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

internal class LanguageProcessorConfigurationDefault<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: String? = null,
    override var defaultGoalRuleName: String? = null,
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? = null,
    override var formatter: Formatter? = null
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class ProcessOptionsDefault<AsmType : Any, ContextType : Any>(
    override val parse: ParseOptions = ParseOptionsDefault(),
    override val syntaxAnalysis: SyntaxAnalysisOptions<AsmType, ContextType> = SyntaxAnalysisOptionsDefault(),
    override val semanticAnalysis: SemanticAnalysisOptions<AsmType, ContextType> = SemanticAnalysisOptionsDefault()
) : ProcessOptions<AsmType, ContextType>

internal class ParseOptionsDefault(
    override var goalRuleName: String? = null,
    override var automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1
) : ParseOptions

internal class SyntaxAnalysisOptionsDefault<AsmType : Any, ContextType : Any>(
    override var active: Boolean = true,
    override var context: ContextType? = null
) : SyntaxAnalysisOptions<AsmType, ContextType>

internal class SemanticAnalysisOptionsDefault<AsmType : Any, ContextType : Any>(
    override var active: Boolean = true,
    override var locationMap: Map<Any, InputLocation> = emptyMap()
) : SemanticAnalysisOptions<AsmType, ContextType>

@DslMarker
annotation class LanguageProcessorConfigurationDslMarker

@LanguageProcessorConfigurationDslMarker
class LanguageProcessorConfigurationBuilder<AsmType : Any, ContextType : Any> {

    private var _targetGrammarName: String? = null
    private var _defaultGoalRuleName: String? = null
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null
    private var _semanticAnalyserResolver: SemanticAnalyser<AsmType, ContextType>? = null
    private var _formatter: Formatter? = null

    fun targetGrammarName(value:String?) {
        _targetGrammarName=value
    }

    fun defaultGoalRuleName(value:String?) {
        _defaultGoalRuleName=value
    }

    fun syntaxAnalyser(func: SyntaxAnalyserResolver<AsmType, ContextType>?) {
        _syntaxAnalyserResolver = func
    }

    fun semanticAnalyserResolver(value: SemanticAnalyser<AsmType, ContextType>?) {
        _semanticAnalyserResolver = value
    }

    fun formatter(value:Formatter?) {
        _formatter = value
    }

    fun build(): LanguageProcessorConfiguration<AsmType, ContextType> {
        return LanguageProcessorConfigurationDefault<AsmType, ContextType>(
            _targetGrammarName,
            _defaultGoalRuleName,
            _syntaxAnalyserResolver,
            _semanticAnalyserResolver,
            _formatter
        )
    }
}


@DslMarker
annotation class ProcessOptionsDslMarker

@ProcessOptionsDslMarker
class ProcessOptionsBuilder<AsmType : Any, ContextType : Any> {

    private var _parser:ParseOptions = ParseOptionsDefault()
    private var _syntaxAnalyser:SyntaxAnalysisOptions<AsmType, ContextType> = SyntaxAnalysisOptionsDefault<AsmType, ContextType>()
    private var _semanticAnalyser:SemanticAnalysisOptions<AsmType, ContextType> = SemanticAnalysisOptionsDefault<AsmType, ContextType>()

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

    fun semanticAnalysis(init: SemanticAnalysisOptionsBuilder<AsmType, ContextType>.() -> Unit){
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

    fun goalRuleName(value: String?) {
        _goalRuleName = value
    }

    fun automatonKind(value: AutomatonKind) {
        _automatonKind = value
    }

    fun build(): ParseOptions {
        return ParseOptionsDefault(_goalRuleName, _automatonKind)
    }
}

@ProcessOptionsDslMarker
class SyntaxAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true
    private var _context: ContextType? = null

    fun active(value: Boolean) {
        _active = value
    }

    fun context(value: ContextType) {
        _context = value
    }

    fun build(): SyntaxAnalysisOptions<AsmType, ContextType> {
        return SyntaxAnalysisOptionsDefault<AsmType, ContextType>(_active, _context)
    }
}

@ProcessOptionsDslMarker
class SemanticAnalysisOptionsBuilder<AsmType : Any, ContextType : Any>() {

    private var _active = true
    private var _locationMap = emptyMap<Any, InputLocation>()

    fun active(value: Boolean) {
        _active = value
    }

    fun locationMap(value: Map<Any, InputLocation>) {
        _locationMap = value
    }

    fun build(): SemanticAnalysisOptions<AsmType, ContextType> {
        return SemanticAnalysisOptionsDefault<AsmType, ContextType>(_active, _locationMap)
    }
}