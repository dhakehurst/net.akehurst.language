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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.builder.crossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.typemodel.builder.typeModel

object AsmTransform : LanguageObjectAbstract<AsmTransformDomain, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "AsmTransform"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString: String = """
namespace $NAMESPACE_NAME

grammar $NAME : Base {
    override namespace = 'namespace' possiblyQualifiedName option* import* asmTransform* ;
    asmTransform = 'asm-transform' IDENTIFIER extends? '{' option* typeImport* transformRule* '} ;
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

    override val crossReferenceString = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val styleString = """
        namespace $NAMESPACE_NAME
        styles $NAME {
            // TODO
        }
    """

    override val grammarModel by lazy {
        grammarModel(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglBase.defaultTargetGrammar.selfReference)

                    concatenation("namespace", overrideKind = OverrideKind.REPLACE) {
                        lit("namespace"); ref("possiblyQualifiedName")
                        lst(0, -1) { ref("option") }
                        lst(0, -1) { ref("import") }
                        lst(0, -1) { ref("asmTransform") }
                    }
                    concatenation("asmTransform") {
                        lit("asm-transform"); ref("IDENTIFIER"); opt { ref("extends") }
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
                    concatenation("grammarRuleIndex") { lit("$"); ref("POSITIVE_INTEGER") }
                    concatenation("POSITIVE_INTEGER", isLeaf = true) { pat("[0-9]+") } //TODO: move this into Base
                }
            }
        }
    }

    const val komposite = """namespace net.akehurst.language.asmTransform.api
        // TODO
    """

    override val typesModel by lazy {
        typeModel(NAME, true, AglBase.typesModel.namespace) {
            namespace("net.akehurst.language.asmTransform.api", listOf("std", "net.akehurst.language.base.api")) {
                interface_("AsmTransformDomain") {}
            }
            namespace("net.akehurst.language.asmTransform.asm", listOf("std", "net.akehurst.language.base.asm","net.akehurst.language.asmTransform.api")) {
                //TODO
            }
        }
    }

    override val asmTransformModel: AsmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typeModel = typesModel,
            createTypes = false
        ) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                ruleSet(NAME) {
                    importTypes(
                        "net.akehurst.language.asmTransform.api",
                        "net.akehurst.language.asmTransform.asm"
                    )
                    createObject("unit", "AsmTransformDomain") {

                    }
                    //TODO: currently the types are not found in the typemodel
                    //    createObject("unit", "DefinitionBlock") {
                    //        assignment("definitions", "child[1]")
                    //    }
                    /*
                    createObject("grammar", "Grammar") {
                        assignment("namespace", "child[1]")
                        assignment("name", "child[1]")
                        assignment("options", "child[4]")
                    }


                    createObject("embedded", "Embedded") {
                        assignment("embeddedGoalName", "child[2].name")
                        assignment(
                            "embeddedGrammarReference",
                            """
                            GrammarReference {
                                localNamespace := ???
                                nameOrQName := child[0]
                            }
                            """.trimIndent()
                        )
                    }
                    createObject("terminal", "Terminal") {
                        assignment("value", "child[0].dropAtBothEnds(1)")
                        assignment("isPattern", "1 == \$alternative")
                    }

                    transRule("qualifiedName", "String", "children.join()")

                    leafStringRule("LITERAL")
                    leafStringRule("PATTERN")
                    leafStringRule("POSITIVE_INTEGER")
                    leafStringRule("POSITIVE_INTEGER_GT_ZERO")

                     */
                }
            }
        }
    }

    override val crossReferenceModel: CrossReferenceModel by lazy {
        crossReferenceModel(NAME) {
            //TODO

        }
    }

    override val formatModel: AglFormatModel by lazy {
        formatModel(NAME) {
//            TODO("not implemented")
        }
    }

    override val styleModel: AglStyleModel by lazy {
        styleModel(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    // TODO
                }
            }
        }
    }

    override val defaultTargetGrammar: Grammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule: String = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<AsmTransformDomain> by lazy { AsmTransformSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<AsmTransformDomain, ContextWithScope<Any, Any>> by lazy { AsmTransformSemanticAnalyser() }
    override val completionProvider: CompletionProvider<AsmTransformDomain, ContextWithScope<Any, Any>> by lazy { AsmTransformCompletionProvider() }

    override fun toString(): String = "${NAMESPACE_NAME}.${NAME}"

}