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
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.asm.GrammarDomainDefault

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionDefault<AsmType : Any, ContextType : Any>(
    override val identity: LanguageIdentity,
    private val aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>?,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    GrammarDomainDefault(SimpleName(identity.last)),
    buildForDefaultGoal
) {

    private var _doObservableUpdates = true

    override val isModifiable: Boolean = true

    private var _configuration: LanguageProcessorConfiguration<AsmType, ContextType> = initialConfiguration
    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = _configuration
        set(value) {
            this.updateFromConfigurationWithoutNotification(value)
        }

    override var grammarString: GrammarString? = null; private set

    override var typesString: TypesString? = null; private set

    override var asmTransformString: AsmTransformString? = null; private set

    override var crossReferenceString: CrossReferenceString? = null; private set

    override var styleString: StyleString? = null; private set

    override var formatString: FormatString? = null; private set

    init {
        updateFromConfigurationWithoutNotification(initialConfiguration)
    }

    override fun update(grammarString: GrammarString?, typesString: TypesString?, asmTransformString: AsmTransformString?, crossReferenceString: CrossReferenceString?, styleString: StyleString?, formatString: FormatString?) {
        val oldGrammarStr = this.grammarString
        val oldTypesStr = this.typesString
        val oldTransformStr = this.asmTransformString
        val oldCrossReferenceStr = this.crossReferenceString
        val oldStyleStr = this.styleString
        val oldFormatStr = this.formatString

        val newConfig = Agl.configuration(base = this.configuration) {
            grammarString?.let { grammarString(it) }
            typesString?.let { typesString(it) }
            asmTransformString?.let { transformString(it) }
            crossReferenceString?.let { crossReferenceString(it) }
            styleString?.let { styleString(it) }
            formatString?.let { formatString(it) }
        }
        updateFromConfigurationWithoutNotification(newConfig)

        notifyGrammarStringObservers(oldGrammarStr,this.grammarString)
        notifyTypesStringObservers(oldTypesStr, this.typesString)
        notifyTransformStringObservers(oldTransformStr, this.asmTransformString)
        notifyCrossReferenceStringObservers(oldCrossReferenceStr, this.crossReferenceString)
        notifyStyleStringObservers(oldStyleStr, this.styleString)
        notifyFormatStringObservers(oldFormatStr, this.formatString)
    }

    fun updateFromConfigurationWithoutNotification(configuration: LanguageProcessorConfiguration<AsmType, ContextType>) {
        this._doObservableUpdates = false
        this._issues.clear()
        this._configuration = configuration

        val oldGrammarStr = this.grammarString

        this.targetGrammarName = configuration.targetGrammarName
        this.defaultGoalRule = configuration.defaultGoalRuleName

//        this._regexEngineKind = configuration.regexEngineKind
//        this._scannerKind = configuration.scannerKind

        this.grammarString = configuration.grammarString
        this.typesString = configuration.typesString
        this.asmTransformString = configuration.asmTransformString
        this.crossReferenceString = configuration.crossReferenceString
        this.styleString = configuration.styleString
        this.formatString = configuration.formatString

//        this._scannerResolver = configuration.scannerResolver
//        this._parserResolver = configuration.parserResolver
//        this._typeModelResolver = configuration.typesResolver
//        this._asmTransformModelResolver = configuration.transformResolver
//        this._crossReferenceModelResolver = configuration.crossReferenceResolver
//        this._syntaxAnalyserResolver = configuration.syntaxAnalyserResolver
//        this._semanticAnalyserResolver = configuration.semanticAnalyserResolver
//
//        this._formatterResolver = configuration.formatResolver
//        this._completionProviderResolver = configuration.completionProviderResolver
        this._styleResolver = configuration.styleResolver

        updateGrammar(oldGrammarStr, configuration.grammarString)

        this._doObservableUpdates = true
        super._processor_cache.reset()
        super._style_cache.reset()
    }

    internal fun updateGrammar(oldValue: GrammarString?, newValue: GrammarString?) {
        if (oldValue != newValue) {
            val res = Agl.grammarFromString<GrammarDomain, SentenceContextAny>(newValue?.value, aglOptions)
            this._issues.addAllFrom(res.allIssues)
            this.grammarDomain = when {
                res.allIssues.errors.isNotEmpty() -> GrammarDomainDefault(SimpleName("Error"))
                else -> res.asm ?: GrammarDomainDefault(SimpleName(identity.last))
            }
        }
    }

    private fun notifyGrammarStringObservers(oldValue: GrammarString?, newValue: GrammarString?) {
        if (oldValue != newValue) {
            grammarStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun notifyTypesStringObservers(oldValue: TypesString?, newValue: TypesString?) {
        if (oldValue != newValue) {
            typesStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun notifyTransformStringObservers(oldValue: AsmTransformString?, newValue: AsmTransformString?) {
        if (oldValue != newValue) {
            asmTransformStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun notifyCrossReferenceStringObservers(oldValue: CrossReferenceString?, newValue: CrossReferenceString?) {
        if (oldValue != newValue) {
            crossReferenceStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun notifyStyleStringObservers(oldValue: StyleString?, newValue: StyleString?) {
        if (oldValue != newValue) {
            styleStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }

    private fun notifyFormatStringObservers(oldValue: FormatString?, newValue: FormatString?) {
        if (oldValue != newValue) {
            formatterStrObservers.forEach { it.invoke(oldValue, newValue) }
        }
    }
}