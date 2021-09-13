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
import net.akehurst.language.agl.grammar.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarSyntaxAnalyser
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

class LanguageRegistry {

    private val _registry = mutableMapOf<String, LanguageDefinition>()

    val agl = object {
        val grammar = this@LanguageRegistry.registerFromDefinition(
            LanguageDefinitionDefault(
                identity = "net.akehurst.language.agl.AglGrammar",
                grammar = """
            /**
             * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
            namespace net.akehurst.language.agl
            grammar AglGrammar {
                skip WHITESPACE = "\s+" ;
                skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
                skip SINGLE_LINE_COMMENT = "//.*$" ;
                
                grammarDefinition = namespace definitions ;
                definitions = grammar+ ;
                namespace = 'namespace' qualifiedName ;
                grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
                extends = 'extends' [qualifiedName / ',']+ ;
                rules = anyRule+ ;
                rule = ruleTypeLabels IDENTIFIER '=' choice ';' ;
                ruleTypeLabels = 'override'? 'skip'? 'leaf'? ;
                skipRule = 'skip' IDENTIFIER '=' choice ';' ;
                choice = ambiguousChoice | priorityChoice | simpleChoice ;
                simpleChoice = [concatenation / '|']* ;
                priorityChoice = [concatenation / '<']* ;
                ambiguousChoice = [concatenation / '||']* ;
                concatenation = concatenationItem+ ;
                concatenationItem = simpleItem | multi | separatedList ;
                simpleItem = terminal | nonTerminal | group ;
                multi = simpleItem multiplicity ;
                multiplicity = '*' | '+' | '?' | POSITIVE_INTEGER '+' | POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
                group = '(' choice ')' ;
                separatedList = '[' simpleItem '/' simpleItem ']' multiplicity ;
                nonTerminal = qualifiedName ;
                qualifiedName = [IDENTIFIER / '.']+ ;
                terminal = LITERAL | PATTERN ;
                
                leaf LITERAL = "'(\\\'|\\\\|[[^\']])*'" ;
                leaf PATTERN = "\"(\\\"|[^\"]*)\"" ;
                leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
                leaf POSITIVE_INTEGER = "[0-9]+";
            }
        """.trimIndent(),
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
            LanguageDefinitionDefault(
                identity = "net.akehurst.language.agl.AglStyle",
                grammar = """
            /**
             * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
             namespace net.akehurst.language.agl
             grammar AglStyle {
                skip WHITESPACE = "\s+" ;
                skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
                skip SINGLE_LINE_COMMENT = "//.*$" ;
             
                rules = rule* ;
                rule = selectorExpression '{' styleList '}' ;
                selectorExpression = selectorSingle ; //TODO
                selectorSingle = LITERAL | PATTERN | IDENTIFIER | META_IDENTIFIER ;
                styleList = style* ;
                style = STYLE_ID ':' STYLE_VALUE ';' ;
                
                leaf LITERAL = "'(\\\'|\\\\|[[^\']])*'" ;
                leaf PATTERN = "\"(\\\"|[^\"]*)\"" ;
                leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
                leaf META_IDENTIFIER = "[\\$][a-zA-Z_][a-zA-Z_0-9-]*" ;
             }
        """.trimIndent(),
                defaultGoalRule = "",
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
            LanguageDefinitionDefault(
                identity = "net.akehurst.language.agl.AglFormat",
                grammar = """
            
            """.trimIndent(),
                defaultGoalRule = "",
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
        this._registry[definition.identity] = definition
        return definition
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
}