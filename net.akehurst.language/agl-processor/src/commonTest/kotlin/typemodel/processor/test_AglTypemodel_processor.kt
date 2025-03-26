/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertTrue

class test_AglTypemodel_processor {

    private companion object {

        fun testPass(typeModeStr: String, expected: TypeModel) {
            val res = Agl.registry.agl.types.processor!!.process(typeModeStr)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val actual = res.asm!!

            TypeModelTest.tmAssertEquals(expected, actual)
        }
    }

    @Test
    fun empty() {
        val typesStr = $$"""
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {

        }
        testPass(typesStr, expected)
    }

    @Test
    fun namespace() {
        val typesStr = $$"""
            namespace test
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) { }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun singleton() {
        val typesStr = $$"""
            namespace test
              singleton Singleton
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                singleton("Singleton")
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun primitive() {
        val typesStr = $$"""
            namespace test
              primitive Primitive
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                primitive("Primitive")
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun enum() {
        val typesStr = $$"""
            namespace test
              enum Enum { A, B, C }
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                enum("Enum", listOf("A", "B", "C"))
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun value() {
        val typesStr = $$"""
            namespace test
              value Value
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                value("Value")
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun collection() {
        val typesStr = $$"""
            namespace test
              collection Collection<T,U,V>
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                collection("Collection", listOf("T", "U", "V"))
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun union() {
        val typesStr = $$"""
            namespace test
              primitive Int
              primitive String
              union Union { Int | String }
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                primitive("Int")
                primitive("String")
                union("Union") {
                    typeRef("Int")
                    typeRef("String")
                }
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun interface__empty() {
        val typesStr = $$"""
            namespace test
              interface Interface
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                interface_("Interface") { }
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun interface_() {
        val typesStr = $$"""
            namespace test
              primitive Int
              interface Interface { val prop:Int }
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                primitive("Int")
                interface_("Interface") {
                    propertyOf(setOf(VAL), "prop", "Int")
                }
            }
        }
        testPass(typesStr, expected)
    }

    @Test
    fun data() {
        val typesStr = $$"""
            namespace test
              primitive Int
              data Data {
               constructor(val prop1:Int)
               val prop2:Int
              }
        """
        val expected = typeModel("ParsedTypesUnit", true, namespaces = emptyList()) {
            namespace("test", imports = emptyList()) {
                primitive("Int")
                data("Data") {
                    constructor_ {
                        this.parameter("prop1", "Int", false)
                    }
                    propertyOf(setOf(VAL), "prop1", "Int")
                    propertyOf(setOf(VAL), "prop2", "Int")
                }
            }
        }
        testPass(typesStr, expected)
    }

}