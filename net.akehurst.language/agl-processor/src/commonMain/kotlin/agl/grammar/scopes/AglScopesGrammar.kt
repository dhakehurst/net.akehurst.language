/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.grammar.scopes

import net.akehurst.language.agl.grammar.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.grammar.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.grammar.grammar.asm.NamespaceDefault
import net.akehurst.language.api.grammar.Rule

/**
    declarations = scopes references
    scopes = scope+
    scope = 'scope' typeReference '{' identifiables '}
    identifiables = identifiable*
    identifiable = 'identify' typeReference 'by' propertyName

    references = 'references' '{' referenceDefinitions '}'
    referenceDefinitions = referenceDefinition*
    referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
    typeReferences = [typeReferences / '|']+

    typeReference = IDENTIFIER     // same as grammar rule name
    propertyReference = IDENTIFIER // same as grammar rule name
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*"
 */
internal class AglScopesGrammar: GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglScopes") {
    companion object {
        const val goalRuleName = "declarations"
        private fun createRules(): List<Rule> {
            val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
            b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
            b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"));
            b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//[^\\n\\r]*"));

            b.rule("declarations").concatenation(b.nonTerminal("scopes"),b.nonTerminal("references"))
            b.rule("scopes").multi(0,-1,b.nonTerminal("scope"))
            b.rule("scope").concatenation(b.terminalLiteral("scope"), b.nonTerminal("typeReference"), b.terminalLiteral("{"), b.nonTerminal("identifiables"), b.terminalLiteral("}"))
            b.rule("identifiables").multi(0, -1, b.nonTerminal("identifiable"))
            b.rule("identifiable").concatenation(b.terminalLiteral("identify"),b.nonTerminal("typeReference"),b.terminalLiteral("by"),b.nonTerminal("propertyReference"))

            b.rule("references").concatenation(b.terminalLiteral("references"), b.terminalLiteral("{"), b.nonTerminal("referenceDefinitions"),b.terminalLiteral("}"))
            b.rule("referenceDefinitions").multi(0,-1,b.nonTerminal("referenceDefinition"))
            b.rule("referenceDefinition").concatenation(b.terminalLiteral("in"),b.nonTerminal("typeReference"),b.terminalLiteral("property"),b.nonTerminal("propertyReference"),b.terminalLiteral("refers-to"),b.nonTerminal("typeReferences"))
            b.rule("typeReferences").separatedList(1,-1,b.terminalLiteral("|"),b.nonTerminal("typeReference"))
            b.rule("typeReference").concatenation(b.nonTerminal("IDENTIFIER"))
            b.rule("propertyReference").concatenation(b.nonTerminal("IDENTIFIER"))
            b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));

            return b.grammar.rule
        }
    }
    init {
        super.rule.addAll(createRules())
    }
}