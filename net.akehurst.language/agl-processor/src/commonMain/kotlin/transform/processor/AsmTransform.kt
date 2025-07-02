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

package net.akehurst.language.transform.processor

import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammar

object AsmTransform { //: LanguageObject {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Transform"
    const val goalRuleName = "unit"

    val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

     val grammarString: String = """
namespace $NAMESPACE_NAME

grammar $NAME : Base {
    override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    transform = 'transform' IDENTIFIER extends? '{' option* typeImport* transformRule* '} ;
    typeImport = 'import-types' possiblyQualifiedName ;
    extends = ':' [possiblyQualifiedName / ',']+ ;
    transformRule = grammarRuleName ':' transformRuleRhs ;
    transformRuleRhs = expressionRule | modifyRule ;
    expressionRule = expression ;
    modifyRule = '{' possiblyQualifiedTypeName '->' statement+ '}' ;
    statement
      = assignmentStatement
      ;
    assignmentStatement = propertyName grammarRuleIndex? ':=' expression ;
    propertyName = IDENTIFIER ;
    expression = Expression::expression ;
   
    grammarRuleName = IDENTIFIER ;
    grammarRuleIndex = '$' POSITIVE_INTEGER ;
    possiblyQualifiedTypeName = possiblyQualifiedName ;

    leaf POSITIVE_INTEGER = "[0-9]+" ;
}
    """

    val grammar = grammar(
        namespace = NAMESPACE_NAME,
        name = NAME
    ) {
        extendsGrammar(AglBase.defaultTargetGrammar.selfReference)

        concatenation("namespace", overrideKind = OverrideKind.REPLACE) {
            lit("namespace"); ref("possiblyQualifiedName")
            lst(0, -1) { ref("option") }
            lst(0, -1) { ref("import") }
            lst(0, -1) { ref("transform") }
        }
        concatenation("transform") {
            lit("transform"); ref("IDENTIFIER"); opt { ref("extends") }
            lit("{")
            lst(0, -1) { ref("option") }
            lst(0, -1) { ref("typeImport") }
            lst(0, -1) { ref("transformRule") }
            lit("}")
        }
        concatenation("extends") { lit(":"); spLst(1, -1) { ref("possiblyQualifiedName"); lit(",") } }
        concatenation("typeImport") { lit("import-types"); ref("possiblyQualifiedName") }
        concatenation("transformRule") {
            ref("grammarRuleName"); lit(":");ref("transformRuleRhs")
        }
        choice("transformRuleRhs") {
            ref("expressionRule")
            ref("modifyRule")
        }
        concatenation("expressionRule") {
            ref("expression")
        }
        concatenation("modifyRule") {
            lit("{"); ref("possiblyQualifiedTypeName"); lit("->"); lst(1, -1) { ref("assignmentStatement") }; lit("}")
        }
        concatenation("assignmentStatement") {
            ref("propertyName"); opt { ref("grammarRuleIndex") }; lit(":="); ref("expression")
        }
        concatenation("propertyName") { ref("IDENTIFIER") }
        concatenation("grammarRuleName") { ref("IDENTIFIER") }
        concatenation("possiblyQualifiedTypeName") { ref("possiblyQualifiedName") }
        concatenation("expression") { ebd(AglExpressions.defaultTargetGrammar.selfReference, "expression") }
        concatenation("grammarRuleIndex"){ lit("$"); ref("POSITIVE_INTEGER") }
        concatenation("POSITIVE_INTEGER", isLeaf = true) { pat("[0-9]+") } //TODO: move this into Base
    }


    const val styleStr = """
    """

    override fun toString(): String = grammarString.trimIndent()
}