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
package net.akehurst.language.agl.grammar.format

import net.akehurst.language.agl.grammar.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.grammar.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.grammar.grammar.asm.NamespaceDefault
import net.akehurst.language.api.grammar.GrammarRule


internal object AglFormatGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglFormat") {
    //companion object {
        const val goalRuleName = "rules"
        private fun createRules(): List<GrammarRule> {
            val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglFormat");
            b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
            b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
            b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//[^\\n\\r]*"));

            b.rule("rules").multi(0, -1, b.nonTerminal("rule"))
            b.rule("rule").concatenation(b.nonTerminal("IDENTIFIER"), b.terminalLiteral("{"), b.terminalLiteral("}"))

            b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));

            return b.grammar.rule
        }
    //}

    const val styleStr = ""

    init {
        super.rule.addAll(createRules())
    }
}