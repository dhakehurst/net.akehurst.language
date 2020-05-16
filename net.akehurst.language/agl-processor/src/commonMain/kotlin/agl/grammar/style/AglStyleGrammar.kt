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
package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.ast.GrammarAbstract
import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.api.grammar.Rule


class AglStyleGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglStyle", createRules()) {
    companion object {
        const val goalRuleName = "rules"
    }
}

/*
    rules = rule* ;
    rule = selectorExpression '{' styleList '}' ;
    selectorExpression = selectorSingle ; //TODO
    selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    styleList = style* ;
    style = STYLE_ID ':' STYLE_VALUE ';' ;
 */
private fun createRules(): List<Rule> {
    val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
    b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
    b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"));
    b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*$"));

    b.rule("rules").multi(0,-1, b.nonTerminal("rule"))
    b.rule("rule").concatenation(b.nonTerminal("selectorExpression"), b.terminalLiteral("{"), b.nonTerminal("styleList"), b.terminalLiteral("}"))
    b.rule("selectorExpression").choiceEqual(b.nonTerminal("selectorSingle"))
    b.rule("selectorSingle").choiceEqual(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("META_IDENTIFIER"))
    // these must match what is in the AglGrammarGrammar
    b.leaf("LITERAL").concatenation(b.terminalPattern("'([^']|\\\\.)*'"));
    b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"]|\\\\.)*?\""));
    b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));
    b.leaf("META_IDENTIFIER").concatenation(b.terminalPattern("[\\$][a-zA-Z_][a-zA-Z_0-9-]*"));

    b.rule("styleList").multi(0,-1,b.nonTerminal("style"))
    b.rule("style").concatenation(b.nonTerminal("STYLE_ID"), b.terminalLiteral(":"), b.nonTerminal("STYLE_VALUE"), b.terminalLiteral(";"))
    b.leaf("STYLE_ID").concatenation(b.terminalPattern("[-a-zA-Z@_$][-a-zA-Z_0-9@]*"));
    b.leaf("STYLE_VALUE").concatenation(b.terminalPattern("([^;:]*)"))

    return b.grammar.rule
}