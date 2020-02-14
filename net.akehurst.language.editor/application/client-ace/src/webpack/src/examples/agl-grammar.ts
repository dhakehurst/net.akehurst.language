import {Examples} from "./examples";

const id = 'agl-grammar';
const label = 'Agl Grammar';

const sentence = `
namespace test

grammar Test {
    skip WS = "\s+" ;

    unit = declaration* ;
    declaration = 'class' ID '{' property* '}' ;
    property = ID ':' typeReference ;
    typeReference = ID typeArguments? ;
    typeArguments = '<' [typeReference / ',']+ '>' ;

    leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;

}
`;
const grammar = `
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
namespace net.akehurst.language.agl.grammar.grammar

grammar Agl {

  skip WHITESPACE = "\\s+" ;
  skip MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
  skip SINGLE_LINE_COMMENT = "//.*?$" ;

  grammarDefinition = namespace grammar ;
  namespace = 'namespace' qualifiedName ;
  grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
  extends = 'extends' [qualifiedName / ',']+ ;
  rules = anyRule+ ;
  anyRule = normalRule | skipRule | leafRule ;
  normalRule = IDENTIFIER '=' choice ';' ;
  skipRule = 'skip' IDENTIFIER '=' choice ';' ;
  leafRule = 'leaf' IDENTIFIER '=' choice ';' ;
  choice = simpleChoice < priorityChoice ;
  simpleChoice = [concatenation / '|']* ;
  priorityChoice = [concatenation / '<']* ;
  concatenation = concatenationItem+ ;
  concatenationItem = simpleItem | multi | separatedList ;
  simpleItem = terminal | nonTerminal | group ;
  multiplicity = '*' | '+' | '?' ;
  multi = simpleItem multiplicity ;
  group = '(' choice ')' ;
  separatedList = '[' simpleItem '/' terminal ']' multiplicity ;
  nonTerminal = IDENTIFIER ;
  terminal = LITERAL | PATTERN ;
  qualifiedName = [IDENTIFIER / '.']+ ;
  IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
  LITERAL = "'(?:\\\\?.)*?'" ;
  PATTERN = "\\"(?:\\\\?.)*?\\"" ;
}
`;
const style = `
'namespace' {
  color: darkgreen;
  font-weight: bold;
}
'grammar' {
  color: darkgreen;
  font-weight: bold;
}
'skip' {
  color: darkgreen;
  font-weight: bold;
}
'leaf' {
  color: darkgreen;
  font-weight: bold;
}
LITERAL {
  color: blue;
}
PATTERN {
  color: darkblue;
}
IDENTIFIER {
  color: darkred;
  font-style: italic;
}
`;
const format = `
`;

Examples.add(id, label, sentence, grammar, style, format);