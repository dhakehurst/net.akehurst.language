/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.test

import net.akehurst.language.typemodel.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.fail

object TypeModelTest {

    fun tmAssertEquals(expected: TypeModel?, actual: TypeModel?) {
        assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                assertEquals(expected.allNamespace.size, actual.allNamespace.size, "number of namespaces in model is different")
                for (k in expected.allNamespace.indices) {
                    val expEl = expected.allNamespace[k]
                    val actEl = actual.allNamespace[k]
                    tmAssertEquals(expEl, actEl, "TypeNamespace")
                }
            }
        }
    }

    fun tmAssertEquals(expected: TypeNamespace?, actual: TypeNamespace?, source: String) {
        assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                assertEquals(expected.allTypesByName.size, actual.allTypesByName.size, "number of types in model is different")
                for (k in expected.allTypesByName.keys) {
                    val expEl = expected.allTypesByName[k]
                    val actEl = actual.allTypesByName[k]
                    tmAssertEquals(expEl, actEl, "TypeModel")
                }
            }
        }
    }

    fun tmAssertEquals(expected: TypeInstance?, actual: TypeInstance?, source: String) {
        when {
            null == expected && null == actual -> true
            null == expected || null == actual -> fail("${source}.TypeUsage do not match")
            else -> {
                assertEquals(expected.isNullable, actual.isNullable, "Different nullable for ${source}.${expected}")
                assertEquals(expected.type.qualifiedName, actual.type.qualifiedName, "Different type for ${source}.${expected}")
                assertEquals(expected.typeArguments.size, actual.typeArguments.size, "Different number of arguments for ${source}.${expected}")
                for (i in expected.typeArguments.indices) {
                    val exp = expected.typeArguments[i]
                    val act = actual.typeArguments[i]
                    tmAssertEquals(exp, act, "Different argument[$i] for ${source}.${expected}")
                }
            }
        }
    }

    fun tmAssertEquals(expected: TypeDefinition?, actual: TypeDefinition?, source: String) {
        when {
            null == expected || null == actual -> fail("should never be null")
            expected is PrimitiveType && actual is PrimitiveType -> tmAssertEquals(expected, actual)
            expected is UnnamedSuperTypeType && actual is UnnamedSuperTypeType -> tmAssertEquals(expected, actual)
            expected is DataType && actual is DataType -> tmAssertEquals(expected, actual)
            expected is CollectionType && actual is CollectionType -> tmAssertEquals(expected, actual)
            expected is TupleType && actual is TupleType -> tmAssertEquals(expected, actual)
            else -> assertSame(expected, actual)
            //else -> fail("Types do not match expected '$expected' actual '$actual' for $source")
        }
    }

    private fun tmAssertEquals(expected: PrimitiveType, actual: PrimitiveType) {
        assertEquals(expected.name, actual.name)
    }

    private fun tmAssertEquals(expected: UnnamedSuperTypeType, actual: UnnamedSuperTypeType) {
        assertEquals(expected.subtypes.size, actual.subtypes.size, "Number of subTypes do not match for '${expected.name}'")
        for (i in 0 until expected.subtypes.size) {
            val exp = expected.subtypes[i]
            val act = actual.subtypes[i]
            tmAssertEquals(exp, act, "subtypes do not match")
        }
    }

    private fun tmAssertEquals(expected: DataType, actual: DataType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.supertypes.size, actual.supertypes.size, "Wrong number of supertypes for '${expected.name}'")
        assertEquals(expected.supertypes.map { it.type.qualifiedName }.toSet(), actual.supertypes.map { it.type.qualifiedName }.toSet())

        assertEquals(expected.subtypes.size, actual.subtypes.size, "Wrong number of subtypes for '${expected.name}'")
        assertEquals(expected.subtypes.map { it.type.qualifiedName }.toSet(), actual.subtypes.map { it.type.qualifiedName }.toSet())

        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual ElementType '${expected.name}'. [${actual.property.values.joinToString { it.name }}]")
            tmAssertEquals(expEl, actEl)
        }
    }

    private fun tmAssertEquals(expected: CollectionType, actual: CollectionType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.supertypes.size, actual.supertypes.size, "Wrong number of superTypes for '${expected.name}'")
        assertEquals(expected.supertypes.map { it.name }.toSet(), actual.supertypes.map { it.name }.toSet())

        // assertEquals(expected.subtypes.size, actual.subtypes.size, "Wrong number of subTypes for '${expected.name}'")
        // assertEquals(expected.subtypes.map { it.name }.toSet(), actual.subtypes.map { it.name }.toSet())

        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual ElementType '${expected.name}'. [${actual.property.values.joinToString { it.name }}]")
            tmAssertEquals(expEl, actEl)
        }
    }

    private fun tmAssertEquals(expected: TupleType, actual: TupleType) {
        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual TupleType '${expected.name}'. [${actual.property.values.joinToString { it.name }}]")
            tmAssertEquals(expEl, actEl)
        }
    }

    private fun tmAssertEquals(expected: PropertyDeclaration?, actual: PropertyDeclaration?) {
        when {
            null == expected || null == actual -> fail("should never be null")
            else -> {
                assertEquals(expected.name, actual.name)
                tmAssertEquals(expected.typeInstance, actual.typeInstance, "Different type instance for ${expected}")
                assertEquals(expected.index, actual.index, "Different childIndex for ${expected}")
            }
        }
    }
}