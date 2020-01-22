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
    rule = SELECTOR '{' styleList '}' ;
    styleList = style* ;
    style = STYLE_ID ':' STYLE_VALUE ';' ;
 */
private fun createRules(): List<Rule> {
    val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
    b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
    b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
    b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*?$"));

    b.rule("rules").multi(0,-1, b.nonTerminal("rule"))
    b.rule("rule").concatenation(b.nonTerminal("SELECTOR"), b.terminalLiteral("{"), b.nonTerminal("styleList"), b.terminalLiteral("}"))
    b.rule("SELECTOR").choiceEqual(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"), b.nonTerminal("IDENTIFIER"))
    b.rule("styleList").multi(0,-1,b.nonTerminal("style"))
    b.rule("style").concatenation(b.nonTerminal("STYLE_ID"), b.terminalLiteral(":"), b.nonTerminal("STYLE_VALUE"), b.terminalLiteral(";"))
    b.rule("STYLE_ID").concatenation(b.terminalPattern("[-a-zA-Z_][-a-zA-Z_0-9-]*"));
    b.rule("STYLE_VALUE").concatenation(b.terminalPattern("[^;]*"))
    // these must match what is in the AglGrammarGrammar
    b.rule("LITERAL").concatenation(b.terminalPattern("'(?:\\\\?.)*?'"));
    b.rule("PATTERN").concatenation(b.terminalPattern("\"(?:\\\\?.)*?\""));
    b.rule("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));
    return b.grammar.rule
}