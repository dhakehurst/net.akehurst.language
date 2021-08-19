/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.processor

class LanguageDefinition(
    val grammar: String,
    val style: String,
    val format: String
)

object AglLanguage {
    val grammar = LanguageDefinition(
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
            
        """.trimIndent()
    )

    val style = LanguageDefinition(
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
            
        """.trimIndent()
    )

    val format = LanguageDefinition(
        grammar = """
            
        """.trimIndent(),
        style = """
            
        """.trimIndent(),
        format = """
            
        """.trimIndent()
    )
}