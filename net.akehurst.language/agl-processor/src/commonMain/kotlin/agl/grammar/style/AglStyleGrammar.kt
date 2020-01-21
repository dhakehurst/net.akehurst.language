package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.ast.GrammarAbstract
import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.api.grammar.Rule


class AglStyleGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglStyle", createRules()) {

}

private fun createRules(): List<Rule> {
    val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
    b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
    b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
    b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*?$"));
    return b.grammar.rule
}