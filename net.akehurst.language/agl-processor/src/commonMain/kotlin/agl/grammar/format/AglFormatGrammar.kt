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
import net.akehurst.language.agl.grammar.scopes.AglScopesGrammar
import net.akehurst.language.api.grammar.GrammarRule


internal object AglFormatGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglFormat") {
    //companion object {
    const val goalRuleName = "unit"
    private fun createRules(): List<GrammarRule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglFormat");
        b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"));
        b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
        b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"));

        b.rule("unit").concatenation(b.nonTerminal("ruleList"))
        b.rule("ruleList").multi(0, -1, b.nonTerminal("formatRule"))
        b.rule("formatRule").concatenation(b.nonTerminal("typeReference"), b.terminalLiteral("->"), b.nonTerminal("formatExpression"))
        b.rule("formatExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("stringExpression"),
            b.nonTerminal("whenExpression")
        )
        b.rule("whenExpression").concatenation(b.terminalLiteral("when"), b.terminalLiteral("{"), b.nonTerminal("whenOptionList"), b.terminalLiteral("}"))
        b.rule("whenOptionList").multi(0,-1,b.nonTerminal("whenOption"))
        b.rule("whenOption").concatenation(b.nonTerminal("expression"), b.terminalLiteral("->"), b.nonTerminal("formatExpression"))
        b.rule("stringExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("literalString"),
            b.nonTerminal("templateString")
        )
        b.rule("literalString").concatenation(b.nonTerminal("LITERAL_STRING"))
        b.rule("templateString").concatenation(b.terminalLiteral("\""), b.nonTerminal("templateContentList"), b.terminalLiteral("\""))
        b.rule("templateContentList").multi(0,-1,b.nonTerminal("templateContent"))
        b.rule("templateContent").choiceLongestFromConcatenationItem(
            b.nonTerminal("text"),
            b.nonTerminal("templateExpression")
        )
        b.rule("text").concatenation(b.nonTerminal("RAW_TEXT"))
        b.rule("templateExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("templateExpressionSimple"),
            b.nonTerminal("templateExpressionEmbedded")
        )
        b.rule("templateExpressionSimple").concatenation(b.nonTerminal("DOLLAR_IDENTIFIER"))
        b.rule("templateExpressionEmbedded").concatenation(b.terminalLiteral("\${"),b.nonTerminal("expression"),b.terminalLiteral("}"))
        //TODO b.rule("expression").concatenation(b.embed("",""))
        b.rule("expression").concatenation(b.nonTerminal("propertyReference"))
        b.rule("propertyReference").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("typeReference").concatenation(b.nonTerminal("IDENTIFIER"))
        b.leaf("DOLLAR_IDENTIFIER").concatenation(b.terminalPattern("[$][a-zA-Z_][a-zA-Z_0-9-]*"))
        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"))
        b.leaf("LITERAL_STRING").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)*'"))
        b.leaf("RAW_TEXT").concatenation(b.terminalPattern("([^\$\"\\\\]|\\\\.)+"))

        return b.grammar.rule
    }
    //}

    const val grammarStr = """
        namespace net.akehurst.language.agl
        grammar AglFormat {
            skip leaf WHITESPACE = "\s+" ;
            skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
            skip leaf SINGLE_LINE_COMMENT = "//[\n\r]*?" ;
        
            unit = ruleList ;
            ruleList = [formatRule]* ;
            formatRule = typeReference '->' formatExpression ;
            formatExpression
              = stringExpression
              | whenExpression
              ;
            whenExpression = 'when' '{' whenOptionList '}' ;
            whenOptionList = whenOption* ;
            whenOption = expression '->' formatExpression ;
            stringExpression = literalString | templateString ;
            literalString = LITERAL_STRING ;
            templateString = '"' templateContentList '"' ;
            templateContentList = templateContent* ;
            templateContent = text | templateExpression ;
            text = RAW_TEXT ;
            templateExpression = templateExpressionSimple | templateExpressionEmbedded ;
            templateExpressionSimple = DOLLAR_IDENTIFIER ;
            templateExpressionEmbedded = '$${'{'}' expression '}'
            
            expression = propertyReference ; //TODO
            
            typeReference = IDENTIFIER ;
            propertyReference = IDENTIFIER ;
            leaf DOLLAR_IDENTIFIER = '$' IDENTIFIER ;
            leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*"
            leaf LITERAL_STRING = "'([^'\\]|\\'|\\\\)*'" ;
            leaf RAW_TEXT = "(\\\"|[^\"])+" ;
        }
    """
    const val styleStr = """
    """
    const val formatterStr = """
    """

    init {
        super.rule.addAll(AglFormatGrammar.createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = AglScopesGrammar.grammarStr.trimIndent()
}