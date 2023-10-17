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

import net.akehurst.language.agl.agl.grammar.format.AglFormatCompletionProvider
import net.akehurst.language.agl.agl.grammar.format.AglFormatSemanticAnalyser
import net.akehurst.language.agl.agl.grammar.grammar.AglGrammarCompletionProvider
import net.akehurst.language.agl.agl.grammar.scopes.AglScopesCompletionProvider
import net.akehurst.language.agl.agl.grammar.scopes.AglScopesSemanticAnalyser
import net.akehurst.language.agl.agl.grammar.style.AglStyleCompletionProvider
import net.akehurst.language.agl.agl.grammar.style.AglStyleSemanticAnalyser
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.grammar.format.AglFormatGrammar
import net.akehurst.language.agl.grammar.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.grammar.scopes.AglScopesGrammar
import net.akehurst.language.agl.grammar.scopes.AglScopesSyntaxAnalyser
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.grammar.style.AglStyleGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.style.AglStyleModel

interface AglLanguages {
    val grammarLanguageIdentity: String
    val styleLanguageIdentity: String
    val formatLanguageIdentity: String
    val scopesLanguageIdentity: String

    val grammar: LanguageDefinition<List<Grammar>, GrammarContext>
    val style: LanguageDefinition<AglStyleModel, SentenceContext<String>>
    val formatter: LanguageDefinition<AglFormatterModel, SentenceContext<String>>
    val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<String>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _grammars = mutableMapOf<String, Grammar>()
    private val _registry = mutableMapOf<String, LanguageDefinition<*, *>>()

    override val agl: AglLanguages = object : AglLanguages {
        override val grammarLanguageIdentity: String = AglGrammarGrammar.qualifiedName
        override val styleLanguageIdentity: String = AglStyleGrammar.qualifiedName
        override val formatLanguageIdentity: String = AglFormatGrammar.qualifiedName
        override val scopesLanguageIdentity: String = AglScopesGrammar.qualifiedName

        override val grammar: LanguageDefinition<List<Grammar>, GrammarContext> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                AglGrammarGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglGrammarGrammar.name)
                    defaultGoalRuleName(AglGrammarGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AglGrammarGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglGrammarSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglGrammarSemanticAnalyser(this@LanguageRegistryDefault), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglGrammarGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglGrammarCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<ScopeModelAgl, SentenceContext<String>>(
                identity = scopesLanguageIdentity,
                AglScopesGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglScopesGrammar.name)
                    defaultGoalRuleName(AglScopesGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AglScopesGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglScopesSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglScopesSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglScopesGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglScopesCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val formatter = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<AglFormatterModel, SentenceContext<String>>(
                identity = formatLanguageIdentity,
                AglFormatGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglFormatGrammar.name)
                    defaultGoalRuleName(AglFormatGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AglFormatGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver {
                        ProcessResultDefault(
                            AglFormatSyntaxAnalyser(it.grammar!!.qualifiedName, it.typeModel!!, it.scopeModel!!),
                            IssueHolder(LanguageProcessorPhase.ALL)
                        )
                    }
                    semanticAnalyserResolver { ProcessResultDefault(AglFormatSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglFormatGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglFormatCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val style: LanguageDefinition<AglStyleModel, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                AglStyleGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglStyleGrammar.name)
                    defaultGoalRuleName(AglStyleGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglStyleSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglStyleSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver {  }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglStyleGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglStyleCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )
    }

    fun <AsmType : Any, ContextType : Any> registerFromDefinition(definition: LanguageDefinition<AsmType, ContextType>): LanguageDefinition<AsmType, ContextType> {
        return if (this._registry.containsKey(definition.identity)) {
            error("LanguageDefinition '${definition.identity}' is already registered, please unregister the old one first")
        } else {
            this._registry[definition.identity] = definition
            definition
        }
    }

    override fun <AsmType : Any, ContextType : Any> register(
        identity: String,
        grammarStr: String?,
        aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType> = this.registerFromDefinition(
        LanguageDefinitionDefault<AsmType, ContextType>(
            identity = identity,
            grammarStrArg = grammarStr,
            aglOptions = aglOptions,
            buildForDefaultGoal = buildForDefaultGoal,
            initialConfiguration = configuration
        )
    )

    override fun unregister(identity: String) {
        this._registry.remove(identity)
    }

    override fun <AsmType : Any, ContextType : Any> findOrNull(identity: String): LanguageDefinition<AsmType, ContextType>? {
        return this._registry[identity] as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull("$localNamespace.$nameOrQName") ?: findOrNull(nameOrQName)
    }

    override fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: String,
        aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType> {
        val existing = this.findOrNull<AsmType, ContextType>(identity)
        return if (null == existing) {
            val placeholder = LanguageDefinitionDefault<AsmType, ContextType>(
                identity = identity,
                grammarStrArg = null,
                aglOptions = aglOptions,
                buildForDefaultGoal = false,
                initialConfiguration = configuration ?: Agl.configurationEmpty()
            )
            registerFromDefinition(placeholder)
        } else {
            existing
        }
    }

    fun findGrammarOrNull(qualifiedName: String): Grammar? = _grammars[qualifiedName]

    override fun findGrammarOrNull(localNamespace: Namespace, nameOrQName: String): Grammar? =
        findGrammarOrNull("${localNamespace.qualifiedName}.$nameOrQName") ?: findGrammarOrNull(nameOrQName)

    override fun registerGrammar(grammar: Grammar) {
        _grammars[grammar.qualifiedName] = grammar
    }
}