/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_mdlFile {

    private companion object {
        val grammarStr = """
namespace test

grammar Mdl {

    skip leaf WHITESPACE = "\s+" ;
    skip leaf COMMENT = "#.*?\n" ;

    file = section+ ;

    section = IDENTIFIER '{'
        content*
    '}' ;

    content = section | parameter ;

    parameter = IDENTIFIER value ;

    value = stringList | matrix | identifier | literal ;
    identifier = IDENTIFIER ;
    matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
    row = [literal / ',']+ | literal+ ;

    stringList = DOUBLE_QUOTE_STRING+ ;

    literal
      = BOOLEAN
      < INTEGER
      < REAL
      < DOUBLE_QUOTE_STRING
      ;

    leaf IDENTIFIER = "[a-zA-Z_$][a-zA-Z_0-9$.-]*" ;

    leaf BOOLEAN             = 'on' | 'off' ;
    leaf INTEGER             = "([+]|[-])?[0-9]+" ;
    leaf REAL                = "[-+]?[0-9]*[.][0-9]+([eE][-+]?[0-9]+)?|[-+]?[0-9]*[eE][-+]?[0-9]+" ;
    leaf DOUBLE_QUOTE_STRING = "\"([^\"\\]|\\.)*\"";
}
    """.trimIndent()

        val processor = Agl.processorFromStringDefault(GrammarString(grammarStr)).processor!!

    }

    @Test
    fun mdlTypeModel() {
        val actual = processor.typeModel
        val expected = grammarTypeModel("test", "Mdl", "File") {
            //file = section+ ;
            dataType("file", "File") {
                propertyListTypeOf("section", "Section", false, 0)
            }
            //section = IDENTIFIER '{' content* '}' ;
            dataType("section", "Section") {
                propertyPrimitiveType("identifier", "String", false, 0)
                propertyListTypeOf("content", "Content", false, 1)
            }
            //content = section | parameter ;
            dataType("content", "Content") {
                subtypes("Section", "Parameter")
            }
            //parameter = IDENTIFIER value ;
            dataType("parameter", "Parameter") {
                propertyPrimitiveType("identifier", "String", false, 0)
                propertyDataTypeOf("value", "Value", false, 1)
            }
            //value = stringList | matrix | identifier | literal ;
            dataType("value", "Value") {
                subtypes("StringList", "Matrix", "Identifier", "Literal")
            }
            //identifier = IDENTIFIER ;
            dataType("identifier", "Identifier") {
                propertyPrimitiveType("identifier", "String", false, 0)
            }
            //matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
            //row = [literal / ',']+ | literal+ ;

            //stringList = DOUBLE_QUOTE_STRING+ ;
            dataType("stringList", "StringList") {
                propertyListType("double_quoted_string", false, 0) { primitiveRef("String") }
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun literal_BOOLEAN() {
        val result = processor.parse("on", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { BOOLEAN : 'on' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_INTEGER() {
        val result = processor.parse("1", Agl.parseOptions { goalRuleName("literal") })
        val expected = processor.spptParser.parse(
            """
            literal { INTEGER : '1' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_1() {
        val result = processor.parse("3.14", Agl.parseOptions { goalRuleName("literal") })
        val expected = processor.spptParser.parse(
            """
            literal { REAL : '3.14' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_2() {
        val result = processor.parse(".14", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { REAL : '.14' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_3() {
        val result = processor.parse("3.14e-05", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { REAL : '3.14e-05' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_4() {
        val result = processor.parse("3.0e5", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { REAL : '3.0e5' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_5() {
        val result = processor.parse(".3e5", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { REAL : '.3e5' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun literal_REAL_6() {
        val result = processor.parse("1e-05", Agl.parseOptions { goalRuleName("literal") })

        val expected = processor.spptParser.parse(
            """
            literal { REAL : '1e-05' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun stringList_1() {
        val result = processor.parse("\"hello\"", Agl.parseOptions { goalRuleName("stringList") })

        val expected = processor.spptParser.parse(
            """
            stringList { DOUBLE_QUOTE_STRING : '"hello"' }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun stringList_2() {
        val result = processor.parse("\"hello\" \"world\"", Agl.parseOptions { goalRuleName("stringList") })

        val expected = processor.spptParser.parse(
            """
            stringList { 
                DOUBLE_QUOTE_STRING : '"hello"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"world"'
            }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun stringList_3() {
        val result = processor.parse("\"aa\" \"bb\" \"cc\"", Agl.parseOptions { goalRuleName("stringList") })

        val expected = processor.spptParser.parse(
            """
            stringList { 
                DOUBLE_QUOTE_STRING : '"aa"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"bb"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"cc"'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun value_stringList_1() {
        val result = processor.parse("\"hello\"", Agl.parseOptions { goalRuleName("value") })

        val expected = processor.spptParser.parse(
            """
             value { literal { DOUBLE_QUOTE_STRING : '"hello"' } }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun value_stringList_2() {
        val result = processor.parse("\"hello\" \"world\"", Agl.parseOptions { goalRuleName("value") })

        val expected = processor.spptParser.parse(
            """
            value { stringList { 
                DOUBLE_QUOTE_STRING : '"hello"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"world"'
            } }
        """.trimIndent()
        )

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun value_stringList_3() {
        val result = processor.parse("\"aa\" \"bb\" \"cc\"", Agl.parseOptions { goalRuleName("value") })

        val expected = processor.spptParser.parse(
            """
            value { stringList { 
                DOUBLE_QUOTE_STRING : '"aa"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"bb"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"cc"'
            } }
        """.trimIndent()
        )

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun array() {
        val result = processor.parse("[ on, 1, 3.14, \"hello\" ]", Agl.parseOptions { goalRuleName("matrix") })

        val expected = processor.spptParser.parse(
            """
            matrix {
                '[' WHITESPACE : ' '
                §matrix§sepList1 { row { §row§sepList1 {
                    literal { BOOLEAN : 'on' }
                    ',' WHITESPACE : ' '
                    literal|1 { INTEGER : '1' }
                    ',' WHITESPACE : ' '
                    literal|2 { REAL : '3.14' }
                    ',' WHITESPACE : ' '
                    literal|3 { DOUBLE_QUOTE_STRING : '"hello"' WHITESPACE : ' ' }
                } } }
                ']'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun array1() {
        val result = processor.parse("[ 1.0,2.0,3.0 ]", Agl.parseOptions { goalRuleName("matrix") })

        val expected = processor.spptParser.parse(
            """
            matrix {
                '[' WHITESPACE : ' '
                §matrix§sepList1 { row { §row§sepList1 {
                    literal|2 { REAL : '1.0' }
                    ','
                    literal|2 { REAL : '2.0' }
                    ','
                    literal|2 { REAL : '3.0' WHITESPACE : ' ' }
                } } }
                ']'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun array2() {
        val result = processor.parse("[ 1.0, 2.0 ]", Agl.parseOptions { goalRuleName("matrix") })

        val expected = processor.spptParser.parse(
            """
            matrix {
                '[' WHITESPACE : ' '
                §matrix§sepList1 { row { §row§sepList1 {
                    literal|2 { REAL : '1.0' }
                    ',' WHITESPACE : ' ' 
                    literal|2 { REAL : '2.0' WHITESPACE : ' ' }
                } } }
                ']'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun row1() {
        val result = processor.parse("1.1,2.2", Agl.parseOptions { goalRuleName("row") })

        val expected = processor.spptParser.parse(
            """
            row { §row§sepList1 {
                literal|2 { REAL : '1.1' }
                ','
                literal|2 { REAL : '2.2' }
            } }
        """.trimIndent()
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun matrix() {
        val result = processor.parse("[ 0,1; 2,3 ]", Agl.parseOptions { goalRuleName("matrix") })

        val expected = processor.spptParser.parse(
            """
            matrix {
                '[' WHITESPACE : ' '
                §matrix§sepList1 {
                    row { §row§sepList1 {
                        literal|1 { INTEGER : '0' }
                        ','
                        literal|1 { INTEGER : '1' }
                    } }
                    ';' WHITESPACE : ' ' 
                    row { §row§sepList1 {
                        literal|1 { INTEGER : '2' }
                        ','
                        literal|1 { INTEGER : '3' WHITESPACE : ' ' }
                    } }
                }
                ']'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }


    @Test
    fun section1() {

        val text = """
			Branch {
			    DstBlock  "b4"
			    DstPort   2
			}
        """.trimIndent()

        val result = processor.parse(text, Agl.parseOptions { goalRuleName("section") })

        val expected = processor.spptParser.parse(
            """
            section {
                IDENTIFIER : 'Branch' WHITESPACE : ' '
                '{' WHITESPACE : '⏎    '
                    §section§multi1 {
                        content|1 { parameter {
                            IDENTIFIER : 'DstBlock' WHITESPACE : '  '
                            value|1 { literal|3 { DOUBLE_QUOTE_STRING : '"b4"' WHITESPACE : '⏎    ' } }
                        } }
                        content|1 { parameter {
                            IDENTIFIER : 'DstPort' WHITESPACE : '   '
                            value|1 { literal|1 { INTEGER : '2' WHITESPACE : '⏎' } }
                        } }
                    }
                '}'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun section2() {

        val text = """
		Line {
		    Branch {
                DecimalParam    1.5
		        DstBlock        "b4"
		    }
		}
        """.trimIndent()

        val result = processor.parse(text, Agl.parseOptions { goalRuleName("section") })

        val expected = processor.spptParser.parse(
            """
            section {
                IDENTIFIER : 'Line' WHITESPACE : ' '
                '{' WHITESPACE : '⏎    '
                §section§multi1 { content { section {
                    IDENTIFIER : 'Branch' WHITESPACE : ' '
                    '{' WHITESPACE : '⏎              '
                        §section§multi1 {
                            content|1 { parameter {
                                IDENTIFIER : 'DecimalParam' WHITESPACE : '    '
                                value|1 { literal|2 { REAL : '1.5' WHITESPACE : '⏎        ' } }
                            } }
                            content|1 { parameter {
                                IDENTIFIER : 'DstBlock' WHITESPACE : '        '
                                value|1 { literal|3 { DOUBLE_QUOTE_STRING : '"b4"' WHITESPACE : '⏎    ' } }
                            } }
                        }
                    '}' WHITESPACE : '⏎'
                } } }
                '}'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
    }

    @Test
    fun misc() {
        val result = processor.parse(
            """
            Block {
                DecimalParam   1.5
                StringParam    "abc"
                OnParam        on
            }
        """.trimIndent(),
            Agl.parseOptions { goalRuleName("section") }
        )

        val expected = processor.spptParser.parse(
            """
            section {
                IDENTIFIER : 'Block' WHITESPACE : ' '
                '{' WHITESPACE : '⏎    '
                    §section§multi1 {
                        content|1 { parameter {
                            IDENTIFIER : 'DecimalParam' WHITESPACE : '   '
                            value|1 { literal|2 { REAL : '1.5' WHITESPACE : '⏎    ' } }
                        } }
                        content|1 { parameter {
                            IDENTIFIER : 'StringParam' WHITESPACE : '    '
                            value|1 { literal|3 { DOUBLE_QUOTE_STRING : '"abc"' WHITESPACE : '⏎    ' } }
                        } }
                        content|1 { parameter {
                            IDENTIFIER : 'OnParam' WHITESPACE : '        '
                            value|1 { literal { BOOLEAN : 'on' WHITESPACE : '⏎' } }
                        } }
                    }
                '}'
            }
        """.trimIndent()
        )

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)

        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        //FIXME: fails because priorities should create BOOLEAN rather than IDENTIFIER - but doesn't

    }

    @Test
    fun misc2() {
        val result = processor.parse(
            """
            Object {
                  PropName		      "WindowsInfo"
                  ObjectID		      2
                  ClassName	      "Simulink.WindowInfo"
                  IsActive		      [1]
                  Location		      [705.0, 195.0, 1025.0, 639.0]
		    }
        """.trimIndent(),
            Agl.parseOptions { goalRuleName("section") }
        )

        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        //assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals("section", result.sppt!!.treeData.userRoot.rule.tag)
        //TODO
    }
}