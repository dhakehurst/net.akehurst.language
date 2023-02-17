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

import net.akehurst.language.agl.agl.grammar.scopes.AglScopesSemanticAnalyser
import net.akehurst.language.agl.agl.grammar.style.AglStyleSemanticAnalyser
import net.akehurst.language.agl.grammar.format.AglFormatGrammar
import net.akehurst.language.agl.grammar.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.*
import net.akehurst.language.agl.grammar.scopes.AglScopesGrammar
import net.akehurst.language.agl.grammar.scopes.AglScopesSyntaxAnalyser
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.grammar.style.AglStyleGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.style.AglStyleModel

interface AglLanguages {
    val grammarLanguageIdentity: String
    val styleLanguageIdentity: String
    val formatLanguageIdentity: String
    val scopesLanguageIdentity: String

    val grammar: LanguageDefinition<List<Grammar>, GrammarContext>
    val style: LanguageDefinition<AglStyleModel, SentenceContext<GrammarItem>>
    val formatter: LanguageDefinition<AglFormatterModel, SentenceContext<GrammarItem>>
    val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<GrammarItem>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _registry = mutableMapOf<String, LanguageDefinition<*, *>>()

    val agl: AglLanguages = object : AglLanguages {
        override val grammarLanguageIdentity: String = AglGrammarGrammar.qualifiedName
        override val styleLanguageIdentity: String = AglStyleGrammar.qualifiedName
        override val formatLanguageIdentity: String = AglFormatGrammar.qualifiedName
        override val scopesLanguageIdentity: String = AglScopesGrammar.qualifiedName

        override val grammar: LanguageDefinition<List<Grammar>, GrammarContext> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                buildForDefaultGoal = false,
                configuration = Agl.configuration {
                    grammarResolver { ProcessResultDefault(AglGrammarGrammar, emptyList()) }
                    targetGrammarName(AglGrammarGrammar.name)
                    defaultGoalRuleName(AglGrammarGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar(AglGrammarGrammar), emptyList()) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), emptyList()) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglGrammarSyntaxAnalyser(), emptyList()) }
                    semanticAnalyserResolver { ProcessResultDefault(AglGrammarSemanticAnalyser(this@LanguageRegistryDefault), emptyList()) }
                    formatterResolver { ProcessResultDefault(null, emptyList()) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglGrammarGrammar.styleStr
                        )
                    }
                }
            )
        )

        override val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<GrammarItem>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<ScopeModelAgl, SentenceContext<GrammarItem>>(
                identity = scopesLanguageIdentity,
                buildForDefaultGoal = false,
                configuration = Agl.configuration {
                    grammarResolver { ProcessResultDefault(AglScopesGrammar, emptyList()) }
                    targetGrammarName(AglScopesGrammar.name)
                    defaultGoalRuleName(AglScopesGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar(AglScopesGrammar), emptyList()) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), emptyList()) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglScopesSyntaxAnalyser(), emptyList()) }
                    semanticAnalyserResolver { ProcessResultDefault(AglScopesSemanticAnalyser(), emptyList()) }
                    formatterResolver { ProcessResultDefault(null, emptyList()) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglScopesGrammar.styleStr
                        )
                    }
                }
            ).also {
                it.syntaxAnalyser?.configure(
                    configurationContext = ContextFromGrammar(AglScopesGrammar),
                )
            }
        )

        override val formatter = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<AglFormatterModel, SentenceContext<GrammarItem>>(
                identity = formatLanguageIdentity,
                buildForDefaultGoal = false,
                configuration = Agl.configuration {
                    grammarResolver { ProcessResultDefault(AglFormatGrammar, emptyList()) }
                    targetGrammarName(AglFormatGrammar.name)
                    defaultGoalRuleName(AglFormatGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar(AglFormatGrammar), emptyList()) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), emptyList()) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglFormatSyntaxAnalyser(), emptyList()) }
                    semanticAnalyserResolver { ProcessResultDefault(null, emptyList()) }
                    formatterResolver { ProcessResultDefault(null, emptyList()) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglFormatGrammar.styleStr
                        )
                    }
                }
            )
        )

        override val style: LanguageDefinition<AglStyleModel, SentenceContext<GrammarItem>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                buildForDefaultGoal = false,
                configuration = Agl.configuration {
                    grammarResolver { ProcessResultDefault(AglStyleGrammar, emptyList()) }
                    targetGrammarName(AglStyleGrammar.name)
                    defaultGoalRuleName(AglStyleGrammar.goalRuleName)
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar(it.grammar!!), emptyList()) }
                    scopeModelResolver { ProcessResultDefault(ScopeModelAgl(), emptyList()) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglStyleSyntaxAnalyser(), emptyList()) }
                    semanticAnalyserResolver { ProcessResultDefault(AglStyleSemanticAnalyser(), emptyList()) }
                    //formatterResolver {  }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglStyleGrammar.styleStr
                        )
                    }
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

    fun <AsmType : Any, ContextType : Any> register(
        identity: String,
        grammarStr: String?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType> = this.registerFromDefinition(
        LanguageDefinitionDefault<AsmType, ContextType>(
            identity,
            grammarStr,
            buildForDefaultGoal,
            configuration
        )
    )

    fun unregister(identity: String) {
        this._registry.remove(identity)
    }

    fun <AsmType : Any, ContextType : Any> findOrNull(identity: String): LanguageDefinition<AsmType, ContextType>? {
        return this._registry[identity] as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull("$localNamespace.$nameOrQName") ?: findOrNull(nameOrQName)
    }

    fun <AsmType : Any, ContextType : Any> findOrPlaceholder(identity: String): LanguageDefinition<AsmType, ContextType> {
        val existing = this.findOrNull<AsmType, ContextType>(identity)
        return if (null == existing) {
            val placeholder = LanguageDefinitionDefault<AsmType, ContextType>(
                identity = identity,
                grammarStrArg = null,
                buildForDefaultGoal = false,
                configuration = Agl.configurationDefault()
            )
            registerFromDefinition(placeholder)
        } else {
            existing
        }
    }

    override fun findGrammarOrNull(namespace: Namespace, name: String): Grammar? =
        this.findWithNamespaceOrNull<Any, Any>(namespace.qualifiedName, name)?.grammar

    override fun registerIfNot(grammar: Grammar) {
        val def = findOrPlaceholder<Any, Any>(grammar.qualifiedName)
        def.grammar = grammar
    }
}