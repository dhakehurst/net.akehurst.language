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
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TransformString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_typemodelFromTransform {

    companion object {
        fun test(grammarStr: String, transformStr: String, expected: TypeModel) {
            val res = Agl.processorFromStringSimple(
                grammarDefinitionStr = GrammarString(grammarStr),
                transformStr = TransformString(transformStr),
                referenceStr = null
            )
            assertTrue(res.issues.isEmpty(), res.issues.toString())
            val actualTm = res.processor!!.typesModel
            assertTrue(res.processor!!.issues.isEmpty(), res.processor!!.issues.toString())
            assertEquals(expected.asString(), actualTm.asString())
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
        val transformStr = """
            namespace test
            transform Test {
                S : S2
            }
        """.trimIndent()

        val expected = typeModel("FromGrammarParsedGrammarUnit", true) {
            grammarTypeNamespace("test.Test") {
                dataType("S", "S2") {
                    propertyPrimitiveType("a", "String", false, 0)
                }
            }
        }
        test(grammarStr, transformStr, expected)
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
        val transformStr = """
            namespace test
            transform Test {
                import types.*

                S : S2
            }
        """.trimIndent()

        val typesModel = typeModel("", true) {
            namespace("types") {
                data("S2") {
                    propertyPrimitiveType("a", "String", false, 0)
                }
            }
        }
        val expected = grammarTypeModel("test", "Test", imports = listOf(StdLibDefault, typesModel.findNamespaceOrNull(QualifiedName("types"))!!)) {
            dataType("S", "S2") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        test(grammarStr, transformStr, expected)
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
        val extTypeModel = """
            namespace types {
                datatype S2 {
                    composite val a: String
                }
            }            
        """
        val transformStr = """
            namespace test
            transform Test {
                import types.*

                S : S2
            }
        """.trimIndent()

        val typesModel = typeModel("", true) {
            namespace("types") {
                data("S2") {
                    propertyPrimitiveType("a", "String", false, 0)
                }
            }
        }
        val expected = grammarTypeModel("test", "Test", imports = listOf(StdLibDefault, typesModel.findNamespaceOrNull(QualifiedName("types"))!!)) {
            dataType("S", "S2") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        test(grammarStr, transformStr, expected)
    }
}