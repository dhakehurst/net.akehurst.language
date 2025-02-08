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

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.grammar.builder.grammarModel


object AglFormat {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammar.OPTION_defaultGoalRule, "unit"))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("unit")!!

    val grammarModel by lazy {
        grammarModel("AglFormat") {
            namespace("net.akehurst.language") {
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
                        lit("$"); lit("["); ebd("Expressions", "IDENTIFIER"); lit("/"); ebd("Expressions", "STRING"); lit("]")
                    }
                    concatenation("templateExpressionEmbedded") {
                        lit("\${"); ref("formatExpression"); lit("}")
                    }

                    concatenation("DOLLAR_IDENTIFIER", isLeaf = true) { pat("[$][a-zA-Z_][a-zA-Z_0-9-]*") }
                    concatenation("RAW_TEXT", isLeaf = true) { pat("([^\$\"\\\\]|\\\\.)+") }
                }
                grammar("Format") {
                    extendsGrammar(AglExpressions.grammar.selfReference)

                    concatenation("unit") {
                        ref("namespace"); lst(1, -1) { ref("format") }
                    }
                    concatenation("format") {
                        lit("format"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
                        lst(1, -1) { ref("formatRule") }
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
                        ref("whenExpression")
                    }
                    concatenation("whenExpression") {
                        lit("when"); lit("{"); lst(1, -1) { ref("whenOption") }; lit("}")
                    }
                    concatenation("whenOption") {
                        ref("expression"); lit("->"); ref("formatExpression")
                    }
                }
            }
        }
    }
    val targetGrammar by lazy { grammarModel.findDefinitionOrNullByQualifiedName(QualifiedName("net.akehurst.language.Format")) !! }


    const val grammarStr = """
        namespace net.akehurst.language.agl
        grammar Template {
            // no skip rules
            templateString = '"' templateContentList '"' ;
            templateContentList = templateContent* ;
            templateContent = text | templateExpression ;
            text = RAW_TEXT ;
            templateExpression = templateExpressionProperty | templateExpressionList | templateExpressionEmbedded ;
            templateExpressionProperty = DOLLAR_IDENTIFIER ;
            templateExpressionList = '$' '[' Expressions::expression '/' Expressions::STRING ']' ;
            templateExpressionEmbedded = '$${'{'}' formatExpression '}'
            
            leaf DOLLAR_IDENTIFIER = '$' IDENTIFIER ;
            leaf RAW_TEXT = "(\\\"|[^\"])+" ;
        }
        
        grammar AglFormat extends Expressions {        
            unit = namespace format+ ;
            format = 'format' IDENTIFIER extends? '{' ruleList '}' ;
            extends = ':' [possiblyQualifiedName / ',']+ ;
            ruleList = formatRule+ ;
            formatRule = typeReference '->' formatExpression ;
            formatExpression
              = expression
              | Template::templateString
              | whenExpression
              ;
            whenExpression = 'when' '{' whenOptionList '}' ;
            whenOptionList = whenOption* ;
            whenOption = expression '->' formatExpression ;
        }
    """
    const val styleStr = """
    """
    const val formatterStr = """
    """

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}