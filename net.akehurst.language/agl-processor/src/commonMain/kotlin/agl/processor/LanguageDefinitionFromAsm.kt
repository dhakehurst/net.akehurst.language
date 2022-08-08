/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.processor.SemanticAnalyserResolver
import net.akehurst.language.api.processor.SyntaxAnalyserResolver
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
class LanguageDefinitionFromAsm<AsmType : Any, ContextType : Any>(
    override val identity: String,
    grammar: Grammar,
    override var targetGrammar: String?,
    override var defaultGoalRule: String?,
    buildForDefaultGoal:Boolean,
    style: String?,
    format: String?,
    syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?,
    semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
) : LanguageDefinition<AsmType, ContextType> {
    //constructor(identity: String, grammar: Grammar) : this(identity, grammar, null, null, null, null, null, null)

    private val _grammarAsm: Grammar = grammar
    private val _processor_cache: CachedValue<LanguageProcessor<AsmType, ContextType>?> = cached {
        val config = Agl.configuration<AsmType, ContextType> {
            defaultGoalRuleName(defaultGoalRule)
            syntaxAnalyserResolver(syntaxAnalyserResolver)
            semanticAnalyserResolver(semanticAnalyserResolver)
        }
        val proc = Agl.processorFromGrammar(_grammarAsm, config)
        if(buildForDefaultGoal) proc.buildFor(null) //null options will use default goal
        proc
    }
    private var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null
    private var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null

    override val grammarObservers = mutableListOf<(String?, String?) -> Unit>()
    override val styleObservers = mutableListOf<(String?, String?) -> Unit>()
    override val formatObservers = mutableListOf<(String?, String?) -> Unit>()

    override var grammarStr: String?
        get() = this._grammarAsm.toString() //TODO:
        set(value) {
            error("Cannot set the grammar of a LanguageDefinitionFromAsm using a String")
        }

    override var style: String? by Delegates.observable(style) { _, oldValue, newValue ->
        styleObservers.forEach { it(oldValue, newValue) }
    }

    override var format: String? by Delegates.observable(format) { _, oldValue, newValue ->
        formatObservers.forEach { it(oldValue, newValue) }
    }

    override var syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?
        get() = _syntaxAnalyserResolver
        set(value) {
            _syntaxAnalyserResolver = value
            this.syntaxAnalyser = value?.invoke(this._grammarAsm)
        }

    override var semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
        get() = _semanticAnalyserResolver
        set(value) {
            _semanticAnalyserResolver = value
            this.semanticAnalyser = value?.invoke(this._grammarAsm)
        }

    override val processor: LanguageProcessor<AsmType, ContextType>? get() = this._processor_cache.value

    override val grammarIsModifiable: Boolean = false
}