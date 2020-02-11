export class Example {

    constructor(
        public id: string,
        public label: string,
        public sentence: string,
        public grammar: string,
        public style: string,
        public format: string
    ) {
    }
}

export class Examples {

    static initialise(selectEl) {
        Examples.list.forEach(eg => {
            Examples.map.set(eg.id, eg);
            let option = document.createElement('option');
            selectEl.appendChild(option);
            option.setAttribute('value', eg.id);
            option.textContent = eg.label;
        });
    }

    static map = new Map<string, Example>();
    static list = [
        new Example(
            'classes',
            'Classes',
            `
class Person {
    name: String
    dob: Date
    friends: List<Person>
}
class class {
    prop: String
}
`,
            `
namespace test

grammar Test {
    skip WS = "\\s+" ;

    unit = declaration* ;
    declaration = 'class' ID '{' property* '}' ;
    property = ID ':' typeReference ;
    typeReference = ID typeArguments? ;
    typeArguments = '<' [typeReference / ',']+ '>' ;

    leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;

}
`,
            `
'class' {
  color: purple;
  font-weight: bold;
}
ID {
  color: red;
  font-style: italic;
}
'{' {
  color: darkgreen;
  font-weight: bold;
}
'}' {
  color: darkgreen;
  font-weight: bold;
}
property {
  background-color: lightgray;
}
typeReference {
  background-color: lightblue;
}
`,
            `
`
        ),
        new Example(
            'agl-grammar',
            'AGL Grammar',
            `
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
            `,
            `
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
            `,
            `
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
`,
            ``
        )
    ];

}