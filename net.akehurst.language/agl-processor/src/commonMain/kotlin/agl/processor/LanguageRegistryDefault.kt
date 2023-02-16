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
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.style.AglStyleRule

interface AglLanguages {
    val grammarLanguageIdentity: String
    val styleLanguageIdentity: String
    val formatLanguageIdentity: String
    val scopesLanguageIdentity: String

    val grammar: LanguageDefinition<List<Grammar>, GrammarContext>
    val style: LanguageDefinition<List<AglStyleRule>, SentenceContext<GrammarItem>>
    val format: LanguageDefinition<Any, Any>
    val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<GrammarItem>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _registry = mutableMapOf<String, LanguageDefinition<*, *>>()

    val agl = object : AglLanguages {
        override val grammarLanguageIdentity: String = AglGrammarGrammar.qualifiedName
        override val styleLanguageIdentity: String = AglStyleGrammar.qualifiedName
        override val formatLanguageIdentity: String = AglFormatGrammar.qualifiedName
        override val scopesLanguageIdentity: String = AglScopesGrammar.qualifiedName

        override val grammar = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                grammarArg = AglGrammarGrammar,
                targetGrammar = null,
                defaultGoalRuleArg = AglGrammarGrammar.goalRuleName,
                buildForDefaultGoal = false,
                scopeModelArg = null,
                styleArg = """
                    'namespace' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'grammar' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'extends' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'override' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'skip' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'leaf' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    LITERAL {
                      foreground: blue;
                    }
                    PATTERN {
                      foreground: darkblue;
                    }
                    IDENTIFIER {
                      foreground: darkred;
                      font-style: italic;
                    }
                """.trimIndent(),
                formatArg = """
                """.trimIndent(),
                syntaxAnalyserResolverArg = { AglGrammarSyntaxAnalyser() },
                semanticAnalyserResolverArg = { AglGrammarSemanticAnalyser(this@LanguageRegistryDefault) },
                aglOptionsArg = Agl.options {
                    parse {
                        goalRuleName(AglScopesGrammar.goalRuleName)
                    }
                    semanticAnalysis {
                        active(false)
                    }
                }
            )
        )

        override val style = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                grammarArg = AglStyleGrammar,
                targetGrammar = null,
                defaultGoalRuleArg = AglStyleGrammar.goalRuleName,
                buildForDefaultGoal = false,
                scopeModelArg = null,
                styleArg = """
                    META_IDENTIFIER {
                      foreground: orange;
                      font-style: bold;
                    }
                    IDENTIFIER {
                      foreground: blue;
                      font-style: bold;
                    }
                    LITERAL {
                      foreground: blue;
                      font-style: bold;
                    }
                    PATTERN {
                      foreground: darkblue;
                      font-style: bold;
                    }
                    STYLE_ID {
                      foreground: darkred;
                      font-style: italic;
                    }
                """.trimIndent(),
                formatArg = """
                """.trimIndent(),
                syntaxAnalyserResolverArg = { AglStyleSyntaxAnalyser() },
                semanticAnalyserResolverArg = { AglStyleSemanticAnalyser() },
                aglOptionsArg = Agl.options {
                    parse {
                        goalRuleName(AglScopesGrammar.goalRuleName)
                    }
                    semanticAnalysis {
                        active(false)
                    }
                }
            )
        )

        override val format = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = formatLanguageIdentity,
                grammarArg = AglFormatGrammar,
                targetGrammar = null,
                defaultGoalRuleArg = AglFormatGrammar.goalRuleName,
                buildForDefaultGoal = false,
                scopeModelArg = null,
                styleArg = """
                """.trimIndent(),
                formatArg = """
                """.trimIndent(),
                syntaxAnalyserResolverArg = { AglFormatSyntaxAnalyser() },
                semanticAnalyserResolverArg = null,
                aglOptionsArg = Agl.options {
                    parse {
                        goalRuleName(AglScopesGrammar.goalRuleName)
                    }
                    semanticAnalysis {
                        active(false)
                    }
                }
            )
        )

        override val scopes: LanguageDefinition<ScopeModelAgl, SentenceContext<GrammarItem>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = scopesLanguageIdentity,
                grammarArg = AglScopesGrammar,
                targetGrammar = null,
                defaultGoalRuleArg = AglScopesGrammar.goalRuleName,
                buildForDefaultGoal = false,
                scopeModelArg = null,
                /*ScopeModelAgl.fromString(ContextFromGrammar(AglScopesGrammar),"""
                        references {
                            in scope property typeReference refers-to GrammarRule
                            in identifiable property typeReference refers-to GrammarRule
                            in referenceDefinition property typeReference refers-to GrammarRule
                            in referenceDefinition property propertyReference refers-to GrammarRule
                        } 
                    """.trimIndent()).asm!!,*/
                styleArg = """
                    'scope' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'identify' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'by' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'references' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'in' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'property' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    'refers-to' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                    '|' {
                      foreground: darkgreen;
                      font-style: bold;
                    }
                """.trimIndent(),
                formatArg = """
                """.trimIndent(),
                syntaxAnalyserResolverArg = { AglScopesSyntaxAnalyser() },
                semanticAnalyserResolverArg = { AglScopesSemanticAnalyser() },
                aglOptionsArg = Agl.options {
                    parse {
                        goalRuleName(AglScopesGrammar.goalRuleName)
                    }
                    semanticAnalysis {
                        active(false)
                    }
                }
            ).also {
                it.syntaxAnalyser?.configure(
                    configurationContext = ContextFromGrammar(this.grammar.processor!!.grammar),
                )
            }
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
        targetGrammar: String?,
        defaultGoalRule: String?,
        buildForDefaultGoal: Boolean,
        scopeModelStr:String?,
        styleStr: String?,
        formatStr: String?,
        syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?,
        semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?,
        aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?,
    ): LanguageDefinition<AsmType, ContextType> = this.registerFromDefinition(
        LanguageDefinitionDefault<AsmType, ContextType>(
            identity, grammarStr, targetGrammar, defaultGoalRule, buildForDefaultGoal,
            scopeModelStrArg = scopeModelStr,
            styleArg = styleStr,
            formatArg = formatStr,
            syntaxAnalyserResolverArg = syntaxAnalyserResolver,
            semanticAnalyserResolverArg = semanticAnalyserResolver,
            aglOptionsArg = aglOptions
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
                targetGrammarArg = null,
                defaultGoalRuleArg = null,
                buildForDefaultGoal = false,
                scopeModelStrArg = null,
                styleArg = null,
                formatArg = null,
                syntaxAnalyserResolverArg = null,
                semanticAnalyserResolverArg = null,
                aglOptionsArg = null
            )
            registerFromDefinition(placeholder)
        } else {
            existing
        }
    }

    override fun findGrammarOrNull(namespace: Namespace, name: String): Grammar? =
        this.findWithNamespaceOrNull<Any, Any>(namespace.qualifiedName, name)?.grammar

}