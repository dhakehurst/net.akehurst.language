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
import net.akehurst.language.agl.grammar.style.AglStyleGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

class LanguageRegistry {

    companion object {
        const val aglGrammarLanguageIdentity:String = "net.akehurst.language.agl.AglGrammar"
        const val aglStyleLanguageIdentity:String = "net.akehurst.language.agl.AglStyle"
        const val aglFormatLanguageIdentity:String = "net.akehurst.language.agl.AglFormat"
    }

    private val _registry = mutableMapOf<String, LanguageDefinition>()

    val agl = object {
        val grammar = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromCode(
                identity = aglGrammarLanguageIdentity,
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

        val style = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromCode(
                identity = aglStyleLanguageIdentity,
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

        val format = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionFromCode(
                identity = aglFormatLanguageIdentity,
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
    }

    fun registerFromDefinition(definition: LanguageDefinition): LanguageDefinition {
        val existing = this._registry[definition.identity]
        return if (null==existing) {
            this._registry[definition.identity] = definition
             definition
        } else {
            definition.grammarObservers.addAll(existing.grammarObservers)
            definition.styleObservers.addAll(existing.styleObservers)
            definition.formatObservers.addAll(existing.formatObservers)
            this._registry[definition.identity] = definition
            definition
        }
    }

    fun register(identity: String,
                 grammar: String,
                 defaultGoalRule: String?,
                 style: String?,
                 format: String?,
                 syntaxAnalyser: SyntaxAnalyser?,
                 semanticAnalyser: SemanticAnalyser?): LanguageDefinition {
        return this.registerFromDefinition(LanguageDefinitionDefault(
            identity,
            grammar,
            defaultGoalRule,
            style,
            format,
            syntaxAnalyser,
            semanticAnalyser
        ))
    }

    fun findOrNull(identity: String): LanguageDefinition? {
        return this._registry[identity]
    }

    fun findOrPlaceholder(identity: String) : LanguageDefinition {
        val existing = this.findOrNull(identity)
        return if (null==existing) {
            val placeholder = LanguageDefinitionDefault(identity, "-placeholder-")
            this._registry[identity] = placeholder
            placeholder
        } else {
            existing
        }
    }
}