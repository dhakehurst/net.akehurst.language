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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_mdlFile {

    private companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mdl {

    skip WHITESPACE = "\s+" ;
    skip COMMENT = "#.*?\n" ;

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

    IDENTIFIER = "[a-zA-Z_$][a-zA-Z_0-9$.-]*" ;

    BOOLEAN             = 'on' | 'off' ;
    INTEGER             = "([+]|[-])?[0-9]+" ;
    REAL                = "[-+]?[0-9]*[.][0-9]+([eE][-+]?[0-9]+)?|[-+]?[0-9]*[eE][-+]?[0-9]+" ;
    DOUBLE_QUOTE_STRING = "\"([^\"\\]|\\.)*\"";
}
    """.trimIndent()

        val processor = Agl.processorFromString(grammarStr)

    }

    @Test
    fun literal_BOOLEAN() {
        val actual = processor.parseForGoal("literal", "on")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("BOOLEAN", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_INTEGER() {
        val actual = processor.parseForGoal("literal", "1")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("INTEGER", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_1() {
        val actual = processor.parseForGoal("literal", "3.14")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_2() {
        val actual = processor.parseForGoal("literal", ".14")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_3() {
        val actual = processor.parseForGoal("literal", "3.14e-05")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_4() {
        val actual = processor.parseForGoal("literal", "3.0e5")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_5() {
        val actual = processor.parseForGoal("literal", ".3e5")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun literal_REAL_6() {
        val actual = processor.parseForGoal("literal", "1e-05")
        assertNotNull(actual)
        assertEquals("literal", actual.root.name)
        assertEquals("REAL", actual.root.asBranch.children[0].name)
    }

    @Test
    fun stringList_1() {
        val actual = processor.parseForGoal("stringList", "\"hello\"")
        assertNotNull(actual)
        assertEquals("stringList", actual.root.name)
        assertEquals(1, actual.root.asBranch.children[0].asBranch.children.size)
        actual.root.asBranch.children[0].asBranch.children.forEach {
            assertEquals("DOUBLE_QUOTE_STRING", it.name)
        }
    }

    @Test
    fun stringList_2() {
        val actual = processor.parseForGoal("stringList", "\"hello\" \"world\"")
        assertNotNull(actual)
        assertEquals("stringList", actual.root.name)
        assertEquals(2, actual.root.asBranch.children[0].asBranch.children.size)
        actual.root.asBranch.children[0].asBranch.children.forEach {
            assertEquals("DOUBLE_QUOTE_STRING", it.name)
        }
    }

    @Test
    fun stringList_3() {
        val actual = processor.parseForGoal("stringList", "\"aa\" \"bb\" \"cc\"")
        val list = actual.root.asBranch.children[0].asBranch.children
        assertNotNull(actual)
        assertEquals("stringList", actual.root.name)
        assertEquals(3, list.size)
        list.forEach {
            assertEquals("DOUBLE_QUOTE_STRING", it.name)
        }
    }

    @Test
    fun value_stringList_1() {
        val actual = processor.parseForGoal("value", "\"hello\"")
        val list = actual.root.asBranch.children[0].asBranch.children
        assertNotNull(actual)
        assertEquals("value", actual.root.name)
        assertEquals(1, list.size)
        list.forEach {
            assertEquals("DOUBLE_QUOTE_STRING", it.name)
        }
    }

    @Test
    fun value_stringList_2() {
        val actual = processor.parseForGoal("value", "\"hello\" \"world\"")
        assertNotNull(actual)
        assertEquals("value", actual.root.name)
    }

    @Test
    fun value_stringList_3() {
        val actual = processor.parseForGoal("value", "\"aa\" \"bb\" \"cc\"")
        assertNotNull(actual)
        assertEquals("value", actual.root.name)
    }

    @Test
    fun array() {
        val actual = processor.parseForGoal("matrix", "[ on, 1, 3.14, \"hello\" ]")
        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun array1() {
        val actual = processor.parseForGoal("matrix", "[ 1.0,2.0,3.0 ]")
        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun array2() {
        val actual = processor.parseForGoal("matrix", "[ 1.0, 2.0 ]")
        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }

    @Test
    fun row1() {
        val actual = processor.parseForGoal("row", "1.1,2.2")
        assertNotNull(actual)
        assertEquals("row", actual.root.name)
    }

    @Test
    fun matrix() {
        val actual = processor.parseForGoal("matrix", "[ 0,1; 2,3 ]")
        assertNotNull(actual)
        assertEquals("matrix", actual.root.name)
    }


    @Test
    fun section1() {

        val text = """
			Branch {
				DstBlock	"b4"
				DstPort		 2
			}
        """.trimIndent()

        val actual = processor.parseForGoal("section", text)

        assertNotNull(actual)
        assertEquals("section", actual.root.name)
    }

    @Test
    fun section2() {

        val text = """
		Line {
			Branch {
                DecimalParam	1.5
				DstBlock	"b4"
			}
		}
        """.trimIndent()

        val actual = processor.parseForGoal("section", text)

        assertNotNull(actual)
        assertEquals("section", actual.root.name)
    }

    @Test
    fun misc() {
        val actual = processor.parseForGoal("section", """
        Block {
			DecimalParam	1.5
			StringParam		"abc"
			OnParam		on
		}
        """.trimIndent())

        assertNotNull(actual)
        assertEquals("section", actual.root.name)
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
    }
}