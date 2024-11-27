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
import net.akehurst.language.typemodel.asm.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

object TypeModelTest {

    fun tmAssertEquals(expected: TypeModel?, actual: TypeModel?) {
        assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                assertEquals(expected.namespace.size, actual.namespace.size, "number of namespaces in model is different")
                for (k in expected.namespace.indices) {
                    val expEl = expected.namespace[k]
                    val actEl = actual.namespace[k]
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
                assertEquals(expected.ownedTypesByName.size, actual.ownedTypesByName.size, "number of types in model is different")
                for (k in expected.ownedTypesByName.keys) {
                    val expEl = expected.ownedTypesByName[k]
                    val actEl = actual.ownedTypesByName[k]
                    tmAssertEquals(expEl, actEl, "TypeModel")
                }
            }
        }
    }

    fun tmAssertEquals(expected: TypeInstance?, actual: TypeInstance?, source: String) {
        when {
            expected == null && actual == null -> fail("should never be null")
            expected is TypeInstanceSimple && actual is TypeInstanceSimple -> tmAssertEquals(expected, actual, source)
            expected is TypeParameterReference && actual is TypeParameterReference -> tmAssertEquals(expected, actual, source)
            expected is TupleTypeInstance && actual is TupleTypeInstance -> tmAssertEquals(expected, actual, source)
           //expected is UnnamedSupertypeTypeInstance && actual is UnnamedSupertypeTypeInstance -> tmAssertEquals(expected, actual, source)
            expected==null || actual==null || expected::class != actual::class -> fail("expected an actual are different types: $source")
            else -> fail("Unsupported subtypes of TypeInstance")
        }
    }

    fun tmAssertEquals(expected: TypeInstanceSimple, actual: TypeInstanceSimple, source: String) {
        assertEquals(expected.isNullable, actual.isNullable, "Different nullable for ${source}.${expected}")
        assertEquals(expected.qualifiedTypeName, actual.qualifiedTypeName, "Different type for ${source}.${expected}")
        assertEquals(expected.typeArguments.size, actual.typeArguments.size, "Different number of arguments for ${source}.${expected}")
        for (i in expected.typeArguments.indices) {
            val exp = expected.typeArguments[i]
            val act = actual.typeArguments[i]
            tmAssertEquals(exp, act, "Different argument[$i] for ${source}.${expected}")
        }
    }

    fun tmAssertEquals(expected: TypeParameterReference, actual: TypeParameterReference, source: String) {
        assertEquals( expected.context.qualifiedName,actual.context.qualifiedName, source)
        assertEquals( expected.typeParameterName, actual.typeParameterName, source)
    }

        fun tmAssertEquals(expected: TypeArgument, actual: TypeArgument, source: String) {
        tmAssertEquals(expected.type, actual.type, source)
    }

    fun tmAssertEquals(expected: TupleTypeInstance, actual: TupleTypeInstance, source: String) {
        assertEquals(expected.isNullable, actual.isNullable, "Different nullable for ${source}.${expected}")
        assertEquals(expected.typeArguments.size, actual.typeArguments.size, "Different number of arguments for ${source}.${expected}")
        for (i in expected.typeArguments.indices) {
            val exp = expected.typeArguments[i]
            val act = actual.typeArguments[i]
            tmAssertEquals(exp, act, "Different argument[$i] for ${source}.${expected}")
        }
        tmAssertEquals(expected.declaration, actual.declaration, source)
    }
/*
    fun tmAssertEquals(expected: UnnamedSupertypeTypeInstance, actual: UnnamedSupertypeTypeInstance, source: String) {
        assertEquals(expected.isNullable, actual.isNullable, "Different nullable for ${source}.${expected}")
        assertEquals(expected.typeArguments.size, actual.typeArguments.size, "Different number of arguments for ${source}.${expected}")
        for (i in expected.typeArguments.indices) {
            val exp = expected.typeArguments[i]
            val act = actual.typeArguments[i]
            tmAssertEquals(exp, act, "Different argument[$i] for ${source}.${expected}")
        }
        assertEquals(expected.declaration.alternatives.size, actual.declaration.alternatives.size, "Different number of subtypes for ${source}.${expected}")
        for (i in expected.declaration.alternatives.indices) {
            val exp = expected.declaration.alternatives[i]
            val act = actual.declaration.alternatives[i]
            tmAssertEquals(exp, act, "subtype[$i] for ${source}.${expected}")
        }
    }
*/
    fun tmAssertEquals(expected: TypeDefinition?, actual: TypeDefinition?, source: String) {
        when {
            null == expected || null == actual -> fail("should never be null")
            expected is SpecialTypeSimple && actual is SpecialTypeSimple -> tmAssertEquals(expected, actual)
            expected is PrimitiveType && actual is PrimitiveType -> tmAssertEquals(expected, actual)
            expected is EnumType && actual is EnumType -> tmAssertEquals(expected, actual)
            expected is UnionType && actual is UnionType -> tmAssertEquals(expected, actual)
            expected is DataType && actual is DataType -> tmAssertEquals(expected, actual)
            expected is CollectionType && actual is CollectionType -> tmAssertEquals(expected, actual)
            expected is TupleType && actual is TupleType -> tmAssertEquals(expected, actual)
            else -> fail("Unsupported subtypes of TypeDeclaration '${expected::class.simpleName}' && '${actual::class.simpleName}'")
            //else -> fail("Types do not match expected '$expected' actual '$actual' for $source")
        }
    }

    private fun tmAssertEquals(expected: SpecialTypeSimple, actual: SpecialTypeSimple) {
        assertEquals(expected.qualifiedName, actual.qualifiedName)
    }

    private fun tmAssertEquals(expected: PrimitiveType, actual: PrimitiveType) {
        assertEquals(expected.name, actual.name)
    }

    private fun tmAssertEquals(expected: EnumType, actual: EnumType) {
        assertEquals(expected.name, actual.name)
        TODO("literals")
    }

    private fun tmAssertEquals(expected: UnionType, actual: UnionType) {
        assertEquals(expected.alternatives.size, actual.alternatives.size, "Number of alternatives do not match for '${expected.name}'")
        for (i in 0 until expected.alternatives.size) {
            val exp = expected.alternatives[i]
            val act = actual.alternatives[i]
            tmAssertEquals(exp, act, "alternatives do not match")
        }
    }

    private fun tmAssertEquals(expected: DataType, actual: DataType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.supertypes.size, actual.supertypes.size, "Wrong number of supertypes for '${expected.name}'")
        assertEquals(expected.supertypes.map { it.declaration.qualifiedName }.toSet(), actual.supertypes.map { it.declaration.qualifiedName }.toSet())

        assertEquals(expected.subtypes.size, actual.subtypes.size, "Wrong number of subtypes for '${expected.name}'")
        assertEquals(expected.subtypes.map { it.qualifiedTypeName }.toSet(), actual.subtypes.map { it.qualifiedTypeName }.toSet())

        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (i in expected.property.indices) {
            val expEl = expected.property[i]
            val actEl = actual.property[i]
            assertNotNull(
                actEl,
                "expected PropertyDeclaration '${expEl.name}' not found in actual ElementType '${expected.name}'. [${actual.property.joinToString { it.name.value }}]"
            )
            tmAssertEquals(expEl, actEl)
        }
    }

    private fun tmAssertEquals(expected: CollectionType, actual: CollectionType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.supertypes.size, actual.supertypes.size, "Wrong number of superTypes for '${expected.name}'")
        assertEquals(expected.supertypes.map { it.qualifiedTypeName }.toSet(), actual.supertypes.map { it.qualifiedTypeName }.toSet())

        // assertEquals(expected.subtypes.size, actual.subtypes.size, "Wrong number of subTypes for '${expected.name}'")
        // assertEquals(expected.subtypes.map { it.name }.toSet(), actual.subtypes.map { it.name }.toSet())

        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (i in expected.property.indices) {
            val expEl = expected.property[i]
            val actEl = actual.property[i]
            assertNotNull(
                actEl,
                "expected PropertyDeclaration '${expEl.name}' not found in actual ElementType '${expected.name}'. [${actual.property.joinToString { it.name.value }}]"
            )
            tmAssertEquals(expEl, actEl)
        }
    }

    private fun tmAssertEquals(expected: TupleType, actual: TupleType) {
        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (i in expected.property.indices) {
            val expEl = expected.property[i]
            val actEl = actual.property[i]
            assertNotNull(
                actEl,
                "expected PropertyDeclaration '${expEl.name}' not found in actual TupleType '${expected.name}'. [${actual.property.joinToString { it.name.value }}]"
            )
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