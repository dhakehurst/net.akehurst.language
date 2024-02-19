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
package net.akehurst.language.agl.language.format

import net.akehurst.language.agl.language.expressions.ExpressionsGrammar
import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.asm.*
import net.akehurst.language.api.language.grammar.GrammarRule


internal object AglFormatGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglFormat") {
    const val goalRuleName = "unit"
    private fun createRules(): List<GrammarRule> {
        val b = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglFormat");
        b.extendsGrammar(ExpressionsGrammar)

        b.rule("unit").concatenation(
            b.nonTerminal("namespace"),
            b.nonTerminal("formatList")
        )
        b.rule("formatList").multi(1, -1, b.nonTerminal("format"))
        b.rule("format").concatenation(
            b.terminalLiteral("format"), b.nonTerminal("qualifiedName"), b.terminalLiteral("{"),
            b.nonTerminal("ruleList"),
            b.terminalLiteral("}")
        )
        b.rule("ruleList").multi(1, -1, b.nonTerminal("formatRule"))
        b.rule("formatRule").concatenation(b.nonTerminal("typeReference"), b.terminalLiteral("->"), b.nonTerminal("formatExpression"))
        b.rule("formatExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("expression"),
            b.nonTerminal("templateString"),
            b.nonTerminal("whenExpression")
        )
        b.rule("whenExpression").concatenation(b.terminalLiteral("when"), b.terminalLiteral("{"), b.nonTerminal("whenOptionList"), b.terminalLiteral("}"))
        b.rule("whenOptionList").multi(0, -1, b.nonTerminal("whenOption"))
        b.rule("whenOption").concatenation(b.nonTerminal("expression"), b.terminalLiteral("->"), b.nonTerminal("formatExpression"))
        b.rule("templateString").concatenation(b.terminalLiteral("\""), b.nonTerminal("templateContentList"), b.terminalLiteral("\""))
        b.rule("templateContentList").multi(0, -1, b.nonTerminal("templateContent"))
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
        b.rule("templateExpressionEmbedded").concatenation(b.terminalLiteral("\${"), b.nonTerminal("formatExpression"), b.terminalLiteral("}"))

        b.rule("typeReference").concatenation(b.nonTerminal("IDENTIFIER"))
        b.leaf("DOLLAR_IDENTIFIER").concatenation(b.terminalPattern("[$][a-zA-Z_][a-zA-Z_0-9-]*"))
        b.leaf("RAW_TEXT").concatenation(b.terminalPattern("([^\$\"\\\\]|\\\\.)+"))

        return b.grammar.grammarRule
    }

    override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, "unit"))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("unit")!!

    const val grammarStr = """
        namespace net.akehurst.language.agl
        grammar AglFormat extends Expressions {        
            unit = namespace formatList ;
            formatList = format+ ;
            format = 'format' qualifiedName '{' ruleList '}' ;
            ruleList = formatRule+ ;
            formatRule = typeReference '->' formatExpression ;
            formatExpression
              = expression
              | templateString
              | whenExpression
              ;
              
            whenExpression = 'when' '{' whenOptionList '}' ;
            whenOptionList = whenOption* ;
            whenOption = expression '->' formatExpression ;
            
            templateString = '"' templateContentList '"' ;
            templateContentList = templateContent* ;
            templateContent = text | templateExpression ;
            text = RAW_TEXT ;
            templateExpression = templateExpressionSimple | templateExpressionEmbedded ;
            templateExpressionSimple = DOLLAR_IDENTIFIER ;
            templateExpressionEmbedded = '$${'{'}' formatExpression '}'
                        
            typeReference = IDENTIFIER ;
            propertyReference = IDENTIFIER ;
            leaf DOLLAR_IDENTIFIER = '$' IDENTIFIER ;
            leaf RAW_TEXT = "(\\\"|[^\"])+" ;
        }
    """
    const val styleStr = """
    """
    const val formatterStr = """
    """

    init {
        super.extends.add(
            GrammarReferenceDefault(ExpressionsGrammar.namespace, ExpressionsGrammar.name).also {
                it.resolveAs(ExpressionsGrammar)
            }
        )
        super.grammarRule.addAll(AglFormatGrammar.createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}