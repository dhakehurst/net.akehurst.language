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

import net.akehurst.language.agl.*
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.asm.TypeModelSimple
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionDefault<AsmType : Any, ContextType : Any>(
    override val identity: LanguageIdentity,
    grammarStrArg: GrammarString?,
    private val aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    GrammarModelDefault(SimpleName(identity.last)),
    buildForDefaultGoal,
    initialConfiguration
) {

    private var _doObservableUpdates = true

    override val isModifiable: Boolean = true

    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() {
            return LanguageProcessorConfigurationEmpty(
                targetGrammarName = this.targetGrammarName,
                defaultGoalRuleName = this.defaultGoalRule,
                regexEngineKind = this._regexEngineKind,
                scannerKind = this._scannerKind,
                scannerResolver = this._scannerResolver,
                parserResolver = this._parserResolver,
                transformResolver = this._asmTransformModelResolver,
                typesResolver = this._typeModelResolver,
                crossReferenceResolver = this._crossReferenceModelResolver,
                syntaxAnalyserResolver = this._syntaxAnalyserResolver,
                semanticAnalyserResolver = this._semanticAnalyserResolver,
                formatResolver = this._formatterResolver,
                styleResolver = this._styleResolver, //not used to create processor
                completionProviderResolver = this._completionProviderResolver
            )
        }
        set(value) {
            this.updateFromConfiguration(value)
        }

    override var grammarString: GrammarString? by Delegates.observable(null) { _, oldValue, newValue ->
        updateGrammarStr(oldValue, newValue)
    }

    override var typesString: TypesString? by Delegates.observable(null) { _, oldValue, newValue ->
        updateTypeModelStr(oldValue, newValue)
    }

    override var transformString: TransformString? by Delegates.observable(null) { _, oldValue, newValue ->
        updateAsmTransformStr(oldValue, newValue)
    }

    override var crossReferenceString: CrossReferenceString? by Delegates.observable(null) { _, oldValue, newValue ->
        updateCrossReferenceStr(oldValue, newValue)
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
    override var styleString: StyleString? by Delegates.observable(null) { _, oldValue, newValue ->
        updateStyleStr(oldValue, newValue)
    }

    override var formatString: FormatString? by Delegates.observable(null) { _, oldValue, newValue ->
        //updateStyleStr(oldValue, newValue)
    }

    init {
        grammarString = grammarStrArg
    }

    override fun update(grammarStr: GrammarString?, typeModelStr: TypesString?, asmTransformStr: TransformString?, crossReferenceStr: CrossReferenceString?, styleStr: StyleString?) {
        this._doObservableUpdates = false
        this._issues.clear()
        val oldGrammarStr = this.grammarString
        val oldTypeModelStr = this.typesString
        val oldTransformStr = this.transformString
        val oldCrossReferenceStr = this.crossReferenceString
        val oldStyleStr = this.styleString
        this.grammarString = grammarStr
        this.typesString = typeModelStr
        this.transformString = asmTransformStr
        this.crossReferenceString = crossReferenceStr
        this.styleString = styleStr
        updateGrammarStr(oldGrammarStr, grammarStr)
        updateTypeModelStr(oldTypeModelStr, typeModelStr)
        updateAsmTransformStr(oldTransformStr, asmTransformStr)
        updateCrossReferenceStr(oldCrossReferenceStr, crossReferenceStr)
        updateStyleStr(oldStyleStr, styleStr)
        this._doObservableUpdates = true
    }

    private fun updateFromConfiguration(configuration: LanguageProcessorConfiguration<AsmType, ContextType>) {
        this._doObservableUpdates = false
        this.targetGrammarName = configuration.targetGrammarName
        this.defaultGoalRule = configuration.defaultGoalRuleName
        this._typeModelResolver = configuration.typesResolver
        this._crossReferenceModelResolver = configuration.crossReferenceResolver
        this._syntaxAnalyserResolver = configuration.syntaxAnalyserResolver
        this._semanticAnalyserResolver = configuration.semanticAnalyserResolver
        this._formatterResolver = configuration.formatResolver
        this._styleResolver = configuration.styleResolver
        this._completionProviderResolver = configuration.completionProviderResolver
        this._doObservableUpdates = true
        super._processor_cache.reset()
    }

    private fun updateGrammarStr(oldValue: GrammarString?, newValue: GrammarString?) {
        if (oldValue != newValue) {
            val res = Agl.grammarFromString<GrammarModel, ContextFromGrammarRegistry>(newValue?.value, aglOptions)
            this._issues.addAllFrom(res.issues)
            this.grammarModel = when {
                res.issues.errors.isNotEmpty() -> GrammarModelDefault(SimpleName("Error"))
                else -> res.asm ?: GrammarModelDefault(SimpleName(identity.last))
            }
            if (_doObservableUpdates) {
                grammarStrObservers.forEach { it.invoke(oldValue, newValue) }
            }
        }
    }

    private fun updateTypeModelStr(oldValue: TypesString?, newValue: TypesString?) {
        if (oldValue != newValue) {
            super._typeModelResolver = {
                when {
                    (null == newValue) -> {
                        ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
                    }
                    else -> {
                        val res = TypeModelSimple.fromString(SimpleName("FromGrammar"+grammarModel!!.name.value),ContextFromGrammar.createContextFrom(grammarModel!!), newValue)
                        when {
                            res.issues.errors.isEmpty() && null != res.asm -> _issues.addAllFrom(res.issues) //add non-errors if any
                            res.issues.errors.isNotEmpty() -> _issues.addAllFrom(res.issues)
                            null == res.asm -> error("Internal error: no TypeModel, but no errors reported")
                            else -> error("Internal error: situation not handled")
                        }
                        res
                    }
                }
            }
            if (_doObservableUpdates) {
                typeModelStrObservers.forEach { it.invoke(oldValue, newValue) }
            }
        }
    }

    private fun updateAsmTransformStr(oldValue: TransformString?, newValue: TransformString?) {
        if (oldValue != newValue) {
            super._asmTransformModelResolver = {
                when {
                    (null == newValue) -> {
                        ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
                    }
                    (null == this.processor?.baseTypeModel) -> {
                        val ih = IssueHolder(LanguageProcessorPhase.ALL)
                        ih.error(null, "BaseTypeModel for LanguageDefinition should not be null")
                        ProcessResultDefault(null, ih)
                    }
                    else -> {
                        val context = ContextFromGrammarAndTypeModel(grammarModel, processor!!.baseTypeModel)
                        val res = TransformDomainDefault.fromString(context, newValue)
                        when {
                            res.issues.errors.isEmpty() && null != res.asm -> _issues.addAllFrom(res.issues) //add non-errors if any
                            res.issues.errors.isNotEmpty() -> _issues.addAllFrom(res.issues)
                            null == res.asm -> error("Internal error: no AsmTransformModel, but no errors reported")
                            else -> error("Internal error: situation not handled")
                        }
                        res
                    }
                }
            }
            if (_doObservableUpdates) {
                asmTransformStrObservers.forEach { it.invoke(oldValue, newValue) }
            }
        }
    }

    private fun updateCrossReferenceStr(oldValue: CrossReferenceString?, newValue: CrossReferenceString?) {
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
                            res.issues.errors.isEmpty() && null != res.asm -> _issues.addAllFrom(res.issues) //add non-errors if any
                            res.issues.errors.isNotEmpty() -> _issues.addAllFrom(res.issues)
                            null == res.asm -> error("Internal error: no CrossReferenceModel, but no errors reported")
                            else -> error("Internal error: situation not handled")
                        }
                        res
                    }
                }
            }
            if (_doObservableUpdates) {
                crossReferenceStrObservers.forEach { it.invoke(oldValue, newValue) }
            }
        }
    }

    private fun updateStyleStr(oldValue: StyleString?, newValue: StyleString?) {
        if (oldValue != newValue) {
            super._styleResolver = {
                if (null == newValue) {
                    ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
                } else {
                    val res = Agl.registry.agl.style.processor!!.process(newValue.value)
                    when {
                        res.issues.errors.isEmpty() && null != res.asm -> _issues.addAllFrom(res.issues) //add non-errors if any
                        res.issues.errors.isNotEmpty() -> _issues.addAllFrom(res.issues)
                        null == res.asm -> error("Internal error: no StyleModel, but no errors reported")
                        else -> error("Internal error: situation not handled")
                    }
                    res
                }
            }
            if (_doObservableUpdates) {
                styleStrObservers.forEach { it.invoke(oldValue, newValue) }
            }
        }
    }
}