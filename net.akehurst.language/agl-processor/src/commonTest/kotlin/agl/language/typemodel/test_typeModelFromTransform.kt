/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.TransformString
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_typemodel {

    companion object {
        fun test(grammarStr: String, typeModelStr: String, expected: TypeModel) {
            val res = Agl.processorFromStringDefault(
                grammarDefinitionStr = GrammarString(grammarStr),
                transformStr = TransformString(typeModelStr),
                crossReferenceModelStr = null
            )
            assertTrue(res.issues.isEmpty(), res.issues.toString())
            assertEquals(expected.asString(), res.processor!!.typeModel.asString())
        }
    }

    @Test
    fun rule_creates_type_with_different_name_same_namespace() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a '1' ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val typeModelStr = """
            transform {
                S -> S2
            }
        """.trimIndent()

        val expected = grammarTypeModel("test", "Test", "") {
            dataType("S", "S2") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        test(grammarStr, typeModelStr, expected)
    }

    @Test
    fun rule_creates_type_from_imported_external_namespace() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val typeModelStr = """
            transform {
                import types.*

                S -> S2
            }
        """.trimIndent()

        val typesModel = typeModel("", true) {
            namespace("types") {
                dataType("S2") {
                    propertyPrimitiveType("a", "String", false, 0)
                }
            }
        }
        val expected = grammarTypeModel("test", "Test", "", imports = listOf(SimpleTypeModelStdLib, typesModel.namespace["types"]!!)) {
            dataType("S", "S2") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        test(grammarStr, typeModelStr, expected)
    }

    @Test
    fun rule_creates_type_from_imported_declared_namespace() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val typeModelStr = """
            namespace types {
                datatype S2 {
                    composite val a: String
                }
            }
            transform {
                import types.*

                S -> S2
            }
        """.trimIndent()

        val typesModel = typeModel("", true) {
            namespace("types") {
                dataType("S2") {
                    propertyPrimitiveType("a", "String", false, 0)
                }
            }
        }
        val expected = grammarTypeModel("test", "Test", "", imports = listOf(SimpleTypeModelStdLib, typesModel.namespace["types"]!!)) {
            dataType("S", "S2") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        test(grammarStr, typeModelStr, expected)
    }
}