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
package net.akehurst.language.ogl.grammar;

import java.util.List;

import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

/**
 * <code>
 * namespace net::akehurst::language::ogl::grammar;
 *
 * grammar OGL {
 *
 *   grammarDefinition : namespace grammar ;
 *   namespace : 'namespace' qualifiedName ';' ;
 *   grammar : 'grammar' IDENTIFIER extends? '{' rules '}' ;
 *   extends : 'extends' [qualifiedName / ',']+ ;
 *   rules : anyRule+ ;
 *   anyRule : normalRule | skipRule ;
 *   normalRule : IDENTIFIER ':' choice ';' ;
 *   skipRule : 'skip' IDENTIFIER ':' choice ';' ;
 *   choice : simpleChoice < priorityChoice ;
 *   simpleChoice : [concatenation / '|']* ;
 *   priorityChoice : [concatenation / '<']* ;
 *   concatenation : concatenationItem+ ;
 *   concatenationItem : simpleItem | multi | separatedList ;
 *   simpleItem : terminal | nonTerminal | group ;
 *   multiplicity : '*' | '+' | '?'
 *   multi : simpleItem multiplicity ;
 *   group : '(' choice ')' ;
 *   separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
 *   nonTerminal : IDENTIFIER ;
 *   terminal : LITERAL | PATTERN ;
 *   LITERAL : "'(?:\\?.)*?'" ;
 *   PATTERN : "\"(?:\\?.)*?\"" ;
 *   qualifiedName : (IDENTIFIER / '::')+ ;
 *   IDENTIFIER : "[a-zA-Z_][a-zA-Z_0-9]*";
 *
 * }
 * </code>
 *
 * @author akehurst
 *
 */
public class OGLGrammar extends GrammarDefault {

    static List<Rule> createRules() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
        b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));
        b.skip("MULTI_LINE_COMMENT").concatenation(new TerminalPatternDefault("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
        b.skip("SINGLE_LINE_COMMENT").concatenation(new TerminalPatternDefault("//.*?$"));

        b.rule("grammarDefinition").concatenation(new NonTerminalDefault("namespace"), new NonTerminalDefault("grammar"));
        b.rule("namespace").concatenation(new TerminalLiteralDefault("namespace"), new NonTerminalDefault("qualifiedName"), new TerminalLiteralDefault(";"));
        b.rule("grammar").concatenation(new TerminalLiteralDefault("grammar"), new NonTerminalDefault("IDENTIFIER"), new NonTerminalDefault("extends"),
                new TerminalLiteralDefault("{"), new NonTerminalDefault("rules"), new TerminalLiteralDefault("}"));
        b.rule("extends").multi(0, 1, new NonTerminalDefault("extends1"));
        b.rule("extends1").concatenation(new TerminalLiteralDefault("extends"), new NonTerminalDefault("extends2"));
        b.rule("extends2").separatedList(1, -1, new TerminalLiteralDefault(","), new NonTerminalDefault("qualifiedName"));
        b.rule("rules").multi(1, -1, new NonTerminalDefault("anyRule"));
        b.rule("anyRule").choice(new NonTerminalDefault("normalRule"), new NonTerminalDefault("skipRule"));
        b.rule("skipRule").concatenation(new TerminalLiteralDefault("skip"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault(":"),
                new NonTerminalDefault("choice"), new TerminalLiteralDefault(";"));
        b.rule("normalRule").concatenation(new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault(":"), new NonTerminalDefault("choice"),
                new TerminalLiteralDefault(";"));
        b.rule("choice").priorityChoice(new NonTerminalDefault("simpleChoice"), new NonTerminalDefault("priorityChoice"));
        b.rule("simpleChoice").separatedList(0, -1, new TerminalLiteralDefault("|"), new NonTerminalDefault("concatenation"));
        b.rule("priorityChoice").separatedList(0, -1, new TerminalLiteralDefault("<"), new NonTerminalDefault("concatenation"));
        b.rule("concatenation").multi(1, -1, new NonTerminalDefault("concatenationItem"));
        b.rule("concatenationItem").choice(new NonTerminalDefault("simpleItem"), new NonTerminalDefault("multi"), new NonTerminalDefault("separatedList"));
        b.rule("simpleItem").choice(new NonTerminalDefault("terminal"), new NonTerminalDefault("nonTerminal"), new NonTerminalDefault("group"));
        b.rule("multi").concatenation(new NonTerminalDefault("simpleItem"), new NonTerminalDefault("multiplicity"));
        b.rule("multiplicity").choice(new TerminalLiteralDefault("*"), new TerminalLiteralDefault("+"), new TerminalLiteralDefault("?"));
        b.rule("group").concatenation(new TerminalLiteralDefault("("), new NonTerminalDefault("choice"), new TerminalLiteralDefault(")"));
        b.rule("separatedList").concatenation(new TerminalLiteralDefault("["), new NonTerminalDefault("simpleItem"), new TerminalLiteralDefault("/"),
                new NonTerminalDefault("LITERAL"), new TerminalLiteralDefault("]"), new NonTerminalDefault("multiplicity"));
        b.rule("nonTerminal").choice(new NonTerminalDefault("IDENTIFIER"));
        b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteralDefault("::"), new NonTerminalDefault("IDENTIFIER"));
        b.rule("terminal").choice(new NonTerminalDefault("LITERAL"), new NonTerminalDefault("PATTERN"));
        b.rule("LITERAL").concatenation(new TerminalPatternDefault("'(?:\\\\?.)*?'"));
        b.rule("PATTERN").concatenation(new TerminalPatternDefault("\"(?:\\\\?.)*?\""));
        b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

        return b.get().getRule();
    }

    public OGLGrammar() {
        super(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
        this.setRule(OGLGrammar.createRules());
    }

}
