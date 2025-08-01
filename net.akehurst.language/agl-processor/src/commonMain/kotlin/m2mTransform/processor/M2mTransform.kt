/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.m2mTransform.api.*
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

object M2mTransform : LanguageObjectAbstract<M2mTransformDomain, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "M2mTransform"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString: String = """
namespace $NAMESPACE_NAME

grammar $NAME : Base {
    override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    transform = 'transform' IDENTIFIER '(' domainParams ')' extends? '{' option* typeImport* transformRule* '} ;
    domainParams = [parameterDefinition / ',']2+ ;
    parameterDefinition = IDENTIFIER ':' DOMAIN_NAME ;
    leaf DOMAIN_NAME = IDENTIFIER ;
    extends = ':' [possiblyQualifiedName / ',']+ ;
    option = 'option' IDENTIFIER '=' expression ;
    typeImport = 'import-types' possiblyQualifiedName ;
    
    transformRule = relation | mapping ;
    relation = 'top'? 'relation' IDENTIFIER '{' pivot* relDomain{2+} when? where? '}' ;
    mapping = 'mapping' IDENTIFIER '{' mapDomain{2+} when? where? '}' ;
    
    pivot = 'pivot' variableDefinition ;
    relDomain = 'domain' IDENTIFIER IDENTIFIER ':' objectPattern
    mapDomain = 'domain' IDENTIFIER variableDefinition ':=' expression
    variableDefinition = IDENTIFIER ':' typeName ;
    typeName = possiblyQualifiedName ;
    expression = Expressions::expression ;
    
    when = 'when' '{' expression '}' ;
    where = 'where' '{' expression '}' ;
    
    objectPattern = typeName '{' propertyPattern*  '}';
    propertyPattern = IDENTIFIER '=' propertyPatternRhs ;
    propertyPatternRhs = expression | namedObjectPattern ;
    namedObjectPattern = (IDENTIFIER ':')? objectPattern ;
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
                        lst(0, -1) { ref("transform") }
                    }
                    concatenation("transform") {
                        lit("transform"); ref("IDENTIFIER"); lit("("); ref("domainParams"); lit(")"); opt { ref("extends") }
                        lit("{")
                        lst(0, -1) { ref("option") }
                        lst(0, -1) { ref("typeImport") }
                        lst(0, -1) { ref("transformRule") }
                        lit("}")
                    }
                    separatedList("domainParams", 2, -1) { ref("parameterDefinition"); lit(",") }
                    concatenation("parameterDefinition") { ref("IDENTIFIER"); lit(":"); ref("DOMAIN_NAME") }
                    concatenation("DOMAIN_NAME", isLeaf = true) { ref("IDENTIFIER") }
                    concatenation("extends") { lit(":"); spLst(1, -1) { ref("possiblyQualifiedName"); lit(",") } }
                    concatenation("typeImport") { lit("import-types"); ref("possiblyQualifiedName") }
                    choice("transformRule") {
                        ref("relation");
                        ref("mapping")
                    }
                    concatenation("relation") {
                        opt { lit("top") }; lit("relation"); ref("IDENTIFIER"); lit("{")
                        lst(0,-1) { ref("pivot") }
                        lst(2, -1) { ref("relDomain") }; opt { ref("when") }; opt { ref("where") }
                        lit("}")
                    }
                    concatenation("mapping") {
                        opt { lit("top") }; lit("mapping"); ref("IDENTIFIER"); lit("{")
                        lst(2, -1) { ref("mapDomain") }; opt { ref("when") }; opt { ref("where") }
                        lit("}")
                    }
                    concatenation("pivot") { lit("pivot"); ref("variableDefinition") }
                    concatenation("relDomain") {
                        lit("domain"); ref("IDENTIFIER"); ref("IDENTIFIER"); lit(":"); ref("objectPattern")
                    }
                    concatenation("mapDomain") {
                        lit("domain"); ref("IDENTIFIER"); ref("variableDefinition"); lit(":="); ref("expression")
                    }
                    concatenation("variableDefinition") { ref("IDENTIFIER"); lit(":"); ref("typeName") }
                    concatenation("typeName") { ref("possiblyQualifiedName") }
                    concatenation("expression") { ebd(AglExpressions.defaultTargetGrammar.selfReference, "expression") }
                    concatenation("when") { lit("when"); lit("{"); ref("expression"); lit("}") }
                    concatenation("where") { lit("where"); lit("{"); ref("expression"); lit("}") }
                    concatenation("objectPattern") { ref("typeName"); lit("{"); lst(0,-1) { ref("propertyPattern")}; lit("}") }
                    concatenation("propertyPattern") { ref("IDENTIFIER"); lit("=="); ref("propertyPatternRhs")}
                    choice("propertyPatternRhs") {
                        ref("expression")
                        ref("namedObjectPattern")
                    }
                    concatenation("namedObjectPattern") { opt { grp{ ref("IDENTIFIER"); lit(":") }}; ref("objectPattern") }
                }
            }
        }
    }

    const val komposite = """namespace net.akehurst.language.m2mTransform.api
        // TODO
    """

    override val typesModel by lazy {
        typeModel(NAME, true, AglBase.typesModel.namespace) {
            namespace("net.akehurst.language.m2mTransform.api", listOf("std", "net.akehurst.language.base.api")) {
                interface_("M2mTransformDomain") {}
            }
            namespace("net.akehurst.language.m2mTransform.asm", listOf("std", "net.akehurst.language.base.asm", "net.akehurst.language.m2mTransform.api")) {
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
                        "net.akehurst.language.m2mTransform.api",
                        "net.akehurst.language.m2mTransform.asm"
                    )
                    createObject("unit", "M2mTransformDomain") {

                    }
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

    override val syntaxAnalyser: SyntaxAnalyser<M2mTransformDomain> by lazy { M2mTransformSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<M2mTransformDomain, ContextWithScope<Any, Any>> by lazy { M2mTransformSemanticAnalyser() }
    override val completionProvider: CompletionProvider<M2mTransformDomain, ContextWithScope<Any, Any>> by lazy { M2mTransformCompletionProvider() }

    override fun toString(): String = "${NAMESPACE_NAME}.${NAME}"

}