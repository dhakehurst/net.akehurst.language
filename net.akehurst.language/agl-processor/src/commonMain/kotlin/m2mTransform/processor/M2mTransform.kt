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

import net.akehurst.language.agl.format.builder.formatDomain
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
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.regex.api.CommonRegexPatterns
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.types.builder.typesDomain

object M2mTransform : LanguageObjectAbstract<M2mTransformDomain, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "M2mTransform"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) }

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
    relation = 'abstract'? 'top'? 'relation' IDENTIFIER '{' pivot* domainObjectPattern{2+} when? where? '}' ;
    mapping = 'abstract'? 'top'? 'mapping' IDENTIFIER '{' domainObjectPattern+ domainAssignment when? where? '}' ;
    
    pivot = 'pivot' variableDefinition ;
    domainObjectPattern = 'domain' IDENTIFIER IDENTIFIER ':' objectPattern
    domainAssignment = 'domain' IDENTIFIER variableDefinition (':=' expression)?
    variableDefinition = IDENTIFIER ':' typeName ;
    typeName = possiblyQualifiedName ;
    expression = Expressions::expression ;
    
    when = 'when' '{' expression '}' ;
    where = 'where' '{' expression '}' ;
    
    objectPattern = typeName '{' propertyPattern*  '}';
    propertyPattern = IDENTIFIER '==' propertyPatternRhs ;
    propertyPatternRhs = expression | namedObjectPattern ;
    namedObjectPattern = (IDENTIFIER ':')? objectPattern ;
}
    """

    override val typesString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val kompositeString: String = """
        namespace net.akehurst.language.m2mTransform.api
          // TODO
    """.trimIndent()

    override val asmTransformString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val styleString: String = """
        namespace ${NAMESPACE_NAME}
          styles ${NAME} {
            $$ "${CommonRegexPatterns.LITERAL.escapedFoAgl.value}" {
              foreground: darkgreen;
              font-weight: bold;
            }
          }
      """

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val grammarDomain by lazy {
        grammarDomain(NAME) {
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
                        opt { lit("abstract") }; opt { lit("top") }; lit("relation"); ref("IDENTIFIER"); lit("{")
                        lst(0, -1) { ref("pivot") }
                        lst(2, -1) { ref("domainObjectPattern") }; opt { ref("when") }; opt { ref("where") }
                        lit("}")
                    }
                    concatenation("mapping") {
                        opt { lit("abstract") }; opt { lit("top") }; lit("mapping"); ref("IDENTIFIER"); lit("{")
                        lst(1, -1) { ref("domainObjectPattern") }; ref("domainAssignment"); opt { ref("when") }; opt { ref("where") }
                        lit("}")
                    }
                    concatenation("pivot") { lit("pivot"); ref("variableDefinition") }
                    concatenation("domainObjectPattern") {
                        lit("domain"); ref("IDENTIFIER"); ref("IDENTIFIER"); lit(":"); ref("objectPattern")
                    }
                    concatenation("domainAssignment") {
                        lit("domain"); ref("IDENTIFIER"); ref("variableDefinition"); opt { grp { lit(":="); ref("expression") } }
                    }
                    concatenation("variableDefinition") { ref("IDENTIFIER"); lit(":"); ref("typeName") }
                    concatenation("typeName") { ref("possiblyQualifiedName") }
                    concatenation("expression") { ebd(AglExpressions.defaultTargetGrammar.selfReference, "expression") }
                    concatenation("when") { lit("when"); lit("{"); ref("expression"); lit("}") }
                    concatenation("where") { lit("where"); lit("{"); ref("expression"); lit("}") }
                    concatenation("objectPattern") { ref("typeName"); lit("{"); lst(0, -1) { ref("propertyPattern") }; lit("}") }
                    concatenation("propertyPattern") { ref("IDENTIFIER"); lit("=="); ref("propertyPatternRhs") }
                    choice("propertyPatternRhs") {
                        ref("expression")
                        ref("namedObjectPattern")
                    }
                    concatenation("namedObjectPattern") { opt { grp { ref("IDENTIFIER"); lit(":") } }; ref("objectPattern") }
                }
            }
        }
    }

    const val komposite = """namespace net.akehurst.language.m2mTransform.api
        // TODO
    """

    override val typesDomain by lazy {
        typesDomain(NAME, true, AglBase.typesDomain.namespace) {
            namespace("net.akehurst.language.m2mTransform.api", listOf("std", "net.akehurst.language.base.api")) {
                interface_("M2mTransformDomain") {}
            }
            namespace("net.akehurst.language.m2mTransform.asm", listOf("std", "net.akehurst.language.base.asm", "net.akehurst.language.m2mTransform.api")) {
                //TODO
            }
        }
    }

    override val asmTransformDomain: AsmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typesDomain = typesDomain,
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

    override val crossReferenceDomain: CrossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {
            //TODO

        }
    }

    override val formatDomain: AglFormatDomain by lazy {
        formatDomain(NAME) {
//            TODO("not implemented")
        }
    }

    override val styleDomain by lazy {
        styleDomain(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    metaRule(CommonRegexPatterns.LITERAL.value) {
                        declaration("foreground", "darkgreen")
                        declaration("font-weight", "bold")
                    }
                }
            }
        }
    }

    override val defaultTargetGrammar: Grammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule: String = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<M2mTransformDomain> by lazy { M2mTransformSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<M2mTransformDomain, ContextWithScope<Any, Any>> by lazy { M2mTransformSemanticAnalyser() }
    override val completionProvider: CompletionProvider<M2mTransformDomain, ContextWithScope<Any, Any>> by lazy { M2mTransformCompletionProvider() }

    override fun toString(): String = "${NAMESPACE_NAME}.${NAME}"

}