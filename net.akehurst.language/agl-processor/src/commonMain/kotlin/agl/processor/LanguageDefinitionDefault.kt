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

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionDefault<AsmType : Any, ContextType : Any>(
    override val identity: String,
    grammarStrArg: String?,
    targetGrammarArg: String?,
    defaultGoalRuleArg: String?,
    buildForDefaultGoal: Boolean,
    scopeModelStrArg:String?,
    styleArg: String?,
    formatArg: String?,
    syntaxAnalyserResolverArg: SyntaxAnalyserResolver<AsmType, ContextType>?,
    semanticAnalyserResolverArg: SemanticAnalyserResolver<AsmType, ContextType>?,
    /** the options to configure building the processor for the registered language */
    aglOptionsArg: ProcessOptions<List<Grammar>, GrammarContext>?
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    defaultGoalRuleArg,
    null,
    buildForDefaultGoal,
    null,
    styleArg,
    formatArg,
    syntaxAnalyserResolverArg,
    semanticAnalyserResolverArg,
    aglOptionsArg
) {

    private val _grammar_cache: CachedValue<Grammar?> = cached {
        val gs = this.grammarStr
        if (null == gs) {
            null
        } else {
            _grammarFromString(gs)
        }
    }.apply { this.resetAction = { old -> grammarObservers.forEach { it(old, null) } } }

    private val _scopeModel_cache: CachedValue<ScopeModel?> = cached {
        val s = this.scopeModelStr
        if (null == s || null==this.grammar) {
            null
        } else {
            val ctx =ContextFromGrammar(this.grammar!!)
            ScopeModelAgl.fromString(ctx, s).asm!! as ScopeModel
        }
    }.apply { this.resetAction = { old -> scopeModelObservers.forEach { it(old, null) } } }


    override val grammarIsModifiable: Boolean = true

    override var grammarStr: String? by Delegates.observable(grammarStrArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._grammar_cache.reset()
            this._scopeModel_cache.reset()
        }
    }

    override var targetGrammar: String? by Delegates.observable(targetGrammarArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            val s = this.grammarStr
            this.grammarStr = null //TODO: is there a better way to do this, with one change?
            this.grammarStr = s
        }
    }

    override var grammar: Grammar?
        get() = this._grammar_cache.value
        set(value) {
            val oldValue = this._grammar_cache.value
            if (value==oldValue) {
                //do nothing
            } else {
                this._grammar_cache.value = value
                this._processor_cache.reset()
                grammarObservers.forEach { it(oldValue, value) }
            }
        }

    override var scopeModelStr: String? by Delegates.observable(scopeModelStrArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._scopeModel_cache.reset()
        }
    }

    override var scopeModel: ScopeModel?
        get() = this._scopeModel_cache.value
        set(value) {
            val oldValue = this._scopeModel_cache.value
            if (value==oldValue) {
                //do nothing
            } else {
                this._scopeModel_cache.value = value
                this._processor_cache.reset()
                scopeModelObservers.forEach { it(oldValue, value) }
            }
        }

    private fun _grammarFromString(newValue: String?): Grammar? {
        return if (null == newValue) {
            null
        } else {
            val g = Agl.registry.agl.grammar.processor!!.process(newValue, aglOptions)
            this._issues = g.issues
            if (g.issues.isEmpty()) {
                val tgtName = this.targetGrammar ?: g.asm!!.first().name
                g.asm!!.firstOrNull { it.name == tgtName }
            } else {
                null
            }
        }
    }
}