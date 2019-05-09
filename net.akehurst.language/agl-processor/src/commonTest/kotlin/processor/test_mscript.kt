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

package net.akehurst.language.processor

import net.akehurst.language.api.parser.ParseFailedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull


class test_mscript {
    companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mscript {

    skip WHITESPACE = "\s+" ;
    skip SINGLE_LINE_COMMENT = "%.*?${'$'}" ;
    skip MULTI_LINE_COMMENT = "%[{].*?%[}]" ;

    script = statementList ;
    statementList = (statement ';'?)* ;
    statement
      = conditional
      | assignment
      | expressionStatement
      //TODO: others
      ;

    assignment = rootVariable '=' expression ;
    conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;

    expressionStatement = expression ;

    expression
      = rootVariable
      < literal
      < createVector
      < matrix
      < functionCall
      < infixExpression
      < groupExpression
      ;

    groupExpression = '(' expression ')' ;

    functionCall = NAME '(' argumentList ')' ;
    argumentList = [ expression / ',' ]* ;

    infixExpression =  expression (operator expression)+ ;
    //TODO: operator precedence !
    operator = arithmeticOperator | relationalOperator | logicalOperator ;
    arithmeticOperator = '+' | '-' | '*' | '.*' | '/' | './' | '\\' | '.\\' | '^' | '.^' | '\'' | '.\'' ;
    relationalOperator = '==' | '~=' | '>' | '>=' | '<' | '<=' ;
    logicalOperator = '&' | '|' | '&&' | '||' | '~' ;

    createVector = expression ':' expression (':' expression)? ;

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

    NAME = "[a-zA-Z_][a-zA-Z_0-9]*" ;

    BOOLEAN             = 'true' | 'false' ;
    INTEGER             = "([+]|[-])?[0-9]+" ;
    REAL                = "[-+]?[0-9]*[.][0-9]+([eE][-+]?[0-9]+)?" ;
    SINGLE_QUOTE_STRING = "'(?:\\?.)*?'" ;
    DOUBLE_QUOTE_STRING = "\"(?:\\?.)*?\"" ;
}
    """.trimIndent()
        val sut = Agl.processor(grammarStr)
    }

    @Test
    fun process_rootVariable_x() {

        val text = "x"
        val actual = sut.parse("rootVariable", text)

        assertNotNull(actual)
        assertEquals("rootVariable", actual.root.name)
    }

    @Test
    fun process_expression_x() {

        val text = "x"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
        assertEquals("expression", actual.root.name)
    }

    @Test
    fun process_BOOLEAN_true() {

        val text = "true"
        val actual = sut.parse("BOOLEAN", text)

        assertNotNull(actual)
        assertEquals("BOOLEAN", actual.root.name)
    }

    @Test
    fun process_expression_true() {

        val text = "true"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
        assertEquals("expression", actual.root.name)
    }

    @Test
    fun process_REAL_0p1() {

        val text = "0.1"
        val actual = sut.parse("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_0p1em5() {

        val text = "0.1e-5"
        val actual = sut.parse("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_p1() {

        val text = ".1"
        val actual = sut.parse("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_p1e5() {

        val text = ".1e5"
        val actual = sut.parse("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_1_fails() {

        val text = "1"
        val e = assertFailsWith(ParseFailedException::class) {
            sut.parse("REAL", text)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun process_INTEGER_1_fails() {
        val text = "1"
        val actual = sut.parse("INTEGER", text)

        assertNotNull(actual)
        assertEquals("INTEGER", actual.root.name)
    }

    @Test
    fun process_matrix_0x0() {

        val text = "[]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_1x1() {

        val text = "[1]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_1x1_2() {

        val text = "[[1]]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_1x4() {

        val text = "[1 2 3 4]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_column_composition_1x4() {

        val text = "[1, 2, 3, 4]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_row_composition_4x1() {

        val text = "[1; 2; 3; 4]"
        val actual = sut.parse("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_argumentList_50() {

        val text = "0" + ",0".repeat(49)
        val actual = sut.parse("argumentList", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func() {

        val text = "func()"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func1() {

        val text = "func(a)"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func2() {

        val text = "func(a,1)"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func3() {

        val text = "func(a,1,'hello')"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func10() {

        val text = "func(a, 1, b, 2, c, 3, d, 4, e, 5)"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func20() {

        val text = "func(a,1,b,2,c,3,d,4,e,5,f,6,g,7,h,8,i,9,j,10)"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func50() {

        val text = "fprintf(''" + ",0".repeat(49) + ")"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_func_100_args() {
        val text = "fprintf(''" + ",0".repeat(99) + ");"
        val actual = sut.parse("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func_func_1() {

        val text = "func( func(a) )"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_operators_1() {

        val text = "1 + 1"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_operators_100() {

        val text = "1"+" + 1".repeat(100)
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_func() {

        val text = "func();"
        val actual = sut.parse("script", text)

        assertNotNull(actual)
    }

    @Test
    fun parse_script_func_args() {

        val text = "func(false,1,'abc',3.14, root);"
        val actual = sut.parse("script", text)

        assertNotNull(actual)
    }
}