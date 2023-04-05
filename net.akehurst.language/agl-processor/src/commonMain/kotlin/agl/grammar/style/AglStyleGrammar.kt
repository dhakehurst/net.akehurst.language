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

import net.akehurst.language.agl.grammar.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.grammar.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.grammar.grammar.asm.NamespaceDefault
import net.akehurst.language.api.grammar.GrammarRule

internal object AglStyleGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglStyle") {
    //companion object {
    const val goalRuleName = "rules"
    private fun createRules(): List<GrammarRule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
        b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
        b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"));
        b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//[^\\n\\r]*"));

        b.rule("rules").multi(0, -1, b.nonTerminal("rule"))
        b.rule("rule").concatenation(b.nonTerminal("selectorExpression"), b.terminalLiteral("{"), b.nonTerminal("styleList"), b.terminalLiteral("}"))
        b.rule("selectorExpression").choiceLongestFromConcatenationItem(b.nonTerminal("selectorAndComposition"), b.nonTerminal("selectorSingle"))
        b.rule("selectorAndComposition").separatedList(2, -1, b.terminalLiteral(","), b.nonTerminal("selectorSingle"))
        b.rule("selectorSingle")
            .choiceLongestFromConcatenationItem(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("META_IDENTIFIER"))
        // these must match what is in the AglGrammarGrammar
        b.leaf("LITERAL").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)*'"))
        b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"\\\\]|\\\\.)*\""))

        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));
        b.leaf("META_IDENTIFIER").concatenation(b.terminalPattern("[\\$][a-zA-Z_][a-zA-Z_0-9-]*"));

        b.rule("styleList").multi(0, -1, b.nonTerminal("style"))
        b.rule("style").concatenation(b.nonTerminal("STYLE_ID"), b.terminalLiteral(":"), b.nonTerminal("STYLE_VALUE"), b.terminalLiteral(";"))
        b.leaf("STYLE_ID").concatenation(b.terminalPattern("[-a-zA-Z_][-a-zA-Z_0-9]*"));
        b.leaf("STYLE_VALUE").concatenation(b.terminalPattern("([^;:]*)"))

        return b.grammar.grammarRule
    }
    //}

    const val styleStr = """META_IDENTIFIER {
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
}"""

    const val scopeModelStr = """
references {
    in scope property typeReference refers-to GrammarRule
    in identifiable property typeReference refers-to GrammarRule
    in referenceDefinition property typeReference refers-to GrammarRule
    in referenceDefinition property propertyReference refers-to GrammarRule
}
    """

    init {
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = """
namespace net.akehurst.language.agl
grammar AglStyle {
    rules = rule* ;
    rule = selectorExpression '{' styleList '}' ;
    selectorExpression
     = selectorSingle
     | selectorAndComposition
     ; //TODO
    selectorAndComposition = [selectorSingle /',']2+ ;
    selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    styleList = style* ;
    style = STYLE_ID ':' STYLE_VALUE ';' ;
}
    """.trimIndent()
}


