/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

abstract class LanguageDefinitionAbstract<AsmType : Any, ContextType : Any>(
    defaultGoalRuleArg: String?,
    grammarArg: Grammar?,
    buildForDefaultGoal: Boolean,
    scopeModelArg:ScopeModel?,
    styleArg: String?,
    formatArg: String?,
    syntaxAnalyserResolverArg: SyntaxAnalyserResolver<AsmType, ContextType>?,
    semanticAnalyserResolverArg: SemanticAnalyserResolver<AsmType, ContextType>?,
    aglOptionsArg: ProcessOptions<List<Grammar>, GrammarContext>?
): LanguageDefinition<AsmType, ContextType> {

    protected val _processor_cache: CachedValue<LanguageProcessor<AsmType, ContextType>?> = cached {
        val g = this.grammar
        if (null == g) {
            null
        } else {
            val config = Agl.configuration<AsmType, ContextType> {
                targetGrammarName(this@LanguageDefinitionAbstract.targetGrammar)
                defaultGoalRuleName(this@LanguageDefinitionAbstract.defaultGoalRule)
                scopeModel(this@LanguageDefinitionAbstract.scopeModel)
                syntaxAnalyserResolver(this@LanguageDefinitionAbstract.syntaxAnalyserResolver)
                semanticAnalyserResolver(this@LanguageDefinitionAbstract.semanticAnalyserResolver)
            }
            val proc = Agl.processorFromGrammar(g, config)
            if (buildForDefaultGoal) proc.buildFor(null) //null options will use default goal
            processorObservers.forEach { it(null,proc) }
            proc
        }
    }.apply { this.resetAction = { old -> processorObservers.forEach { it(old,null) } } }

    override var defaultGoalRule: String? by Delegates.observable(defaultGoalRuleArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override var grammar: Grammar? by Delegates.observable(grammarArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
            grammarObservers.forEach { it(oldValue, newValue) }
        }
    }

    override val processor: LanguageProcessor<AsmType, ContextType>? get() = this._processor_cache.value

    override var scopeModel:ScopeModel? by Delegates.observable(scopeModelArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
            scopeModelObservers.forEach { it(oldValue, newValue) }
        }
    }

    override val syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>?
        get() = this._processor_cache.value?.let { proc -> _syntaxAnalyserResolver?.invoke(proc.grammar) }

    final override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?
        get() = _syntaxAnalyserResolver
        set(value) {
            _syntaxAnalyserResolver = value
        }

    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
        get() = this._processor_cache.value?.let { proc -> _semanticAnalyserResolver?.invoke(proc.grammar) }

    final override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
        get() = _semanticAnalyserResolver
        set(value) {
            _semanticAnalyserResolver = value
        }

    override var aglOptions: ProcessOptions<List<Grammar>, GrammarContext>? = aglOptionsArg

    final override var style: String? by Delegates.observable(styleArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            styleObservers.forEach { it(oldValue, newValue) }
        }
    }

    final override var format: String? by Delegates.observable(formatArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            formatObservers.forEach { it(oldValue, newValue) }
        }
    }

    override val processorObservers = mutableListOf<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>()
    override val grammarObservers = mutableListOf<(Grammar?, Grammar?) -> Unit>()
    override val scopeModelObservers= mutableListOf<(ScopeModel?, ScopeModel?) -> Unit>()
    override val styleObservers = mutableListOf<(String?, String?) -> Unit>()
    override val formatObservers = mutableListOf<(String?, String?) -> Unit>()

    override val issues: List<LanguageIssue> get() = _issues

    init {
        this.syntaxAnalyserResolver = syntaxAnalyserResolverArg
        this.semanticAnalyserResolver = semanticAnalyserResolverArg
    }

    override fun toString(): String = identity

    protected var _issues = listOf<LanguageIssue>()
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null
    private var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null

}