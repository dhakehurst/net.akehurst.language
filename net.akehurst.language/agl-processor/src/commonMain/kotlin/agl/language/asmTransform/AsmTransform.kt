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

import net.akehurst.language.agl.language.base.AglBase
import net.akehurst.language.agl.language.expressions.AglExpressions
import net.akehurst.language.agl.language.grammar.asm.builder.grammar

object AsmTransform {

    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(AglExpressions.goalRuleName)!!

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "Transform"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)

        concatenation("unit") {
            ref("namespace"); lst(1, -1) { ref("transform") }
        }
        concatenation("transform") {
            lit("transform");ref("IDENTIFIER");lit("{")
            lst(1, -1) { ref("transformRule") }
            lit("}")
        }
        concatenation("transformRule") {
            ref("grammarRuleName"); lit(":");ref("transformRuleRhs")
        }
        choice("transformRuleRhs") {
            ref("createRule")
            ref("modifyRule")
        }
        concatenation("createRule") {
            ref("possiblyQualifiedTypeName"); opt { ref("statementBlock") }
        }
        concatenation("statementBlock") {
            lit("{"); lst(1, -1) { ref("assignmentStatement") }; lit("}")
        }
        concatenation("modifyRule") {
            lit("{"); ref("possiblyQualifiedTypeName"); lit("->"); lst(1, -1) { ref("assignmentStatement") }; lit("}")
        }
        concatenation("assignmentStatement") {
            ref("propertyName"); lit(":="); ebd(AglExpressions.grammar.selfReference, "expression")
        }
        concatenation("propertyName") { ref("IDENTIFIER") }
        concatenation("grammarRuleName") { ref("IDENTIFIER") }
        concatenation("possiblyQualifiedTypeName") { ref("qualifiedName") }
    }

    const val grammarStr = """
namespace net.akehurst.language.agl

grammar Transform : Base {

    unit = namespace transform+ ;
    namespace = 'namespace' qualifiedName ;
    transform = 'transform' IDENTIFIER '{' transformRule+ '} ;
    transformRule = grammarRuleName ':' transformRuleRhs ;
    transformRuleRhs = createRule | modifyRule ;
    createRule = possiblyQualifiedTypeName statementBlock? ;
    statementBlock = '{' statement+ '}' ;
    modifyRule = '{' possiblyQualifiedTypeName '->' statement+ '}' ;
    statement
      = assignmentStatement
      ;
    assignmentStatement = propertyName ':=' expression ;
    propertyName = IDENTIFIER ;
    expression = Expression::expression ;
   
    grammarRuleName = IDENTIFIER ;
    possiblyQualifiedTypeName = qualifiedName ;

}
    """

    const val styleStr = """
    """

    //TODO: gen this from the ASM
    override fun toString(): String = AglExpressions.grammarStr.trimIndent()
}