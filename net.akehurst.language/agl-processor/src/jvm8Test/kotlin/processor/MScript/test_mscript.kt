/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.MScript

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.sentence.api.InputLocation
import testFixture.utils.parseError
import kotlin.test.*

class test_mscript {
    private companion object {
        private val grammarStr = this::class.java.getResource("/MScript/version_/grammar.agl").readText()
        val sut = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
    }

    @Test
    fun mscript_typeModel() {
        val actual = sut.typesModel
        val expected = grammarTypeModel("com.yakindu.modelviewer.parser", "Mscript") {
            dataType("script", "Script") {
                // script = statementList ;
                propertyListSeparatedTypeOf("statementList", "Line", "String", false, 0)
            }
            dataType("statementList", "StatementList") {
                // statementList = [line / "\R"]* ;
                propertyListSeparatedTypeOf("line", "Line", "String", false, 0)
            }
            dataType("line", "Line") {
                // line = [statement / ';']* ';'? ;
                propertyListSeparatedTypeOf("statement", "Statement", "String", false, 0)
            }
            dataType("statement", "Statement") {
                // statement
                //   = conditional
                //   | assignment
                //   | expressionStatement
                //   //TODO: others
                //   ;
                subtypes("Conditional", "Assignment", "ExpressionStatement")
            }
            dataType("conditional", "Conditional") {
                // conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;
                propertyDataTypeOf("expression", "Expression", false, 1)
                propertyListSeparatedTypeOf("statementList", "Line", "String", false, 3)
                propertyListSeparatedTypeOf("statementList2", "Line", "String", false, 5)
            }
            dataType("assignment", "Assignment") {
                // assignment = rootVariable '=' expression ;
                propertyDataTypeOf("rootVariable", "RootVariable", false, 0)
                propertyDataTypeOf("expression", "Expression", false, 2)
            }
            dataType("", "ExpressionStatement") {
                // expressionStatement = expression ;
                propertyDataTypeOf("expression", "Expression", false, 0)
            }
            dataType("", "Expression") {
                // expression
                //   = rootVariable
                //   | literalExpression
                //   | matrix
                //   | functionCallOrIndex
                //   | prefixExpression
                //   | infixExpression
                //   | groupExpression
                //   ;
                subtypes("RootVariable", "LiteralExpression", "Matrix", "FunctionCallOrIndex", "PrefixExpression", "InfixExpression", "GroupExpression")
            }
            dataType("", "GroupExpression") {
                // groupExpression = '(' expression ')' ;
                //superType("expression")
                propertyDataTypeOf("expression", "Expression", false, 1)
            }
            dataType("", "FunctionCallOrIndex") {
                // functionCall = NAME '(' argumentList ')' ;
                //superType("expression")
                propertyPrimitiveType("name", "String", false, 0)
                propertyListSeparatedTypeOf("argumentList", "Argument", "String", false, 2)
            }
            dataType("", "ArgumentList") {
                // argumentList = [ argument / ',' ]* ;
                propertyListSeparatedTypeOf("argument", "Argument", "String", false, 0)
            }
            dataType("", "Argument") {
                // argument = expression | colonOperator ;
                subtypes("Expression", "ColonOperator")
            }
            dataType("", "PrefixExpression") {
                // prefixExpression = prefixOperator expression ;
                propertyPrimitiveType("prefixOperator", "String", false, 0)
                propertyDataTypeOf("expression", "Expression", false, 1)
            }
            stringTypeFor("PrefixOperator")
            //elementType("prefixOperator") {
            // prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataType("", "InfixExpression") {
                // infixExpression =  [ expression / infixOperator ]2+ ;
                propertyListSeparatedTypeOf("expression", "Expression", "String", false, 0)
            }
            stringTypeFor("InfixOperator")
            //elementType("infixOperator") {
            // infixOperator
            //        = '.*' | '*' | './' | '/' | '.\\' | '\\' | '+' | '-'    // arithmetic
            //        | '==' | '~=' | '>' | '>=' | '<' | '<='                 // relational
            //        | '&' | '|' | '&&' | '||' | '~'                         // logical
            //        | ':'                                                   // vector creation
            //        ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataType("", "ColonOperator") {
                propertyPrimitiveType("colon", "String", false, 0)
            }
            dataType("", "Matrix") {
                // matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
                propertyListSeparatedTypeOf("row", "Row", "String", false, 1)
            }
            dataType("", "Row") {
                // row = expression (','? expression)* ;
                propertyDataTypeOf("expression", "Expression", false, 0)
                propertyListOfTupleType("\$group", false, 1) {
                    typeRef(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, "String", true)
                    typeRef("expression", "Expression", false)
                }
            }
            dataType("", "LiteralExpression") {
                propertyPrimitiveType("literalValue", "String", false, 0)
            }
            stringTypeFor("LiteralValue")
            //elementType("literalValue") {
            //    literal
            //      = BOOLEAN
            //      | number
            //      | SINGLE_QUOTE_STRING
            //      | DOUBLE_QUOTE_STRING
            //      ;
            //    propertyUnnamedPrimitiveType(PrimitiveType.ANY, false, 0)
            //}
            dataType("", "RootVariable") {
                // rootVariable = NAME ;
                propertyPrimitiveType("name", "String", false, 0)
            }
            stringTypeFor("Number")
            //elementType("number") {
            // number = INTEGER | REAL ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
        }
        assertEquals(expected.asString(), actual?.asString())
        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun process_empty_line() {

        val text = """
        """
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_empty_line_several() {

        val text = """


        """
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_single_line_comment() {

        val text = "% this is a comment"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

    }

    @Test
    fun process_single_line_comment_several() {

        val text = """
            % this is a comment
            % this is a comment
            % this is a comment
            % this is a comment
        """.trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

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
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_rootVariable_x() {

        val text = "x"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("rootVariable") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("rootVariable", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_expression_x() {

        val text = "x"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("expression", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_BOOLEAN_true() {

        val text = "true"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("BOOLEAN") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("BOOLEAN", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_expression_true() {

        val text = "true"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("expression", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_REAL_0p1() {

        val text = "0.1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("REAL", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_REAL_0p1em5() {

        val text = "0.1e-5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("REAL", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_REAL_p1() {

        val text = ".1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("REAL", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_REAL_p1e5() {

        val text = ".1e5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("REAL", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_REAL_1_fails() {

        val text = "1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("REAL") })

        val expIssues = listOf(
            parseError(InputLocation(0, 1, 1, 1, null), text, setOf("<GOAL>"), setOf("REAL"))
        )
        assertNull(result.sppt)
        assertEquals(expIssues, result.issues.errors)

    }

    @Test
    fun process_INTEGER_1_fails() {
        val text = "1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("INTEGER") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("INTEGER", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_empty() {
        val text = "''"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_simple() {
        val text = "'xxx'"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_SINGLE_QUOTE_STRING_withLineBreak() {
        val text = """'xx
            x'""".trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("SINGLE_QUOTE_STRING") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("SINGLE_QUOTE_STRING", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_0x0() {

        val text = "[]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_1x1() {

        val text = "[1]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_1x1_2() {

        val text = "[[1]]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_row_x4() {

        val text = "1 2 3 4"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("row") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("row", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_1x4() {

        val text = "[1 2 3 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_column_composition_1x4() {

        val text = "[1, 2, 3, 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_matrix_row_composition_4x1() {

        val text = "[1; 2; 3; 4]"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_field_access() {
        val text = "cn.src_cpu"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("matrix") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        assertEquals("matrix", result.sppt!!.treeData.userRoot!!.rule.tag)
    }

    @Test
    fun process_argumentList_50() {

        val text = "0" + ",0".repeat(49)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("argumentList") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func1() {

        val text = "func(a)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func2() {

        val text = "func(a,1)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func3() {

        val text = "func(a,1,'hello')"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func10() {

        val text = "func(a, 1, b, 2, c, 3, d, 4, e, 5)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func20() {

        val text = "func(a,1,b,2,c,3,d,4,e,5,f,6,g,7,h,8,i,9,j,10)"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func50() {

        val text = "fprintf(''" + ",0".repeat(49) + ")"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_func_func_1() {

        val text = "func( func(a) )"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_line_continuation() {

        val text = """
            func( 1, 2, ...
              3, 4)
        """.trimIndent()
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_operators_1() {

        val text = "1 + 1"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_operators_10() {

        val text = "1" + " + 1".repeat(10)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_operators_100() {

        val text = "1" + " + 1".repeat(100)
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_expression_groups() {

        val text = "((1+1)*(2+3)+4)*5"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("expression") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }

    @Test
    fun process_func_100_args() {
        val text = "fprintf(''" + ",0".repeat(99) + ");"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("script") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_functionCall_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("functionCallOrIndex") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_statement_func() {

        val text = "func()"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("statement") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_line_func() {

        val text = "func();"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("line") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_statementList_func() {

        val text = "func();"
        val result = sut.parse(text, Agl.parseOptions { goalRuleName("statementList") })

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())

    }

    @Test
    fun process_script_nested_func() {

        val text = "disp(get_param(gcbh,'xxx'))"
        val result = sut.process(text, Agl.options { parse { goalRuleName("script") } })

        val expected = asmSimple {
            element("Script") {
                propertyListOfElement("statementList") {
                    element("Line") {
                        propertyListOfElement("statement") {
                            element("ExpressionStatement") {
                                propertyElementExplicitType("expression", "FunctionCallOrIndex") {
                                    propertyString("name", "disp")
                                    propertyListOfElement("argumentList") {
                                        element("FunctionCallOrIndex") {
                                            propertyString("name", "get_param")
                                            propertyListOfElement("argumentList") {
                                                element("RootVariable") {
                                                    propertyString("name", "gcbh")
                                                }
                                                element("LiteralExpression") {
                                                    propertyString("literalValue", "'xxx'")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(result.issues.errors.isEmpty(),result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(expected.asString(indentIncrement = " "), result.asm?.asString(indentIncrement = " "))
    }

    @Test
    fun process_assigment_var_literal() {

        val text = "x=1"
        val parseResult = sut.parse(text, Agl.parseOptions { goalRuleName("assignment") })
        assertTrue(parseResult.issues.errors.isEmpty(),parseResult.issues.toString())

        val result = sut.process(text, Agl.options { parse { goalRuleName("assignment") } })
        assertTrue(result.issues.errors.isEmpty(),result.issues.toString())
        val actual = result.asm!!
        val expected = asmSimple {
            element("Assignment") {
                propertyElementExplicitType("lhs", "RootVariable") {
                    propertyString("name", "x")
                }
                propertyElementExplicitType("expression", "LiteralExpression") {
                    propertyString("literalValue", "1")
                }
            }
        }

        assertEquals(expected.asString("", "  "), actual.asString("", "  "))
    }

    @Test
    fun process_assigment_matrix_var() {

        val text = "[x,y] = pair"
        val parseResult = sut.parse(text, Agl.parseOptions { goalRuleName("assignment") })
        assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())

        val result = sut.process(text, Agl.options { parse { goalRuleName("assignment") } })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val actual = result.asm!!
        val expected = asmSimple {
            element("Assignment") {
                propertyElementExplicitType("lhs", "Matrix") {
                    propertyListOfElement("row") {
                        element("Row") {
                            propertyElementExplicitType("expression", "RootVariable") {
                                propertyString("name", "x")
                            }
                            propertyListOfElement("\$list") {
                                tuple {
                                    propertyElementExplicitType("expression", "RootVariable") {
                                        propertyString("name", "y")
                                    }
                                }
                            }
                        }
                    }
                }
                propertyElementExplicitType("expression", "RootVariable") {
                    propertyString("name", "pair")
                }
            }
        }

        assertEquals(expected.asString("", "  "), actual.asString("", "  "))
    }

    @Test
    fun demo() {

        val sentence = """
            %{
              Adapted from an example in the MathWorks Script Documentation
              https://uk.mathworks.com/help/matlab/learn_matlab/scripts.html
            %}
            % Create and plot a sphere with radius r.
            [x,y,z] = sphere;       % Create a unit sphere.
            r = 2;
            surf(x*r,y*r,z*r)       % Adjust each dimension and plot.
            axis(equal)             % Use the same scale for each axis. 
             
            % Find the surface area and volume.
            A = 4*pi*r^2;
            V = (4/3)*pi*r^3;
        """.trimIndent()

        val parseResult = sut.parse(sentence)
        assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())

        val result = sut.process(sentence)
        assertTrue(result.issues.errors.isEmpty(), parseResult.issues.toString())

    }

    @Test
    fun bug() {

        val sentence = """
            %{
              Adapted from an example in the MathWorks Script Documentation
              https://uk.mathworks.com/help/matlab/learn_matlab/scripts.html
            %}
            % Create and plot a sphere with radius r.
            [x,y,z] = sphere;       % Create a unit sphere.
            r = 2;
            surf(x*r,y*r,z*r)       % Adjust each dimension and plot.
            axis(equal)             % Use the same scale for each axis. 
             
            % Find the surface area and volume.
            A = 4*pi*r^2;
            V = (4/3)*pi*r^3;
        """.trimIndent()

        val parseResult = sut.parse(sentence)
        assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())

        val result = sut.process(sentence)
        assertTrue(result.issues.errors.isEmpty(), parseResult.issues.toString())

    }


}