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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.syntaxAnalyser.AsmSimple
import net.akehurst.language.api.syntaxAnalyser.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_mscript {
    companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mscript {

    // end-of-line ('\n') is not whitespace as it marks end of a line in a statementList
    skip WHITESPACE = "[ \t\x0B\f]+" ;
    skip LINE_CONTINUATION =  "[.][.][.](?:.*)\R" ;
    skip COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
         MULTI_LINE_COMMENT = "%[{](?:.|\n)*?%[}]" ;
         SINGLE_LINE_COMMENT = "(?:%[^{].+${'$'})|(?:%${'$'})" ;

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

    assignment = rootVariable '=' expression ;
    conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;

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

    NAME = "[a-zA-Z_][a-zA-Z_0-9]*" ;

    COLON               = ':' ;
    BOOLEAN             = 'true' | 'false' ;
    INTEGER             = "([+]|[-])?[0-9]+" ;
    REAL                = "[-+]?[0-9]*[.][0-9]+([eE][-+]?[0-9]+)?" ;
    SINGLE_QUOTE_STRING = "'(?:[^'\\]|\\.)*'" ;
    DOUBLE_QUOTE_STRING = "\"(?:[^\"\\]|\\.)*\"" ;
}
    """.trimIndent()
        val sut = Agl.processorFromString(grammarStr)
    }

    @Test
    fun process_single_line_comment() {

        val text = "% this is a comment"
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)

    }

    @Test
    fun process_multi_line_comment() {

        val text = """
            %{
             a multiline comment
             a multiline comment
            %}
        """.trimIndent()
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)

    }

    @Test
    fun process_rootVariable_x() {

        val text = "x"
        val actual = sut.parseForGoal("rootVariable", text)

        assertNotNull(actual)
        assertEquals("rootVariable", actual.root.name)
    }

    @Test
    fun process_expression_x() {

        val text = "x"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
        assertEquals("expression", actual.root.name)
    }

    @Test
    fun process_BOOLEAN_true() {

        val text = "true"
        val actual = sut.parseForGoal("BOOLEAN", text)

        assertNotNull(actual)
        assertEquals("BOOLEAN", actual.root.name)
    }

    @Test
    fun process_expression_true() {

        val text = "true"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
        assertEquals("expression", actual.root.name)
    }

    @Test
    fun process_REAL_0p1() {

        val text = "0.1"
        val actual = sut.parseForGoal("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_0p1em5() {

        val text = "0.1e-5"
        val actual = sut.parseForGoal("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_p1() {

        val text = ".1"
        val actual = sut.parseForGoal("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_p1e5() {

        val text = ".1e5"
        val actual = sut.parseForGoal("REAL", text)

        assertNotNull(actual)
        assertEquals("REAL", actual.root.name)
    }

    @Test
    fun process_REAL_1_fails() {

        val text = "1"
        val ex = ParseFailedException::class
        val e = assertFailsWith<ParseFailedException> {
            sut.parseForGoal("REAL", text)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun process_INTEGER_1_fails() {
        val text = "1"
        val actual = sut.parseForGoal("INTEGER", text)

        assertNotNull(actual)
        assertEquals("INTEGER", actual.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_empty() {
        val text = "''"
        val actual = sut.parseForGoal("SINGLE_QUOTE_STRING", text)

        assertNotNull(actual)
        assertEquals("SINGLE_QUOTE_STRING", actual.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_simple() {
        val text = "'xxx'"
        val actual = sut.parseForGoal("SINGLE_QUOTE_STRING", text)

        assertNotNull(actual)
        assertEquals("SINGLE_QUOTE_STRING", actual.root.name)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_withLineBreak() {
        val text = """'xx
            x'""".trimIndent()
        val actual = sut.parseForGoal("SINGLE_QUOTE_STRING", text)

        assertNotNull(actual)
        assertEquals("SINGLE_QUOTE_STRING", actual.root.name)
    }

    @Test
    fun process_matrix_0x0() {

        val text = "[]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_1x1() {

        val text = "[1]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_1x1_2() {

        val text = "[[1]]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_row_x4() {

        val text = "1 2 3 4"
        val actual = sut.parseForGoal("row", text)

        assertNotNull(actual)
        assertEquals("row", actual.root.name)
    }

    @Test
    fun process_matrix_1x4() {

        val text = "[1 2 3 4]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_column_composition_1x4() {

        val text = "[1, 2, 3, 4]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_matrix_row_composition_4x1() {

        val text = "[1; 2; 3; 4]"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_field_access() {
        val text = "cn.src_cpu"
        val actual = sut.parseForGoal("matrix", text)

        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun process_argumentList_50() {

        val text = "0" + ",0".repeat(49)
        val actual = sut.parseForGoal("argumentList", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func() {

        val text = "func()"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func1() {

        val text = "func(a)"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func2() {

        val text = "func(a,1)"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func3() {

        val text = "func(a,1,'hello')"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func10() {

        val text = "func(a, 1, b, 2, c, 3, d, 4, e, 5)"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func20() {

        val text = "func(a,1,b,2,c,3,d,4,e,5,f,6,g,7,h,8,i,9,j,10)"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func50() {

        val text = "fprintf(''" + ",0".repeat(49) + ")"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func_func_1() {

        val text = "func( func(a) )"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_line_continuation() {

        val text = """
            func( 1, 2, ...
              3, 4)
        """.trimIndent()
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_operators_1() {

        val text = "1 + 1"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_operators_10() {

        val text = "1" + " + 1".repeat(10)
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_operators_100() {

        val text = "1" + " + 1".repeat(100)
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_groups() {

        val text = "((1+1)*(2+3)+4)*5"
        val actual = sut.parseForGoal("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_empty() {

        val text = ""
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_blankline() {

        val text = """
        """
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_func_100_args() {
        val text = "fprintf(''" + ",0".repeat(99) + ");"
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_functionCall_func() {

        val text = "func()"
        val actual = sut.parseForGoal("functionCall", text)

        assertNotNull(actual)
    }

    @Test
    fun process_statement_func() {

        val text = "func()"
        val actual = sut.parseForGoal("statement", text)

        assertNotNull(actual)
    }

    @Test
    fun process_line_func() {

        val text = "func();"
        val actual = sut.parseForGoal("line", text)

        assertNotNull(actual)
    }

    @Test
    fun process_statementList_func() {

        val text = "func();"
        val actual = sut.parseForGoal("statementList", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_func() {

        val text = "func();"
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_func3() {

        val text = """
            func();
            func();
            func();
        """.trimIndent()
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun parse_script_func_args() {

        val text = "func(false,1,'abc',3.14, root);"
        val actual = sut.parseForGoal("script", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_nested_func() {

        val text = "disp(get_param(gcbh,'xxx'))"
        val actual = sut.processForGoal(AsmSimple::class,"script", text)

        val expected = asmSimple {
            element(":script") {

            }
        }

        assertEquals(expected.asString(" "), actual.asString(" "))
    }

}