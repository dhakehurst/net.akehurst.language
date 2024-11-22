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

import net.akehurst.language.agl.*
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.AsmFactory
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

abstract class LanguageDefinitionAbstract<AsmType:Any, ContextType : Any>(
    argGrammarModel: GrammarModel,
    argBuildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType,  ContextType>
) : LanguageDefinition<AsmType, ContextType> {

    abstract override val identity: LanguageIdentity
    abstract override var grammarStr: GrammarString?

    override var grammarModel: GrammarModel by Delegates.observable(argGrammarModel) { _, oldValue, newValue ->
        // check not same Grammar object,
        // the qname of the grammar might be the same but a different object with different rules
        if (oldValue !== newValue) {
            this._processor_cache.reset()
            this.grammarObservers.forEach { it(oldValue, newValue) }
        }
    }

    override val targetGrammar: Grammar? get() = this.grammarModel.allDefinitions.lastOrNull { it.name == this.targetGrammarName } ?: this.grammarModel.primary

    abstract override val isModifiable: Boolean

    override var targetGrammarName: SimpleName? by Delegates.observable(initialConfiguration.targetGrammarName) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override var defaultGoalRule: GrammarRuleName? by Delegates.observable(initialConfiguration.defaultGoalRuleName) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    abstract override var crossReferenceModelStr: CrossReferenceString?

    override val typeModel: TypeModel?
        get() = this.processor?.typeModel

    override val crossReferenceModel: CrossReferenceModel?
        get() = this.processor?.crossReferenceModel
//        set(value) {
//            val oldValue = this.processor?.crossReferenceModel
//            if (oldValue != value) {
//                _crossReferenceModelResolver = { ProcessResultDefault(value, IssueHolder(LanguageProcessorPhase.ALL)) }
//                crossReferenceModelObservers.forEach { it(oldValue, value) }
//            }
//        }

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
    //abstract override var aglOptions: ProcessOptions<DefinitionBlock<Grammar>, GrammarContext>?

    override val processor: LanguageProcessor<AsmType, ContextType>? get() = this._processor_cache.value

    override val issues: IssueCollection<LanguageIssue> get() = _issues

    abstract override var styleStr: StyleString?

    override val style: AglStyleModel?
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
//        set(value) {
//            val oldValue = _style
//            if (oldValue != value) {
//                _styleResolver = { ProcessResultDefault(value, IssueHolder(LanguageProcessorPhase.ALL)) }
//                styleObservers.forEach { it(oldValue, value) }
//            }
//        }

    override val processorObservers = mutableListOf<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>()
    override val grammarStrObservers = mutableListOf<(oldValue: GrammarString?, newValue: GrammarString?) -> Unit>()
    override val grammarObservers = mutableListOf<(oldValue: GrammarModel, newValue: GrammarModel) -> Unit>()
    override val crossReferenceModelStrObservers = mutableListOf<(oldValue: CrossReferenceString?, newValue: CrossReferenceString?) -> Unit>()

    //override val crossReferenceModelObservers = mutableListOf<(oldValue: CrossReferenceModel?, newValue: CrossReferenceModel?) -> Unit>()
    override val formatterStrObservers = mutableListOf<(oldValue: FormatString?, newValue: FormatString?) -> Unit>()

    //override val formatterObservers = mutableListOf<(oldValue: AglFormatterModel?, newValue: AglFormatterModel?) -> Unit>()
    override val styleStrObservers = mutableListOf<(oldValue: StyleString?, newValue: StyleString?) -> Unit>()
    //override val styleObservers = mutableListOf<(oldValue: AglStyleModel?, newValue: AglStyleModel?) -> Unit>()

    override fun toString(): String = identity.value

    // --- implementation ---

    protected val _processor_cache: CachedValue<LanguageProcessor<AsmType, ContextType>?> = cached {
        val g = this.targetGrammar
        if (null == g) {
            null //if no targetGrammar, don't provide a processor
        } else {
            val proc = Agl.processorFromGrammar(this.grammarModel, this.configuration)
            if (argBuildForDefaultGoal) proc.buildFor(null) //null options will use default goal
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

    protected var _regexEngineKind = initialConfiguration.regexEngineKind //TODO: make observable
    protected var _scannerKind = initialConfiguration.scannerKind

    protected var _crossReferenceModelResolver: CrossReferenceModelResolver<AsmType, ContextType>? by Delegates.observable(initialConfiguration.crossReferenceModelResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _scannerResolver: ScannerResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.scannerResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _parserResolver: ParserResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.parserResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _typeModelResolver: TypeModelResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.typeModelResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.syntaxAnalyserResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.semanticAnalyserResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _formatterResolver: FormatterResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.formatterResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _styleResolver: StyleResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.styleResolver) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _completionProviderResolver: CompletionProviderResolver<AsmType,  ContextType>? by Delegates.observable(initialConfiguration.completionProvider) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
}