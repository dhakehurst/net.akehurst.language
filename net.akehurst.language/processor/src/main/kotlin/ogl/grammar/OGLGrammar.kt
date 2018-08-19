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

package net.akehurst.language.ogl.grammar

import net.akehurst.language.ogl.semanticStructure.NamespaceDefault
import net.akehurst.language.ogl.semanticStructure.GrammarDefault
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault
import net.akehurst.language.api.grammar.Rule

class OGLGrammar : GrammarDefault(NamespaceDefault("net.akehurst.language.ogl.grammar"), "OGL") {

    init {
        this.rule = createRules();
    }

}

private fun createRules() : List<Rule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.ogl.grammar"), "OGL");
        b.skip("WHITESPACE").concatenation(TerminalPatternDefault("\\s+"));
        b.skip("MULTI_LINE_COMMENT").concatenation(TerminalPatternDefault("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
        b.skip("SINGLE_LINE_COMMENT").concatenation(TerminalPatternDefault("//.*?$"));

        b.rule("grammarDefinition").concatenation(NonTerminalDefault("namespace"), NonTerminalDefault("grammar"));
        b.rule("namespace").concatenation(TerminalLiteralDefault("namespace"), NonTerminalDefault("qualifiedName"),  TerminalLiteralDefault(";"));
        b.rule("grammar").concatenation(TerminalLiteralDefault("grammar"), NonTerminalDefault("IDENTIFIER"),  NonTerminalDefault("extends"),
                 TerminalLiteralDefault("{"),  NonTerminalDefault("rules"),  TerminalLiteralDefault("}"));
        b.rule("extends").multi(0, 1,  NonTerminalDefault("extends1"));
        b.rule("extends1").concatenation( TerminalLiteralDefault("extends"),  NonTerminalDefault("extends2"));
        b.rule("extends2").separatedList(1, -1,  TerminalLiteralDefault(","),  NonTerminalDefault("qualifiedName"));
        b.rule("rules").multi(1, -1,  NonTerminalDefault("anyRule"));
        b.rule("anyRule").choice( NonTerminalDefault("normalRule"),  NonTerminalDefault("skipRule"));
        b.rule("skipRule").concatenation( TerminalLiteralDefault("skip"),  NonTerminalDefault("IDENTIFIER"),  TerminalLiteralDefault(":"),
                 NonTerminalDefault("choice"),  TerminalLiteralDefault(";"));
        b.rule("normalRule").concatenation( NonTerminalDefault("IDENTIFIER"),  TerminalLiteralDefault(":"),  NonTerminalDefault("choice"),
                 TerminalLiteralDefault(";"));
        b.rule("choice").priorityChoice( NonTerminalDefault("simpleChoice"),  NonTerminalDefault("priorityChoice"));
        b.rule("simpleChoice").separatedList(0, -1,  TerminalLiteralDefault("|"),  NonTerminalDefault("concatenation"));
        b.rule("priorityChoice").separatedList(0, -1,  TerminalLiteralDefault("<"),  NonTerminalDefault("concatenation"));
        b.rule("concatenation").multi(1, -1,  NonTerminalDefault("concatenationItem"));
        b.rule("concatenationItem").choice( NonTerminalDefault("simpleItem"),  NonTerminalDefault("multi"),  NonTerminalDefault("separatedList"));
        b.rule("simpleItem").choice( NonTerminalDefault("terminal"),  NonTerminalDefault("nonTerminal"),  NonTerminalDefault("group"));
        b.rule("multi").concatenation( NonTerminalDefault("simpleItem"),  NonTerminalDefault("multiplicity"));
        b.rule("multiplicity").choice( TerminalLiteralDefault("*"),  TerminalLiteralDefault("+"),  TerminalLiteralDefault("?"));
        b.rule("group").concatenation( TerminalLiteralDefault("("),  NonTerminalDefault("choice"),  TerminalLiteralDefault(")"));
        b.rule("separatedList").concatenation( TerminalLiteralDefault("["),  NonTerminalDefault("simpleItem"),  TerminalLiteralDefault("/"),
                 NonTerminalDefault("LITERAL"),  TerminalLiteralDefault("]"),  NonTerminalDefault("multiplicity"));
        b.rule("nonTerminal").choice( NonTerminalDefault("IDENTIFIER"));
        b.rule("qualifiedName").separatedList(1, -1,  TerminalLiteralDefault("::"),  NonTerminalDefault("IDENTIFIER"));
        b.rule("terminal").choice( NonTerminalDefault("LITERAL"),  NonTerminalDefault("PATTERN"));
        b.rule("LITERAL").concatenation( TerminalPatternDefault("'(?:\\\\?.)*?'"));
        b.rule("PATTERN").concatenation( TerminalPatternDefault("\"(?:\\\\?.)*?\""));
        b.rule("IDENTIFIER").concatenation( TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

        return b.grammar.rule
    }