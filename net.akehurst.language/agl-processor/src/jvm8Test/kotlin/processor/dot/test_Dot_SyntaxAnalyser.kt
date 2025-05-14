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
package net.akehurst.language.processor.dot


import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Dot_SyntaxAnalyser {

    companion object {
        private val grammarStr = this::class.java.getResource("/dot/version_9.0.0/grammar.agl").readText()
        var processor: LanguageProcessor<Asm, ContextWithScope<Any, Any>> = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
    }

    @Test
    fun dotTypeModel() {
        val actual = processor.typesModel
        val expected = grammarTypeModel("net.akehurst.language.example.dot", "Dot") {
            // graph = STRICT? type ID? '{' stmt_list '}' ;
            dataFor("graph", "Graph") {
                propertyPrimitiveType("STRICT", "String", true, 0)
                propertyPrimitiveType("type", "String", false, 1)
                propertyPrimitiveType("ID", "String", true, 2)
                propertyListTypeOf("stmt_list", "stmt1", false, 3)
            }
            // stmt_list = stmt1 * ;
            listTypeOf("stmt_list", "Stmt1")
            // stmt1 = stmt  ';'? ;
            dataFor("stmt1", "Stmt1") {
                propertyDataTypeOf("stmt", "Stmt", false, 0)
            }
            // 	stmt
            //	    = node_stmt
            //      | edge_stmt
            //      | attr_stmt
            //      | ID '=' ID
            //      | subgraph
            //      ;
            dataFor("stmt", "Stmt") {
                subtypes("Node_stmt", "Edge_stmt", "Attr_stmt", "Subgraph")
            }
            // node_stmt = node_id attr_lists? ;
            dataFor("node_stmt", "Node_stmt") {
                propertyDataTypeOf("node_id", "Node_id", false, 1)
                propertyDataTypeOf("attr_lists", "Attr_lists", true, 1)
            }
            // node_id = ID port? ;
            dataFor("node_id", "Node_id") {
                propertyDataTypeOf("id", "Id", false, 0)
                propertyDataTypeOf("port", "Port", true, 1)
            }
            dataFor("", "expressionStatement") {
                // expressionStatement = expression ;
                propertyDataTypeOf("expression", "expression", false, 0)
            }
            dataFor("", "expression") {
                // expression
                //   = rootVariable
                //   | literalExpression
                //   | matrix
                //   | functionCallOrIndex
                //   | prefixExpression
                //   | infixExpression
                //   | groupExpression
                //   ;
                subtypes("rootVariable", "literalExpression", "matrix", "functionCallOrIndex", "prefixExpression", "infixExpression", "groupExpression")
            }
            dataFor("", "groupExpression") {
                // groupExpression = '(' expression ')' ;
                propertyDataTypeOf("expression", "expression", false, 1)
            }
            dataFor("", "functionCallOrIndex") {
                // functionCall = NAME '(' argumentList ')' ;
                propertyPrimitiveType("NAME", "String", false, 0)
                propertyListSeparatedTypeOf("argumentList", "argument", "String", false, 2)
            }
            dataFor("argumentList", "argumentList") {
                // argumentList = [ argument / ',' ]* ;
                propertyListSeparatedTypeOf("argument", "argument", "String", false, 0)
            }
            dataFor("argument", "argument") {
                // argument = expression | colonOperator ;
                subtypes("expression", "colonOperator")
            }
            dataFor("", "prefixExpression") {
                // prefixExpression = prefixOperator expression ;
                propertyPrimitiveType("prefixOperator", "String", false, 0)
                propertyDataTypeOf("expression", "expression", false, 1)
            }
            stringTypeFor("prefixOperator")
            //elementType("prefixOperator") {
            // prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataFor("", "infixExpression") {
                // infixExpression =  [ expression / infixOperator ]2+ ;
                propertyListSeparatedTypeOf("expression", "expression", "String", false, 0)
            }
            stringTypeFor("infixOperator")
            //elementType("infixOperator") {
            // infixOperator
            //        = '.*' | '*' | './' | '/' | '.\\' | '\\' | '+' | '-'    // arithmetic
            //        | '==' | '~=' | '>' | '>=' | '<' | '<='                 // relational
            //        | '&' | '|' | '&&' | '||' | '~'                         // logical
            //        | ':'                                                   // vector creation
            //        ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataFor("", "colonOperator") {
                propertyPrimitiveType("COLON", "String", false, 0)
            }
            dataFor("", "matrix") {
                // matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
                propertyListSeparatedTypeOf("row", "row", "String", false, 1)
            }
            dataFor("", "row") {
                // row = expression (','? expression)* ;
                propertyDataTypeOf("expression", "expression", false, 0)
                propertyListOfTupleType("\$group", false, 1) {
                    typeRef(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, "String", true)
                    typeRef("expression", "expression", false)
                }
            }
            dataFor("", "literalExpression") {
                propertyPrimitiveType("literalValue", "String", false, 0)
            }
            stringTypeFor("literalValue")
            //elementType("literalValue") {
            //    literal
            //      = BOOLEAN
            //      | number
            //      | SINGLE_QUOTE_STRING
            //      | DOUBLE_QUOTE_STRING
            //      ;
            //    propertyUnnamedPrimitiveType(PrimitiveType.ANY, false, 0)
            //}
            dataFor("", "rootVariable") {
                // rootVariable = NAME ;
                propertyPrimitiveType("NAME", "String", false, 0)
            }
            stringTypeFor("number")
            //elementType("number") {
            // number = INTEGER | REAL ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
        }
        assertEquals(expected.asString(), actual?.asString())
        TypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun one_node() {

        val sentence = """
            graph {
               a
            }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis { context(ContextAsmSimple()) }
        })
        val actual = result.asm?.root?.firstOrNull()
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(actual)

        val expected = asmSimple {
            element("Graph") {
                propertyString("strict", null)
                propertyString("type", "graph")
                propertyString("id", null)
                propertyListOfElement("stmt_list") {
                    element("Stmt1") {
                        propertyElementExplicitType("stmt", "Node_stmt") {
                            propertyElementExplicitType("node_id", "Node_id") {
                                propertyString("id", "a")
                                propertyString("port", null)
                            }
                            propertyString("attr_lists", null)
                        }
                    }
                }
            }
        }
        assertEquals(expected.asString(indentIncrement = " "), result.asm?.asString(indentIncrement = " "))
    }

    @Test
    fun one_edge() {

        val sentence = """
            graph {
               a -- b
            }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis { context(ContextAsmSimple()) }
        })
        val actual = result.asm?.root?.firstOrNull()
        assertNotNull(actual)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun one_node_html() {
        val sentence = """
graph {
  < <table></table> >
}
        """.trimIndent()
        val result = processor.process(sentence)
        val actual = result.asm?.root?.firstOrNull()
        assertNotNull(actual)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun ID_html() {
        val sentence = "<<x></x>>"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName("ID") } })
        val actual = result.asm?.root?.firstOrNull()
        assertNotNull(actual)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

}