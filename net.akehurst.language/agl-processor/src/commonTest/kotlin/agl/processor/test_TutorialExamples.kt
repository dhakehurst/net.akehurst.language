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

package net.akehurst.language.agl.processor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_TutorialExamples {


    @Test
    fun example3_int_as_userDefinedType() {
        val grammarStr = """
            namespace test
            grammar Test {
                typeReference =  builtInType | userDefinedType;
                userDefinedType = NAME ;
                builtInType = 'int' | 'boolean' | 'real' ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val result = pr.processor!!.parse("int")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.errors.size, result.issues.toString())
        assertEquals(2, result.sppt!!.maxNumHeads)

        val actual = result.sppt!!.toStringAll.trim()

        val expected = "typeReference { userDefinedType { NAME : 'int' } }"

        assertEquals(expected, actual)
    }

    @Test
    fun example3_xxx_as_userDefinedType() {
        val grammarStr = """
            namespace test
            grammar Test {
                typeReference =  userDefinedType | builtInType;
                builtInType = 'int' | 'boolean' | 'real' ;
                userDefinedType = NAME ;
                NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val result = pr.processor!!.parse("xxx")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = result.sppt!!.toStringAll.trim()

        val expected = """
                typeReference { userDefinedType { NAME { "[a-zA-Z][a-zA-Z0-9]*" : 'xxx' } } }
        """.trimIndent()

        assertEquals(expected, actual)
    }

    @Test
    fun example3_boolean_as_builtInType() {
        val grammarStr = """
            namespace test
            grammar Test {
                typeReference =  userDefinedType | builtInType;
                builtInType = 'int' | 'boolean' | 'real' ;
                userDefinedType = NAME ;
                NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val result = pr.processor!!.parse("boolean")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.errors.size, result.issues.toString())
        assertEquals(2, result.sppt!!.maxNumHeads)

        val actual = result.sppt!!.toStringAll.trim()

        val expected = """
                typeReference { builtInType { 'boolean' } }
        """.trimIndent()

        assertEquals(expected, actual)
    }

    @Test
    fun example3_int_as_builtInType() {
        val grammarStr = """
            namespace test
            grammar Test {
                typeReference =  userDefinedType | builtInType;
                builtInType = 'int' | 'boolean' | 'real' ;
                userDefinedType = NAME ;
                NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val result = pr.processor!!.parse("int")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.errors.size, result.issues.toString())
        assertEquals(2, result.sppt!!.maxNumHeads)

        val actual = result.sppt!!.toStringAll.trim()

        val expected = """
                typeReference { builtInType { 'int' } }
        """.trimIndent()

        assertEquals(expected, actual)
    }

}

