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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.builder.crossReferenceModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel


object AglFormat : LanguageObjectAbstract<AglFormatModel, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Format"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString = """
        namespace $NAMESPACE_NAME
        grammar Template {
            // no skip rules
            templateString = '"' templateContentList '"' ;
            templateContentList = templateContent* ;
            templateContent = text | templateExpression ;
            text = RAW_TEXT ;
            templateExpression = templateExpressionProperty | templateExpressionList | templateExpressionEmbedded ;
            templateExpressionProperty = DOLLAR_IDENTIFIER ;
            templateExpressionList = '$[' Expressions::propertyName '/' Expressions::STRING ']' ;
            templateExpressionEmbedded = '$${'{'}' AglFormat::formatExpression '}'
            
            leaf DOLLAR_IDENTIFIER = '$' IDENTIFIER ;
            leaf RAW_TEXT = "(\\\"|[^\"])+" ;
        }
        
        grammar $NAME extends ${AglExpressions.NAME} {        
            override definition = format ;
            format = 'format' IDENTIFIER extends? '{' ruleList '}' ;
            extends = ':' [possiblyQualifiedName / ',']+ ;
            ruleList = formatRule* ;
            formatRule = typeReference '->' formatExpression ;
            
            // TODO: override expression +=| formatExpression
            
            formatExpression
              = expression
              | Template::templateString
           //   | whenExpression
              ;
           // whenExpression = 'when' '{' whenOptionList '}' ;
           // whenOptionList = whenOption* ;
            override whenOption = expression '->' formatExpression ;
            override whenOptionElse = 'else' '->' formatExpression ;
        }
    """

    override val grammarModel by lazy {
        grammarModel(name=NAME, grammarRegistry = Agl.registry) {
            namespace(NAMESPACE_NAME) {
                grammar("Template") {
                    concatenation("templateString") {
                        lit("\""); lst(0, -1) { ref("templateContent") }; lit("\"")
                    }
                    choice("templateContent") {
                        ref("text")
                        ref("templateExpression")
                    }
                    concatenation("text") { ref("RAW_TEXT") }
                    choice("templateExpression") {
                        ref("templateExpressionProperty")
                        ref("templateExpressionList")
                        ref("templateExpressionEmbedded")
                    }
                    concatenation("templateExpressionProperty") { ref("DOLLAR_IDENTIFIER") }
                    concatenation("templateExpressionList") {
                        lit("\$["); ebd("Expressions", "propertyName"); lit("/"); ebd("Expressions", "STRING"); lit("]")
                    }
                    concatenation("templateExpressionEmbedded") {
                        lit("\${"); ebd("Format","formatExpression"); lit("}")
                    }
                    concatenation("DOLLAR_IDENTIFIER", isLeaf = true) { pat("[$][a-zA-Z_][a-zA-Z_0-9-]*") }
                    concatenation("RAW_TEXT", isLeaf = true) { pat("([^" +
                            "$\"\\\\]|\\\\.)+") }
                }
                grammar(NAME) {
                    extendsGrammar(AglExpressions.defaultTargetGrammar.selfReference)
                    concatenation("definition", OverrideKind.REPLACE) { ref("format") }
                    concatenation("format") {
                        lit("format"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
                        lst(0, -1) { ref("formatRule") }
                        lit("}")
                    }
                    concatenation("extends") {
                        lit(":"); spLst(1, -1) { ref("possiblyQualifiedName"); lit(",") }
                    }
                    concatenation("formatRule") {
                        ref("typeReference"); lit("->"); ref("formatExpression")
                    }
                    choice("formatExpression") {
                        ref("expression")
                        ebd("Template", "templateString")
              //          ref("whenExpression")
                    }
              //      concatenation("whenExpression") {
              //          lit("when"); lit("{"); lst(1, -1) { ref("whenOption") }; lit("}")
              //      }
                    concatenation("whenOption", OverrideKind.REPLACE) {
                        ref("expression"); lit("->"); ref("formatExpression")
                    }
                    concatenation("whenOptionElse", OverrideKind.REPLACE) {
                        lit("else"); lit("->"); ref("formatExpression")
                    }
                }
            }
        }
    }

    const val komposite = """
    """

    override val typesModel: TypeModel by lazy {
        typeModel(NAME, true, AglBase.typesModel.namespace) {  }
    }

    override val asmTransformModel: AsmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typeModel = typesModel,
            createTypes = false
        ) {
            namespace(NAMESPACE_NAME){
                ruleSet(NAME) {
                    //TODO
                }
            }
        }
    }

    override val crossReferenceModel: CrossReferenceModel by lazy {
        crossReferenceModel(AglStyle.NAME) {
            //TODO
        }
    }

    override val styleModel by lazy {
        styleModel(AglStyle.NAME) {
            namespace(AglStyle.NAMESPACE_NAME) {
                styles(AglStyle.NAME) {
                    metaRule("'[^']+'") {
                        declaration("foreground", "darkgreen")
                        declaration("font-weight", "bold")
                    }
                }
            }
        }
    }

    override val formatModel by lazy {
        formatModel(AglStyle.NAME) {
//            TODO("not implemented")
        }
    }

    override val defaultTargetGrammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<AglFormatModel>? by lazy { AglFormatSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<AglFormatModel, ContextWithScope<Any, Any>>? by lazy { AglFormatSemanticAnalyser() }
    override val completionProvider: CompletionProvider<AglFormatModel, ContextWithScope<Any, Any>>? by lazy { AglFormatCompletionProvider() }

}