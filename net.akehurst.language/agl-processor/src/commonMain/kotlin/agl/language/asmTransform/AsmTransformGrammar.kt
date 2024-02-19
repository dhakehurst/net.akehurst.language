/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.language.asmTransform

import net.akehurst.language.agl.language.base.BaseGrammar
import net.akehurst.language.agl.language.expressions.ExpressionsGrammar
import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.asm.*
import net.akehurst.language.api.language.grammar.GrammarRule

object AsmTransformGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AsmTransform") {

    const val goalRuleName = "unit"

    private fun createRules(): List<GrammarRule> {
        val b = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AsmTransform")
        b.extendsGrammar(BaseGrammar)

        b.rule("unit").concatenation(
            b.nonTerminal("namespace"),
            b.nonTerminal("transformList")
        )
        b.rule("transformList").multi(1, -1, b.nonTerminal("transform"))
        b.rule("transform").concatenation(
            b.terminalLiteral("transform"),
            b.nonTerminal("qualifiedName"),
            b.terminalLiteral("{"),
            b.nonTerminal("transformRuleList"),
            b.terminalLiteral("}"),
        )
        b.rule("transformRuleList").multi(1, -1, b.nonTerminal("transformRule"))
        b.rule("transformRule").concatenation(
            b.nonTerminal("grammarRuleName"),
            b.terminalLiteral(":"),
            b.nonTerminal("transformRuleRhs")
        )
        b.rule("transformRuleRhs").choiceLongestFromConcatenationItem(
            b.nonTerminal("createRule"),
            b.nonTerminal("modifyRule")
        )
        b.rule("createRule").concatenation(b.nonTerminal("typeName"))
        b.rule("modifyRule").concatenation(
            b.terminalLiteral("{"),
            b.nonTerminal("typeName"),
            b.terminalLiteral("->"),
            b.nonTerminal("statementList"),
            b.terminalLiteral("}"),
        )
        b.rule("statementList").multi(1, -1, b.nonTerminal("assignmentStatement"))
        b.rule("assignmentStatement").concatenation(
            b.nonTerminal("propertyName"),
            b.terminalLiteral(":="),
            b.embed("Expression", "expression")
        )
        b.rule("propertyName").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("grammarRuleName").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("typeName").concatenation(b.nonTerminal("qualifiedName"))

        return b.grammar.grammarRule
    }

    override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(ExpressionsGrammar.goalRuleName)!!

    const val grammarStr = """
namespace net.akehurst.language.agl

grammar AsmTransform {

    unit = namespace transform+ ;
    namespace = 'namespace' qualifiedName ;
    transform = 'transform' NAME '{' transformRule+ '} ;
    transformRule = grammarRuleName ':' transformRuleRhs ;
    transformRuleRhs = createRule | modifyRule ;
    createRule = typeName ;
    modifyRule = '{' typeName '->' statement+ '}'
    statement
      = assignmentStatement
      ;
    assignmentStatement = propertyName ':=' expression ;
    propertyName = IDENTIFIER ;
    expression = Expression::expression ;
   
    grammarRuleName = IDENTIFIER ;
    typeName = qualifiedName ;

}
    """

    const val styleStr = """
    """

    init {
        super.extends.add(
            GrammarReferenceDefault(BaseGrammar.namespace, BaseGrammar.name).also {
                it.resolveAs(BaseGrammar)
            }
        )

        super.grammarRule.addAll(createRules())
        //should only be one embedded grammar rule
        super.allResolvedEmbeddedRules.forEach {
            it.embeddedGrammarReference.resolveAs(ExpressionsGrammar)
        }
    }

    //TODO: gen this from the ASM
    override fun toString(): String = ExpressionsGrammar.grammarStr.trimIndent()
}