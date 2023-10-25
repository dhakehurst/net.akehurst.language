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

package net.akehurst.language.agl.language.scopes

import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.language.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.language.grammar.asm.GrammarOptionDefault
import net.akehurst.language.agl.language.grammar.asm.NamespaceDefault
import net.akehurst.language.api.language.grammar.GrammarRule

/**

 */
internal object AglScopesGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglScopes") {
    //companion object {
    const val goalRuleName = "unit"
    private fun createRules(): List<GrammarRule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglStyle");
        b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"));
        b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"));
        b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"));

        b.rule("unit").multi(0, -1, b.nonTerminal("namespace"))
        b.rule("namespace")
            .concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"), b.terminalLiteral("{"), b.nonTerminal("declarations"), b.terminalLiteral("}"))
        b.rule("declarations").concatenation(b.nonTerminal("rootIdentifiables"), b.nonTerminal("scopes"), b.nonTerminal("referencesOpt"))
        b.rule("rootIdentifiables").multi(0, -1, b.nonTerminal("identifiable"))
        b.rule("scopes").multi(0, -1, b.nonTerminal("scope"))
        b.rule("scope").concatenation(b.terminalLiteral("scope"), b.nonTerminal("typeReference"), b.terminalLiteral("{"), b.nonTerminal("identifiables"), b.terminalLiteral("}"))
        b.rule("identifiables").multi(0, -1, b.nonTerminal("identifiable"))
        b.rule("identifiable").concatenation(b.terminalLiteral("identify"), b.nonTerminal("typeReference"), b.terminalLiteral("by"), b.nonTerminal("navigationOrNothing"))
        b.rule("referencesOpt").optional(b.nonTerminal("references"))
        b.rule("references").concatenation(b.terminalLiteral("references"), b.terminalLiteral("{"), b.nonTerminal("referenceDefinitions"), b.terminalLiteral("}"))
        b.rule("referenceDefinitions").multi(0, -1, b.nonTerminal("referenceDefinition"))
        b.rule("referenceDefinition").concatenation(
            b.terminalLiteral("in"),
            b.nonTerminal("typeReference"),
            b.terminalLiteral("{"),
            b.nonTerminal("referenceExpressionList"),
            b.terminalLiteral("}"),
        )
        b.rule("referenceExpressionList").multi(0, -1, b.nonTerminal("referenceExpression"))
        b.rule("referenceExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("propertyReferenceExpression"),
            b.nonTerminal("collectionReferenceExpression"),
        )
        b.rule("propertyReferenceExpression").concatenation(
            b.terminalLiteral("property"),
            b.nonTerminal("navigation"),
            b.terminalLiteral("refers-to"),
            b.nonTerminal("typeReferences"),
            b.nonTerminal("fromOpt")
        )
        b.rule("fromOpt").optional(b.nonTerminal("from"))
        b.rule("from").concatenation(
            b.terminalLiteral("from"),
            b.nonTerminal("navigation")
        )
        b.rule("collectionReferenceExpression").concatenation(
            b.terminalLiteral("forall"),
            b.nonTerminal("navigation"),
            b.nonTerminal("ofTypeOpt"),
            b.terminalLiteral("{"),
            b.nonTerminal("referenceExpressionList"),
            b.terminalLiteral("}"),
        )
        b.rule("ofTypeOpt").optional(b.nonTerminal("ofType"))
        b.rule("ofType").concatenation(b.terminalLiteral("of-type"), b.nonTerminal("typeReference"))
        b.rule("navigation").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("propertyReference"))
        b.rule("typeReferences").separatedList(1, -1, b.terminalLiteral("|"), b.nonTerminal("typeReference"))
        b.rule("typeReference").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("navigationOrNothing").choiceLongestFromConcatenationItem(b.terminalLiteral("§nothing"), b.nonTerminal("navigation"))
        b.rule("propertyReference").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));

        return b.grammar.grammarRule
    }

    override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    const val grammarStr = """
namespace net.akehurst.language.agl
grammar AglScopes {

    skip WHITESPACE = "\s+" ;
    skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    unit = namespace* ;
    namespace = 'namespace' qualifiedName '{' declarations '}' ;
    declarations = rootIdentifiables scopes references? ;
    rootIdentifiables = identifiable* ;
    scopes = scope* ;
    scope = 'scope' typeReference '{' identifiables '}' ;
    identifiables = identifiable* ;
    identifiable = 'identify' typeReference 'by' navigationOrNothing ;

    references = 'references' '{' referenceDefinitions '}' ;
    referenceDefinitions = referenceDefinition* ;
    referenceDefinition = 'in' typeReference '{' referenceExpression* '}' ;
    referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
    propertyReferenceExpression = 'property' navigation 'refers-to' typeReferences from? ;
    from = 'from' navigation ;
    collectionReferenceExpression = 'forall' navigation ofType? '{' referenceExpressionList '}' ;
    ofType = 'of-type' typeReference ;
    navigationOrNothing = '§nothing' | navigation ;
    navigation = [propertyReference / '.']+ ;
    typeReferences = [typeReference / '|']+ ;

    typeReference = IDENTIFIER ;
    propertyReference = IDENTIFIER ;
    qualifiedName = [IDENTIFIER / '.']+ ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
}
"""

    const val styleStr = """${"$"}keyword {
  foreground: darkgreen;
  font-style: bold;
}"""

    const val formatterStr = """
@TODO
References -> when {
    referenceDefinitions.isEmpty -> "references { }"
    else -> "
      references {
        referenceDefinitions
      }
    "
}
ReferenceDefinitions -> [referenceDefinition / '\n']
ReferenceDefinition -> "in §typeReference property §propertyReference refers-to §typeReferences"
    """

    init {
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}