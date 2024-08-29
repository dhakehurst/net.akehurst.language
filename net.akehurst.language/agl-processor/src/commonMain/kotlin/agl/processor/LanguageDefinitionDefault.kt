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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.base.DefinitionBlockDefault
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.language.base.DefinitionBlock
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.LanguageProcessorConfiguration
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ProcessOptions
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionDefault<AsmType : Any, ContextType : Any>(
    override val identity: QualifiedName,
    grammarStrArg: String?,
    private val aglOptions: ProcessOptions<DefinitionBlock<Grammar>, ContextFromGrammarRegistry>?,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    DefinitionBlockDefault(emptyList()),
    buildForDefaultGoal,
    initialConfiguration
) {

    private var _doObservableUpdates = true

    override val isModifiable: Boolean = true

    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = LanguageProcessorConfigurationEmpty(
            targetGrammarName = this.targetGrammarName,
            defaultGoalRuleName = this.defaultGoalRule,
            regexEngineKind = this._regexEngineKind,
            scannerKind = this._scannerKind,
            scannerResolver = this._scannerResolver,
            parserResolver = this._parserResolver,
            typeModelResolver = this._typeModelResolver,
            crossReferenceModelResolver = this._crossReferenceModelResolver,
            syntaxAnalyserResolver = this._syntaxAnalyserResolver,
            semanticAnalyserResolver = this._semanticAnalyserResolver,
            formatterResolver = this._formatterResolver,
            styleResolver = this._styleResolver, //not used to create processor
            completionProvider = this._completionProviderResolver
        )
        set(value) {
            this.updateConfiguration(value)
            super._processor_cache.reset()
        }

    override var grammarStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (_doObservableUpdates) {
            updateGrammarStr(oldValue, newValue)
        }
    }

    override var crossReferenceModelStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (_doObservableUpdates) {
            updateCrossReferenceModelStr(oldValue, newValue)
        }
    }

    /*
        override var formatStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
            if (oldValue != newValue) {
                super._formatterResolver = {
                    if (null==newValue) {
                        ProcessResultDefault(null, emptyList())
                    } else {
                        Agl.registry.agl.formatter.processor!!.process(newValue)
                    }
                }
            }
        }
    */
    override var styleStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (_doObservableUpdates) {
            updateStyleStr(oldValue, newValue)
        }
    }

    init {
        grammarStr = grammarStrArg
    }

    override fun update(grammarStr: String?, crossReferenceModelStr: String?, styleStr: String?) {
        this._doObservableUpdates = false
        val oldGrammarStr = this.grammarStr
        val oldScopeModelStr = this.crossReferenceModelStr
        val oldStyleStr = this.styleStr
        this.grammarStr = grammarStr
        this.crossReferenceModelStr = crossReferenceModelStr
        this.styleStr = styleStr
        updateGrammarStr(oldGrammarStr, grammarStr)
        updateCrossReferenceModelStr(oldScopeModelStr, crossReferenceModelStr)
        updateStyleStr(oldStyleStr, styleStr)
        this._doObservableUpdates = true
    }

    private fun updateConfiguration(configuration: LanguageProcessorConfiguration<AsmType, ContextType>) {
        this._doObservableUpdates = false
        this.targetGrammarName = configuration.targetGrammarName
        this.defaultGoalRule = configuration.defaultGoalRuleName
        this._typeModelResolver = configuration.typeModelResolver
        this._crossReferenceModelResolver = configuration.crossReferenceModelResolver
        this._syntaxAnalyserResolver = configuration.syntaxAnalyserResolver
        this._semanticAnalyserResolver = configuration.semanticAnalyserResolver
        this._formatterResolver = configuration.formatterResolver
        this._styleResolver = configuration.styleResolver
        this._completionProviderResolver = configuration.completionProvider
        this._doObservableUpdates = true
    }

    private fun updateGrammarStr(oldValue: String?, newValue: String?) {
        if (oldValue != newValue) {
            val res = Agl.grammarFromString<DefinitionBlock<Grammar>, ContextFromGrammarRegistry>(newValue, aglOptions)
            this._issues.addAll(res.issues)
            this.grammarList = when {
                res.issues.errors.isNotEmpty() -> DefinitionBlockDefault(emptyList())
                else -> res.asm ?: DefinitionBlockDefault(emptyList())
            }
            grammarStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun updateCrossReferenceModelStr(oldValue: String?, newValue: String?) {
        if (oldValue != newValue) {
            super._crossReferenceModelResolver = {
                when {
                    (null == newValue) -> {
                        ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
                    }

                    (null == this.typeModel) -> {
                        val ih = IssueHolder(LanguageProcessorPhase.ALL)
                        ih.error(null, "TypeModel for LanguageDefinition should not be null")
                        ProcessResultDefault(null, ih)
                    }

                    else -> {
                        val res = CrossReferenceModelDefault.fromString(ContextFromTypeModel(this.typeModel!!), newValue)
                        //val res = Agl.registry.agl.crossReference.processor!!.process(newValue)
                        when {
                            res.issues.errors.isEmpty() && null != res.asm -> _issues.addAll(res.issues) //add non-errors if any
                            res.issues.errors.isNotEmpty() -> _issues.addAll(res.issues)
                            null == res.asm -> error("Internal error: no CrossReferenceModel, but no errors reported")
                            else -> error("Internal error: situation not handled")
                        }
                        res
                    }
                }
            }
            crossReferenceModelStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun updateStyleStr(oldValue: String?, newValue: String?) {
        if (oldValue != newValue) {
            super._styleResolver = {
                if (null == newValue) {
                    ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
                } else {
                    val res = Agl.registry.agl.style.processor!!.process(newValue)
                    when {
                        res.issues.errors.isEmpty() && null != res.asm -> _issues.addAll(res.issues) //add non-errors if any
                        res.issues.errors.isNotEmpty() -> _issues.addAll(res.issues)
                        null == res.asm -> error("Internal error: no StyleModel, but no errors reported")
                        else -> error("Internal error: situation not handled")
                    }
                    res
                }
            }
            styleStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }
}