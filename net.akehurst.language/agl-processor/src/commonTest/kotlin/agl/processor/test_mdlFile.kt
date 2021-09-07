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

import net.akehurst.language.api.sppt.SPPTParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_mdlFile {

    private companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mdl {

    skip leaf WHITESPACE = "\s+" ;
    skip leaf COMMENT = "#.*?\n" ;

    file = section+ ;

    section = IDENTIFIER '{'
        content*
    '}' ;

    content = section | parameter ;

    parameter = IDENTIFIER value ;

    value = stringList | literal | matrix | identifier ;
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

        val processor = Agl.processorFromString(grammarStr)

    }

    @Test
    fun literal_BOOLEAN() {
        val actual = processor.parseForGoal("literal", "on")

        val expected = processor.spptParser.parse("""
            literal { BOOLEAN : 'on' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_INTEGER() {
        val actual = processor.parseForGoal("literal", "1")
        val expected = processor.spptParser.parse("""
            literal|1 { INTEGER : '1' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_1() {
        val actual = processor.parseForGoal("literal", "3.14")
        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '3.14' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_2() {
        val actual = processor.parseForGoal("literal", ".14")

        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '.14' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_3() {
        val actual = processor.parseForGoal("literal", "3.14e-05")

        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '3.14e-05' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_4() {
        val actual = processor.parseForGoal("literal", "3.0e5")

        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '3.0e5' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_5() {
        val actual = processor.parseForGoal("literal", ".3e5")

        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '.3e5' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun literal_REAL_6() {
        val actual = processor.parseForGoal("literal", "1e-05")

        val expected = processor.spptParser.parse("""
            literal|2 { REAL : '1e-05' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun stringList_1() {
        val actual = processor.parseForGoal("stringList", "\"hello\"")

        val expected = processor.spptParser.parse("""
            stringList { DOUBLE_QUOTE_STRING : '"hello"' }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun stringList_2() {
        val actual = processor.parseForGoal("stringList", "\"hello\" \"world\"")

        val expected = processor.spptParser.parse("""
            stringList {
                DOUBLE_QUOTE_STRING : '"hello"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"world"'
            }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun stringList_3() {
        val actual = processor.parseForGoal("stringList", "\"aa\" \"bb\" \"cc\"")

        val expected = processor.spptParser.parse("""
            stringList {
                DOUBLE_QUOTE_STRING : '"aa"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"bb"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"cc"'
            }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun value_stringList_1() {
        val actual = processor.parseForGoal("value", "\"hello\"")

        val expected = processor.spptParser.parse("""
             value|1 { literal|3 { DOUBLE_QUOTE_STRING : '"hello"' } }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun value_stringList_2() {
        val actual = processor.parseForGoal("value", "\"hello\" \"world\"")

        val expected = processor.spptParser.parse("""
            value { stringList {
                DOUBLE_QUOTE_STRING : '"hello"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"world"'
            } }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun value_stringList_3() {
        val actual = processor.parseForGoal("value", "\"aa\" \"bb\" \"cc\"")

        val expected = processor.spptParser.parse("""
            value { stringList {
                DOUBLE_QUOTE_STRING : '"aa"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"bb"' WHITESPACE : ' '
                DOUBLE_QUOTE_STRING : '"cc"'
            } }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun array() {
        val actual = processor.parseForGoal("matrix", "[ on, 1, 3.14, \"hello\" ]")

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun array1() {
        val actual = processor.parseForGoal("matrix", "[ 1.0,2.0,3.0 ]")

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun array2() {
        val actual = processor.parseForGoal("matrix", "[ 1.0, 2.0 ]")

        val expected = processor.spptParser.parse("""
            matrix {
                '[' WHITESPACE : ' '
                §matrix§sepList1 { row { §row§sepList1 {
                    literal|2 { REAL : '1.0' }
                    ',' WHITESPACE : ' ' 
                    literal|2 { REAL : '2.0' WHITESPACE : ' ' }
                } } }
                ']'
            }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun row1() {
        val actual = processor.parseForGoal("row", "1.1,2.2")

        val expected = processor.spptParser.parse("""
            row { §row§sepList1 {
                literal|2 { REAL : '1.1' }
                ','
                literal|2 { REAL : '2.2' }
            } }
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun matrix() {
        val actual = processor.parseForGoal("matrix", "[ 0,1; 2,3 ]")

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }


    @Test
    fun section1() {

        val text = """
			Branch {
			    DstBlock  "b4"
			    DstPort   2
			}
        """.trimIndent()

        val actual = processor.parseForGoal("section", text)

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
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

        val actual = processor.parseForGoal("section", text)

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        assertEquals(expected.toStringAll,actual.toStringAll)
    }

    @Test
    fun misc() {
        val actual = processor.parseForGoal("section", """
            Block {
                DecimalParam   1.5
                StringParam    "abc"
                OnParam        on
            }
        """.trimIndent())

        val expected = processor.spptParser.parse("""
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
        """.trimIndent())

        //FIXME: fails because priorities should create BOOLEAN rather than IDENTIFIER - but doesn't
        assertEquals(expected.toStringAll,actual.toStringAll)

    }

    @Test
    fun misc2() {
        val actual = processor.parseForGoal("section", """
            Object {
                  PropName		      "WindowsInfo"
                  ObjectID		      2
                  ClassName	      "Simulink.WindowInfo"
                  IsActive		      [1]
                  Location		      [705.0, 195.0, 1025.0, 639.0]
		    }
        """.trimIndent())

        assertNotNull(actual)
        assertEquals("section", actual.root.name)
        //TODO
    }
}