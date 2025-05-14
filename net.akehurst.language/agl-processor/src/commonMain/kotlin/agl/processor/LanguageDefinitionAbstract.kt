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
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

abstract class LanguageDefinitionAbstract<AsmType:Any, ContextType : Any>(
    argGrammarModel: GrammarModel,
    argBuildForDefaultGoal: Boolean
) : LanguageDefinition<AsmType, ContextType> {

    abstract override val identity: LanguageIdentity

    override var grammarModel: GrammarModel? by Delegates.observable(argGrammarModel) { _, oldValue, newValue ->
        // check not same Grammar object,
        // the qname of the grammar might be the same but a different object with different rules
        if (oldValue !== newValue) {
            this._processor_cache.reset()
            this.grammarObservers.forEach { it(oldValue, newValue) }
        }
    }

    override val targetGrammar: Grammar? get() = this.grammarModel?.allDefinitions?.lastOrNull { it.name == this.targetGrammarName } ?: this.grammarModel?.primary

    abstract override val isModifiable: Boolean

    override var targetGrammarName: SimpleName? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
    override var defaultGoalRule: GrammarRuleName? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    override val typesModel: TypeModel? get() = this.processor?.typesModel
    override val transformModel: TransformModel? get() = this.processor?.transformModel
    override val crossReferenceModel: CrossReferenceModel? get() = this.processor?.crossReferenceModel
    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? get() = this.processor?.syntaxAnalyser
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? get() = this.processor?.semanticAnalyser
    override val formatter: Formatter<AsmType>? get() = this.processor?.formatter

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

    override val issues: IssueCollection<LanguageIssue> get() = _issues

    override val processor: LanguageProcessor<AsmType, ContextType>? get() = this._processor_cache.value
    override val styleModel: AglStyleModel? get() = this._style_cache.value

    override val processorObservers = mutableListOf<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>()
    override val grammarStrObservers = mutableListOf<(oldValue: GrammarString?, newValue: GrammarString?) -> Unit>()
    override val grammarObservers = mutableListOf<(oldValue: GrammarModel?, newValue: GrammarModel?) -> Unit>()
    override val typeModelStrObservers = mutableListOf<(oldValue: TypesString?, newValue: TypesString?) -> Unit>()
    override val asmTransformStrObservers = mutableListOf<(oldValue: TransformString?, newValue: TransformString?) -> Unit>()
    override val crossReferenceStrObservers = mutableListOf<(oldValue: CrossReferenceString?, newValue: CrossReferenceString?) -> Unit>()

    //override val crossReferenceModelObservers = mutableListOf<(oldValue: CrossReferenceModel?, newValue: CrossReferenceModel?) -> Unit>()
    override val formatterStrObservers = mutableListOf<(oldValue: FormatString?, newValue: FormatString?) -> Unit>()

    //override val formatterObservers = mutableListOf<(oldValue: AglFormatterModel?, newValue: AglFormatterModel?) -> Unit>()
    override val styleStrObservers = mutableListOf<(oldValue: StyleString?, newValue: StyleString?) -> Unit>()
    //override val styleObservers = mutableListOf<(oldValue: AglStyleModel?, newValue: AglStyleModel?) -> Unit>()

    override fun toString(): String = identity.value

    // --- implementation ---

    protected val _processor_cache: CachedValue<LanguageProcessor<AsmType, ContextType>?> = cached {
        val tg = this.targetGrammar
        val gm = this.grammarModel
        if (null == tg || null == gm) {
            null //if no targetGrammar, don't provide a processor
        } else {
            val proc = Agl.processorFromGrammar(gm, this.configuration)
            if (argBuildForDefaultGoal) proc.buildFor(null) //null options will use default goal
            processorObservers.forEach { it(null, proc) }
            proc
        }
    }.apply { this.resetAction = { old -> processorObservers.forEach { it(old, null) } } }

    // style is not part of the processor...only used by editors
    protected val _style_cache: CachedValue<AglStyleModel?> = cached {
        _styleResolver?.let {
            val p = this.processor
            if (null == p) {
                null
            } else {
                val r = it.invoke(p)
                _issues.addAllFrom(r.issues)
                r.asm
            }
        }
    }

    protected var _issues = IssueHolder(LanguageProcessorPhase.ALL)
/*
    protected var _regexEngineKind: RegexEngineKind by Delegates.observable(RegexEngineKind.AGL) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _scannerKind : ScannerKind by Delegates.observable(ScannerKind.OnDemand) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _crossReferenceModelResolver: CrossReferenceResolver<AsmType, ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _scannerResolver: ScannerResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _parserResolver: ParserResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _typeModelResolver: TypesResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _asmTransformModelResolver: TransformResolver<AsmType, ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _semanticAnalyserResolver: SemanticAnalyserResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _formatterResolver: FormatResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

    protected var _completionProviderResolver: CompletionProviderResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }
*/

    protected var _styleResolver: StyleResolver<AsmType,  ContextType>? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            this._processor_cache.reset()
        }
    }

}