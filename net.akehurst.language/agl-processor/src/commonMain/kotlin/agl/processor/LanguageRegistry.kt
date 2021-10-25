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

import net.akehurst.language.agl.grammar.GrammarRegistryDefault
import net.akehurst.language.agl.grammar.format.AglFormatGrammar
import net.akehurst.language.agl.grammar.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.grammar.scopes.AglScopesGrammar
import net.akehurst.language.agl.grammar.scopes.AglScopesSyntaxAnalyser
import net.akehurst.language.agl.grammar.style.AglStyleGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser

interface AglLanguages {
    val grammarLanguageIdentity: String
    val styleLanguageIdentity: String
    val formatLanguageIdentity: String
    val scopesLanguageIdentity:String

    val grammar: LanguageDefinition
    val style: LanguageDefinition
    val format: LanguageDefinition
    val scopes:LanguageDefinition
}

class LanguageRegistry {

    private val _registry = mutableMapOf<String, LanguageDefinition>()

    val agl = object : AglLanguages {
        override val grammarLanguageIdentity: String = "net.akehurst.language.agl.AglGrammar"
        override val styleLanguageIdentity: String = "net.akehurst.language.agl.AglStyle"
        override val formatLanguageIdentity: String = "net.akehurst.language.agl.AglFormat"
        override val scopesLanguageIdentity: String = "net.akehurst.language.agl.AglScopes"

        override val grammar = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                grammar = AglGrammarGrammar(),
                defaultGoalRule = AglGrammarGrammar.goalRuleName,
                style = """
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
                format = """
                """.trimIndent(),
                syntaxAnalyser = AglGrammarSyntaxAnalyser(GrammarRegistryDefault), //TODO: enable the registry to be changed,
                semanticAnalyser = AglGrammarSemanticAnalyser()
            )
        )

        override val style = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                grammar = AglStyleGrammar(),
                defaultGoalRule = AglStyleGrammar.goalRuleName,
                style = """
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
                format = """
                """.trimIndent(),
                syntaxAnalyser = AglStyleSyntaxAnalyser(),
                semanticAnalyser = null
            )
        )

        override val format = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = formatLanguageIdentity,
                grammar = AglFormatGrammar(),
                defaultGoalRule = AglFormatGrammar.goalRuleName,
                style = """
                """.trimIndent(),
                format = """
                """.trimIndent(),
                syntaxAnalyser = AglFormatSyntaxAnalyser(),
                semanticAnalyser = null
            )
        )

        override val scopes: LanguageDefinition= this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = scopesLanguageIdentity,
                grammar = AglScopesGrammar(),
                defaultGoalRule = AglScopesGrammar.goalRuleName,
                style = """
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
                format = """
                """.trimIndent(),
                syntaxAnalyser = AglScopesSyntaxAnalyser(),
                semanticAnalyser = null
            ).also {
                it.syntaxAnalyser?.configure(
                    configurationContext = ContextFromGrammar(this.grammar.processor!!.grammar),
                    configuration = """
                        references {
                            in scope property typeReference refers-to Rule
                            in identifiable property typeReference refers-to Rule
                            in referenceDefinition property typeReference refers-to Rule
                            in referenceDefinition property propertyReference refers-to Rule
                        } 
                    """.trimIndent()
                )
            }
        )
    }

    fun registerFromDefinition(definition: LanguageDefinition): LanguageDefinition {
        return if (this._registry.containsKey(definition.identity)) {
            error("LanguageDefinition '${definition.identity}' is already registered, please unregister the old one first")
        } else {
            this._registry[definition.identity] = definition
            definition
        }
    }

    fun register(
        identity: String, grammar: String, defaultGoalRule: String?,
        style: String?, format: String?, syntaxAnalyser: SyntaxAnalyser<*,*>?, semanticAnalyser: SemanticAnalyser<*,*>?
    ): LanguageDefinition = this.registerFromDefinition(
        LanguageDefinitionDefault(
            identity, grammar, defaultGoalRule,
            style, format, syntaxAnalyser, semanticAnalyser
        )
    )

    fun unregister(identity: String) {
        this._registry.remove(identity)
    }

    fun findOrNull(identity: String): LanguageDefinition? {
        return this._registry[identity]
    }

    fun findOrPlaceholder(identity: String): LanguageDefinition {
        val existing = this.findOrNull(identity)
        return if (null == existing) {
            val placeholder = LanguageDefinitionDefault(identity, null)
            registerFromDefinition(placeholder)
        } else {
            existing
        }
    }
}