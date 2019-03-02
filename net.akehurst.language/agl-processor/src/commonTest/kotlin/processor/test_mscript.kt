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

import net.akehurst.language.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_mscript {
    companion object {
        val grammarStr = """
namespace com.yakindu.modelviewer.parser

grammar Mscript {

    skip WHITESPACE = "\s+" ;

    script = statement* ;
    statement
      = functionCall ';'
      | 'if' expression 'then' statement* 'else' statement* 'end'
      //TODO: others
      ;

    functionCall = NAME '(' arguments ')' ;
    arguments = [ expression / ',' ]* ;

    expression
      = rootVariable
      < literal
      < functionCall
      ;

    rootVariable = NAME ;

    literal
      = BOOLEAN
      | INTEGER
      | REAL
      | SINGLE_QUOTE_STRING
      ;


    NAME = "[a-zA-Z_][a-zA-Z_0-9]*" ;

    BOOLEAN             = 'true' | 'false' ;
    INTEGER             = "[0-9]+" ;
    REAL                = "([0-9]+[.])?[0-9]+" ;
    SINGLE_QUOTE_STRING = "'(?:\\?.)*?'" ;
}
    """.trimIndent()
        val sut = Agl.processor(grammarStr)
    }


    @Test
    fun process_expression_true() {

        val text = "true"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
        assertEquals("expression", actual.root.name)
    }

    @Test
    fun process_expression_rootVarriable() {

        val text = "abc"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_expression_func() {

        val text = "func()"
        val actual = sut.parse("expression", text)

        assertNotNull(actual)
    }

    @Test
    fun process_script_func() {

        val text = "func();"
        val actual = sut.parse("script",text)

        assertNotNull(actual)
    }

    @Test
    fun parse_script_func_args() {

        val text = "func(false,1,'abc',3.14, root);"
        val actual = sut.parse("script",text)

        assertNotNull(actual)
    }
}