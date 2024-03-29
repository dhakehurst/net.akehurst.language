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

import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

abstract class LanguageDefinitionAbstract<AsmType : Any, ContextType : Any>(
    grammar: Grammar?,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinition<AsmType, ContextType> {

    abstract override val identity: String
    abstract override var grammarStr: String?

    override var grammar: Grammar? by Delegates.observable(grammar) { _, oldValue, newValue ->
        // check not same Grammar object,
        // the qname of the grammar might be the same but a different object with different rules
        if (oldValue !== newValue) {
            this._processor_cache.reset()
            this.grammarObservers.forEach { it(oldValue, newValue) }
        }
    }

    abstract override val isModifiable: Boolean

    override var targetGrammarName: String? by Delegates.observable(initialConfiguration.targetGrammarName) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override var defaultGoalRule: String? by Delegates.observable(initialConfiguration.defaultGoalRuleName) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    abstract override var scopeModelStr: String?

    override var scopeModel: ScopeModel?
        get() = this.processor?.scopeModel
        set(value) {
            val oldValue = this.processor?.scopeModel
            if (oldValue != value) {
                _scopeModelResolver = { ProcessResultDefault(value, IssueHolder(LanguageProcessorPhase.ALL)) }
                scopeModelObservers.forEach { it(oldValue, value) }
            }
        }

    override val syntaxAnalyser: SyntaxAnalyser<AsmType>?
        get() = this.processor?.syntaxAnalyser

    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
        get() = this.processor?.semanticAnalyser

    override val formatter: Formatter<AsmType>?
        get() = this.processor?.formatter

    /*
    abstract override var formatStr: String?

    override var formatterModel: AglFormatterModel?
        get() = this.processor?.formatterModel
        set(value) {
            val oldValue = this.processor?.formatterModel
            if (oldValue != value) {
                _formatterResolver = { ProcessResultDefault(value, emptyList()) }
                formatterObservers.forEach { it(oldValue, value) }
            }
        }
*/
    //abstract override var aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?

    override val processor: LanguageProcessor<AsmType, ContextType>? get() = this._processor_cache.value

    override val issues: IssueCollection<LanguageIssue> get() = _issues

    abstract override var styleStr: String?

    override var style: AglStyleModel?
        get() {
            return if (null == _style) {
                _styleResolver?.let {
                    val p = this.processor
                    if (null == p) {
                        null
                    } else {
                        val r = it.invoke(p)
                        _issues.addAll(r.issues)
                        r.asm
                    }
                }
            } else {
                _style
            }
        }
        set(value) {
            val oldValue = _style
            if (oldValue != value) {
                _styleResolver = { ProcessResultDefault(value, IssueHolder(LanguageProcessorPhase.ALL)) }
                styleObservers.forEach { it(oldValue, value) }
            }
        }

    override val processorObservers = mutableListOf<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>()
    override val grammarStrObservers = mutableListOf<(oldValue: String?, newValue: String?) -> Unit>()
    override val grammarObservers = mutableListOf<(oldValue: Grammar?, newValue: Grammar?) -> Unit>()
    override val scopeStrObservers = mutableListOf<(oldValue: String?, newValue: String?) -> Unit>()
    override val scopeModelObservers = mutableListOf<(oldValue: ScopeModel?, newValue: ScopeModel?) -> Unit>()
    override val formatterStrObservers = mutableListOf<(oldValue: String?, newValue: String?) -> Unit>()
    override val formatterObservers = mutableListOf<(oldValue: AglFormatterModel?, newValue: AglFormatterModel?) -> Unit>()
    override val styleStrObservers = mutableListOf<(oldValue: String?, newValue: String?) -> Unit>()
    override val styleObservers = mutableListOf<(oldValue: AglStyleModel?, newValue: AglStyleModel?) -> Unit>()

    override fun toString(): String = identity

    // --- implementation ---

    protected val _processor_cache: CachedValue<LanguageProcessor<AsmType, ContextType>?> = cached {
        val g = this.grammar
        if (null == g) {
            null
        } else {
            val proc = Agl.processorFromGrammar(g, this.configuration)
            if (buildForDefaultGoal) proc.buildFor(null) //null options will use default goal
            processorObservers.forEach { it(null, proc) }
            proc
        }
    }.apply { this.resetAction = { old -> processorObservers.forEach { it(old, null) } } }

    //private var _grammar_cache: CachedValue<Grammar?> = cached {
    //    val res = this._grammarResolver?.invoke()
    //    this._issues.addAll(res?.issues?: emptyList())
    //    res?.asm
    //}.apply { this.resetAction = { old -> grammarObservers.forEach { it(old, null) } } }

    protected var _issues = IssueHolder(LanguageProcessorPhase.ALL)
    protected val _style: AglStyleModel? = null

    protected var _scopeModelResolver: ScopeModelResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.scopeModelResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _typeModelResolver: TypeModelResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.typeModelResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.syntaxAnalyserResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.semanticAnalyserResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _formatterResolver: FormatterResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.formatterResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _styleResolver: StyleResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.styleResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    protected var _completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.completionProvider) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
}