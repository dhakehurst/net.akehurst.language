/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor

import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.typeModel.StringType
import net.akehurst.language.api.typeModel.TypeModelTest
import net.akehurst.language.api.typeModel.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class test_mscript {
    companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mscript {

    // end-of-line ('\n') is not whitespace as it marks end of a line in a statementList
    skip leaf WHITESPACE = "[ \t\x0B\f]+" ;
    skip leafLINE_CONTINUATION =  "[.][.][.](?:.*)\R" ;
    skip leaf COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
         leaf MULTI_LINE_COMMENT = "%[{](?:.|\n)*?%[}]" ;
         leaf SINGLE_LINE_COMMENT = "(?:%[^{].+${'$'})|(?:%${'$'})" ;

    script = statementList ;
    statementList = [line / "\R"]* ;
    // if we treat '\n' as part of the WHITESPACE skip rule, we get ambiguity in statements
    line = [statement / ';']* ';'? ;

    statement
      = conditional
      | assignment
      | expressionStatement
      //TODO: others
      ;

    conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;
    assignment = rootVariable '=' expression ;

    expressionStatement = expression ;

    expression
      = rootVariable
      | literal
      | matrix
      | functionCall
      | prefixExpression
      | infixExpression
      | groupExpression
      ;

    groupExpression = '(' expression ')' ;

    functionCall = NAME '(' argumentList ')' ;
    argumentList = [ argument / ',' ]* ;
    argument = expression | COLON ;

    prefixExpression = prefixOperator expression ;
    prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;

    infixExpression =  [ expression / infixOperator ]2+ ;
    infixOperator
        = '.*' | '*' | './' | '/' | '.\\' | '\\' | '+' | '-'    // arithmetic
        | '==' | '~=' | '>' | '>=' | '<' | '<='                 // relational
        | '&' | '|' | '&&' | '||' | '~'                         // logical
        | ':'                                                   // vector creation
        ;

    matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
    row = expression (','? expression)* ;

    literal
      = BOOLEAN
      | number
      | SINGLE_QUOTE_STRING
      | DOUBLE_QUOTE_STRING
      ;

    rootVariable = NAME ;

    number = INTEGER | REAL ;

    leaf NAME = "[a-zA-Z_][a-zA-Z_0-9]*" ;

    leaf COLON               = ':' ;
    leaf BOOLEAN             = 'true' | 'false' ;
    leaf INTEGER             = "([+]|[-])?[0-9]+" ;
    leaf REAL                = "[-+]?[0-9]*[.][0-9]+([eE][-+]?[0-9]+)?" ;
    leaf SINGLE_QUOTE_STRING = "'(?:[^'\\]|\\.)*'" ;
    leaf DOUBLE_QUOTE_STRING = "\"(?:[^\"\\]|\\.)*\"" ;
}
    """.trimIndent()
        val sut = Agl.processorFromString<AsmSimple, Any>(grammarStr)
    }

    @Test
    fun mscript_typeModel() {
        val actual = sut.typeModel
        val expected = typeModel {
            elementType("script") {
                // script = statementList ;
                propertyListSeparatedTypeOf("statementList", "line", StringType, false, 0)
            }
            elementType("statementList") {
                // statementList = [line / "\R"]* ;
                propertyListSeparatedTypeOf("line", "line", StringType, false, 0)
            }
            elementType("line") {
                // line = [statement / ';']* ';'? ;
                propertyListSeparatedTypeOf("statement", "statement", StringType, false, 0)
                propertyUnnamedStringType(true, 1)
            }
            elementType("statement") {
                // statement
                //   = conditional
                //   | assignment
                //   | expressionStatement
                //   //TODO: others
                //   ;
                subTypes("conditional", "assignment", "expressionStatement")
            }
            elementType("conditional") {
                // conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;
                propertyElementTypeOf("expression", "expression", false, 1)
                propertyListSeparatedTypeOf("statementList", "line", StringType, false, 3)
                propertyListSeparatedTypeOf("statementList2", "line", StringType, false, 5)
            }
            elementType("assignment") {
                // assignment = rootVariable '=' expression ;
                propertyElementTypeOf("rootVariable", "rootVariable", false, 0)
                propertyElementTypeOf("expression", "expression", false, 2)
            }
            elementType("expressionStatement") {
                // expressionStatement = expression ;
                propertyElementTypeOf("expression", "expression", false, 0)
            }
            elementType("expression") {
                // expression
                //   = rootVariable
                //   | literal
                //   | matrix
                //   | functionCall
                //   | prefixExpression
                //   | infixExpression
                //   | groupExpression
                //   ;
                //subTypes("rootVariable", "literal", "matrix", "functionCall", "prefixExpression", "infixExpression", "groupExpression")
                propertyUnnamedAnyType(false, 0)
            }
            elementType("groupExpression") {
                // groupExpression = '(' expression ')' ;
                //superType("expression")
                propertyElementTypeOf("expression", "expression", false, 1)
            }
            elementType("functionCall") {
                // functionCall = NAME '(' argumentList ')' ;
                //superType("expression")
                propertyStringType("NAME", false, 0)
                propertyListSeparatedTypeOf("argumentList", "argument", StringType, false, 2)
            }
            elementType("argumentList") {
                // argumentList = [ argument / ',' ]* ;
                propertyListSeparatedTypeOf("argument", "argument", StringType, false, 0)
            }
            elementType("argument") {
                // argument = expression | COLON ;
                propertyUnnamedAnyType( false, 0)
            }
            elementType("prefixExpression") {
                // prefixExpression = prefixOperator expression ;
                propertyStringType("prefixOperator", false, 0)
                propertyElementTypeOf("expression", "expression", false, 1)
            }
            stringTypeFor("prefixOperator")
            //elementType("prefixOperator") {
                // prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            elementType("infixExpression") {
                // infixExpression =  [ expression / infixOperator ]2+ ;
                propertyListSeparatedTypeOf("expression", "expression", StringType, false, 0)
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
            elementType("matrix") {
                // matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
                propertyListSeparatedTypeOf("row", "row", StringType, false, 1)
            }
            elementType("row") {
                // row = expression (','? expression)* ;
                propertyElementTypeOf("expression", "expression", false, 0)
                propertyListOfTupleType("\$group", false, 1) {
                    propertyUnnamedStringType(true, 0)
                    propertyElementTypeOf("expression", "expression", false, 1)
                }
            }
            stringTypeFor("literal")
            //elementType("literal") {
                //    literal
                //      = BOOLEAN
                //      | number
                //      | SINGLE_QUOTE_STRING
                //      | DOUBLE_QUOTE_STRING
                //      ;
            //    propertyUnnamedPrimitiveType(PrimitiveType.ANY, false, 0)
            //}
            elementType("rootVariable") {
                // rootVariable = NAME ;
                propertyStringType("NAME", false, 0)
            }
            stringTypeFor("number")
            //elementType("number") {
                // number = INTEGER | REAL ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun process_single_line_comment() {

        val text = "% this is a comment"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_multi_line_comment() {

        val text = """
            %{
             a multiline comment
             a multiline comment
            %}
        """.trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_rootVariable_x() {

        val text = "x"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("rootVariable") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("rootVariable", result.sppt!!.root.name)
    }

    @Test
    fun process_expression_x() {

        val text = "x"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("expression", result.sppt!!.root.name)
    }

    @Test
    fun process_BOOLEAN_true() {

        val text = "true"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("BOOLEAN") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("BOOLEAN", result.sppt!!.root.name)
    }

    @Test
    fun process_expression_true() {

        val text = "true"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("expression", result.sppt!!.root.name)
    }

    @Test
    fun process_REAL_0p1() {

        val text = "0.1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("REAL", result.sppt!!.root.name)
    }

    @Test
    fun process_REAL_0p1em5() {

        val text = "0.1e-5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("REAL", result.sppt!!.root.name)
    }

    @Test
    fun process_REAL_p1() {

        val text = ".1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("REAL", result.sppt!!.root.name)
    }

    @Test
    fun process_REAL_p1e5() {

        val text = ".1e5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("REAL", result.sppt!!.root.name)
    }

    @Test
    fun process_REAL_1_fails() {

        val text = "1"
        val ex = ParseFailedException::class
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        val expIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^1",
                setOf("REAL")
            )
        )
        assertNull(result.sppt)
        assertEquals(expIssues, result.issues)

    }

    @Test
    fun process_INTEGER_1_fails() {
        val text = "1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("INTEGER") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("INTEGER", result.sppt!!.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_empty() {
        val text = "''"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_simple() {
        val text = "'xxx'"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_withLineBreak() {
        val text = """'xx
            x'""".trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_0x0() {

        val text = "[]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_1x1() {

        val text = "[1]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_1x1_2() {

        val text = "[[1]]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_row_x4() {

        val text = "1 2 3 4"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("row") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("row", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_1x4() {

        val text = "[1 2 3 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_column_composition_1x4() {

        val text = "[1, 2, 3, 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_matrix_row_composition_4x1() {

        val text = "[1; 2; 3; 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_field_access() {
        val text = "cn.src_cpu"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        assertEquals("matrix", result.sppt!!.root.name)
    }

    @Test
    fun process_argumentList_50() {

        val text = "0" + ",0".repeat(49)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("argumentList") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func1() {

        val text = "func(a)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func2() {

        val text = "func(a,1)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func3() {

        val text = "func(a,1,'hello')"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func10() {

        val text = "func(a, 1, b, 2, c, 3, d, 4, e, 5)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func20() {

        val text = "func(a,1,b,2,c,3,d,4,e,5,f,6,g,7,h,8,i,9,j,10)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func50() {

        val text = "fprintf(''" + ",0".repeat(49) + ")"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_func_func_1() {

        val text = "func( func(a) )"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_line_continuation() {

        val text = """
            func( 1, 2, ...
              3, 4)
        """.trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_operators_1() {

        val text = "1 + 1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_operators_10() {

        val text = "1" + " + 1".repeat(10)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_operators_100() {

        val text = "1" + " + 1".repeat(100)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_expression_groups() {

        val text = "((1+1)*(2+3)+4)*5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_script_empty() {

        val text = ""
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun process_script_blankline() {

        val text = """
        """
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_func_100_args() {
        val text = "fprintf(''" + ",0".repeat(99) + ");"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_functionCall_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("functionCall") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_statement_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("statement") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_line_func() {

        val text = "func();"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("line") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_statementList_func() {

        val text = "func();"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("statementList") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_script_func() {

        val text = "func();"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_script_func3() {

        val text = """
            func();
            func();
            func();
        """.trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun parse_script_func_args() {

        val text = "func(false,1,'abc',3.14, root);"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)

    }

    @Test
    fun process_script_nested_func() {

        val text = "disp(get_param(gcbh,'xxx'))"
        val result = sut.process(text, Agl.options { parse { goalRuleName("script") } })

        val expected = asmSimple {
            root("script") {
                propertyListOfElement("statementList") {
                    element("line") {
                        propertyListOfElement("statement") {
                            element("expressionStatement") {
                                propertyElementExplicitType("expression", "functionCall") {
                                    propertyString("NAME", "disp")
                                    propertyListOfElement("argumentList") {
                                        element("argument") {
                                            propertyUnnamedElement("functionCall") {
                                                propertyString("NAME", "get_param")
                                                propertyListOfElement("argumentList") {
                                                    element("argument") {
                                                        propertyUnnamedElement("rootVariable") {
                                                            propertyString("NAME", "gcbh")
                                                        }
                                                    }
                                                    element("argument") {
                                                        propertyUnnamedElement("literal") {
                                                            propertyUnnamedString("'xxx'")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        propertyUnnamedString(null)
                    }
                }
            }
        }

        assertNotNull(result.asm)
        assertEquals(emptyList(), result.issues)
        assertEquals(expected.asString(" "), result.asm?.asString(" "))
    }

}