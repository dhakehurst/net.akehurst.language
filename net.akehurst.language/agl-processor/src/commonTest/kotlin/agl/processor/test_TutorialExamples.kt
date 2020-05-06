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
    fun example1() {

    }

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
        val processor = Agl.processor(grammarStr)

        val sppt = processor.parse("int")
        val actual = sppt.toStringAll.trim()
        assertNotNull(sppt)

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
        val processor = Agl.processor(grammarStr)

        val sppt = processor.parse("xxx")
        val actual = sppt.toStringAll.trim()
        assertNotNull(sppt)

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
        val processor = Agl.processor(grammarStr)

        val sppt = processor.parse("boolean")
        val actual = sppt.toStringAll.trim()
        assertNotNull(sppt)

        val expected = """
                typeReference|1 { builtInType { 'boolean' } }
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
        val processor = Agl.processor(grammarStr)

        val sppt = processor.parse("int")
        val actual = sppt.toStringAll.trim()
        assertNotNull(sppt)

        val expected = """
                typeReference|1 { builtInType { 'int' } }
        """.trimIndent()

        assertEquals(expected, actual)
    }

}

