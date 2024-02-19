/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.language.base

import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.language.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.language.grammar.asm.GrammarOptionDefault
import net.akehurst.language.agl.language.grammar.asm.NamespaceDefault
import net.akehurst.language.api.language.grammar.GrammarRule

internal object BaseGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "Base") {
    const val goalRuleName = "qualifiedName"
    private fun createRules(): List<GrammarRule> {
        val b = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl.language"), "Base")
        b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"))
        b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"))
        b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"))

        b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"))
        b.rule("import").concatenation(b.terminalLiteral("import"), b.nonTerminal("qualifiedName"))

        b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));

        return b.grammar.grammarRule
    }

    override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    const val grammarStr = """
namespace net.akehurst.language.agl

grammar Base {

    skip WHITESPACE = "\s+" ;
    skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    namespace = 'namespace' qualifiedName ;
    import = 'import' qualifiedName all? ;
    all = '.*' ;

    qualifiedName = [IDENTIFIER / '.']+ ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;

}
"""

    const val styleStr = """${"$"}keyword {
  foreground: darkgreen;
  font-style: bold;
}"""

    const val formatterStr = """
    """

    init {
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}