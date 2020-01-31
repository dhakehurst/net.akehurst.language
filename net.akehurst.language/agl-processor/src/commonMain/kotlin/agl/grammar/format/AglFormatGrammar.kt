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

import net.akehurst.language.agl.ast.GrammarAbstract
import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.api.grammar.Rule


class AglFormatGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglFormat", createRules()) {
    companion object {
        const val goalRuleName = "rules"
    }
}

private fun createRules(): List<Rule> {
    val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglFormat");
    b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
    b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
    b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*?$"));
    return b.grammar.rule
}