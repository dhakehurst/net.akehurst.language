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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.typeModel.*
import kotlin.test.*

class test_deriveTypeModelFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
    }

    @Test
    fun nonleaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("a",false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("v",false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyUnnamedType(BuiltInType.STRING,false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_of_choice_1() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = 'a' | 'b' | 'c' ;
                M = 'x' | 'y' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                subTypes("L", "M")
            }
            elementType("L") {
                //superType("S")
                propertyUnnamedType(BuiltInType.STRING, false,0)
            }
            elementType("M") {
                //superType("S")
                propertyUnnamedType(BuiltInType.STRING,false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyUnnamedType(BuiltInType.STRING,true, 0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun multi_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyUnnamedListType(BuiltInType.STRING,false,0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun slist_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListType("as",BuiltInType.ANY,false,0)
            }
            elementType("as") {
                propertyUnnamedListType(BuiltInType.ANY,false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun slist_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = [a / ',']* ;
                a = 'a' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListType("as", BuiltInType.ANY,false,0) // of String
            }
            elementType("as") {
                propertyListType("a",BuiltInType.ANY,false,0)
            }
            elementType("a") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun slist_multi_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ass ;
                ass = [as / ',']* ;
                as = a* ;
                a = 'a' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListType("ass", BuiltInType.ANY,false,0) // of String
            }
            elementType("ass") {
                propertyListType("as",BuiltInType.ANY, false,0)
            }
            elementType("as") {
                propertyListTypeOf("a","a",false,0)
            }
            elementType("a") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("a",false,0)
                propertyStringType("b",false,1)
                propertyStringType("c",false,2)
            }
            elementType("a") {}
            elementType("b") {}
            elementType("c") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_x3_literal_with_separator() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ',' b ',' c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("a",false,0)
                propertyStringType("b",false,2)
                propertyStringType("c",false,4)
            }
            elementType("a") {}
            elementType("b") {}
            elementType("c") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_multi_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = 'a'* ;
            }
        """.trimIndent()

        val (grammars, gramIssues) = grammarProc.process<List<Grammar>, Any>(grammarStr)
        assertNotNull(grammars)
        assertTrue(gramIssues.isEmpty())

        val actual = TypeModelFromGrammar(grammars.last()).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListType("as",BuiltInType.STRING,false,0) // of String
            }
            elementType("as") {
                propertyUnnamedListType(BuiltInType.STRING,false,0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

}