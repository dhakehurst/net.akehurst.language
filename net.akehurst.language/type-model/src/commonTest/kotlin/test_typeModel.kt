/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.api

import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_typeModel {

    @Test
    fun testBuilder() {
        val tm = typeModel("name", true) {
            namespace("ns") {
                dataType("Person") {
                    propertyPrimitiveType("name", "String", false, 0)
                }
            }
        }

        TypeModelTest.tmAssertEquals(tm, tm)
    }

    /*    @Test
        fun tuples() {

            val tm = typeModel("name", true) {
                namespace("ns") {
                    tupleType {
                        property("x", "String")
                    }
                }
            }

        }*/

    @Test
    fun conformsTo() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("A")
                dataType("B") {
                    supertypes("A")
                }
                dataType("C") {
                    supertypes("B")
                }
                dataType("D")
            }
        }

        assertTrue(SimpleTypeModelStdLib.NothingType.conformsTo(SimpleTypeModelStdLib.NothingType))
        assertFalse(SimpleTypeModelStdLib.NothingType.conformsTo(SimpleTypeModelStdLib.AnyType))
        assertFalse(SimpleTypeModelStdLib.AnyType.conformsTo(SimpleTypeModelStdLib.NothingType))
        assertTrue(SimpleTypeModelStdLib.String.conformsTo(SimpleTypeModelStdLib.AnyType))
        assertTrue(SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.String)).conformsTo(SimpleTypeModelStdLib.AnyType))

        val A = tm.findFirstByNameOrNull("A")!!
        val B = tm.findFirstByNameOrNull("B")!!
        val C = tm.findFirstByNameOrNull("C")!!
        val D = tm.findFirstByNameOrNull("D")!!

        assertTrue(A.conformsTo(SimpleTypeModelStdLib.AnyType.declaration))
        assertFalse(SimpleTypeModelStdLib.AnyType.declaration.conformsTo(A))
        assertTrue(A.conformsTo(A))
        assertFalse(A.conformsTo(B))
        assertFalse(A.conformsTo(C))
        assertFalse(A.conformsTo(D))

        assertTrue(B.conformsTo(SimpleTypeModelStdLib.AnyType.declaration))
        assertFalse(SimpleTypeModelStdLib.AnyType.declaration.conformsTo(B))
        assertTrue(B.conformsTo(A))
        assertTrue(B.conformsTo(B))
        assertFalse(B.conformsTo(C))
        assertFalse(B.conformsTo(D))

        assertTrue(C.conformsTo(SimpleTypeModelStdLib.AnyType.declaration))
        assertFalse(SimpleTypeModelStdLib.AnyType.declaration.conformsTo(C))
        assertTrue(C.conformsTo(A))
        assertTrue(C.conformsTo(B))
        assertTrue(C.conformsTo(C))
        assertFalse(C.conformsTo(D))

        TODO("Tuples, UnnamedSuperType, Lists, Primitives")
    }

}